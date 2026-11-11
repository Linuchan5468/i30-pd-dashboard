package com.veloster.vehicle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import com.veloster.i18n.UiText;
import java.util.Locale;

/** Original PD dashboard drawing, with CN7 N's three-column interaction arrangement. */
public final class AccelerationTimerRenderer {
    public static final int NONE=0,TOGGLE=1,START_UP=2,START_DOWN=3,TARGET_UP=4,TARGET_DOWN=5,HISTORY_RESET=6,HISTORY_PAGE=7;
    private static final int BLUE=0xffa4d9ef,WHITE=0xffe1edf2,MUTED=0xff91a5ae,LINE=0xff303e46,PANEL=0xff101920,RED=0xffff5065;
    private final Paint paint=new Paint(3);
    private int historyPage;
    public void toggleHistoryPage(){historyPage=historyPage==0?1:0;}
    public void resetHistoryPage(){historyPage=0;}
    /** Coordinates are in the parent's 800x480 logical canvas. */
    public static int hit(float x,float y){
        if(x>=282&&x<548&&y>=92&&y<328)return TOGGLE;
        if(x>=564&&x<668&&y>=128&&y<180)return START_UP;
        if(x>=564&&x<668&&y>=264&&y<316)return START_DOWN;
        if(x>=678&&x<782&&y>=128&&y<180)return TARGET_UP;
        if(x>=678&&x<782&&y>=264&&y<316)return TARGET_DOWN;
        if(x>=18&&x<268&&y>=402&&y<442)return HISTORY_PAGE;
        if(x>=564&&x<782&&y>=350&&y<394)return HISTORY_RESET;
        return NONE;
    }
    public void draw(Canvas c,AccelerationTimer.Snapshot s){
        text(c,UiText.t("가속 측정 기록"),18,78,18,BLUE,true);
        text(c,UiText.t("시작 / 취소"),282,78,18,BLUE,true);
        text(c,"km/h",722,78,16,MUTED,false);
        panel(c,18,92,268,392,PANEL,LINE);
        int first=historyPage*5;
        if(s.history.length==0)text(c,UiText.t("측정 기록 없음"),40,135,18,MUTED,false);
        for(int i=0;i<5&&first+i<s.history.length;i++){
            AccelerationTimer.Result r=s.history[first+i];float y=126+i*55;
            text(c,r.startKmh+"–"+r.targetKmh+" km/h",32,y,17,WHITE,false);
            text(c,AccelerationTimer.formatTime(r.elapsedMs),139,y+25,22,BLUE,true);
            if(i<4)line(c,30,y+38,256,y+38,LINE);
        }
        button(c,UiText.t("기록 페이지")+" "+(historyPage+1)+" / 2",18,402,268,442,false,true);
        panel(c,282,92,548,328,PANEL,s.active()?BLUE:LINE);
        String state=s.state==AccelerationTimer.State.ARMED?"시작 속도 대기":s.state==AccelerationTimer.State.RUNNING?"측정 중":
            s.state==AccelerationTimer.State.COMPLETE?"측정 완료":s.state==AccelerationTimer.State.CANCELLED?"취소됨":
            s.state==AccelerationTimer.State.ABORTED?"측정 중단":"준비";
        centered(c,AccelerationTimer.formatTime(s.elapsedMs),415,176,44,WHITE,true);
        centeredFit(c,UiText.t(state),415,221,22,244,s.state==AccelerationTimer.State.ABORTED?RED:BLUE,true);
        centeredFit(c,s.active()?UiText.t("터치하여 취소"):UiText.t("터치하여 준비"),415,258,17,244,s.active()||s.canArm?WHITE:MUTED,false);
        centeredFit(c,s.speedFresh?String.format(Locale.US,"%.1f km/h",s.speedKmh):UiText.t("속도 신호 없음"),415,303,18,244,MUTED,false);
        selector(c,564,668,UiText.t("시작 속도"),s.startKmh,!s.active(),s.startKmh<Math.min(190,s.targetKmh-10),s.startKmh>0);
        selector(c,678,782,UiText.t("목표 속도"),s.targetKmh,!s.active(),s.targetKmh<200,s.targetKmh>s.startKmh+10);
        button(c,UiText.t("기록 초기화"),564,350,782,394,false,!s.active());
        fitted(c,UiText.t("준비 후 시작 속도를 넘으면 측정합니다."),282,351,15,266,MUTED,false);
        fitted(c,UiText.t("0 km/h 설정은 1 km/h부터 측정합니다."),282,374,14,266,MUTED,false);
        String reason=s.reason.length()>0?UiText.t(s.reason):UiText.t("AVN 수신 속도 기반의 참고용 측정값");
        fitted(c,reason,282,416,14,500,s.reason.length()>0?RED:MUTED,false);
        fitted(c,UiText.t("안전한 장소에서 준비한 후 전방에 집중하세요."),18,465,16,764,MUTED,false);
    }
    private void selector(Canvas c,float left,float right,String title,int value,boolean editable,boolean up,boolean down){
        panel(c,left,92,right,328,PANEL,LINE);centeredFit(c,title,(left+right)/2,117,16,right-left-8,BLUE,false);
        button(c,"+",left+4,128,right-4,180,false,editable&&up);
        centered(c,Integer.toString(value),(left+right)/2,237,42,WHITE,true);
        button(c,"−",left+4,264,right-4,316,false,editable&&down);
    }
    private void panel(Canvas c,float l,float t,float r,float b,int fill,int edge){
        paint.setStyle(Paint.Style.FILL);paint.setColor(fill);c.drawRoundRect(new RectF(l,t,r,b),7,7,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(edge);c.drawRoundRect(new RectF(l,t,r,b),7,7,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void button(Canvas c,String label,float l,float t,float r,float b,boolean active,boolean enabled){
        panel(c,l,t,r,b,active?0xff294653:0xff17242c,enabled?LINE:0xff202b30);
        centered(c,label,(l+r)/2,(t+b)/2+7,20,enabled?BLUE:0xff51616a,false);
    }
    private void text(Canvas c,String label,float x,float y,float size,int color,boolean bold){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(size);c.drawText(label,x,y,paint);
    }
    private void centered(Canvas c,String label,float x,float y,float size,int color,boolean bold){
        paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);paint.setTextSize(size);
        text(c,label,x-paint.measureText(label)/2,y,size,color,bold);
    }
    private void fitted(Canvas c,String label,float x,float y,float size,float width,int color,boolean bold){
        paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);paint.setTextSize(size);
        float measured=paint.measureText(label);text(c,label,x,y,measured>width?size*width/measured:size,color,bold);
    }
    private void centeredFit(Canvas c,String label,float x,float y,float size,float width,int color,boolean bold){
        paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);paint.setTextSize(size);
        float measured=paint.measureText(label);centered(c,label,x,y,measured>width?size*width/measured:size,color,bold);
    }
    private void line(Canvas c,float x,float y,float xx,float yy,int color){paint.setColor(color);paint.setStrokeWidth(1);c.drawLine(x,y,xx,yy,paint);}
}
