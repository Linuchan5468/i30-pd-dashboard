import com.veloster.vehicle.AccelerationTimer;

/** Synthetic timing receipts only: no Android, road test or device connection. */
public final class AccelerationTimerTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static AccelerationTimer timer(int from,int to){AccelerationTimer t=new AccelerationTimer();check(t.configure(from,to),"valid configuration");return t;}
    private static void speed(AccelerationTimer t,float speed,long at){t.onSpeed(speed,true,at);}
    private static void state(AccelerationTimer t,long now,AccelerationTimer.State expected,String why){check(t.snapshot(now).state==expected,why);}
    private static void complete(AccelerationTimer t,long base){
        speed(t,0,base);check(t.arm(base),"arm fresh genuine zero");
        speed(t,1,base+100);speed(t,10,base+400);speed(t,20,base+700);speed(t,30,base+1000);
        state(t,base+1000,AccelerationTimer.State.COMPLETE,"zero-start completes once");
    }
    public static void main(String[] args){
        AccelerationTimer t=timer(0,30);
        check(!t.arm(0),"absent speed cannot arm");
        check(!t.snapshot(0).speedFresh,"no synthetic speed");
        complete(t,0);
        check(t.snapshot(1000).history.length==1,"one completed record");
        check(t.snapshot(1000).elapsedMs==900,"CN7 zero-start begins at received1kmh");
        check(!t.snapshot(1000).canArm,"above start speed cannot arm");
        for(int i=0;i<100;i++)t.snapshot(1000+i);
        check(t.snapshot(1100).history.length==1,"rendering cannot append measurements");
        speed(t,0,1200);speed(t,40,1400);
        check(t.snapshot(1400).history.length==1,"completion never auto-rearms");
        speed(t,0,1500);check(t.arm(1500),"new explicit arm permitted");t.cancel();
        state(t,1500,AccelerationTimer.State.CANCELLED,"cancel pending run");
        check(t.snapshot(1500).history.length==1,"cancel adds no history");

        t=timer(20,40);speed(t,0,0);check(t.arm(0),"rolling target can be armed while stationary");
        speed(t,10,100);speed(t,19,200);speed(t,20,250);
        state(t,250,AccelerationTimer.State.ARMED,"nonzero start uses strictly greater speed");
        speed(t,21,300);state(t,300,AccelerationTimer.State.RUNNING,"speed starts measurement");
        speed(t,39,600);speed(t,41,700);
        check(t.snapshot(700).elapsedMs==400,"finish is linearly interpolated between genuine speed receipts");
        check(t.snapshot(700).history[0].startKmh==20&&t.snapshot(700).history[0].targetKmh==40,"record retains selected interval");

        t=timer(20,40);speed(t,19,900);check(t.arm(1050),"arm from fresh receipt");
        speed(t,21,1100);speed(t,41,1500);
        check(t.snapshot(1500).elapsedMs==430,"start cannot precede explicit arm");
        t=timer(0,30);speed(t,0,0);check(t.arm(0),"arm0");
        speed(t,0.5f,100);state(t,100,AccelerationTimer.State.ARMED,"0.5kmh does not start CN7 zero interval");
        speed(t,2,200);check(t.snapshot(200).elapsedMs==67,"start1kmh interpolates at133.33ms");
        check(!t.configure(10,40),"selection locked while active");
        t.cancel();check(t.configure(10,40),"selection editable after cancellation");
        check(!AccelerationTimer.validRange(-10,30)&&!AccelerationTimer.validRange(20,20)&&!AccelerationTimer.validRange(0,201)&&!AccelerationTimer.validRange(5,30),"range validates bounds order and ten kmh steps");
        check(AccelerationTimer.validRange(190,200),"highest CN7 kmh pair");

        t=timer(0,30);speed(t,0,0);check(t.snapshot(1000).canArm,"exact maximum age is accepted");
        check(!t.snapshot(1001).canArm&&!t.arm(1001),"stale zero cannot arm");
        t=timer(0,30);speed(t,0,0);t.arm(0);speed(t,1,1000);
        state(t,1000,AccelerationTimer.State.RUNNING,"exact continuity boundary accepted");
        speed(t,30,2001);state(t,2001,AccelerationTimer.State.ABORTED,"long gap aborts before completion");
        check(t.snapshot(2001).history.length==0,"gaps cannot produce false record");
        t=timer(0,30);speed(t,0,0);t.arm(0);speed(t,1,100);
        state(t,1101,AccelerationTimer.State.ABORTED,"silence aborts on snapshot without another packet");
        t=timer(0,30);speed(t,0,0);t.arm(0);t.gap(10);speed(t,30,20);
        state(t,20,AccelerationTimer.State.ABORTED,"disconnect cannot complete or rearm");
        speed(t,0,30);check(t.arm(30),"fresh receipt after gap can explicitly arm");
        speed(t,1,40);t.onSpeed(0,false,50);
        state(t,50,AccelerationTimer.State.ABORTED,"invalid source aborts running run");

        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,301}){
            t=timer(0,30);speed(t,0,0);t.arm(0);t.onSpeed(bad,true,100);
            state(t,100,AccelerationTimer.State.ABORTED,"non-finite or out-of-range source aborts");
            check(!t.snapshot(100).speedFresh,"invalid speed hidden");
        }
        t=timer(0,30);speed(t,0,100);t.arm(100);speed(t,2,200);speed(t,30,199);
        state(t,200,AccelerationTimer.State.ABORTED,"backward receipt aborts");
        t=timer(0,30);speed(t,0,100);t.arm(100);speed(t,30,100);
        state(t,100,AccelerationTimer.State.ARMED,"same read timestamp has no extra timing resolution");
        check(t.snapshot(1101).state==AccelerationTimer.State.ABORTED,"duplicate timestamp does not refresh speed");
        t=timer(0,30);speed(t,0,100);t.arm(100);speed(t,2,200);
        state(t,199,AccelerationTimer.State.RUNNING,"a frame timestamp before concurrent receipt does not abort");
        check(!t.snapshot(199).speedFresh,"future receipt hidden from earlier UI frame");
        t.gap(210);speed(t,0,205);check(!t.arm(211),"pre-gap receipt cannot revive current speed");
        t=timer(0,30);speed(t,0,100);t.gap(200);speed(t,0,200);
        check(!t.arm(201),"receipt at exact gap timestamp cannot revive held data");
        speed(t,0,201);check(t.arm(201),"strictly later post-gap receipt can arm");

        t=timer(0,30);
        for(int i=0;i<15;i++)complete(t,i*2000);
        check(t.snapshot(29000).history.length==10,"history bounded at CN7's ten records");
        String saved=t.save();AccelerationTimer restored=new AccelerationTimer();restored.restore(saved);
        check(saved.equals(restored.save()),"configuration and bounded records survive process restart");
        check(restored.snapshot(0).state==AccelerationTimer.State.IDLE&&!restored.snapshot(0).speedFresh,"restoring cannot create live data or arm");
        restored.restore("1|10|40|10,40,1234|bad|0,30,-1|0,30,999999999|0,30,2000");
        check(restored.snapshot(0).history.length==2&&restored.snapshot(0).history[0].elapsedMs==1234,"malformed persisted records skipped in original order");
        String good=restored.save();restored.restore("1|10|10|0,30,1000");
        check(good.equals(restored.save()),"corrupt config leaves prior valid records");
        restored.clearHistory();check(restored.snapshot(0).history.length==0,"explicit reset clears records");
        check(restored.snapshot(0).startKmh==10&&restored.snapshot(0).targetKmh==40,"history reset keeps configuration");
        check("00:00.00".equals(AccelerationTimer.formatTime(0))&&"00:06.99".equals(AccelerationTimer.formatTime(6999))&&"01:01.00".equals(AccelerationTimer.formatTime(61000)),"time format floors centiseconds without claiming accuracy");
        System.out.println("PASS "+checks+" acceleration timer checks");
    }
}
