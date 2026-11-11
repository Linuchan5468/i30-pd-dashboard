import com.veloster.vehicle.CircuitPicker;
import com.veloster.vehicle.LapCircuit;
import com.veloster.vehicle.Cn7CircuitCatalog;

/** Canvas picker transitions and deterministic touch regions without Android. */
public final class CircuitPickerTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        LapCircuit[] source=Cn7CircuitCatalog.korea();CircuitPicker picker=new CircuitPicker(source);
        check(!picker.isOpen()&&picker.touch(140,140)==CircuitPicker.NONE,"closed picker does not intercept taps");
        picker.open(source[0].id);check(picker.isOpen(),"open explicit circuit list");
        check(picker.visibleGroups().length==4&&picker.trackPages()==1,"four actual Korean circuits on one page");
        check(picker.group().count()==2&&picker.selected().id.equals(source[0].id),"KIC selection has its two own courses");
        check(picker.touch(438,381)==CircuitPicker.SELECTED,"number2 touch selects a course");
        check(picker.selected().id.equals(source[1].id)&&picker.courseIndex()==1,"KIC Standing distinct from F1");
        check(picker.touch(140,140)==CircuitPicker.CONSUMED&&picker.courseIndex()==1,"selecting the same circuit does not reset course");
        check(picker.touch(370,381)==CircuitPicker.SELECTED&&picker.selected().id.equals(source[0].id),"number1 returns KIC F1");
        check(picker.touch(140,276)==CircuitPicker.SELECTED,"third row selects Taebaek");
        check(picker.group().name.equals("Taebaek Speedway")&&picker.group().count()==1&&picker.selected().id.equals(source[4].id),"Taebaek selection cannot cycle into another track");
        check(picker.touch(438,381)==CircuitPicker.CONSUMED&&picker.selected().id.equals(source[4].id),"nonexistent Taebaek course2 ignored");
        check(picker.touch(718,77)==CircuitPicker.CLOSED&&!picker.isOpen(),"Back closes without extra confirmation");
        picker.open(source[3].id);check(picker.group().name.equals("Inje Speedium")&&picker.courseIndex()==1,"reopen keeps actual selected Inje A");
        check(picker.touch(200,77)==CircuitPicker.CONSUMED&&picker.nameSorted(),"official name sort is separate from selection");
        check(picker.visibleGroups()[0].name.equals("AMG Speedway")&&picker.selected().id.equals(source[3].id),"name sorting preserves chosen circuit/course");
        check(picker.touch(140,140)==CircuitPicker.SELECTED&&picker.group().name.equals("AMG Speedway"),"sorted circuit row chooses the displayed circuit");
        check(picker.touch(10,470)==CircuitPicker.CONSUMED,"modal background cannot trigger underlying lap button");

        LapCircuit[] many=new LapCircuit[11];for(int i=0;i<many.length;i++)many[i]=new LapCircuit("test_1_"+i,"Synthetic only","Course"+i,500,(LapCircuit.Gate)null,null,"synthetic");
        picker=new CircuitPicker(many);picker.open(many[0].id);
        check(picker.coursePages()==3,"numbered courses page in groups of five");
        check(picker.touch(756,381)==CircuitPicker.CONSUMED&&picker.coursePage()==1,"course next arrow pages without selecting");
        check(picker.touch(370,381)==CircuitPicker.SELECTED&&picker.courseIndex()==5,"page2 number6 selects its actual course");
        picker.touch(756,381);check(picker.touch(370,381)==CircuitPicker.SELECTED&&picker.courseIndex()==10,"page3 number11 selects its actual course");
        picker.touch(756,381);check(picker.coursePage()==2,"course pagination does not wrap");
        picker.touch(310,381);check(picker.coursePage()==1&&picker.courseIndex()==10,"page arrow alone preserves current course");
        picker=new CircuitPicker(new LapCircuit[0]);picker.open(null);check(picker.selected()==null&&picker.visibleGroups().length==0,"empty catalog opens safely");
        check(picker.touch(140,140)==CircuitPicker.CONSUMED&&picker.touch(438,381)==CircuitPicker.CONSUMED,"empty catalog cannot select synthetic entry");
        System.out.println("PASS "+checks+" circuit picker checks");
    }
}
