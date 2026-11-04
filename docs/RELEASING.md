# 📦 Releasing OSMTracker

Steps to follow when releasing a new version of OSMTracker for Android. This document is for maintainers with write access to the repository.

There are three release tracks:

- **Nightly**: automatic, produced by the [nightly workflow](.github/workflows/nightly.yml). Telegram notification is pending (see [issue #672](https://github.com/labexp/osmtracker-android/issues/672))
- **Testing**: a release candidate uploaded to the *Internal testing* track on Google Play.
- **Stable**: a validated release promoted to *Production* on Google Play.


## 1. Prepare the stable release

Open a Pull Request from `develop` to `master`.

Title: `Preparing version <YYYY.MM>`. See [PR #682](https://github.com/labexp/osmtracker-android/pull/682) for an example. 

In the PR description, write the changelog for this release. Gather it from the commits on develop since the last release. The [Changelog wiki page](https://github.com/labexp/osmtracker-android/wiki/Changelog) documents releases up to 2024.07.09; newer releases are documented in the [GitHub releases page](https://github.com/labexp/osmtracker-android/releases). 

During release preparation, it is common to include some pull requests that were almost ready. These are accepted as long as they do not represent critical changes that would require additional review or testing.

Once the release branch is frozen (that is, no more code changes are accepted and the branch becomes the release candidate), update the ["What's new" slide in the app intro](https://github.com/labexp/osmtracker-android/blob/master/app/src/main/java/net/osmtracker/activity/Intro.kt) with a summary of the changelog. 

Keep the changelog short: a few bullet points describing user-facing changes. The full detail is in the code and the commit history. Note that the changelog will keep receiving translations after the branch is frozen.

Increase `versionCode` in [app/build.gradle](app/build.gradle) to a value higher than the last release published on Google Play ([`versionName` is generated automatically](https://github.com/labexp/osmtracker-android/commit/cc8b65211ca9691e7da701be75288907aca11209) from the current date in format `yyyy.MM.dd`).


## 2. Build the release

Generate the signed Android App Bundle (AAB) from Android Studio: *Build > Generate Signed Bundle / APK > Android App Bundle*. 

The AAB will be at `app/build/outputs/bundle/release/app-release.aab`.

The signing keystore and credentials are **not** stored in the repository. The maintainer running the build loads them into Android Studio when prompted. Ask an existing maintainer for access to the keystore.


## 3. Publish on Google Play

- Open the [Google Play Console](https://play.google.com/console) and select OSMTracker.
- Upload the AAB to the **Internal testing** track first.
- Verify the app installs and the critical features work: track recording, upload to OSM, GPX export.
- Promote the release to **Production**. Wait at least one week after publishing the testing version.


## 4. Tag the release in Git

- Open the [releases page](https://github.com/labexp/osmtracker-android/releases) on GitHub.
- Click **Draft a new release**.
- In **Choose a tag**, type the version (`yyyy.MM.dd`, e.g. `2026.02`) and select **Create new tag**.
- Set the target branch to `master`.
- Set the release title to `Release <version>` (e.g. `Release 2026.02`).
- Write the release notes (same content as the PR changelog).
- Publish the release.


## 5. Post-release

- Announce the release in the [OSMTracker Telegram group](https://t.me/OSMTracker). This step is currently manual (could be automated from the release workflow in the future 🙏).  
- Verify the release is picked up by F-Droid and builds successfully. F-Droid builds from the source code, so the tag and the source tree need to be consistent.
- Close any issues that this release resolves.



## Note on iteration

The release process is iterative. It is common to publish a version to the *Internal testing* track (see step 3), find an issue, and go back to step 1 to prepare a new version with an increased `versionCode` before promoting to *Production*. This loop may run several times.
