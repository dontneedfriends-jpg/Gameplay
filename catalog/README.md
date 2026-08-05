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

For schema v2 sources the generator enforces artifact size and SHA-256, HTTPS URLs without embedded
credentials, source commit provenance, supported Android page sizes, safe required-file paths,
dependency existence, unique stable version keys, and the stable-to-experimental dependency rule.

Built-in runtime defaults are not yet represented by the remote catalog. They must be inventoried as
catalog components before enabling the final CI rule that requires every application default to
resolve through the catalog.
