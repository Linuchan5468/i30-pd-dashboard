import com.veloster.vehicle.OverviewLayout;
import com.veloster.vehicle.Telemetry;

/** PD pedal replaces the visible throttle slot without changing geometry or preferences. */
public final class OverviewLayoutTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        OverviewLayout layout=new OverviewLayout();
        int count=layout.count();
        check(layout.signalAt(3)==9,"default JSN slot remains throttle");
        float left=layout.left(9),top=layout.top(9),width=layout.width(9),height=layout.height(9);
        float throttleLeft=layout.left(Telemetry.POWER),throttleTop=layout.top(Telemetry.POWER);
        float stateLeft=layout.left(Telemetry.TCS),stateTop=layout.top(Telemetry.TCS);
        layout.style(9,0);layout.style(Telemetry.ACCEL_PEDAL,1);
        check(layout.hit(left+width/2,top+height/2)==9,"default hit selects throttle");
        layout.pdProfile(true);
        check(layout.count()==count&&layout.signalAt(3)==Telemetry.ACCEL_PEDAL,"PD replaces a card without adding a slot");
        check(layout.left(Telemetry.ACCEL_PEDAL)==left&&layout.top(Telemetry.ACCEL_PEDAL)==top
            &&layout.width(Telemetry.ACCEL_PEDAL)==width&&layout.height(Telemetry.ACCEL_PEDAL)==height,
            "PD pedal retains card geometry");
        check(layout.headline(Telemetry.ACCEL_PEDAL)&&layout.hit(left+width/2,top+height/2)==Telemetry.ACCEL_PEDAL,
            "PD headline and touch both address pedal");
        check(layout.signalAt(10)==9&&!layout.headline(9)&&layout.left(9)==throttleLeft&&layout.top(9)==throttleTop,
            "PD throttle has a distinct non-headline card in the prior unsupported power slot");
        check(layout.hit(throttleLeft+width/2,throttleTop+height/2)==9,
            "PD throttle card touch does not select pedal");
        check(layout.signalAt(16)==Telemetry.PD_TCS_ENABLED&&layout.grouped(Telemetry.PD_TCS_ENABLED)
            &&layout.left(Telemetry.PD_TCS_ENABLED)==stateLeft&&layout.top(Telemetry.PD_TCS_ENABLED)==stateTop,
            "PD enabled state group replaces the intervention group in the same slot");
        check(layout.hit(stateLeft+width/2,stateTop+height/2)==Telemetry.PD_TCS_ENABLED,
            "PD state group touches select its independent signals");
        int stateStyle=layout.style(Telemetry.PD_TCS_ENABLED);layout.cycle(Telemetry.PD_TCS_ENABLED);
        check(layout.style(Telemetry.PD_TCS_ENABLED)==stateStyle,"grouped enabled state does not cycle gauge styles");
        check(layout.signalAt(0)==7&&layout.signalAt(1)==0&&layout.signalAt(2)==8&&layout.signalAt(4)==10,
            "other headline signals retain their slots");
        check(layout.style(Telemetry.ACCEL_PEDAL)==1&&layout.style(9)==0,"pedal and throttle styles are separate");
        layout.cycle(Telemetry.ACCEL_PEDAL);
        check(layout.style(Telemetry.ACCEL_PEDAL)==0&&layout.style(9)==0,"editing pedal does not change throttle preference");
        check(layout.hit(left+width+3,top+height/2)==-1&&layout.hit(left,top-1)==-1,
            "header and inter-card gap remain outside touch targets");
        layout.pdProfile(false);
        check(layout.signalAt(3)==9&&layout.signalAt(10)==Telemetry.POWER&&layout.signalAt(16)==Telemetry.TCS
            &&layout.hit(left+width/2,top+height/2)==9&&layout.style(9)==0,
            "return to JSN restores throttle with original style");
        System.out.println("PASS "+checks+" PD overview layout checks");
    }
}
