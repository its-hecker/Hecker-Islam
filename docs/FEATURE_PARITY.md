# Salat Times 1.7.0 reference parity

User-provided, owner-authorized APK: `ST-261104-v1.7.0-stable-317dffb.apk`.

## Implemented on `fix/standalone-offline-expressive`

- Hecker's Calendar and Azaan functionality from their established repositories
- Animated Material-inspired UI: dynamic color, round surfaces, floating bottom bar, tab-press spring, staggered entrance
- Prayer-time dashboard with countdown
- Offline Qur'an (114 Surahs, 6,236 verses), Azkar (11 categories, 133 chapters, 287 entries) and Hadith (3,574 entries, 493 categories) readers
- Bookmarks, search, saved Tasbeeh, daily five-prayer tracking
- Qibla bearing with smooth device compass movement
- Nearby mosque search through another installed maps app
- Light/dark palettes
- **Bundled reference databases; no app-side ZIP/APK picker or content download**
  (maintainer must commit the asset pack as described in STANDALONE_BUILD.md)

## Partial/not yet exact

- Original application is Jetpack Compose; this project's main shell still uses native Android Views rather than the same Compose implementations and motion specs
- Full original prayer schedule, advanced calculation choices, regional rules, fasting/night-time data
- Reciter audio catalog metadata is bundled, but some listed remote recordings are not included inside the reference APK
- Monthly, moon and Sun home widgets, exact launcher and widget previews
- Detailed map tiles, built-in live routing and satellite search
- Hijri export and exact customization settings, transitions, fonts, layouts and gestures
- Full original notifications, multi-language options and encrypted backup/restore
- All exact per-screen animations and layouts still require runtime inspection and rebuild

The reference assets are copied by permission of the original app owner. The app
does not include someone else's bundled executable code. Check third-party
font/audio licensing separately before distributing.
