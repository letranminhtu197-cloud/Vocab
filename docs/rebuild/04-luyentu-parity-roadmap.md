# Vocab — Luyện Từ 1.4.0 parity roadmap

This is a behavioral reconstruction plan based on the previously inspected Luyện Từ 1.4.0 APK. It is not a claim that the original Flutter source was recovered. Only observable/reasonably inferable features are included; unavailable implementation details must be verified from the APK or screenshots before being treated as exact.

## Product constraints that take precedence over parity

- 100% offline after installation; no INTERNET permission, account, cloud sync, Firebase, analytics, ads, billing, subscription, remote leaderboard, server chat, or server classes.
- No preloaded vocabulary library. The learner imports or enters their own words. A small removable sample set may exist only as an explicit test action.
- Rebuild original behavior in native Kotlin/Compose with original assets only when licensed or user-provided. Do not copy proprietary assets/code from the APK.

## Feature parity matrix

| Luyện Từ area observed in reference | Vocab target | Status |
|---|---|---|
| Vocabulary library and categories | Local word library, category/topic organization, search, edit/delete, CSV import/export | Partial; Stage 3 foundation exists |
| Flashcard practice | Card reveal and self-rating integrated with SRS | Existing lesson flow; needs visual/flow parity pass |
| Listening | Offline TTS where supported; listen then type; explicit fallback when no offline voice is installed | Engine foundation only; UI/audio integration pending |
| Matching | Local pair-matching game with clear completion and no duplicate scoring | Not implemented |
| Typing | Exact answer checking and clear retry/answer feedback | Active practice hub integrated; needs CI verification |
| Quiz | Distractors drawn from local word library; robust insufficient-data state | Active practice hub integrated; needs CI verification |
| Mixed practice | Session planner that mixes eligible exercise types without generating impossible questions | Not implemented |
| Sentence/context exercise | Blank the target word in user-authored example; skip invalid examples | Active practice hub integrated; needs CI verification |
| Spaced repetition | Persist ratings, review dates, lapses and due queue; avoid duplicate rating writes | Existing SRS connected; needs end-to-end tests |
| Learning path / roadmap | Topic-level progress and start/resume sessions | Partial existing feature; needs parity review |
| Progress/statistics | Accuracy, daily activity, due/weak/mastered distribution and session summary | Partial existing local statistics |
| Reminders/widgets | Optional local scheduled reminders; no push server | Needs audit and implementation if appropriate |
| Profile, shop, subscriptions, classes, chat, online leaderboard/shared streak | Remove or replace only with local profile/settings and device-local stats | Intentionally excluded because online/backend dependent |
| Offline import/export/backup | CSV field mapping, validation report, safe backup/restore | Partial; column mapping and real DB integration tests pending |

## Stage 4 delivery sequence

1. Interactive practice hub and mode selection — implemented in this increment; CI result still needs confirmation.
2. Complete question/session flow: next/back or skip behavior, no duplicate answer scoring, robust empty/insufficient-data states, session summary.
3. Flashcard and SRS session parity: clear Again/Hard/Good/Easy semantics and persisted review state.
4. Matching game and mixed practice.
5. Offline speech: TTS availability check, audio controls, graceful no-voice fallback; do not promise audio capture if the device lacks offline speech recognition.
6. Learning-path, progress, reminders and settings parity review.
7. UI polish, accessibility, rotation/process recreation checks, Room instrumentation tests, APK build, manifest permission audit and device smoke test.

## Acceptance gates

A feature is not considered complete merely because its domain logic exists. It must be connected to UI, persist state correctly, have relevant tests, pass CI, preserve the offline manifest constraint, and be smoke-tested on an Android device before claiming device-level validation.
