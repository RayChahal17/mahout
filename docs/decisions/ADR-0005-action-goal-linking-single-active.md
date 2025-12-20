# ADR-0005: Action can be linked to 0 or 1 Goal (single active link)

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
Your fast-ship V1 wants simple UX:
- An Action either contributes to one Goal or none
- Avoid many-to-many UI and complexity

But we still want to preserve receipts correctly if linking changes over time.

---

## Decision
- V1 UI allows selecting **at most one Goal** for an Action (or no Goal).
- Under the hood, we may still store link history as intervals (recommended):
    - At any moment, an Action has 0..1 **active** Goal link.
    - If user changes the linked Goal:
        - close previous link interval
        - open new interval
- Goal receipts are computed from sessions that occurred during the link interval(s).

---

## Options considered
1) Put nullable `goalId` directly on Action row (simple, but rewrites history)
2) Interval link table, but enforce only one active goal per action (chosen)
3) Many-to-many linking (not V1)

---

## Consequences
### Positive
- Simple UI
- Correct historical receipts
- Future-proof if you later allow multi-goal

### Negative / tradeoffs
- Slightly more query complexity than a single field

### Follow-ups
- Add guardrails in use case: “close any active link before opening a new one”
- Add tests for relink behavior
