# Hecker Islam

**Hecker Islam** combines Hecker's Calendar and Shia Azaan projects into one offline-first Android application. This initial integration preserves their separate original codebases and adds a new unified dashboard, Qibla compass, and Tasbeeh counter.

## Implemented in the integration branch

- **Calendar:** Gregorian, calculated Hijri and Desi dates, religious occasions (Wiladat/Eid/Shahadat), month and date navigation, Fatimiyya views, themes, fonts and accents. Uses the original Calendar 2.3 assets and activity.
- **Azaan:** Jaʿfari/Qom and Tehran prayer calculations, three daily announcements (Fajr, combined Dhuhr–Asr, combined Maghrib–Isha), Arabic recitation plus optional Urdu and English explanations, audio import/preview, local city/timezone selection, prayer offsets, reliable exact-alarm scheduling, and lockscreen countdown notifications where supported. Uses the Azaan app's feat/male-reciter-countdown source.
- **Unified dashboard:** Live upcoming-prayer summary, location status, and navigation to the Calendar, Azaan, Qibla and Tasbeeh.
- **Qibla:** Sensor-based direction relative to Kaaba, using the location selected in Azaan.
- **Tasbeeh:** Local counter and saved progress by remembrance phrase.

The existing **Calendar** and **Azaan** repositories remain untouched. This does not yet reproduce every function in the user-supplied Salat Times APK.

## Planned Salat Times reference parity

See [docs/FEATURE_PARITY.md](docs/FEATURE_PARITY.md). The reference was inspected for feature discovery only. The app does not redistribute third-party proprietary code, fonts, photos, recordings, translations, or branding. Qur'an, Hadith and Azkar modules require appropriately licensed/verifiable text sources.

## Build

Android Gradle Plugin 8.9.2, Gradle 8.11.1, JDK 17 and Android SDK 35:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions builds and uploads the debug APK for every push. If you need a Gradle wrapper locally, run `gradle wrapper --gradle-version 8.11.1` once and commit the generated wrapper files.

## Notes

- No account, ad SDK, Internet permission or background location permission.
- Because this is a new application ID (`com.hecker.islam`), settings from the previous two separate apps do not automatically migrate. Set city, calculation method and recordings again on first use.
- Azaan audio files must be provided/imported by the user. No copyrighted human reciter recording is bundled.
- Compare locally calculated prayer times against your trusted community timetable before using alerts.
