# Stage 4 — Active practice engine

## Scope of this increment

This increment adds a deterministic, offline-first domain engine for three active-recall modes:

- **Multiple choice**: the target term is shown and the learner selects its meaning. Distractors are drawn from the user's own local library; the question is unavailable when there are not enough distinct distractors.
- **Listen-and-type foundation**: the expected answer is the term and the prompt is its meaning. The UI can invoke the existing on-device speech/TTS layer separately; this engine itself does not claim to capture audio.
- **Contextual fill-in-the-blank**: the target term is blanked from the user's example sentence. The question is unavailable when the example is missing or does not contain the term.

Grading normalizes Unicode (NFKC), case, and repeated whitespace. It deliberately preserves punctuation and spelling; it does not silently accept near-miss spellings. Questions only use imported/user-created words; no bundled word bank or network source is required.

## Files

- `PracticeEngine.kt`: question models, question generation and deterministic answer grading.
- `PracticeEngineTest.kt`: tests for option uniqueness, insufficient distractors, typing normalization, contextual blanking, and incorrect/empty answers.

## Acceptance criteria for this increment

- All practice generation and grading logic is pure Kotlin and offline.
- A mode returns no question when required source data is missing.
- Multiple-choice options do not contain normalized duplicates.
- Incorrect answers never receive a correct result.
- UI integration, persisted per-mode analytics, matching-game UI, and on-device audio interaction remain follow-up work; this increment alone does not mark Stage 4 complete.
