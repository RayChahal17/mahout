# ADR-0002: Store timestamps as Instant; derive day/week buckets from ZoneId

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
Mahout depends heavily on time (sessions, timeline, weekly rollups).
Time bugs destroy trust:
- DST shifts
- midnight boundaries
- “yesterday vs today” confusion
- user traveling across timezones

---

## Decision
- Store all event timestamps in DB as `Instant` (Room converter uses epoch millis).
- When the UI needs “day buckets”, derive using:
    - `ZoneId.systemDefault()`
    - Convert `Instant` → `ZonedDateTime` → `LocalDate`
- Timeline day view is computed by:
    - local day start: `LocalDate.atStartOfDay(zone)`
    - local day end: next day start

---

## Options considered
1) Store as LocalDateTime only
2) Store as Long epoch millis (manual conversions everywhere)
3) Store as Instant + shared conversion utilities (chosen)

---

## Consequences
### Positive
- DST-safe, travel-safe
- Clear separation of “storage time” vs “display time”
- Fewer bugs in receipts/rollups

### Negative / tradeoffs
- Need discipline: never compare Instants as if they were local dates

### Follow-ups
- Create `core/time/TimeProvider` later for testability
- Add unit tests for day-bucketing and DST boundaries
