- Remove all bottom navigation assets: menu, tint selectors, nav item backgrounds, nav-specific styles/colors, and FAB hook.
- Simplify `activity_main.xml` to only a `NavHostFragment` and toolbar (no pill card, no bottom nav, no FAB).
- Trim `nav_graph.xml` to a single-stack start destination (Aim) with existing non-tab flows (e.g., settings).
- Simplify `MainActivity` to basic NavigationUI (toolbar back only) with no bottom-nav wiring.
- Clean themes/colors to drop bottom-nav entries and unused nav tints.
- Build/check to ensure resources link and app launches with the single-stack flow.

---

title: Reset Single-Stack Navigation

---

- Remove all bottom navigation assets: menu, tint selectors, nav item backgrounds, nav-specific styles/colors, and FAB hook.
- Simplify `activity_main.xml` to only a `NavHostFragment` and toolbar (no pill card, no bottom nav, no FAB).
- Trim `nav_graph.xml` to a single-stack start destination (Aim) with existing non-tab flows (e.g., settings).
- Simplify `MainActivity` to basic NavigationUI (toolbar back only) with no bottom-nav wiring.
- Clean themes/colors to drop bottom-nav entries and unused nav tints.
- Build/check to ensure resources link and app launches with the single-stack flow.

---

title: Reset Single-Stack Navigation

---

- Remove all bottom navigation assets: menu, tint selectors, nav item backgrounds, nav-specific styles/colors, and FAB hook.
- Simplify `activity_main.xml` to only a `NavHostFragment` and toolbar (no pill card, no bottom nav, no FAB).
- Trim `nav_graph.xml` to a single-stack start destination (Aim) with existing non-tab flows (e.g., settings).
- Simplify `MainActivity` to basic NavigationUI (toolbar back only) with no bottom-nav wiring.
- Clean themes/colors to drop bottom-nav entries and unused nav tints.