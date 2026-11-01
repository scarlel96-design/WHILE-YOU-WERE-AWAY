# GPT SUPPORT REPORT — V2-01 / continuation 3

## Baseline and ownership

- Main: `work/v2-playable-core` at `062797f54ee63a65b2cc79870a619d1eeea0daa1`, verified locally and remotely.
- Support: `gpt-support/v2-01-observation-client-gate`, separate checkout `work/v2-observation-support`.
- Version 0.3.1-dev.1. Save format 5. Actor schema optional 1. Return Network schema 1. Art revision 2.
- Main production integration remains MAIN-owned. This branch is an adoption candidate, not a main merge.
- Caveman HOLD. No delegated agents or V2-02 implementation.

## Contribution estimate

Previous percentage, this increment's percentage, and cumulative percentage are **UNMEASURED**.
The repository contains no effort denominator or comparable time records for the entire V2-01 milestone.
Seven requested support workstreams now have concrete artifacts: observation, knowledge, NPC ownership, station selection, evidence tool, protocol, and negative regression coverage.
This inventory is not a claim of 40% effort. The 40% threshold remains unconfirmed.
Next support candidates are main-adoption diff review and importing actual client logs into the strict evidence format.
Do not create another general framework to fill an estimated percentage.

## Previous artifacts and inspected files

Read `SUPPORT_REBASE_REPORT.md` for the compatibility decisions.
The two named tier-A ZIPs were not located. No content-specific audit or merge of those archives occurred.
Existing current signal/load helpers remain useful. Older format assertions and incoming copies are superseded.
Old add-only auditing is dropped as a gate for this extraction. Historical files remain unchanged.

Inspected production: Wayfarer, StoryActors, StoryActorRecord, NpcEvents, StoryNpc, StationBlock, ReturnNetworkEvents, ReturnNetworkState, ReturnNetworkFixtures, NarrativeData, OpeningEvents, StoryConfig.
Inspected tests: ActorGameTests, NpcGameTests, ReturnNetworkIntegrationGameTests, ReturnNetworkSupportGameTests, signal tests, schema/transaction/preservation inventory.
Inspected tooling: build.gradle and the four existing return-network support tools.

## Production files touched

| File | Reason | Intended behavior change |
| --- | --- | --- |
| `entity/Wayfarer.java` | Call the shared range, facing, streak, and confirmation functions. Preserve LOS short-circuit ordering. | NO |
| `StoryActors.java` | Use the tested notification eligibility predicate. | NO |
| `content/StationBlock.java` | Use the tested observed/neutral text selector. | NO |
| `NpcEvents.java` | Use the tested ordinary/context dialogue selector. | NO |
| `ReturnNetworkEvents.java` | Use the tested completed-event glance and dialogue delegation policy. | NO |

New production helpers: `support/ReturnerObservationPolicy.java`, `ChaseKnowledge.java`, `ReturnNetworkNpcPolicy.java`.
All helpers are connected to production. They create no persistent facts and do not perform IO.
The 32-block limit, 0.35 dot threshold, eight ticks, and existing near-zero distance guard remain unchanged.
No schema, generation, checkpoint, transaction, navigation fallback, or corruption policy changed.
The existing completed-event relay glance remains present for gameTime modulo 200 below 30.
It still takes adapter precedence during that short window. The support does not silently redesign that behavior.

## Coverage and limits

- Nine policy GameTests cover positive exposure, tick-eight confirmation, distance/facing boundaries, occlusion input, streak reset, intermittent exposure, and original predicate equivalence.
- Three knowledge GameTests cover notice eligibility, single persisted observation fact, and immutable neutral/observed station selection.
- Three NPC policy GameTests cover resident delegation, ordinary/context dialogue, and stable participant identity/facts.
- One actual Wayfarer negative GameTest covers the supported creative mock through real server ticks and Actor NBT roundtrip.
- Existing unobserved completion regression is reused, not duplicated.
- Nineteen Python self-tests cover strict evidence handling and protected human fields.

These are policy/server tests, not real-client play or human observation.
NPC tests do not prove live movement or delivery of dialogue packets.
Station tests prove selector immutability. The production diff adds no particles, block writes, or durable mutation.

## Initial failed entity fixture

Symptom: the first optional positive entity fixture did not record observation. The other 129 tests passed.
Cause: the official `makeMockServerPlayerInLevel()` returns GameTestHelper$2, whose `isCreative()` always returns true.
Calling `setGameMode(SURVIVAL)` does not override that method.
Evidence: `evidence/support-v2-01/mock-server-player-api.txt` and `modified-gradle.log`.
Fix: replace the invalid survival assumption with a negative creative-guard integration test.
No fake-player subclass, reflection override, player-list hack, or production guard bypass was added.
Positive survival entity observation remains an actual-client gate. Shared calculation boundaries are automated.

