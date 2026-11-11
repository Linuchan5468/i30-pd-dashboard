package com.veloster.vehicle;

/** Data validity, source pairing and radial geometry; no Android or synthetic UI. */
public final class GForcePointTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<0.00001f,message);}
    private static Telemetry.Sample sample(float value,boolean valid,boolean seen,boolean stale,long age){
        return new Telemetry.Sample(value,valid,false,seen,stale,age);
    }
    private static Telemetry.Sample good(float value){return sample(value,true,true,false,0);}
    public static void main(String[] args){
        GForcePoint point=GForcePoint.from(null,null);
        check(!point.valid&&!point.lateralValid&&!point.longitudinalValid,"missing axes have no point");
        check(Float.isNaN(point.x)&&Float.isNaN(point.y),"missing is not center geometry");
        point=GForcePoint.from(good(0),good(0));
        check(point.valid&&!point.clipped,"received zero is a real point");near(point.x,0,"zero x");near(point.y,0,"zero y");
        near(GForcePoint.RANGE_G,1,"practical display radius is exactly one G");
        point=GForcePoint.from(good(0.5f),good(0.5f));
        near(point.x,0.5f,"positive lateral is right");near(point.y,0.5f,"acceleration shows rearward weight transfer down");
        point=GForcePoint.from(good(-0.5f),good(-0.5f));
        near(point.x,-0.5f,"negative lateral is left");near(point.y,-0.5f,"braking shows forward weight transfer up");
        point=GForcePoint.from(good(0.5f),good(-0.5f));
        near(point.x,0.5f,"upper-right x");near(point.y,-0.5f,"upper-right y");
        near(point.lateralG,0.5f,"display convention leaves raw lateral sign unchanged");
        near(point.longitudinalG,-0.5f,"display convention leaves raw braking sign unchanged");
        point=GForcePoint.from(good(-0.5f),good(0.5f));
        near(point.x,-0.5f,"lower-left x");near(point.y,0.5f,"lower-left y");
        point=GForcePoint.from(good(1),good(0));
        check(point.valid&&!point.clipped,"exact boundary is not clipped");near(point.x,1,"boundary x");
        point=GForcePoint.from(good(3),good(4));
        check(point.clipped,"off-scale vector is marked");near(point.x,0.6f,"radial clip preserves direction x");near(point.y,0.8f,"radial clip preserves weight transfer direction y");
        near(point.lateralG,3,"clipping never changes lateral reading");near(point.longitudinalG,4,"clipping never changes longitudinal reading");
        point=GForcePoint.from(good(0.8f),good(0.8f));
        check(point.clipped,"diagonal resultant above one G is clipped even when each axis is below it");
        near(point.x,(float)Math.sqrt(0.5),"diagonal one-G radial bound x");near(point.y,(float)Math.sqrt(0.5),"diagonal one-G radial bound y");
        point=GForcePoint.from(good(1),sample(0,false,true,false,0));
        check(!point.valid&&point.lateralValid&&!point.longitudinalValid,"one invalid axis cannot create 2D point");
        point=GForcePoint.from(sample(0,true,false,false,0),good(0));check(!point.valid,"unreceived zero is not real center");
        point=GForcePoint.from(sample(1,true,true,true,60000),good(0));check(!point.valid,"stale axis is withheld defensively");
        point=GForcePoint.from(sample(1,true,true,false,1),good(0));check(!point.valid,"different receipt ages cannot be mixed");
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            point=GForcePoint.from(good(bad),good(0));check(!point.valid&&!point.lateralValid,"non-finite lateral withheld");
            point=GForcePoint.from(good(0),good(bad));check(!point.valid&&!point.longitudinalValid,"non-finite longitudinal withheld");
        }
        Telemetry data=new Telemetry();
        byte[] esp={(byte)255,(byte)0xe3,0x7f,0x30,0,0,0,0};
        data.hscanPd(0xc7,0x13,esp,1000);
        point=GForcePoint.from(data.sample(11,1000),data.sample(12,1000));
        check(point.valid,"canonical ESP12 center is received");near(point.lateralG,0,"ESP12 lateral already in G");near(point.longitudinalG,0,"ESP12 longitudinal already in G");
        esp[1]|=8;data.hscanPd(0xc7,0x13,esp,1100);
        point=GForcePoint.from(data.sample(11,1100),data.sample(12,1100));check(!point.valid&&!point.lateralValid&&point.longitudinalValid,"ESP12 axis status is respected");
        esp[1]&=~8;data.hscanPd(0xc7,0x13,esp,1200);
        point=GForcePoint.from(data.sample(11,61199),data.sample(12,61199));check(point.valid,"normal source freshness retained");
        point=GForcePoint.from(data.sample(11,61200),data.sample(12,61200));check(!point.valid,"exact canonical stale boundary hides point");
        data.clear();point=GForcePoint.from(data.sample(11,61201),data.sample(12,61201));check(!point.valid,"source clear removes the point");
        System.out.println("PASS "+checks+" G-force point checks");
    }
}
