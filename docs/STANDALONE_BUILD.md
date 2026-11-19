# Standalone packaging — no app imports

The original Salat Times APK contains approximately 7.6 MB of offline Qur'an,
Azkar, Hadith, city, Azaan and background catalogs (40 files). Hecker Islam must
ship these inside `app/src/main/assets/reference/`, **not** ask customers to
select an APK/ZIP after installing. The build now reads only those packaged assets.

## One-time maintainer setup

1. Check out `fix/standalone-offline-expressive`.
2. Copy your friend's authorized reference APK anywhere on your build computer.
3. From the repository root, run:

```bash
python3 tools/bundle_reference_assets.py /path/to/ST-261104-v1.7.0-stable-317dffb.apk
python3 tools/bundle_reference_assets.py --verify

git add app/src/main/assets/reference
git -c user.name=Hecker -c user.email=hecker@users.noreply.github.com commit -m "assets: ship complete offline library"
git push origin fix/standalone-offline-expressive
```

Or unpack the provided `HeckerIslam-Standalone-Offline-Assets.zip`
**at the Git repository root** instead of running the bundling script; it contains
the exact paths under `app/src/main/assets/reference/`.

Once these files are pushed, **every future GitHub Actions APK includes the complete
offline library without any user setup**. The workflow checks counts before
publishing the standalone APK artifact; it intentionally does not upload
an incomplete build.

## Verify

```bash
python3 tools/bundle_reference_assets.py --verify
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep 'assets/reference/quran/quran_uthmani.json'
```

Expected data: 114 Qur'an chapters, 6,236 verses, 11 Azkar categories,
133 Azkar chapters, 287 items, 493 Hadith categories, 3,574 Hadith entries
and the full city catalog. The bundled Brotli Hadith file can be read
using the app's `org.brotli:dec` dependency.

The pack includes **catalog metadata** for remote reciters; remote sound files
not physically included in the original APK cannot be bundled without
separate audio sources. The app's existing locally imported recitations remain
supported as a user choice, not as an app-setup requirement.

## UI & motion

The UI uses Android native cards with Material You dynamic colors, a raised
pill navigation bar, staggered entrances, spring-like press animations,
screen fades, and an animated Qibla compass and Tasbeeh counter. This
approximates the original app's Material 3 Expressive interactions; this
branch does **not** claim frame-perfect Compose layout and animation parity.

As authorized, reference content is taken only from the supplied APK.
For redistribution, ensure appropriate licensing for any separately owned
fonts, audio recordings or third-party media.
