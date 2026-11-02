# Latest continuity rule — 2026-11-02

**Read [docs/PROJECT_CONTINUITY.md](docs/PROJECT_CONTINUITY.md), [docs/DECISION_LOG.md](docs/DECISION_LOG.md), [docs/CONTENT_ROADMAP.md](docs/CONTENT_ROADMAP.md), and [docs/WORK_ORDER_PROTOCOL.md](docs/WORK_ORDER_PROTOCOL.md) before drafting or starting new work orders.** Read the existing design and evidence documents linked there. These are repository continuity documents, not evidence that game features have been implemented.

**Effective only for new work orders after the active V2-03 work.** Do not change the active V2-03 scope or silently reinterpret its acceptance criteria. Current and newly added docs do not change runtime/save data. Source-of-truth precedence and dated overrides are in PROJECT_CONTINUITY.md. Earlier priorities recorded below are historical where they conflict with later approved decisions.

**Active execution policy:** GPT-6.1 SOL/Codex main implementation line only; no compulsory GPT support 40% split. Content first. The user performs the first large human survival/fear/audio playtest when approximately 50–60% of the defined total gameplay content has been implemented. This is not permission to skip build, targeted GameTests, storage/canonical-actor, world-preservation or package integrity checks. Do not run repetitive Computer Use survival mining/hunting/station discovery to close the old V2-01E gameplay gates. Preserve NOT TESTED honestly.

**Creative safeguard:** Strong survival horror, analog-horror grammar, meaningful jump scares, distinctive monsters and sound, expressive compact high-quality structures, world-scale setpieces and lasting safe aftermath; preserve long-form mysteries, present NPC agency, player-past amnesia, distinct Observer/Shadow mystery and five endings. Do not collapse all anomalies into one explanation. Dialogue remains text/chat; no required voice generation. Respect player-authored worlds, controls, privacy and existing safe recovery contracts.

---

# Development continuity

- Keep all agent-launched clients and test programs muted before startup. The user requested silent testing.
- Do not change the user's global volume or normal Minecraft profile. Use isolated development profiles.
- runClient and runClientSmoke apply zero volume to every sound category before launch. Preserve this gate.
- Check audio by decoding files, measuring samples and checking scheduling; do not audibly preview it automatically.
- Distinguish unit, headless Minecraft, client-resource, screenshot and manual-playtest evidence.
- This is a first development slice, not the completed multi-ending campaign. Keep the documented remaining work explicit.
- Historical art slice is 0.3.0-dev.1. Preserve evidence/art/baseline and both prior 0.1/0.2 releases.
- runCitySmoke and runCityReload share a separate muted profile; both need explicit PASS markers, not only process exit 0.
- Vanilla GameTestServer creates only preset dimensions. Actual city creation and persistence are checked in the isolated integrated-client runs.
- CityLayout.VERSION=1 and its stable cell order are persisted construction data. Do not reorder a released plan without migration.
- CityArtLayout.VERSION=2 is used only for new districts. Never replace an existing revision-1 district implicitly.
- City horror is a ledger-gated one-shot client presentation. Keep actor cleanup and the saved one-shot flag covered by the muted smoke/reload tests.
- Package the current art slice with tools/package-art.py --prepare, tools/verify-art-transaction.ps1, then tools/package-art.py. Keep source evidence and runtime evidence distinct.

- The user revised priorities on 2026-09-09. docs/FUNCTION_FIRST.md supersedes previous roadmap ordering. Finish this art pass, then prioritize playable functions, recovery and all-ending integration; defer final art/polish.
- The Overworld remains a core survival/story stage, not merely the city entrance. Never randomly destroy or overwrite player builds.
- Do not call a feature complete without in-game integration, persistence, exception recovery, cleanup and onward progression evidence. For current checkpoint coverage and remaining F01 work, use evidence/stability/GATES.md.

- 0.3.1 introduces schema 4 (reads 1/2/3) and leased scene checkpoints. Check docs/RECOVERY_031.md and evidence/stability/GATES.md before resuming; the complete stability phase is NOT SEALED.
- Preserve evidence/stability/baseline (0.3 JAR/source). Do not overwrite evidence/art or evidence/city with new test output.
- RecoverySmoke is opt-in fault injection. Cut tasks halt only the isolated test JVM after a durable midpoint. A process exit alone never passes: require named gate markers and successful next-process reconstruction.

- User extension 33-81 is recorded in docs/MYSTERY_RESOURCE_EXTENSION.md. Keep stabilization first; resource packs, investigation/puzzles/items and foreshadow runtime follow in the specified order. docs/asset-contract.json tracks current placeholders; no external pack is selected or bundled.

