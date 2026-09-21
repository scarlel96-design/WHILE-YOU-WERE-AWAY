# GPT SUPPORT RESULT — RETURN NETWORK INTEGRATION CLOSURE V1

## Current / Baseline

0.3.1-dev.1, `work/return-network-integration`, campaign/event PARTIAL, support code and automation complete; actual runtime integration and event sealing remain MAIN-owned. Save format 5, optional Actor schema 1, Return Network schema 1, city art revision 2. No version promotion, new content or persistent fields. Caveman HOLD; final art DEFERRED.

Repository: https://github.com/scarlel96-design/WHILE-YOU-WERE-AWAY

Actual local/remote tip at start and before finalization: `b5d26787e4532cffd6379a6ea1e4cc8b6898ebe8`, branch `work/return-network-integration`. Implementation ancestor `79a0d47d7b8cf403c701bde81ae1e2453cf2e370` remains the MAIN implementation baseline. `570c4f8` is historical inherited input only. No later remote commit was present at those reads. Support started from a clean separate worktree. The six pre-existing staged pycache deletions in the original master worktree were inspected and left untouched.

Immutable baseline copies:
- `evidence/return-network-support-v1/baseline/main.jar`: SHA256 `c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162`.
- `evidence/return-network-support-v1/baseline/main-source.zip`: SHA256 `c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda`.

PRE-EXISTING MAIN EVIDENCE: 95 GameTests, 176857 Core assertions, build PASS; real clients 21 PASS / 22 attempts including A–F and G-before/after cuts. These prior clients are not new support runs.

## Work performed / production files touched

Only new files were added. All 2116 pre-existing tracked files remain unchanged. No existing runtime method has been replaced or wired to the helpers.

SAFE_SUPPORT_IMPLEMENTATION:
1. `src/main/java/io/github/whileaway/support/ReturnNetworkSignal.java`: pure runtime-only planner, sequence, dispatch, observations and replay policy.
2. `src/main/java/io/github/whileaway/support/ReturnNetworkLoadAssessment.java`: read-only diagnostic of dependency readiness, explicitly not a replacement for the production updater.
3. `src/main/java/io/github/whileaway/ReturnNetworkSupportGameTests.java`: seven additional NeoForge GameTests, no automatic repair or chunk loading.
4. Two standalone Java test classes under `src/test/java/io/github/whileaway/support/`.
5. Four tools under `tools/ci/`: static audit, evidence-path init script, baseline/binary probe, source bundle/rollback tool.
6. This report, failure matrix, handoff, immutable baseline copies and fresh evidence.

PROPOSED_PRODUCTION_PATCH: none applied. CODEX_ONLY_CHANGE: carrier wiring, live readiness collection, actual-world verification, final seal/release judgment.

## Independent loading / NPC-only / relay-only

`assess(Snapshot)` considers eligible player, dimension, NPC chunk, NPC entity-store readiness, canonical identity, relay, environment, then path. `Result.factMutationAuthorized` is always false and rejects construction with true. ARRIVED alone yields diagnostic READY; AVAILABLE, temporary failure, unconfirmed identity and missing dependencies remain distinguishable. It never selects/spawns/replaces an actor or requests a chunk.

The pure tests cover absent/present dependency combinations, reload order, return order, environment-before-path, immutable identity/facts and no mutation authorization: 53 assertions PASS. Model/serialization GameTests also pass. **Actual independent chunk loading is NOT RUN.** A fixture recipe is authored in FAILURE_MATRIX.md, not an executable claim of physical unload.

Two integration findings are important:
- Relay `(42,68,46)` and NPC arrival stand `(44,65,47)` share chunk `(2,2)`. At arrival, pretending they are independently loaded chunks is invalid. Use an earlier spatially separate approach or explicitly identified entity-store readiness case; prove actual coordinates/readiness.
- The production update belongs to a live StoryNpc. An unloaded NPC means no invocation, so cached waitState can be stale. The support model describes desired diagnostic state, not proof that production wrote WAITING_NPC.

## Path / environment / lighting

The matrix covers temporary obstruction, closed doors, narrow passages, player blocks, range loss, NPC push/block, chunk edge, changed equipment, removed lighting and restoration. No teleport, obstacle destruction, force-load, regeneration or >48-block navigation expansion was added.

New GameTests exercise actual loaded blocks for a lit relay, unlit relay, removed relay, wrong lamp, obstruction, water and restoration. All relevant chunks are asserted loaded before any block read/write; fixture-only edits are restored in finally. The fixed-coordinate test has a separate batch. Existing path 30-block success / 49-block blocked regression remains in the unchanged suite.

