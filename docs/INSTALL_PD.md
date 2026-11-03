# i30 PD installation and launch / 설치·실행

Only the exact target in [README](../README_ko.md) was checked. Perform installation and screen interaction while parked. Use an already working ADB connection. No root is required merely to install this separate app; OEM signature/permission policies still apply.

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

4. Install the PD APK and request launch:

```sh
adb -s SERIAL install -r apks/N-Dashboard-v11-PD-API17.apk
adb -s SERIAL shell am start -n com.veloster.dashboard/.DashboardActivity
```

Check the actual screen and values while parked. An accepted command does not prove useful telemetry. Signature mismatch/unsupported target requires investigation; no permission-policy bypass is provided. The app retains package name `com.veloster.dashboard` to preserve the tested update path.

The factory launcher may hide sideloaded apps. Use an existing reachable launcher or the unchanged optional My Apps companion. For optional companion behavior see the upstream [Quick entry guide](QUICK_ENTRY.md); its reboot/shortcut test limitations apply. **The inherited `scripts/install.py`/OEM menu profile targets JSN and does not install PD v11.** Neither this APK nor this PR creates a PD OEM menu entry. Do not apply the inherited JSN OEM menu procedure to PD.

No PC is needed for the app's existing receive path after launch. The owner confirmed app execution and retained settings after a cold boot. Automatic full-screen launch at boot is not implied by that confirmation. No new ADB or vehicle operation was performed to prepare this public PR.
