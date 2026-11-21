# Hecker Islam — Mola Ali Mojza reader

## Shipped content

The Mojzat catalog at `app/src/main/assets/mojzat/catalog.json` contains only **Mola Ali (A.S.) — the Iranian woodcutter story**. It is provided in Urdu and Roman Urdu. The previously included Bibi Fatima (S.A.) retelling was withdrawn from the app at the user's request because its accuracy was uncertain.

This is a traditional devotional narrative rather than an authenticated hadith.

## Reader features

- Urdu and Roman Urdu toggle with Urdu right-to-left layout.
- Recite using the installed Urdu Android text-to-speech voice; the reader uses the foreground playback service.
- Adjustable font size and recitation speed, bookmarks, source information and sharing.

## Nahj al-Balagha quote sharing

The dashboard's WhatsApp Status card uses a dynamically rendered 9:16 image: dark teal gradient, gold ornamentation, framed Urdu quote and a centered attribution to **امام علی علیہ السلام** below the frame, separated with generous vertical spacing. The image does not carry Hecker Islam branding or a numbered wisdom reference. A preview appears inside the app before sharing; users choose My Status within WhatsApp.

## QA checks

1. Confirm only one Mojza story appears in the catalog and the dashboard/More show the Mola Ali shortcut.
2. Open the story in Urdu and Roman Urdu and verify bookmarks, playback and text sharing.
3. Open the status preview for short and long Nahj al-Balagha quotes; ensure the attribution remains below the panel without overlapping its border.
4. Verify sharing to WhatsApp Status and other apps.
5. Run `gradle testDebugUnitTest lintDebug assembleDebug` and install the updated APK.