`LIT=true` is currently a structural condition from cp4 onward. Do NOT animate this property OFF for signal presentation; it would invalidate environment readiness. MAIN must choose an independent transient carrier. Human readability, unrelated player lighting interference and actual path obstruction play are NOT RUN in support.

## Signal architecture / SHORT-SHORT-LONG

`SEMANTIC_PATTERN` is immutable SHORT, SHORT, LONG. `Timing` takes injected ticks, requires positive gaps/durations and SHORT < LONG. `plan` uses checked arithmetic; immutable Timeline validates the exact origin, pulse order, duration classes and configured gaps. No milliseconds/tick timing is saved as story truth; no final Minecraft tuning is asserted.

`TransientSession` states: ACTIVE -> COMPLETED, or ACTIVE -> RESTART_REQUIRED on interruption/readiness loss/missed dispatch deadline. Dispatch is exactly on planned ticks. Duplicate polling does not emit another pulse. A skipped start does not silently catch up: MAIN must explicitly restart under a distinct session identity, with monotonic start time. Restart always recreates the complete pattern; old emissions and observations are not concatenated. Failed overflow leaves the old state unchanged.

`EmittedPulse` and `ObservedPulse` differ. `matchExact` compares structure only; `observeMatch` additionally requires COMPLETED. Partial/duplicate/wrong-order/mismatched-observer/session samples fail. Technical samples are not proof that a human perceived the signal.

Completed replay is denied by default. Explicit permitted replay uses AMBIENT and a new identity. The model contains no story commit callback, NBT schema or save API, so presentation cannot grant cp5 evidence/shared facts/completion. Existing MAIN commits remain authoritative.

Signal pure Java tests: 27/27 PASS. **SHORT-SHORT-LONG semantic model implemented; actual sound/light carrier not wired or tuned.** No production event silently switched from a bell to this sequence.

## Existing return-light regression / Integrity / persistence

New GameTests retain completed checkpoint-4 prerequisite, NpcState receipts, home binding, actual return-light dependency recognition versus an ordinary lamp, actor UUID/instance/generation/fact serialization, and RN_PARTICIPANT_IDENTITY_MISMATCH detection. They are not live NPC conversations, maintenance, death or fresh-process client proof.

No NarrativeData, ReturnNetworkState, ReturnNetworkIntegrity, StoryStorage, commitReturnNetwork, StoryActorRecord or NpcNavigation edits. Candidate validation -> same-NBT event/NPC facts -> disk readback -> durable hash -> live publish is unchanged. Existing block/story commit order and write guards remain intact. No schema/migration/downgrade changes.

## Fresh validation

| Gate | Result | What it proves |
|---|---|---|
| Static audit + self-test | STATIC_PASS | support allowlist, 2116 protected files unchanged, forbidden API checks; not a formal semantic proof |
| Standalone Java compile | PURE_JAVA_COMPILE_PASS | both pure helpers and both tests compile with JDK21 |
| Signal tests | 27/27 PASS | temporal/observation/replay failure surfaces above |
| Load tests | 53 assertions PASS | read-only diagnostic combinations and precedence |
| Full Gradle compile/build | GRADLE_COMPILE_PASS / PASS | final configuration compiles/builds |
| Headless NeoForge | GAMETEST_PASS, 102/102 | unchanged original95 plus seven new tests |
| Core | 176857 assertions PASS | fresh existing core regression |
| Binary preservation | PASS | all120 original classes and102 asset entries byte-identical |
| New real client / fresh client process | NOT RUN / NOT RUN | no support client launch |
| Actual NPC-only / relay-only unload | NOT RUN | model and authored recipe only |
| New signal actual-client readability | NOT RUN | MAIN integration/tuning required |
| CI | NOT RUN | local automation is not hosted CI evidence |

Two complete Gradle invocations passed 102 tests and176857 core assertions each; count remains **102 unique GameTests**, not204. First build 58s, final build54s; these are reported Gradle elapsed values, not model coding-time/cost measurements. The second run verifies the corrected independent evidence path.

Exact commands and literal outputs are retained in VERIFICATION.txt and logs. Final Gradle evidence directory is `gametest/35307dd7-9978-4416-a950-70f539daa5ef`; first attempt's new evidence was relocated to `gametest/attempt-1`, without modifying any prior evidence.

