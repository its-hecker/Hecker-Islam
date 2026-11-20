#!/usr/bin/env python3
"""Bundle Shia Urdu Qur'an translations in a standalone APK at build time.

Data is fetched from the AlQuran Cloud API (https://alquran.cloud/api);
translation copyright remains with the named translators. Attribution is
displayed inside Hecker Islam. Do not modify or misattribute translation text.
"""
import json
import pathlib
import time
import urllib.error
import urllib.request

BASE = "https://api.alquran.cloud/v1/quran/"
DEST = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/reference/quran"
EDITIONS = {
    "ur.jawadi": ("Syed Zeeshan Haider Jawadi", "ur_jawadi.json"),
    "ur.najafi": ("Muhammad Hussain Najafi", "ur_najafi.json"),
}
EXPECTED = 6236

def download(edition):
    url = BASE + edition
    last_error = None
    for attempt in range(4):
        try:
            request = urllib.request.Request(
                url, headers={"Accept": "application/json", "User-Agent": "HeckerIslam/1.0 (educational Quran reader)"}
            )
            with urllib.request.urlopen(request, timeout=40) as response:
                raw = response.read(12 * 1024 * 1024 + 1)
            if len(raw) > 12 * 1024 * 1024:
                raise ValueError("Translation API response exceeds maximum size")
            payload = json.loads(raw)
            if payload.get("status") != "OK":
                raise ValueError("API response was not successful")
            data = payload.get("data", {})
            if data.get("edition", {}).get("identifier") != edition:
                raise ValueError("Translation edition mismatch")
            ayahs = data.get("ayahs", [])
            if len(ayahs) != EXPECTED:
                raise ValueError(f"Expected {EXPECTED} verses, received {len(ayahs)}")
            rows, seen = [], set()
            for ayah in ayahs:
                surah = ayah.get("surah", {}).get("number")
                number = ayah.get("numberInSurah")
                translation = ayah.get("text")
                if not isinstance(surah, int) or not 1 <= surah <= 114:
                    raise ValueError("Invalid surah index")
                if not isinstance(number, int) or number < 1 or number > 286:
                    raise ValueError("Invalid ayah index")
                if not isinstance(translation, str) or not translation.strip():
                    raise ValueError("Missing translated text")
                key = (surah, number)
                if key in seen:
                    raise ValueError("Duplicate ayah")
                seen.add(key)
                rows.append([surah, number, translation])
            rows.sort(key=lambda row: (row[0], row[1]))
            return rows
        except (urllib.error.URLError, TimeoutError, ValueError, KeyError, json.JSONDecodeError) as err:
            last_error = err
            print(f"{edition} download attempt {attempt + 1} failed: {err}", flush=True)
            time.sleep(min(2 ** attempt, 8))
    raise RuntimeError(f"Could not bundle {edition}: {last_error}")

def main():
    DEST.mkdir(parents=True, exist_ok=True)
    for edition, (translator, filename) in EDITIONS.items():
        rows = download(edition)
        target = DEST / filename
        target.write_text(json.dumps(rows, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        print(f"Bundled {len(rows)} Urdu ayahs by {translator}: {target.name}", flush=True)

if __name__ == "__main__":
    main()
