# Screen Documentation
### Thali (working title) — V1 Meal Mode

For each screen: purpose, key elements, and states to handle. This is meant to be a build checklist, not a visual mockup — you own the actual UI design.

---

## 1. Onboarding — Basic Info
**Purpose:** Collect stats needed to compute targets.
**Elements:** Age, weight, height, sex, activity level inputs; progress indicator (step 1 of 3).
**States:** Validation errors (empty/invalid fields) inline, not blocking navigation until fixed.

## 2. Onboarding — Goal & Preference
**Purpose:** Capture goal type and dietary preference (used both for target calc and AI prompt context).
**Elements:** Goal selector (lose/maintain/gain, single-select), dietary preference selector (single-select), optional region preference (e.g. "mostly North Indian / South Indian / mixed" — improves AI prompt relevance).
**States:** N/A, straightforward selection screen.

## 3. Onboarding — Target Confirmation
**Purpose:** Show computed daily targets, let user adjust before committing.
**Elements:** Calorie target (editable), macro split (protein/carbs/fat, editable), "Looks good" CTA.
**States:** Recalculate live if user edits any value manually.

## 4. Home Dashboard
**Purpose:** Daily summary + entry point to logging. This is the screen opened most often — should load instantly from local cache.
**Elements:**
- Today's calories consumed vs. target (ring or bar)
- Macro breakdown (protein/carbs/fat vs. target)
- Recent meals logged today (list, tap to view/edit)
- Prominent "Log Meal" FAB (camera icon)
- Secondary entry: "Search / Manual Entry"
**States:** Empty state (no meals logged yet today — friendly prompt), loading state on first cold start, error state if local DB read fails (rare, but handle it).

## 5. Camera Capture
**Purpose:** Capture or select a meal photo.
**Elements:** Live camera preview (CameraX), shutter button, gallery-picker alternative, flash toggle.
**States:** Permission-denied state (explain why camera is needed, link to settings), captured-photo preview with retake/confirm before upload.

## 6. Analyzing (Loading/Transition State)
**Purpose:** Bridge between capture and result — this is a real screen, not just a spinner, because AI latency is a few seconds and blank spinners kill trust.
**Elements:** Progress indicator, short reassuring copy ("Looking at your plate..."), cancel option.
**States:** Timeout/error state → offer retry or fall through to manual entry.

## 7. AI Result — Confident Case
**Purpose:** Show identified food(s) + macros for direct confirmation (Flow 1).
**Elements:** Item(s) with editable quantity, macro breakdown (calories/protein/carbs/fat), confidence indicator (subtle, not alarming — e.g. a small "AI identified" tag), "Confirm & Log" CTA, "This isn't right" link → drops to manual correction or re-triggers clarification.
**States:** Multi-item meals show each item individually, editable independently.

## 8. Clarification Chat
**Purpose:** Core differentiator screen (Flow 2) — resolve ambiguity through targeted Q&A.
**Elements:** Chat-style message list (app messages left, user responses right), quick-reply chip row under each app question (primary interaction), free-text input as fallback, progress indicator ("Question 2 of 3").
**States:** 
- Mid-conversation (chips active, waiting for input)
- Reconciling (brief loading after last answer, before showing final breakdown)
- Should never feel like an open-ended chatbot — bounded, predictable, always converges to a result

## 9. Reconciled Result / Confirm Log
**Purpose:** Final review before saving — same shape whether reached via Flow 1 or Flow 2.
**Elements:** Full breakdown, per-item editable fields, "Save as usual meal?" toggle (feeds Food Memory), "Confirm & Log" CTA.
**States:** Inline edit mode per field.

## 10. Manual Search / Entry
**Purpose:** Fallback logging path (Flow 4).
**Elements:** Search bar, results list (bundled common foods + personal Food Memory entries prioritized at top), quantity stepper on selection, macro preview before confirming.
**States:** No-results state (offer fully custom manual macro entry as last resort), loading state for any remote lookup.

## 11. Meal History
**Purpose:** Browse past logs (Flow 5 entry point).
**Elements:** Date-grouped list of meals, daily subtotal per date, tap-through to detail.
**States:** Empty state for a day with no logs, pagination/lazy-load for long history.

## 12. Meal Detail / Edit
**Purpose:** View or amend a specific past log.
**Elements:** Same structure as Reconciled Result screen, but editable and persisted on save, "Delete meal" option.
**States:** Confirm-before-delete dialog.

## 13. Profile / Settings
**Purpose:** Adjust targets, dietary preference, manage Food Memory entries.
**Elements:** Editable targets, dietary preference, a list of saved Food Memory entries (with ability to delete/edit stale ones — important, otherwise memory becomes clutter over time).
**States:** N/A.

---

## Screens Explicitly Deferred (V2/V3, do not build now)

- Mode switcher (Meal Mode / Workout Mode)
- Workout logging screens (exercise picker, set/rep entry, rest timer)
- Hands-free voice coach interaction screen (largely non-visual by design — mostly audio + a minimal now-playing-style screen)