## Failures encountered / rework

1. Signal prototype passed14 authored tests, but review reproduced four missed failure surfaces (exit1): resume during final LONG threw; a missed first deadline stranded the session; Timeline accepted a different origin; interrupted pulses could structurally match. Root cause: tail-resume scheduling and a structural matcher without completion gating. Sol rewrote only its assigned pure model/tests around explicit RESTART_REQUIRED and session-gated observation. Main reran27 tests successfully. Preserved initial log and pre-fix failures; no claim of first-attempt success.
2. Load helper draft risked being mistaken for production wait ordering; review required explicit diagnostic-only semantics and environment-before-path once a live NPC exists. Tests/docs were corrected before final runs. Added no production repair.
3. Static audit initially flagged a test-only Serializable import used to prove absence of serialization. Narrow exception now permits only that exact import in standalone tests; production helper I/O remains blocked. Self-tests cover this exception. Initial audit FAIL is preserved.
4. First Gradle init set a task JVM property but NeoForge's generated run arguments selected the original evidence path. Tests/build passed; this was evidence-routing, not runtime failure. Init now configures the NeoForge run model. Final fresh run passed and wrote into support evidence. Original/new file checks show no old evidence overwritten.
5. Restricted shell remote read hit loopback proxy port9; an approved network-capable retry read the exact remote tip. This was execution context, not a repository/code failure.
6. Sol work briefly paused at usage capacity; it resumed on the same model and files. No incomplete intermediate state was published as final validation.
7. Git Bash rollback initially failed to create its signal pipe in the restricted execution context (Win32 error5, exit -1073741502), before executing the script. The approved retry executed the same copied-ZIP rollback successfully, exit0; original source/JAR/worlds remained untouched.

## Models / Caveman

Two explicitly authorized GPT-5.6 Sol High assignments: signal model/tests; load helper/seven GameTests/tests. Parent Codex reviewed semantics, requested revisions, authored audit/package/docs and performed final compile/regression/binary verification. No Terra/Luna, no Caveman, no model A/B benchmark. Signal had one substantive review-driven redesign round covering four defects; load received review corrections. There is no measured token saving or proof of equal model quality; quota readings are not per-task token accounting. No assertion of a measured parent-model name beyond the host role.

## Artifact / GitHub / rollback

Target artifact: `dist/return-network-support-v1/whileaway-gpt-support-return-network-closure-v1.zip`. It preserves every original MAIN source ZIP entry and adds support sources/tools/docs/evidence. It is not a promoted release JAR. `evidence/return-network-support-v1/DIFF.patch` is the five-file Java addition diff; all tooling/docs are also included in the bundle.

Baseline/source/binary checks are completed before packaging. Packaging hashes, disposable-ZIP rollback and final upload are recorded in the post-package receipt (and final task response); the immutable archive cannot contain its own final hash. Rollback is deliberately an artifact-copy test, **not a save downgrade or a production deployment rollback**. It accepts only a verified support ZIP below `build/return-network-support-rollback/` and restores the preserved MAIN source ZIP. It needs the repository baseline copy and BUNDLE.sha256 sidecar. It never edits runtime source, deployed JARs or worlds.

Rollback preflight already passed on a disposable ZIP containing the unchanged MAIN source plus all five support Java additions: input SHA256 `fa6b78fbb658a1785ce175d1438f349c3907a9f42e201c208b7287d0dfac7a23`, restored SHA256 `c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda`, exit0. The published bundle copy is checked again after packaging; this is not an additional gameplay test.

Upload only the actual `work/return-network-integration` branch, no master/force push. Final upload receipt records implementation commit and observed remote tip. The archive report is the pre-publication snapshot; the repository receipt proves publication.

## Final status / exact next step

Support safe implementation and local automatic gates: PASS within the stated model/helper scope. Physical independent-load fixture: AUTHORED / NOT RUN. Actual-client and production signal integration: NOT RUN. Return Network event and overall campaign: PARTIAL / NOT SEALED.

MAIN first reviews this diff and the same-chunk/stale-wait/LIT hazards, then performs real independent dependency loading, path/lighting changes and existing return-light actual-client regression. It must integrate/tune the transient signal, verify human-readable SHORT-SHORT-LONG, partial interruption/reconnect/aftermath and disk/identity invariants, then decide the event seal. Item/Reward/Structure Fallback and subsequent Scenario/Memory/Threat/Opening work require MAIN roadmap decisions; none were started here.
