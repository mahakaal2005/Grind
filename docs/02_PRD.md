# Product Requirements Document (PRD)
### Thali (working title) — AI Meal & Fitness Companion

---

## 1. Vision

Build a nutrition and fitness companion that trusts you enough to ask instead of guessing, and remembers your answers so it never has to ask twice. Long-term, it grows into two purpose-built modes: a **meal-logging mode** for daily nutrition tracking, and a **hands-free workout mode** for gym sessions where the app talks you through your sets like a coach with earphones in.

V1 ships meal-logging only. Workout mode is a defined, designed-for future, not an afterthought bolted on later.

## 2. Problem Statement

Photo-based calorie tracking apps promised to remove the biggest barrier to consistent tracking — logging friction — and mostly succeeded on speed, but failed on trust:

- **Mixed/composite dishes break the model.** A camera cannot see cooking oil, butter, or sauce volume, and vision models trained mostly on Western stock food photography default to average portions instead of what's actually on the plate. Reported variance on curries, stir-fries, and mixed dishes runs 25–50%, almost always underestimating.
- **The database is thin for regional and homemade food.** Users searching for a food the AI got wrong often find it's simply not in the database, forcing estimation or abandoned logs.
- **The experience is a black box.** Users cannot correct the AI's mistakes in a way it remembers — every wrong guess repeats itself indefinitely.
- **Trust erosion compounds.** Paywalled core features, opaque pricing, and (in the case of the market leader) an actual data breach and app store removal for deceptive billing all point to a category where user trust is currently a competitive weakness, not a given.

None of this is a hypothesis — it's the direct, repeated finding across independent 2026 reviews of the category leader.

## 3. Target User

**Primary persona — "Daily Home Cook Tracker"**
- Eats mostly home-cooked, mixed meals (dal, sabzi, roti, rice, curries) — not packaged/branded food
- Has tried a calorie app before, quit within weeks because logging felt tedious or inaccurate
- Wants directional accuracy (is this a 500 or a 900 calorie meal), not lab-grade precision
- Values speed of logging above almost everything else

**Secondary persona (V2+) — "Gym-Goer"**
- Already tracks meals somewhat consistently
- Wants a frictionless way to log sets/reps/weight without pulling out a phone and typing mid-set
- Wears earphones during workouts, wants audio-first interaction

## 4. Competitive Landscape

| App | Strength | Weakness this product exploits |
|---|---|---|
| Cal AI | Fast photo logging, slick UI, cheapest AI-photo price point | Poor accuracy on mixed/regional dishes, no correction memory, paywalled core feature, trust issues (breach, billing removal) |
| MyFitnessPal | Huge food database, established | Cluttered, ad-heavy, manual-entry-heavy, no real AI-first flow |
| Cronometer | Deep micronutrient tracking (80–100+ nutrients) | Not photo-first, steep manual logging effort, not built for speed |
| Lose It! | Cleaner free tier, some AI photo recognition | "Good enough" accuracy only, same mixed-dish weakness as others |

**Positioning:** Not "more accurate AI" (you cannot out-train a foundation model as a student project) — **"smarter flow around an imperfect AI."** The differentiation is architectural/UX, not model quality, which is exactly the kind of thing a strong engineer can actually build and defend in an interview.

## 5. Goals

- **G1:** Logging a meal must feel faster or equal to competitors for simple/confident cases, and *more trustworthy* for ambiguous/mixed cases.
- **G2:** The app should get *better* for a specific user over time — corrections should never have to be repeated for the same dish.
- **G3:** V1 must be a complete, coherent, shippable product on its own — not a tech demo that only works in the happy path.

## 6. Non-Goals (V1)

- Competing on raw AI/vision model accuracy
- Being a comprehensive food database (defer to manual search/barcode as fallback, not the core loop)
- Multi-user/social/community features
- Monetization

## 7. Feature Set

### V1 — Meal Mode (this PRD's primary scope)
1. Onboarding: basic stats, goal (lose/maintain/gain), dietary preference, computed daily targets
2. Photo capture → AI food identification with per-item confidence scoring
3. Confidence-gated clarification chat — targeted follow-up questions only when needed
4. Manual search / text entry fallback
5. Personal food memory — confirmed/corrected meals saved for 1-tap relog
6. Daily dashboard — calories, macros, progress vs. target
7. Meal history / edit past logs

### V2 — Workout Mode (design now, build later)
- Exercise logging: sets, reps, weight, rest timer
- Progressive overload tracking, PR detection
- Mode switcher on the home screen: **Meal Mode ↔ Workout Mode**
- Workout Mode redesigns the interaction model entirely: minimal screen interaction, large tap targets, designed to be glanced at between sets rather than actively read

### V3 — Hands-Free Coach Mode (concept, not committed)
- Voice-first interaction layered on top of Workout Mode
- User has earphones in; app runs like background audio (similar mental model to a podcast or music app with foreground service)
- Flow: app announces the next exercise and target sets/reps → user performs the set → user says (or taps) "done, 40kg, 8 reps" → app logs it, announces rest duration, starts a timer → at rest end, app cues the next set audibly → repeats until the workout is complete
- Technically this implies: Speech-to-Text for logging sets, Text-to-Speech for cues, a foreground service to keep audio alive with the screen off, and a simple state machine (exercise → set → rest → next set / next exercise)
- Explicitly not scoped for build until V1 and V2 are solid — documented here so the architecture doesn't accidentally paint you into a corner

## 8. Success Metrics

Since this is a portfolio project, not a funded startup, metrics are personal-use + demonstrable, not growth-based:
- Personal daily usage for 2+ consecutive weeks without abandoning it
- Clarification flow triggers correctly on genuinely ambiguous meals and stays silent on confident ones (i.e., it isn't annoying)
- Zero data loss on logged meals across app restarts / offline moments

## 9. Risks

| Risk | Mitigation |
|---|---|
| Vision AI API costs during development/demo | Use a cheap-tier multimodal model (e.g. Gemini Flash tier) with request caps during dev |
| Vision model still misidentifies food badly even with clarification | Clarification chat should let user override the *identified dish itself*, not just quantities |
| Scope creep into workout mode before V1 is solid | This PRD's V1 scope is the actual MVP boundary — see MVP document |
| Over-engineering the architecture before any screen works end-to-end | Build the manual-entry + dashboard loop first (see MVP build order) |
