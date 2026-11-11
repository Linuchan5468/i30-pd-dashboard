package com.veloster.vehicle;

/** F1 state, shared hit geometry and gesture semantics, without Android. */
public final class F1GForceInteractionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void notHit(float x,float y,boolean media,boolean expanded,String message){
        check(!F1GForceLayout.hit(x,y,media,expanded),message);
    }
    private static void disabled(boolean enabled){
        GForceTouch touch=new GForceTouch();
        touch.down(68,329,enabled,false,false,100);
        check(!touch.resetCandidate(),"disabled profile/page does not schedule reset");
        check(!touch.hold(900),"disabled profile/page cannot reset");
        check(!touch.up(68,329,200),"disabled profile/page cannot expand");
    }
    public static void main(String[] args){
        F1GForceLayout state=new F1GForceLayout();
        check(!state.expanded(),"initial F1 is compact");
        boolean media=state.toggle(false);
        check(state.expanded()&&!media,"normal F1 expands without media");
        media=state.toggle(media);
        check(!state.expanded()&&!media,"collapse restores normal F1");
        media=state.toggle(true);
        check(state.expanded()&&!media,"media F1 gives room to enlarged plot");
        media=state.toggle(media);
        check(!state.expanded()&&media,"collapse restores previous media page");
        media=state.toggle(false);
        check(state.expanded()&&!media,"normal may expand again after media cycle");
        media=state.toggleMedia(media);
        check(!state.expanded()&&media,"mini artwork opens media and collapses plot");
        media=state.toggle(media);
        check(state.expanded()&&!media,"media after mini override expands correctly");
        media=state.toggle(media);
        check(!state.expanded()&&media,"subsequent collapse restores media, not stale normal state");
        check(!state.toggleMedia(true)&&state.toggleMedia(false),"unexpanded artwork retains its old media toggle");

        check(F1GForceLayout.hit(18,282,false,false)&&F1GForceLayout.hit(276,369,false,false),"normal plot bounds include existing tile edges");
        notHit(17.9f,329,false,false,"normal left gap");notHit(276.1f,329,false,false,"normal right gap");
        notHit(68,281.9f,false,false,"normal upper gap");notHit(68,369.1f,false,false,"normal lower gap");
        check(F1GForceLayout.hit(30,290,true,false)&&F1GForceLayout.hit(138,290,true,false),"media upper-left plot is active");
        check(F1GForceLayout.hit(252,344,true,false)&&F1GForceLayout.hit(252,412,true,false),"media readouts join plot target");
        notHit(138.1f,300,true,false,"media upper-right belongs to gear");
        notHit(252.1f,370,true,false,"media readout right gap");notHit(92,412.1f,true,false,"media lower gap");
        check(F1GForceLayout.hit(18,116,false,true)&&F1GForceLayout.hit(482,434,false,true),"expanded plot uses its explicit panel bounds");
        notHit(17.9f,272,false,true,"expanded left gap");notHit(482.1f,272,false,true,"expanded compact-instruments gap");
        notHit(250,115.9f,false,true,"expanded upper LED/RPM gap");notHit(250,434.1f,false,true,"expanded lower music/shift gap");

        F1GForceLayout.Plot normal=F1GForceLayout.plot(false,false),combined=F1GForceLayout.plot(true,false),large=F1GForceLayout.plot(false,true);
        check(normal.x==68&&normal.y==329&&normal.radius==29,"normal plot geometry remains physically validated baseline");
        check(combined.x==92&&combined.y==348&&combined.radius==36,"media plot geometry remains baseline");
        check(large.radius>normal.radius*4&&large.radius>combined.radius*3,"expanded plot is substantially larger");
        check(F1GForceLayout.plot(true,true)==large,"expanded geometry is independent of remembered media flag");
        for(boolean m:new boolean[]{false,true})for(boolean e:new boolean[]{false,true}){
            F1GForceLayout.Plot p=F1GForceLayout.plot(m,e);
            check(p.hitRadius>p.radius,"circle reset target includes visible edge");
            check(p.circle(p.x,p.y)&&p.circle(p.x+p.hitRadius,p.y),"circle reset target includes center and edge");
            check(!p.circle(p.x+p.hitRadius+0.1f,p.y),"outside circle cannot reset");
            check(F1GForceLayout.hit(p.x,p.y,m,e),"visible center is a size-toggle target");
            for(int x=0;x<=800;x+=20)for(int y:new int[]{0,8,33,46,52})notHit(x,y,m,e,"plot never captures navigation/header");
            for(int x:new int[]{18,44,70})for(int y:new int[]{436,452,479})notHit(x,y,m,e,"plot never captures mini artwork");
            for(int x:new int[]{210,231,252,440,461,482,548,569,590,740,761,782})notHit(x,465,m,e,"plot never captures either shift-control layout");
            for(int x:new int[]{494,537,580,588,637,686,694,738,782})notHit(x,465,m,e,"plot never captures media transport");
        }
        for(int x:new int[]{294,400,506})for(int y:new int[]{120,200,321,367})notHit(x,y,false,false,"normal central gear remains independent");
        for(int x:new int[]{180,248,316})for(int y:new int[]{143,200,290,340})notHit(x,y,true,false,"media gear remains independent");
        for(int x:new int[]{32,150,258,368,470})for(int y:new int[]{420,432,438})notHit(x,y,true,false,"media pedals remain independent");
        for(int x:new int[]{18,200,394,406,600,782})for(int y:new int[]{377,403,423,434})notHit(x,y,false,false,"normal pedals remain independent");
        for(int x:new int[]{510,600,782})for(int y:new int[]{116,200,320,434})notHit(x,y,false,true,"expanded compact instruments remain independent");

        GForceTouch touch=new GForceTouch();
        touch.down(68,329,true,false,false,100);
        check(touch.resetCandidate(),"circle press schedules reset candidate");
        check(!touch.hold(899),"799ms is not a reset");
        check(touch.up(68,329,899),"799ms release is still a size-toggle tap");
        check(!touch.resetCandidate()&&!touch.hold(1000),"tap consumes the gesture");
        touch.down(68,329,true,false,false,100);
        check(touch.hold(900),"800ms circle hold resets exactly once");
        check(!touch.hold(1000),"held reset cannot repeat");
        check(!touch.up(68,329,901),"reset-consumed release cannot expand");
        touch.down(180,320,true,false,false,100);
        check(!touch.resetCandidate(),"readout region can toggle but cannot reset history");
        check(touch.up(180,320,200),"readout region quick tap toggles");
        touch.down(180,320,true,false,false,100);
        check(!touch.hold(900)&&!touch.up(180,320,900),"long readout press neither resets nor unexpectedly expands");
        touch.down(68,329,true,false,false,100);
        check(!touch.up(68,329,900),"late release cannot expand if hold callback was delayed");
        touch.down(68,329,true,false,false,100);touch.move(83.9f,344.9f);
        check(touch.up(83.9f,344.9f,200),"movement below16px retains tap");
        for(float[] delta:new float[][]{{16,0},{-16,0},{0,16},{0,-16}}){
            touch.down(68,329,true,false,false,100);touch.move(68+delta[0],329+delta[1]);
            check(!touch.resetCandidate()&&!touch.hold(900),"16px movement cancels reset");
            check(!touch.up(68,329,200),"moving back cannot resurrect canceled tap");
        }
        touch.down(68,329,true,false,false,100);touch.cancel();
        check(!touch.hold(900)&&!touch.up(68,329,200),"ACTION_CANCEL equivalent is terminal");
        touch.down(68,329,true,false,false,100);
        check(!touch.up(68,329,99),"backwards release cannot tap");
        check(!touch.hold(900),"backwards release consumes gesture");
        touch.down(68,329,true,false,false,100);
        check(!touch.hold(99),"backwards clock cannot reset");
        check(!touch.resetCandidate()&&!touch.up(68,329,200),"backwards hold cancels rather than resurrecting later");
        check(!touch.hold(900),"backwards hold cannot reset after clock recovers");
        touch.down(68,329,true,false,false,-1);
        check(!touch.resetCandidate()&&!touch.hold(900)&&!touch.up(68,329,100),"negative initial clock disables gesture");
        disabled(false);disabled(false); // Caller gates non-PD and non-F1 separately.
        touch.down(400,200,true,false,false,100);
        check(!touch.resetCandidate()&&!touch.hold(900)&&!touch.up(400,200,200),"gear press cannot turn into G-force gesture");
        touch.down(92,348,true,true,false,100);
        check(touch.resetCandidate()&&touch.up(92,348,200),"media circle uses same gesture contract");
        touch.down(250,272,true,false,true,100);
        check(touch.resetCandidate()&&touch.hold(900)&&!touch.up(250,272,901),"expanded circle reset also consumes release");
        touch.down(480,430,true,false,true,100);
        check(!touch.resetCandidate()&&touch.up(480,430,200),"expanded panel edge collapses without resetting");
        check(!F1GForceLayout.covered(346.9f,false,false,true),"visible plot above notice remains usable");
        check(F1GForceLayout.covered(347,false,false,true),"notice boundary blocks hidden plot");
        check(F1GForceLayout.covered(355,false,false,true),"notice blocks lower circle reset region");
        check(!F1GForceLayout.covered(355,true,false,true),"media page has no overlaid notice");
        check(!F1GForceLayout.covered(355,false,true,true),"expanded page suppresses overlaid notice");
        check(!F1GForceLayout.covered(355,false,false,false),"normal plot remains usable without notice");
        touch.down(68,355,!F1GForceLayout.covered(355,false,false,true),false,false,100);
        check(!touch.hold(900)&&!touch.up(68,355,200),"covered lower circle cannot reset or expand");
        System.out.println("PASS "+checks+" F1 G-force state, hit and gesture checks");
    }
}
