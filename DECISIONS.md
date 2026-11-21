# Decisions and deviations

Running log of choices made during the build, and places where the build deviates from `PLAN.md`.

## Phase 1

### Open questions settled (Plan §15)
- **Accent colour: electric lime** (`#C8F23C`). In the light theme lime is only used as a *fill*
  (FAB, primary buttons, selected chips, copy glow) with dark ink on top. Text and icons that need
  the accent on a light surface use a deep olive (`#4A6300`) so they pass WCAG AA.
- **DI: Hilt.**
- **Icon:** placeholder tote-bag mark in lime on warm near-black, with a monochrome layer for themed
  icons. Final icon is Phase 6 work.

### Toolchain
- **AGP 8.13 / Kotlin 2.2 / Gradle 8.14, compileSdk and targetSdk 36.** Newer AGP 9.x and API 37
  exist, but AGP 9 changes the Kotlin/DSL setup substantially. Staying on the mature 8.x line keeps
  the build predictable; moving to targetSdk 37 is a Phase 6 task.
- `:core:model` and `:core:matching` are plain Kotlin/JVM modules (no Android). `:core:data` holds
  Room, DataStore, repositories and the shared `ClipboardWriter` (the tile and picker will reuse it).
  `:feature:autofill` and `:feature:tile` will be added in their phases.
- `:core:matching` currently has only the text normaliser (Plan §7.2) and its tests. The full
  engine is Phase 3.

### Fonts and brand marks
- **Geist** (display and text) and **Geist Mono** (link values), bundled as TTFs under the SIL Open
  Font License (`third_party/geist/OFL.txt`). Bundled rather than downloadable fonts so nothing ever
  needs the network.
- Brand marks (GitHub, X, Behance, Dribbble, Google Drive) are SVG paths from **Simple Icons**
  (CC0, `third_party/simple-icons/LICENSE.md`). LinkedIn is not in Simple Icons and Medium's mark
  doesn't read at badge size, so both are drawn as monograms ("in", "M") on their brand colours.
- Monochrome brands (GitHub, X, Medium) use an inverted tile so they never disappear into the
  background in either theme.

### Data
- Enums are stored by name, not ordinal. `keywords` are newline-joined and `profileIds` are
  comma-joined strings in the entry row; this avoids join tables for small, rarely queried lists.
- The `profiles` table and the entry `profile_ids` column exist from schema v1 so Phase 5 does not
  need a migration. They are not surfaced in the UI yet.
- Changing an entry's category moves it to the end of the new section. Edits never reset
  `useCount`, `lastUsedAt` or `createdAt`.
- Cloud backup excludes the database and DataStore (`data_extraction_rules.xml`,
  `backup_rules.xml`). Device-to-device transfer keeps them.

### UI
- **Reordering uses a dedicated "Reorder" mode** (toolbar button) with drag handles, using
  `sh.calvin.reorderable`. In normal mode, long-press opens the actions sheet (Plan §8.4), so
  long-press can't also start a drag. Entries move only within their own category section.
- **Pinned entries appear twice:** in the pinned row and in their category section. The row is a
  shortcut, and this keeps manual order per category intact.
- **Shared-element transition**: list cards, pinned cards and the FAB each morph into the editor
  (Compose `SharedTransitionLayout` + `sharedBounds`).
- **Copy feedback**: haptic (`CONFIRM` on API 30+, `CLOCK_TICK` before), the card glows lime and
  fades, and a "Copied · Label" pill slides up and fades out. On Android 13+ the system clipboard
  confirmation also appears; per Plan §13 we accept it.
- **Reduced motion**: Compose scales animation durations by the system animator scale, so
  "Remove animations" makes these transitions instant without extra code.
- **Empty vault** reuses the onboarding seed templates (Plan §6.4) as one-tap starters. Full
  onboarding is Phase 5.
- Search (a P2 feature) was cheap to add with the vault list, so it's in already.
- Settings currently has only theme (system/dark/light), the dynamic colour toggle (off by default,
  API 31+) and About. Other sections arrive with their phases.
- The profile switcher pill is deferred to Phase 5, along with profiles.

### Testing without an emulator
- The build container has no KVM, so instrumented tests can't run here. Compose UI tests and Room
  tests run on the JVM under **Robolectric**. `ScreenshotTest` renders the main screens in dark and
  light to `app/build/screenshots/` (Robolectric native graphics) for visual review.
- Release unit tests are disabled for `:app` because the Compose test manifest is debug-only.

## Phase 2

- **Tile module wiring.** `:feature:tile` holds `ToteTileService` but not the picker, which needs the
  design system in `:app`. The tile asks for a `PickerLauncher` (a one-method interface) through
  Hilt, and `:app` binds it to `PickerActivity`. The tile module never references `:app`.
- **The tile is a launcher, not a toggle.** It is always `STATE_ACTIVE` and declares
  `TOGGLEABLE_TILE=false`. Its subtitle says "Copy an entry" until profiles (Phase 5) replace it with
  the active profile's name.
