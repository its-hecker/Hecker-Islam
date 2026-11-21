# Hecker Islam — Mojzaat reader

## Shipped content

The Mojzat catalog at `app/src/main/assets/mojzat/catalog.json` currently contains three Urdu-script stories: **Mola Ali (A.S.) — the Iranian woodcutter**, **Sayyida Fatima Zahra (S.A.) — the sunaran story**, and **Hazrat Abbas (A.S.) — the four-page scanned narrative**. The first two entries were present before this update and are not modified here. The Hazrat Abbas story was manually transcribed from user-supplied photos in page order **33 → 34 → 35 → 36**, with paragraph separators preserving page breaks. Its source is the supplied scanned pages, not a verified hadith reference.

This is a traditional devotional narrative rather than an authenticated hadith.

## Reader features

- Urdu right-to-left reader; the language toggle remains available for stories containing a Roman Urdu version.
- Recite using the installed Urdu Android text-to-speech voice; the reader uses the foreground playback service.
- Adjustable font size and recitation speed, bookmarks, source information and sharing.

## Nahj al-Balagha quote sharing

The dashboard's WhatsApp Status flow now renders **eight** different 9:16 artwork templates (Emerald Classic, Midnight Minimal, Ivory Manuscript, Royal Garnet, Ocean Split, Garden Glow, Bronze Heritage, Moonlit Paper). Successive share previews rotate through all eight styles before repetition, with their progress remembered across app launches. The preview includes a **Change design** action to select the next template, and the same preview image is exported to WhatsApp/other apps. Templates vary the palette, panel framing, ornamentation and quote typography/alignment. Every image keeps the legible Urdu quote with **امام علی علیہ السلام** below the quotation area, without Hecker Islam branding or a numbered wisdom reference.

## Dashboard wisdom rotation

- Wisdom quotes are dealt from a shuffled, persistent no-repeat deck. All available dashboard quotes are shown before a new cycle begins.
- The current quote and next position are saved across app restarts; UI refreshes do not advance the deck.
- A small **next** icon beside the dashboard wisdom heading advances immediately.
- Automatic rotation still works, but draws from the same no-repeat deck.
- At a new cycle boundary, the previous quote is excluded from the first draw so no immediate repeat occurs.

## QA checks

1. Confirm the Mojza index shows all three entries, including `معجزہ حضرت عباس علیہ السلام`; open Abbas and verify the story begins on page 33, continues in page order, and ends with the page 36 conclusion.
2. Open the story in Urdu and Roman Urdu and verify bookmarks, playback and text sharing.
3. Open the status preview for short and long Nahj al-Balagha quotes; ensure the attribution remains below the panel without overlapping its border.
4. Verify sharing to WhatsApp Status and other apps.
5. Tap the next icon through the entire set of quotes and verify no repeats until all have appeared; restart the app mid-cycle and confirm progress is retained.
6. Run `gradle testDebugUnitTest lintDebug assembleDebug` and install the updated APK.
