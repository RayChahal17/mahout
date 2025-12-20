# ADR-0007: Fast-ship cuts for V1 (time-only, no backup, no growth)

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
V1 must ship quickly while preserving the “real Mahout feel”.
Some features are real engineering work that would delay shipping.

---

## Decision
For V1 fast-ship:
- **Time-based Actions only**
    - No checklist/check-off actions
- **No backup/export/import**
    - Add Settings warning: “Beta: Data may be lost; no backup yet.”
- **No growth/viral/invites/share cards**
    - No “invite loops”, no share artifacts, no referral mechanics

These are deferred to Beta/V2.

---

## Options considered
1) Ship everything in the original V1 spec (slower)
2) Fast-ship cuts but preserve Path + North Star quality (chosen)
3) Cut Path/North Star quality to ship faster (rejected)

---

## Consequences
### Positive
- Faster shipping
- Less surface area for bugs
- Focus on the “wow” tabs

### Negative / tradeoffs
- Users can lose data (must be communicated)
- Some original spec items move to Beta

### Follow-ups
- Keep a Beta backlog doc later (separate from V1 scope lock)
