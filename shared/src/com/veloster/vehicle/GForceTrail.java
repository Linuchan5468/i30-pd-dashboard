package com.veloster.vehicle;

/** Session-only received history: 16 directional maxima and 18 recent distinct points. */
public final class GForceTrail {
    public static final int DIRECTIONS=16,RECENT_POINTS=18;
    public static final class Entry {
        public final GForcePoint point;
        public final long receivedAt;
        private Entry(GForcePoint value,long at){point=value;receivedAt=at;}
    }
    public static final class Snapshot {
        public final Entry[] envelope,recent,history;
        public final GForcePoint current;
        public final long currentAt;
        /** Signed raw-axis extrema since reset; NaN until an actual valid pair arrives. */
        public final float minLateralG,maxLateralG,minLongitudinalG,maxLongitudinalG;
        private Snapshot(Entry[] maxima,Entry[] latest,Entry[] all,GForcePoint value,long at,
                float minLat,float maxLat,float minLong,float maxLong){
            envelope=maxima;recent=latest;history=all;current=value;currentAt=at;
            minLateralG=minLat;maxLateralG=maxLat;minLongitudinalG=minLong;maxLongitudinalG=maxLong;
        }
    }
    private final Entry[] envelope=new Entry[DIRECTIONS],recent=new Entry[RECENT_POINTS];
    private int count;
    private long lastReceipt=-1,lastRecorded=-1,blockedThrough=-1,lastReceiveNow=-1,lastSnapshotNow=-1,currentAt=-1;
    private GForcePoint current;
    private float minLateralG=Float.NaN,maxLateralG=Float.NaN,minLongitudinalG=Float.NaN,maxLongitudinalG=Float.NaN;

    private static boolean same(GForcePoint a,GForcePoint b){
        return a.lateralG==b.lateralG&&a.longitudinalG==b.longitudinalG;
    }
    private static int direction(GForcePoint point){
        double angle=Math.atan2(point.longitudinalG,point.lateralG);
        if(angle<0)angle+=Math.PI*2;
        return Math.min(DIRECTIONS-1,(int)(angle*DIRECTIONS/(Math.PI*2)));
    }
    private static double radiusSquared(GForcePoint point){
        return (double)point.lateralG*point.lateralG+(double)point.longitudinalG*point.longitudinalG;
    }
    /** Called on actual PD ESP12 delivery, never from painting or a display timer. */
    public synchronized void receive(Telemetry.Sample lateral,Telemetry.Sample longitudinal,long now){
        if(now<0||(lastReceiveNow>=0&&now<lastReceiveNow)){breakCurrent(now,true);return;}
        lastReceiveNow=now;
        GForcePoint point=GForcePoint.from(lateral,longitudinal);
        if(lateral==null||longitudinal==null||lateral.ageMs!=longitudinal.ageMs
                ||lateral.ageMs<0||lateral.ageMs>now){breakCurrent(now);return;}
        long at=now-lateral.ageMs;
        if(at<=blockedThrough)return;
        if(lastReceipt>=0&&at<lastReceipt){breakCurrent(now);return;}
        current=point;currentAt=at;lastReceipt=at;
        if(!point.valid){blockedThrough=at;return;}
        if(at==lastRecorded)return; // Replayed receipt cannot grow or refresh history.
        lastRecorded=at;
        if(Float.isNaN(minLateralG)){
            minLateralG=maxLateralG=point.lateralG;
            minLongitudinalG=maxLongitudinalG=point.longitudinalG;
        }else{
            minLateralG=Math.min(minLateralG,point.lateralG);maxLateralG=Math.max(maxLateralG,point.lateralG);
            minLongitudinalG=Math.min(minLongitudinalG,point.longitudinalG);maxLongitudinalG=Math.max(maxLongitudinalG,point.longitudinalG);
        }
        Entry entry=new Entry(point,at);
        int bucket=direction(point);
        if(envelope[bucket]==null||radiusSquared(point)>radiusSquared(envelope[bucket].point))envelope[bucket]=entry;
        int found=-1;
        for(int i=0;i<count;i++)if(same(recent[i].point,point)){found=i;break;}
        if(found>=0){
            for(int i=found;i<count-1;i++)recent[i]=recent[i+1];
            recent[count-1]=entry;
        }else{
            if(count==RECENT_POINTS){for(int i=1;i<count;i++)recent[i-1]=recent[i];}
            else count++;
            recent[count-1]=entry;
        }
    }
    /** A source/lifecycle gap hides live data; clearly historical records remain. */
    public synchronized void breakCurrent(long now){
        breakCurrent(now,false);
    }
    private void breakCurrent(long now,boolean clockReset){
        // A caller may capture its clock just before a concurrent receipt arrives.
        // Such a lifecycle/reset call must still exclude that held pre-stop receipt.
        blockedThrough=clockReset?now:Math.max(blockedThrough,Math.max(now,Math.max(lastReceipt,currentAt)));
        current=null;currentAt=-1;lastReceipt=-1;lastRecorded=-1;
        lastReceiveNow=now;lastSnapshotNow=-1;
    }
    /** Explicit user reset, including the held receipt so it cannot repaint history. */
    public synchronized void reset(long now){
        for(int i=0;i<DIRECTIONS;i++)envelope[i]=null;
        for(int i=0;i<RECENT_POINTS;i++)recent[i]=null;
        minLateralG=maxLateralG=minLongitudinalG=maxLongitudinalG=Float.NaN;
        count=0;breakCurrent(now);
    }
    public synchronized Snapshot snapshot(long now){
        if(now<0||(lastSnapshotNow>=0&&now<lastSnapshotNow))breakCurrent(now,true);
        lastSnapshotNow=now;
        if(currentAt>=0&&now>=currentAt&&now-currentAt>=Telemetry.STALE_MS)breakCurrent(now);
        Entry[] maxima=envelope.clone(),latest=new Entry[count];
        System.arraycopy(recent,0,latest,0,count);
        Entry[] combined=new Entry[DIRECTIONS+count];int size=0;
        for(Entry entry:maxima){
            if(entry==null)continue;
            boolean duplicate=false;
            for(Entry newer:latest)if(same(entry.point,newer.point)){duplicate=true;break;}
            if(!duplicate)combined[size++]=entry;
        }
        for(Entry entry:latest)combined[size++]=entry;
        Entry[] history=new Entry[size];System.arraycopy(combined,0,history,0,size);
        // A render timestamp captured before a concurrent receipt cannot show future data.
        GForcePoint live=currentAt>=0&&currentAt<=now?current:null;
        return new Snapshot(maxima,latest,history,live,live==null?-1:currentAt,
            minLateralG,maxLateralG,minLongitudinalG,maxLongitudinalG);
    }
}
