#!/usr/bin/env python3
"""Build the checked-in component manifest from deterministic catalog sources."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_INDEX = ROOT / "catalog" / "index.yml"
DEFAULT_OUTPUT = ROOT / "manifest.json"
SOURCE_LAYOUT = (
    ("drivers.yml", "driver"),
    ("dxvk.yml", "dxvk"),
    ("proton.yml", "proton"),
    ("fex.yml", "fexcore"),
    ("wowbox64.yml", "wowbox64"),
    ("vkd3d.yml", "vkd3d"),
    ("wine.yml", "wine"),
    ("box64.yml", "box64"),
    ("audio.yml", "audio"),
)
LEGACY_TYPES = {group for _, group in SOURCE_LAYOUT if group != "audio"}
ENTRY_KEYS = {"id", "name", "url", "variant", "arch"}


class CatalogError(ValueError):
    pass


def load_yaml_subset(path: Path) -> Any:
    """Load JSON, which is intentionally the dependency-free YAML 1.2 subset used here."""
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise CatalogError(f"missing catalog source: {path}") from error
    except json.JSONDecodeError as error:
        raise CatalogError(f"invalid JSON-compatible YAML in {path}: {error}") from error


def dump_yaml_subset(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def _safe_source_path(catalog_dir: Path, relative: str) -> Path:
    candidate = (catalog_dir / relative).resolve()
    if candidate.parent != catalog_dir.resolve():
        raise CatalogError(f"catalog source must be a direct child of catalog/: {relative}")
    return candidate


def load_and_validate_sources(index_path: Path = DEFAULT_INDEX) -> tuple[dict[str, Any], dict[str, list[dict[str, Any]]]]:
    index = load_yaml_subset(index_path)
    if not isinstance(index, dict):
        raise CatalogError("catalog/index.yml must contain an object")
    if index.get("sourceSchemaVersion") != 1:
        raise CatalogError("sourceSchemaVersion must be 1")
    output = index.get("output")
    if not isinstance(output, dict) or output.get("version") != 1 or not output.get("updatedAt"):
        raise CatalogError("index output must contain version=1 and updatedAt")
    sources = index.get("sources")
    if not isinstance(sources, list) or not sources:
        raise CatalogError("index sources must be a non-empty list")

    groups: dict[str, list[dict[str, Any]]] = {}
    ids: set[str] = set()
    for source in sources:
        if not isinstance(source, dict) or set(source) != {"path", "type"}:
            raise CatalogError("each index source must contain exactly path and type")
        relative = source["path"]
        expected_type = source["type"]
        if not isinstance(relative, str) or not isinstance(expected_type, str):
            raise CatalogError("source path and type must be strings")
        if expected_type in groups:
            raise CatalogError(f"duplicate catalog type: {expected_type}")
        document = load_yaml_subset(_safe_source_path(index_path.parent, relative))
        if not isinstance(document, dict) or set(document) != {"type", "components"}:
            raise CatalogError(f"{relative} must contain exactly type and components")
        if document["type"] != expected_type:
            raise CatalogError(f"{relative}: expected type {expected_type}, found {document['type']}")
        components = document["components"]
        if not isinstance(components, list):
            raise CatalogError(f"{relative}: components must be a list")

        validated: list[dict[str, Any]] = []
        for position, entry in enumerate(components):
            label = f"{relative} components[{position}]"
            if not isinstance(entry, dict):
                raise CatalogError(f"{label}: entry must be an object")
            unknown = set(entry) - ENTRY_KEYS
            missing = {"id", "name", "url"} - set(entry)
            if unknown or missing:
                raise CatalogError(f"{label}: unknown={sorted(unknown)} missing={sorted(missing)}")
            if any(not isinstance(entry[key], str) or not entry[key].strip() for key in ("id", "name", "url")):
                raise CatalogError(f"{label}: id, name and url must be non-empty strings")
            if not entry["url"].lower().startswith("https://"):
                raise CatalogError(f"{label}: url must use HTTPS")
            if entry["id"] in ids:
                raise CatalogError(f"duplicate component id: {entry['id']}")
            ids.add(entry["id"])
            validated.append(entry)
        groups[expected_type] = validated
    return output, groups


def generate_manifest(index_path: Path = DEFAULT_INDEX) -> str:
    output, groups = load_and_validate_sources(index_path)
    items: dict[str, list[dict[str, Any]]] = {}
    for group, entries in groups.items():
        if not entries:
            continue
        if group not in LEGACY_TYPES:
            raise CatalogError(f"legacy manifest cannot contain non-empty type: {group}")
        items[group] = entries
    return dump_yaml_subset({"version": 1, "updatedAt": output["updatedAt"], "items": items})


def bootstrap_from_legacy(output_path: Path = DEFAULT_OUTPUT, index_path: Path = DEFAULT_INDEX) -> None:
    legacy = load_yaml_subset(output_path)
    if not isinstance(legacy, dict) or legacy.get("version") != 1 or not isinstance(legacy.get("items"), dict):
        raise CatalogError("bootstrap input must be a legacy version 1 manifest")
    catalog_dir = index_path.parent
    targets = [index_path, *(catalog_dir / filename for filename, _ in SOURCE_LAYOUT)]
    existing = [str(path) for path in targets if path.exists()]
    if existing:
        raise CatalogError("refusing to overwrite existing catalog sources: " + ", ".join(existing))
    catalog_dir.mkdir(parents=True, exist_ok=True)
    sources = []
    for filename, group in SOURCE_LAYOUT:
        (catalog_dir / filename).write_text(
            dump_yaml_subset({"type": group, "components": legacy["items"].get(group, [])}),
            encoding="utf-8",
        )
        sources.append({"path": filename, "type": group})
    index_path.write_text(
        dump_yaml_subset(
            {
                "sourceSchemaVersion": 1,
                "output": {"version": 1, "updatedAt": legacy.get("updatedAt")},
                "sources": sources,
            }
        ),
        encoding="utf-8",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if manifest.json is stale")
    parser.add_argument("--bootstrap-from-legacy", action="store_true", help="split the current v1 manifest once")
    parser.add_argument("--index", type=Path, default=DEFAULT_INDEX)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args(argv)
    try:
        if args.bootstrap_from_legacy:
            bootstrap_from_legacy(args.output, args.index)
        generated = generate_manifest(args.index)
        if args.check:
            current = args.output.read_text(encoding="utf-8") if args.output.exists() else ""
            if current != generated:
                raise CatalogError(f"{args.output} is stale; run scripts/catalog/generate_catalog.py")
            print(f"catalog ok: {args.output}")
        else:
            args.output.write_text(generated, encoding="utf-8")
            print(f"generated: {args.output}")
        return 0
    except CatalogError as error:
        print(f"catalog error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
