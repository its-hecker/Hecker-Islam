# Hecker Islam — Mojzat reader

## Shipped catalog

The app currently includes two traditional devotional narratives in `app/src/main/assets/mojzat/catalog.json`:

- **Mola Ali (A.S.) — Iran ka Lakrihara:** Urdu and Roman Urdu presentations.
- **Bibi Fatima (S.A.) — معجزۂ جناب سیدہ طاہرہ:** an original Urdu retelling based on user-provided screenshots of non-consecutive scanned pages. Since some pages were not supplied, this is NOT a verbatim or complete transcription and does not claim a verified religious chain of narration.

These are majlisi/traditional stories, **not authenticated hadith**. The image-based Fatima retelling has no external URL because the visible Scribd address was incomplete; the source is described as user-provided scanned excerpts rather than a fabricated link.

## Reader features

- The Mola Ali story retains the Urdu/Roman Urdu selector; Bibi Fatima is Urdu-only.
- Urdu text is laid out right-to-left with adjustable text size.
- Recitation uses device Urdu text-to-speech from the shared foreground playback service. An installed Urdu TTS voice is required. Recitation speed is adjustable.
- Bookmarking and sharing work for both entries; the original Mola Ali source link is preserved.

## Related changes

- The Quran reader can speak the selected bundled Shia Urdu translation for a Surah using Urdu TTS, with pause/resume and the same speed picker. Arabic recitation remains Mishary Alafasy's streamed recording.
- The dashboard Nahj al-Balagha wisdom card can generate a vertical quote image and share it through WhatsApp; users choose **My Status** in the WhatsApp share flow.

## QA

1. Confirm both Mojzat cards appear and render in the correct scripts.
2. Test Urdu narration with a device that has an Urdu voice and verify pause/resume while the screen is off.
3. Open a Surah, enable Urdu translation and tap **Listen • اردو ترجمہ**. Confirm changing the Urdu translator changes the text to be spoken.
4. Share a dashboard wisdom quote to WhatsApp and select **My Status**, checking the story image and reference number.
5. Run `gradle testDebugUnitTest lintDebug assembleDebug` and install the resulting APK.
