# 🍄 Wildwood Companion

An unofficial, open-source, pixel-cottagecore companion app for **Cattails: Wildwood Story**, designed around the **AYN Thor bottom screen**.

## Hardware sizing
- Native portrait panel: **1080 x 1240 px** (width:height **27:31**).
- Typical dual-screen landscape orientation: **1240 x 1080 px** (width:height **31:27**).
- UI is responsive; Android status/navigation bars reduce available viewport.
- Does **not** need privileged permissions, root, or the Play Store.
- The user launches it on the bottom display using the Thor's dual-screen interface. This app does not forcibly move itself to another display.

## Earlier v0.2 foundation
- Pixel-inspired custom CSS design, responsive bottom-screen UI and seven functional navigation sections.
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
- No Fandom wiki article prose or third-party art is bundled with the APK.
- Names, topic labels and links are an attribution-bearing index pointing to https://cattails-game.fandom.com/wiki/Cattails:_Wildwood_Story .
- Future copied/adapted wiki material needs attribution and compliance with the relevant CC BY-SA licensing conditions; game screenshots/images may have separate rights.
- Unofficial project, not affiliated with Falcon Development or the Cattails Wiki.

## Backup
Use Settings → Export backup before uninstalling or clearing app data. Restore on the same app install or a new one. `localStorage` is tied to the installed Android app's data and may be lost on uninstall.

## v0.3: Downloadable offline encyclopedia (beta)
- **Offline tab**: "Download / resume library" crawls the `Wildwood Story` category and nested subcategories through Fandom's MediaWiki `api.php`, downloads article HTML, and stores searchable article text in private Android storage. Files already present are skipped when resuming.
- **Search** looks through the actual text of downloaded articles, not just their titles. Use the search field in the Offline tab; an empty search displays locally available pages.
- **Offline reader** shows cached article HTML in a sandboxed frame with network resource access prohibited. Up to two small, successfully fetched article images are converted to embedded data URLs. Many illustrations, interactive features, complex maps, and large images will not be present offline.
- **Source attribution** for articles appears in the reader, with a copy-source option. Cached pages are downloaded by the user at runtime; wiki content and third-party images are *not* included in the distributed source or APK.
- **Network needed to set up library**: Fandom may rate-limit or refuse API requests. Failed pages aren't silently marked downloaded, and the app attempts to resume if run again. No guarantee that every wiki page has been categorized correctly.
- **Existing notes preserved**: app package ID and the original localStorage key are unchanged. Updating *over* the existing installation should keep notes, tasks, and map pins, provided the APK uses the same signing certificate. Back up your journal first.
- **No compiled APK** included; upload the ZIP contents to the repository root and run GitHub Actions again.

### Updating the GitHub repository from v0.2
On a PC, the easiest way to replace the existing source tree is GitHub Desktop (clone the repo, copy v0.3 files into the local folder, commit, push). Using the browser uploader can also replace files, but ensure `.github/workflows/build.yml` retains `android-actions/setup-android@v4`. Each push to main triggers a build.

### Scope & licensing
Fandom wiki prose is generally CC BY-SA 3.0; inspect the individual wiki and media licenses. For public redistribution of derived article text, include attribution and the applicable ShareAlike terms. Downloaded pictures might be copyrighted game artwork; *do not package cached pictures into a public APK without verifying their individual redistribution rights*. This beta's runtime personal offline cache and its ability to request images do not establish redistribution permission. This is not an official game guide or a complete verified dataset.
