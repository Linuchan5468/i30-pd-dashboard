# Screenshot provenance

These nine images were selected for the English and Korean README galleries. They show the project app during development and verification of PD v1.2.0 in the ARM32 QEMU/API17 emulator. Vehicle readings, acceleration times and GPS positions are synthetic examples. They do not establish real-vehicle performance, satellite reception or measurement accuracy.

All images are unchanged 800×480 native guest framebuffer captures. They contain the app UI without PC windows or notifications. Missing and unsupported readings stay `-`.

Eight screenshots use the exact released internal build21 APK. The acceleration image comes from development build20, whose acceleration UI and timer/receiver logic are unchanged in build21. The lap image records the synthetic GPS test and the course-selection image uses source COURSE links; the synthetic GPS test route is separate from the displayed course map.

[SCREENSHOTS.json](SCREENSHOTS.json) records image hashes and the internal build for each image. Private emulator helpers and raw logs are not included in this folder. The upstream license, external dependencies and source-map provenance remain documented in [NOTICE](../../NOTICE.md).
