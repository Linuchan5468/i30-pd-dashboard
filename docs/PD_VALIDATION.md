# PD validation scope

Target: one Korean i30 PD N Line DCT7 / Gen5 API17 / `pd_kr` / `eng.vmadmin.20260114.084130`. No year, engine, regional or firmware-wide compatibility is inferred. Initial evidence dates are2026-10-29; public preparation is2026-11-03. Updated vehicle confirmations were supplied by the owner on2026-11-03; the dates of those later tests were not specified.

Independent PD v1.0.0 (`1.0.0-pd`, versionCode12), built on2026-11-10, has offline signature/integrity and payload-comparison checks only. Its DEX and compiled resources match historical v11, with only the manifest version attributes changed. No new vehicle or emulator installation, update/settings-retention or cold-boot check was performed. The observations below remain attributed to the historical builds. See [current build provenance](PD_1.0.0_BUILD_PROVENANCE.json).

| Item | Evidence and limits |
|---|---|
| RPM/speed | Earlier PD versions displayed live RPM changes and parked speed0; recorded driving speed0–74.5km/h. This is not an accuracy calibration across the range. |
| Coolant/intake/ambient, throttle/pedal, brake/steering, acceleration | Received values and continuous updates observed in v8; owner confirmed brake/steering response while parked. Absolute sensor calibration and acceleration-axis road comparison remain incomplete. |
| Gear | Initial captures covered P/N/R/D and gears1–4. The owner subsequently confirmed P/R/N and all D1–D7 on the vehicle. Fresh selector/current-gear pairs; unknown numeric gear displays D/M. |
| TPMS | Captured psi values matched owner-reported parked cluster32/32/32/31. Unit1/2 conversions are host-only. Missing FF values remain unavailable. |
| TCS/ESC | Owner operation and recorded raw steps0/1/2 support ON/ON, OFF/ON, OFF/OFF. The owner subsequently confirmed individual TCS/ESC ON/OFF behavior on the vehicle, including TCS-only disable while ESC stays ON. This confirms enabled-state operation/display; it does not claim measurement of active stability/brake intervention. |
| ABS | Warning lamp only. No ABS intervention/safety operation validation. |
| Torque/power/boost | Recorded-source replay: reference torque is received via mux3; reference maximum interval1868ms motivated a3s window. Pressure pair window1s. Estimated net torque/power and pressure differences, not independently measured engine output. Recorded boost approximately-0.032–1.210bar. |
| v11 UI/install | Installed APK readback hash matched; preferences preserved; RPM3000 lower clamp and restart persistence verified; both F1 name sizes visually checked, original target6000 restored. Ignition was off with AVN powered. |
| Cold boot/settings | The owner confirmed that the app runs after a cold boot and saved settings are retained. This does not assert automatic full-screen launch at boot. Extended-duration operation has no new measured result. |
| e-LSD | Not fitted to the confirmed vehicle; unavailable. |

Oil temperature could not be implemented for this vehicle because no usable value was found in the AVN receive path. A previous OBD instrument reportedly read around92°C while the examined AVN oil field remained raw0; that report is not substituted as an app reading. Valid battery data, wheel speeds and actual ABS intervention remain unestablished. The confirmed vehicle is not equipped with e-LSD. Conservative sentinel/status/range policies in source are not a complete OEM enum specification.

The retained drive capture had808s of complete frames before collection stopped before its900s limit. Complete frames were replayed with recorded receipt times; this is partial-session evidence, not a successful full-duration capture. Raw captures, system/device logs, settings and vehicle identifiers are not public. The sanitized [VALIDATION.json](../VALIDATION.json) describes aggregate results; it is not a substitute for new vehicle validation.

Mapping research cross-checked the OEM PD receive structure with [opendbc Hyundai CCAN definitions](https://github.com/commaai/opendbc/blob/master/opendbc/dbc/hyundai_2015_ccan.dbc), [OPKR TPMS interpretation](https://github.com/openpilotkr/openpilot/blob/3ff6dc6081c2a986c75292eefa0952641ab66b6f/selfdrive/car/hyundai/carstate.py), and a [Hyundai PDe owner manual](https://dmassets.hyundai.com/is/content/hyundaiautoever/PDe_2025_en_UKpdf) for the functional two-stage ESC distinction. That2025 UK manual does not define this Korean vehicle's CAN enum. No DBC, third-party source implementation or OEM decompiled implementation is bundled in the PD additions. See [NOTICE](../NOTICE.md).
