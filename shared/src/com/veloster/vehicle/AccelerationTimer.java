package com.veloster.vehicle;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Read-only acceleration measurement from decoded CLU11 speed receipts in km/h.
 * Receipt times must use one monotonic clock. Painting never supplies a speed.
 * This is an estimate from AVN reception, not a calibrated performance instrument.
 */
public final class AccelerationTimer {
    public static final int STEP_KMH=10,MAX_START_KMH=190,MAX_TARGET_KMH=200,HISTORY_LIMIT=10;
    /** App policy: a missing second of source data invalidates a pending run. */
    public static final long MAX_GAP_MS=1000,MAX_RUN_MS=600000;
    public enum State { IDLE, ARMED, RUNNING, COMPLETE, CANCELLED, ABORTED }
    public static final class Result {
        public final int startKmh,targetKmh;
        public final long elapsedMs;
        public Result(int start,int target,long elapsed){startKmh=start;targetKmh=target;elapsedMs=elapsed;}
    }
    public static final class Snapshot {
        public final int startKmh,targetKmh;
        public final State state;
        /** Translation key for the last refusal or interruption. */
        public final String reason;
        public final long elapsedMs;
        public final float speedKmh;
        public final boolean speedFresh,canArm;
        public final Result[] history;
        private Snapshot(int start,int target,State s,String r,long elapsed,float speed,boolean fresh,boolean arm,Result[] records){
            startKmh=start;targetKmh=target;state=s;reason=r;elapsedMs=elapsed;
            speedKmh=speed;speedFresh=fresh;canArm=arm;history=records;
        }
        public boolean active(){return state==State.ARMED||state==State.RUNNING;}
    }
    private final ArrayList<Result> history=new ArrayList<Result>();
    private int startKmh=0,targetKmh=100;
    private State state=State.IDLE;
    private String reason="";
    private float speed;
    private long receivedAt=-1,armedAt=-1,lastBoundary=-1;
    private boolean speedValid;
    private double startedAt=-1;
    private long elapsedMs,revision;

