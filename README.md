# Tote

A personal vault of the links and answers you type into every form: LinkedIn, portfolio, GitHub,
email, CV link, stock answers. Tote suggests them when you tap a form field (autofill), or you copy them from the app or a Quick Settings tile.
Android, Kotlin + Jetpack Compose. Fully offline: **no INTERNET permission**.

See [`PLAN.md`](PLAN.md) for the build plan and [`DECISIONS.md`](DECISIONS.md) for deviations.

## Modules
| Module | What |
|---|---|
| `:app` | Compose UI, theme, navigation |
| `:core:model` | Plain Kotlin models, value classification, formatting, validation |
| `:core:data` | Room, DataStore, repositories, clipboard |
| `:core:matching` | Pure Kotlin field-matching engine: shared JSON keyword dictionary, scoring, ignore rules, 61 job-form fixtures |
| `:feature:tile` | Quick Settings tile that opens the picker sheet |
| `:feature:autofill` | Autofill service: structure parsing, dataset building, dropdown and inline presentations |

## Build

GitHub Actions runs the unit tests and builds `Tote.apk` on every push to `main` or `dev` and on every pull
request (see `.github/workflows/build.yml`). The APK is attached to the run page. Pushing a tag like `v0.1.0`
publishes a release with `Tote.apk`.

To build on a computer with the Android SDK: `./gradlew test assembleDebug`. The wrapper fetches the right
Gradle. JVM tests include Robolectric screenshots of the main screens in `app/build/screenshots/`.

The release APK is signed with a key kept in two repository secrets:

| Secret | What it holds |
|---|---|
| `TOTE_KEY` | The PKCS12 key file, base64-encoded |
| `TOTE_KEY_PASSWORD` | Its password |

Without them, branch builds are signed with a throwaway key and tag builds refuse to publish. Keep the same
key forever: Android only installs an update that is signed with the same key as the installed app.

To make the key once (keep `tote.p12` and the password somewhere safe):
```sh
keytool -genkeypair -keystore tote.p12 -storetype pkcs12 -alias tote -keyalg RSA -keysize 4096 \
  -validity 10000 -dname "CN=Tote"
base64 -w0 tote.p12 > tote.p12.b64   # paste this into TOTE_KEY
```

## Status
- [x] Phase 1: foundation and vault
- [x] Phase 2: Quick Settings tile and picker
- [x] Phase 3: matching engine
- [x] Phase 4: autofill service
- [ ] Phase 5: profiles, learning, polish
- [ ] Phase 6: release prep
