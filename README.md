# Hecker Islam

An offline-first unified Islamic Android app, combining Hecker's Calendar and Azaan projects and a reference-inspired Material 3 interface.

## Included features

- **Shia Azaan:** Ja'fari and Tehran prayer calculations, three daily announcements, adjustable offsets, imported recordings and notifications
- **Calendar:** Gregorian / Hijri / Desi dates, Islamic occasions and customization
- **Qur'an:** 114 chapters, 6,236 Uthmani verses, reader, offline search, bookmarks
- **Azkar:** 11 categories, 133 chapters, 287 supplications with translations/repeat counts and saved items
- **Hadith:** 3,574 searchable records, 493 categories and bookmarks
- **Prayer tracking:** privately saved history of five daily prayers
- **Qibla and Tasbeeh:** compass and saved remembrance counts
- **Home UI:** rounded Material-inspired cards, floating nav bar, Material You dynamic colors, dark/light mode and animated interaction

## Standalone APK: bundled offline data

**No manual import is required inside the app.** On `fix/standalone-offline-expressive`,
all 40 authorized catalog files are embedded into `app/src/main/assets/reference`
when built for release. The original Salat Times reference APK is only
needed by the maintainer **one time** to commit the source data into Git.

See **[Standalone build guide](docs/STANDALONE_BUILD.md)** to embed the assets. 
GitHub Actions intentionally **will not upload an incomplete APK**: the
`hecker-islam-standalone-debug-apk` artifact is published only after
the offline dataset verification succeeds.

## Local build

Android SDK 35, JDK 17, Gradle 8.11.1:

```bash
python3 tools/bundle_reference_assets.py --verify
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The UI is still being brought toward the reference application's exact
Compose animation and layout behavior. This Java-native integration is
not yet pixel-perfect; see [Feature parity](docs/FEATURE_PARITY.md).

The Calendar and Azaan source repositories remain unchanged.
Hecker Islam has its own application ID `com.hecker.islam`.
