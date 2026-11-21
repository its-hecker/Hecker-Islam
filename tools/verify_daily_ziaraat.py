#!/usr/bin/env python3
"""Validate the bundled seven-day Ziyarat catalog before APK assembly."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/assets/ziaraat/daily.json"
EXPECTED = {
    1: {"sun_ali", "sun_fatima"},
    2: {"mon_has", "mon_husayn"},
    3: {"tue_three"},
    4: {"wed_four"},
    5: {"thu_askari"},
    6: {"fri_mahdi"},
    7: {"sat_prophet"},
}

def main():
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    assert data["schemaVersion"] == 1
    entries = data["ziaraat"]
    assert len(entries) == 9, "Expected nine daily ziaraat"
    found = {day: set() for day in EXPECTED}
    for z in entries:
        day, uid = z["day"], z["id"]
        assert day in EXPECTED, f"Invalid weekday {day}"
        assert uid not in found[day], f"Duplicate {uid}"
        found[day].add(uid)
        for field in ("name", "urduName", "arabic", "urdu", "source"):
            assert isinstance(z[field], str) and z[field].strip(), (uid, field)
        assert z["source"].startswith("https://"), uid
        assert len(z["arabic"]) >= 130, f"Arabic too short for {uid}"
        assert len(z["urdu"]) >= 150, f"Urdu too short for {uid}"
        assert len(z["arabic"].splitlines()) > 1, f"Expected multiple lines for {uid}"
    assert found == EXPECTED, f"Weekday entries don't match: {found!r}"
    print("PASS: Nine sourced Arabic/Urdu Ziaraat across seven weekdays")

if __name__ == "__main__":
    try:
        main()
    except (KeyError, AssertionError, ValueError) as exc:
        print(f"FAIL: daily ziaraat validation: {exc}", file=sys.stderr)
        raise SystemExit(1)
