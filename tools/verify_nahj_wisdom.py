#!/usr/bin/env python3
"""Fail builds when the Nahj al-Balagha offline catalog is incomplete."""
import json
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "app/src/main/assets/nahj/sayings_480.json"
data = json.loads(path.read_text(encoding="utf-8"))
items = data["sayings"]
assert len(items) == 480, f"Expected 480 sayings, got {len(items)}"
assert [x["number"] for x in items] == list(range(1, 481)), "Numbering is missing or duplicated"
assert all(len(x["arabic"].strip()) >= 12 for x in items), "Missing original Arabic"
assert all("urdu" in x for x in items), "Missing Urdu-availability field"
meaning_count = sum(bool(x["urdu"].strip()) for x in items)
assert meaning_count >= 50, f"Original Urdu paraphrases unexpectedly missing: {meaning_count}"
print(f"Nahj collection verified: {len(items)} unique numbered Arabic sayings, {meaning_count} original Urdu summaries")
