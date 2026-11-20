# Tote: Build Plan

> **Name:** Tote
> **Platform:** Android first (Chrome extension later)
> **Stack:** Kotlin + Jetpack Compose
> **Status:** Planning. This document is the single source of truth for the first build.

---

## 0. How to use this plan (for the Claude Code session)

- This repo contains only this plan. Scaffold the Android project in the repo root.
- Work **phase by phase** (Section 12). Finish each phase's acceptance criteria before starting the next. Commit at the end of each phase with a clear message.
- **UI quality is a first-class requirement.** Do not ship stock Material 3 screens. Follow the design system in Section 8.
- The field-matching logic (Section 7) must be **pure Kotlin with no Android dependencies**, so it can be unit-tested on the JVM.
- The app must **never request the INTERNET permission**. Everything stays on the device.
- When something in this plan turns out to be wrong or impossible on current Android, note the deviation in a `DECISIONS.md` file and continue.

---

## 1. The problem

Filling job applications and other forms means constantly hunting for the same things: LinkedIn URL, portfolio link, GitHub, email, phone number, CV link, and short stock answers. Each time it means leaving the form, finding the link, copying it, and coming back. It's slow and annoying.

## 2. The solution

An Android app that holds a personal **vault** of reusable values and puts them into any form on the phone in one or two taps, through three surfaces:

1. **Autofill service** (main product). When a form field is focused in any app or browser, Tote suggests the right value ("LinkedIn URL" field → LinkedIn link) as a dropdown or as inline chips above the keyboard.
2. **Quick Settings tile** (instant fallback). Pull down the notification shade, tap the tile, pick an entry from a bottom sheet, and it's copied to the clipboard.
3. **The app itself.** A clean, beautiful vault editor for managing entries.

## 3. Goals and non-goals

### Goals (v1)
- Store, organise, edit and reorder vault entries.
- Copy any entry from anywhere in two taps (tile).
- Smart autofill suggestions in native apps and in browser web forms.
- A "show all entries" fallback inside autofill when matching fails.
- A polished, distinctive UI with good motion and haptics.
- Fully offline. No accounts, no network, no analytics.

### Non-goals (v1)
- Passwords or payment cards (this is not a password manager).
- Cloud sync between devices.
- iOS.
- AI or LLM features.
- The Chrome extension (planned for later, Section 14).

---

## 4. Users and core scenarios

**Primary user:** a developer applying to jobs, filling forms on phone and laptop.

| # | Scenario | Surface |
|---|----------|---------|
| S1 | Job form in Chrome has a field "LinkedIn Profile". User taps it and sees "LinkedIn · linkedin.com/in/…" as a suggestion. One tap fills it. | Autofill |
| S2 | Field label is vague ("Website"). User sees the top 2–3 likely matches plus "All entries…". | Autofill |
| S3 | An app doesn't play well with autofill. User pulls down the shade, taps the Tote tile, picks "Portfolio", it's copied, pastes it. | Tile |
| S4 | User adds a new entry "CV (Non-tech)" with a Google Drive link and puts it in the "Non-tech" profile. | App |
| S5 | User switches active profile from "Tech" to "Non-tech" so the CV and bio suggestions change. | App / Tile |
| S6 | User picks "Portfolio" for a field on a specific site. Next time, that field on that site suggests Portfolio first. | Autofill (learning) |

---

## 5. Features by priority

### P0: MVP
- Vault: create, edit, delete, reorder entries.
- Entry types: Link, Email, Phone, Short text, Long text (answers or bios).
- Categories (fixed): Links, Contact, Documents, Answers.
- Tap an entry in the app to copy it, with haptic feedback and a confirmation animation.
- Quick Settings tile that opens a bottom-sheet picker and copies the chosen entry.
- First-run onboarding with seed suggestions (Section 6.4).

### P1: Autofill
- Autofill service with field matching (Section 7).
- Dropdown presentations (all supported versions) and inline suggestions (Android 11+).
- "All entries…" dataset that opens a picker and fills the chosen value.
- Onboarding step that guides the user to set Tote as the autofill service and to enable third-party autofill in Chrome.
- Per-entry custom keywords (user-added match words).

