import com.veloster.vehicle.HeaderTabs;

/** Pure geometry and real gesture sequences; no Android dependency. */
public final class HeaderTabsTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(float value,float expected,String why){check(Math.abs(value-expected)<0.0001f,why);}
    private static int tap(HeaderTabs t,float x,float y){check(t.down(x,y),"header owns down");return t.up(x,y);}
    public static void main(String[] args){
        HeaderTabs t=new HeaderTabs();near(t.scroll(),0,"instance begins at first tab");near(t.left(0),144,"first left");
        near(t.left(1),244,"spacing after overview");near(t.left(6)+t.width(6),728,"full content end");
        near(HeaderTabs.MAX_SCROLL,136,"content maximum");
        for(int id=0;id<5;id++)check(tap(t,t.left(id)+t.width(id)/2,27)==id,"visible tab hit "+id);
        check(!t.down(143,27)&&!t.down(592,27)&&!t.down(200,7)&&!t.down(200,46),"half-open clipped viewport");
        check(t.up(200,27)==-1&&!t.move(200,27),"unowned gesture has no effect");
        check(tap(t,241,27)==-1,"spacing has no tab action");
        check(tap(t,590,27)==5,"visible part of lap can be selected");near(t.scroll(),78,"selected clipped lap becomes visible");
        t.ensureVisible(6);near(t.scroll(),136,"settings can be fully revealed");
        check(tap(t,570,27)==6,"settings id is returned as an action; no selection state stored");
        t.ensureVisible(0);near(t.scroll(),0,"selecting first tab returns to start");
        t.down(500,27);check(t.move(492,27),"horizontal slop gesture owned");near(t.scroll(),0,"exact8px does not drag");
        check(t.up(492,27)==4,"exact slop inside same tab remains tap");
        t.down(500,27);t.move(491,27);near(t.scroll(),9,"past slop scrolls");check(t.up(491,27)==-1,"drag does not activate");
        t.ensureVisible(0);t.down(500,27);t.move(480,27);t.move(500,27);check(t.up(500,27)==-1,"returning swipe never becomes click");
        t.down(500,27);check(t.up(450,27)==-1,"large release displacement without MOVE is still swipe");near(t.scroll(),50,"release scroll accounted for");
        t.down(300,27);t.move(300,36);float held=t.scroll();t.move(200,27);check(t.up(300,27)==-1,"vertical cancellation remains sticky");near(t.scroll(),held,"vertical cancellation cannot later scroll");
        t.down(300,27);t.move(100,27);near(t.scroll(),136,"left swipe clamps maximum");t.up(100,27);
        t.down(300,27);t.move(900,27);near(t.scroll(),0,"right swipe clamps minimum outside viewport");t.up(900,27);
        t.down(500,27);t.move(450,27);t.cancel();check(t.up(450,27)==-1,"cancel cannot activate");near(t.scroll(),50,"cancel preserves scroll");
        t.down(500,27);t.move(Float.NaN,27);check(t.up(500,27)==-1,"nonfinite move cancels safely");
        check(!t.down(Float.NaN,27)&&!t.down(Float.POSITIVE_INFINITY,27),"nonfinite down rejected");
        for(int id=0;id<HeaderTabs.COUNT;id++){
            t.ensureVisible(id);check(t.left(id)>=HeaderTabs.LEFT&&t.left(id)+t.width(id)<=HeaderTabs.RIGHT,"ensure visible "+id);
            check(tap(t,t.left(id)+t.width(id)/2,27)==id,"tab hit after scroll "+id);
        }
        for(int i=0;i<500;i++){
            float start=150+i%400;t.down(start,27);t.move(start+(i%2==0?1000:-1000),27);t.up(start,27);
            check(t.scroll()>=0&&t.scroll()<=HeaderTabs.MAX_SCROLL,"bounded repeated swipe");
        }
        check(new HeaderTabs().scroll()==0,"new instance does not inherit scroll");
        boolean bad=false;try{t.ensureVisible(7);}catch(IllegalArgumentException expected){bad=true;}check(bad,"invalid id rejected");
        System.out.println("PASS "+checks+" horizontal header geometry/gesture checks");
    }
}