- Latest continuation keeps the adopted 0.3.1 JAR/source immutable. Use docs/STABILITY_REUSE.md and evidence/reuse/GATES.md for the working slice. Do not call the work 0.3.2 or full stability PASS.
- Package the new work with tools/package-reuse.py and tools/verify-reuse-transaction.ps1, not package-stability.py (which targets the adopted dist filename).
- NeoForge SavedData.save queues IO. NarrativeData overrides its file save to synchronously finish atomic NBT replacement and preserve dirty on error. Never regress to treating a queued save as a durable checkpoint.
- Run Gradle tasks serially in this checkout. Concurrent incremental compilers were observed to collide on shared class outputs; keep the failed attempt as tooling evidence, not a gameplay defect.
- BoundarySmoke uses work/boundary-smoke with all audio categories zero. Investigation order is runBoundary4A, B, D, E, Verify; presentation checks use 1/2 D, E, Verify. A PASS cut marker requires matching on-disk snapshots and the next process, not only Runtime.halt.
- Scene lease tokens are not persistent causal dependency links. CityInvestigation is the existing three-book adapter, not the future complete Investigation/Puzzle/Item framework. Do not claim its notification receipt proves item/reward atomicity.

- New adopted baseline is dist/stability-work (JAR 9062d039..., source 9f0f464e...). Never run package-reuse.py again against this baseline. Current actor-lifecycle slice uses dist/lifecycle-work and evidence/lifecycle.
- StoryActors stores optional actorSchema=1 inside schema4 NarrativeData. Identity is logical storyId + instance + role + canonical UUID + generation. Missing entity storage must be loaded before reconstruction; old generations are fenced on join.
- ActorSmoke is an explicitly scripted chase fixture, not natural survival or a shipped NPC/composite-event test. All six A/B/C/D/E/Verify processes must use the same runtime source and remain muted.
- NPC villager GameTests validate generic lifecycle primitives only. They do not establish actual named NPC campaign behavior, item transaction atomicity, causal links or structure fallback.

- Adopted lifecycle-work JAR/source stay immutable (07e888a0... / cc1a1899...). The current limited campaign-pilot slice is PARTIAL; see docs/CAMPAIGN_PILOT_031.md. Do not run package-lifecycle.py against the baseline again.
- Caveman was a bounded Skill-only A/B pilot, not a project-wide setting. Do not persist Caveman across future work or compress final handoffs without user approval. Numeric results are evidence/caveman-pilot/usage.json; no causal or billing savings claim.
- All new launched clients must be muted and placed on the user-selected RIGHT secondary Windows DISPLAY1. Refresh tools/select-test-monitor.ps1 and require the runtime window bounds assertion.
- Wayfarer owner absence now suspends instead of completing by distance. Existing ActorSmoke D still uses escape completion; update that fixture to genuine combat completion before rerunning old ABCDE tests. Preserve old evidence/lifecycle logs.

- Current work is wayfarer-stability, based on immutable dist/campaign-pilot (JAR 9c5f7810..., source 81b47fcc...). Use tools/package-wayfarer-stability.py; never republish campaign-pilot. ActorSmoke D now uses genuine combat. All current runtime evidence belongs in evidence/wayfarer-stability and isolated work/wayfarer-* profiles.
- docs/DESIGN_V04.md adopts the user's design v0.4 as the higher narrative contract: Continuity Graph preserves relationships/vacancies; the last day is synthetic; identity includes the right to change. This is NOT mod version 0.4 and introduces no runtime/save/art changes during stability.
- Preserve player-created builds even in correction-front scenes. Keep lethal telegraphs reliable, missed-scene evidence recoverable, and story-directed memory changes distinct from data corruption. Major psychological scenes target No-Spawn, No-Stinger and Free-Look together; design adoption alone is not PASS.
- Caveman remains HOLD/off in this stability slice. Do not start another A/B test or change model/delegate without the applicable user authorization. Final handoffs remain detailed.

- New adopted baseline is immutable dist/wayfarer-stability (JAR 3d3b08be..., source 21175125...). Current output is dist/load-guard; use tools/package-load-guard.py, not the adopted baseline's packager. See docs/LOAD_GUARD_031.md.
- NarrativeData.get must use StoryStorage.open, never a computeIfAbsent fallback after parser failure. Existing corrupt/unsupported files are blocked and not registered as empty SavedData. Keep world-local caching, original-byte quarantine and the bound-file/hash write guard.
- Final load-guard policy blocks missing UUID and missing/negative generation. Do not reuse the discarded prototype that inferred generation from snapshot alone. Only missing snapshot.Motion receives the tested stationary runtime-default repair; this is not a complete actor reconstruction policy.
- Latest evidence is evidence/load-guard/client-final plus crash/exceptions. evidence/load-guard/client and prototype contain development attempts, including the startup-stop deadlock; do not count them as final PASS. Blocked client smoke uses explicit real-server save dispatch and then test-only halt, not a normal survival autosave-timer playthrough.
- Actor recovery remains PARTIAL. Next is unique live canonical evidence for missing UUID, then confirmed-missing generation transaction/crash boundaries. No NPC/item/reward/fallback expansion before Actor recovery sealing.