### P2: Polish and smarts
- Profiles (e.g., Tech / Non-tech). Entries can belong to "All" or specific profiles. One active profile at a time.
- Local learning: remember chosen entries per (website or app + field signature).
- Search in the vault and in the picker.
- Pinned or favourite entries shown first in the tile picker.
- Usage counts to sort the picker by most used.
- Home-screen widget (optional, if time allows).
- Export or import the vault as a JSON file (manual backup, user-initiated).

### P3: Later
- Chrome extension (desktop) sharing the same data model.
- Optional biometric lock for the app.
- Application tracker ("forms I filled today").

---

## 6. Data model (conceptual)

### 6.1 Entry
| Field | Description |
|-------|-------------|
| id | Unique id |
| label | Display name, e.g., "LinkedIn" |
| value | The text to insert |
| type | Link / Email / Phone / ShortText / LongText |
| category | Links / Contact / Documents / Answers |
| icon | Auto-detected brand icon (LinkedIn, GitHub, X, Behance, Dribbble, Medium, Google Drive, generic globe, mail, phone, doc, text) with manual override |
| keywords | Extra user-defined match words |
| profiles | "All" or a set of profile ids |
| pinned | Boolean |
| sortOrder | For manual reordering |
| useCount, lastUsedAt | For ranking |
| createdAt, updatedAt | Timestamps |

### 6.2 Profile
id, name, accent colour (optional), sortOrder. Exactly one is **active**. "All" is implicit.

### 6.3 Learned mapping (P2)
Key: source (web domain or app package) + field signature (normalised hint, id, name and label). Value: entry id, plus a hit count and lastUsedAt. Stored locally and clearable from Settings.

### 6.4 Seed suggestions (onboarding)
Offer one-tap templates the user fills in: LinkedIn, Portfolio, GitHub, Email, Phone, CV link, Location (city, country), Years of experience, Short bio, "Why do you want to work here?" draft. Pre-filling actual values is optional. The owner's own values for testing are:
- LinkedIn: `linkedin.com/in/toluwalope-adegoke`
- Portfolio: `https://www.tnuell.site`
- GitHub: `github.com/ulot2`
- Email: `tolu.nuell@gmail.com`

(Do not hardcode these into the shipped app. Use them only as manual test data.)

### 6.5 Value normalisation
- Links: store as entered. On autofill into a URL-type field, add `https://` if no scheme is present. Show without the scheme in the UI.
- Phone: store as entered and do not reformat.

---

## 7. Field matching engine (the core logic)

A pure-Kotlin module. Input: a description of a form field. Output: ranked entry suggestions.

### 7.1 Signals extracted from each field (by the autofill service)
In priority order:
1. Android `autofillHints` (e.g., emailAddress, phone, url)
2. HTML attributes from web views: `autocomplete`, `type`, `name`, `id`, `placeholder`, `aria-label`, `label`
3. View hint text, view id resource name, content description
4. Nearby label text (the closest preceding text view in the same parent or container)
5. Input type flags (email, phone, URI)
6. Source: web domain or app package name

### 7.2 Normalisation
- Lowercase, split camelCase and snake_case, strip punctuation, collapse whitespace.
- Remove filler words ("your", "please", "enter", "the", "profile", "url", "link", "address" are low-weight, not removed. Tune with tests.)

### 7.3 Built-in keyword dictionary
Map each well-known entry kind to synonyms. Examples:
- LinkedIn → linkedin, linked in, li profile
- GitHub → github, git hub, code repository, gitlab (weak)
- Portfolio → portfolio, website, personal site, personal website, homepage, web site, work samples
- Email → email, e-mail, mail
- Phone → phone, mobile, telephone, tel, whatsapp, contact number
- CV → cv, resume, résumé, curriculum vitae
- Twitter/X → twitter, x handle, x profile
- Location → city, location, where are you based, address (weak)
- Bio → about you, about yourself, summary, bio, introduce yourself
- Years of experience → years of experience, yoe, experience (years)

