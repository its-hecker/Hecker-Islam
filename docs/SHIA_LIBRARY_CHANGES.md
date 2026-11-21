# Shia content, current location and Nahj al-Balagha update

This change is based on `fix/standalone-offline-expressive`, not the old one-file `main` branch.

## Current location
- Requests foreground coarse/fine permission, and uses an existing fix if it is younger than ten minutes.
- Falls back to parallel network/GPS listeners, with a twenty-second timeout and choices to retry or select a city.
- Recalculates the prayer times and reschedules alerts after a new fix.
- The initial Azan city picker includes **Use current location (auto-update)** above the manual city list.
- Device location and manually chosen cities are separate persisted modes: geocoding no longer disables automatic tracking, while choosing a city or coordinates disables it.
- After opt-in, the dashboard checks the recent fix on each return and listens for movement while visible (300 m minimum change; network location preferred to save battery). Prayer alerts are recalculated after significant movement.
- The current neighborhood is reverse-geocoded where the Android geocoder supports it; otherwise coordinates are shown. Precise neighborhood names depend on available providers and location accuracy.
- Foreground location only: no background permission or continuous tracking while the app is closed. Changes appear while the app is open or when it is reopened.
- Test on-device with Location enabled and disabled, approximate/precise permission, indoors, and outside Pakistan.

## Daily Ziaraat (Saturday–Friday)
- A dedicated **Daily Ziaraat** screen is accessible in **Shia Duas**. It highlights the current weekday using the device's local calendar and lists the readings for every day.
- Nine traditional readings are bundled offline for seven days: Saturday (Prophet Muhammad), Sunday (Imam Ali and Sayyida Fatima), Monday (Imam Hasan and Imam Hussain), Tuesday (Imams Sajjad, Baqir and Sadiq), Wednesday (Imams Kazim, Reza, Jawad and Hadi), Thursday (Imam Hasan al-Askari), Friday (Imam Mahdi).
- Each entry has Arabic text, a **newly written Urdu translation**, adjustable device text-to-speech recitation in Arabic or Urdu, bookmarking and a source link.
- The Arabic wording is cross-checked against *Mafatih al-Jinan* weekday Ziaraat listed at https://www.ahkaam.org/ziyaraat and https://www.duas.org/week.htm. A textual variation may exist between publications; compare the linked traditional source. Urdu translations are explanatory renderings, not copied translations.
- These devotional readings are optional and do not form part of the obligatory prayers. The complete catalog ships in `assets/ziaraat/daily.json`, checked by `tools/verify_daily_ziaraat.py` in CI.

## After Namaz Ziaraat
- Adds an offline **After Namaz Ziaraat** shortcut and four reader entries: all three together, Imam Husayn/Hazrat Abbas, Imam Reza, and Imam-e-Zamana.
- Each entry has complete Arabic salutations and a carefully written Urdu rendering, selectable Arabic and Urdu device-voice recitation, speed controls and bookmarks.
- These are optional post-prayer devotions, not compulsory parts of Salah; the Arabic source is https://www.duas.org/ziarataftersalaat.htm.

