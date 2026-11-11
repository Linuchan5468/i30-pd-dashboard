#!/usr/bin/env python3
"""Run synthetic host tests without Android, private logs, network or ADB."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    found = shutil.which(args.java)
    if not found:
        raise SystemExit("JDK 17 java is required")
    java = Path(found).resolve()
    javac = java.with_name("javac.exe" if os.name == "nt" else "javac")
    hub = ROOT / "apps/hub/src/com/veloster/carplayhub"
    sources = [ROOT / "shared/src/com/veloster/vehicle/Telemetry.java",
               ROOT / "shared/src/com/veloster/vehicle/AccelerationTimer.java",
               ROOT / "shared/src/com/veloster/vehicle/HeaderTabs.java",
               ROOT / "shared/src/com/veloster/vehicle/LapCircuit.java",
               ROOT / "shared/src/com/veloster/vehicle/LapTimer.java",
               ROOT / "shared/src/com/veloster/vehicle/Cn7CircuitCatalog.java",
               ROOT / "shared/src/com/veloster/vehicle/Cn7CircuitRoutes.java",
               ROOT / "shared/src/com/veloster/vehicle/CircuitPicker.java",
               ROOT / "shared/src/com/veloster/vehicle/GForcePoint.java",
               ROOT / "shared/src/com/veloster/vehicle/GForceTrail.java",
               ROOT / "shared/src/com/veloster/vehicle/F1GForceLayout.java",
               ROOT / "shared/src/com/veloster/vehicle/GForceTouch.java",
               ROOT / "shared/src/com/veloster/vehicle/GaugeLayout.java",
               ROOT / "shared/src/com/veloster/vehicle/TorqueUnit.java",
               ROOT / "shared/src/com/veloster/vehicle/PdPowertrain.java",
               ROOT / "shared/src/com/veloster/vehicle/PdGearState.java",
               ROOT / "shared/src/com/veloster/vehicle/OverviewLayout.java",
               ROOT / "shared/src/com/veloster/i18n/UiText.java",
               ROOT / "apps/dashboard/src/com/veloster/dashboard/HscanFrameDecoder.java"]
    sources += [ROOT / "apps/dashboard/src/com/veloster/dashboard/HscanReadPolicy.java"]
    sources += [hub / (name + ".java") for name in ("RadioTrial", "BluetoothSession", "RebootRequest", "QuickEntryPolicy")]
    tests = sorted((ROOT / "tests").glob("*Test.java"))
    sources += tests
    with tempfile.TemporaryDirectory(prefix="vn-public-tests-") as temporary:
        subprocess.run([str(javac), "-encoding", "UTF-8", "-d", temporary,
                        *map(str, sources)], check=True)
        for test in tests:
            packages={"HscanFrameDecoderTest":"com.veloster.dashboard.",
                      "GForcePointTest":"com.veloster.vehicle.",
                      "GForceTrailTest":"com.veloster.vehicle."}
            packages.update({"GaugeLayoutTest":"com.veloster.vehicle.","F1GForceInteractionTest":"com.veloster.vehicle."})
            packages["HscanReadPolicyTest"]="com.veloster.dashboard."
            cls = packages.get(test.stem,"") + test.stem
            subprocess.run([str(java), "-ea", "-cp", temporary, cls], check=True)
    print("PASS: " + str(len(tests)) + " synthetic host test programs. Vehicle operation is outside this scope.")


if __name__ == "__main__":
    main()
