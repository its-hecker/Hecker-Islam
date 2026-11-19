#!/usr/bin/env python3
"""Export user-authorized offline catalogs from the reference APK for the in-app importer."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import sys

PREFIXES = ('assets/quran/', 'assets/azkar/', 'assets/hadith/',
            'assets/location/', 'assets/adhan/', 'assets/adhan_background/')
if len(sys.argv) != 3:
    raise SystemExit('Usage: python3 tools/extract_reference_content.py <source.apk> <output.zip>')
source, output = map(Path, sys.argv[1:])
if not source.is_file():
    raise SystemExit('Reference APK does not exist: ' + str(source))
count = 0
with ZipFile(source) as apk, ZipFile(output, 'w', compression=ZIP_DEFLATED, compresslevel=7) as dest:
    for name in apk.namelist():
        if name.startswith(PREFIXES) and not name.endswith('/'):
            dest.writestr(name, apk.read(name))
            count += 1
print('Saved', count, 'authorized data files to', output)
