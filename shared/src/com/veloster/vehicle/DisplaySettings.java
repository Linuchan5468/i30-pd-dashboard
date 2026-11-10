package com.veloster.vehicle;

import com.veloster.i18n.LanguageSettings;
import com.veloster.i18n.UiText;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

/** Per-app display settings; never changes vehicle units or telemetry samples. */
public final class DisplaySettings {
    private final Activity activity;
    private final Runnable changed;
    private AlertDialog dialog;
    public DisplaySettings(Activity activity,Runnable changed){this.activity=activity;this.changed=changed;}
    private static SharedPreferences prefs(Context context){return context.getSharedPreferences("pit_vehicle_ui",0);}
    public static TorqueUnit torqueUnit(Context context){
        try{return TorqueUnit.fromPreference(prefs(context).getString("torque_unit","nm"));}
        catch(ClassCastException e){return TorqueUnit.NEWTON_METRE;}
    }
    public void dismiss(){AlertDialog old=dialog;dialog=null;if(old!=null)old.dismiss();}
    private void show(AlertDialog next){dismiss();dialog=next;next.show();}
    public void show(){
        if(activity.isFinishing())return;
        show(new AlertDialog.Builder(activity).setTitle(UiText.t("표시 설정"))
            .setItems(new String[]{UiText.t("토크 단위")+" · "+torqueUnit(activity).label(),"Language / 언어"},new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface d,int which){
                    if(which==0)showTorque();else{dismiss();LanguageSettings.show(activity,changed);}
                }
            }).setNegativeButton(UiText.t("취소"),null).create());
    }
    private void showTorque(){
        final TorqueUnit[] modes={TorqueUnit.NEWTON_METRE,TorqueUnit.KILOGRAM_FORCE_METRE};
        final int[] selected={torqueUnit(activity)==TorqueUnit.KILOGRAM_FORCE_METRE?1:0};
        show(new AlertDialog.Builder(activity).setTitle(UiText.t("토크 단위"))
            .setSingleChoiceItems(new String[]{UiText.t("뉴턴미터 (N·m)"),UiText.t("킬로그램힘미터 (kgf·m)")},selected[0],new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface d,int which){selected[0]=which;}
            }).setPositiveButton(UiText.t("저장"),new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface d,int which){
                    TorqueUnit previous=torqueUnit(activity);
                    if(prefs(activity).edit().putString("torque_unit",modes[selected[0]].preference()).commit())changed.run();
                    else{
                        // commit can update the in-memory map even when disk persistence fails.
                        prefs(activity).edit().putString("torque_unit",previous.preference()).commit();
                        changed.run();
                        show(new AlertDialog.Builder(activity).setMessage(UiText.t("표시 설정을 저장하지 못했습니다.")).setPositiveButton(UiText.t("확인"),null).create());
                    }
                }
            }).setNegativeButton(UiText.t("취소"),null).create());
    }
}