- **Lock screen:** tapping the tile while locked calls `unlockAndRun` first, so the vault never opens
  over the lock screen.
- **Android 14+** launches the picker with the `PendingIntent` form of `startActivityAndCollapse`;
  older versions use the `Intent` form.
- **Picker activity:** translucent, its own task (`taskAffinity=""`), excluded from recents, not
  exported, and it finishes in `onStop`. That keeps it from pulling the vault forward or lingering
  behind other apps. The window has no animation of its own: the sheet springs in and slides out
  in Compose, with drag-to-dismiss on the header.
- **Search isn't auto-focused** in the picker. The common path is tap-and-go, and an open keyboard
  would hide most of the list.
- **After a pick** the row shows a check for ~0.4 s, then the sheet leaves. The haptic tick fires on
  tap. On Android 13+ the system also shows its own clipboard confirmation (accepted, Plan §13).
- **Picker order** (`EntryRanking`, pure Kotlin): pinned, then use count, then last used, then the
  vault's own order. In search, label-prefix matches come first.
- **Add-tile prompt:** Settings has a "Quick Settings tile" card. Android 13+ gets the system
  `requestAddTileService` prompt; older versions get three illustrated manual steps. Onboarding
  (Phase 5) will reuse the same `TilePrompt` helper.
- The "under 3 seconds from shade to paste" acceptance check needs a real phone; it can't be timed
  in this container. Nothing in the path waits on I/O beyond the first Room read.

## Phase 3

- **The dictionary is JSON** (`core/matching/src/main/resources/app/tote/core/matching/dictionary.json`),
  not Kotlin, so the planned Chrome extension can load the same file (Plan §14). It holds kind
  synonyms (strong and weak), generic link words, ignore phrases, and the meanings of Android
  autofill hints and HTML `autocomplete` tokens. It's parsed once, lazily.
- **`:core:matching` depends on `:core:model`**, which is also pure Kotlin, so it can score real
  `Entry` objects. It has no Android dependency.
- **Normalisation** now also folds accents ("résumé" → "resume") and applies a crude plural stem
  ("links" → "link", but never "address" or "status").
- **Entry kind** comes from the brand icon first, then the type (email, phone), then the label
  ("Why this company?" → why_company, "CV (Non-tech)" → cv). A link with a globe icon and no
  recognisable label counts as a portfolio. Every entry also matches on its own label and on its
  user keywords, so entries with no known kind ("Pronouns") still get suggested.
- **Scoring** (constants in `FieldMatcher`): hint/autocomplete 100; best hit per signal tier,
  summed: label/placeholder/aria/hint 60, name/id 45, nearby text 35. Weak phrases score half,
  and hits inside unsplit ids ("linkedinprofileurl") score 80%. Generic link words (or a URL-typed
  field) give every link 32, so "Website" offers Portfolio first and then the other links (S2).
  Learned picks add 200. Threshold 30. Anything under 30% of the top score is dropped. Max 3.
- **Longer phrases swallow shorter ones** of a different meaning: "address" (location, weak) doesn't
  fire inside "email address", nor "contact" (phone, weak) inside "contact email". Generic link
  words never swallow, so "Other website" still counts as "website" for Portfolio.
- **Type fit is a filter, not just a boost**, for email, tel and number inputs: an email field only
  ever offers email entries, and a number field only numeric values. URL fields boost links and
  sink everything else. Text areas favour long answers.
- **Search ignoring is narrow** on purpose. A field counts as a search box only if its type is
  `search`, its name/id is `q`/`search`/`query`, or its label *starts* with "Search". "Job search
  status" and "How did you find us?" stay normal fields. An earlier draft that ignored any field
  containing "find" or "search" would have hidden real questions.
- **`autocomplete="off"` is not an ignore signal.** Job-board forms set it everywhere (Google Forms
  does), and Plan §7.5 only asks us to respect "not important for autofill".
- **Fallback** (nothing passed the threshold): the 3 most-used entries plus "All entries…", only in
  text and URL inputs. Number, date, email-only and phone-only inputs get nothing at all. In a text
  area the fallback puts long answers first; in a URL input, links first.
- **Learning hook**: `MatchContext.learned` and `FieldSignature.key()` are in place now. Storing
  and clearing learned mappings is Phase 5.
- **Fixtures**: 61 field descriptions in `core/matching/src/test/resources/fixtures/job_forms.json`
  (Greenhouse, Lever, Workable, Ashby, Google Forms, Typeform, LinkedIn Easy Apply, native apps,
  plus ambiguous and negative cases). `FixtureTest` reports every failing fixture at once, with
  scores and reasons. Changing the dictionary (removing "summary") makes the runner fail and name
  the fixture.

## Phase 4

