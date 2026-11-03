# Changelog

This file covers the PD adaptation against upstream/fork baseline `2be141b` (2026-10-26 UI release). The original seven commits are ancestors of this branch. Upstream's later optional Performance dashboard commit is not included in this PD change. Dates below describe local development/vehicle work, not GitHub release dates.

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
