package com.veloster.vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/** Circuit list plus numbered course selection, matching the native Navi flow. */
public final class CircuitPicker {
    public static final int NONE=0,CONSUMED=1,SELECTED=2,CLOSED=3;
    public static final int TRACK_ROWS=4,COURSE_BUTTONS=5;
    public static final class Group {
        public final String id,name;
        private final LapCircuit[] courses;
        private Group(String key,ArrayList<LapCircuit> list){id=key;courses=list.toArray(new LapCircuit[list.size()]);name=courses[0].name;}
        public int count(){return courses.length;}
        public LapCircuit course(int index){return index>=0&&index<courses.length?courses[index]:null;}
    }
    private final Group[] groups;
    private boolean open,sortName;
    private int groupIndex,courseIndex,trackPage,coursePage;
    public CircuitPicker(LapCircuit[] catalog){
        LinkedHashMap<String,ArrayList<LapCircuit>> grouped=new LinkedHashMap<String,ArrayList<LapCircuit>>();
        if(catalog!=null)for(LapCircuit c:catalog){
            int separator=c.id.lastIndexOf('_');String key=separator>0?c.id.substring(0,separator):c.name;
            ArrayList<LapCircuit> courses=grouped.get(key);if(courses==null){courses=new ArrayList<LapCircuit>();grouped.put(key,courses);}courses.add(c);
        }
        ArrayList<Group> all=new ArrayList<Group>();for(String key:grouped.keySet())all.add(new Group(key,grouped.get(key)));
        groups=all.toArray(new Group[all.size()]);
    }
    public void open(String selectedId){
        open=true;
        for(int i=0;i<groups.length;i++)for(int j=0;j<groups[i].count();j++)if(groups[i].course(j).id.equals(selectedId)){groupIndex=i;courseIndex=j;}
        coursePage=courseIndex/COURSE_BUTTONS;
        int at=0;for(Group g:ordered()){if(g==group())break;at++;}trackPage=at/TRACK_ROWS;
    }
    public boolean isOpen(){return open;}
    public void close(){open=false;}
    public boolean nameSorted(){return sortName;}
    public int trackPage(){return trackPage;}
    public int trackPages(){return Math.max(1,(groups.length+TRACK_ROWS-1)/TRACK_ROWS);}
    public int coursePage(){return coursePage;}
    public int courseIndex(){return courseIndex;}
    public Group group(){return groups.length==0?null:groups[groupIndex];}
    public LapCircuit selected(){return group()==null?null:group().course(courseIndex);}
    public int coursePages(){return group()==null?1:Math.max(1,(group().count()+COURSE_BUTTONS-1)/COURSE_BUTTONS);}
    public Group[] visibleGroups(){Group[] order=ordered();int first=trackPage*TRACK_ROWS,end=Math.min(order.length,first+TRACK_ROWS);return Arrays.copyOfRange(order,Math.min(first,end),end);}
    private Group[] ordered(){Group[] order=groups.clone();if(sortName)Arrays.sort(order,new Comparator<Group>(){public int compare(Group a,Group b){return a.name.compareToIgnoreCase(b.name);}});return order;}
    /** Canvas coordinates; all taps are consumed while this modal content is open. */
    public int touch(float x,float y){
        if(!open)return NONE;
        if(x>=654&&x<782&&y>=58&&y<96){open=false;return CLOSED;}
        if(x>=154&&x<278&&y>=58&&y<96){sortName=!sortName;trackPage=0;return CONSUMED;}
        if(x>=18&&x<278&&y>=108&&y<376){
            int row=(int)((y-108)/68);if(row<TRACK_ROWS&&y<108+row*68+64){
                Group[] visible=visibleGroups();if(row<visible.length){
                    Group chosen=visible[row];if(chosen!=group()){for(int i=0;i<groups.length;i++)if(groups[i]==chosen)groupIndex=i;courseIndex=0;coursePage=0;return SELECTED;}
                }
            }return CONSUMED;
        }
        if(y>=414&&y<458){
            if(x>=18&&x<140&&trackPage>0)trackPage--;
            else if(x>=152&&x<278&&trackPage+1<trackPages())trackPage++;
            return CONSUMED;
        }
        if(y>=358&&y<405){
            if(x>=290&&x<332){if(coursePage>0)coursePage--;return CONSUMED;}
            if(x>=740&&x<782){if(coursePage+1<coursePages())coursePage++;return CONSUMED;}
            for(int slot=0;slot<COURSE_BUTTONS;slot++)if(x>=342+slot*72&&x<402+slot*72){
                int index=coursePage*COURSE_BUTTONS+slot;
                if(group()!=null&&index<group().count()&&index!=courseIndex){courseIndex=index;return SELECTED;}
            }
        }
        return CONSUMED;
    }
}
