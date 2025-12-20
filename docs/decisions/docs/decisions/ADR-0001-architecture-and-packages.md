# ADR-0001: MVVM + Hilt + feature-based packages (XML + ViewBinding)

**Status:** Accepted  
**Date:** 2025-12-20  
**Owner:** Mahout

---

## Context
Mahout will become large quickly. We need an architecture that:
- scales without turning into spaghetti
- is testable
- avoids refactors later

We also want feature-based packaging to keep files discoverable.

---

## Decision
- UI: XML + ViewBinding only
- Single Activity + Navigation Component + Fragments
- MVVM: Fragment → ViewModel → UseCases → Repos → Room
- Hilt DI across all layers
- **Feature-based packages** as the default structure:
    - `domain/<feature>/model`
    - `data/local/<feature>/entity` + `dao`
    - `data/repository/<feature>` (later)
    - `ui/<feature>` (already exists)

---

## Options considered
1) Layer-based packages (all entities together, all DAOs together)
2) Feature-based packages (Aim/Path/NorthStar separated)
3) Multi-module architecture

---

## Consequences
### Positive
- New features land in one obvious “zone”
- Less cross-feature accidental coupling
- Easier to onboard future contributors

### Negative / tradeoffs
- Some shared code needs a deliberate “common” location
- You must be disciplined about boundaries (don’t import UI into domain)

### Follow-ups
- Create shared packages only when truly shared:
    - `core/time`, `core/result`, `core/dispatchers`, etc.