Entry kind is inferred from the entry's icon or brand, type and label. User-defined keywords are added on top.

### 7.4 Scoring
- Exact autofill-hint or HTML-type match (email or tel type → Email or Phone entries): very high score.
- Keyword match in label, placeholder or aria-label: high. In name or id: medium. In nearby text: medium-low.
- Type compatibility: a URL-type field boosts Link entries. An email field excludes non-email entries.
- Learned mapping for the same source + signature: top boost.
- Active profile filter applied first. Pinned and use count only break ties.
- Return the top 3 above a confidence threshold, then always append "All entries…".
- If nothing passes the threshold, show the top 3 most-used plus "All entries…" only when the field looks like free text (avoid noise on password, OTP, number and date fields).

### 7.5 Fields to ignore
Passwords, OTP or verification codes, credit card fields, search bars, and anything marked as not important for autofill. Never offer suggestions there.

### 7.6 Tests (required)
A fixture-driven JVM test suite with at least 40 realistic field descriptions taken from common job-application platforms (Greenhouse, Lever, Workable, Ashby, Google Forms, Typeform, LinkedIn Easy Apply-style forms). Each fixture asserts the expected top suggestion. Include ambiguous cases ("Website", "Other link", "Links") and negative cases (password, OTP).

---

## 8. Design system and UI (high priority)

The UI must feel premium and distinctive, not like a default Material app.

### 8.1 Direction
**"Calm, tactile, confident."** Dark-first, with a crafted light theme. Big readable labels, generous spacing, soft depth, delightful but quick motion. Think of a well-made wallet or keychain: everything you need, beautifully organised.

### 8.2 Tokens
- **Typography:** one characterful display face for headings (e.g., Satoshi, General Sans, or Geist) and a highly legible text face for values (Inter or Geist). Use a monospace face (Geist Mono or JetBrains Mono) for link values. Define a clear type scale: display, title, body, label, mono.
- **Colour:** custom scheme, not Material defaults.
  - Dark: near-black warm background, slightly lifted surfaces, one vivid accent (suggestion: electric lime or warm coral, but pick one and commit), muted secondary text.
  - Light: warm off-white background, the same accent tuned for contrast.
  - Brand icons may use their own colours inside small rounded containers.
  - Dynamic colour (Material You): optional toggle in Settings, **off by default**.
- **Shape:** large radii on cards (20–24dp) and sheets, pill-shaped buttons and chips.
- **Elevation:** mostly flat with subtle tonal layering. Avoid heavy shadows.
- **Spacing:** 4dp base grid. Screen padding of 20dp.
- All contrast must meet WCAG AA.

### 8.3 Motion and feedback
- Shared-element transition from an entry card to its edit screen.
- Spring-based list reordering with drag handles.
- Copy feedback: light haptic tick, the card briefly glows in the accent colour, and a small "Copied" pill slides up and fades out.
- Bottom sheets use spring physics. Respect the system "remove animations" setting.

### 8.4 Screens
1. **Onboarding (3–4 steps):** what it does → add your first entries (seed templates as tappable cards) → enable autofill (deep link to the system setting, with a short visual explaining it) → add the Quick Settings tile (use the system "add tile" prompt on Android 13+) → done.
2. **Vault (home):** large title, profile switcher pill at the top, search, then sections by category. Pinned entries appear as a horizontal row of bigger cards at the top. Each card shows the icon, label and truncated value (mono). Tap to copy, long-press for actions (edit, pin, share, delete). The FAB adds an entry.
3. **Entry editor:** label, value (with paste-from-clipboard helper), type and category (auto-suggested from the value, e.g., a linkedin.com URL → LinkedIn icon + Links), icon picker, profiles, custom keywords as chips, and a live preview of how the suggestion will look in autofill.
4. **Picker sheet** (used by the tile and by "All entries…" in autofill): translucent, sheet-style activity with search at the top, pinned entries first, then by most used. Tap = copy (tile) or fill (autofill).
5. **Settings:** autofill status (on or off, with a fix button), Chrome setup help, active profile, manage profiles, learned mappings (view and clear), theme (system, dark, light), dynamic colour toggle, export or import, about.
6. **Empty states:** illustrated and friendly, with one clear action.

