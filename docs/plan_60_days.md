Mahout V1 — Updated 60‑Day Build Plan
(FAST‑SHIP • TIME‑ONLY)
Scope source of truth: Mahout V1 Scope Lock (Fast‑Ship Time‑Only) — last updated: 2025‑12‑18
Absolute exclusions (DO NOT build in V1)
☐ Check‑offs / CheckEvents / checklists / “did it today” toggles
☐ Encrypted backup/export/import/restore
☐ Growth/viral/share mechanics
Non‑negotiables (must ship)
☐ Path: time‑based Actions + Sessions, Timer (ForegroundService), Manual Log Time, Sessions list,
Timeline/day‑map renderer, unused 10‑min cells selection → log time, polish + correctness
☐ Aim: Chief Aim, Goals CRUD, Action ↔ Goal linking (0/1 per Action), time‑based receipts/rollups
(7‑day activity + weekly)
☐ Elephant: Mood log + history + basic trends; mood log triggers North Star micro‑response
☐ Mahout: Journal hub (unified list + editor)
☐ North Star: Unbreakable JSON‑first renderer (validated models only) + fallback card,
schedule‑on‑open rules (morning/evening) + mood‑trigger, memory system + deletion semantics
☐ Settings: entry point from overflow across app, Memory enable/disable, Clear memory, Clear data
(danger zone), Beta warning (“Data may be lost; no backup yet”), Safety/trust footer + crisis link
(always accessible)
NEW LOCKED FEATURE (V1): Meditation = Text‑Guided + Ambient
Mode A (screen‑only)
Why this is locked in V1: it keeps the experience “guided” via paced reading, without re‑introducing voice/
TTS complexity, and Mode A avoids background audio/services.
What we WILL ship in V1
☐ A) Nightly Meditation Scripts (text, regenerated nightly)
☐ Generate one script per evening (first open after 7pm)
☐ Regenerates nightly (new script each evening; cached per date so it’s deterministic for that night)
☐ Script includes: title + type (Calm Reset / Work Temple / Future You / Gratitude)
☐ Script includes: steps/chunks (short paragraphs)
☐ Script includes: closing bridge → next action (Journal or Path time block)
☐ B) Guided reading experience (still “guided” without voice)
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
1
☐ Dedicated Meditation screen
☐ Shows one chunk at a time
☐ Optional auto‑advance pacing (timer‑based), pause/resume, next/prev
☐ Shows remaining time + progress
☐ C) Ambient “Mode A” audio (optional, no background)
☐ Small library of ambience loops (rain, temple drone, white noise, etc.)
☐ Tap to play while reading
☐ Hard rule: audio stops when user leaves Meditation screen (no background, no service, no
notifications)
☐ D) Logging
☐ Completing a meditation logs time (time‑only receipts remain consistent)
☐ At minimum: “Meditation minutes completed today” visible to North Star context
☐ E) Evening trigger
☐ Fits existing schedule rule: first open after 7pm triggers evening content
☐ Meditation script generation happens here
Scheduling note (updated): We build Meditation after North Star’s memory + prompt/context system is
working, since the best scripts depend on memory/context.
What we will NOT ship in V1 (explicit)
☐ No spoken voice (no TTS, no generated voice, no recording pipeline)
☐ No background playback (no Mode B / no media notification / no audio service)
☐ No download manager / streaming audio packs (bundle a few loops or ship no audio)
☐ No check‑offs mechanics
Dates
This plan covers 60 days from Fri Dec 19, 2025 → Mon Feb 16, 2026.
Daily Definition of Done (DoD) — always true
☐ App builds & runs on a real device
☐ Today’s slice works end‑to‑end (even if ugly)
☐ Commit to Git (main stays runnable)
☐ 1‑line dev log: “Tomorrow: next smallest step is ___”
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
2
DAY‑BY‑DAY
Note: To keep Canvas checkboxes visible, each day uses only top‑level checkboxes.
Day 01 — Fri Dec 19 — Scope freeze + decisions (UNCHANGED)
☐ Create repo root file V1_SCOPE_LOCK.md (exclusions + must‑ship list + DoD)
☐ Create docs/decisions/ADR_TEMPLATE.md (Context/Decision/Consequences)
☐ Write ADR #1: time storage = epoch millis; local bucketing uses explicit ZoneId
☐ Write ADR #2: timeline implementation choice (RecyclerView‑based OR custom view)
☐ Write ADR #3: unused‑cell selection = 10‑min granularity; default action = last used
☐ Create Git repo + .gitignore (ignore .idea/ , *.iml , /build/ )
☐ Decide branch policy: main always runnable; feature branches for risky work
☐ Commit: chore: scope lock + ADRs + repo hygiene
Day 02 — Sat Dec 20 — Project bootstrap (XML only) (UNCHANGED)
☐ Create Android project (Kotlin + XML, NO Compose)
☐ Set package name (target com.mahout.app )
☐ Enable ViewBinding in app/build.gradle
☐ Remove Compose template files/deps if wizard added them
☐ Add base Material theme resources (themes.xml, colors.xml, typography.xml)
☐ Run on a real device once (baseline) and screenshot home screen
☐ Create README.md (run instructions + device notes)
☐ Commit: chore: android project bootstrap
Day 03 — Sun Dec 21 — Core deps + Hilt baseline (UNCHANGED)
☐ Add deps: Material, Navigation, Hilt, Room, Coroutines, Lifecycle
☐ Apply Hilt Gradle plugin + kapt (or ksp—choose and lock)
☐ Create MahoutApplication with @HiltAndroidApp
☐ Create di/DispatchersModule.kt (IO/Default/Main + qualifiers)
☐ Create logging wrapper (Timber optional)
☐ Verify DI works by injecting a test dependency into Activity
☐ Run on device; ensure no Hilt init crash
☐ Commit: chore: add core deps + hilt baseline
Day 04 — Mon Dec 22 — Nav skeleton + Settings entry point
(UNCHANGED)
☐ Create MainActivity hosting NavHostFragment
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
3
☐ Add bottom nav with 5 destinations: Aim/Path/Elephant/Mahout/North Star
☐ Create 5 Fragments + layouts (placeholders + ViewBinding)
☐ Add overflow menu in toolbar
☐ Create SettingsFragment (stub) and wire overflow → Settings
☐ Confirm Settings reachable from every tab
☐ Confirm Settings back returns to previous tab
☐ Commit: chore: nav skeleton + settings entry
Day 05 — Tue Dec 23 — UI foundation + reusable components
(UNCHANGED)
☐ Create a base toolbar approach (single style)
☐ Add reusable Loading overlay view (XML include)
☐ Add reusable EmptyState component (icon + title + body + CTA)
☐ Add Confirm dialog helper (title/body/confirm/cancel)
☐ Add Snackbar helper for errors/success
☐ Add dimens: spacing_4/8/12/16/24/32
☐ Add consistent text appearances (title/body/caption)
☐ Commit: chore: UI foundation components
Day 06 — Wed Dec 24 — Room database scaffold (schema‑first)
☐ Create data/local/MahoutDatabase.kt (version=1)
☐ Lock time storage approach: store times as Long epochMillis (ADR if not already)
☐ Create empty DAO interfaces in data/local/dao/
☐ Create di/DatabaseModule.kt providing DB + DAOs
☐ Create dev-only “DB sanity” screen (lists table counts)
☐ Confirm DB opens on device (no migration crash)
☐ Commit: data: room db scaffold
Day 07 — Thu Dec 25 — Entities/DAOs: Path + Aim core
☐ Add ChiefAimEntity + ChiefAimDao
☐ Add GoalEntity + GoalDao
☐ Add ActionEntity + ActionDao (time‑based only)
☐ Ensure ActionEntity.goalId is nullable (0/1 goal link) OR implement link table (ActionGoalLink)
☐ Add indices on Action: goalId + schedule fields
☐ Add SessionEntity + SessionDao (startAtMillis/endAtMillis/durationMillis/actionId/note?)
☐ Add index on Session: actionId + startAtMillis
☐ Add foreign keys but avoid cascades that delete history
☐ Add basic DAO queries: insert/update/delete/list
☐ Commit: data: aim/path entities + daos
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
4
Day 08 — Fri Dec 26 — Entities/DAOs: Elephant + Mahout + North
Star storage
☐ Add MoodLogEntity + MoodLogDao (timestamp + mood + note)
☐ Add JournalEntryEntity + JournalEntryDao (type + body + timestamps)
☐ Add MemoryEntity + MemoryDao
☐ Add FutureProfileEntity + FutureProfileDao
☐ Add NorthStarMessageEntity + NorthStarMessageDao (rawJson/schemaVersion/messageType/
createdAt/parseStatus/errorCode)
☐ Add DAO queries: latest, list by date, delete
☐ Confirm DB sanity screen shows counts for all tables
☐ Commit: data: elephant/mahout/northstar entities + daos
Day 09 — Sat Dec 27 — Architecture pattern (MVVM + Repo +
UseCases)
☐ Create domain models: ChiefAim, Goal, Action, Session, MoodLog, JournalEntry
☐ Create mappers Entity↔Domain in data/mapper/
☐ Create repository interfaces in domain/repo/
☐ Create Room-backed implementations in data/repo/
☐ Create one UseCase (e.g., UpsertGoalUseCase)
☐ Create one ViewModel using viewModelScope + StateFlow
☐ Wire one Fragment to ViewModel and show data
☐ Add one unit test for a pure function (proves test setup)
☐ Commit: arch: mvvm repo usecase baseline
Day 10 — Sun Dec 28 — Aim: Chief Aim CRUD
☐ Aim screen shows current Chief Aim or empty state
☐ Add edit UI (dialog/sheet) with validation (non‑empty)
☐ ViewModel state: Loading/Content/Error
☐ Save via UseCase → Repo → Room (IO dispatcher)
☐ Handle first‑run create (no record yet)
☐ Show snackbar: “Saved” / friendly errors
☐ Commit: feat(aim): chief aim crud
Day 11 — Mon Dec 29 — Aim: Goals CRUD (updated to match
current direction)
☐ Goals list (RecyclerView + DiffUtil)
☐ Add/edit goal dialog: title + optional why + optional targetDate
☐ Auto‑derive horizon from targetDate (prevents mismatch)
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
5
☐ Roadmap buckets UI: horizontal scroll headings (30d / 1–6m / 6–24m / 2–10y / Archived)
☐ Archive goal confirmation (soft archive)
☐ Delete goal confirmation (soft delete)
☐ Delete behavior: unlink actions (do NOT delete sessions)
☐ Commit: feat(aim): goals crud
Day 12 — Tue Dec 30 — Path: Actions CRUD (time‑only, 0/1 goal link)
(updated label)
☐ Actions list screen (shows cadence + optional target minutes)
☐ Add/edit action screen (dialog ok)
☐ Field: name (required)
☐ Field: schedule cadence (one‑time/daily/weekly)
☐ Field: optional target minutes
☐ Field: goal link picker (0/1) + unlink
☐ Persist changes + refresh list
☐ Verify unlink keeps past sessions intact
☐ Commit: feat(path): actions crud (time‑only)
Day 13 — Wed Dec 31 — Path: ForegroundService timer (skeleton)
☐ Add manifest entries for ForegroundService
☐ Create TimerForegroundService class
☐ Create notification channel + ongoing notification
☐ Add notification actions: Pause/Resume/Stop
☐ Android 13+ notification permission request flow (UI + fallback)
☐ Choose TimerState persistence and record in ADR (Room table recommended)
☐ Confirm service starts and shows notification on device
☐ Commit: feat(path): timer foreground service scaffold
Day 14 — Thu Jan 01 — Path: Timer card UI + controller layer
☐ Build Timer card UI (action picker + start/pause/resume/stop)
☐ Create TimerController abstraction (UI never touches service directly)
☐ Implement service binding or command intents through controller
☐ ViewModel observes timer state Flow and updates UI
☐ Define behavior for switching actions while running (block OR stop+switch)
☐ Commit: feat(path): timer card + controller
Day 15 — Fri Jan 02 — Stop flow → Session persistence
☐ When Stop pressed: compute start/end/duration
☐ Persist Session via UseCase/Repo
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
6
☐ Guardrail: duration > 0
☐ Guardrail: endAt >= startAt
☐ Guardrail: actionId exists
☐ Optional note capture sheet on stop
☐ Confirm session appears in Sessions list
☐ Commit: feat(path): stop creates session
Day 16 — Sat Jan 03 — Manual Log Time flow
☐ Manual log UI: pick action
☐ Start time picker
☐ End time picker
☐ Allow crossing midnight; show computed duration
☐ Validate end != start
☐ Insert session
☐ Implement overlap detection function (pure)
☐ Show warning dialog if overlap
☐ Commit: feat(path): manual log time
Day 17 — Sun Jan 04 — Sessions list + delete (edit optional)
☐ Sessions list grouped by day
☐ Row shows action name + start/end + duration
☐ Tap session opens details bottom sheet
☐ Delete confirmation dialog
☐ Delete executes on IO
☐ Optional (only if ahead): edit session times/action
☐ Commit: feat(path): sessions list + delete
Day 18 — Mon Jan 05 — Timeline renderer v1 (visual contract)
☐ Render selected day (Today)
☐ Render time axis 00:00→24:00
☐ Render sessions as blocks proportional to time
☐ Render unused time as 10‑min cells (for selection layer)
☐ Block labels: action name + duration
☐ Prev/next day buttons
☐ Commit: feat(path): timeline renderer v1
Day 19 — Tue Jan 06 — Timeline interactions + selection behavior
☐ Tap session block opens details sheet with delete
☐ Single tap selects one unused cell
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
7
☐ Long‑press starts selection mode
☐ Drag expands selection
☐ Tap toggles cells on/off
☐ Show selected state + total minutes counter
☐ Clear selection button
☐ Commit: feat(path): timeline selection interactions
Day 20 — Wed Jan 07 — Selection → “Log X minutes” + overlap
snapping
☐ Selection opens bottom sheet
☐ Default action = last used
☐ Allow changing action
☐ Optional note field
☐ Confirm creates a Session with selected start/end
☐ Selection snaps/limits at existing sessions
☐ Final insert double-checks overlaps
☐ Unit test overlap/snap algorithm
☐ Commit: feat(path): selection -> log time
Day 21 — Thu Jan 08 — Time math hardening (DST/timezone)
☐ Create TimeUtils conversions (epoch millis ↔ LocalDateTime)
☐ Create startOfDay/endOfDay with ZoneId
☐ Create startOfWeek/endOfWeek Mon–Sun
☐ Test DST days (23/25 hour days)
☐ Test crossing midnight
☐ Fix off-by-one day rendering
☐ Commit: chore(time): harden day/week boundaries
Day 22 — Fri Jan 09 — Foreground reliability + Path polish
☐ Test: background + lock
☐ Test: swipe away app
☐ Test: low memory (if possible)
☐ Test: notification actions work
☐ Ensure timer state survives process death
☐ Timeline performance test with many sessions
☐ Empty states + helpful copy
☐ Touch targets + spacing polish
☐ Commit: chore(path): reliability + polish
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
8
Day 23 — Sat Jan 10 — Aim receipts foundation (time‑only)
☐ DAO query: total minutes per action for range
☐ DAO query: total minutes per goal for range
☐ Calculator: activeDays/7
☐ Calculator: weekly totals
☐ Unit tests for calculators (edge days)
☐ Add placeholder receipts UI block
☐ Commit: feat(aim): receipts foundation (time‑only)
Day 24 — Sun Jan 11 — Goal detail receipts UI (minimal)
☐ Goal detail screen (header + placeholder receipts panel)
☐ Show last 7 days total minutes
☐ Show activeDays/7
☐ Show last touched date
☐ Ensure queries run on IO dispatcher
☐ Empty state when no sessions
☐ Commit: feat(aim): goal receipts UI
Day 25 — Mon Jan 12 — Weekly rollups consistency
☐ Enforce Mon–Sun boundary in TimeUtils
☐ Show “This week” minutes per goal
☐ Optional: show last week minutes + delta
☐ Unit tests for week boundary
☐ Fix any mismatches vs raw sessions
☐ Commit: chore(aim): weekly rollups consistent
Day 26 — Tue Jan 13 — Aim/Path integration QA
☐ E2E: create goal → action → link
☐ E2E: track time → session saved
☐ E2E: receipts update after save
☐ E2E: selection log creates session
☐ Check: unlink action keeps old sessions
☐ Fix top 5 bugs
☐ Write docs/qa/smoke_test.md
☐ Commit: chore: integration QA
Day 27 — Wed Jan 14 — Elephant: Mood log entry
☐ Mood entry UI (quick choice)
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
9
☐ Optional note field
☐ Save MoodLog via MVVM + Room
☐ Empty state
☐ Success snackbar
☐ ViewModel emits “moodSaved” event
☐ Commit: feat(elephant): mood log
Day 28 — Thu Jan 15 — Elephant: Mood history + basic trends
☐ Mood history list grouped by day
☐ Delete confirmation
☐ Delete executes on IO
☐ Basic trends: last 7 logs summary
☐ No-crash on empty data
☐ Commit: feat(elephant): history + basic trends
Day 29 — Fri Jan 16 — Mood triggers North Star (event + cool‑down)
☐ On mood saved, emit trigger event
☐ Persist last-trigger timestamp
☐ Enforce cool-down (no spamming)
☐ Call mock NorthStarService
☐ UI shows loading
☐ UI shows fallback on error
☐ Commit: feat(elephant): mood triggers north star (mock)
Day 30 — Sat Jan 17 — Mahout: Journal list + create
☐ Journal list (reverse chronological)
☐ Add entry screen
☐ Save entry to Room
☐ Empty state
☐ Delete confirmation
☐ Commit: feat(mahout): journal list + create
Day 31 — Sun Jan 18 — Mahout: Journal editor hardening
☐ Autosave draft locally
☐ Edit existing entries
☐ Delete confirmation
☐ Keyboard/IME polish
☐ Commit: feat(mahout): editor hardening
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
10
Day 32 — Mon Jan 19 — Cross-tab integration
☐ Add “journal this feeling” shortcut from mood history
☐ Prefill journal editor with mood context
☐ Back stack returns correctly
☐ Fix navigation bugs
☐ Commit: chore: elephant->mahout integration
Day 33 — Tue Jan 20 — North Star: JSON schema models + strict
parser (NO meditation yet)
☐ Choose JSON library and lock it (Moshi recommended)
☐ Define schemaVersioned model: DailyLetter
☐ Define schemaVersioned model: MicroResponse
☐ Define common blocks: title/body/cta
☐ Strict parser + validator (schemaVersion required)
☐ Parser returns typed error
☐ Fixtures: valid JSON
☐ Fixtures: invalid JSON
☐ Commit: feat(northstar): schema + strict parser
Day 34 — Wed Jan 21 — North Star: Unbreakable renderer (NO
meditation yet)
☐ RecyclerView renderer for validated models
☐ Fallback card on parsing failure
☐ Fallback card on network failure
☐ Fallback card on empty payload
☐ Retry button triggers fetch
☐ Optional dev-only raw JSON viewer
☐ Prove malformed JSON never crashes UI
☐ Commit: feat(northstar): unbreakable renderer
Day 35 — Thu Jan 22 — North Star: Provider abstraction + cache
☐ Create NorthStarService interface returning Result
☐ Implement mock provider from fixtures
☐ Save raw JSON + metadata to Room
☐ Render cached immediately
☐ Offline-first behavior verified
☐ Commit: feat(northstar): provider + cache
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
11
Day 36 — Fri Jan 23 — North Star: Schedule-on-open rules (morning/
evening)
☐ Morning: first open triggers daily letter
☐ Evening: first open after 7pm triggers evening content (non-meditation placeholder for now)
☐ Persist last-run timestamps
☐ Debug force-run toggle
☐ Rate limit within X minutes
☐ Commit: feat(northstar): schedule-on-open rules
Day 37 — Sat Jan 24 — North Star: Memory system v1 (engine, not
UI)
☐ Memory repository + use cases (add/read/delete)
☐ Deletion semantics: deleted memory never appears
☐ Prompt builder can query memory snippets (behind feature flag)
☐ Unit tests for memory filtering + deletion
☐ Commit: feat(northstar): memory system v1
Day 38 — Sun Jan 25 — North Star: Prompt/context builder v1
(sessions + mood + memory)
☐ Context includes last 7 days sessions summary
☐ Context includes inactivity streak
☐ Context includes last mood
☐ Context includes Future Profile (if set)
☐ Memory snippets included only if enabled
☐ Unit tests for context builder
☐ Commit: feat(northstar): prompt context builder
Day 39 — Mon Jan 26 — North Star end-to-end integration (trigger
→ request → cache → render)
☐ Trigger → request → cache → render works
☐ Cached render appears instantly
☐ Refresh runs only when schedule allows
☐ Offline behavior: cached shows + clear message
☐ Fix integration bugs
☐ Commit: chore: north star end-to-end
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
12
Day 40 — Tue Jan 27 — Settings: Memory controls
☐ Toggle: enable/disable memory
☐ Prompt builder excludes memory when disabled
☐ Button: clear memory (confirmation)
☐ Button: clear data (typed confirmation)
☐ Large DB operations on IO + loading UI
☐ Commit: feat(settings): memory + clear data controls
Day 41 — Wed Jan 28 — Settings: Beta warning + Safety footer
☐ Beta warning shown prominently
☐ Safety/trust footer always accessible (crisis link + disclaimers)
☐ About/version section
☐ Settings reachable from overflow across all tabs
☐ Commit: feat(settings): beta + safety footer
Day 42 — Thu Jan 29 — Meditation: Nightly text script model +
cache wiring (AFTER memory)
☐ Define schemaVersioned model: NightMeditationScript (title/type/chunks/bridge)
☐ Add fixtures: valid/invalid meditation JSON
☐ Add provider route/tag for “evening meditation script”
☐ Cache script payload per date (deterministic for that night)
☐ Add entry point card/button to open Meditation screen when script exists
☐ Commit: feat(meditation): nightly script + cache
Day 43 — Fri Jan 30 — Meditation: Guided reader screen (chunk-bychunk)
☐ Dedicated Meditation screen
☐ Shows one chunk at a time
☐ Next/prev + pause/resume
☐ Progress indicator + remaining time
☐ Optional auto-advance pacing (timer-based, NOT a service)
☐ Commit: feat(meditation): reader screen
Day 44 — Sat Jan 31 — Meditation: Ambient Mode A (screen-only)
☐ Bundle 1–3 ambience loops (or stub with 1)
☐ MediaPlayer playback only while Meditation screen is visible
☐ Hard rule: stop in onPause/onStop + when navigating away
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
13
☐ No background playback, no notification, no service
☐ Commit: feat(meditation): mode-a ambience
Day 45 — Sun Feb 01 — Meditation: Logging + North Star context
hook
☐ Completing meditation logs time (Session or dedicated MeditationSession)
☐ Ensure “Meditation minutes completed today” is computable
☐ Include meditation minutes in North Star context (memory-aware scripts benefit)
☐ Commit: feat(meditation): logging + context
Day 46 — Mon Feb 02 — Crash-proofing pass (lifecycle)
☐ Rotate each screen; no crashes
☐ repeatOnLifecycle used everywhere
☐ “Don’t keep activities” test
☐ Fix leaks/double collectors
☐ Commit: chore: lifecycle hardening
Day 47 — Tue Feb 03 — Path torture tests (timer reliability)
☐ Timer background test
☐ Lock screen test
☐ Stop from notification saves session
☐ Kill app → reopen timer state correct
☐ Fix ForegroundService edge cases
☐ Commit: chore(path): torture tests
Day 48 — Wed Feb 04 — Timeline torture tests
☐ Many sessions performance test
☐ Selection never overlaps sessions
☐ Day switching correct
☐ Fix rendering jank
☐ Commit: chore(path): timeline torture tests
Day 49 — Thu Feb 05 — Receipts correctness
☐ Spot-check rollups vs raw sessions
☐ Weekly boundary check
☐ Fix off-by-one errors
☐ Update QA checklist
☐ Commit: chore(aim): receipts correctness
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
14
Day 50 — Fri Feb 06 — North Star “unbreakable” test suite
☐ Test: missing fields
☐ Test: wrong types
☐ Test: unknown enum values
☐ Test: huge strings
☐ Test: empty payload
☐ Verify fallback always renders
☐ Verify app never crashes
☐ Commit: test(northstar): unbreakable suite
Day 51 — Sat Feb 07 — Offline-first behavior QA
☐ Airplane mode: North Star cached feed renders
☐ Mood logs work offline
☐ Journal works offline
☐ Clear error messaging for refresh failures
☐ Fix caching bugs
☐ Commit: chore: offline-first QA
Day 52 — Sun Feb 08 — Regression checklist v1 (write + run)
☐ Write docs/qa/regression_checklist.md
☐ Run full fresh-install flow
☐ Log top 10 bugs
☐ Fix top 3 critical bugs
☐ Commit: chore: regression checklist + fixes
Day 53 — Mon Feb 09 — Bug bash day
☐ Fix crashers first
☐ Fix timer reliability issues
☐ Fix navigation/state loss
☐ Re-run smoke test
☐ Commit
Day 54 — Tue Feb 10 — UX + performance consolidation day
☐ Empty states + copy improvements
☐ Loading UI everywhere
☐ Error UI everywhere
☐ Confirm dialogs consistent
☐ DiffUtil for all lists
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
15
☐ DB work on IO dispatcher
☐ Remove re-query loops
☐ Commit: chore: UX polish + performance
Day 55 — Wed Feb 11 — App identity + release build readiness
☐ App name
☐ App icon
☐ Versioning (versionCode/versionName)
☐ Release build compiles
☐ Permissions audit (only needed permissions remain)
☐ Install release build on device
☐ Commit: chore: identity + release readiness
Day 56 — Thu Feb 12 — Regression pass #2 + data integrity
verification
☐ Re-run regression checklist
☐ Fix remaining blockers
☐ Freeze behavioral changes
☐ Generate large session dataset
☐ Verify delete flows don’t break history
☐ Verify clear data returns clean state
☐ Commit
Day 57 — Fri Feb 13 — Soak test day
☐ 2-hour real device soak test
☐ Background/foreground repeatedly
☐ Fix remaining crashers
☐ Commit
Day 58 — Sat Feb 14 — Release candidate tag + known issues
☐ Tag v1-rc1
☐ Write known issues + beta warning notes
☐ Backup repo remotely
☐ Prepare Feb 17–20 bug list
Day 59 — Sun Feb 15 — Final smoke test + freeze
☐ Final smoke test
☐ No new features
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
•
16
☐ Only bug fixes from now on
☐ Prepare final release checklist for Feb 20
Day 60 — Mon Feb 16 — Freeze + handoff checklist
☐ Final smoke test (again)
☐ No new features
☐ Only bug fixes
☐ Prepare final release checklist for Feb 20
•
•
•
•
•
•
17