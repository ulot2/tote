# Tote

A personal vault of the links and answers you type into every form: LinkedIn, portfolio, GitHub,
email, CV link, stock answers. Tap to copy now; autofill and a Quick Settings tile are coming.
Android, Kotlin + Jetpack Compose. Fully offline: **no INTERNET permission**.

See [`PLAN.md`](PLAN.md) for the build plan and [`DECISIONS.md`](DECISIONS.md) for deviations.

## Modules
| Module | What |
|---|---|
| `:app` | Compose UI, theme, navigation |
| `:core:model` | Plain Kotlin models, value classification, formatting, validation |
| `:core:data` | Room, DataStore, repositories, clipboard |
| `:core:matching` | Pure Kotlin field-matching engine (Phase 3) |

## Build and test
```sh
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew test               # JVM + Robolectric tests; screenshots land in app/build/screenshots/
```
Requires JDK 17+ and an Android SDK with platform 36 (`sdk.dir` in `local.properties` or `ANDROID_HOME`).

## Status
- [x] Phase 1: foundation and vault
- [ ] Phase 2: Quick Settings tile and picker
- [ ] Phase 3: matching engine
- [ ] Phase 4: autofill service
- [ ] Phase 5: profiles, learning, polish
- [ ] Phase 6: release prep
