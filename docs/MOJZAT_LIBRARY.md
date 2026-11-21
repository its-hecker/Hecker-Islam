# Hecker Islam — Mola Ali Mojza reader

## Shipped content

The Mojzat catalog at `app/src/main/assets/mojzat/catalog.json` contains only **Mola Ali (A.S.) — the Iranian woodcutter story**. It is provided in Urdu and Roman Urdu. The previously included Bibi Fatima (S.A.) retelling was withdrawn from the app at the user's request because its accuracy was uncertain.

This is a traditional devotional narrative rather than an authenticated hadith.

## Reader features

- Urdu and Roman Urdu toggle with Urdu right-to-left layout.
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

1. Confirm only one Mojza story appears in the catalog and the dashboard/More show the Mola Ali shortcut.
2. Open the story in Urdu and Roman Urdu and verify bookmarks, playback and text sharing.
3. Open the status preview for short and long Nahj al-Balagha quotes; ensure the attribution remains below the panel without overlapping its border.
4. Verify sharing to WhatsApp Status and other apps.
5. Tap the next icon through the entire set of quotes and verify no repeats until all have appeared; restart the app mid-cycle and confirm progress is retained.
6. Run `gradle testDebugUnitTest lintDebug assembleDebug` and install the updated APK.
