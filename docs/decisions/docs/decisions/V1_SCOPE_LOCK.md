# Mahout V1 Scope Lock (FAST-SHIP)

**Status:** LOCKED for V1 fast-ship.
**Purpose:** This file is the “contract” that prevents scope creep and rework.
If something is not explicitly IN SCOPE here, it is OUT OF SCOPE until Beta/V2.

---

## 0) What “V1” means for Mahout (FAST-SHIP)
V1 must feel like “real Mahout” because:
- **Path** looks premium and is satisfying to use (timeline is not ugly/temporary).
- **North Star** is reliable: it never crashes even if AI output is bad.

V1 is NOT a full product. It is a usable, stable core with the minimum set of flows.

---

## 1) Product-wide rules (non-negotiable)
### 1.1 Tech + architecture
- Android Studio project
- Kotlin + XML views only (NO Jetpack Compose)
- Single-Activity, multi-fragment (Navigation Component)
- ViewBinding (not DataBinding)
- MVVM:
    - UI (Fragment/Activity) → ViewModel (StateFlow) → UseCases → Repositories → Data sources (Room first)
- Hilt DI everywhere (ViewModels, repos, Room DB)

### 1.2 Data model rules
- Local-first using Room
- No accounts, no login, no cloud sync in V1
- All user data exists only on device
- **WARNING:** V1 has NO backup/export/import (deferred to Beta)
    - Settings must show: **“Beta: Data may be lost; no backup yet.”**

### 1.3 Time + timezone rules
- Store timestamps as `Instant` (epoch millis in DB)
- Derive “day buckets” using user’s local timezone (`ZoneId.systemDefault()`)
- All timeline math must be resilient to DST and midnight boundaries

### 1.4 Quality gates (definition of done)
V1 is acceptable only if:
- App does not crash during normal flows
- Timer does not “die” when app goes background (ForegroundService)
- North Star never hard-crashes on invalid AI output (fallback UI always)
- Offline use works (Room is local)

---

## 2) V1 scope by tab (FAST-SHIP)

### 2.1 Path (MUST FEEL AMAZING)
#### In scope (V1)
**Time-based Actions only**:
- Create/edit/archive an Action (TIME tracking only)
- Start/Pause/Resume/Stop a timer for a selected Action
- ForegroundService + persistent notification
- Session records saved locally
- Manual log time flow (select time range → creates session)
- Session list per Action (edit/delete)
- Timeline renderer:
    - **Worked sessions** render as continuous “filled blocks” (not 10-min cells)
    - **Unused time** renders as **10-minute cells**
    - Tap behavior:
        - Tap a **filled block** → confirm **delete** (no recolor/edit in V1)
        - Tap unused cells → select range → “Log time” (creates session)

#### Out of scope (V1)
- Checklist / check-off actions (completions without time)
- Multi-color tagging, category palettes, advanced styling rules
- Social sharing, “streak share cards,” invites

---

### 2.2 Aim (Meaning + Structure)
#### In scope (V1)
- Chief Aim: CRUD (likely single record)
- Goals CRUD (simple)
- Actions can optionally be linked to a Goal:
    - **Constraint: an Action can be linked to 0 or 1 Goal**
    - User can unlink any time
- Basic receipts:
    - Goal detail shows totals for time logged from linked Actions
    - Weekly rollups (basic)

#### Out of scope (V1)
- Many-to-many linking UI (Action linked to multiple goals at once)
- Growth mechanics (invite flows, share loops)

---

### 2.3 Elephant (Mood)
#### In scope (V1)
- Mood log (feeling + optional intensity + optional note)
- History list
- Saving a mood log can trigger a small North Star “micro-response” prompt

#### Out of scope (V1)
- Social sharing/feed/posting
- Advanced analytics beyond simple 7-day trend

---

### 2.4 Mahout (Journaling hub)
#### In scope (V1)
- Unified journal list
- Create/edit entries
- Entry types (as locked in your V1 spec)
- Prompt chips/templates (simple)

#### Out of scope (V1)
- Growth mechanics
- Audio playback features (unless explicitly required later)

---

### 2.5 North Star (CORE DIFFERENTIATOR)
#### In scope (V1)
- Conversation UI that renders from **validated models**
- **JSON-first** AI output:
    - Strict parsing
    - Schema versioning
    - UI fallback card if parsing fails (never crash)
- Timing rules:
    - Morning: first open trigger
    - Evening: after ~7pm trigger
    - Mood-trigger trigger
- Memory system:
    - Memory summaries + future profile records (stored locally)
    - Settings screen must include:
        - Clear memory/data deletion semantics
        - “Safety/trust” footer + crisis link

#### Out of scope (V1)
- Cloud sync
- Human-to-human sharing
- Export/import backup (deferred to Beta)

---

## 3) Explicit V1 cuts (FAST-SHIP decisions)
These are “hard no’s” for V1:
- ✅ NO checklist/check-off tracking (time-based only)
- ✅ NO backup/export/import (Beta only)
- ✅ NO viral growth / share cards / invites
- ✅ NO accounts/auth/sync

---

## 4) Terms (so we don’t argue later)
- **Action:** A trackable unit in Path (time-based in V1).
- **Session:** A continuous time log with start/end/duration for an Action.
- **Timeline:** Day view made of:
    - Worked session blocks (continuous)
    - Unused 10-minute cells (selectable)

---

## 5) Repo status snapshot (so we know where we are)
- Phase 0 (Nav shell + tabs + Hilt + ViewBinding + Settings entry point): DONE on main
- Phase 1 (Room schema): in progress on feature branch
- Day 1 docs (this file + ADRs): to be committed now

---

## 6) How we handle “new ideas”
If a new idea comes up:
1) If it’s in scope, we implement it.
2) If it’s out of scope, we add it to a “Beta backlog” list (not implemented in V1).
3) If it changes a locked decision, we must write a new ADR and accept the rework cost explicitly.
