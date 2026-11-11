package com.veloster.vehicle;

import java.util.ArrayList;
import java.util.Locale;

/** Independent read-only manual/GPS lap timer. No OEM engine code is reused. */
public final class LapTimer {
    /** App quality limits, not a claim about the OEM's native matching algorithm. */
    public static final long GPS_STALE_MS=3000,MIN_GPS_LAP_MS=10000,MAX_LAP_MS=3600000;
    public static final float MAX_ACCURACY_METRES=10;
    public static final int HISTORY_LIMIT=99,MAX_SESSION_LAPS=99;
    public enum Mode { MANUAL, GPS }
    public enum State { IDLE, ARMED, RUNNING, STOPPED, ABORTED }
    public static final class Result {
        public final String circuitId;
        public final Mode mode;
        public final int lap;
        public final long elapsedMs;
        private final long[] splits;
        public Result(String id,Mode mode,int lap,long elapsed,long[] splitTimes){circuitId=id;this.mode=mode;this.lap=lap;elapsedMs=elapsed;splits=splitTimes.clone();}
        /** A whole-lap interval is not presented as a verified OEM sector. */
        public long[] sectorTimes(){return splits.length<=1?new long[0]:splits.clone();}
    }
    public static final class Snapshot {
        public final Mode mode;public final State state;public final LapCircuit circuit;
        public final String reason;public final long elapsedMs,bestMs,lastMs;
        public final int currentLap,nextSector,sectorCount;
        public final boolean gpsFresh,canArmGps;
        public final double latitude,longitude;public final float accuracyMetres;
        public final double gpsRadiusMetres;
        public final Result[] history;
        private Snapshot(Mode m,State s,LapCircuit c,String r,long elapsed,long best,long last,int lap,int next,int count,boolean fresh,boolean arm,double lat,double lon,float accuracy,double radius,Result[] records){
            mode=m;state=s;circuit=c;reason=r;elapsedMs=elapsed;bestMs=best;lastMs=last;currentLap=lap;nextSector=next;sectorCount=count;
            gpsFresh=fresh;canArmGps=arm;latitude=lat;longitude=lon;accuracyMetres=accuracy;gpsRadiusMetres=radius;history=records;
        }
        public boolean active(){return state==State.ARMED||state==State.RUNNING;}
    }
    private final LapCircuit[] catalog;
    private final double matchRadiusMetres,rearmDistanceMetres;
    private final ArrayList<Result> history=new ArrayList<Result>();
    private LapCircuit circuit;
    private Mode mode=Mode.MANUAL;private State state=State.IDLE;
    private String reason="";
    private boolean sourceWarning;
    private long revision,receivedAt=-1,fixAt=-1,boundary=-1,armedAt=-1;
    private double lat,lon,startedAt=-1,distanceSinceStart;
    private float accuracy;
    private boolean fixValid;
    private boolean goalLatched;
    private double hitLatitude,hitLongitude;
    private int completed,nextSector;
    private long elapsedMs;
    private final ArrayList<Long> splits=new ArrayList<Long>();
    public LapTimer(){this(new LapCircuit[0],10,20);}
    /** Explicit raw-GPS adaptation:10m proximity,20m rearm; the UI shows10m. */
    public LapTimer(LapCircuit[] circuits){this(circuits,10,20);}
    public LapTimer(LapCircuit[] circuits,double matchRadiusMetres,double rearmDistanceMetres){
        if(Double.isNaN(matchRadiusMetres)||Double.isInfinite(matchRadiusMetres)||matchRadiusMetres<0||matchRadiusMetres>50
            ||Double.isNaN(rearmDistanceMetres)||Double.isInfinite(rearmDistanceMetres)||rearmDistanceMetres<20||rearmDistanceMetres>200)throw new IllegalArgumentException("GPS criterion");
        this.matchRadiusMetres=matchRadiusMetres;this.rearmDistanceMetres=rearmDistanceMetres;
        catalog=circuits==null?new LapCircuit[0]:circuits.clone();
        for(LapCircuit c:catalog)if(c==null)throw new IllegalArgumentException("catalog entry");
        circuit=catalog.length==0?null:catalog[0];
        if(circuit!=null&&circuit.automaticReady()&&matchRadiusMetres>0)mode=Mode.GPS;
    }
    public LapCircuit[] circuits(){return catalog.clone();}
    private boolean active(){return state==State.ARMED||state==State.RUNNING;}
    public synchronized boolean mode(Mode value){
        if(active()||value==null)return false;
        if(mode!=value){mode=value;state=State.IDLE;reason="";sourceWarning=false;elapsedMs=0;revision++;}return true;
    }
    public synchronized boolean selectCircuit(String id){
        if(active())return false;
        LapCircuit found=null;for(LapCircuit c:catalog)if(c.id.equals(id))found=c;
        if(found==null)return false;
        if(circuit!=found){circuit=found;state=State.IDLE;reason="";sourceWarning=false;elapsedMs=0;revision++;}return true;
    }
    private String id(){return circuit==null?"manual":circuit.id;}
    private void resetSession(){completed=0;nextSector=0;splits.clear();distanceSinceStart=0;elapsedMs=0;goalLatched=false;}
    public synchronized boolean startManual(long now){
        if(active()||mode!=Mode.MANUAL||now<0)return false;
        resetSession();state=State.RUNNING;startedAt=now;reason="";sourceWarning=false;return true;
    }
    public synchronized boolean markLap(long now){
        if(mode!=Mode.MANUAL||state!=State.RUNNING)return false;
        if(now<startedAt){abort("랩 측정 시간이 역전되어 중단됨");return false;}
        if(now-startedAt<300)return false; // Reject a duplicated button release.
        if(now-startedAt>MAX_LAP_MS){abort("랩 측정 시간 제한에 도달함");return false;}
        finish(now);return true;
    }
    private boolean fresh(long now){return fixValid&&fixAt>=0&&now>=fixAt&&now-fixAt<=GPS_STALE_MS&&receivedAt<=now;}
    public synchronized boolean armGps(long now){
        expire(now);
        if(active()||mode!=Mode.GPS)return false;
        if(matchRadiusMetres<=0){reason="GPS 자동 기준이 설정되지 않았습니다";sourceWarning=false;return false;}
        if(circuit==null||!circuit.automaticReady()){reason="확인된 출발선 좌표가 없습니다";sourceWarning=false;return false;}
        if(!fresh(now)){reason="새 GPS 위치를 기다립니다";sourceWarning=true;return false;}
        if(LapCircuit.distance(lat,lon,circuit.start.latitude,circuit.start.longitude)>2000){reason="선택한 서킷의 출발선 근처에서 준비하세요";sourceWarning=false;return false;}
        resetSession();state=State.ARMED;startedAt=-1;armedAt=now;reason="";sourceWarning=false;return true;
    }
    public synchronized void onLocation(double latitude,double longitude,float accuracyMetres,long fixTime,long receiptTime){
        if(receiptTime<receivedAt||fixTime<fixAt||(boundary>=0&&(receiptTime<=boundary||fixTime<=boundary))){
            invalidate("GPS 수신 시간이 역전되어 중단됨");boundary=Math.max(boundary,Math.max(receivedAt,receiptTime));return;
        }
        boolean valid=LapCircuit.coordinate(latitude,longitude)&&!Float.isNaN(accuracyMetres)&&!Float.isInfinite(accuracyMetres)
            &&accuracyMetres>0&&accuracyMetres<=MAX_ACCURACY_METRES&&fixTime>=0&&receiptTime>=fixTime&&receiptTime-fixTime<=GPS_STALE_MS;
        if(!valid){invalidate("GPS 위치 또는 정확도가 유효하지 않습니다");receivedAt=Math.max(receivedAt,receiptTime);boundary=Math.max(boundary,receivedAt);return;}
        if(fixTime==fixAt||receiptTime==receivedAt)return;
        boolean continuous=fixValid&&fixAt>=0&&fixTime-fixAt<=GPS_STALE_MS&&receiptTime-receivedAt<=GPS_STALE_MS&&receivedAt>boundary;
        double step=continuous?LapCircuit.distance(lat,lon,latitude,longitude):0;
        if(continuous&&step*1000/(fixTime-fixAt)>100){invalidate("GPS 위치가 급변하여 중단됨");receivedAt=receiptTime;boundary=Math.max(boundary,receivedAt);return;}
        if(mode==Mode.GPS&&active()&&!continuous)abort("GPS 수신이 끊겨 랩 측정을 중단함");
        if(mode==Mode.GPS&&active()&&continuous){
            double speed=step*1000/(fixTime-fixAt);
            // Independent raw-GPS adaptation of the native point-to-movement-
            // segment criterion. Source points are not fabricated gate endpoints.
            // OEM map snapping/link matching/GNSS-DR are not reproduced here.
            boolean moving=speed>=2&&step>=Math.max(2,Math.min(accuracy,accuracyMetres)*0.5);
            if(state==State.ARMED&&moving&&fixTime>armedAt){
                double crossing=circuit.start.proximity(lat,lon,latitude,longitude,matchRadiusMetres);
                if(!Double.isNaN(crossing)){
                    startedAt=Math.max(armedAt,fixAt+(fixTime-fixAt)*crossing);state=State.RUNNING;
                    distanceSinceStart=step*(1-crossing);nextSector=0;splits.clear();
                    goalLatched=true;hitLatitude=latitude;hitLongitude=longitude;
                }
            }else if(state==State.RUNNING){
                distanceSinceStart+=step;
                boolean wasLatched=goalLatched;
                if(goalLatched&&LapCircuit.distance(hitLatitude,hitLongitude,latitude,longitude)>rearmDistanceMetres)goalLatched=false;
                // A segment leaving the last hit must only rearm. Its first
                // endpoint may still be on the goal; it is not a new passage.
                if(moving&&!wasLatched&&!goalLatched){
                    double crossing=circuit.goal.proximity(lat,lon,latitude,longitude,matchRadiusMetres);
                    double at=fixAt+(fixTime-fixAt)*crossing;
                    // Additional app guard against a nearby goal being treated
                    // as a completed full lap without traversing the course.
                    double minimumDistance=circuit.lengthMetres>0?Math.max(200,circuit.lengthMetres*0.8):200;
                    if(!Double.isNaN(crossing)){
                        goalLatched=true;hitLatitude=latitude;hitLongitude=longitude;
                        if(at-startedAt>=MIN_GPS_LAP_MS&&distanceSinceStart>=minimumDistance)finish(at);
                    }
                }
            }
        }
        lat=latitude;lon=longitude;accuracy=accuracyMetres;fixAt=fixTime;receivedAt=receiptTime;fixValid=true;
        // Recovery clears only a source warning in an inactive display. A
        // completed interruption or an unrelated refusal remains reviewable.
        if(sourceWarning&&(state==State.IDLE||state==State.STOPPED)){reason="";sourceWarning=false;}
        if(mode==Mode.GPS&&state==State.RUNNING&&fixTime-startedAt>MAX_LAP_MS)abort("랩 측정 시간 제한에 도달함");
    }
    private void finish(double at){
        long duration=Math.round(at-startedAt);long[] times=new long[splits.size()+1];long previous=0;
        for(int i=0;i<splits.size();i++){times[i]=splits.get(i)-previous;previous=splits.get(i);}
        times[times.length-1]=duration-previous;
        history.add(0,new Result(id(),mode,++completed,duration,times));
        while(history.size()>HISTORY_LIMIT)history.remove(history.size()-1);
        startedAt=at;elapsedMs=0;distanceSinceStart=0;nextSector=0;splits.clear();revision++;
        if(completed>=MAX_SESSION_LAPS){state=State.STOPPED;elapsedMs=duration;startedAt=-1;reason="99랩 측정을 마쳤습니다";}
    }
    private void abort(String why){if(active()){state=State.ABORTED;reason=why;sourceWarning=false;startedAt=-1;elapsedMs=0;}}
    private void invalidate(String why){fixValid=false;if(mode==Mode.GPS)abort(why);if(!active()){reason=why;sourceWarning=true;}}
    public synchronized void gpsUnavailable(String why,long now){invalidate(why);boundary=Math.max(boundary,Math.max(now,receivedAt));}
    public synchronized void gap(long now){
        if(active())abort(mode==Mode.GPS?"GPS 수신이 끊겨 랩 측정을 중단함":"랩 측정을 중단했습니다");
        fixValid=false;boundary=Math.max(boundary,Math.max(now,receivedAt));
    }
    public synchronized void stop(long now){
        if(!active())return;
        if(state==State.RUNNING){if(now<startedAt){abort("랩 측정 시간이 역전되어 중단됨");return;}elapsedMs=Math.min(MAX_LAP_MS,Math.round(now-startedAt));}
        state=State.STOPPED;reason="";sourceWarning=false;startedAt=-1;
    }
    private void expire(long now){
        if(mode==Mode.GPS&&active()&&fixAt>=0&&now>=fixAt&&now-fixAt>GPS_STALE_MS){invalidate("GPS 수신이 끊겨 랩 측정을 중단함");boundary=Math.max(boundary,now);}
        if(state==State.RUNNING&&now>=startedAt&&now-startedAt>MAX_LAP_MS)abort("랩 측정 시간 제한에 도달함");
    }
    public synchronized Snapshot snapshot(long now){
        expire(now);boolean fresh=fresh(now);long best=0,last=0;
        ArrayList<Result> selected=new ArrayList<Result>();
        for(Result r:history)if(r.circuitId.equals(id())&&r.mode==mode){selected.add(r);if(last==0)last=r.elapsedMs;if(best==0||r.elapsedMs<best)best=r.elapsedMs;}
        long elapsed=state==State.RUNNING?Math.max(0,Math.round(now-startedAt)):elapsedMs;
        boolean canArm=!active()&&matchRadiusMetres>0&&fresh&&circuit!=null&&circuit.automaticReady()&&LapCircuit.distance(lat,lon,circuit.start.latitude,circuit.start.longitude)<=2000;
        return new Snapshot(mode,state,circuit,reason,elapsed,best,last,Math.min(MAX_SESSION_LAPS,completed+1),nextSector,0,
            fresh,canArm,lat,lon,accuracy,matchRadiusMetres,selected.toArray(new Result[selected.size()]));
    }
    public synchronized void clearHistory(){if(!active()){history.clear();revision++;}}
    public synchronized long revision(){return revision;}
    public synchronized String save(){
        StringBuilder s=new StringBuilder("1|").append(mode.name()).append('|').append(id());
        for(Result r:history){s.append('|').append(r.circuitId).append(',').append(r.mode.name()).append(',').append(r.lap).append(',').append(r.elapsedMs);
            for(long split:r.splits)s.append(',').append(split);}
        return s.toString();
    }
    public synchronized void restore(String saved){
        if(active()||saved==null||saved.length()>100000)return;
        String[] parts=saved.split("\\|",-1);if(parts.length<3||!"1".equals(parts[0]))return;
        Mode selectedMode;try{selectedMode=Mode.valueOf(parts[1]);}catch(IllegalArgumentException e){return;}
        LapCircuit selected=null;for(LapCircuit c:catalog)if(c.id.equals(parts[2]))selected=c;
        if(selected==null&&!"manual".equals(parts[2]))return;
        ArrayList<Result> records=new ArrayList<Result>();
        for(int i=3;i<parts.length&&records.size()<HISTORY_LIMIT;i++){
            String[] f=parts[i].split(",",-1);if(f.length<5||f.length>37||!f[0].matches("[A-Za-z0-9_-]{1,80}"))continue;
            try{
                Mode m=Mode.valueOf(f[1]);int lap=Integer.parseInt(f[2]);long duration=Long.parseLong(f[3]);
                if(lap<1||lap>1000000||duration<=0||duration>MAX_LAP_MS)continue;
                long[] times=new long[f.length-4];long sum=0;boolean ok=true;
                for(int j=4;j<f.length;j++){long split=Long.parseLong(f[j]);if(split<=0||split>duration)ok=false;times[j-4]=split;sum+=split;}
                if(ok&&sum==duration)records.add(new Result(f[0],m,lap,duration,times));
            }catch(IllegalArgumentException e){/* Invalid stored record cannot become a new lap. */}
        }
        circuit=selected;mode=selectedMode;history.clear();history.addAll(records);state=State.IDLE;reason="";sourceWarning=false;startedAt=-1;elapsedMs=0;completed=0;revision++;
    }
    public static String formatTime(long ms){long cs=Math.max(0,ms)/10;return String.format(Locale.US,"%02d:%02d.%02d",cs/6000,cs/100%60,cs%100);}
}
