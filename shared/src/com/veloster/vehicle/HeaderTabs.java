package com.veloster.vehicle;

/** Instance-only header viewport, scrolling and touch ownership in logical800x480. */
public final class HeaderTabs {
    public static final int COUNT=7;
    public static final float LEFT=144,RIGHT=592,TOP=8,BOTTOM=46,SPACING=6,TOUCH_SLOP=8;
    public static final float VIEWPORT_WIDTH=RIGHT-LEFT,CONTENT_WIDTH=584,MAX_SCROLL=CONTENT_WIDTH-VIEWPORT_WIDTH;
    private static final float[] WIDTHS={94,54,74,82,96,96,52};
    private float offset,downX,downY,downScroll;
    private boolean captured,cancelled,dragging;
    private int downId=-1;

    private static void validId(int id){if(id<0||id>=COUNT)throw new IllegalArgumentException("Header tab id");}
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private static float clamp(float value){return Math.max(0,Math.min(MAX_SCROLL,value));}
    private static boolean inside(float x,float y){return x>=LEFT&&x<RIGHT&&y>=TOP&&y<BOTTOM;}
    public float width(int id){validId(id);return WIDTHS[id];}
    public float left(int id){
        validId(id);float x=LEFT-offset;
        for(int i=0;i<id;i++)x+=WIDTHS[i]+SPACING;
        return x;
    }
    public float scroll(){return offset;}
    private int hit(float x,float y){
        if(!inside(x,y))return -1;
        for(int id=0;id<COUNT;id++){float left=left(id);if(x>=left&&x<left+WIDTHS[id])return id;}
        return -1;
    }
    /** Captures the viewport, including gaps so a swipe can start between tabs. */
    public boolean down(float x,float y){
        cancel();if(!inside(x,y))return false;
        captured=true;downX=x;downY=y;downScroll=offset;downId=hit(x,y);return true;
    }
    /** True while this gesture is owned, even after a vertical movement cancels it. */
    public boolean move(float x,float y){
        if(!captured)return false;
        if(cancelled)return true;
        if(!finite(x)||!finite(y)||Math.abs(y-downY)>TOUCH_SLOP){cancelled=true;return true;}
        float dx=x-downX;
        if(Math.abs(dx)>TOUCH_SLOP)dragging=true;
        if(dragging)offset=clamp(downScroll-dx);
        return true;
    }
    /** A swipe never selects a tab, including a swipe back to its starting point. */
    public int up(float x,float y){
        if(!captured)return -1;
        move(x,y);
        int id=!cancelled&&!dragging&&downId>=0&&hit(x,y)==downId?downId:-1;
        cancel();if(id>=0)ensureVisible(id);return id;
    }
    /** Cancels touch state without resetting the instance's current scroll. */
    public void cancel(){captured=cancelled=dragging=false;downId=-1;}
    public void ensureVisible(int id){
        float x=left(id);
        if(x<LEFT)offset=clamp(offset+x-LEFT);
        else if(x+width(id)>RIGHT)offset=clamp(offset+x+width(id)-RIGHT);
    }
}
