package com.veloster.vehicle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.Locale;

/** Shared received-point plot; drawing never appends, resets or estimates data. */
public final class GForcePlotRenderer {
    private static final int LINE=0xff303638,BLUE=0xffa4d9ef,RED=0xffff4059,MUTED=0xff91a5ae;
    // CN7 reference behavior: hue represents radius in 0.2 G bands, rather
    // than separate trips or saved records. These are our own vector dots.
    private static final int[] CONTOUR_COLORS={0xff024fff,0xff487aff,0xff6c7aff,0xffb675e7,0xffff493f,0xffff2020};
    private final Paint paint=new Paint(3);

    private static int contourColor(float x,float y){
        double magnitude=Math.sqrt((double)x*x+(double)y*y)*GForcePoint.RANGE_G;
        return CONTOUR_COLORS[Math.min(CONTOUR_COLORS.length-1,(int)(magnitude/0.2))];
    }
    private void contourDot(Canvas c,float x,float y,float cx,float cy,float radius,float dot){
        paint.setColor(contourColor(x,y));c.drawCircle(cx+x*radius,cy+y*radius,dot,paint);
    }
    private void contourSegment(Canvas c,GForcePoint first,GForcePoint last,float cx,float cy,float radius,float dot){
        float dx=last.x-first.x,dy=last.y-first.y;
        // Interpolation only connects the recorded directional maxima visually.
        // It does not create telemetry samples or add to received history.
        int steps=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*radius/Math.max(3,radius/25)));
        for(int i=1;i<=steps;i++){
            float fraction=(float)i/steps;
            contourDot(c,first.x+dx*fraction,first.y+dy*fraction,cx,cy,radius,dot);
        }
    }
    private void contour(Canvas c,GForceTrail.Snapshot trail,float cx,float cy,float radius,float dot){
        GForcePoint first=null,previous=null;int count=0;
        for(GForceTrail.Entry entry:trail.envelope){
            if(entry==null)continue;
            GForcePoint point=entry.point;
            if(first==null){first=point;contourDot(c,point.x,point.y,cx,cy,radius,dot);}
            else contourSegment(c,previous,point,cx,cy,radius,dot);
            previous=point;count++;
        }
        if(count>3)contourSegment(c,previous,first,cx,cy,radius,dot);
    }
    private static String peak(float value,boolean negative){
        if(Float.isNaN(value)||(negative?value>0:value<0))return "—";
        return String.format(Locale.US,"%.2f",Math.abs(value));
    }
    private void peaks(Canvas c,GForceTrail.Snapshot trail,float cx,float cy,float radius){
        paint.setColor(MUTED);paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(Math.max(8,Math.min(11,radius/10)));
        paint.setTextAlign(Paint.Align.LEFT);
        c.drawText(peak(trail.minLateralG,true),cx-radius+4,cy-4,paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText(peak(trail.maxLateralG,false),cx+radius-4,cy-4,paint);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(peak(trail.minLongitudinalG,true),cx,cy-radius+12,paint);
        c.drawText(peak(trail.maxLongitudinalG,false),cx,cy+radius-4,paint);
    }
    public void draw(Canvas c,GForceTrail.Snapshot trail,GForcePoint point,float cx,float cy,float radius,float liveRadius,float historyRadius){
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(LINE);
        c.drawCircle(cx,cy,radius,paint);c.drawCircle(cx,cy,radius/2,paint);
        c.drawLine(cx-radius,cy,cx+radius,cy,paint);c.drawLine(cx,cy-radius,cx,cy+radius,paint);
        paint.setStyle(Paint.Style.FILL);
        if(trail!=null){
            for(GForceTrail.Entry entry:trail.recent){
                paint.setColor(0x4d000000|((entry.point.clipped?RED:BLUE)&0x00ffffff));
                c.drawCircle(cx+entry.point.x*radius,cy+entry.point.y*radius,historyRadius,paint);
            }
            contour(c,trail,cx,cy,radius,Math.max(0.8f,Math.min(1.8f,historyRadius*0.45f)));
            if(radius>=64)peaks(c,trail,cx,cy,radius);
        }
        if(point!=null&&point.valid){
            paint.setColor(point.clipped?RED:BLUE);
            c.drawCircle(cx+point.x*radius,cy+point.y*radius,liveRadius,paint);
        }else{
            paint.setColor(MUTED);paint.setTextSize(Math.min(20,radius/2));paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);c.drawText("—",cx,cy+5,paint);
        }
    }
}
