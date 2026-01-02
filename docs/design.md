# Mahout UI Design System (Safe Copy)

## Non‑negotiable rule (from you)

* **We will not create lots of extra color/theme/style files.**
* **Preferred minimal resource set** going forward:

    * `app/src/main/res/values/colors.xml` (+ optional `values-night/colors.xml`)
    * `app/src/main/res/values/themes.xml`
    * `app/src/main/res/values/dimens.xml` (and *only if needed* one extra `mahout_dimens.xml`)
    * Drawables/styles: **only when absolutely required**, otherwise reuse existing.
* When we need a new token:

    * Add it to **existing files above**, don’t create a new “mahout_*.xml” values file.

---

## Tech stack / libraries selected (the “packages”)

These are from `gradle/libs.versions.toml` (your repo’s source of truth).

### Build + language

| What                  | Selected |
| --------------------- | -------- |
| Android Gradle Plugin | `8.13.2` |
| Kotlin                | `2.0.21` |

### Core app libraries

| Alias (libs.*)                      | Maven coord                                        |  Version | Why it exists                                               |
| ----------------------------------- | -------------------------------------------------- | -------: | ----------------------------------------------------------- |
| `androidx-core-ktx`                 | `androidx.core:core-ktx`                           | `1.17.0` | Kotlin extensions for core Android APIs.                    |
| `androidx-appcompat`                | `androidx.appcompat:appcompat`                     |  `1.7.1` | AppCompat support; base compatibility & themes.             |
| `material`                          | `com.google.android.material:material`             | `1.13.0` | Material Components (Material3 widgets used throughout UI). |
| `androidx-activity`                 | `androidx.activity:activity-ktx`                   | `1.12.2` | Activity APIs + ActivityResult contracts.                   |
| `androidx-constraintlayout`         | `androidx.constraintlayout:constraintlayout`       |  `2.2.1` | ConstraintLayout for premium XML layouts.                   |
| `androidx-navigation-fragment-ktx`  | `androidx.navigation:navigation-fragment-ktx`      |  `2.7.7` | Navigation Component (Fragment KTX).                        |
| `androidx-navigation-ui-ktx`        | `androidx.navigation:navigation-ui-ktx`            |  `2.7.7` | Navigation UI helpers (BottomNav/Toolbar).                  |
| `androidx-lifecycle-runtime-ktx`    | `androidx.lifecycle:lifecycle-runtime-ktx`         |  `2.9.4` | `repeatOnLifecycle`, lifecycle‑aware coroutines.            |
| `androidx-lifecycle-viewmodel-ktx`  | `androidx.lifecycle:lifecycle-viewmodel-ktx`       |  `2.9.4` | ViewModel + coroutine helpers.                              |
| `kotlinx-coroutines-android`        | `org.jetbrains.kotlinx:kotlinx-coroutines-android` |  `1.8.1` | Coroutines + main dispatcher.                               |
| `hilt-android`                      | `com.google.dagger:hilt-android`                   | `2.51.1` | Dependency injection (Hilt).                                |
| `hilt-compiler`                     | `com.google.dagger:hilt-compiler`                  | `2.51.1` | Hilt annotation processor.                                  |
| `androidx-hilt-navigation-fragment` | `androidx.hilt:hilt-navigation-fragment`           |  `1.2.0` | Hilt + Navigation integration.                              |
| `androidx-room-runtime`             | `androidx.room:room-runtime`                       |  `2.6.1` | Room database runtime.                                      |
| `androidx-room-ktx`                 | `androidx.room:room-ktx`                           |  `2.6.1` | Room coroutines/KTX.                                        |
| `androidx-room-compiler`            | `androidx.room:room-compiler`                      |  `2.6.1` | Room annotation processor.                                  |

### Test libs (kept standard)

| Alias                    | Maven coord                            |  Version |
| ------------------------ | -------------------------------------- | -------: |
| `junit`                  | `junit:junit`                          | `4.13.2` |
| `androidx-junit`         | `androidx.test.ext:junit`              |  `1.2.1` |
| `androidx-espresso-core` | `androidx.test.espresso:espresso-core` |  `3.6.1` |

---

## Theme selection (global styling)

* **Theme parent:** `Theme.Material3.DayNight.NoActionBar`
* Your project theme file: `app/src/main/res/values/themes.xml`

### Key theme mappings (what the app uses)

* `colorPrimary` → `@color/mahout_accent`
* `colorOnPrimary` → `@color/mahout_on_accent`
* `android:colorBackground` → `@color/mahout_bg`
* `colorSurface` → `@color/mahout_surface`
* `colorOnSurface` → `@color/mahout_ink`

---

## Color system (source of truth)

**Primary files:**

* `app/src/main/res/values/colors.xml`
* `app/src/main/res/values-night/colors.xml`
* `app/src/main/res/values/mahout_chip_colors.xml` (only 3 chip aliases)

### Semantic palette (how we use it)

* **Background (paper):** `mahout_bg`
* **Surfaces (cards/pills):** `mahout_surface`, `mahout_surface_alt`
* **Text/ink:** `mahout_ink` (main), `mahout_text_secondary`, `mahout_text_primary/secondary/tertiary`
* **Hairlines/outlines:** `mahout_outline_soft` (default), `mahout_outline` (stronger)
* **Accent (gold):** `mahout_accent` + `mahout_on_accent`
* **Soft category tints (for action color-coding):** `mahout_purple_*`, `mahout_blue_*`, `mahout_gold_*`
* **Premium shadow tokens:** `premium_shadow`, `premium_shadow_light`

### Full token list (Mahout `mahout_*` colors)

> Columns: Token name → Light value → Dark value → “Used in res refs” (rough count from XML).

"+color_table+"

*(If you add more tokens later, append them here + to `values/colors.xml` + `values-night/colors.xml` only.)*

---

## Dimension tokens (premium spacing + corner radii)

Source: `app/src/main/res/values/dimens.xml` and `values/mahout_dimens.xml`.

"+dimen_table+"

---

## Component styling (so we stay consistent)

### Global

* Background uses `mahout_bg` (paper look).
* Surfaces/cards use `mahout_surface` with **soft outline** `mahout_outline_soft`.
* Text hierarchy:

    * Headings: `mahout_text_primary`
    * Secondary/meta: `mahout_text_secondary`
    * Tertiary: `mahout_text_tertiary`

### Bottom navigation (target “premium” spec)

* Container is a pill surface (`mahout_surface`) with soft outline (`mahout_outline_soft`) and shadow (`premium_shadow`).
* Icon/txt color should always contrast the surface: use `mahout_ink` (selected), `mahout_text_secondary` (unselected).
* Center Elephant is a separate floating circle using `mahout_accent` with `mahout_on_accent` icon.

### Timer tray (when running)

* Tray should feel “primary” using surface + stronger shadow.
* Only fade **action cards** behind it (not the timeline). Fading targets should be explicit views only.

---

## Safety notes (so we don’t break builds again)

* When you reference framework attributes, always prefix with `android:` (example: `android:colorBackground`).
* Don’t introduce custom attrs unless we truly need them (attrs are easy to break resource linking).
* Avoid adding new style overlays unless there is a clear payoff and we can keep it in existing `themes.xml` / `styles.xml`.
