"""Keep the inherited JSN installer from selecting the higher PD release."""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import menu_entry


class PdReleaseSelectionTests(unittest.TestCase):
    def test_jsn_installer_keeps_upstream_apks(self):
        apps = menu_entry.load_apps(ROOT)
        self.assertEqual(apps["com.veloster.dashboard"]["path"].name,
                         "N-Dashboard-v6-API17.apk")
        self.assertEqual(apps["com.veloster.carplayhub"]["path"].name,
                         "My-Apps-v10-API17.apk")
        pd_hash = menu_entry.sha(ROOT / "apks/N-Dashboard-v11-PD-API17.apk")
        self.assertNotIn(pd_hash, apps["com.veloster.dashboard"]["accepted"])


if __name__ == "__main__":
    unittest.main()