- **Module wiring.** `:feature:autofill` holds the service, the structure parser, presentations and
  the response builder. The picker stays in `:app` and both the tile and autofill reach it through
  `PickerIntents` in `:core:data` (this replaces Phase 2's tile-only `PickerLauncher`).
- **Fast path:** `VaultSnapshot` (`:core:data`) keeps the vault and active profile in memory, fed
  from Room on an app-wide scope, so a fill request only waits on disk the very first time after
  the process starts (capped at 2 s). Parsing and matching run off the main thread. Debug builds log
  the response time only; no values or field text are ever logged.
- **Parser** reads hints, HTML attributes (`type`, `name`, `id`, `placeholder`, `aria-label`,
  `autocomplete`, and Chrome's `label`), view hint, id entry, content description, input-type
  flags and the web domain (inherited from the WebView node). The "nearby label" is the last
  visible text before the field in document order. A field's own typed text is never read.
  Checkboxes, radios, buttons, file inputs and hidden or disabled views are skipped. It runs over
  a small `NodeView` interface so the rules are unit-tested without a device.
- **Datasets:** one per suggestion per field, each filling just that field, so the system shows the
  right rows whichever field is focused without a new request. Up to 15 fields per screen.
  Dataset ids are `e:<entryId>` only.
- **Use counting** reads the fill event history at the next request (`TYPE_DATASET_SELECTED`),
  de-duplicated by a response id in the client state. Picks made through "All entries…" are
  counted by the picker. Storing *learned* mappings (Plan §6.3) is Phase 5; the matcher already
  accepts them.
- **No noise in chat and search boxes.** The "most used" fallback is only offered on screens that
  look like forms (two or more fillable fields, or a web page). A lone text box only gets
  suggestions for a real match. Otherwise every messaging app would show a Tote dropdown on its
  compose box.
- **Inline suggestions** (Android 11+) go on the focused field only, within the keyboard's
  `maxSuggestionCount`, keeping one slot for "All entries…". They're skipped if the keyboard doesn't
  support the v1 inline UI; the dropdown always works.
- **Dropdown rows** are RemoteViews with system fonts (custom fonts can't cross into other apps),
  Tote's palette in light and night variants, and badges drawn to bitmaps by `IconRenderer` with
  the same colours as the app (`EntryIconPalette` in `:core:model`, now shared by both).
- **Icons:** generic glyphs are Material Symbols Rounded (Apache-2.0,
  `third_party/material-symbols/LICENSE`) and brand marks are Simple Icons (CC0), as vector
  drawables.
- **"All entries…"** uses dataset authentication with a mutable `PendingIntent` (the system adds
  the screen structure to it). It opens `AutofillPickerActivity`, a standard-launch-mode subclass
  of the picker: the single-task, own-task picker used by the tile can't return a result to the app
  being filled. The picker then answers with a one-field dataset.
- **Deprecated `Dataset.Builder` APIs** (pre-API 33) are used on every version. They still work
  and cover API 26+ with one code path; moving to `Presentations`/`Field` is a later clean-up.
- **No browser compatibility mode.** Chrome now hands forms to autofill services itself once
  "Autofill using another service" is on, and declaring compat packages could degrade browsers
  that already support autofill natively. To revisit after testing Firefox, Samsung Internet and
  others on a device.
- **Tote ignores its own screens**, except the autofill test screen.
- **Setup UI:** Settings has an Autofill card (on/off status shown with an icon and a word, not just
  colour; "Turn on" opens the system prompt; the one-service warning), Chrome steps with an "Open
  Chrome" button (Chrome has no public intent for its autofill settings page), and "Try it". The
  vault shows a dismissible banner while autofill is off. Full onboarding is Phase 5.
- **Try autofill screen** (`AutofillPlaygroundActivity`, in release builds too): native fields
  labelled only by nearby text, plus a local Greenhouse/Lever-style job form in a WebView
  (`assets/playground/job_form.html`, no network). It covers Plan §11's test activity and lets S1/S2
  be checked on a phone without a real job site.
- **Not verifiable here:** the actual autofill flow needs a device or emulator (no KVM in the build
  container). What's covered on the JVM: parsing, planning, dataset building, presentations
  (rendered to PNG), the fill result, and the manifest wiring.

## Fix after 0.2.0: autofill returned nothing in release builds

- **Cause:** the keyword dictionary was loaded with a path relative to `KeywordDictionary`'s
  package. R8 renames that package in release builds (`app.tote.core.matching` → `m5`), so the
  lookup missed the file and every fill request failed and returned nothing. Debug builds and the
  unit tests weren't minified, so they passed.
- **Fix:** an absolute resource path (`KeywordDictionary.RESOURCE_PATH`) plus a test that pins it.
  Checked on *minified* code: R8 run over the matching engine with a small driver program. The
  old code fails with "dictionary.json missing" and the fixed code returns LinkedIn for "LinkedIn
  Profile".
- **Visibility:** the service now records what its last request did (counts, timing, or the error's
  class name, never values or field text) and the Try autofill screen shows it. A silent failure
  like this one is now visible on the phone.
