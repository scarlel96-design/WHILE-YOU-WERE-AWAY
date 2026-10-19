# V2-01 playable core: Opening implementation checkpoint

## Current

- Version: `0.3.1-dev.1`.
- Branch: `work/v2-playable-core`.
- Input commit: `87917ce518b5dbdd0d9631e600033f5060523b0a`.
- V2-01: **PARTIAL**.
- Save format: 5. Actor schema: optional 1. Return Network schema: 1.
- Art revision: 2. Caveman: HOLD. 0.3.2: not promoted.

## Implemented game content

- A new player enters a first-person memory route before Overworld survival.
- The player moves through a damaged service corridor, uses a lever, and crosses two temporal gaps.
- Short environmental text and a quiet bell establish rail, signal, and missing-memory cues.
- The route returns the player to the saved Overworld position and restores ordinary Minecraft control.
- A settled Yeoul resident chooses a light-side destination during daytime and the home destination at night. This is a minimal life loop, not the complete V2 living-NPC goal.

## Implemented infrastructure

- `OpeningStage` defines monotonic semantic steps: `NOT_STARTED`, `STARTED`, `TRANSITION`, `OVERWORLD_COMMITTED`, `COMPLETED`.
- `NarrativeData.Entry` stores opening stage and the original Overworld position and view.
- `NarrativeData.commitOpening` writes the next stage synchronously. It verifies source hash, reads the file back, and publishes the live state only after a durable match. A write failure activates the existing write guard.
- The loader treats missing opening fields in existing player records as `COMPLETED`. It rejects malformed explicit opening fields.
- `OpeningEvents` builds the small route in a dedicated dimension and resumes from durable stage after reconnect.
- `StoryEvents` waits for `COMPLETED` before normal discovery progression.
- Save format 5 and all existing Actor and Return Network schemas remain unchanged.

## Validation work

- Baseline `compileJava test`: exit 0, `BUILD SUCCESSFUL in 6m 20s`.
- Modified `compileJava test`: exit 0, `BUILD SUCCESSFUL in 30s`.
- Final `build`: exit 0, `PASS core assertions=176857`, `BUILD SUCCESSFUL in 10s`.
- Final `runGameTestServer`: exit 0; 111 tests started, `BUILD SUCCESSFUL in 47s`.
- Four **scripted real Minecraft clients** ran muted on the right secondary `DISPLAY1`. Fresh completed Opening. Reopen retained `COMPLETED`. Cut stopped at x=29 after the first gap. Resume completed in a new process. These checks do not establish human scene readability.
- The transaction rollback restored a separate copy to baseline SHA-256 `259ba59126764362ad84f1f51d85c441f9beb2ec6c5f206fbf176a4a65d920bf`. The modified file remains changed.

## Failure history

- The first reconnect route attempted to repeat the first temporal gap after the player had already crossed it. The trigger covered positions past the intended gap. `OpeningEvents.advance` now limits each gap to its local x interval. The Cut/Resume process pair then completed with two gap transitions.
- A local Gradle wrapper attempt hit a permissions error on the shared Gradle lock file. A permitted retry completed. This was a tooling failure, not a gameplay test failure.
- Local Git remote lookup reported `getaddrinfo() thread failed to start` while the host had several Java processes. GitHub API independently confirmed the remote branch at the input commit. Remote upload remains a separate gate until a successful push and readback.

## Remaining V2-01 work

- Test Opening with an unassisted human playthrough. Assess comprehension, pacing, sound, and reduced-motion needs.
- Expand Yeoul's routine to include rest, ordinary interaction, and player-independent work.
- Connect ordinary survival and natural discovery to the existing Return Network event.
- Implement the NoEvent conditioning and the first actual Returner presence, warning, fair counterplay, optional crisis, disengage, and recovery.
- Add a natural revisit and first reinterpretation.
- Run the V2-01 milestone gate from a new world after the full sequence is playable.

## Next exact start

Open `src/main/java/io/github/whileaway/NpcEvents.java` and the current Yeoul residence state. Implement an ordinary, non-story interaction and a clear work/rest loop. Then connect the survival return route to the first NoEvent eligibility. Keep persistent transitions within the existing fail-closed storage boundary.

## Evidence and publication

- Client logs: `evidence/v2-opening/client/{Fresh,Reopen,Cut,Resume}.txt`.
- Transaction evidence: `evidence/v2-opening/transaction/`.
- GitHub upload status, branch, commit SHA, and package hashes must be recorded after remote readback.
