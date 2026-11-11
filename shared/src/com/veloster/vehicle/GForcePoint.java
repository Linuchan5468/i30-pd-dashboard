package com.veloster.vehicle;

/** Current received G-force point. Geometry is separate from Android drawing. */
public final class GForcePoint {
    public static final float RANGE_G=1f;
    public final boolean lateralValid,longitudinalValid,valid,clipped;
    public final float lateralG,longitudinalG,x,y;

    private GForcePoint(Telemetry.Sample lateral,Telemetry.Sample longitudinal){
        lateralValid=usable(lateral);longitudinalValid=usable(longitudinal);
        lateralG=lateralValid?lateral.value:Float.NaN;
        longitudinalG=longitudinalValid?longitudinal.value:Float.NaN;
        // ESP12 publishes both axes in one receipt. Never combine different receipts.
        valid=lateralValid&&longitudinalValid&&lateral.ageMs==longitudinal.ageMs;
        if(!valid){x=Float.NaN;y=Float.NaN;clipped=false;return;}
        // Canvas Y grows downwards. Acceleration transfers weight rearwards;
        // this display convention must never change the received numeric values.
        double xx=lateralG/RANGE_G,yy=longitudinalG/RANGE_G;
        double distance=Math.sqrt(xx*xx+yy*yy);
        clipped=distance>1;
        if(clipped){xx/=distance;yy/=distance;}
        x=(float)xx;y=(float)yy;
    }
    private static boolean usable(Telemetry.Sample sample){
        return sample!=null&&sample.valid&&sample.seen&&!sample.stale&&sample.ageMs>=0
            &&!Float.isNaN(sample.value)&&!Float.isInfinite(sample.value);
    }
    public static GForcePoint from(Telemetry.Sample lateral,Telemetry.Sample longitudinal){
        return new GForcePoint(lateral,longitudinal);
    }
}
