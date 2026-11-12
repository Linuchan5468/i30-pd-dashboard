# Changelog

This file covers the PD adaptation against upstream/fork baseline `2be141b` (2026-10-26 UI release). The original seven commits are ancestors of this branch. Upstream's later optional Performance dashboard commit is not included in this PD change. Dates describe development, validation or repository work as stated, not GitHub Release publication dates. PD now uses an independent MAJOR.MINOR.PATCH series; historical v7–v11 labels remain as evidence identifiers.

## Unreleased — patch for 1.2.0

- Suppress calculated PD torque and power when a new engine receipt reports zero RPM. Previously, nonzero torque-ratio fields could produce a torque number with the engine stopped. Both derived readings now become unavailable (`-`) immediately; fresh positive RPM restores the ordinary calculation, including idle while parked.
- Added model and display-path regression checks for running → stopped → idle. No vehicle-speed threshold or fabricated zero torque is used. This is a source patch; the published v1.2.0 APK and release remain unchanged.

## i30 PD 1.2.0 — 2026-11-11

- Added PD G-force 2D gauges and expandable F1 plots with shared received-point history. Set the circular range to ±1 G and display positive longitudinal acceleration below center for weight transfer. Numeric readings retain their received signs.
- Applied the inspected CN7 N dotted-contour and radius-based blue/purple/red color rules. The independently implemented history retains 16 directional maxima and 18 recent distinct points. Directional peak labels appear on larger plots. Explicit hold resets the session history.
- Added an acceleration-test tab with 10 km/h interval selection, arm/cancel and the ten most recent successful results. Selection and completed records persist. The CN7 speed-state rule starts a zero-start test at 1 km/h and a rolling test above the selected initial speed.
- Estimate crossing times between actual decoded PD CLU11 receipts. Disconnects, invalid data, reversed times, background loss and gaps over one second interrupt active measurements. These are AVN receipt-time estimates with field accuracy awaiting validation.
- Added horizontal scrolling to the top tabs, keeping the clock and return control fixed. Swiping does not activate a tab.
- Added manual and GPS-estimated lap timing, saved completed laps, best/last times and a Korean catalog of four circuits and six courses. Circuit selection follows the inspected N navigation flow: choose a circuit from the list, then a numbered course; the map and saved selection update immediately. Selection is locked while a measurement is active.
- Reconstructed course maps from the original COURSE link coordinates rather than circuit AREA outlines. Each link is drawn separately; KIC GP/Standing and Inje Full/A retain their distinct routes. The PD selection screen and artwork are independently implemented.
- GPS timing uses the source start/goal points with a 10 m proximity policy, 20 m rearm distance and independent minimum-time/distance guards. It uses ordinary Android GPS updates and requires precise-location permission. This is an estimate without the original navigation map-matching service; real-vehicle GPS lap timing has not been validated.
- Fixed API17 local-socket idle reads reported as `IOException: Try again` so they do not unnecessarily reconnect the HSCAN receiver or interrupt measurements. EOF and other connection failures still invalidate the stream.
- Corrected overlapping lap/selector titles and replaced a missing dropdown font glyph with a drawn triangle. Fresh GPS fixes clear inactive warmup warnings; interruption reasons remain visible. Hardware Back closes the circuit selector first.
- Release APK: `i30-PD-Dashboard-v1.2.0-API17.apk`, versionName `1.2.0-pd`, versionCode21, existing package/certificate. SHA-256 `ab52c385496e90fa9affa5384bc41ec022f85e1763da4189a15a0a67d56ab119`.
- Checked the final APK in the ARM32 QEMU/API17 emulator. A synthetic GPS route completed one Taebaek lap of120,953ms; course selection and the completed record survived process restart. The v20 normal-receiver acceleration test recorded9,521ms against9,411ms at the sender (+110ms), exceeding the100ms comparison threshold by10ms. The final UI-only build retains that timer/receiver logic. These are functional tests, not timing-accuracy or vehicle validation. No new vehicle installation, driving or cold-boot test was performed.

## i30 PD 1.1.0 — 2026-11-10

- Added persisted N·m / kgf·m torque display selection under SET → Torque unit (설정 → 토크 단위). Default remains N·m; save/cancel are explicit, and language selection remains available under SET.
- Applied the selection to overview cards, gauge values/scales and graph values, existing history and axes. Converts using 1 kgf·m = 9.80665 N·m; values display to one decimal place. All-invalid graph history uses the selected unit for fallback axes.
- Kept received samples/history/timestamps and torque/power calculations in N·m. Unit changes do not create samples, restore stale values or change other signal units.
- Added host conversion and sample/history preservation checks. API17/V1-signed APK uses versionName1.1.0-pd and internal versionCode13, with the existing package and certificate.
- Tested the exact installed APK in the existing ARM32 QEMU/API17 emulator using a separate synthetic-input fixture: overview/gauge35.7 kgf·m from350 N·m, graph values/history/axes in both units, save/cancel, zero/negative/missing data and process-restart preference retention. Stopped the fixture, cleared its samples and verified a normal relaunch with the saved unit. No shipping test hooks or lifecycle bypass were added.
- Published v1.1.0 with the same emulator-tested APK, unchanged hash and signing certificate. The v1.0.0 Release remains available. No new vehicle installation, settings/cold-boot or telemetry accuracy validation was performed for publication.

