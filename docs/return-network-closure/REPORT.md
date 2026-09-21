# MAIN RESULT — RETURN NETWORK FIRST RESPONSE CLOSURE PREFLIGHT

## Current / safe pause
0.3.1-dev.1 / work/return-network-integration / PARTIAL. First compound event NOT SEALED; campaign PARTIAL; no 0.3.2; next subsystem NOT STARTED. Save5 / optionalActor1 / ReturnNetwork1 / art2 unchanged. Caveman HOLD. Final art DEFERRED.

Weekly remaining usage was 7% at the final work boundary. Finish the in-flight source-identical regression/package/upload, then pause before Phase E production integration. This is an A-D preflight package, not a sealed event release. No power operation requested or performed by this work.

## Baseline / Support audit
Actual initial local and remote branch HEAD: 7aa36c50ea0f13201304e0ad0f25c6b3f7a5edf3. Baseline inspection found no newer remote commit. Support implementation b6ceda9f, MAIN inherited b5d26787. Baseline copies preserved under evidence/return-network-closure/baseline:
- main.jar c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162
- main-source.zip c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda
- support-source.zip 24b676bef766abddf7a9186284749ddf7613f739fc809be0a80bc59122335318

AUDIT.md classifies immutable signal semantics and read-only loading diagnosis SAFE_TO_ADOPT; runtime scheduler/carrier, static audit and packaging REQUIRES_ADAPTATION. No blind copying/reimplementation. Existing MAIN 21/22 clients and Support 102 tests are historical evidence, not these new client passes.

## Actual changes and why
- ReturnNetworkSmoke.tick: one opt-in test dispatch line to ReturnNetworkClosureProbe before the existing exception hook. Ordinary gameplay logic is unchanged.
- ReturnNetworkClosureProbe.step: freeze cp1 and identity at actual discovery while Yeoul remains in home chunk(2,4), different from relay chunk(2,2). Move only the isolated fixture player; inspect actual hasChunkAt, areEntitiesLoaded and canonical entity lookup. Hold NPC-only and relay-only availability for80 ticks each; save disk snapshots, return and naturally complete. No force chunks, NPC teleport, new actor or generation.
- ReturnNetworkEnvironmentProbe.step: bounded actual obstacle/environment fixtures. Preserve fixture-placed blocks while blocked, restore only those fixture changes, and verify natural resumption. Eight cp2 variants plus closed route and cp4 structural LIT loss. Never change durable progression to manufacture a test result.
- tools/ci/return-network-closure.init.gradle: isolated muted right DISPLAY1 client profile, valid simulation distance5, render distance2, separate evidence. New Seed uses existing fresh-world seed harness; ReturnLight uses existing NpcSmoke ResumeB against a copy of that fresh seed.
- tools/run-return-network-closure.ps1: exact command ledger, source hashes before/after, PASS marker, JVM exit and PID gone, monitor/mute assertions.
- tools/verify-return-network-closure.py: cross-snapshot event/identity/fact verification and final-source manifest validation.
- tools/package-return-network-closure-preflight.py: non-overwriting preflight package, class/asset comparison, exact-source ZIP, JAR-only rollback on a disposable copy.

NarrativeData, ReturnNetwork state/schema, checkpoint contracts, StoryStorage, commitReturnNetwork, Actor identity, generation, NpcNavigation, production ReturnNetworkEvents and assets were NOT edited. createPath(destination,0,48) preserved. No new durable fields/timers. Commit candidate -> integrity -> same NBT -> durable write -> readback/hash -> live publish remains unchanged.

## New actual Minecraft runs (scripted, not manual survival)
All successful runs used muted isolated profiles on right secondary DISPLAY1. Fresh JVM and PID termination were checked. No audible/readability user test is claimed.

| Run | Result | What it proves |
|---|---|---|
| Availability1 | FAIL fixture | invalid simulationDistance2 and near player retained both components; bounded check rejected false independent-load claim |
| Availability2 | FAIL fixture | distant player unloaded both; bounded check rejected false independent-load claim |
| Availability3 | PASS, superseded source | real NPC-only and relay-only availability then same-identity cp6; later test hook additions require final-source rerun |
| EnvironmentMatrix1 | PASS | actual closed route UNREACHABLE; natural resume; eight variants; cp4 LIT loss waits, restored equipment completes |
| Reopen1 | PASS | new JVM same completed environment world, identical event and NPC facts, no duplicate completion |
| Seed1 | PASS | new world actual return-light introduction/navigation/completion, NPCcp4 |
| ReturnLight1 | PASS | copied fresh world, new JVM, same UUID/instance/generation0, home, first conversation, shared_return_light and maintenance receipts unchanged |
| Availability4 | PASS final source | real independent availability with final Java source, cp1 disk identity/facts frozen, natural cp6 aftermath |

New actual-client aggregate:6 PASS /8 attempts. Final Java source subset:5 PASS /5 attempts. Earlier Availability3 is retained but not promoted into final-source proof. No new hard-cut signal tests ran.

