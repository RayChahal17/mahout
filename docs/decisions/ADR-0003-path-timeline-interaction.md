# ADR-0003: Timeline = session blocks + unused 10-minute cells (tap rules locked)

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
Path must feel premium in V1, but we must avoid building an over-complicated editor.
We want a timeline that looks great and is intuitive:
- worked time feels like “real blocks”
- unused time is easy to tap/select

---

## Decision
Timeline day view renders:
1) **Worked Sessions** as continuous filled blocks (not discretized)
2) **Unused time** as a grid of **10-minute cells**

Tap rules (locked):
- Tap a filled session block → show confirm dialog → **Delete**
- Tap unused cell(s) → multi-select contiguous range → “Log time” → creates a Session
- No recoloring or re-tagging session blocks in V1

Important: V1 is time-only; there are no check-offs.

---

## Options considered
1) Everything as 10-min cells (including sessions)
2) Sessions as blocks + unused as 10-min cells (chosen)
3) Full timeline editor (drag handles, resizing, recoloring)

---

## Consequences
### Positive
- Looks premium without huge engineering cost
- Selection is easy and discoverable
- Editing scope stays controlled

### Negative / tradeoffs
- Users can’t resize sessions from the timeline in V1
- “Delete only” for existing blocks may feel strict (acceptable for V1)

### Follow-ups
- Define selection visuals (selected cell state)
- Define how to resolve overlaps (manual log cannot overlap session)
