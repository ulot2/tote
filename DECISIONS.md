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
