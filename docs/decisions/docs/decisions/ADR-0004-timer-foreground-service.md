# ADR-0004: Timer uses ForegroundService + notification controls

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
A normal coroutine timer in a ViewModel dies when:
- app backgrounds
- process is reclaimed
- user opens another app

Path dies if the timer is flaky.

---

## Decision
- Implement timer using an Android ForegroundService.
- Show a persistent notification while timing.
- Notification can include basic actions (Pause/Stop) if feasible.
- The “source of truth” for time is:
    - service start Instant
    - paused durations (if we implement pause)
    - current Instant

---

## Options considered
1) ViewModel-only timer (not reliable)
2) WorkManager (wrong tool for real-time timer)
3) ForegroundService (chosen)

---

## Consequences
### Positive
- Reliable timing across backgrounding
- User trust improves

### Negative / tradeoffs
- More boilerplate (service lifecycle)
- Must handle notification permission on newer Android versions

### Follow-ups
- Decide pause implementation model (single session with pauses vs split sessions)
- Add “in-progress session” row in DB
