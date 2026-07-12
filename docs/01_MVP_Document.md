# MVP Document — Thali (working title)
### AI-powered meal tracker built for real, mixed, home-cooked meals

---

## 1. The One-Line Pitch

Snap a photo of your meal. When the AI is confident, it logs instantly. When it isn't — especially on mixed dishes like curries, dal, and thalis — it asks you one or two sharp questions instead of silently guessing wrong, and it remembers your answer next time.

## 2. Why This, Why Now

The category leader (Cal AI) has grown into a ~$30M/year business on a genuinely broken core promise: photo-based calorie tracking. Its own reviewers report 25–50% variance on mixed dishes, a database too thin for regional/homemade food, and a "black box" model that can't be corrected or taught. That failure mode maps almost exactly onto Indian home cooking — dal, sabzi, roti, thalis — which is systematically the hardest case for every existing app in this category, global or local.

This isn't "let's build another calorie counter." This is: **identify a specific, well-documented failure mode of the market leader, and design a flow that structurally avoids it.**

## 3. MVP Goal

Ship a single-mode Android app that proves one thing works end-to-end and works well:

> A user can log a real Indian home-cooked meal by photo, get an accurate macro breakdown even when the dish is mixed/ambiguous, correct it in under 10 seconds when needed, and have the app remember that correction permanently.

If that loop works, everything else (workout mode, social, gamification, etc.) is additive. If it doesn't, nothing else matters.

## 4. In Scope for MVP

| Feature | Why it's in |
|---|---|
| Photo capture → AI food identification | Core value prop |
| Confidence-gated clarification chat | The actual differentiator — this is the "hack" |
| Manual food search + entry fallback | Needed for when photo isn't practical (packaged food, eating out) |
| Personal food memory (saved/corrected meals → 1-tap relog) | Directly solves "AI can't be taught" complaint about competitors |
| Daily macro/calorie dashboard | Table stakes, needed to make logging feel worthwhile |
| Basic onboarding (goals, dietary preference, daily targets) | Needed to compute targets and personalize |
| Local persistence, offline-tolerant logging | App must not lose a logged meal due to no signal at time of eating |

## 5. Explicitly Out of Scope for MVP

- **Workout logging / workout mode** — planned as V2, deliberately excluded now. Combining it into V1 would dilute the one thing this MVP needs to prove.
- **Voice-driven hands-free workout coach mode** (earphones-in, gym use case) — V3 concept, interesting but has its own separate technical surface (voice I/O, timers, background audio) and shouldn't block V1 shipping.
- Barcode scanning
- Social features, streaks, gamification
- Wearable / Health Connect integration
- Multi-user / cloud sync / accounts beyond local device
- Micronutrient tracking beyond calories + 4 macros (protein, carbs, fat, fiber)
- Monetization/paywall — not relevant for a resume/portfolio build

## 6. Success Criteria (how you know the MVP actually worked)

1. You (the real daily user) can log 3 meals a day for a week without wanting to quit — this is the actual bar, not a vanity metric.
2. On mixed dishes, the clarification flow produces a macro estimate you'd trust, not one you have to fight with.
3. A previously-corrected meal can be re-logged in one tap, with correct macros, on the second occurrence.
4. Cold-start to logged-meal (photo → confirmed log) takes under 20 seconds for a simple, confident case.

## 7. Suggested Build Order

1. Data models + local persistence (Room) for meals, food items, targets
2. Manual entry + dashboard first — gives you a working app fast, and it's the fallback path anyway
3. Photo capture + AI analysis integration (single API call, no chat yet) — get the "happy path" working
4. Confidence-gated branching + clarification chat flow — the hard, differentiating part
5. Personal food memory / correction persistence
6. Onboarding + polish + README + demo video

## 8. What Goes on the Resume / In the Interview

- "Identified a specific, documented failure mode (25–50% variance on mixed dishes) in a $30M-revenue competitor, and designed a confidence-gated conversational clarification flow to structurally address it."
- "Designed a personal-food-memory system so repeat corrections persist per user, addressing a top user complaint about AI black-box tracking."
- This gives you a genuine "why did you build it this way" story instead of "I followed a tutorial."