- New immutable input is dist/load-guard (JAR a66cf057..., source c5d0161e...). Current work/output is canonical-recovery; use tools/package-canonical.py and never republish the load-guard baseline.
- CanonicalRecovery may repair a missing UUID only through the existing event ticket plus one exact live identity. It keeps a private strict-loader preflight draft, requires both position chunks/entity stores ready and distinct loaded observations, quarantines the unchanged original, and commits only after agreement. Malformed UUID/generation remain manual; ambiguity never chooses a winner.
- PREPARED + generation + entityUUID is a durable reservation, not proof of entity existence. Retry it with the same epoch/UUID. Never turn PREPARED into SUSPENDED merely for owner absence. Actual replacement entity storage is flushed before ACTIVE binding commit; preserve six GENERATION_* observation boundaries.
- A known older generation is fenced on entity join. Same-generation/other identity conflicts poison story writes and pause AI instead of silently selecting/discarding a winner. Story activation guards never return an empty campaign.
- Final current evidence is evidence/canonical-recovery/canonical-final, client-final, crash and exceptions. canonical/ contains six development clients and must not be counted as final. Use GATES.json and VERIFICATION.txt after their current verifier succeeds; no historical PASS promotion.
- tools/LateActorFixture.java writes only an explicitly named copied fixture world. Late mode verifies actual entity-chunk deserialization after a generation-1 replacement; verify-late-region.py checks persisted cleanup. No normal world mutation or broad region scan is authorized by this fixture.
- Keep save format 4 / actorSchema 1 / art revision 2 and mod 0.3.1-dev.1. No new recovery fields changed permanent storage meaning. Next campaign work after the current Actor gates is a real NPC event; no 0.3.2, transactions, fallback, art or Caveman expansion yet.

- New input is immutable dist/canonical-recovery (JAR 82b0201c..., source 987953a2...). Current NPC slice uses dist/npc-lifecycle and evidence/npc-lifecycle. Never republish the adopted canonical output. See docs/NPC_LIFECYCLE_031.md; NPC A/B/C/D/E does not certify death/chunk/dimension/copy exceptions or full NPC stability.
- NPC home/goal/work are single-valued state; dialogue/met/shared-event/link are durable receipts. Do not mistake a deliberate work-state change for lost event history. Keep common Actor identity and fail-closed decisions, provisional art, silent right DISPLAY1 clients and serial Gradle execution.

- User requested usage-aware SAFE PAUSE (not shutdown): check current five-hour and weekly limits at major work boundaries. Near exhaustion, do not start another large coding/test batch. Finish the in-flight save/recovery pair, preserve logs/artifacts and exact next step, mark unfinished gates PARTIAL/NOT TESTED, then pause. Never deliberately consume usage to reach a threshold. No automatic computer shutdown without a new explicit request.

- Immutable input for npc-exceptions is dist/npc-lifecycle (80a80cf1... JAR, eba768de... source). New output/evidence use npc-exceptions only. Use package-npc-exceptions.py; never republish npc-lifecycle. Player-independent NPC ownership does not imply an unfinished event has a living witness. Preserve PREPARED and generation while no eligible player is present; completed residents remain independent.

- npc-world uses immutable npc-exceptions baseline (19636c1b... JAR / d34f61ad... source). Only new opt-in NpcWorldSmoke plus isolated tool/config changes; every existing 83 compiled class is byte-identical. Safe pause marker at evidence/npc-world/PAUSE_REQUESTED stops new launches. Check GATES.json and current client command ledger before resuming; next expected Chunk4, not full NPC closure.

- npc-world continuation completed travel9 + divergent-copy5 actual clients (12 new this continuation). Copy harness is opt-in; baseline83 classes unchanged, current85. Rerun verify-npc-world.py THEN verify-npc-copy.py, as the first writes travel-only gates. Current build evidence tests-4.log; tests-2/3 are preserved build-tool failures. The old two-case pause verifier is historical only. Next NpcEvents.update residence/path/return-light environment classification; NPC corruption clients and combined regression remain, so no composite/0.3.2/art expansion. Preserve dist/npc-world before the next production change.


## GitHub 업로드 정책

- 앞으로 생성·수정·검증한 모든 작업물은 작업 완료 후 GitHub 저장소 `https://github.com/scarlel96-design/WHILE-YOU-WERE-AWAY`에 업로드한다.
- 업로드 대상은 코드, 문서, 테스트 결과, 검증 보고서, 변경 내역, 패키지 산출물 및 관련 상태 기록이다.
- 각 작업 보고서와 관련 문서에는 업로드 여부, 브랜치, 커밋 SHA, 업로드 대상 경로를 구체적으로 기록한다.
- 로컬 작업만 완료된 상태는 원격 업로드 완료로 표시하지 않는다.
