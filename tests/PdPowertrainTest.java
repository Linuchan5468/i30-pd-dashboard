import com.veloster.vehicle.PdPowertrain;

/** Source-driven PD calculations, temporal pairing and unit conversion boundaries. */
public final class PdPowertrainTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<0.0001f,message);}
    private static byte[] std(int raw){byte[] b=new byte[8];b[0]=(byte)(0xc0|raw);return b;}
    private static byte[] rpm(){byte[] b=new byte[8];b[0]=5;b[1]=(byte)128;b[2]=(byte)0xe0;b[3]=0x2e;return b;}
    private static byte[] ambient(int raw){byte[] b=new byte[8];b[0]=(byte)(raw<<3);return b;}
    private static byte[] pressure(int raw){byte[] b=new byte[8];b[2]=(byte)((raw&15)<<4);b[3]=(byte)(raw>>4);return b;}
    public static void main(String[] args){
        PdPowertrain p=new PdPowertrain();PdPowertrain.Result r;
        r=p.receive(8,rpm(),100);check(r.torqueUpdated&&!r.torqueValid,"wait for actual standard torque");
        r=p.receive(9,std(20),200);check(!r.torqueUpdated&&!r.boostUpdated,"standard alone does not publish");
        r=p.receive(8,rpm(),250);check(r.torqueValid&&r.torqueAt==200,"oldest actual source timestamp");
        near(r.torqueNm,100,"50percent of200Nm");near(r.powerPs,42.7137728f,"3000rpm100Nm converted to metric horsepower");
        byte[] b=rpm();b[1]=0;b[5]=(byte)128;r=p.receive(8,b,300);
        near(r.torqueNm,-100,"negative net torque retained");near(r.powerPs,-42.7137728f,"negative power retained");
        r=p.receive(8,rpm(),2068);check(r.torqueValid&&r.torqueAt==200,"observed1868ms multiplex gap retains original source age");
        r=p.receive(8,rpm(),3200);check(r.torqueValid&&r.torqueAt==200,"three-second standard boundary inclusive without refresh");
        p.receive(9,new byte[8],3201);r=p.receive(8,rpm(),3202);
        check(!r.torqueValid,"nonstandard multiplexer never refreshes standard age");
        p.clear();
        p.receive(9,std(20),1300);
        for(int flag:new int[]{2,16,32,128}){b=rpm();b[0]|=flag;r=p.receive(8,b,1301);check(!r.torqueValid,"unobserved torque flag withheld "+flag);}
        b=rpm();b[1]=(byte)255;check(!p.receive(8,b,1302).torqueValid,"unknown ACOR boundary withheld");
        b=rpm();b[5]=(byte)255;check(!p.receive(8,b,1302).torqueValid,"unknown friction boundary withheld");
        b=rpm();b[2]=(byte)255;b[3]=(byte)255;check(!p.receive(8,b,1302).torqueValid,"unknown RPM withheld");
        r=p.receive(9,std(0),1303);check(r.torqueUpdated&&!r.torqueValid,"invalid standard immediately invalidates");
        check(!p.receive(8,rpm(),1304).torqueValid,"invalid standard cannot be reused");
        r=p.receive(9,std(63),1305);check(r.torqueUpdated&&!r.torqueValid,"unknown standard boundary withheld");
        p.receive(9,std(20),1400);r=p.receive(8,rpm(),1399);
        check(r.torqueUpdated&&!r.torqueValid,"future-relative/out-of-order RPM withheld");
        check(p.receive(8,rpm(),1401).torqueValid,"later chronological RPM can use current standard");
        r=p.receive(9,std(20),1390);check(r.torqueUpdated&&!r.torqueValid,"out-of-order standard invalidates source");
        check(!p.receive(8,rpm(),1402).torqueValid,"out-of-order standard not cached");
        r=p.receive(0x0d,pressure(800),1500);check(r.boostUpdated&&!r.boostValid,"wait for actual ambient pressure");
        r=p.receive(0x0a,ambient(27),1600);check(!r.boostUpdated,"ambient alone does not publish");
        r=p.receive(0x0d,pressure(800),1650);check(r.boostValid&&r.boostAt==1600,"boost oldest source timestamp");
        near(r.boostBar,0.0593705544f,"absolute hPa minus converted mmHg gives bar");
        r=p.receive(0x0d,pressure(700),1700);near(r.boostBar,-0.0728294456f,"vacuum retained rather than invented zero");
        check(p.receive(0x0d,pressure(800),2600).boostValid,"boost boundary inclusive");
        check(!p.receive(0x0d,pressure(800),2601).boostValid,"expired ambient invalidates boost");
        p.receive(0x0a,ambient(27),2700);check(!p.receive(0x0d,pressure(4095),2701).boostValid,"pressure all-ones boundary withheld");
        r=p.receive(0x0a,ambient(31),2702);check(r.boostUpdated&&!r.boostValid,"invalid ambient immediately invalidates");
        check(!p.receive(0x0d,pressure(800),2703).boostValid,"invalid ambient not reused");
        p.receive(0x0a,ambient(27),2800);check(!p.receive(0x0d,pressure(800),2799).boostValid,"out-of-order pressure withheld");
        p.receive(9,std(20),2900);p.receive(0x0a,ambient(27),2900);p.clear();
        check(!p.receive(8,rpm(),1).torqueValid&&!p.receive(0x0d,pressure(800),1).boostValid,"ignition clear resets sources and clock floor");
        r=p.receive(8,new byte[7],2);check(!r.torqueUpdated&&!r.boostUpdated,"wrong lengths ignored");
        r=p.receive(0x55,new byte[8],3);check(!r.torqueUpdated&&!r.boostUpdated,"unknown function ignored");
        System.out.println("PASS "+checks+" PD powertrain checks");
    }
}
