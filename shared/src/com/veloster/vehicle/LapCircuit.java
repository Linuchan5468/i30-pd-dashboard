package com.veloster.vehicle;

/** Factual circuit metadata separated from the timer and from OEM artwork. */
public final class LapCircuit {
    public final String id,name,courseName,provenance;
    public final double lengthMetres;
    /** Independently recovered source points; they are never joined into a gate. */
    public final Point start,goal;
    /** Null means that a verified finish-line geometry has not been recovered. */
    public final Gate finish;
    private final Gate[] sectors;
    private final double[] outline;
    public LapCircuit(String id,String name,String courseName,double lengthMetres,Gate finish,Gate[] orderedSectors,String provenance){
        this(id,name,courseName,lengthMetres,finish,orderedSectors,new double[0],provenance);
    }
    public LapCircuit(String id,String name,String courseName,double lengthMetres,Gate finish,Gate[] orderedSectors,double[] outlineLatLon,String provenance){
        this(id,name,courseName,lengthMetres,finish,orderedSectors,outlineLatLon,null,null,provenance);
    }
    public LapCircuit(String id,String name,String courseName,double lengthMetres,Point start,Point goal,double[] outlineLatLon,String provenance){
        this(id,name,courseName,lengthMetres,null,null,outlineLatLon,start,goal,provenance);
    }
    private LapCircuit(String id,String name,String courseName,double lengthMetres,Gate finish,Gate[] orderedSectors,double[] outlineLatLon,Point start,Point goal,String provenance){
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,80}")||name==null||courseName==null||provenance==null)throw new IllegalArgumentException("circuit metadata");
        if(Double.isNaN(lengthMetres)||Double.isInfinite(lengthMetres)||lengthMetres<0||lengthMetres>100000)throw new IllegalArgumentException("course length");
        this.id=id;this.name=name;this.courseName=courseName;this.lengthMetres=lengthMetres;this.finish=finish;this.start=start;this.goal=goal;this.provenance=provenance;
        sectors=orderedSectors==null?new Gate[0]:orderedSectors.clone();
        if(sectors.length>32)throw new IllegalArgumentException("sector count");
        for(Gate g:sectors)if(g==null)throw new IllegalArgumentException("sector geometry");
        outline=outlineLatLon==null?new double[0]:outlineLatLon.clone();
        if(outline.length%2!=0||outline.length>20000)throw new IllegalArgumentException("course outline");
        for(int i=0;i<outline.length;i+=2)if(!coordinate(outline[i],outline[i+1]))throw new IllegalArgumentException("course coordinate");
    }
    public Gate[] sectorGates(){return sectors.clone();}
    public double[] outline(){return outline.clone();}
    public boolean automaticReady(){return start!=null&&goal!=null;}
    public String displayName(){return courseName.length()==0?name:name+" · "+courseName;}
    public static boolean coordinate(double lat,double lon){return !Double.isNaN(lat)&&!Double.isNaN(lon)&&!Double.isInfinite(lat)&&!Double.isInfinite(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180;}
    public static double distance(double aLat,double aLon,double bLat,double bLon){
        double dy=Math.toRadians(bLat-aLat),dx=Math.toRadians(bLon-aLon);
        double h=Math.sin(dy/2)*Math.sin(dy/2)+Math.cos(Math.toRadians(aLat))*Math.cos(Math.toRadians(bLat))*Math.sin(dx/2)*Math.sin(dx/2);
        return 6371008.8*2*Math.atan2(Math.sqrt(Math.max(0,h)),Math.sqrt(Math.max(0,1-h)));
    }
    public static final class Point {
        public final double latitude,longitude;
        public Point(double lat,double lon){if(!coordinate(lat,lon))throw new IllegalArgumentException("source point");latitude=lat;longitude=lon;}
        /** Nearest point on the real fix segment, including clamped endpoints. */
        public double proximity(double fromLat,double fromLon,double toLat,double toLon,double radiusMetres){
            double cos=Math.cos(Math.toRadians(latitude));
            double x=(fromLon-longitude)*cos,y=fromLat-latitude;
            double dx=(toLon-fromLon)*cos,dy=toLat-fromLat,denominator=dx*dx+dy*dy;
            double t=denominator>0?Math.max(0,Math.min(1,-(x*dx+y*dy)/denominator)):0;
            double nearestLat=fromLat+(toLat-fromLat)*t,nearestLon=fromLon+(toLon-fromLon)*t;
            return distance(latitude,longitude,nearestLat,nearestLon)<=radiusMetres?t:Double.NaN;
        }
    }
    /** Two verified WGS84 endpoints. Direction0 preserves an unknown source direction. */
    public static final class Gate {
        public final double aLat,aLon,bLat,bLon;
        /** +1: negative to positive signed side; -1: reverse; 0: either. */
        public final int direction;
        public Gate(double aLat,double aLon,double bLat,double bLon,int direction){
            if(!coordinate(aLat,aLon)||!coordinate(bLat,bLon)||direction< -1||direction>1)throw new IllegalArgumentException("gate");
            double width=distance(aLat,aLon,bLat,bLon);
            if(width<1||width>1000)throw new IllegalArgumentException("gate width");
            this.aLat=aLat;this.aLon=aLon;this.bLat=bLat;this.bLon=bLon;this.direction=direction;
        }
        public double latitude(){return(aLat+bLat)/2;}
        public double longitude(){return(aLon+bLon)/2;}
        /** Finite segment intersection, returning the fix-pair fraction or NaN. */
        public double crossing(double fromLat,double fromLon,double toLat,double toLon){
            double scale=Math.cos(Math.toRadians(latitude()));
            double gx=(bLon-aLon)*scale,gy=bLat-aLat;
            double px=(fromLon-aLon)*scale,py=fromLat-aLat;
            double dx=(toLon-fromLon)*scale,dy=toLat-fromLat;
            double before=gx*py-gy*px,after=gx*(py+dy)-gy*(px+dx);
            if(before==0||before*after>0||before==after)return Double.NaN;
            if(direction>0&&!(before<0&&after>=0)||direction<0&&!(before>0&&after<=0))return Double.NaN;
            double denominator=dx*gy-dy*gx;
            if(Math.abs(denominator)<1e-20)return Double.NaN;
            double t=(-px*gy+py*gx)/denominator,u=(-px*dy+py*dx)/denominator;
            return t>=0&&t<=1&&u>=0&&u<=1?t:Double.NaN;
        }
    }
}
