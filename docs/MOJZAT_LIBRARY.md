# Hecker Islam — Mola Ali Mojza reader

## Current offline content

Only **Mola Mushkil Kusha (A.S.) aur Iran ka Lakrihara** remains in the shipped catalog at `app/src/main/assets/mojzat/catalog.json`. Previously listed stories about Hazrat Abbas and Bibi Fatima are no longer included in this section, nor are their categories or source links.

The narrative follows the woodcutter of Iran, his daughter, burnt firewood, stones, precious jewels, royal necklace, imprisonment and reunion. It is a traditional majlis/devotional retelling, **not an authenticated hadith or a proven historical report**. The source link is preserved for context.

## Reading and voice

- Choose **اردو** or **Roman Urdu** using the two reading buttons inside the story. Urdu is the default; the selection persists.
- Both scripts are included offline. The Urdu adaptation is a retelling rather than a verbatim source transcription.
- Recitation always reads **Urdu script with the installed Urdu Android text-to-speech voice**, even when the Roman Urdu display is selected. This avoids handing Latin-script transliteration to an English voice, which mispronounces Urdu words.
- The device must have an Urdu TTS voice installed and enabled for speech to work. Unsupported devices show a voice-installation message; the app does not claim to bundle a human narration.
- Recitation speed is shared with Dua and Hadith reading and persists in `islam_reader/tts_speed`: 0.65x, 0.80x, 1.00x, 1.20x, 1.40x and 1.60x.
- Font-size adjustment, bookmarking, source link, sharing and the app's bottom navigation remain.

## QA checklist

1. Open Mojzaat from the dashboard and More. Confirm only the Mola Ali card appears.
2. Open the story, switch Urdu ↔ Roman Urdu, exit and re-enter. Verify the language selection persists.
3. Confirm Urdu RTL layout, both full story scripts, font controls, bookmarks, share text and source link.
4. Test recitation on a device with an Urdu TTS voice, then one without it. Verify the installed voice is used and no silent English fallback mispronounces the Urdu text.
5. Confirm speech speed persists across Mojzaat, Duas and Hadith.
6. Run `gradle testDebugUnitTest lintDebug assembleDebug` through the Android workflow and install the built APK.
