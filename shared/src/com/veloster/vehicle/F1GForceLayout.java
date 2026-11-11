package com.veloster.vehicle;

/** Logical F1 plot geometry and local expansion state. No telemetry or preferences. */
public final class F1GForceLayout {
    public static final class Plot {
        public final float x,y,radius,hitRadius;
        private Plot(float x,float y,float radius,float hitRadius){this.x=x;this.y=y;this.radius=radius;this.hitRadius=hitRadius;}
        public boolean circle(float px,float py){float dx=px-x,dy=py-y;return dx*dx+dy*dy<=hitRadius*hitRadius;}
    }
    private static final Plot NORMAL=new Plot(68,329,29,35);
    private static final Plot MEDIA=new Plot(92,348,36,42);
    private static final Plot EXPANDED=new Plot(250,272,125,131);
    private boolean expanded,previousMedia;
    public boolean expanded(){return expanded;}
    /** Returns the media flag appropriate for the resulting page. */
    public boolean toggle(boolean media){
        if(expanded){expanded=false;return previousMedia;}
        previousMedia=media;expanded=true;return false;
    }
    /** The existing mini artwork always opens media from the expanded page. */
    public boolean toggleMedia(boolean media){
        if(expanded){expanded=false;return true;}
        return !media;
    }
    public static Plot plot(boolean media,boolean expanded){return expanded?EXPANDED:media?MEDIA:NORMAL;}
    /** Conservatively match the existing now-playing overlay's full coverage. */
    public static boolean covered(float y,boolean media,boolean expanded,boolean notice){return notice&&!media&&!expanded&&y>=347;}
    public static boolean hit(float x,float y,boolean media,boolean expanded){
        if(expanded)return x>=18&&x<=482&&y>=116&&y<=434;
        if(media){
            // The upper right belongs to the existing blue gear card.
            return x>=30&&x<=252&&y>=344&&y<=412||x>=30&&x<=138&&y>=290&&y<=412;
        }
        return x>=18&&x<=276&&y>=282&&y<=369;
    }
}
