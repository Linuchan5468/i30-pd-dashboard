import com.veloster.vehicle.Cn7CircuitRoutes;
import java.util.HashSet;

/** Exact source geometry, separate course selections and independent links. */
public final class Cn7CircuitRoutesTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void near(double actual,double expected,String why){check(Math.abs(actual-expected)<1e-12,why);}
    public static void main(String[] args){
        Cn7CircuitRoutes.Route[] routes=Cn7CircuitRoutes.korea();
        String[] ids={"kor_41000000_41010000","kor_41000000_41010001","kor_41000001_41010100","kor_41000001_41010101","kor_41000002_41010200","kor_41000003_41010300"};
        int[] links={28,11,20,14,8,16},points={363,166,306,208,159,315},segments={335,155,286,194,151,299};
        check(routes.length==6,"six verified Korean course maps");int total=0;
        for(int c=0;c<routes.length;c++){
            Cn7CircuitRoutes.Route route=routes[c];check(route==Cn7CircuitRoutes.forId(ids[c]),"stable cached route identity");
            check(route.id.equals(ids[c])&&route.sourceCourseId==Integer.parseInt(ids[c].substring(ids[c].lastIndexOf('_')+1)),"source course identity");
            check(route.linkCount==links[c]&&route.pointCount==points[c],"exact course-specific source link/point counts");
            check(route.segmentCount==segments[c]&&route.segmentCount==route.pointCount-route.linkCount,"only within-link segments; no joining or closing links");
            check(route.sourceSHA256.equals(Cn7CircuitRoutes.SOURCE_SHA256),"source binary provenance");
            check(route.minLatitude<route.maxLatitude&&route.minLongitude<route.maxLongitude,"source-linked projection bounds");
            HashSet<Integer> sourceIds=new HashSet<Integer>();int actualPoints=0;
            for(int i=0;i<route.linkCount;i++){
                Cn7CircuitRoutes.Link link=route.link(i);check(sourceIds.add(link.sourceLinkId),"source link appears once per course");
                check(link.pointCount()>=2&&link.type==(link.flags&31),"source type and independent polyline");
                check(link.sourceShapeRecordOffset>0&&link.sourceCourseLinkRecordOffset>0,"each line maps to original binary records");
                actualPoints+=link.pointCount();
                for(int p=0;p<link.pointCount();p++){
                    check(link.latitude(p)>=route.minLatitude&&link.latitude(p)<=route.maxLatitude
                        &&link.longitude(p)>=route.minLongitude&&link.longitude(p)<=route.maxLongitude,"every source point inside its computed route bounds");
                }
            }
            check(actualPoints==route.pointCount,"no source points dropped or added");total+=actualPoints;
        }
        check(total==1517,"all1517 selected source point references preserved");
        check(Cn7CircuitRoutes.forId(null)==null&&Cn7CircuitRoutes.forId("usa_84000001_1")==null,"unsupported geometry never falls back to AREA");
        check(routes[0].link(0).sourceLinkId==2656498&&routes[0].link(0).pointCount()==13,"exact KIC GP first link");
        near(routes[0].link(0).latitude(0),34.731116666666665,"original latitude conversion");
        near(routes[0].link(0).longitude(0),126.41657222222223,"original longitude conversion");
        check(routes[0].link(0).sourceLinkId!=routes[1].link(0).sourceLinkId,"KIC GP/Standing retain distinct selections");
        HashSet<Integer> injeA=new HashSet<Integer>();for(int i=0;i<routes[3].linkCount;i++)injeA.add(routes[3].link(i).sourceLinkId);
        boolean fullOnly=false;for(int i=0;i<routes[2].linkCount;i++)if(!injeA.contains(routes[2].link(i).sourceLinkId))fullOnly=true;
        check(fullOnly,"Inje FULL/A actual link selections differ");
        routes[0]=null;check(Cn7CircuitRoutes.korea()[0]!=null,"array access cannot mutate stored routes");
        System.out.println("PASS "+checks+" actual COURSE link geometry checks");
    }
}