## Independent loading
NPC home and relay are actually separated during approach; arrival stand and relay remain same chunk(2,2). We did not pretend same-chunk blocks had separate chunk loading.
NPC available/relay unavailable and converse both held80 ticks with unchanged cp1 state, instance and participant identity; return naturally reachedcp6. Availability4 final disk fe4e489e8e14f0ffc837e32b2da823a34aea52fb8363c5182734eea09fa155ae.
Yeoul b576b8cc-0a7d-45ea-8216-52f25bc0fc8a; instance2ee78da8-6cfd-490b-8a2f-f037a46b5add; generation0; canonical1.
Cached WAIT remained NONE while updater absent; durable progression stayed frozen and resumed correctly. Classification: stale diagnostic display in tested case, not observed production progression failure. No new persistent WAIT field added.
Present chunk + entity storage pending transition: NOT REPRODUCED; no PASS claim.

## Environment / lighting
Closed actual NPC route reported UNREACHABLE; opening it let original NPC walk normally. Eight variants: feet obstruction, water, head obstruction, missing relay, wrong relay block, out-of-interaction-range, actual player/NPC collision, unrelated sea-lantern interference. Collision displacementSquared0.21761583335811552 confirms physical movement, not a mock. Every held state retained event facts; production deleted no fixture blocks. cp4 LIT=false failed structural readiness and waited; restoring original activated relay resumed. This does NOT prove transient signal readability and does not flicker production LIT.
Environment completed disk6f6902bcc8ca8e8aec71b8b493a2c7a5a658fd56e23ff82330c4bda00fc18655; reopen disk0bee22bddce5112e3c6a7171c8c70b78945014b6993ff6ef9ca44e70af369f65. Whole-file hashes differ due runtime NPC position; event and permanent NPC facts compare equal.

## Return-Light regression
Fresh seed NPC6e91a7b4-1ba4-4d11-9a4a-dc9dddbdc21a, instance620606dd-1458-48ef-a25a-cb055fb097f8, generation0. Seed and resident reopen match identity and facts. cp4; introduction, first_conversation, shared_return_light, home and maintain_return_light remain. ReturnNetwork remainscp0. This is a new actual prerequisite regression, not historical evidence or full natural survival.

## Failure history
Both-loaded -> Availability1 fixture -> invalid client simulation2 plus retained ticket range -> init/probe fixed to valid5 and observed spatial boundary -> final actual independent combinations PASS.
Both-unloaded -> Availability2 fixture -> over-distant player anchor -> bounded actual availability search and nearer approach anchors -> Availability3 and final-source4 PASS.
These are fixture setup failures, not actor identity or save corruption failures. Failed logs and fixture snapshots retained under attempts. No known production defect was patched in this phase.

## Gates and remaining work
A Support audit: complete.
B actual component-independent availability/re-entry: verified for separated approach chunks; entity-storage pending transition NOT REPRODUCED.
C scripted path/environment/light validity: PASS, not human signal readability.
D fresh return-light + new-process maintenance regression: PASS.
E production SHORT-SHORT-LONG: NOT STARTED. Existing production still single bell + text.
F new signal interruption/replay/reconnect: NOT RUN.
G human scene/signal readability: NOT RUN.
H final post-signal regression: NOT RUN. Current preflight rerun is102 GameTests +176857 Core assertions + build, gated by GATES.json/tests-final.log.
I event SEAL: NOT SEALED. Campaign PARTIAL. No subsystem/release advancement.
New targeted corruption fixtures were not executed; existing load/integrity regression tests retained. Prior MAIN corruption/hard-cut/death/dimension evidence remains PRE-EXISTING MAIN EVIDENCE.

## Exact next start
First verify remote/local HEAD and preflight package hashes. Start Phase E: adapt Support semantic SHORT SHORT LONG to an actual tick driver and transient visual/audio carrier; never toggle structural relay LIT, serialize signal ticks, change checkpoint3/4/5 semantics or bypass commitReturnNetwork. Strict scheduled-tick misses restart a fresh session, never burst catch-up. Preserve emitted/observed/matched separation. Then fresh actual clients for interruption before/among/during pulses, post-cp5/F/6 reopens, explicit ambient-only completed replay; assess visual/audio readability, rerun full102+ tests/build on exact final source, and decide SEALED only with actual evidence.

## Storage / rollback / models / upload
Save5 / Actor1 / ReturnNetwork1 retained. No migration or downgrade changes. Rollback restores previous verified MAIN JAR only to build/rollback-return-network-closure/test.jar; no world or progress edits. Original immutable baseline packages and original master six staged deletions preserved.
No new Terra/Luna/Sol delegation; current main agent performed audit, fixtures and review. Prior Sol Support remains inherited. No model-routing/token-savings experiment, no Caveman use. Only account usage was measured (weekly93%used at safe-pause decision); no task-level token attribution claimed.
Upload target: scarlel96-design/WHILE-YOU-WERE-AWAY branch work/return-network-integration only. Code/tests/docs, new closure evidence and dist/return-network-closure-preflight are the intended upload set. Exact successful commit/remote receipt is recorded in evidence/return-network-closure/UPLOAD.json after publication. This source package predates only that metadata receipt; no production source changes are allowed after validation.

Verifier correction: initial disk verifier expected a serialized cp0 returnNetwork key and raised KeyError. NarrativeData.savePayload intentionally omits the extension at NOT_STARTED. Corrected assertion requires absence in BOTH seed and resident; no production edit or fallback inference. Re-run required and recorded.
