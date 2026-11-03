# i30 PD Dashboard

Unofficial i30 PD adaptation of [reikop/veloster-n-dashboard](https://github.com/reikop/veloster-n-dashboard). [한국어](README_ko.md)

This fork adds a PD-specific receive decoder and dashboard presentation. Original commits, the unchanged [MIT LICENSE](LICENSE), and upstream notices are retained. Upstream README and validation/release records are preserved under [docs/upstream](docs/upstream/README.md); their Veloster N observations are historical and do not establish PD compatibility.

## Confirmed target and scope

Work was observed on **one Korean Hyundai i30 PD N Line with a 7-speed DCT**, using the factory Gen5 Android 4.2.2/API17 head unit: product `pd_kr`, incremental build `eng.vmadmin.20260114.084130` (firmware `PD.KOR.SOP.V141.260114.STD_H`). Other years, trims, engines, regions, firmware builds and ordinary Android devices have not been validated. The socket receiver retains an exact product/build gate. JSN code remains, but the PD v11 APK has not been retested on a Veloster N.

## Download and install

Current PD artifact: [N-Dashboard-v11-PD-API17.apk](apks/N-Dashboard-v11-PD-API17.apk), version **11 / 2.0-pdkr**. This is the actual installed/readback-matched artifact from 2026-10-29. Its initial final-UI check was performed with ignition off and AVN power retained. The owner subsequently confirmed that the app runs after a cold boot and retains its saved settings. [Validation details](docs/PD_VALIDATION.md).

```sh
python3 scripts/verify_release.py
```

[PD installation and launch](docs/INSTALL_PD.md) explains target checks, backups and ADB commands. The inherited `scripts/install.py` and OEM menu tools remain **JSN-profile tools**, not PD installers. They select upstream JSN artifacts; this fork does not publish modified OEM menu APKs. My Apps v10 is retained unchanged as an optional upstream companion, with its own upstream test limits.

APKs use the public AOSP platform test certificate for compatibility. It is not a private publisher identity. No signing private keys are included. See [NOTICE.md](NOTICE.md) and [release.json](release.json).

## PD changes

- PD RPM/speed reception, separate throttle and accelerator-pedal values, coolant/intake/ambient temperatures, brake pressure, steering angle/rate and longitudinal/lateral acceleration.
- P/R/N and D1–D7 display with fresh selector/gear pairing. The owner confirmed P/R/N and every forward gear D1–D7 on the vehicle.
- Four tire pressures; parked cluster comparison was 32/32/32/31 psi. Other encoded units have host conversion tests only.
- TCS/ESC **enabled states** displayed ON/OFF; ABS is labelled as its **warning lamp**, not ABS intervention. The owner confirmed the individual TCS and ESC ON/OFF behavior on the vehicle, including TCS-only disable with ESC remaining ON.
- Estimated net torque/power from received torque inputs, and gauge boost from absolute/ambient pressure. These are research calculations, not measured crankshaft/wheel output or calibrated instruments.
- PD F1 branding (`PD`, `i30 N Line`), larger vehicle-name text, overview layout and target RPM range **3000–6700**, in 100 RPM steps. Existing 6000 defaults/settings are retained.

Missing or invalid data remains `-`. Oil temperature could not be implemented for this vehicle because no usable oil-temperature value was found on the AVN receive path. The vehicle has no e-LSD, so that feature is unavailable. Valid battery SOC/voltage, wheel speeds and actual ABS intervention remain unestablished on the verified PD receive path. Invalid-value policies and numerical mappings include research interpretations. [Detailed limits and evidence](docs/PD_VALIDATION.md).

## Development and provenance

[CHANGELOG](CHANGELOG.md) records actual PD changes; [build guide](docs/BUILD.md) explains external tools and unsigned builds. Synthetic tests require Python and a JDK:

```sh
python3 scripts/test.py --java /path/to/jdk/bin/java
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/generate_translations.py --check
```

No OEM firmware, APK/ODEX implementations, raw vehicle captures, device identifiers, personal settings/backups, SDK binaries or signing keys are added by this PD publication. Compile-only OEM declarations are excluded from the app DEX. Receive-only telemetry does not send ECU diagnostic/control commands; explicit My Apps settings/media actions have separate behavior. This is an unofficial research project.
