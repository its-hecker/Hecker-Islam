#!/usr/bin/env python3
"""Package the *authorized* Salat Times reference content into Android assets.

Usage:
  python3 tools/bundle_reference_assets.py /path/to/ST-261104-v1.7.0-stable-317dffb.apk
  python3 tools/bundle_reference_assets.py /path/to/HeckerIslam-reference-content-pack.zip
  python3 tools/bundle_reference_assets.py --verify

The APK generated after this one-time *developer* operation is standalone:
end users never need to import files or connect to the Internet.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/reference"
PREFIXES = (
    "quran/", "azkar/", "hadith/", "location/",
    "adhan/", "adhan_background/",
)
REQUIRED = {
    "quran/chapters.json": 114,
    "quran/quran_uthmani.json": 6236,
    "azkar/categories.json": 11,
    "azkar/chapters.json": 133,
    "azkar/items.json": 287,
    "hadith/categories.json": 493,
}
MANDATORY = ("hadith/hadith.ndjson.br", "location/city_index.json")
MAX_FILE = 12 * 1024 * 1024
MAX_TOTAL = 28 * 1024 * 1024

def verify(folder: Path) -> dict[str, dict]:
    manifest: dict[str, dict] = {}
    for file in sorted(folder.rglob("*")):
        if not file.is_file():
            continue
        rel = file.relative_to(folder).as_posix()
        if rel == "bundle_manifest.json":
            continue
        if not rel.startswith(PREFIXES):
            raise ValueError("Unexpected data file: " + rel)
        if file.is_symlink():
            raise ValueError("Symlinks not allowed: " + rel)
        size = file.stat().st_size
        if size <= 0 or size > MAX_FILE:
            raise ValueError("Invalid file size for " + rel)
        manifest[rel] = {"size": size, "sha256": hashlib.sha256(file.read_bytes()).hexdigest()}
    for rel, expected in REQUIRED.items():
        if rel not in manifest:
            raise ValueError("Missing " + rel)
        with (folder / rel).open("r", encoding="utf-8") as stream:
            payload = json.load(stream)
        if not isinstance(payload, list) or len(payload) != expected:
            raise ValueError(f"{rel} contains {len(payload)} entries; expected {expected}")
    for rel in MANDATORY:
        if rel not in manifest:
            raise ValueError("Missing " + rel)
    if len(manifest) < 40:
        raise ValueError(f"Only {len(manifest)} files; expected the complete 40-file catalog")
    return manifest

def bundle(source: Path) -> None:
    if not source.is_file():
        raise FileNotFoundError(source)
    with tempfile.TemporaryDirectory(prefix="hecker-islam-reference-") as directory:
        temp = Path(directory) / "reference"
        temp.mkdir()
        bytes_read = 0
        with ZipFile(source) as archive:
            for item in archive.infolist():
                if not item.filename.startswith("assets/") or item.is_dir():
                    continue
                relative = item.filename[len("assets/"):]
                if not relative.startswith(PREFIXES):
                    continue
                safe = Path(relative)
                if safe.is_absolute() or ".." in safe.parts or "\\" in relative:
                    raise ValueError("Invalid archive entry: " + relative)
                if item.file_size > MAX_FILE:
                    raise ValueError("Entry too large: " + relative)
                bytes_read += item.file_size
                if bytes_read > MAX_TOTAL:
                    raise ValueError("Archive exceeds safety limit")
                target = temp / safe
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(item) as raw, target.open("wb") as stream:
                    shutil.copyfileobj(raw, stream)
        manifest = verify(temp)
        (temp / "bundle_manifest.json").write_text(
            json.dumps({"source": source.name, "version": 1, "files": manifest},
                       ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        backup = DEST.with_name(DEST.name + "_old")
        if backup.exists():
            shutil.rmtree(backup)
        if DEST.exists():
            DEST.rename(backup)
        try:
            shutil.move(str(temp), str(DEST))
        except Exception:
            if backup.exists():
                backup.rename(DEST)
            raise
        if backup.exists():
            shutil.rmtree(backup)
        print(f"Bundled {len(manifest)} offline catalogs ({bytes_read:,} bytes) into {DEST.relative_to(ROOT)}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", nargs="?", type=Path, help="authorized reference APK or content ZIP")
    parser.add_argument("--verify", action="store_true", help="fail if required bundled catalogs are absent")
    args = parser.parse_args()
    try:
        if args.verify:
            files = verify(DEST)
            print(f"Standalone content verified: {len(files)} files")
        elif args.source:
            bundle(args.source)
        else:
            parser.error("Specify a source APK/ZIP or --verify")
    except (FileNotFoundError, ValueError, KeyError, OSError, json.JSONDecodeError) as ex:
        print("Offline asset verification failed:", ex, file=sys.stderr)
        sys.exit(1)
