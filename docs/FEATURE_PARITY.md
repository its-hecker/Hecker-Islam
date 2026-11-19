# Hecker Islam — reference feature parity

Reference: personally supplied Salat Times `ST-261104-v1.7.0-stable-317dffb.apk` (permission to reuse granted by owner).

**What is present on branch `feat/reference-ui-parity`:**
- New Material 3 Expressive-style home with rounded prayer card, navigation tabs, appearance toggle, prayer schedule, and themed feature shortcuts
- Existing Hecker Calendar: Gregorian / Hijri / Desi views, religious occasions, themes
- Existing Azaan app: Ja'fari calculations, alarms, combined announcements, local settings, custom recitations
- Qibla compass with true-north correction
- Tasbeeh with persistent counts
- Qur'an: all 114 chapter metadata, 6,236 original Uthmani verses, surah listing, verse search, bookmarks
- Azkar: 11 categories, 133 chapters, 287 entries, Arabic/English, repeat counts, references and bookmarks
- Hadith: 3,574 records, 493 category metadata, search and bookmarks, Arabic/English details and explanations
- Prayer tracker: five daily prayers and daily local persistence
- Offline reference-data importer: accepts original APK and the reference-content-pack ZIP, copies only whitelisted catalogs locally
- Nearby mosques: launches device maps app at the configured coordinates to search for mosques
- Light/dark theme switch

**Content packaging:** the user-provided APK includes 40 offline catalogs. Since binary assets are intentionally kept out of GitHub source, choose "Import offline library" in Hecker Islam and select either the original APK or the reference-content-pack ZIP. The importer validates required content and prevents path traversal. The 65-entry Azaan sound catalog is metadata; the original APK does not embed every audio file in the catalog. Existing Hecker Azaan audio importing is preserved.

**Not yet exact:** The reference is a compiled Jetpack Compose APK, not the editable Compose source. A screenshot-perfect port of all settings, animations, reciters, moon/Sun widgets, hijri export, advanced prayer calculations, local backup/restore, mosque satellite maps, notifications and all language options still requires development and runtime tests. This branch is an iterative native implementation, not a claim of full parity.

**Notes:** No third-party executable code is repackaged and no API key has been extracted. Religious text displayed from the provided APK should be proofread against trusted sources before publishing. Keep the owner’s copyright attribution when distributing authorized text, media and branding.
