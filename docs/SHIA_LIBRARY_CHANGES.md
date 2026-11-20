# Shia content, current location and Nahj al-Balagha update

This change is based on `fix/standalone-offline-expressive`, not the old one-file `main` branch.

## Current location
- Requests foreground coarse/fine permission, and uses an existing fix if it is younger than ten minutes.
- Falls back to parallel network/GPS listeners, with a twenty-second timeout and choices to retry or select a city.
- Recalculates the prayer times and reschedules alerts after a new fix.
- Location is only requested by the user; the app does not request background location.
- Test on-device with Location enabled and disabled, approximate/precise permission, indoors, and outside Pakistan.

## Shia duas
- Adds **Dua Imam-e-Zamana (Dua al-Faraj)** and **Dua-e-Ahad**.
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
