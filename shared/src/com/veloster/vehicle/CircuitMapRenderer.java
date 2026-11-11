package com.veloster.vehicle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/** Source COURSE link polylines, with one cached Path per independent link. */
public final class CircuitMapRenderer {
    private static final int TRACK=0xffa4d9ef,START=0xff78d698,GOAL=0xffff6b77,WHITE=0xffe1edf2,MUTED=0xff91a5ae;
    private final Paint paint=new Paint(3);
    private Cn7CircuitRoutes.Route route;
    private Path[] paths=new Path[0];
    private float cachedLeft=Float.NaN,cachedTop,cachedRight,cachedBottom;
    private double centreLatitude,centreLongitude,cosLatitude,scale;
    private float centreX,centreY;

    public CircuitMapRenderer(){}
    private float x(double longitude){return (float)(centreX+(longitude-centreLongitude)*cosLatitude*scale);}
    private float y(double latitude){return (float)(centreY-(latitude-centreLatitude)*scale);}
    private void prepare(Cn7CircuitRoutes.Route selected,float left,float top,float right,float bottom){
        if(route==selected&&cachedLeft==left&&cachedTop==top&&cachedRight==right&&cachedBottom==bottom)return;
        route=selected;cachedLeft=left;cachedTop=top;cachedRight=right;cachedBottom=bottom;
        if(selected==null){paths=new Path[0];return;}
        centreLatitude=(selected.minLatitude+selected.maxLatitude)/2;
        centreLongitude=(selected.minLongitude+selected.maxLongitude)/2;
        cosLatitude=Math.cos(Math.toRadians(centreLatitude));centreX=(left+right)/2;centreY=(top+bottom)/2;
        float padding=Math.min(14,Math.min(right-left,bottom-top)*0.08f);
        scale=Math.min((right-left-padding*2)/Math.max(1e-12,(selected.maxLongitude-selected.minLongitude)*cosLatitude),
            (bottom-top-padding*2)/Math.max(1e-12,selected.maxLatitude-selected.minLatitude));
        paths=new Path[selected.linkCount];
        for(int i=0;i<paths.length;i++){
            Cn7CircuitRoutes.Link link=selected.link(i);Path path=new Path();
            path.moveTo(x(link.longitude(0)),y(link.latitude(0)));
            for(int p=1;p<link.pointCount();p++)path.lineTo(x(link.longitude(p)),y(link.latitude(p)));
            // No close() and no line between independent source links.
            paths[i]=path;
        }
    }
    /** AREA site-boundary data is never read or presented as a selected course. */
    public void draw(Canvas canvas,LapCircuit circuit,float left,float top,float right,float bottom,
            boolean fresh,double latitude,double longitude){
        if(right<=left||bottom<=top)return;
        Cn7CircuitRoutes.Route selected=circuit==null?null:Cn7CircuitRoutes.forId(circuit.id);
        prepare(selected,left,top,right,bottom);
        int save=canvas.save();canvas.clipRect(left,top,right,bottom);
        if(selected==null){
            paint.setStyle(Paint.Style.FILL);paint.setColor(MUTED);paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(12);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("—",(left+right)/2,(top+bottom)/2,paint);
            canvas.restoreToCount(save);return;
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.5f,Math.min(3,(right-left)/150)));
        paint.setColor(TRACK);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        for(Path path:paths)canvas.drawPath(path,paint);
        float marker=Math.min(5,Math.max(2.5f,(right-left)/90));
        if(circuit.start!=null){paint.setColor(START);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
            canvas.drawCircle(x(circuit.start.longitude),y(circuit.start.latitude),marker+1,paint);}
        if(circuit.goal!=null){paint.setColor(GOAL);paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x(circuit.goal.longitude),y(circuit.goal.latitude),marker*0.7f,paint);}
        if(fresh&&LapCircuit.coordinate(latitude,longitude)){
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff173947);canvas.drawCircle(x(longitude),y(latitude),marker+2,paint);
            paint.setColor(WHITE);canvas.drawCircle(x(longitude),y(latitude),marker,paint);
        }
        canvas.restoreToCount(save);
    }
}
