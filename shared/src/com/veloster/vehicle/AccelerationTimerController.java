package com.veloster.vehicle;

import android.content.Context;
import android.content.SharedPreferences;

/** UI actions and durable successful results; no vehicle or GPS output API. */
public final class AccelerationTimerController {
    private final AccelerationTimer timer;
    private final SharedPreferences preferences;
    private final AccelerationTimerRenderer renderer=new AccelerationTimerRenderer();
    private long savedRevision;
    public AccelerationTimerController(Context context,AccelerationTimer model){
        timer=model;preferences=context.getApplicationContext().getSharedPreferences("pd_acceleration_timer",0);
        try{timer.restore(preferences.getString("records_v1",null));}catch(ClassCastException e){/* Incompatible preference: keep defaults. */}
        savedRevision=timer.revision();
    }
    public AccelerationTimerRenderer renderer(){return renderer;}
    public synchronized void onSpeed(float kmh,boolean valid,long receiptAt){timer.onSpeed(kmh,valid,receiptAt);saveIfChanged();}
    public synchronized void gap(long now){timer.gap(now);}
    private void saveIfChanged(){
        long revision=timer.revision();
        if(revision!=savedRevision){preferences.edit().putString("records_v1",timer.save()).apply();savedRevision=revision;}
    }
    /** Parent passes a stable tap transformed to logical 800x480 coordinates. */
    public synchronized boolean touch(float x,float y,long now){
        int hit=AccelerationTimerRenderer.hit(x,y);
        if(hit==AccelerationTimerRenderer.NONE)return false;
        AccelerationTimer.Snapshot s=timer.snapshot(now);
        if(hit==AccelerationTimerRenderer.TOGGLE){if(s.active())timer.cancel();else timer.arm(now);}
        else if(hit==AccelerationTimerRenderer.HISTORY_PAGE)renderer.toggleHistoryPage();
        else if(!s.active()){
            int start=s.startKmh,target=s.targetKmh;
            if(hit==AccelerationTimerRenderer.START_UP)start+=10;
            else if(hit==AccelerationTimerRenderer.START_DOWN)start-=10;
            else if(hit==AccelerationTimerRenderer.TARGET_UP)target+=10;
            else if(hit==AccelerationTimerRenderer.TARGET_DOWN)target-=10;
            else if(hit==AccelerationTimerRenderer.HISTORY_RESET){timer.clearHistory();renderer.resetHistoryPage();}
            if(hit!=AccelerationTimerRenderer.HISTORY_RESET)timer.configure(start,target);
        }
        saveIfChanged();return true;
    }
}
