# User Flows
### Thali (working title) — V1 Meal Mode

---

## Flow 1: Confident / Simple Meal (Happy Path)

```
Home Dashboard
   → Tap "Log Meal" (camera FAB)
   → Camera screen → capture photo
   → [Loading: "Analyzing your meal..."]
   → AI returns high-confidence result (e.g. "Banana, 1 medium — 96% confidence")
   → Result screen shows identified item(s) + macros, pre-filled
   → User taps "Confirm & Log" (can still edit quantity if they want)
   → Meal saved → Dashboard updates totals → toast/animation confirms
```
**Target time: under 20 seconds, minimal taps.**

---

## Flow 2: Ambiguous / Mixed Dish (The Core Differentiator)

```
Home Dashboard
   → Tap "Log Meal"
   → Camera screen → capture photo of a thali (dal, sabzi, rice, roti)
   → [Loading: "Analyzing your meal..."]
   → AI detects composite dish + low confidence on quantities
   → Routes to Clarification Chat (NOT silently logged as a guess)

   Clarification Chat:
   → App: "I can see dal, a roti, and rice on this plate. How many rotis?"
        → User taps a quick-select chip: [1] [2] [3] [4+]
   → App: "Got it. Is the dal more like a thin bowl or a thick, generous serving?"
        → User taps: [Thin bowl] [Regular bowl] [Generous bowl]
   → App: "Any oil, ghee, or butter added on top?"
        → User taps: [None] [A little] [Generous]
   → App reconciles answers → shows final structured breakdown
   → User reviews → taps "Confirm & Log" (or edits any single value inline)
   → Prompt: "Save this as 'My usual dal-roti' for faster logging next time?" → Yes/No
   → Meal saved → Dashboard updates
```
**Key UX rule: max 2–3 questions. If the flow feels like an interrogation, it's failed its own design goal.**

---

## Flow 3: Repeat Meal (Personal Food Memory)

```
Home Dashboard
   → Tap "Log Meal"
   → Camera screen → capture photo
   → AI identifies dish, matches an existing Food Memory entry with high similarity
   → Skips full clarification — shows: "Looks like your usual dal-roti (2 rotis, regular dal). Log this?"
   → User taps "Yes, log it" (one tap) — OR "Not quite, let me adjust" → drops into Flow 2's chat with pre-filled defaults
   → Meal saved
```
**This is the flow that should get faster the more the user uses the app — worth demoing this explicitly, it's the strongest interview talking point.**

---

## Flow 4: Manual Entry Fallback

```
Home Dashboard
   → Tap "Log Meal"
   → Instead of camera, tap "Search / Manual Entry"
   → Search bar → type food name → results list (bundled common foods + any saved food-memory entries)
   → Select item → adjust quantity → macros auto-calculate
   → Confirm & Log
```
Used when: AI is unavailable/offline, food isn't visual (e.g. a protein shake already mixed), or user just prefers typing.

---

## Flow 5: Editing a Past Log

```
Dashboard → History tab
   → Select a past meal
   → Meal Detail screen → tap "Edit"
   → Adjust any food item's quantity or macros
   → Save → Dashboard totals recalculate for that day
```

---

## Flow 6: Onboarding (First Launch Only)

```
Welcome screen
   → Basic stats: age, weight, height, activity level
   → Goal: lose / maintain / gain weight
   → Dietary preference: veg / non-veg / eggetarian / vegan (affects AI prompt context)
   → App computes suggested daily calorie + macro targets (editable)
   → User confirms targets → lands on Dashboard (empty state, prompts first log)
```

---

## Flow Priority for Build Order

1. Flow 6 (Onboarding) + Flow 4 (Manual Entry) — gets you a working, usable app fastest
2. Flow 1 (Happy Path AI) — proves the core AI integration works
3. Flow 2 (Clarification Chat) — the hard, differentiating part, build once Flow 1 is solid
4. Flow 3 (Food Memory) — layers on top of Flow 2 once corrections exist to learn from
5. Flow 5 (Editing) — needed for completeness but low-risk, build last
