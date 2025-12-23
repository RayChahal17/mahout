# ADR-0008: Timer persistence uses TimerState table + Session segments

**Status:** Accepted  
**Date:** 2025-12-23  
**Owner:** Mahout

---

## Context

The Path timer must be reliable across:
- backgrounding (user leaves the app)
- configuration changes
- process death / OS reclaim (less common but real)

We already have a `sessions` table that represents time logs.

However, sessions alone do **not** clearly represent the timer UX states:
- RUNNING vs PAUSED vs STOPPED

If we only look at "is there a session with endAt = null":
- RUNNING can be inferred (maybe)
- PAUSED vs STOPPED cannot be distinguished

---

## Decision

We persist timer UX state in a **singleton Room table**:

- `timer_state` (one row, `timerId = "timer"`)
- fields: `status`, `actionId`, `currentSessionId`, `accumulatedMillis`, `updatedAt`

We still store actual work time in `sessions`:

- When RUNNING: create an in-progress session row (endAt = null)
- When PAUSE: finalize that session (endAt + durationMillis) and set timer_state = PAUSED
- When RESUME: create a new in-progress session row and set timer_state = RUNNING
- When STOP: finalize any in-progress session and clear timer_state (meaning STOPPED)

This means one "timer run" may produce multiple session rows (one per RUNNING segment).

---

## Consequences

### Positive
- UI can always tell RUNNING vs PAUSED vs STOPPED reliably
- Durability: state survives process death
- No schema changes required to support pause/resume (no need to add pause columns to sessions)

### Negative / tradeoffs
- A single logical "run" can generate multiple sessions (may show as separate blocks later)
- We need to merge segments later if we want a single block in the timeline UI

### Follow-ups
- Day-map renderer can optionally merge adjacent segments from the same action
  if they are close enough (ex: < 1 minute gap) to present a single block.
- Add UI timer card (Day 14) that reflects TimerState.
