package com.veloster.vehicle;

/** Display conversion only. Received samples and power calculations stay in N.m. */
public enum TorqueUnit {
    NEWTON_METRE("nm","N·m"),
    KILOGRAM_FORCE_METRE("kgfm","kgf·m");

    private final String preference,label;
    TorqueUnit(String preference,String label){this.preference=preference;this.label=label;}
    public String preference(){return preference;}
    public String label(){return label;}
    public static TorqueUnit fromPreference(String value){
        return "kgfm".equals(value)?KILOGRAM_FORCE_METRE:NEWTON_METRE;
    }
    public float fromNewtonMetres(float value){
        return this==KILOGRAM_FORCE_METRE?(float)(value/9.80665):value;
    }
    public float displayValue(int signal,float value){
        float displayed=Telemetry.displayValue(signal,value);
        return signal==Telemetry.TORQUE?fromNewtonMetres(displayed):displayed;
    }
    public String unit(int signal){return signal==Telemetry.TORQUE?label:Telemetry.UNITS[signal];}
}
