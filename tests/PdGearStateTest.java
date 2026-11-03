import com.veloster.vehicle.PdGearState;

/** Transition, source-time and DCT7 boundaries. No vehicle or Android is required. */
public final class PdGearStateTest {
    private static int checks;
    private static byte[] frame(int raw){byte[] b=new byte[8];b[1]=(byte)raw;return b;}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void value(PdGearState.Result r,int expected,long time,String message){
        check(r.updated&&r.valid&&r.value==expected&&r.time==time,message);
    }
    public static void main(String[] args){
        PdGearState s=new PdGearState();
        PdGearState.Result r=s.receive(0x1e,frame(1),100);
        check(r.updated&&!r.valid,"current gear alone has no selector meaning");
        value(s.receive(0x1d,frame(0xc0),110),PdGearState.P,110,"confirmed P ignores high bits");
        value(s.receive(0x1e,frame(14),900),PdGearState.P,110,"P timestamp is not refreshed by current gear");
        value(s.receive(0x1d,frame(0xc6),1000),PdGearState.N,1000,"confirmed N transition");
        value(s.receive(0x1d,frame(7),1100),PdGearState.R,1100,"confirmed R transition");
        value(s.receive(0x1e,frame(14),1200),PdGearState.R,1100,"reverse raw14 never becomes forward14");
        value(s.receive(0x1d,frame(5),1300),PdGearState.D,1300,"D discards previous reverse current");
        value(s.receive(0x1e,frame(1),1400),101,1300,"new D1 pair uses oldest source time");
        value(s.receive(0x1d,frame(5),1500),101,1400,"repeated D uses recent actual gear without refreshing its time");
        value(s.receive(0x1e,frame(0xf7),1600),107,1500,"DCT7 permits seventh gear and ignores counter bits");
        value(s.receive(0x1e,frame(8),1700),PdGearState.D,1500,"eighth is unknown on this seven-speed car");
        value(s.receive(0x1e,frame(0),1800),PdGearState.D,1500,"unknown zero cannot reuse previously received seventh");
        value(s.receive(0x1e,frame(1),2500),101,1500,"pair accepts exactly1000ms boundary");
        value(s.receive(0x1e,frame(2),2501),PdGearState.D,1500,"expired selector cannot create a numeric pair");
        value(s.receive(0x1d,frame(5),3501),102,2501,"pair accepts current exactly1000ms old");
        value(s.receive(0x1d,frame(5),3502),PdGearState.D,3502,"expired current falls back to freshly received D");
        value(s.receive(0x1e,frame(3),3600),103,3502,"fresh third gear after expiry");
        value(s.receive(0x1d,frame(6),3700),PdGearState.N,3700,"N clears old third gear immediately");
        value(s.receive(0x1d,frame(5),3800),PdGearState.D,3800,"new D epoch cannot reuse previous third gear");
        value(s.receive(0x1e,frame(3),3800),103,3800,"same-batch timestamp pair is valid");
        r=s.receive(0x1e,frame(7),3799);
        check(!r.updated&&!r.valid&&r.time<=3799,"late receipt cannot replace state or expose future time");
        value(s.receive(0x1d,frame(5),3900),103,3800,"ignored late receipt did not mutate third gear");
        r=s.receive(0x1d,frame(15),4000);
        check(r.updated&&!r.valid&&r.time==4000,"unknown selector invalidates immediately");
        r=s.receive(0x1e,frame(1),4100);
        check(r.updated&&!r.valid&&r.time==4000,"current alone cannot revive unknown selector");
        for(int mode:new int[]{4,8,12}){
            long at=5000+mode*10;
            value(s.receive(0x1d,frame(mode),at),PdGearState.M,at,"new manual-mode epoch needs a new current receipt");
            value(s.receive(0x1e,frame(2),at+1),102,at,"manual mode supports received second gear");
        }
        check(!s.receive(8,frame(1),100000).updated,"wrong function ignored");
        check(!s.receive(0x1d,new byte[7],100000).updated,"wrong length ignored");
        check(!s.receive(0x1e,null,100000).updated,"null ignored");
        check(!s.receive(0x1e,frame(1),-1).updated,"negative time ignored");
        s.clear();
        r=s.receive(0x1e,frame(7),10);
        check(r.updated&&!r.valid,"clear removes selector and accepts a new generation clock");
        value(s.receive(0x1d,frame(5),11),PdGearState.D,11,"clear plus new selector cannot reuse pre-selector current");
        value(s.receive(0x1e,frame(1),12),101,11,"new generation can form a fresh pair");
        value(s.receive(0x1d,frame(0),13),PdGearState.P,13,"Park overrides previous forward gear");
        value(s.receive(0x1e,frame(7),70000),PdGearState.P,13,"unrelated current cannot refresh an expired Park receipt");
        check(!s.receive(0x1d,frame(0),69999).updated,"out-of-order selector does not refresh expired Park");
        System.out.println("PASS "+checks+" PD gear state checks");
    }
}