## i30 PD 1.0.0 — 2026-11-10

- Started independent PD public versioning at 1.0.0, Android `versionName=1.0.0-pd`; retained package/signing identity and raised internal `versionCode` to12 for upgrades from code11.
- Published `i30-PD-Dashboard-v1.0.0-API17.apk` and updated the current download/install instructions, release metadata and checksums. Original JSN/My Apps versioning is unchanged.
- Recompiled the same application sources. DEX and compiled resources match the installed historical v11 byte-for-byte; binary manifest changes are limited to the two version attributes. V1 signature, signer continuity, API17/DEX035 and alignment checks passed offline.
- No telemetry, UI, permissions or settings logic changes. No new vehicle/emulator installation or cold-boot test; existing vehicle observations remain attributed to the historical builds. Preserved the historical v11 APK and build/validation records.
- Checked that all PD APKs remain excluded from the inherited JSN installer.

## 2.0-pdkr / versionCode 11 — 2026-10-29

- Target RPM minimum lowered from 6000 to 3000; range 3000–6700, step100. Preserved default and existing settings. Verified minimum clamp, persistence after restart and restoration to6000 on the installed app.
- Enlarged i30 N Line text by3 renderer size units in both F1 views (11→14 and10→13); visually checked both screens.
- Installed APK readback matched SHA-256 `d3614615cbb10c79e87a800d79eef245e32a8677d05e083de357d457b94b686d`.

## PD development leading to v11 — 2026-10-29

- v7: added exact `pd_kr`/build receive profile and PD function/length mappings for RPM and speed; corrected payload offsets and independent speed units.
- v8: added temperatures, separate throttle/pedal, brake/steering and acceleration mappings; PD layout, validity and expiry tests.
- v9/v10: added selector/current-gear pairing for DCT7, TPMS decoding, TCS/ESC enabled-state interpretation and explicit ABS warning-lamp label. Corrected TCS-only disable so ESC remains enabled.
- Added received-source torque/power and gauge-pressure calculations. Reference torque is received, not a fixed350N·m constant. Final reference-torque age window3s; pressure pair1s; result timestamp uses the oldest source receipt.
- Added PD/i30 N Line F1 branding and retained the PD layout with ignition off. Clearing ignition removes old samples; invalid/missing readings remain unavailable.
- Added host checks for PD telemetry, framing, selector/gear state, powertrain calculations and overview layout. Original JSN paths remain; they were not revalidated on a Veloster N with v11.

## Public Fork preparation — 2026-11-03

- Published actual v11 sources and the historical installed APK, with checksums, sanitized validation/build provenance, PD installation guide and explicit verification limits.
- Preserved upstream history, LICENSE and notices; archived upstream README/release/validation records. No raw vehicle captures, OEM implementation binaries or private keys added.
- Kept inherited JSN installation/menu tools on JSN APKs so the higher PD version is not automatically selected for the upstream profile.
- Updated public build/test compiler flags to Java8 (accepted by JDK17/21) while retaining D8 min-api17/DEX035; compile-only OEM declarations are passed as D8 classpath, never packaged.
- No new vehicle installation, driving, cold-boot or ECU diagnostic operation was performed for this publication.

## Owner-confirmed validation update — 2026-11-03

- Confirmed app execution after cold boot and preservation of saved settings; this does not assert automatic full-screen launch.
- Confirmed P/R/N and all forward gears D1–D7 on the vehicle.
- Confirmed individual TCS/ESC ON/OFF behavior, including TCS-only disable with ESC remaining ON.
- Clarified oil-temperature unavailability: no usable value found in the AVN receive path, so not implemented for this vehicle.
- Clarified that the confirmed vehicle is not fitted with e-LSD; that feature is unavailable.
- These corrections use the owner's report received on2026-11-03; no new agent-operated vehicle test, source or APK change was performed.

## PD-only documentation cleanup — 2026-11-03

- Removed duplicated upstream guides, archived README/release/validation snapshots and five upstream documentation images from the current tree to prevent confusion with PD observations.
- Rewrote build/translation instructions for this Fork and kept the PD installation, validation and build-provenance documents. Updated companion setup and all remaining document links.
- Preserved upstream commit history, LICENSE, copyright/NOTICE and dependency rights. Application logic, APKs and app resources remain unchanged; legacy tool help points to the original external JSN guides.
