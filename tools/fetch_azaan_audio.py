#!/usr/bin/env python3
"""Bundle the verified Arabic muezzin recording used by the earlier Azaan project.

The URL is the one already checked by Azaan-app's recitation-source workflow.
Always verify SHA-256 so accidental HTML/changed audio is never published as an APK.
For redistribution, maintainers must have the appropriate recording permissions.
"""
from __future__ import annotations
import hashlib
import pathlib
import tempfile
import urllib.request

SOURCE = "https://shiavoice.com/stream-EhBAk"
EXPECTED_SHA256 = "d1d7cd97b416aeb34573ea86af1d79c2ec062a0ddaccf7652b997362ab74a329"
OUTPUT = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/azaan/nizar_al_qatari.mp3"
MAX_SIZE = 12 * 1024 * 1024

def verify(data: bytes) -> None:
    if not (data.startswith(b"ID3") or data.startswith(b"\\xff\\xfb")):
        raise RuntimeError("Audio source is not an MP3 recording")
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED_SHA256:
        raise RuntimeError("Recitation SHA-256 mismatch; source may have changed: " + digest)

def main() -> None:
    if OUTPUT.is_file():
        verify(OUTPUT.read_bytes())
        print("Verified previously bundled Nizar Al-Qatari recitation")
        return
    request = urllib.request.Request(SOURCE, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read(MAX_SIZE + 1)
    if len(data) > MAX_SIZE:
        raise RuntimeError("Recitation file exceeds size limit")
    verify(data)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(data)
    print(f"Bundled and verified {len(data)} bytes of offline Arabic azaan audio")

if __name__ == "__main__":
    main()
