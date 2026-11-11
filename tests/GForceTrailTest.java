package com.veloster.vehicle;

/** Receipt identity, bounded history, maxima and lifecycle behavior without Android. */
public final class GForceTrailTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<0.00001f,message);}
    private static Telemetry.Sample sample(float value,boolean valid,long age){
        return new Telemetry.Sample(value,valid,false,true,false,age);
    }
    private static void receive(GForceTrail trail,float lateral,float longitudinal,long at,long now){
        trail.receive(sample(lateral,true,now-at),sample(longitudinal,true,now-at),now);
    }
    private static int maxima(GForceTrail.Snapshot snapshot){int n=0;for(GForceTrail.Entry e:snapshot.envelope)if(e!=null)n++;return n;}
    public static void main(String[] args){
        GForceTrail trail=new GForceTrail();GForceTrail.Snapshot view=trail.snapshot(0);
        check(view.current==null&&view.history.length==0,"new history contains no synthetic center");
        check(Float.isNaN(view.minLateralG)&&Float.isNaN(view.maxLateralG)
            &&Float.isNaN(view.minLongitudinalG)&&Float.isNaN(view.maxLongitudinalG),"no invented zero peaks before a valid receipt");
        receive(trail,0,0,1000,1000);view=trail.snapshot(1000);
        check(view.current!=null&&view.current.valid,"received zero is a live point");
        near(view.current.x,0,"zero stays centered x");near(view.current.y,0,"zero stays centered y");
        check(view.recent.length==1&&maxima(view)==1&&view.history.length==1,"zero is retained once without overlapping records");
        near(view.minLateralG,0,"genuine zero initializes minimum lateral");near(view.maxLongitudinalG,0,"genuine zero initializes maximum longitudinal");
        for(int i=0;i<200;i++)trail.snapshot(1000+i);
        view=trail.snapshot(1200);check(view.recent.length==1&&view.currentAt==1000,"painting never appends or refreshes a source");
        receive(trail,0,0,1000,1300);view=trail.snapshot(1300);
        check(view.recent.length==1&&view.recent[0].receivedAt==1000,"same receipt with increasing age is not appended");
        receive(trail,0,0,2000,2300);view=trail.snapshot(2300);
        check(view.currentAt==2000&&view.recent.length==1&&view.recent[0].receivedAt==2000,"new identical receipt refreshes current but not memory count");
        check(view.envelope[0].receivedAt==1000,"equal radius does not replace a historical maximum");
        view.recent[0]=null;check(trail.snapshot(2301).recent[0]!=null,"snapshot arrays cannot mutate retained records");
        trail.receive(sample(1,false,0),sample(0,true,0),2400);view=trail.snapshot(2400);
        check(view.current!=null&&!view.current.valid&&view.history.length==1,"invalid axis hides current geometry but keeps historical records");
        near(view.maxLateralG,0,"invalid pair does not update lateral peak");
        receive(trail,0,0,2000,2500);view=trail.snapshot(2500);
        check(view.currentAt==2400&&!view.current.valid&&view.recent.length==1,"old valid receipt cannot bridge an invalid one");
        receive(trail,0.3f,-0.2f,2600,2600);view=trail.snapshot(2600);
        check(view.current.valid&&view.recent.length==2,"a new valid receipt resumes without interpolation");
        near(view.maxLateralG,0.3f,"valid paired lateral maximum");near(view.minLongitudinalG,-0.2f,"valid paired braking minimum preserves raw sign");
        trail.breakCurrent(2700);view=trail.snapshot(2800);
        check(view.current==null&&view.recent.length==2,"disconnect or background hides current and retains records");
        near(view.maxLateralG,0.3f,"source gap retains explicitly historical peak");
        receive(trail,0.3f,-0.2f,2600,2801);check(trail.snapshot(2801).current==null,"held receipt cannot revive after lifecycle stop");
        receive(trail,-0.1f,0.2f,2802,2802);view=trail.snapshot(2802);
        check(view.current.valid&&view.currentAt==2802&&view.recent.length==3,"fresh resume receipt is admitted");
        trail.reset(2900);view=trail.snapshot(2900);
        check(view.current==null&&view.history.length==0&&view.recent.length==0&&maxima(view)==0,"explicit reset clears all bounded memory");
        check(Float.isNaN(view.maxLateralG)&&Float.isNaN(view.minLongitudinalG),"reset also clears peak validity");
        receive(trail,-0.1f,0.2f,2802,3000);check(trail.snapshot(3000).history.length==0,"reset cannot repaint cached history");
        receive(trail,0,0,3001,3001);check(trail.snapshot(3001).current.valid,"fresh received zero works after reset");
        view=trail.snapshot(63000);check(view.current!=null,"current uses existing freshness threshold before boundary");
        view=trail.snapshot(63001);check(view.current==null&&view.history.length==1,"exact stale boundary hides live point but retains clearly historical zero");
        view=trail.snapshot(1000000000L);check(view.history.length==1,"historical maxima/recent records have no arbitrary TTL");

        trail=new GForceTrail();
        for(int i=0;i<GForceTrail.DIRECTIONS;i++){
            double angle=(i+0.5)*Math.PI*2/GForceTrail.DIRECTIONS;
            receive(trail,(float)(Math.cos(angle)*0.5),(float)(Math.sin(angle)*0.5),1000+i,1000+i);
        }
        view=trail.snapshot(1100);check(maxima(view)==16&&view.recent.length==16,"all sixteen direction buckets are independent");
        GForceTrail.Entry first=view.envelope[0];double angle=Math.PI/GForceTrail.DIRECTIONS;
        receive(trail,(float)(Math.cos(angle)*0.25),(float)(Math.sin(angle)*0.25),1200,1200);
        check(trail.snapshot(1200).envelope[0].receivedAt==first.receivedAt,"smaller radius preserves directional maximum");
        receive(trail,(float)(Math.cos(angle)*1.5),(float)(Math.sin(angle)*1.5),1300,1300);
        view=trail.snapshot(1300);check(view.envelope[0].receivedAt==1300,"larger radius replaces only its direction");
        near(view.envelope[1].point.lateralG,(float)(Math.cos(3*Math.PI/GForceTrail.DIRECTIONS)*0.5),"neighbor maximum is unchanged");

        trail=new GForceTrail();
        for(int i=0;i<5000;i++){
            receive(trail,i/10000f,(float)Math.sin(i*0.07),i+1,i+1);
            if(i%97==0){view=trail.snapshot(i+1);check(view.recent.length<=18&&maxima(view)<=16&&view.history.length<=34,"high-rate history remains bounded");}
        }
        view=trail.snapshot(5000);check(view.recent.length==18,"recent point cap is exactly eighteen");
        check(view.recent[0].receivedAt==4983&&view.recent[17].receivedAt==5000,"oldest recent coordinates are replaced first");
        GForcePoint repeated=view.recent[3].point;
        receive(trail,repeated.lateralG,repeated.longitudinalG,5001,5001);view=trail.snapshot(5001);
        check(view.recent.length==18&&view.recent[17].receivedAt==5001,"returning to a prior coordinate moves it without duplicate storage");
        near(view.recent[17].point.lateralG,repeated.lateralG,"revisited coordinate is actual received value");

        trail=new GForceTrail();receive(trail,3,4,1000,1500);view=trail.snapshot(1500);
        check(view.currentAt==1000&&view.recent[0].receivedAt==1000,"receipt is reconstructed as now minus paired age");
        near(view.history[0].point.x,0.6f,"clipped historical direction x");near(view.history[0].point.y,0.8f,"clipped historical weight transfer direction y");
        near(view.history[0].point.lateralG,3,"history retains actual off-scale lateral value");
        near(view.maxLateralG,3,"peak readout retains off-scale raw lateral");near(view.maxLongitudinalG,4,"peak readout retains off-scale raw longitudinal");
        trail.receive(sample(0,true,0),sample(0,true,1),1600);view=trail.snapshot(1600);
        check(view.current==null&&view.history.length==1,"unpaired axis receipts cannot add a center point");
        receive(trail,1,1,1700,1700);view=trail.snapshot(1699);
        check(view.current==null,"a frame captured before a concurrent receipt cannot show future data");
        view=trail.snapshot(1701);check(view.current!=null&&view.current.valid,"later render sees the genuine concurrent receipt");
        view=trail.snapshot(1600);check(view.current==null&&view.history.length>0,"backward render clock breaks live data and keeps historical records");
        receive(trail,0,0,1702,1702);check(trail.snapshot(1702).current.valid,"fresh input after clock break resumes");
        receive(trail,1,0,1600,1600);view=trail.snapshot(1703);
        check(view.current==null&&view.history.length>0,"backward receive clock cannot connect live data");
        trail.receive(sample(0,true,2000),sample(0,true,2000),1800);
        check(trail.snapshot(1800).current==null,"negative reconstructed source time is rejected");
        trail=new GForceTrail();receive(trail,1,1,5000,5000);
        trail.reset(4999);receive(trail,1,1,5000,5001);
        check(trail.snapshot(5001).history.length==0,"reset clock captured before a concurrent receipt cannot re-admit it");
        receive(trail,0,0,5002,5002);check(trail.snapshot(5002).current.valid,"new post-reset receipt is still admitted");
        // Radial maxima and axis maxima are different statistics in one angular bucket.
        trail=new GForceTrail();receive(trail,0.9f,0.1f,1000,1000);
        receive(trail,0.88f,0.3f,1100,1100);view=trail.snapshot(1100);
        check(view.envelope[0].receivedAt==1100,"larger resultant wins directional contour bucket");
        near(view.maxLateralG,0.9f,"axis peak is not lost when a larger radius has a smaller lateral value");
        near(view.maxLongitudinalG,0.3f,"longitudinal peak uses actual raw samples");
        System.out.println("PASS "+checks+" G-force trail checks");
    }
}