## Client evidence implementation

Tool: `tools/validation/v2_01_client_evidence.py`.
Template: `tools/validation/v2-01-client-session-template.json`.
Protocol: `docs/V2_01_CLIENT_VALIDATION_PROTOCOL.md`.

The tool includes all fifteen scenarios and the twelve MUST requirements.
Missing scenarios remain NOT_TESTED. Any MUST FAIL makes the reported gate FAIL.
Any other non-PASS MUST result makes the gate PARTIAL.
Executed records require full build/JAR hashes, identity, start/observed/expected text, tester, notes, and reference metadata.
PASS requires existing local evidence files. References receive content hashes and sizes.
Unknown/duplicate scenarios, duplicate JSON keys, mixed builds, missing references, and escaping paths are rejected.
The tool preserves existing output for invalid input and atomically replaces valid summaries.
Exit codes: 0 gate PASS; 1 valid PARTIAL/FAIL report; 2 invalid input.
Human ratings require explicit HUMAN provenance and real-client evidence mode.
Synthetic self-tests and samples never grant real-client PASS or human ratings.
The tool validates reported evidence. It does not prove the tester's observations independently.

Sample result: `V2_01_CLIENT_GATE=PARTIAL HUMAN_EXPERIENCE=NOT_TESTED`, expected exit 1.
Sample output: `evidence/support-v2-01/sample-summary.json`.

## Validation status

Fresh baseline: 114/114 GameTests PASS, 176858 Core assertions PASS, build PASS, exit 0.
Python: 19/19 PASS on the host runner. Syntax: two Python files parsed successfully.
First modified GameTest gate: 129 PASS, one invalid mock-survival fixture failed. Preserve that failed log.
Final gate: 130/130 GameTests PASS, 176858 Core assertions PASS, build PASS, exit 0.
Final runtime: `BUILD SUCCESSFUL in 1m 1s`. Package hashes are recorded in `evidence/support-v2-01/VERIFICATION.txt`.
Protected persistence/schema/navigation sources and build.gradle have no diff against main baseline.

Environment failures: initial worktree checkout exceeded Windows path length; command-scoped core.longpaths repaired checkout.
Sandbox Python TemporaryDirectory cleanup failed with WinError 5. Host execution passed all nineteen tests.
Neither issue is classified as a Minecraft gameplay defect.

## Human and client gates

New support REAL CLIENT: NOT TESTED. No Minecraft client was launched by this support task.
HUMAN EXPERIENCE: NOT TESTED. AUDIO READABILITY: NOT HUMAN-VERIFIED.
Computer Use kernel-assets error is historical, not reattempted or modified here.
Repository relevance of that blocker: NONE demonstrated.
Use the manual protocol with the same evidence matrix when Computer Use is unavailable.

## MAIN HANDOFF

1. Compare current main HEAD with this support branch's implementation commit.
2. Audit only the current support diff. Do not apply the old tier-A bundles.
3. Select the current helper, production extraction, tests, evidence tool, template, and protocol files.
4. Recheck that runtime behavior, thresholds, checkpoint meanings, and storage semantics remain unchanged.
5. Merge or cherry-pick the selected implementation commit through MAIN's normal integration process.
6. Run the full GameTest/Core/build gate on the resulting main source.
7. Run actual client scenarios from a fresh world using the protocol and strict evidence tool.
8. Keep V2-01 PARTIAL until MAIN's client acceptance and gameplay review finish.

Do not cherry-pick historical baselines or evidence-only fixture commits as production changes.
The support package and publication records identify exact implementation and artifact commits.

## Status and safe pause

GPT SUPPORT IMPLEMENTATION: current implementation delivered; named tier-A archive inspection remains PARTIAL.
V2-01 AUTOMATED COVERAGE: PASS for this scoped increment; positive survival/client coverage remains open.
V2-01 CLIENT GATE: NOT TESTED. HUMAN EXPERIENCE: NOT TESTED. REAL CLIENT: NOT TESTED.
V2-01 campaign milestone remains PARTIAL. Version 0.3.2 is not promoted.
Weekly usage reached 97% during the previous implementation task. That task finished its gate, package, rollback, and upload.
The 2026-11-01 continuation observed 1% weekly usage consumed before starting bounded evidence-tool work.
The continuation adds a log-draft importer and fixes unexecuted REAL_CLIENT reporting.
Read `docs/SUPPORT_V2_01_LOG_INGESTION_REPORT.md` for this continuation's evidence and adoption instructions.
The importer never grants scenario PASS from log markers.
Resume MAIN adoption review or actual client acceptance after the support diff review.
Do not start another large support subsystem from this handoff.
