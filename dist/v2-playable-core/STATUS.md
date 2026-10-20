# V2-01 playable core: survival and first-presence increment

## Current

- Version: `0.3.1-dev.1`.
- Branch: `work/v2-playable-core`.
- Input source commit: `c6b20c5fa591267fdc38f59cab543446174b8e51`.
- Local history was merged with remote `d2320fcc8d5ffc5c8b987ac7b6c4e40993505ca8` before publication.
- V2-01: **PARTIAL**. Campaign: **PARTIAL**. Return Network: previously sealed.
- Save format: 5. Actor schema: optional 1. Return Network schema: 1.
- Art revision: 2. Caveman: HOLD. Version 0.3.2: not promoted.

## Implemented game content

- Yeoul works near the light during the day, returns home, and rests. A mundane interaction leaves story progress unchanged.
- Return Network completion now precedes the first Returner cues. The first two cues are distant sounds before manifestation.
- The Returner walks toward the station. A player who blocks its narrow route receives a warning and a reaction interval. Moving clear lets it disengage. Continued obstruction can cause a short pursuit.
- The original station shows one new clue when revisited after the encounter. This interaction does not replay the encounter.
- First-login quest direction was removed to leave normal Minecraft survival in the player's control.

## Infrastructure and code intent

- `NpcEvents.update` checks the fixed home when it evaluates the NPC environment. The changing work destination no longer masquerades as residence damage.
- `StoryNpc.tryOrdinaryTalk` provides a non-story response for ordinary encounters.
- `CityAccess.canEnter` allows Return Network access before the Wayfarer encounter is complete.
- `StoryEvents.tick` gates the first threat cues on durable Return Network completion.
- `Director.choose` schedules two ambient cues before manifestation at progress stage 3.
- `Wayfarer.blocksReturnRoute` uses route geometry, not proximity alone. The actor has no teleport fallback or forced chunk load.
- `StationBlock.useWithoutItem` serves the revisit clue at the saved station after completion.
- Persistent checkpoint meanings, save format, Actor identity, and Return Network schema did not change.

## Validation

- `gradlew.bat --offline --no-daemon test compileJava`: exit 0; `BUILD SUCCESSFUL in 49s`.
- `gradlew.bat --offline --no-daemon runGameTestServer build`: exit 0; `All 113 required tests passed :)`; `PASS core assertions=176858`; `BUILD SUCCESSFUL in 1m 25s`.
- `git diff --check`: exit 0, no output.
- New real-client tests for this increment: **NOT TESTED**. Earlier Opening and Return Network client evidence does not verify this new flow.
- Human readability, player observation, optional crisis, recovery, and natural revisit: **PARTIAL / NOT TESTED** as applicable.

## Failure history

- The first Gradle attempt failed to open its daemon socket. A permitted serial rerun passed. This was an execution environment failure.
- Git Bash failed before executing `ROLLBACK.sh` with `NtCreateDirectoryObject` status `0xC0000022`. A PowerShell copy test restored a separate probe to the baseline hash. The shell script itself remains unexecuted on this host.
- The remote branch and local branch had different commits with the same source tree. A normal merge retained both histories and the current changes.

## Transaction and preservation

- The input package at `dist/v2-opening/` remains unchanged.
- `transaction/BASELINE_FILE` preserves the original station source hash.
- `transaction/MODIFIED_FILE` retains the changed source. `transaction/ROLLBACK_PROBE` was restored to baseline.
- `transaction/DIFF_FILE`, `transaction/VERIFICATION.txt`, and `transaction/ROLLBACK.sh` record the change and reversal procedure.
- Rollback restores a source-file copy or the prior package. It does not downgrade a save or roll back a world.

## Remaining work and exact next start

1. Run the first unassisted, muted real-client V2-01 pass on right secondary `DISPLAY1` from a fresh world. Record Opening, normal survival, Yeoul, Return Network, NoEvent, Returner, recovery, and revisit separately.
2. Verify a player actually perceives the Returner before recording an observation fact. Current completion can occur after route arrival or the 2,400-tick lifetime without proved observation.
3. Check visual and audio warning readability. Confirm that correct route clearance avoids crisis.
4. Implement or adjust only the gameplay that fails that pass. Then run the full V2-01 milestone gate.

## Publication

- Publish this increment and all relevant source, logs, diff, package, and status files to `work/v2-playable-core`.
- Record the final commit and remote readback in the completion report. Do not mark V2-01 complete from automated tests alone.
