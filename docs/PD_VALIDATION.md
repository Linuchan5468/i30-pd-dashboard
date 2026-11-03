# PD validation scope

Target: one Korean i30 PD N Line DCT7 / Gen5 API17 / `pd_kr` / `eng.vmadmin.20260114.084130`. No year, engine, regional or firmware-wide compatibility is inferred. Evidence dates are2026-10-29; public preparation is2026-11-03.

| Item | Evidence and limits |
|---|---|
| RPM/speed | Earlier PD versions displayed live RPM changes and parked speed0; recorded driving speed0–74.5km/h. This is not an accuracy calibration across the range. |
| Coolant/intake/ambient, throttle/pedal, brake/steering, acceleration | Received values and continuous updates observed in v8; owner confirmed brake/steering response while parked. Absolute sensor calibration and acceleration-axis road comparison remain incomplete. |
| Gear | P/N/R/D, stationary sequential1 and driving gears1–4 observed; 5–7 synthetic tests only. Fresh selector/current-gear pairs; unknown numeric gear displays D/M. |
| TPMS | Captured psi values matched owner-reported parked cluster32/32/32/31. Unit1/2 conversions are host-only. Missing FF values remain unavailable. |
| TCS/ESC | Owner operation and recorded raw steps0/1/2 support ON/ON, OFF/ON, OFF/OFF. The final correction was replayed; no subsequent ignition-on UI comparison of final v11. This is enabled status, not intervention. |
| ABS | Warning lamp only. No ABS intervention/safety operation validation. |
| Torque/power/boost | Recorded-source replay: reference torque is received via mux3; reference maximum interval1868ms motivated a3s window. Pressure pair window1s. Estimated net torque/power and pressure differences, not independently measured engine output. Recorded boost approximately-0.032–1.210bar. |
| v11 UI/install | Installed APK readback hash matched; preferences preserved; RPM3000 lower clamp and restart persistence verified; both F1 name sizes visually checked, original target6000 restored. Ignition was off with AVN powered. |
| Cold boot/extended final operation | Final v11 subsequent ignition-on/cold boot and long-term vehicle use not verified. QEMU work on OEM firmware is not PD dashboard vehicle validation. |

A previous OBD instrument reportedly read oil around92°C while the examined AVN oil field remained raw0. No value is substituted from that report. Oil, valid battery data, wheel speed, e-LSD and real ABS/TCS/ESC intervention remain unestablished. Conservative sentinel/status/range policies in source are not a complete OEM enum specification.

The retained drive capture had808s of complete frames before collection stopped before its900s limit. Complete frames were replayed with recorded receipt times; this is partial-session evidence, not a successful full-duration capture. Raw captures, system/device logs, settings and vehicle identifiers are not public. The sanitized [VALIDATION.json](../VALIDATION.json) describes aggregate results; it is not a substitute for new vehicle validation.

Mapping research cross-checked the OEM PD receive structure with [opendbc Hyundai CCAN definitions](https://github.com/commaai/opendbc/blob/master/opendbc/dbc/hyundai_2015_ccan.dbc), [OPKR TPMS interpretation](https://github.com/openpilotkr/openpilot/blob/3ff6dc6081c2a986c75292eefa0952641ab66b6f/selfdrive/car/hyundai/carstate.py), and a [Hyundai PDe owner manual](https://dmassets.hyundai.com/is/content/hyundaiautoever/PDe_2025_en_UKpdf) for the functional two-stage ESC distinction. That2025 UK manual does not define this Korean vehicle's CAN enum. No DBC, third-party source implementation or OEM decompiled implementation is bundled in the PD additions. See [NOTICE](../NOTICE.md).
