# Hecker Islam — Mojzat and recitation library (Roman Urdu)

## Current catalog

The previous eight short Urdu summaries have been replaced by a four-entry Roman Urdu catalog, presented in a left-to-right reader with a source link and a clearly stated narration status.

1. **Mola Mushkil Kusha (A.S.) aur Iran ka Lakrihara** — based on the two uploaded screenshots and the matching public Urdu version, covering the daughter, destroyed firewood, precious stones, royal necklace, imprisonment and ending. Reference: https://islamicdate.com.pk/mojza-mola-mushkil-kusha/
2. **Chat Pat Bibi ka Mojza** — from the two uploaded Paheliyan.pk screenshots, the Nasrani woman's story and the linked account of the Prophet's family and Janab-e-Fizza. References: https://paheliyan.pk/chat-pat-bibi-ka-mojza/ and https://kahaniyan.org/chat-pat-bibi-ka-mojza/
3. **Janab-e-Sayyeda (S.A.) ki Kahani — Sunaran** — a retelling of the illustrated Sunaran story. The uploaded Scribd screenshots display only part of the document, not all 49 pages, so the app does not represent this as a full transcription.
4. **Mola Ghazi Abbas (A.S.) — Umm-ul-Banin aur Wafa** — the reliably documented family and Karbala background, keyed to the opening phrase provided by the user. The requested *specific* miracle narrative beginning "Janab-e-Fatima ki wafat ke baad Umm-ul-Banin ke batan se aik bacha paida hoga..." has **not** been located in a source that establishes its complete story. The app does not invent the rest. Reference: https://alkafeel.net/news/index?id=12368&lang=ur

The catalog lives at `app/src/main/assets/mojzat/catalog.json`. The old 8 entries are absent. Retellings are original prose, not word-for-word reproductions of third-party text.

## Reader UI

- Roman Urdu content renders left-to-right and supports font size changes.
- Categories: All, Mola Ali, Ghazi Abbas, Bibi Sayyeda and Saved; search supports Roman words.
- Existing persistent bottom navigation, dark theme, reader cards, sharing, saved stories and links are retained.
- Roman Urdu is read using the Android device text-to-speech engine. English/Latin-script voices may mispronounce Roman Urdu. A human narration is not bundled.

## Recitation speed

`ReaderSpeech` provides a persistent, shared speed selector with **0.65x, 0.80x, 1.00x, 1.20x, 1.40x and 1.60x**. It is available in Mojzat, Shia Duas, Shia Hadith and sourced web readers. Changing speed stops queued speech; tapping Recite starts playback at the selected speed. The setting is stored in `islam_reader` preferences, key `tts_speed`.

## Religious and historical status

Popular majlis stories, dreams and miracle retellings should not be represented as authenticated hadith or independently established historical facts. The Janab-e-Sayyeda entry is incomplete relative to the pictured book, and the exact Ghazi Abbas miracle requested remains unlocated. Do not treat customary niyaz details as compulsory religious rulings.

## Verification

On-device QA: open each of the four stories, test left-to-right layout, navigation, bookmarks, search/filter, change speed, listen/stop, rotation, and tap source links. Also verify the saved speed in Dua, Hadith and external-page readers, and confirm a complete GitHub Actions debug APK build.
