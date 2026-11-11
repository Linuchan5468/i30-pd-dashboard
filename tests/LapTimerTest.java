import com.veloster.vehicle.LapCircuit;
import com.veloster.vehicle.LapTimer;
import com.veloster.vehicle.Cn7CircuitCatalog;
import java.util.HashSet;

/** Synthetic clock/positions; no GPS-provider or track-driving claim. */
public final class LapTimerTest {
    private static int checks;
    private static final double DEG=111195.0802335329;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b,double tolerance,String why){check(Math.abs(a-b)<=tolerance,why);}
    private static LapCircuit track(){return new LapCircuit("synthetic","Synthetic test only","Square",500,new LapCircuit.Point(0,0),new LapCircuit.Point(0,0),new double[0],"synthetic test fixture; not a real circuit");}
    private static LapTimer gps(){return new LapTimer(new LapCircuit[]{track()},10,20);}
    private static void location(LapTimer t,double east,double north,long at){t.onLocation(north/DEG,east/DEG,5,at,at);}
    private static void state(LapTimer t,long at,LapTimer.State state,String why){check(t.snapshot(at).state==state,why);}
    private static void complete(LapTimer t){
        location(t,-20,0,0);check(t.armGps(0),"fresh start proximity can arm");
        location(t,20,0,1000);state(t,1000,LapTimer.State.RUNNING,"source point proximity starts lap");
        location(t,120,0,3000);location(t,120,120,5000);location(t,0,120,7000);location(t,-20,0,10000);location(t,20,0,12000);
    }
    public static void main(String[] args){
        LapTimer t=new LapTimer();check(t.snapshot(0).mode==LapTimer.Mode.MANUAL,"no catalog defaults to manual");
        check(t.startManual(0),"manual starts without GPS");check(!t.markLap(299),"duplicate touch guard");
        check(t.markLap(1000),"first manual lap");check(t.markLap(2500),"second manual lap");
        check(t.snapshot(2500).history.length==2,"two complete laps only");
        check(t.snapshot(2500).bestMs==1000&&t.snapshot(2500).lastMs==1500,"best and previous lap");
        check(t.snapshot(2500).history[0].lap==2&&t.snapshot(2500).currentLap==3,"manual lap numbering");
        check(!t.mode(LapTimer.Mode.GPS),"mode locked during run");t.stop(2800);
        state(t,2800,LapTimer.State.STOPPED,"stop preserves partial display");
        check(t.snapshot(2800).elapsedMs==300&&t.snapshot(2800).history.length==2,"stop does not falsely complete partial lap");
        String saved=t.save();LapTimer restored=new LapTimer();restored.restore(saved);
        check(saved.equals(restored.save()),"manual history persists exactly");
        state(restored,0,LapTimer.State.IDLE,"restoring never starts a stopwatch");
        restored.clearHistory();check(restored.snapshot(0).history.length==0,"reset clears history");
        t=new LapTimer();t.startManual(1000);check(!t.markLap(999),"backward manual time rejected");state(t,1000,LapTimer.State.ABORTED,"backward manual clock aborts");

        LapCircuit.Point point=new LapCircuit.Point(0,0);
        near(point.proximity(0,-20/DEG,0,20/DEG,1),0.5,0.00001,"original-style segment projection finds midpoint");
        near(point.proximity(0,0,0,20/DEG,1),0,0.00001,"projection clamps at first endpoint");
        check(Double.isNaN(point.proximity(20/DEG,-20/DEG,20/DEG,20/DEG,10)),"nearby parallel passage outside radius does not hit");
        t=gps();check(t.snapshot(0).mode==LapTimer.Mode.GPS,"verified source catalog defaults GPS");
        check(!t.armGps(0),"cannot arm absent GPS");
        complete(t);check(t.snapshot(12000).history.length==1,"one complete GPS lap");
        check(t.snapshot(12000).history[0].elapsedMs==10500,"source-point segment fractions set start and goal estimates");
        check(t.snapshot(12000).history[0].sectorTimes().length==0&&t.snapshot(12000).sectorCount==0,"unverified OEM sector boundaries are not invented");
        check(t.snapshot(12000).currentLap==2,"GPS continues into next lap");
        location(t,20,0,13000);location(t,20,0,14000);
        check(t.snapshot(14000).history.length==1,"stationary repeats cannot append laps");
        t.stop(14000);String gpsSaved=t.save();restored=gps();restored.restore(gpsSaved);
        check(gpsSaved.equals(restored.save())&&!restored.snapshot(0).gpsFresh,"GPS results persist without fabricating fresh position");

        t=gps();location(t,-20,0,0);t.armGps(0);location(t,20,0,1000);location(t,60,0,2000);location(t,-20,0,4000);
        check(t.snapshot(4000).history.length==0,"short-cut return cannot complete full source course");
        t=gps();location(t,0,0,0);t.armGps(0);location(t,0,0,1000);
        state(t,1000,LapTimer.State.ARMED,"stationary GPS cannot start lap");
        t=gps();location(t,-20,0,0);t.armGps(0);location(t,20,0,1000);state(t,4001,LapTimer.State.ABORTED,"GPS silence aborts at quality boundary");
        check(t.snapshot(4001).history.length==0,"stale GPS adds no result");
        t=gps();location(t,-20,0,0);t.armGps(0);t.onLocation(0,20/DEG,11,1000,1000);
        state(t,1000,LapTimer.State.ABORTED,"accuracy above ten metres aborts");check(!t.snapshot(1000).gpsFresh,"bad accuracy is not live position");
        t.onLocation(0,-20/DEG,5,999,1001);check(!t.armGps(1001),"a fix held before invalidity cannot rearm");
        t=gps();location(t,-20,0,0);t.armGps(0);location(t,20,0,1000);location(t,2000,0,1100);
        state(t,1100,LapTimer.State.ABORTED,"implausible position jump aborts");check(!t.snapshot(1100).gpsFresh,"position spike cannot arm a new lap");
        t=gps();location(t,-20,0,100);t.armGps(100);location(t,20,0,200);location(t,-20,0,199);
        state(t,200,LapTimer.State.ABORTED,"backward GPS fix time aborts");
        t=gps();location(t,-20,0,100);t.gap(200);t.onLocation(0,-20/DEG,5,200,201);
        check(!t.armGps(201),"held source fix at gap time cannot revive position");
        location(t,-20,0,202);check(t.armGps(202),"new fix after gap can arm");
        t.gpsUnavailable("permission missing",203);state(t,203,LapTimer.State.ABORTED,"provider or permission loss aborts");
        check("permission missing".equals(t.snapshot(203).reason),"provider reason remains readable");
        String interruption=t.snapshot(203).reason;location(t,-20,0,204);
        check(t.snapshot(204).gpsFresh&&interruption.equals(t.snapshot(204).reason),"fresh GPS preserves an aborted measurement reason");
        t=gps();t.gap(100);t.onLocation(0,0,5,100,101);
        check(t.snapshot(101).reason.length()>0,"old first fix creates a source warning");
        location(t,0,0,102);check(t.snapshot(102).gpsFresh&&t.snapshot(102).reason.length()==0,"new valid GPS clears idle warmup warning");
        t=new LapTimer();t.gpsUnavailable("GPS warming up",100);location(t,0,0,101);
        check(t.snapshot(101).reason.length()==0&&t.snapshot(101).mode==LapTimer.Mode.MANUAL,"manual idle also clears recovered GPS warning");
        t.startManual(200);t.stop(1200);t.gpsUnavailable("GPS unavailable",1300);location(t,0,0,1301);
        check(t.snapshot(1301).state==LapTimer.State.STOPPED&&t.snapshot(1301).reason.length()==0&&t.snapshot(1301).elapsedMs==1000,"stopped display clears recovered source warning without resetting elapsed time");
        t=gps();location(t,3000,0,0);check(!t.armGps(0),"distant circuit cannot arm");
        String refusal=t.snapshot(0).reason;location(t,3005,0,1000);
        check(t.snapshot(1000).gpsFresh&&refusal.equals(t.snapshot(1000).reason),"fresh GPS preserves non-source circuit refusal");
        t=new LapTimer(new LapCircuit[]{track()},0,20);location(t,0,0,0);t.mode(LapTimer.Mode.GPS);check(!t.armGps(0),"explicitly disabled criterion stays disabled");
        refusal=t.snapshot(0).reason;location(t,0,0,1000);check(refusal.equals(t.snapshot(1000).reason),"fresh GPS preserves disabled-criterion refusal");

        t=new LapTimer();t.startManual(0);
        for(int i=1;i<=99;i++)check(t.markLap(i*1000),"bounded lap accepted");
        state(t,99000,LapTimer.State.STOPPED,"native-compatible ninety-nine-lap session limit");
        check(t.snapshot(99000).history.length==99&&!t.markLap(100000),"no hundredth lap in completed session");
        t.startManual(100000);t.markLap(101000);t.stop(101001);check(t.snapshot(101001).history.length==99,"retained history is bounded across sessions");
        String valid=t.save();t.restore("1|MANUAL|manual|manual,MANUAL,1,-5,-5|garbage");check(t.snapshot(0).history.length==0,"bad persisted records skipped");
        t.restore(valid);check(t.snapshot(0).history.length==99,"valid bounded records reload");

        LapCircuit[] korea=Cn7CircuitCatalog.korea(),usa=Cn7CircuitCatalog.usa(),all=Cn7CircuitCatalog.all();
        check(korea.length==6&&usa.length==99&&all.length==105,"actual source circuit/course counts");
        HashSet<String> ids=new HashSet<String>();
        for(LapCircuit c:all){
            check(ids.add(c.id),"source course IDs unique");check(c.automaticReady()&&c.finish==null,"actual start/goal points are not invented gate endpoints");
            check(c.lengthMetres>0&&c.provenance.contains("SHA256="),"source length and provenance present");
            check(c.outline().length>=6,"source circuit area outline preserved");
        }
        near(korea[0].start.latitude,34.73076388888889,1e-12,"KIC source start latitude unchanged");
        near(korea[0].goal.longitude,126.41619166666666,1e-12,"KIC source goal longitude unchanged");
        check(korea[0].lengthMetres==5615,"KIC source course length unchanged");
        System.out.println("PASS "+checks+" lap timer/catalog checks");
    }
}
