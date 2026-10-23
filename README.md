# Gym Stuff

A simple native Android workout tracker for your Pixel. Weights are in kilograms. No account, server, ads, or internet permission.

## Try the local browser preview

Open `preview/index.html` in Chrome by double-clicking it in your file manager. No installation or build is needed. This is a browser version for trying the screens and logging flow; it is not the Android APK. Its data is stored separately in this browser. Keep using the same browser and file location to retain your preview data, or export a backup from History.

Alternatively, run this from the project folder in your terminal:

```sh
python3 -m http.server 8765 --bind 127.0.0.1 --directory preview
```

Then open `http://localhost:8765`. Stop the server with Ctrl+C. The server could not be started inside the restricted agent environment, but the HTML preview also works without a server. File and localhost previews use separate browser storage.

Preview logic checks: `node tests/preview.test.cjs`. Browser rendering has not been verified in this environment.

## Using the app

1. Your six-day plan is preloaded: Monday **Push A**, Tuesday **Pull A + abs**, Wednesday **Legs A**, Thursday **Push B + abs**, Friday **Pull B**, Saturday **Legs B + abs**, and Sunday rest. Open **Routines** to see each exercise's set count and target rep range, or add your own exercises.
2. Open **Train** and start a routine. Enter weight and reps for each completed set. Add or remove sets as needed; use 0 kg for bodyweight exercises.
3. See the last workout for the same routine above each exercise. Turn on **Compare with previous week** for the most recent matching workout from the previous Monday–Sunday week. If you use different A/B workouts, create separate routines for them.
4. Tap **Finish & save workout**. Completely blank sets are skipped; partially entered sets must be completed or removed.
5. Open **History** for all saved sets, grouped by week. The Train screen counts workouts completed this week.

An unfinished workout saves as you type and resumes after reopening. Routine changes apply to the next session. Renaming an exercise preserves its comparison history. Removing and re-adding it creates a new exercise.

Starting a planned routine creates its prescribed number of set rows (including two-set accessories and four-set calf exercises). Target reps are displayed separately; actual reps and weights stay blank until you log them. The abs exercises use the final abs guidance you supplied: crunches 3 × 8–15, raises 3 × 10–15, and Thursday rotations 2 × 10–15 each side. Exercise alternatives separated by a slash mean choose one option.

Existing installations receive this plan once on reopening. Completely unused original Push/Pull/Legs placeholders are replaced. Custom routines, history and unfinished sessions are preserved; the six routines are appended when existing data is in use. Plan IDs and a migration marker prevent duplicates on reload or restore. This implements the supplied plan; it does not independently verify the gym's equipment.

The plan's source is `app/src/main/assets/ppl-plan.json`. After editing it, run `node scripts/sync-plan.cjs` to update the browser copy. Check synchronization with `node scripts/sync-plan.cjs --check`.

Data stays in this app's private phone storage. **History → Export backup** saves a JSON file through Android's file picker, including your routines, history, and draft. **Restore backup** replaces current data. Export before uninstalling or switching phones. There is no cloud sync.

## Generate the APK with GitHub Actions

The APK has **not** been built in this workspace: Android SDK/build tools are unavailable and network downloads are blocked.

1. Put this project in a GitHub repository (include `.github/workflows/android.yml`).
2. Open the repository's **Actions** tab → **Build Android APK** → **Run workflow**. It also builds on pushes to `main` or `master`.
3. After a successful run, download the **Gym-Stuff-APK** artifact and unzip it.
4. Transfer `app-debug.apk` to your Pixel, open it, and allow installation from that source when Android asks.

This is a debug APK for personal use, not a Play Store release. Separate CI runs can use different debug signing keys: if an update is rejected, export a backup before uninstalling the old version, then install and restore. A production release should use a persistent private signing key.

## Build locally

Install JDK 17, Android SDK platform 35 and Gradle 8.11.1. Set `ANDROID_HOME` to your SDK directory, or set `sdk.dir` in an untracked `local.properties` file. Then run:

```sh
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The project uses Android Gradle Plugin 8.9.2 and native Android views, with no third-party runtime dependencies. Android 8.0 or later is required. No Gradle wrapper binary is bundled; CI installs the pinned Gradle version.

## Verification

Run the date-boundary tests with a JDK:

```sh
mkdir -p /tmp/gym-stuff-tests
javac -d /tmp/gym-stuff-tests app/src/main/java/com/gymstuff/app/WorkoutDates.java tests/WorkoutDatesTest.java
java -cp /tmp/gym-stuff-tests com.gymstuff.app.WorkoutDatesTest
```

Before relying on the app, verify on an Android device: create exercises; log and finish a session; start another matching routine and check comparison; close/reopen during a draft; export and restore; check keyboard and navigation-bar spacing. Android compilation and device testing remain unverified in this workspace.