### 8.5 Autofill presentations
- Dropdown rows: brand icon + label + muted truncated value, styled to match the app.
- Inline suggestion chips (Android 11+): icon + label, built with the inline suggestions UI library and styled within platform limits.
- The final item is always "All entries…" with the app's icon.

### 8.6 App identity
A simple, bold app icon that reads well at tile size (e.g., a stylised key, bookmark or bolt shape in the accent colour). Adaptive and themed (monochrome) icon variants. A matching Quick Settings tile icon.

### 8.7 Accessibility
Content descriptions on all icons, TalkBack-friendly actions (copy, edit), 48dp minimum touch targets, support for large font scales without clipping, and no information carried by colour alone.

---

## 9. Technical architecture

### 9.1 Stack
- Kotlin, Jetpack Compose, Material 3 (heavily themed)
- Navigation Compose
- Room for entries, profiles and learned mappings
- DataStore (Preferences) for settings (theme, active profile, onboarding done)
- Hilt for dependency injection (or Koin if simpler, but pick one and stay consistent)
- Coroutines + Flow
- AndroidX Autofill + `androidx.autofill:autofill` inline suggestions library
- Kotlinx Serialization for export and import
- Testing: JUnit 5 or JUnit 4, Turbine for Flows, Compose UI tests, Robolectric where needed

### 9.2 SDK levels
- minSdk **26** (Android 8.0, where the Autofill Framework starts)
- Inline suggestions require API 30+. Feature-detect and fall back to the dropdown.
- targetSdk: latest stable

### 9.3 Modules
- `:app` contains Compose UI, navigation, theme and the picker activity
- `:core:model` contains plain Kotlin data classes
- `:core:data` contains Room, DataStore and repositories
- `:core:matching` is the **pure Kotlin/JVM** matching engine (no Android imports)
- `:feature:autofill` contains the AutofillService, structure parsing and dataset building
- `:feature:tile` contains the TileService

(A single-module start is acceptable in Phase 1, but `:core:matching` must be separate from the beginning.)

### 9.4 Autofill service flow
1. System calls the fill request with the screen's view structure.
2. Traverse all nodes and collect fillable text fields with their signals (Section 7.1). Skip ignored fields (7.5).
3. Determine the source (web domain from the node, or app package).
4. For the focused field (and any other recognised fields on the screen), run the matching engine.
5. Build datasets: one per suggestion, each with a dropdown presentation and (API 30+) an inline presentation that respects the inline spec sent in the request.
6. Add an "All entries…" dataset that uses an authentication IntentSender to open the picker sheet. The picker returns the selected value as the fill result.
7. When a dataset is chosen, record the selection for learning (P2) and increment the use count.
8. Keep the fill request handler fast. Load entries from an in-memory cache kept warm from Room via Flow.

Save requests (offering to save newly typed values) are **out of scope for v1**.

### 9.5 Quick Settings tile flow
1. Tile tap → launch the picker sheet activity (on Android 14+, use the PendingIntent-based launch-and-collapse API).
2. User picks an entry → copy to clipboard (mark links as non-sensitive and phone or email as non-sensitive. Use the sensitive flag only if an entry is marked private in future) → haptic feedback → close.
3. Tile subtitle shows the active profile name.

### 9.6 Privacy and security
- No INTERNET permission. Declare that in the README later and in the Play listing.
- Exclude the database from cloud backup by default (backup rules), with export as the user-controlled backup.
- No logging of entry values. Logs may contain field signatures in debug builds only.
- The autofill service must not retain view structures beyond the request.

### 9.7 Browser support notes
- Chrome on Android: the user must enable "Autofill using another service" in Chrome's autofill settings. Onboarding and Settings should explain this with a deep link where possible.
- Firefox and other browsers: generally work with the Autofill Framework. Verify during testing.
- Some apps ignore autofill entirely. The tile covers these.