    public static boolean validRange(int start,int target){
        return start>=0&&start<=MAX_START_KMH&&target>=STEP_KMH&&target<=MAX_TARGET_KMH
            &&start%STEP_KMH==0&&target%STEP_KMH==0&&target>=start+STEP_KMH;
    }
    public synchronized boolean configure(int start,int target){
        if(active()||!validRange(start,target))return false;
        if(start!=startKmh||target!=targetKmh){startKmh=start;targetKmh=target;state=State.IDLE;elapsedMs=0;reason="";revision++;}
        return true;
    }
    private boolean active(){return state==State.ARMED||state==State.RUNNING;}
    private boolean fresh(long now){return speedValid&&receivedAt>=0&&now>=receivedAt&&now-receivedAt<=MAX_GAP_MS;}
    public synchronized boolean arm(long now){
        expire(now);
        if(active())return false;
        if(!fresh(now)){reason="속도 신호를 기다립니다";return false;}
        if(speed>startKmh){reason="시작 속도 이하에서 준비하세요";return false;}
        state=State.ARMED;reason="";armedAt=now;startedAt=-1;elapsedMs=0;
        return true;
    }
    public synchronized void cancel(){if(active()){state=State.CANCELLED;reason="";startedAt=-1;elapsedMs=0;}}
    private void abort(String why){if(active()){state=State.ABORTED;reason=why;startedAt=-1;elapsedMs=0;}}
    /** Call only on a genuinely new admitted PD CLU11 receipt, including invalid ones. */
    public synchronized void onSpeed(float kmh,boolean valid,long at){
        boolean ok=valid&&!Float.isNaN(kmh)&&!Float.isInfinite(kmh)&&kmh>=0&&kmh<=300&&at>=0;
        if(at<receivedAt||(lastBoundary>=0&&at<=lastBoundary)){
            abort("속도 수신 시간이 역전되어 중단됨");speedValid=false;return;
        }
        // Several HSCAN frames can share a socket-read timestamp. They provide
        // no extra timing resolution. Never complete a run from zero-time pairs.
        if(at==receivedAt){if(!ok){abort("속도 신호가 유효하지 않아 중단됨");speedValid=false;}return;}
        if(!ok){
            abort("속도 신호가 유효하지 않아 중단됨");speedValid=false;
            receivedAt=Math.max(receivedAt,at);return;
        }
        boolean connected=speedValid&&receivedAt>=0&&at-receivedAt<=MAX_GAP_MS&&receivedAt>lastBoundary;
        if(active()&&!connected){abort("속도 수신이 끊겨 중단됨");}
        float previous=speed;long previousAt=receivedAt;
        if(state==State.ARMED&&connected&&at>armedAt){
            // CN7 N's speed state path uses >=1 km/h for its 0-start, and
            // >selected start otherwise. A direct accelerator-pedal gate is
            // not present in that path. Retain that speed-based behavior.
            float threshold=startKmh==0?1f:startKmh;
            boolean reached=startKmh==0?kmh>=threshold:kmh>threshold;
            if(reached&&previous<=threshold&&kmh>previous){
                startedAt=Math.max(armedAt,crossing(previous,kmh,previousAt,at,threshold));
                state=State.RUNNING;
            }
        }
        if(state==State.RUNNING){
            if(at-startedAt>MAX_RUN_MS)abort("측정 시간 제한에 도달함");
            else if(kmh>=targetKmh&&connected&&kmh>previous){
                double finished=crossing(previous,kmh,previousAt,at,targetKmh);
                long duration=Math.round(finished-startedAt);
                if(duration>0){
                    elapsedMs=duration;state=State.COMPLETE;reason="";
                    history.add(0,new Result(startKmh,targetKmh,duration));
                    while(history.size()>HISTORY_LIMIT)history.remove(history.size()-1);
                    revision++;
                }else abort("속도 수신 간격으로 측정할 수 없음");
            }
        }
        speed=kmh;receivedAt=at;speedValid=true;
    }
    /** Linear crossing estimate between two real receipts; no missing data is filled. */
    private static double crossing(float a,float b,long ta,long tb,float threshold){
        double fraction=Math.max(0,Math.min(1,(threshold-a)/(double)(b-a)));
        return ta+(tb-ta)*fraction;
    }
    /** Queue gaps, disconnect, ignition transition and background loss use this API. */
    public synchronized void gap(long now){
        abort("속도 수신이 끊겨 중단됨");speedValid=false;
        lastBoundary=Math.max(lastBoundary,Math.max(now,receivedAt));
    }
    private void expire(long now){
        // A UI frame captured just before a concurrent receipt is not a clock reset.
        if(active()&&receivedAt>=0&&now>=receivedAt&&now-receivedAt>MAX_GAP_MS){abort("속도 수신이 끊겨 중단됨");speedValid=false;}
        if(state==State.RUNNING&&now>=startedAt&&now-startedAt>MAX_RUN_MS)abort("측정 시간 제한에 도달함");
    }
    public synchronized Snapshot snapshot(long now){
        expire(now);
        long elapsed=state==State.RUNNING?Math.max(0,Math.round(now-startedAt)):elapsedMs;
        boolean fresh=fresh(now);
        return new Snapshot(startKmh,targetKmh,state,reason,elapsed,speed,fresh,!active()&&fresh&&speed<=startKmh,
            history.toArray(new Result[history.size()]));
    }
    public synchronized void clearHistory(){history.clear();revision++;}
    public synchronized long revision(){return revision;}
    /** Preferences contain only configuration and the last ten successful results. */
    public synchronized String save(){
        StringBuilder s=new StringBuilder("1|").append(startKmh).append('|').append(targetKmh);
        for(Result r:history)s.append('|').append(r.startKmh).append(',').append(r.targetKmh).append(',').append(r.elapsedMs);
        return s.toString();
    }
    /** A saved run can never arm itself after process restart. Invalid records are skipped. */
    public synchronized void restore(String saved){
        if(active())return;
        if(saved==null||saved.length()>2048)return;
        String[] parts=saved.split("\\|",-1);
        if(parts.length<3||!"1".equals(parts[0]))return;
        int from,to;
        try{from=Integer.parseInt(parts[1]);to=Integer.parseInt(parts[2]);}catch(NumberFormatException e){return;}
        if(!validRange(from,to))return;
        ArrayList<Result> records=new ArrayList<Result>();
        for(int i=3;i<parts.length&&records.size()<HISTORY_LIMIT;i++){
            String[] fields=parts[i].split(",",-1);
            if(fields.length!=3)continue;
            try{
                int a=Integer.parseInt(fields[0]),b=Integer.parseInt(fields[1]);long duration=Long.parseLong(fields[2]);
                if(validRange(a,b)&&duration>0&&duration<=MAX_RUN_MS)records.add(new Result(a,b,duration));
            }catch(NumberFormatException e){/* An incompatible record cannot become a measurement. */}
        }
        startKmh=from;targetKmh=to;history.clear();history.addAll(records);
        state=State.IDLE;reason="";elapsedMs=0;startedAt=-1;armedAt=-1;revision++;
    }
    public static String formatTime(long ms){
        long centiseconds=Math.max(0,ms)/10;
        return String.format(Locale.US,"%02d:%02d.%02d",centiseconds/6000,(centiseconds/100)%60,centiseconds%100);
    }
}
