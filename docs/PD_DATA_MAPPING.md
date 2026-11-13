# PD data collection and mapping

**English** | [한국어](PD_DATA_MAPPING_ko.md) · [README](../README.md)

This document explains where the i30 PD mappings came from and how the app receives and interprets them. **The factory AVN parser from the confirmed PD vehicle is the primary source**, supported by the original app's receiver, public CAN definitions and vehicle observations. The adaptation did not apply a single Internet mapping table unchanged.

Reviewed on 2026-11-13 against [commit `7a6f92b`](https://github.com/Linuchan5468/i30-pd-dashboard/commit/7a6f92b9122f36879734663bdf00130d79927c3b). Writing this document involved no new vehicle connection or capture. See [derived values](#derived-values-and-display) for the distinction between current source and the published APK.

## Confirmed environment

The work covers **one Korean i30 PD N Line with a seven-speed DCT**, using its factory Gen5 AVN.

| Item | Confirmed value |
| --- | --- |
| Product | `pd_kr` |
| Android | 4.2.2 / API 17 |
| Incremental build | `eng.vmadmin.20260114.084130` |
| Firmware | `PD.KOR.SOP.V141.260114.STD_H` |

Other vehicles, years, regions, engines and AVN builds require separate confirmation. The receiver retains this exact product/build gate. [Validation scope](PD_VALIDATION.md).

## Sources

| Source | Use |
| --- | --- |
| [reikop/veloster-n-dashboard](https://github.com/reikop/veloster-n-dashboard) | Standalone Android app structure and HSCAN socket reception. The inherited JSN layout did not expose PD RPM/speed correctly, so a separate PD branch was added. |
| Factory `com.infobank.odex` obtained from the confirmed vehicle | Primary PD message table and parser evidence: internal function numbers, actual CAN IDs, lengths, byte/bit positions and conversions. OEM binaries and decompiled implementations are not distributed. |
| [commaai/opendbc Hyundai CCAN](https://github.com/commaai/opendbc/blob/master/opendbc/dbc/hyundai_2015_ccan.dbc) | Cross-checking corresponding message IDs before comparing TPS, intake temperature, ESP12 acceleration/pressure and torque-input definitions. The app does not assume that every DBC signal reaches the AVN. |
| [OPKR TPMS interpretation](https://github.com/openpilotkr/openpilot/blob/3ff6dc6081c2a986c75292eefa0952641ab66b6f/selfdrive/car/hyundai/carstate.py) | Cross-checking encoded tire-pressure units. The app normalizes them to psi. |
| Vehicle captures and owner observations | Comparing ignition, brake, steering, gears, TCS/ESC states and cluster tire pressures with received data. Observed response is distinct from instrument calibration. |

The factory receive path was traced through:

1. `Can2BPDMessage`: message names, actual CAN IDs, MICOM function numbers and lengths.
2. `CanMessageManager`: lookup of incoming functions in that table.
3. `CanCsm.receivedCanDataFor_ALL_C`: record separation and construction of parser input arrays.
4. `Can2BCsm`: byte positions, signs and scaling in `parseEMS11`, `parseCLU11`, `parseEMS12`, `parseSAS11` and `parseESP12`.
5. `ConvertType`: unsigned-byte conversion. Factory service getters were also used to compare interpretations.

The opendbc `master` URL is a moving reference, not an immutable commit verified by this project. External references and limits are also recorded in [NOTICE](../NOTICE.md) and [validation](PD_VALIDATION.md).

Not every bit or unit was established directly by the OEM parser. Corresponding public definitions supplement ESP12 acceleration/status bits and some physical units; invalid-value rejection also includes conservative app policy.

CN7 N software was inspected later for G-force graphics, acceleration timing and circuit-selection behavior. **The CN7 N app is not the primary source of the PD sensor-byte mappings.**

## Collection path

```text
Vehicle signals → factory AVN receive layer → hscand
                → UNIX abstract socket: hscan_mux
                → HscanDataClient → HscanFrameDecoder
                → Telemetry / PdGearState / PdPowertrain → UI/history
```

The factory initialization configuration starts `hscand` with `/dev/tcc-tsif1`. The app reads the local stream already published by the AVN at `hscan_mux`. This does not reconstruct the entire MICOM path or expose every raw vehicle CAN frame.

- The app connects as a local receiver. It adds no OBD PID query, UDS diagnostic request, CAN control command or receive-start handshake.
- ADB was the PC connection used for installation, research and retrieval. Normal telemetry does not travel through PC ADB.
- IGN and app-lifecycle conditions gate reception. Receiver startup and IGN transitions clear the telemetry model. A socket-only disconnect breaks partial frames and G-force/acceleration continuity; it does not immediately erase all general values. Repeated cached getter calls are not treated as new receipts.
- Receipt time is `SystemClock.elapsedRealtime()` at completion of `LocalSocket.read()`, not an ECU/CAN hardware timestamp. An unchanged value received again is still a new sample.

Implementation: [HscanDataClient](../apps/dashboard/src/com/veloster/dashboard/HscanDataClient.java), [VehicleDataClient](../apps/dashboard/src/com/veloster/dashboard/VehicleDataClient.java).

## Framing and indices

A 264-byte batch holds multiple records. A socket read does not necessarily align with a batch boundary.

| Offset | Current decoder check |
| --- | --- |
| 0–7 | Header `FF 0B 8E 01 8B C7 01 00` |
| 8 | Batch type `0xC7` |
| 9–10 | Little-endian record count, at most 84 |
| 11 onward | Repeated `[function:1 byte][length:1 byte][payload:1–8 bytes]` |
| End of records through 262 | Zero padding |
| All 264 bytes | XOR result equals zero |

The decoder validates the complete batch structure, XOR and padding before applying the PD function/exact-length allowlist. It handles fragmentation, consecutive batches and header resynchronization. Reconnection or a receive-queue gap discards partial frames. API17's transient `IOException: Try again` is treated as an idle read. This XOR checks the **local stream wrapper**; it is not a replacement for vehicle CAN CRC validation. [HscanFrameDecoder](../apps/dashboard/src/com/veloster/dashboard/HscanFrameDecoder.java).

**Internal functions are not CAN arbitration IDs.** Function `0x08` maps to EMS11/CAN `0x316` in the factory table, and `0x02` maps to CLU11/CAN `0x4F1`. App dispatch uses the function; DBC comparison uses the corresponding CAN message.

The OEM parser receives `[function, payload...]`, whereas the app separates the function. OEM indices 3/4 therefore become app payload `b[2]`/`b[3]`. Tracing this one-byte difference corrected the initial RPM candidate.

## Applied mappings

Below, `b[n]` is a **zero-based unsigned payload byte, excluding the function**. `u16LE` and `s16LE` mean unsigned and signed little-endian 16-bit values. Bit fields combined with `+` occupy disjoint positions. Read [Telemetry](../shared/src/com/veloster/vehicle/Telemetry.java) for the complete validity conditions.

| Function / length | Signal | Interpretation |
| --- | --- | --- |
| `0x08` / 8 | RPM | `u16LE(b[2],b[3]) × 0.25 rpm`. Raw 65535 is invalid; received zero RPM is valid. |
| `0x02` / 4 | Speed | `raw=b[1]+((b[2]&1)<<8)`, then `raw×0.5`. If bit1 of `b[2]` is 1, convert mph to km/h with `×1.609344`. Raw 511 is invalid. |
| `0x09` / 8 | Coolant | `b[1]×0.75−48°C`; raw 255 invalid. |
| `0x09` / 8 | Accelerator pedal | `b[6]×0.3906%`; raw 255 invalid. Separate from throttle. |
| `0x09` / 8 | TPS | `b[5]×0.4694836−15.0234742%`. Accept raw 32–245, clamp to 0–100%. |
| `0x0B` / 8 | Intake temperature | `b[2]×0.75−48°C`; raw 255 invalid. |
| `0x14` / 8 | Ambient temperature | `b[3]×0.5−40°C`; raw 255 invalid. |
| `0x19` / 5 | Steering angle/rate | `s16LE(b[0],b[1])×0.1°`, `b[2]×4°/s`. Accept observed state `b[3]==7`, absolute raw angle ≤10800 and raw rate <255. |
| `0x13` / 8 | Brake pressure | `((b[3]>>2)+((b[4]&63)<<6))×0.1 bar`, requiring `(b[4]&0xC0)==0`. |
| `0x13` / 8 | Lateral/longitudinal acceleration | ESP12 formulas below. |
| `0x1C` / 4 | TCS/ESC/ABS | State bits below. |
| `0x20` / 6 | TPMS | Wheel order, state and units below. |
| `0x1D` / 8 | Selector | `b[1]&15`: 0=P, 7=R, 6=N, 5=D. Codes 4/8/12 enter the manual branch; every code's physical meaning has not been established. |
| `0x1E` / 8 | Current forward gear | `b[1]&15`: 1–7. Pair with fresh D/M selector data within one second. |

Synthetic example: RPM bytes `b[2]=0xE0`, `b[3]=0x2E` give raw 12000, or 3000 rpm. Speed raw 100 with unit 0 gives 50 km/h. Conversely, signed steering `FFFF` means −0.1°; a universal FF-invalid rule would be incorrect.

### G-force

```text
Lateral raw = b[0] + ((b[1] & 7) << 8)
Longitudinal raw = (b[1] >> 5) + (b[2] << 3)
Acceleration m/s² = raw × 0.01 − 10.23
G = acceleration / 9.80665
```

Lateral requires `(b[1]&0x18)==0`; longitudinal requires `(b[3]&3)==0`. Both require raw <2047. These are **vehicle ESP12 acceleration signals**, not Android `SensorManager` or PC accelerometer readings.

### TCS, ESC and ABS

- TCS-off step: `(b[0]>>1)&3`; 0=ON, 1=OFF. Other values are unknown.
- ESC-off step: `b[1]&3`; 0/1=ON, 2=OFF. Disabling traction alone does not mark ESC OFF.
- ABS: `b[0]&1` is the **warning lamp**, not control intervention.
- An all-FF record is invalid.

### TPMS and gears

TPMS bytes `b[2]` through `b[5]` are front-left, front-right, rear-left and rear-right. Check status `(b[1]&7)==1` and unit `(b[1]>>3)&3`.

| Unit code | Encoded unit | Conversion to psi |
| --- | --- | --- |
| 0 | psi | raw |
| 1 | 5 kPa | raw × 0.7251887 |
| 2 | 0.1 bar | raw × 1.4503774 |

Raw 255 invalidates that wheel only; converted values above 100 psi are also withheld. Vehicle comparison covered unit 0 at 32/32/32/31 psi. Units 1/2 have host conversion checks only.

Selector transitions discard current-gear data from before the change. Without a fresh gear, the display shows D/M only. Gears are not inferred from RPM/speed ratios. [PdGearState](../shared/src/com/veloster/vehicle/PdGearState.java).

## Derived values and display

Torque, power and boost are **research calculations combining received fields**, not single-field instrument measurements. [PdPowertrain](../shared/src/com/veloster/vehicle/PdPowertrain.java).

```text
Reference torque = (EMS12 b[0] & 63) × 10 N·m
  Update only for mux3, b[0] >> 6 == 3; reject raw 0/63

Net torque N·m = (EMS11 b[1] − EMS11 b[5]) × 0.390625 × reference torque / 100
Power PS = net torque × RPM × 2π / 60 / 735.49875

Ambient raw = (function 0x0A b[0] >> 3) & 31
Ambient mmHg = raw × 10.731613 + 458.98
Absolute-pressure raw = (function 0x0D b[2] >> 4) + (b[3] << 4)
Absolute pressure hPa = raw × 1.322
Gauge boost bar = (absolute hPa − ambient mmHg × 1.33322387415) / 1000
```

Reference torque comes from mux3, rather than a hardcoded 350 N·m constant. It must be within three seconds; the pressure pair must be within one second. Other mux traffic does not refresh the reference timestamp, and results retain the older input timestamp. Additional rejection conditions include the EMS11 `0xB2` status mask, RPM raw 65535, ratio raw 255, ambient raw 31 and absolute-pressure raw 4095. Valid negative torque/boost is retained. Display conversion uses `1 kgf·m = 9.80665 N·m`; stored torque and the torque input to power calculations remain in N·m. Calculated power is in PS.

The 2026-11-12 main-source patch makes derived torque/power unavailable on a new zero-RPM receipt. RPM itself still displays zero, and positive idle RPM still permits calculation. **At this review date, the patch is in source but not the published v1.2.0/code21 APK.** [CHANGELOG](../CHANGELOG.md).

Longitudinal G-force plot reversal and steering display reversal are UI choices, separate from received numeric signs. Acceleration timing uses AVN CLU11 receipt times, not ECU hardware timestamps. GPS lap timing uses a separate Android location-provider path.

## Evidence and limits

The historical drive capture stopped before its 900-second limit. The retained interval was approximately 808.547 seconds: 5,236,176 bytes, 19,834 complete frames and 426,431 records. Retained-region rejects, discards and trailing partial bytes were zero, but the overall capture ended with a failed status and is not reported as a successful 900-second run. Replay reported 9,917 RPM and speed samples each, RPM 621–4,845, speed 0–74.5 km/h and no valid oil-temperature receipt. Raw captures and device identifiers are not public. [Aggregate validation data](../VALIDATION.json).

Research capture preserved socket bytes and monotonic receipt times; replay fed the actual Java decoder/models. Assumed intervals for older binaries without timestamps were distinguished from recorded receipt times. Those private research capture/replay tools are not included in this public repository. Public [model/decoder tests](../tests/) use synthetic inputs and do not establish vehicle validation.

| Item | Evidence scope |
| --- | --- |
| RPM, speed, temperatures, pedal, brake and steering | Receipts/updates observed; parked brake/steering response also confirmed. Not absolute sensor calibration. |
| Gears | Owner confirmed P/R/N and every D1–D7 display on the vehicle. |
| TCS/ESC | Owner confirmed individual ON/OFF behavior. Not measurement of active control intervention. |
| Oil temperature | Candidate `0x0D b[4]×0.75−40°C` remained raw 0 even with the engine running. Raw 0/255 is withheld. A separate OBD instrument's reported approximately 92°C was not substituted as an app reading. This does not mean the ECU lacks oil-temperature data. |
| Battery | Candidate conversion exists in code, but valid vehicle SOC/voltage remains unestablished. |
| Wheel speeds | The factory PD table has function −1 and an empty parser for WHL_SPD11; no reception evidence on this AVN path. |
| e-LSD | Not fitted to the confirmed vehicle; unavailable. |

Missing/invalid data stays `-`. The general display's 60-second stale limit, one-second gear pairing, three-second reference-torque window, one-second pressure window and some status/range exclusions are **conservative app policies**, not manufacturer timeouts or a complete OEM state dictionary. A new invalid receipt invalidates the corresponding previous value. Receiver startup and IGN transitions clear the full model; socket-only disconnection does not immediately erase every value. Remaining general values retain their timestamps and follow the ordinary stale/pairing rules.

For another vehicle, verify its AVN function numbers, lengths, array layout and parser rather than copying CAN IDs alone. [Validation](PD_VALIDATION.md), [NOTICE](../NOTICE.md).

---

**English** | [한국어](PD_DATA_MAPPING_ko.md) · [README](../README.md)
