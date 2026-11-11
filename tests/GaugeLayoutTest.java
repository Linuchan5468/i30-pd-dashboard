package com.veloster.vehicle;

/** Stored gauge compatibility and card/hit geometry; no Android dependency. */
public final class GaugeLayoutTest {
    private static int checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static void rejected(GaugeLayout layout,int slot,int signal,int style){
        int oldSignal=layout.signal(slot),oldStyle=layout.style(slot);boolean rejected=false;
        try{layout.set(slot,signal,style);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"invalid selection rejected");
        check(layout.signal(slot)==oldSignal&&layout.style(slot)==oldStyle,"invalid selection is atomic");
    }
    public static void main(String[] args){
        GaugeLayout layout=new GaugeLayout();
        int[] defaults={0,7,9,1,2,10,11,12,15,16,17,13};
        check(!layout.pdProfile(),"unset source cannot offer PD plot");
        for(int i=0;i<GaugeLayout.SLOTS;i++){
            check(layout.signal(i)==defaults[i],"default source unchanged");
            check(layout.style(i)==(i<3?0:2),"default style unchanged");
        }
        for(int signal=0;signal<Telemetry.LABELS.length;signal++){
            for(int style=0;style<=2;style++){
                layout.set(0,signal,style);
                check(layout.signal(0)==signal&&layout.style(0)==style,"legacy preference preserves source/style");
                check(layout.displayStyle(0)==style,"legacy style displays unchanged");
            }
            check(layout.styleCount(signal)==3,"non-PD style choices unchanged");
        }
        layout.set(0,11,GaugeLayout.STYLE_GFORCE);
        check(layout.style(0)==3&&layout.displayStyle(0)==2,"saved PD selection survives load with safe non-PD display");
        layout.pdProfile(true);
        check(layout.displayStyle(0)==3,"PD restores persisted 2D style");
        for(int signal=0;signal<Telemetry.LABELS.length;signal++){
            check(layout.styleCount(signal)==((signal==11||signal==12)?4:3),"only G-force sources offer fourth style");
            if(signal!=11&&signal!=12)rejected(layout,0,signal,3);
        }
        layout.source(0,12);
        check(layout.signal(0)==12&&layout.style(0)==3,"switching G-force axis retains same 2D plot");
        layout.source(0,7);
        check(layout.signal(0)==7&&layout.style(0)==2,"unrelated source safely becomes numeric");
        for(int style=0;style<=2;style++){
            layout.set(0,11,style);layout.source(0,7);
            check(layout.style(0)==style,"ordinary source editing preserves original styles");
        }
        layout.set(0,11,3);layout.set(1,7,1);layout.swap(0,1);
        check(layout.signal(1)==11&&layout.style(1)==3,"swap carries plot with G-force source");
        check(layout.signal(0)==7&&layout.style(0)==1,"swap preserves other source/style");
        layout.pdProfile(false);
        check(layout.style(1)==3&&layout.displayStyle(1)==2,"switching profile does not erase stored preference");
        layout.pdProfile(true);
        check(layout.displayStyle(1)==3,"returning PD restores plot");
        rejected(layout,0,11,-1);rejected(layout,0,11,4);rejected(layout,0,-1,2);
        rejected(layout,0,Telemetry.LABELS.length,2);
        for(boolean dense:new boolean[]{false,true}){
            layout.dense(dense);
            check(layout.count()==(dense?12:6)&&layout.columns()==(dense?4:3),"grid shape unchanged");
            for(int i=0;i<layout.count();i++){
                float l=layout.left(i),t=layout.top(i),w=layout.width(),h=layout.height();
                check(l>=0&&t>=80&&l+w<=800&&t+h<=480,"card remains below navigation inside screen");
                check(layout.hit(l+w/2,t+h/2)==i,"card center targets existing edit slot");
                check(layout.hit(l,t)==i,"inclusive upper-left card edge");
                check(layout.hit(l+w,t+h/2)==-1,"right gap remains untargeted");
                check(layout.hit(l+w/2,t+h)==-1,"bottom gap remains untargeted");
                // New plot/readouts use the same uniformly scaled 252x190 card.
                float scale=dense?124f/190:1;
                check(17*scale>=0&&127*scale<=w&&160*scale<=h,"2D circle fits both card grids");
            }
            check(layout.hit(100,71)==-1,"grid hint/header never opens gauge editor");
        }
        layout.reset();
        for(int i=0;i<GaugeLayout.SLOTS;i++)check(layout.signal(i)==defaults[i]&&layout.style(i)==(i<3?0:2),"reset preserves legacy defaults");
        System.out.println("PASS "+checks+" gauge preference, eligibility and geometry checks");
    }
}
