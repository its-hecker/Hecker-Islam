# Hecker Islam: APK icons and trustworthy releases

## Launcher icon

The app manifest now references density-specific `@mipmap/ic_launcher` and
`@mipmap/ic_launcher_round` bitmap resources. GitHub Actions runs
`tools/generate_launcher_icons.py` to rasterize the existing committed
`ic_azaan.xml` artwork into all five densities before Gradle builds the APK.
This is intended to make APK icons visible in file managers that do not
support VectorDrawable previews. The installed app still uses the same design.

## Debug vs release APK

The normal Android workflow produces a **debug APK**, signed with a runner's
temporary debug certificate. It is suitable for local testing, not for trusted
public distribution. The signing certificate can change between CI builds.

The `Signed release` workflow is **manual** and intentionally refuses to run
without the developer's own long-lived signing key. Its APK is verified with
Android `apksigner` before upload. The signing key is not committed to GitHub.

Add the following four secrets to the repository's GitHub Actions Secrets:

- `HECKER_ISLAM_KEYSTORE_B64` — Base64 encoding of the existing release
  keystore, kept private
- `HECKER_ISLAM_STORE_PASSWORD` — keystore password
- `HECKER_ISLAM_KEY_ALIAS` — signing key alias
- `HECKER_ISLAM_KEY_PASSWORD` — key password

Then manually dispatch the **Signed release** workflow. Always use the **same
private release key** for future releases, and protect the keystore offline.
Do not put keys/passwords in issues, commits, or public downloads.

**Migration warning:** Existing installs of a debug-signed APK cannot be
updated in place with a different release signing certificate. Plan a
backup/upgrade procedure before switching signing identities; do not
silently uninstall the existing app.

## Google Play Protect

Play Protect may show **"hasn't seen an app from this developer before"**
for newly distributed sideloaded apps. This is a developer/app reputation
notice, not an icon bug, and it cannot be reliably removed by renaming,
repackaging, or self-signing the APK. A correctly signed release is essential
for reliable updates and distribution, but **does not guarantee the notice
will disappear**. Official Play Console distribution and developer
verification are the appropriate routes for building trust. Do not disable
Play Protect or add workarounds to evade its checks.
