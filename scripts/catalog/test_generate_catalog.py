import json
import tempfile
import unittest
from pathlib import Path

from generate_catalog import CatalogError, generate_manifest, load_and_validate_sources


class GenerateCatalogTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.catalog_dir = Path(self.temp_dir.name) / "catalog"
        self.catalog_dir.mkdir()

    def tearDown(self):
        self.temp_dir.cleanup()

    def write_source(self, entries):
        (self.catalog_dir / "dxvk.yml").write_text(
            json.dumps({"type": "dxvk", "components": entries}),
            encoding="utf-8",
        )
        index = {
            "sourceSchemaVersion": 1,
            "output": {"version": 1, "updatedAt": "2026-08-05"},
            "sources": [{"path": "dxvk.yml", "type": "dxvk"}],
        }
        index_path = self.catalog_dir / "index.yml"
        index_path.write_text(json.dumps(index), encoding="utf-8")
        return index_path

    def test_generation_is_deterministic(self):
        index = self.write_source(
            [{"id": "dxvk-2.7", "name": "DXVK 2.7", "url": "https://example.com/dxvk.wcp"}]
        )
        first = generate_manifest(index)
        second = generate_manifest(index)
        self.assertEqual(first, second)
        self.assertEqual("dxvk-2.7", json.loads(first)["items"]["dxvk"][0]["id"])

    def test_duplicate_ids_are_rejected(self):
        entry = {"id": "same", "name": "Same", "url": "https://example.com/a.wcp"}
        index = self.write_source([entry, entry])
        with self.assertRaisesRegex(CatalogError, "duplicate component id"):
            load_and_validate_sources(index)

    def test_non_https_urls_are_rejected(self):
        index = self.write_source(
            [{"id": "dxvk", "name": "DXVK", "url": "http://example.com/dxvk.wcp"}]
        )
        with self.assertRaisesRegex(CatalogError, "must use HTTPS"):
            load_and_validate_sources(index)

    def test_source_cannot_escape_catalog_directory(self):
        outside = Path(self.temp_dir.name) / "outside.yml"
        outside.write_text("{}", encoding="utf-8")
        index = {
            "sourceSchemaVersion": 1,
            "output": {"version": 1, "updatedAt": "2026-08-05"},
            "sources": [{"path": "../outside.yml", "type": "dxvk"}],
        }
        index_path = self.catalog_dir / "index.yml"
        index_path.write_text(json.dumps(index), encoding="utf-8")
        with self.assertRaisesRegex(CatalogError, "direct child"):
            load_and_validate_sources(index_path)


if __name__ == "__main__":
    unittest.main()
