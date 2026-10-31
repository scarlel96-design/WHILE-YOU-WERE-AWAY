# V2-01 client validation protocol

## Purpose and claim boundary

Use this protocol after MAIN audits and adopts the support diff.
Record actual observations. Do not convert GameTest results into client or human results.
The evidence tool checks report structure and local file hashes. It does not independently observe Minecraft.
Treat the historical Computer Use kernel-assets error as a repository-independent blocker unless new evidence proves a repository cause.
Use direct manual play when Computer Use is unavailable. Do not modify Codex internals to complete this protocol.

## Prepare the isolated client

1. Record the adopted main branch and full implementation SHA.
2. Verify the selected JAR with `Get-FileHash -Algorithm SHA256 -LiteralPath <JAR>`.
3. Create a separate Minecraft profile and save directory.
4. Preserve the user's normal profile, worlds, and global volume.
5. Set all sound categories to zero before agent-launched testing.
6. Select the right secondary `DISPLAY1` and verify the actual window bounds.
7. Create a new survival world for the primary acceptance path.
8. Copy `tools/validation/v2-01-client-session-template.json` into the evidence directory as `client-session.json`.
9. Set `evidence_mode` to `REAL_CLIENT` only for actual client observations.
10. Record `tester_kind` as `HUMAN` or `AUTOMATED` for each executed scenario.

Keep muted visual verification separate from human audio verification.
Record `audio_readability=NOT_TESTED` when no person heard the scene.
Use a separate explicitly selected isolated sound-on session for human audio review.

## Primary path and shortcut rules

Play Opening, Overworld, survival, Yeoul, Return Network, NoEvent, Returner, recovery, and revisit in order.
Do not use debug checkpoints, fabricated story facts, creative mode, or teleport commands in this primary path.
Collect resources, craft basic tools, make shelter, and resume normal play between authored events.
Record where discovery stalls. Do not force an event to conceal a progression or readability defect.
Use isolated copies for secondary negative cases. Label any fixture arrangement or environment manipulation.
Never use a normal player world for a corruption or destructive fixture.

## Scenario matrix

| Scenario | Start condition and action | Expected invariant and evidence |
| --- | --- | --- |
| `new_world_boot` | Start the selected JAR in a fresh survival world. | World loads with the recorded build. Capture profile, world ID, and startup log. |
| `opening_transition` | Play the first-person fragment through its natural exit. | Overworld commit stays monotonic. Capture transition and fresh-process reopening. |
| `yeoul_first_contact` | Reach Yeoul through ordinary exploration. | Existing Yeoul participates. Record UUID, instance, generation, and first-contact context. |
| `return_network_normal_path` | Complete Return-Light, then investigate the relay naturally. | Checkpoints 0–6 progress without duplicate facts. Record signal, interaction, shared experience, and aftermath. |
| `returner_unobserved_completion` | Leave the figure unobserved while allowing its encounter to finish. | Termination does not create `player_observed_returner` or the resolution notice/music. Record persisted facts separately from absence of a caption. |
| `returner_positive_observation` | Face a visible Returner within 32 blocks for sustained exposure. | Observation fact appears and survives reload. Record sightline, distance, actor identity, and saved facts. |
| `observation_interrupted` | Interrupt a partial exposure with a view change or disconnect. | Partial exposure does not combine across interruption. Record before/after facts and fresh exposure. Exact tick boundaries also require automated evidence. |
| `occluded_returner` | Place an opaque obstruction in a secondary fixture's sightline. | Proximity alone does not grant observation. Record the obstruction and unchanged fact. |
| `warning_ignored_crisis` | Continue blocking the route after a readable warning in an isolated copy. | A reaction window precedes bounded crisis. Record warning, response interval, escape, and cleanup. |
| `recovery` | Clear the route or escape the crisis. | Threat cleanup permits ordinary Minecraft play. Record target/audio cleanup and resumed activity. |
| `station_revisit_observed` | Revisit the original station after an observed encounter. | Physical-trace text appears without forced interpretation, replay, or new durable facts. Repeat the interaction. |
| `station_revisit_unobserved` | Revisit after unobserved completion or an older save without the fact. | Neutral text appears. Record absence of invented encounter knowledge and repeated-interaction mutations. |
| `yeoul_post_event_dialogue` | Speak to Yeoul after Return Network completion. Repeat while crouching. | Ordinary dialogue remains available. Crouching selects context dialogue. Neither interaction creates new facts. |
| `yeoul_daily_route` | Observe work and rest after event completion. | Resident movement resumes outside the existing brief relay glance. Record actual navigation and stable participant identity. |
| `survival_continuity` | Mine, craft, build, explore, sleep, and return to normal play. | Story staging preserves survival freedom and player-built geometry. Record activities and any interruption. |

The required client gate uses twelve MUST scenarios defined by the directive.
The three additional scenarios remain visible in the summary even when not executed.
Do not interpret optional status as evidence that the scenario passed.

## Save and restart observations

1. Save and reopen near the Opening-to-Overworld transition.
2. Save and reopen after Return Network intervention or response.
3. Disconnect during a partial Returner exposure in a secondary copy.
4. Save and reopen after confirmed observation and after encounter termination.
5. Compare checkpoint, facts, UUID, instance, generation, and completion receipts.
6. Record the old and new process identifiers when a process restart is claimed.

Never infer saved state from a screen caption alone.
Keep developer reads of NBT separate from the primary unassisted player actions.
Keep death, dimension, chunk, and corruption checks risk-based when their production code is unchanged.

## Evidence files and metadata

Use names such as `session-01_returner-positive_01.png` and `session-01_client.log`.
Store references beneath the directory containing `client-session.json`.
Use local file paths in `evidence_reference`. The validator rejects missing files and paths outside that directory.
Record full `build_sha`, `jar_sha256`, world/profile identity, start, observed, expected, tester, and notes.
Use one build/JAR pair per summary. Split different builds into separate sessions.
Record human readability, fear pacing, and audio readability independently.
Set every unexecuted result to `NOT_TESTED`; leave its human fields `NOT_TESTED`.

## Run the strict summary

Run from the repository root:

```powershell
python tools/validation/v2_01_client_evidence.py evidence/v2-playable-core/client-session.json evidence/v2-playable-core/client-summary.json
```

Exit 0 means all reported MUST client scenarios passed structural validation.
Exit 1 means the valid report has a PARTIAL or FAIL client gate.
Exit 2 means invalid input. The tool preserves an existing output on invalid input.
The tool writes a valid summary through a temporary file and atomic replacement.
Missing scenarios become `NOT_TESTED`. Duplicate scenarios and mixed builds are rejected.
Synthetic reports never produce a real-client gate PASS.
Automated records never grant human ratings.

## Acceptance

Set a scenario to PASS only when its observation and references support the expected invariant.
Set FAIL for an observed contradiction. Record reproduction and failure evidence.
Set BLOCKED when the environment prevents execution. Name the external blocker.
Set NOT_TESTED when the scenario was not executed.
Keep V2-01 PARTIAL until MAIN completes actual client acceptance and reviews remaining gameplay issues.
Do not start V2-02 or promote 0.3.2 from a validator summary alone.
