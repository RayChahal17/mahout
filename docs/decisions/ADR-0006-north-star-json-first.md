# ADR-0006: North Star uses strict JSON models + fallbacks (never crash)

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
North Star is the “soul” of Mahout, but AI output is unpredictable.
If the app crashes or renders nonsense, trust is destroyed.

---

## Decision
- All AI outputs that drive UI must be parsed into validated models.
- JSON schema has versioning (e.g., `schemaVersion: 1`).
- If parsing fails:
    - show a fallback card (“I couldn’t format that yet”)
    - allow user to retry
    - never crash the screen

---

## Options considered
1) Render raw text always (less structure)
2) Mixed raw text + partial JSON (hard to reason about)
3) Strict JSON-first + fallback UI (chosen)

---

## Consequences
### Positive
- Stability and trust
- UI is deterministic
- Easier to iterate on “scripts” later

### Negative / tradeoffs
- Need schema discipline and migrations (schema versions)

### Follow-ups
- Define the V1 schemas as Kotlin data classes
- Add strict parsing + safe error mapping
