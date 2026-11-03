import com.veloster.vehicle.Telemetry;

/** PD-specific byte positions, units and expiry; fixtures contain no vehicle identity. */
public final class PdTelemetryTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void value(Telemetry t,int signal,float expected,long now,String message){
        Telemetry.Sample s=t.sample(signal,now);
        check(s.valid&&Math.abs(s.value-expected)<0.001f,message);
    }
    public static void main(String[] args){
        Telemetry t=new Telemetry();
        t.pdProfile(true);
        check(t.pdProfile()&&!t.sample(0,0).seen,"PD device UI is selected even without ignition data");
        // Decoder payload excludes the function byte present in the OEM parser input.
        byte[] ems={5,13,(byte)0xd2,9,13,12,0,127};
        t.hscanPd(0xc7,8,ems,1000);
        value(t,0,628.5f,1000,"EMS11 RPM uses payload2/3, not3/4");
        check(!t.sample(7,1000).seen,"RPM never creates speed");
        byte[] clu={4,0,0,0};
        t.hscanPd(0xc7,2,clu,1100);
        value(t,7,0,1100,"stationary zero needs no NMode unit event");
        clu[1]=120;t.hscanPd(0xc7,2,clu,1200);
        value(t,7,60,1200,"km/h half-unit scale");
        clu[2]=2;t.hscanPd(0xc7,2,clu,1300);
        value(t,7,96.56064f,1300,"mph normalized to km/h once");
        clu[1]=(byte)255;clu[2]=1;t.hscanPd(0xc7,2,clu,1400);
        check(!t.sample(7,1400).valid,"OEM speed error raw511 withheld");
        ems[2]=(byte)255;ems[3]=(byte)255;t.hscanPd(0xc7,8,ems,1500);
        check(!t.sample(0,1500).valid,"RPM sentinel withheld conservatively");
        t.clear();t.hscanPd(0xc8,8,new byte[8],2000);t.hscanPd(0xc7,8,new byte[7],2000);
        t.hscanPd(0xc7,7,new byte[8],2000);t.hscanPd(0xc7,2,new byte[8],2000);
        check(!t.sample(0,2000).seen&&!t.sample(7,2000).seen,"wrong cid function and lengths ignored");
        t.hscanPd(0xc7,8,new byte[8],3000);t.hscanPd(0xc7,2,new byte[4],3000);
        value(t,0,0,3000,"received engine-off zero is valid");
        value(t,7,0,62999,"received zero retains normal freshness");
        check(!t.sample(0,63000).valid&&!t.sample(7,63000).valid,"stopped stream expires at60seconds");
        t.clear();check(!t.sample(0,63001).seen&&!t.sample(7,63001).seen,"ignition clear removes receipts");
        check(t.pdProfile(),"PD profile survives ignition clear on same device");
        byte[] ems12={0,(byte)176,0,0,0,38,64,0};
        t.hscanPd(0xc7,9,ems12,64000);
        value(t,2,84,64000,"coolant is distinct from oil");
        value(t,Telemetry.ACCEL_PEDAL,24.9984f,64000,"pedal uses payload6 not throttle payload5");
        value(t,9,2.8169026f,64000,"EMS12 actual throttle DBC scale");
        ems12[5]=(byte)245;t.hscanPd(0xc7,9,ems12,64001);value(t,9,100,64001,"TPS endpoint roundoff only");
        ems12[5]=31;ems12[1]=(byte)255;ems12[6]=(byte)255;t.hscanPd(0xc7,9,ems12,64002);
        check(!t.sample(2,64002).valid&&!t.sample(9,64002).valid&&!t.sample(Telemetry.ACCEL_PEDAL,64002).valid,"invalid temperature pedal and TPS withheld");
        byte[] intake=new byte[8];intake[2]=120;t.hscanPd(0xc7,0x0b,intake,64100);value(t,Telemetry.INTAKE,42,64100,"EMS15 intake byte2");
        intake[2]=(byte)255;t.hscanPd(0xc7,0x0b,intake,64101);check(!t.sample(Telemetry.INTAKE,64101).valid,"IAT sentinel withheld");
        byte[] oil=new byte[8];t.hscanPd(0xc7,0x0d,oil,64200);check(!t.sample(1,64200).valid,"constant unsupported oil zero is not measured -40");
        oil[4]=(byte)160;t.hscanPd(0xc7,0x0d,oil,64201);value(t,1,80,64201,"nonzero oil uses its own scale");
        byte[] ambient=new byte[8];ambient[3]=(byte)134;t.hscanPd(0xc7,0x14,ambient,64300);value(t,Telemetry.AMBIENT,27,64300,"FATC outside Celsius");
        byte[] battery={0,0,76,61,32,0,0,0};t.hscanPd(0xc7,0,battery,64400);
        value(t,Telemetry.BAT_SOC,76,64400,"battery percent with clear invalid flags");value(t,Telemetry.BAT_VOLTAGE,14.253f,64400,"battery LE14 millivolts and offset");
        battery[6]=(byte)128;t.hscanPd(0xc7,0,battery,64401);
        check(!t.sample(Telemetry.BAT_SOC,64401).valid&&!t.sample(Telemetry.BAT_VOLTAGE,64401).valid,"battery invalid flag cannot be ignored");
        battery[6]=0;battery[7]=(byte)128;t.hscanPd(0xc7,0,battery,64402);check(!t.sample(Telemetry.BAT_VOLTAGE,64402).valid,"battery error flag cannot be ignored");
        byte[] sas={(byte)206,(byte)255,2,7,0};t.hscanPd(0xc7,0x19,sas,64500);
        value(t,Telemetry.STEERING,-5,64500,"SAS signed LE angle");value(t,Telemetry.STEERING_SPEED,8,64500,"SAS unsigned magnitude rate");
        sas[0]=(byte)255;sas[1]=(byte)255;t.hscanPd(0xc7,0x19,sas,64501);value(t,Telemetry.STEERING,-0.1f,64501,"FFFF angle is legitimate negative tenth");
        sas[0]=0;sas[1]=0;t.hscanPd(0xc7,0x19,sas,64502);value(t,Telemetry.STEERING,0,64502,"new center value replaces prior angle");
        sas[3]=0;t.hscanPd(0xc7,0x19,sas,64503);check(!t.sample(Telemetry.STEERING,64503).valid,"unverified SAS state withheld");
        byte[] esp={(byte)255,(byte)0xe3,0x7f,0x30,0,0,0,0};t.hscanPd(0xc7,0x13,esp,64600);
        value(t,10,1.2f,64600,"pressure packed12 bit offset26");value(t,11,0,64600,"zero lateral in G");value(t,12,0,64600,"zero longitudinal in G");
        esp[1]|=8;t.hscanPd(0xc7,0x13,esp,64601);check(!t.sample(11,64601).valid&&t.sample(12,64601).valid,"lateral status invalidates only its channel");
        esp[3]|=2;esp[4]|=(byte)128;t.hscanPd(0xc7,0x13,esp,64602);
        check(!t.sample(12,64602).valid&&!t.sample(10,64602).valid,"longitudinal and pressure diagnostic flags withheld");
        check(!t.sample(8,64602).seen&&!t.sample(Telemetry.TPMS_FL,64602).seen&&!t.sample(Telemetry.POWER,64602).valid,"extra packets never invent gear pressures or power");
        check(!t.sample(Telemetry.AMBIENT,124300).valid,"extra channels retain sixty-second expiry");
        t.hscanPd(0xc7,0x1c,new byte[]{0,0,0,1},125000);
        value(t,Telemetry.PD_TCS_ENABLED,1,125000,"TCS OFF lamp clear means enabled");
        value(t,Telemetry.PD_ESC_ENABLED,1,125000,"ESC normal step means enabled");
        value(t,Telemetry.PD_ABS_WARNING,0,125000,"ABS warning clear is not ABS disabled");
        t.hscanPd(0xc7,0x1c,new byte[]{2,1,0,1},125000);
        value(t,Telemetry.PD_TCS_ENABLED,0,125000,"traction-only disabled");
        value(t,Telemetry.PD_ESC_ENABLED,1,125000,"traction-only disable leaves ESC enabled");
        t.hscanPd(0xc7,0x1c,new byte[]{2,2,0,1},125001);
        value(t,Telemetry.PD_TCS_ENABLED,0,125001,"physical user-disabled TCS capture");
        value(t,Telemetry.PD_ESC_ENABLED,0,125001,"physical user-disabled ESC capture");
        t.hscanPd(0xc7,0x1c,new byte[]{(byte)255,(byte)255,(byte)255,(byte)255},125002);
        check(!t.sample(Telemetry.PD_ABS_WARNING,125002).valid,"allFF status is not an ABS warning");
        t.hscanPd(0xc7,0x1d,new byte[8],126000);
        check("P".equals(Telemetry.gearLabel((int)t.sample(8,126000).value)),"selector label remains distinct from numbered gear");
        Telemetry.Series gearHistory=t.history(8,126000,60000);
        check(!gearHistory.valid[gearHistory.size()-1],"park is not a negative gear point on graphs");
        check("D7".equals(Telemetry.gearLabel(107))&&"—".equals(Telemetry.gearLabel(108)),"DCT7 maximum and displayed prefix");
        t.hscanPd(0xc7,0x20,new byte[]{0,0,(byte)255,(byte)255,(byte)255,(byte)255},127000);
        check(!t.sample(Telemetry.TPMS_FL,127000).valid,"stationary unreported pressures are not zero PSI");
        t.hscanPd(0xc7,0x20,new byte[]{0,1,33,34,32,35},127001);
        value(t,Telemetry.TPMS_FL,33,127001,"PD PSI front left");value(t,Telemetry.TPMS_FR,34,127001,"PD PSI front right");
        value(t,Telemetry.TPMS_RL,32,127001,"PD PSI rear left");value(t,Telemetry.TPMS_RR,35,127001,"PD PSI rear right");
        t.hscanPd(0xc7,0x20,new byte[]{0,1,33,(byte)255,32,35},127002);
        check(t.sample(Telemetry.TPMS_FL,127002).valid&&!t.sample(Telemetry.TPMS_FR,127002).valid,"missing individual pressure never contaminates its neighbours");
        t.hscanPd(0xc7,0x20,new byte[]{0,9,45,45,45,45},127003);
        value(t,Telemetry.TPMS_FL,32.63349f,127003,"encoded5kPa is normalized to PSI");
        t.hscanPd(0xc7,0x20,new byte[]{0,17,23,23,23,23},127004);
        value(t,Telemetry.TPMS_FL,33.35868f,127004,"encoded0.1bar is normalized to PSI");
        t.hscanPd(0xc7,0x20,new byte[]{0,25,33,34,32,35},127005);
        check(!t.sample(Telemetry.TPMS_FL,127005).valid,"unknown pressure unit must not be labelled PSI");
        check(!t.sample(Telemetry.TPMS_RR,187005).valid,"pressure also expires without new receipt");
        System.out.println("PASS "+checks+" PD telemetry checks");
    }
}
