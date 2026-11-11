package com.veloster.vehicle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import com.veloster.i18n.UiText;
import java.util.Locale;

/** PD artwork and interactions written independently; source coordinates stay factual. */
public final class LapTimerRenderer {
    public static final int NONE=0,START=1,STOP=2,MODE_MANUAL=3,MODE_GPS=4,TRACK_NEXT=5,HISTORY_PAGE=6,HISTORY_RESET=7;
    private static final int BLUE=0xffa4d9ef,WHITE=0xffe1edf2,MUTED=0xff91a5ae,LINE=0xff303e46,PANEL=0xff101920,RED=0xffff5065;
    private final Paint p=new Paint(3);
    private final Path dropdownArrow=new Path();
    private final CircuitMapRenderer circuitMap=new CircuitMapRenderer(),pickerMap=new CircuitMapRenderer();
    private CircuitPicker picker;
    private int page;
    public void resetPage(){page=0;}
    public void setPicker(CircuitPicker value){picker=value;}
    public void nextPage(int count){page=(page+1)%Math.max(1,(count+3)/4);}
    public static int hit(float x,float y){
        if(x>=18&&x<145&&y>=96&&y<134)return MODE_MANUAL;
        if(x>=151&&x<278&&y>=96&&y<134)return MODE_GPS;
        if(x>=290&&x<782&&y>=96&&y<134)return TRACK_NEXT;
        if(x>=290&&x<530&&y>=350&&y<405)return START;
        if(x>=542&&x<782&&y>=350&&y<405)return STOP;
        if(x>=18&&x<143&&y>=398&&y<437)return HISTORY_PAGE;
        if(x>=151&&x<278&&y>=398&&y<437)return HISTORY_RESET;
        return NONE;
    }
    public void draw(Canvas c,LapTimer.Snapshot s){
        if(picker!=null&&picker.isOpen()){drawPicker(c,s);return;}
        text(c,UiText.t("랩 타이머"),18,78,18,BLUE,true);
        button(c,UiText.t("수동 랩"),18,96,145,134,s.mode==LapTimer.Mode.MANUAL,!s.active());
        button(c,UiText.t("GPS 추정")+" · "+String.format(Locale.US,"%.0fm",s.gpsRadiusMetres),151,96,278,134,s.mode==LapTimer.Mode.GPS,!s.active());
        dropdown(c,s.circuit==null?UiText.t("확인된 서킷 정보 없음"):s.circuit.displayName(),290,96,782,134,!s.active());
        panel(c,18,144,278,390,PANEL,LINE);
        text(c,UiText.t("랩 기록"),30,168,17,BLUE,true);
        int first=page*4;
        if(s.history.length==0)text(c,UiText.t("측정 기록 없음"),33,211,18,MUTED,false);
        for(int i=0;i<4&&first+i<s.history.length;i++){
            LapTimer.Result r=s.history[first+i];float y=204+i*49;
            text(c,"LAP "+r.lap,32,y,17,WHITE,false);
            fitted(c,LapTimer.formatTime(r.elapsedMs),128,y,24,137,r.elapsedMs==s.bestMs?BLUE:WHITE,true);
            if(i<3)line(c,30,y+15,266,y+15,LINE);
        }
        button(c,UiText.t("기록 페이지")+" "+(page+1),18,398,143,437,false,true);
        button(c,UiText.t("기록 초기화"),151,398,278,437,false,!s.active());
        panel(c,290,144,570,338,PANEL,s.active()?BLUE:LINE);
        String status=s.state==LapTimer.State.RUNNING?"랩 측정 중":s.state==LapTimer.State.ARMED?"출발선 통과 대기":s.state==LapTimer.State.STOPPED?"랩 측정 종료":s.state==LapTimer.State.ABORTED?"랩 측정 중단":"준비";
        centered(c,"LAP "+s.currentLap,430,176,21,BLUE,true,260);
        centered(c,LapTimer.formatTime(s.elapsedMs),430,224,42,WHITE,true,260);
        centered(c,UiText.t(status),430,259,20,s.state==LapTimer.State.ABORTED?RED:BLUE,true,260);
        fitted(c,UiText.t("최고")+"  "+(s.bestMs>0?LapTimer.formatTime(s.bestMs):"—"),306,292,18,250,BLUE,false);
        fitted(c,UiText.t("직전")+"  "+(s.lastMs>0?LapTimer.formatTime(s.lastMs):"—"),306,321,18,250,MUTED,false);
        drawCircuit(c,s);
        String start=s.mode==LapTimer.Mode.MANUAL?(s.state==LapTimer.State.RUNNING?"랩 기록":"시작"):(s.state==LapTimer.State.RUNNING?"자동 측정 중":s.state==LapTimer.State.ARMED?"출발선 대기 중":"GPS 측정 준비");
        button(c,UiText.t(start),290,350,530,405,s.active(),s.mode==LapTimer.Mode.MANUAL||s.canArmGps);
        button(c,UiText.t("종료"),542,350,782,405,false,s.active());
        String reason=s.reason.length()>0?UiText.t(s.reason):s.mode==LapTimer.Mode.MANUAL?UiText.t("랩 버튼으로 구간을 기록합니다."):UiText.t("원본 기준점 기반 GPS 추정 · 순정 맵 매칭과 다름");
        fitted(c,reason,290,432,15,492,s.reason.length()>0?RED:MUTED,false);
        fitted(c,UiText.t("안전한 장소에서 준비한 후 전방에 집중하세요."),18,469,15,764,MUTED,false);
    }
    private void drawCircuit(Canvas c,LapTimer.Snapshot s){
        panel(c,582,144,782,338,PANEL,LINE);
        LapCircuit circuit=s.circuit;
        if(circuit==null){centered(c,UiText.t("서킷 미선택"),682,220,17,MUTED,false,184);return;}
        circuitMap.draw(c,circuit,588,150,776,298,s.gpsFresh,s.latitude,s.longitude);
        fitted(c,String.format(Locale.US,"%.0f m",circuit.lengthMetres),592,316,16,85,BLUE,false);
        fitted(c,s.gpsFresh?String.format(Locale.US,"GPS ±%.0f m",s.accuracyMetres):UiText.t("GPS 대기"),674,316,14,100,MUTED,false);
    }
    private void drawPicker(Canvas c,LapTimer.Snapshot s){
        text(c,UiText.t("서킷 선택"),18,84,22,WHITE,true);
        button(c,UiText.t("이름순"),154,58,278,96,picker.nameSorted(),true);
        button(c,UiText.t("돌아가기"),654,58,782,96,false,true);
        CircuitPicker.Group[] groups=picker.visibleGroups();
        for(int row=0;row<groups.length;row++){
            CircuitPicker.Group group=groups[row];float top=108+row*68;
            boolean selected=group==picker.group();panel(c,18,top,278,top+64,selected?0xff294653:PANEL,selected?BLUE:LINE);
            fitted(c,group.name,30,top+27,19,235,selected?BLUE:WHITE,selected);
            text(c,group.count()+" "+UiText.t("코스"),30,top+52,15,MUTED,false);
        }
        centered(c,(picker.trackPage()+1)+" / "+picker.trackPages(),148,399,15,MUTED,false,250);
        button(c,"‹",18,414,140,458,false,picker.trackPage()>0);
        button(c,"›",152,414,278,458,false,picker.trackPage()+1<picker.trackPages());
        LapCircuit selected=picker.selected();panel(c,290,104,782,350,PANEL,LINE);
        if(selected!=null){
            fitted(c,selected.name,304,130,20,464,BLUE,true);
            pickerMap.draw(c,selected,304,144,768,340,s.gpsFresh,s.latitude,s.longitude);
            fitted(c,selected.courseName,290,435,20,372,WHITE,true);
            fitted(c,String.format(Locale.US,"%.0f m",selected.lengthMetres),674,435,19,108,BLUE,true);
        }else centered(c,UiText.t("확인된 서킷 정보 없음"),536,230,21,MUTED,false,456);
        button(c,"‹",290,358,332,405,false,picker.coursePage()>0);
        button(c,"›",740,358,782,405,false,picker.coursePage()+1<picker.coursePages());
        if(picker.group()!=null)for(int slot=0;slot<CircuitPicker.COURSE_BUTTONS;slot++){
            int index=picker.coursePage()*CircuitPicker.COURSE_BUTTONS+slot;
            if(index<picker.group().count())button(c,Integer.toString(index+1),342+slot*72,358,402+slot*72,405,index==picker.courseIndex(),true);
        }
        fitted(c,UiText.t("서킷과 코스를 누르면 바로 선택됩니다."),290,469,15,492,MUTED,false);
    }
    private void panel(Canvas c,float l,float t,float r,float b,int fill,int edge){p.setColor(fill);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(l,t,r,b),6,6,p);p.setColor(edge);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(new RectF(l,t,r,b),6,6,p);p.setStyle(Paint.Style.FILL);}
    private void dropdown(Canvas c,String value,float l,float t,float r,float b,boolean enabled){
        panel(c,l,t,r,b,0xff17242c,LINE);
        int color=enabled?BLUE:0xff657681;
        centered(c,value,(l+r-24)/2,(t+b)/2+6,18,color,false,r-l-48);
        float x=r-19,y=(t+b)/2;
        dropdownArrow.reset();dropdownArrow.moveTo(x-6,y-3);dropdownArrow.lineTo(x+6,y-3);dropdownArrow.lineTo(x,y+4);dropdownArrow.close();
        p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(dropdownArrow,p);
    }
    private void button(Canvas c,String value,float l,float t,float r,float b,boolean active,boolean enabled){panel(c,l,t,r,b,active?0xff294653:0xff17242c,LINE);centered(c,value,(l+r)/2,(t+b)/2+6,18,enabled?BLUE:0xff657681,false,r-l-14);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);c.drawText(value,x,y,p);}
    private void fitted(Canvas c,String value,float x,float y,float size,float width,int color,boolean bold){p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(size);float measured=p.measureText(value);text(c,value,x,y,measured>width?size*width/measured:size,color,bold);}
    private void centered(Canvas c,String value,float x,float y,float size,int color,boolean bold,float width){p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(size);float measured=p.measureText(value);if(measured>width){size*=width/measured;p.setTextSize(size);}text(c,value,x-p.measureText(value)/2,y,size,color,bold);}
    private void line(Canvas c,float x,float y,float xx,float yy,int color){p.setColor(color);p.setStrokeWidth(1);c.drawLine(x,y,xx,yy,p);}
}
