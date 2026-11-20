# Shia Mojzat — Urdu miracle and karamat story library

## Scope

Hecker Islam includes an offline-first native Urdu reader for popular Shia **معجزات و کرامات** narratives. The initial 8-entry collection includes:

1. Imam Ali (a.s.) — the Iranian woodcutter and the Mola Mushkil Kusha tale (featured)
2. Bibi Fatima (s.a.) — the Sunaran / goldsmith's wife, child and veiled lady (featured)
3. Ghazi Abbas Alamdar (a.s.) — the sick child at the shrine (featured)
4. Ghazi Abbas Alamdar (a.s.) — Shaykh Tustari and the two bags
5. Ghazi Abbas Alamdar (a.s.) — the dream/intercession story
6. Bibi Fatima (s.a.) — the wedding narrative, included in the Sunaran tale
7. Bibi Fatima (s.a.) — the two lost girls, included in the Sunaran tale
8. Imam Ja'far al-Sadiq (a.s.) — the separate woodcutter/Konday narrative

The app also offers links to larger Urdu books and collections to continue browsing beyond these eight entries.

## Content policy

- The stories are **original Urdu retellings of publicly accessible accounts**, not pasted passages from books or websites.
- Popular majlis tales, later reports and dream accounts are clearly labelled. A religious attribution is not itself a verification of historicity or hadith authenticity.
- Avoid promises of guaranteed cures, healing, or outcomes, or claims that any practice is a required part of religion.
- Keep source URLs and status notes for all additions, particularly when users submit stories.
- This is **not an exhaustive collection of every miracle ever reported**. Add reviewed entries via `app/src/main/assets/mojzat/catalog.json`, using stable `id`, `category`, `title`, `subtitle`, `label`, `text`, `source`, and `sourceTitle` fields.

## UI and functionality

- Native Material You-themed rounded panels and existing persistent bottom navigation.
- Featured stories; categories for Imam Ali, Hazrat Abbas, Bibi Fatima, Imam Ja'far Sadiq; Urdu text search; favourites.
- Reading mode with full offline Urdu narrative, text resizing, sharing, link to attributed source and Urdu text-to-speech recitation (Android device voice required).
- TTS speech engine is an Android synthesized reader, not a prerecorded reciter.
- Existing calendar and Azaan UI are unchanged.

## Verification

GitHub Actions: Android unit tests, lint and assembleDebug. On-device test: RTL layout, focus while searching, category filters, bookmarks, all featured stories, recitation start/stop, background/return navigation, and source links.
