# i30 PD installation and launch / 설치·실행

Historical vehicle observations apply only to the exact target in [README](../README_ko.md). The current v1.1.0 development APK has synthetic-input emulator tests, with no new vehicle installation/cold-boot test. Perform vehicle installation and screen interaction while parked. Use an already working ADB connection. No root is required merely to install this separate app; OEM signature/permission policies still apply.

1. Check `adb devices -l` and replace `SERIAL` below with your device. Confirm product/build before installing:

```sh
adb -s SERIAL shell getprop ro.product.name
adb -s SERIAL shell getprop ro.build.version.incremental
```

Expected: `pd_kr`, `eng.vmadmin.20260114.084130`. Stop if different; changing a property to bypass the receiver gate does not establish compatibility.

2. Verify the complete checkout's binaries:

```sh
python3 scripts/verify_release.py
```

3. If the dashboard is installed, record its version and back up its APK before updating. `pm path` returns the exact APK path to pass to `adb pull`; save it outside this public checkout. Keep your own settings backup where available. Do not uninstall or clear app data to work around an update/signature error.

```sh
adb -s SERIAL shell dumpsys package com.veloster.dashboard
adb -s SERIAL shell pm path com.veloster.dashboard
```

4. Install the current PD v1.1.0 development APK and request launch. No new vehicle installation test has been performed; historical vehicle checks refer to v11 and earlier builds. The published v1.0.0 Release remains separately available:

```sh
adb -s SERIAL install -r apks/i30-PD-Dashboard-v1.1.0-API17.apk
adb -s SERIAL shell am start -n com.veloster.dashboard/.DashboardActivity
```

Check the actual screen and values while parked. An accepted command does not prove useful telemetry. Signature mismatch/unsupported target requires investigation; no permission-policy bypass is provided. The app retains package name `com.veloster.dashboard` and the same signing certificate. Its internal versionCode13 is higher than v1.0.0/code12 and historical v11. Update acceptance and saved settings still need checking on the vehicle. The historical installed APK remains available as provenance; installing a lower-code APK over this update may be rejected.

For torque units, open **SET → Torque unit** (Korean: **설정 → 토크 단위**), choose N·m or kgf·m, then save. Cancel leaves the previous selection in place. This is an app display preference, not a vehicle-unit setting. Language selection is available in the same SET menu.

The factory launcher may hide sideloaded apps. Use an existing reachable launcher or the unchanged optional My Apps v10 companion. To install and request opening that companion while parked:

```sh
adb -s SERIAL install -r apks/My-Apps-v10-API17.apk
adb -s SERIAL shell am start -n com.veloster.carplayhub/.HubActivity
```

Its Quick entry setting is opt-in: choose Floating + notification, save, then verify HOME → edge button → My Apps → N Dashboard on the actual screen. If no usable shortcut appears, retain ADB access or use your existing launcher. The owner's dashboard cold-boot confirmation does not separately validate every My Apps shortcut/boot-restoration behavior.

**The retained `scripts/install.py`/OEM menu profile targets JSN and does not install PD APKs.** Neither dashboard nor companion APK creates a PD OEM menu entry. The JSN OEM menu procedure is outside this PD guide.

No PC is needed for the app's existing receive path after launch. The owner confirmed app execution and retained settings after a cold boot. Automatic full-screen launch at boot is not implied by that confirmation. No new ADB or vehicle operation was performed to prepare this public PR.
