# F-Droid publishing guide for Quick Search

This project ships two **distribution flavors** from one codebase:

| Flavor | Gradle task | Use |
|--------|-------------|-----|
| `standard` (default) | `assembleStandardRelease` | Google Play, GitHub releases |
| `fdroid` | `assembleFdroidRelease` | [F-Droid](https://f-droid.org/) |

The `standard` flavor keeps Google Play in-app review and in-app updates. The `fdroid` flavor uses the same app with no-op stubs, does not bundle Play Core libraries, uses the system font, and defaults web suggestions to off.

## Build locally

```bash
./gradlew assembleFdroidRelease
```

APK output: `app/build/outputs/apk/fdroid/release/`

```bash
./gradlew assembleStandardRelease
```

## Upstream metadata (in this repo)

F-Droid reads listing text and graphics from:

```
fastlane/metadata/android/en-US/
```

When releasing, update `versionCode` / `versionName` in `app/build.gradle.kts`, add `changelogs/<versionCode>.txt`, and tag the release commit. If you keep the current F-Droid-specific tagging scheme, use tags like `3.7-fdroid`.

## Submit to F-Droid

1. **Compliance** — MIT license, public source, FOSS dependencies in the `fdroid` variant. See the [inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/).

2. **Request for Packaging** — Open an issue: https://gitlab.com/fdroid/rfp/-/issues/new  
   Include app id `com.tk.quicksearch`, source URL, license, and confirm you approve inclusion.

3. **fdroiddata metadata** — Fork https://gitlab.com/fdroid/fdroiddata and add `metadata/com.tk.quicksearch.yml`. A starter file is in [docs/fdroiddata-example.yml](fdroiddata-example.yml). The important build block:

   ```yaml
   Builds:
     - versionName: '3.6'
       versionCode: 65
       commit: <full-git-sha-for-that-release>
       gradle:
         - fdroid
   ```

4. **Anti-features** (expected) — declare `NonFreeNet` for the optional proprietary network services this app can use (AI providers and user-enabled web suggestions/search integrations). F-Droid maintainers may request additional labels for optional proprietary app/service integrations depending on their review.

5. **Test** — Use [fdroidserver](https://f-droid.org/en/docs/Installing_the_Server_and_Repo_Tools) or fdroiddata CI: `fdroid lint com.tk.quicksearch`, `fdroid build com.tk.quicksearch`.

6. **Merge request** — Submit to fdroiddata; after merge, the app is built on F-Droid’s infrastructure (typically 24–48 hours to appear).

## Release checklist

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
- [ ] Tag the F-Droid release commit (current scheme: `<versionName>-fdroid`)
- [ ] `./gradlew assembleFdroidRelease` succeeds
- [ ] `./gradlew assembleStandardRelease` succeeds
- [ ] Update fdroiddata `Builds` entry (or rely on auto-update after first inclusion)

## Reproducible builds

Optional but recommended. F-Droid can verify that their APK matches yours when you publish signed release binaries and enable reproducible build metadata in fdroiddata. See https://f-droid.org/en/docs/Reproducible_Builds/