## Maghfirat and Qabristan recitations
- Adds **Dua-e-Maghfirat (دعائے مغفرت)** for all living and deceased believers, using a short Arabic supplication from *Al-Kafi* (https://shialibrary.com/book/kafi/chapter/527065), with an original Urdu translation.
- Adds **Durood-e-Qabristan / Salaam-e-Qabristan (درودِ قبرستان / سلامِ اہلِ قبور)** using the cemetery greeting reported from Imam Ja'far al-Sadiq (a.s.) via Abdullah ibn Sinan at https://www.duas.org/mobile/visit-graveyard.html. This is a *salaam*, not a fixed salawat.
- Both entries work offline with Arabic and Urdu readings, device text-to-speech recitation, adjustable speed, and bookmarks; the source link requires internet.
- An index shortcut provides quick access to these supplications; wording is not presented as an entire funeral prayer.

## 0.4.5 — New supplications, Azaan and welcome
- **Dua-e-Yastasheer** appears under Shia Duas with an Urdu introduction and a linked complete source. Its introduction is not the full Urdu translation.
- **Dua-e-Bait al-Mamoor (دعائے بیت المعمور)** was subsequently restored at the user's request using the *newly supplied two photographed pages*, not the former link-only catalogue entry. The 11 printed Arabic lines and their Urdu explanations are displayed as alternating pairs over the same two-page sequence, bundled offline; Arabic and Urdu device-voice recitation, speed and bookmarking are supported. The wording is transcribed from the provided images and has not been independently authenticated against another edition.
- The bundled Nizar al-Qatari Azaan recording now begins after an introductory passage, seeking to the observed silence boundary at approximately 25.55 seconds. This only applies to the built-in recording, not imported audio; the spoken phrase boundary should be confirmed on-device.
- The app keeps the Aliyyun Waliullah devotional testimony in the display/TTS recitation preset and removes its on/off toggle. This preference must not be described as a universal religious obligation or a formal constituent of Adhan/Iqamah; see Sayyid Sistani's rulings https://www.sistani.org/english/book/48/2228/.
- Launcher begins with a Yes/No personal devotional confirmation. Accepting with **Don't remind me again** remembers consent locally, shows the requested greeting once, and skips the question on later launches. A No choice prevents entry while allowing reconsideration or exit. The dialog avoids derogatory language.
- The app never transmits the answer, and previous app data is preserved on upgrade.

## Shia duas
- Adds **Dua Imam-e-Zamana (Dua al-Faraj)** and **Dua-e-Ahad**.
- Adds **Hadith-e-Kisa (حدیثِ کساء)** to the Shia Duas catalogue, with Urdu introduction, bookmarking, and a linked in-app full Arabic/Urdu reader with Arabic and Urdu audio at FZIC. The complete narration and recitations require internet access.
- Short Arabic duas include original Urdu renderings offline.
- The longer duas show clearly labelled Urdu introductions, not misrepresented abbreviated translations.
- Links for Dua Kumayl, Tawassul, Nudba, Iftitah, Ahad and Ziyarat Ashura open verified complete Arabic/Urdu readings from FZIC **inside Hecker Islam**; other sources continue to open from Duas.org, where language availability differs.
- Network is needed for full-length linked readings and recitations.

## Hadith
- Hadith tab and dashboard lead to `ShiaHadithActivity`, an in-app viewer for **Al-Kafi, Man La Yahduruhu al-Faqih, Ghurar al-Hikam, Kitab al-Ghayba, Al-Amali and Thawab al-A'mal**, from Rewayaat.info.
- Old non-Shia/generic reader access is redirected. No authentication claim or grading is made about individual narrations.
- This first implementation is **online**, not a complete offline Shia hadith replacement. The older reference data may still remain in the build assets pending a dedicated content-pack migration.

## Nahj al-Balagha
- 480-number wisdom index. Individual sayings can be read in Urdu inside the app using `nehjulbalagah.com/hikmat/hikmat-N/`. Its edition contains 497 numbered sayings, so check numbering differences.
- Thirteen source-checked short sayings are additionally available offline, with concise original Urdu meanings and Arabic excerpts.
- Home dashboard rotates between these offline sayings every 90 seconds while visible, and shows the reference number.
- Remaining sayings use the in-app full Urdu source instead of fabricated offline translations.

## Credits
Top-right info icon displays the exact requested message:

Developed by Syed Ali Raza Kazmi

Special Credits to Sabber Kazmi

## Verification
Run the `android.yml` workflow, then install its standalone APK on a device and confirm: location permission, GPS/network fixes, Urdu text RTL, linked online readers, status bar/immersive insets, and navigation to/from Hadith and Nahj. Android WebView and internet access are required for linked full-text readers.
