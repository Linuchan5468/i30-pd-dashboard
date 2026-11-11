package com.veloster.vehicle;

/** One F1 gesture: a short tap toggles size; a consumed hold can only reset. */
public final class GForceTouch {
    public static final long HOLD_MS=800;
    public static final float MOVE_SLOP=16;
    private boolean active,resetCandidate,media,expanded;
    private float downX,downY;
    private long began;
    public void down(float x,float y,boolean enabled,boolean media,boolean expanded,long now){
        this.media=media;this.expanded=expanded;downX=x;downY=y;began=now;
        active=enabled&&now>=0&&F1GForceLayout.hit(x,y,media,expanded);
        resetCandidate=active&&F1GForceLayout.plot(media,expanded).circle(x,y);
    }
    public boolean resetCandidate(){return active&&resetCandidate;}
    public void move(float x,float y){if(Math.abs(x-downX)>=MOVE_SLOP||Math.abs(y-downY)>=MOVE_SLOP)cancel();}
    public void cancel(){active=false;resetCandidate=false;}
    public boolean hold(long now){
        if(!active)return false;
        if(now<began){cancel();return false;}
        if(!resetCandidate||now-began<HOLD_MS)return false;
        cancel();return true;
    }
    public boolean up(float x,float y,long now){
        boolean tap=active&&now>=began&&now-began<HOLD_MS&&Math.abs(x-downX)<MOVE_SLOP&&Math.abs(y-downY)<MOVE_SLOP&&F1GForceLayout.hit(x,y,media,expanded);
        cancel();return tap;
    }
}
