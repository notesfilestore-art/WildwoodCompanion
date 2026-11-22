# 🍄 Wildwood Companion

An unofficial, open-source, pixel-cottagecore companion app for **Cattails: Wildwood Story**, designed around the **AYN Thor bottom screen**.

## Hardware sizing
- Native portrait panel: **1080 x 1240 px** (width:height **27:31**).
- Typical dual-screen landscape orientation: **1240 x 1080 px** (width:height **31:27**).
- UI is responsive; Android status/navigation bars reduce available viewport.
- Does **not** need privileged permissions, root, or the Play Store.
- The user launches it on the bottom display using the Thor's dual-screen interface. This app does not forcibly move itself to another display.

## What's implemented in v0.1 prototype
- Pixel-inspired custom CSS design, responsive bottom-screen UI and six functional navigation sections.
- Offline *index* of documented wiki topics, cat names and item categories, with an integrated wiki reader (articles require internet).
- Journal with add/edit/delete and in-game date labels, stored locally.
- Tasks with add/check/delete, stored locally.
- Simple map planner (personal locations), stored locally; official interactive game maps not yet included.
- JSON data backup/export and restore.
- No trackers, cloud accounts or ads in the companion UI. The embedded third-party Fandom website may display its own advertisements and tracking; see its policies. Internet permission is used only for online wiki pages and any third-party content those pages load.

**Not yet implemented:** full verified wiki-derived guides, gift preference datasets, character events, official game maps, import/update pipeline, multiple save profiles. The wiki index is not a complete game guide. Do not treat missing data as factual claims.

## Embedded wiki reader
- Wiki articles from `cattails-game.fandom.com` open in a separate WebView inside the app, with Back, Refresh, and Close controls.
- Other website links open in the device browser. The bundled guide index and local notes remain usable offline.
- Fandom may use redirects, authentication challenges, third-party ads or WebView restrictions, so wiki compatibility must be verified on a real Thor; the reader cannot guarantee every page will load.

## Build APK
Install Android Studio (JDK 17 + Android SDK 35) and open this folder as a Gradle project. Build → Build APK(s). Or upload to GitHub and run the **Build sideload APK** workflow, then download the `WildwoodCompanion-debug-apk` artifact. A published GitHub Release also attaches the debug APK.

The automated build is **not signed with a stable production signing key**: debug signing is suitable for testing but not for an update-stable public release. Before distributing public releases, configure a persistent, privately secured signing key, then use that consistently for every release. Test installation and update behavior on a physical Thor.

## Environment check
This source archive was updated without an Android SDK or Gradle installation in the execution environment; a verified compiled APK is not included. Run the GitHub Actions workflow to build a test APK, and test it on an AYN Thor.

## Data and licensing
- Application code/artwork: MIT licensed (see LICENSE).
- No Fandom wiki article prose or third-party art is bundled in v0.1.
- Names, topic labels and links are an attribution-bearing index pointing to https://cattails-game.fandom.com/wiki/Cattails:_Wildwood_Story .
- Future copied/adapted wiki material needs attribution and compliance with the relevant CC BY-SA licensing conditions; game screenshots/images may have separate rights.
- Unofficial project, not affiliated with Falcon Development or the Cattails Wiki.

## Backup
Use Settings → Export backup before uninstalling or clearing app data. Restore on the same app install or a new one. `localStorage` is tied to the installed Android app's data and may be lost on uninstall.
