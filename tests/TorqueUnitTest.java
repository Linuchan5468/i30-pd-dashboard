import com.veloster.vehicle.Telemetry;
import com.veloster.vehicle.TorqueUnit;
import java.util.Arrays;
import java.util.Locale;

/** Presentation conversions must never change received samples or receipt history. */
public final class TorqueUnitTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<0.0001f,message);}
    private static byte[] standard(){byte[] b=new byte[8];b[0]=(byte)(0xc0|20);return b;}
    private static byte[] engine(){byte[] b=new byte[8];b[0]=5;b[1]=(byte)128;b[2]=(byte)0xe0;b[3]=0x2e;return b;}

    public static void main(String[] args){
        TorqueUnit nm=TorqueUnit.NEWTON_METRE,kgfm=TorqueUnit.KILOGRAM_FORCE_METRE;
        check(TorqueUnit.fromPreference("nm")==nm,"saved newton metre selection");
        check(TorqueUnit.fromPreference("kgfm")==kgfm,"saved kilogram-force metre selection");
        for(String unknown:new String[]{null,"","other","KGFM"})
            check(TorqueUnit.fromPreference(unknown)==nm,"missing or incompatible selection defaults to N-m");
        check("nm".equals(nm.preference())&&"kgfm".equals(kgfm.preference()),"stable preference keys");
        check("N·m".equals(nm.label())&&"kgf·m".equals(kgfm.label()),"torque unit identifies force, not mass");
        check("N·m".equals(nm.unit(Telemetry.TORQUE))&&"kgf·m".equals(kgfm.unit(Telemetry.TORQUE)),"torque unit label follows selection");
        near(nm.fromNewtonMetres(350),350,"N-m values unchanged");
        near(kgfm.fromNewtonMetres(98.0665f),10,"standard gravity conversion");
        near(kgfm.fromNewtonMetres(9.80665f),1,"one kilogram-force metre");
        near(kgfm.fromNewtonMetres(-98.0665f),-10,"negative net torque retained");
        near(kgfm.fromNewtonMetres(0),0,"received zero remains zero");
        check("35.7".equals(String.format(Locale.US,"%.1f",kgfm.displayValue(Telemetry.TORQUE,350))),"350 N-m displays 35.7 kgf-m");
        for(TorqueUnit selected:TorqueUnit.values()){
            check(Float.isNaN(selected.fromNewtonMetres(Float.NaN)),"NaN does not become a measured zero");
            check(selected.fromNewtonMetres(Float.POSITIVE_INFINITY)==Float.POSITIVE_INFINITY,"positive infinity remains invalid");
            check(selected.fromNewtonMetres(Float.NEGATIVE_INFINITY)==Float.NEGATIVE_INFINITY,"negative infinity remains invalid");
            for(int i=0;i<Telemetry.LABELS.length;i++)if(i!=Telemetry.TORQUE){
                check(Telemetry.UNITS[i].equals(selected.unit(i)),"other unit labels unchanged "+i);
                near(selected.displayValue(i,25.5f),Telemetry.displayValue(i,25.5f),"other channels preserve display semantics "+i);
            }
            near(selected.displayValue(Telemetry.STEERING,-13.5f),13.5f,"steering orientation unchanged");
            near(selected.displayValue(8,107),7,"D7 numeric gauge position unchanged");
            check(Float.isNaN(selected.displayValue(8,-1)),"park gear has no invented numeric position");
        }

        Telemetry data=new Telemetry();data.pdProfile(true);
        check(!data.sample(Telemetry.TORQUE,1000).seen,"new model has no torque sample");
        kgfm.displayValue(Telemetry.TORQUE,data.sample(Telemetry.TORQUE,1000).value);
        check(!data.sample(Telemetry.TORQUE,1000).valid&&!data.sample(Telemetry.TORQUE,1000).seen,"unit conversion cannot create a receipt");
        data.hscanPd(0xc7,9,standard(),1000);data.hscanPd(0xc7,8,engine(),1100);
        Telemetry.Sample before=data.sample(Telemetry.TORQUE,1200);
        Telemetry.Series history=data.history(Telemetry.TORQUE,1200,60000);
        check(before.valid&&history.size()==1,"fixture creates one real torque receipt");
        near(before.value,100,"fixture source torque remains N-m");
        String diagnostic=data.diagnostics(1200);
        for(int n=0;n<1000;n++){
            TorqueUnit selected=n%2==0?kgfm:nm;
            selected.displayValue(Telemetry.TORQUE,before.value);
            for(float raw:history.values)selected.displayValue(Telemetry.TORQUE,raw);
        }
        Telemetry.Sample after=data.sample(Telemetry.TORQUE,1200);
        Telemetry.Series later=data.history(Telemetry.TORQUE,1200,60000);
        near(after.value,before.value,"repeated toggles do not accumulate conversion error in source");
        check(after.valid==before.valid&&after.seen==before.seen&&after.ageMs==before.ageMs&&after.stale==before.stale,"toggles do not refresh sample age or validity");
        check(Arrays.equals(later.values,history.values)&&Arrays.equals(later.times,history.times)
            &&Arrays.equals(later.valid,history.valid)&&Arrays.equals(later.breakBefore,history.breakBefore),"raw history and break markers unchanged");
        check(diagnostic.equals(data.diagnostics(1200)),"unit changes do not add receipt counts");
        check(!data.sample(Telemetry.TORQUE,61000).valid,"unit changes cannot stop normal expiry");
        data.clear();
        check(!data.sample(Telemetry.TORQUE,62000).seen&&data.history(Telemetry.TORQUE,62000,60000).size()==0,"ignition clear keeps data unavailable in either unit");
        System.out.println("PASS "+checks+" torque display unit checks");
    }
}
