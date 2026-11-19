# Reference APK feature inventory and implementation status

Reference file provided by the project owner: `ST-261104-v1.7.0-stable-317dffb.apk`, package `islamalorabi.salat.times`.

Inspection basis: app package strings, bundled data catalogs, component names and class references. These indicate modules, not proof that every module works in every location/build.

| Feature | Reference APK evidence | Hecker Islam status |
| --- | --- | --- |
| Prayer calculations / Azaan | prayer models, receivers, 65-entry audio catalog | **Integrated** from existing Shia Azaan |
| Calendar | Hijri calendar and month export classes | **Integrated** Calendar 2.3 with Gregorian/Hijri/Desi |
| Qibla | Qibla shortcut class | **Initial functional compass** |
| Tasbeeh | Tasbeeh shortcut, counter database, widgets | **Initial local counter** |
| Qur'an | 114 chapter metadata, 6,236 Uthmani verses catalog, Mushaf classes | **Planned**, appropriately licensed content |
| Azkar / Dhikr | 11 categories and 287-item catalog | **Planned** |
| Hadith | 3,574 hadith, 493 categories, multilingual index and bookmarks | **Planned** |
| Mosques nearby | mosque data model and shortcut class | **Planned**; optional online map and consent |
| Prayer tracker | tracked-prayer entity, reminders and widgets | **Planned** |
| Azaan backgrounds | 13-background catalog | **Planned**; only permissively licensed replacements |
| Hijri exports | month export formats and calendar modes | **Planned** |
| Custom widgets | prayer grid/list, month, moon, sun | **Planned** |
| Customization | blur-style, tint, status-bar models | **Planned** original Material-style UI |
| Data backup/restore | encrypted backup, settings, favourites, reading progress | **Planned** |
| Qur'an/Hadith bookmarks | reading progress and favourites storage | **Planned** |
| Location catalog | 33,742 city records | **Existing** Azaan city/manual GPS support; enhanced search planned |

This is a clean-room, owner-controlled implementation. No third-party APK data or binary contents have been copied into the repository.
