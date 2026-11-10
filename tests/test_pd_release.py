"""Keep the inherited JSN installer from selecting the higher PD release."""
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import menu_entry


class PdReleaseSelectionTests(unittest.TestCase):
    def test_current_pd_metadata_preserves_upgrade_order(self):
        release = menu_entry.read_json(ROOT / "release.json")
        current_path = release["pd_versioning"]["current_artifact"]
        current = next(e for e in release["apks"] if e["file"] == current_path)
        previous = [e for e in release["apks"] if e.get("installer_profile") == "pd_kr"
                    and e["file"] != current_path]
        manifest = ET.parse(ROOT / "apps/dashboard/AndroidManifest.xml").getroot()
        android = "{http://schemas.android.com/apk/res/android}"
        self.assertEqual(current["installer_profile"], "pd_kr")
        self.assertEqual(current["pd_version"], release["pd_versioning"]["current_version"])
        self.assertEqual(current["package"], manifest.get("package"))
        self.assertEqual(current["version_name"], manifest.get(android + "versionName"))
        self.assertEqual(current["version_code"], int(manifest.get(android + "versionCode")))
        self.assertTrue(previous)
        self.assertGreater(current["version_code"], max(e["version_code"] for e in previous))

    def test_jsn_installer_keeps_upstream_apks(self):
        apps = menu_entry.load_apps(ROOT)
        self.assertEqual(apps["com.veloster.dashboard"]["path"].name,
                         "N-Dashboard-v6-API17.apk")
        self.assertEqual(apps["com.veloster.carplayhub"]["path"].name,
                         "My-Apps-v10-API17.apk")
        release = menu_entry.read_json(ROOT / "release.json")
        pd_entries = [e for e in release["apks"] if e.get("installer_profile") == "pd_kr"]
        self.assertGreaterEqual(len(pd_entries), 2)
        for entry in pd_entries:
            with self.subTest(file=entry["file"]):
                pd_hash = menu_entry.sha(ROOT / entry["file"])
                self.assertNotIn(pd_hash, apps["com.veloster.dashboard"]["accepted"])


if __name__ == "__main__":
    unittest.main()