---

## 10. Quality bar

- No jank: 60fps lists, no main-thread database access.
- Autofill responds in under ~100ms on a mid-range device for a typical form.
- Cold start under 1s on a mid-range device.
- Works on Android 8 through the latest version. Test on at least API 26, 30, 34 and latest emulators.
- Proper dark and light themes, landscape-safe and large-font-safe.

---

## 11. Testing plan

- **Unit:** matching engine fixtures (Section 7.6), normalisation, value formatting, repository logic.
- **Instrumented:** Room DAOs, autofill structure parsing using a test activity with diverse fields (native EditTexts with and without hints, plus a WebView loading local HTML job-form fixtures stored in test assets).
- **UI:** Compose tests for vault list, editor validation, picker search.
- **Manual checklist:** fill real forms in Chrome (Greenhouse, Lever, Google Forms, Typeform), the LinkedIn app, Gmail compose, and WhatsApp (expected to need the tile).

---

## 12. Phased roadmap

### Phase 1: Foundation and vault
- Project scaffold, module setup, theme tokens (Section 8.2), fonts, app icon placeholder.
- Room + repository, CRUD for entries, categories, reorder, pin.
- Vault screen + entry editor + copy-with-feedback.
- **Acceptance:** can add, edit, delete, reorder and copy entries. The UI follows the design system in dark and light themes.

### Phase 2: Quick Settings tile and picker
- Picker sheet activity (search, pinned first, most-used order).
- TileService + add-tile prompt.
- **Acceptance:** from any app, shade → tile → pick → paste works in under 3 seconds.

### Phase 3: Matching engine
- `:core:matching` with dictionary, normalisation, scoring and ignore rules.
- 40+ fixture tests passing.
- **Acceptance:** all fixture tests are green, and ambiguous and negative cases behave as specified.

### Phase 4: Autofill service
- Service declaration and configuration, structure parsing, dataset building (dropdown + inline), "All entries…" via picker.
- Onboarding and settings for enabling autofill, plus Chrome guidance.
- **Acceptance:** scenario S1 and S2 work in Chrome and in a native test app. Ignored fields show nothing.

### Phase 5: Profiles, learning and polish
- Profiles + switcher (app, tile subtitle, picker).
- Learned mappings + settings screen to clear them.
- Onboarding flow complete, empty states, motion polish, accessibility pass.
- Export and import JSON.
- **Acceptance:** scenarios S4–S6 work, and the accessibility checklist passes.

### Phase 6: Release prep
- Final icon and branding, Play Store listing assets (screenshots, short description, privacy statement), R8 or minification, signed release build, README.
- **Acceptance:** release APK or AAB builds and installs cleanly. Sideload test on a real device.

---

## 13. Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Only one autofill service can be active, so the user may lose password-manager autofill | Explain this clearly in onboarding. The tile works regardless. Consider an "enable only when job hunting" tip. |
| Web forms have poor labels | Multi-signal scoring, nearby-label detection, learning, and the always-present "All entries…" option |
| Chrome needs a manual setting | Clear onboarding step with screenshots and deep link |
| Inline suggestions styling is limited | Keep presentations simple. Ensure the dropdown fallback looks great. |
| Clipboard confirmation toasts on Android 13+ | Accept the system toast and show our own feedback only inside the app |
| OEM quirks (Samsung, Xiaomi, Tecno, Infinix) | Test on at least one OEM device. The tile is the universal fallback. |

---

## 14. Future: Chrome extension (desktop)

Same concept for laptop forms. Planned features: popup vault editor, a keyboard-shortcut command palette, a right-click "Insert →" menu, text shortcuts (e.g., `;li`), and the same matching engine logic ported to TypeScript (keep the keyword dictionary in a shareable format so both platforms use one source). Data sync between phone and extension is a separate future decision (export/import first, then possibly encrypted sync).

---

## 15. Open questions (decide during build)
- Final icon.
- Accent colour choice (lime vs coral vs other).
- Hilt vs Koin.
- Whether to publish on the Play Store or keep it as a sideloaded personal tool first.
