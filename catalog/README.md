# Component catalog sources

`manifest.json` is generated. Do not edit it directly.

Edit the typed source files in this directory, then run:

```text
python scripts/catalog/generate_catalog.py
python scripts/catalog/generate_catalog.py --check
```

The `.yml` sources intentionally use JSON-compatible YAML 1.2. This keeps them readable by YAML
tools while allowing deterministic generation with the Python standard library and no downloaded
packages.

The current sources preserve the legacy v1 output while component provenance, hashes, sizes and
compatibility metadata are collected. The application already accepts validated schema v2 catalogs;
the generator must switch to v2 only when every migrated artifact has real, verified metadata.
