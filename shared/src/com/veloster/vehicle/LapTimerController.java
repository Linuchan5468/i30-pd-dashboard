package com.veloster.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.reflect.Method;

/** Ordinary Android GPS subscription and local preferences; no navigation/CAN output. */
public final class LapTimerController implements LocationListener {
    private final LapTimer timer;
    private final SharedPreferences preferences;
    private final LapTimerRenderer renderer=new LapTimerRenderer();
    private final CircuitPicker picker;
    private final LocationManager locations;
    private Method elapsedNanos;
    private boolean listening;
    private long savedRevision;
    public LapTimerController(Context context,LapTimer model){
        Context app=context.getApplicationContext();timer=model;
        picker=new CircuitPicker(timer.circuits());renderer.setPicker(picker);
        preferences=app.getSharedPreferences("pd_lap_timer",0);
        try{timer.restore(preferences.getString("records_v1",null));}catch(ClassCastException e){/* Keep valid defaults. */}
        savedRevision=timer.revision();locations=(LocationManager)app.getSystemService(Context.LOCATION_SERVICE);
        // The public API17 method is present on the PD firmware. Reflection is
        // only for the repository's API16 compilation jar, not a hidden API.
        try{elapsedNanos=Location.class.getMethod("getElapsedRealtimeNanos");}catch(Exception unavailable){elapsedNanos=null;}
    }
    public LapTimerRenderer renderer(){return renderer;}
    public synchronized boolean back(){if(!picker.isOpen())return false;picker.close();return true;}
    public synchronized LapTimer.Snapshot snapshot(long now){saveIfChanged();return timer.snapshot(now);}
    public synchronized void startListening(){
        if(listening)return;
        long now=SystemClock.elapsedRealtime();
        if(locations==null){timer.gpsUnavailable("GPS 제공자를 찾을 수 없습니다",now);return;}
        if(elapsedNanos==null){timer.gpsUnavailable("GPS 단조 증가 시각 API를 사용할 수 없습니다",now);return;}
        try{
            if(!locations.getAllProviders().contains(LocationManager.GPS_PROVIDER)){timer.gpsUnavailable("이 시스템에 GPS 제공자가 없습니다",now);return;}
            if(!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)){timer.gpsUnavailable("시스템 GPS 위치 설정을 켜 주세요",now);return;}
            locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,200,0,this,Looper.getMainLooper());listening=true;
            // Cached locations are intentionally not admitted as live fixes.
        }catch(SecurityException denied){timer.gpsUnavailable("정확한 위치 권한이 필요합니다",now);}
        catch(RuntimeException unavailable){timer.gpsUnavailable("GPS 위치 수신을 시작할 수 없습니다",now);}
    }
    public synchronized void stopListening(long now){
        if(locations!=null&&listening)try{locations.removeUpdates(this);}catch(RuntimeException ignored){/* Subscription already ended. */}
        listening=false;timer.gap(now);saveIfChanged();
    }
    public synchronized void onLocationChanged(Location location){
        if(!listening||location==null)return;
        long now=SystemClock.elapsedRealtime();
        try{
            long nanos=((Long)elapsedNanos.invoke(location)).longValue();long at=nanos/1000000L;
            // Timestamp0 is a default/unset field for Android Location. It is
            // not proof of a new hardware fix even near process startup.
            if(nanos<=0){timer.gpsUnavailable("GPS 위치 시각이 유효하지 않습니다",now);return;}
            timer.onLocation(location.getLatitude(),location.getLongitude(),location.hasAccuracy()?location.getAccuracy():Float.NaN,at,now);
            saveIfChanged();
        }catch(Exception invalid){timer.gpsUnavailable("GPS 위치 시각을 읽을 수 없습니다",now);}
    }
    public synchronized void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider))timer.gpsUnavailable("시스템 GPS 위치 설정이 꺼졌습니다",SystemClock.elapsedRealtime());}
    public synchronized void onProviderEnabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider)&&!listening)startListening();}
    public synchronized void onStatusChanged(String provider,int status,Bundle extras){
        if(LocationManager.GPS_PROVIDER.equals(provider)&&status!=2)timer.gpsUnavailable("GPS 위성 위치를 기다립니다",SystemClock.elapsedRealtime());
    }
    private void saveIfChanged(){long revision=timer.revision();if(revision!=savedRevision){preferences.edit().putString("records_v1",timer.save()).apply();savedRevision=revision;}}
    /** The parent passes stable taps in logical 800x480 coordinates, y>=92. */
    public synchronized boolean touch(float x,float y,long now){
        if(picker.isOpen()){
            if(timer.snapshot(now).active())return true;
            int action=picker.touch(x,y);
            if(action==CircuitPicker.SELECTED&&picker.selected()!=null){timer.selectCircuit(picker.selected().id);renderer.resetPage();saveIfChanged();}
            return true;
        }
        int hit=LapTimerRenderer.hit(x,y);if(hit==LapTimerRenderer.NONE)return false;
        LapTimer.Snapshot s=timer.snapshot(now);
        if(hit==LapTimerRenderer.STOP)timer.stop(now);
        else if(hit==LapTimerRenderer.START){if(s.mode==LapTimer.Mode.MANUAL){if(s.state==LapTimer.State.RUNNING)timer.markLap(now);else timer.startManual(now);}else timer.armGps(now);}
        else if(hit==LapTimerRenderer.HISTORY_PAGE)renderer.nextPage(s.history.length);
        else if(!s.active()){
            if(hit==LapTimerRenderer.MODE_MANUAL)timer.mode(LapTimer.Mode.MANUAL);
            else if(hit==LapTimerRenderer.MODE_GPS){timer.mode(LapTimer.Mode.GPS);startListening();}
            else if(hit==LapTimerRenderer.TRACK_NEXT){
                picker.open(s.circuit==null?null:s.circuit.id);
            }else if(hit==LapTimerRenderer.HISTORY_RESET){timer.clearHistory();renderer.resetPage();}
        }
        saveIfChanged();return true;
    }
}
