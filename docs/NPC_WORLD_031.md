Current: While You Were Away / 0.3.1-dev.1 / npc-world / PARTIAL
NPC world travel and copied-world divergence verified; NPC environment/corruption closure remains PARTIAL.
Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21.0.5 / save format 4 / optional actorSchema 1 / art revision 2 unchanged.
No 0.3.2 promotion, composite event, final art, resource pack, new monster, or Caveman use.

BASELINE input JAR: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\npc-world\baseline\previous.jar
SHA256 19636c1b8d8c8b9faf7693f5c05e69b199bb896c4f131c7f72c34331ca099c19
BASELINE source: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\npc-world\baseline\previous-source.zip
SHA256 d34f61add10bcfd2ff44fe6cfc4034f74abab503d0f074eb0940b55646afeb4d
Immediate continuation snapshot (previous npc-world JAR/source) preserved at C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\npc-world\history\pause-20260914:
JAR f5b159a950233a7c2413d2dc0d8e7b4f65287d44f728a1cd79c4323f7c02d438
SOURCE 924bd5ca92bce9221751d684ae257113521703a6b866cab4234cf2eb716efa6a
Neither adopted npc-exceptions distribution nor original seed worlds were overwritten.

ACTUAL CHANGES
Previously prepared NpcWorldSmoke.tick was executed for remaining Chunk4, Nether1/2, End1/2, CutNether2/ResumeNether2. Existing Java runtime unchanged for these nine cases; RUNTIME_SOURCE.sha256 checks that source.
Added client/NpcCopySmoke.java: property whileaway.npcCopy is opt-in. Seed creates a real campaign NPC via existing adapter, saves checkpoint 1; AdvanceA uses real interaction/navigation to finish at 4; AdvanceB uses real interaction then removes the eligible witness to remain at 2; ReloadA/B use separate fresh processes. Identity, residence, work, facts, candidate uniqueness, and repeated interaction are asserted. No NPC teleport, fact edits, block rewrite or generation override.
build.gradle: isolated runNpcCopySeed/AdvanceA/AdvanceB/ReloadA/ReloadB tasks, muted profile work/npc-copy, mandatory right secondary DISPLAY1 bounds. Test-only spectator/creative changes apply only to fixture player.
tools/run-npc-copy.py: copies a closed seed directory twice, records full save-tree manifests before/after each process for the inactive world and seed; verifies child PID exit and named PASS marker. It refuses to overwrite old attempts.
tools/verify-npc-copy.py: compares saved NBT identity, different progress/work/facts and fresh reload preservation; not just in-memory assertions. COPY_RUNTIME_SOURCE.sha256 covers the current source with the new harness.
tools/NpcWorldFactProbe.java and probe-npc-world-facts.ps1: existing six semantic corruption cases plus two valid round trips, now explicitly retaining npc:doyun/shared_repair relationship through StoryStorage and disk reload. This is a storage probe, not a live NPC-to-NPC scene or actual corrupted-world client.
Package/verification tools validate both opt-in harnesses, baseline class equality, current build result, source manifests, artifact reopening, and JAR-copy-only rollback.
All 83 baseline classes remain byte-identical; 85 current classes = 83 baseline + NpcWorldSmoke + NpcCopySmoke. All 69 asset/data entries unchanged. Existing five GameTest source files unchanged.
No production NpcEvents/NpcState/StoryActors/StoryStorage semantics changed. No new schema, migration, environmental classifier, path policy or corruption recovery was introduced. No NPC relationship semantics were replaced with a single affinity score.

REAL MINECRAFT EVIDENCE
Travel: 9 processes total, of which Chunk1/2 were verified before this continuation; 7 newly executed. Checkpoints 1/2/4 actually unloaded their entity/record/snapshot/home/light chunks and entity storage for 100 observed ticks. Snapshot/registered/residence anchors in these fixtures share chunk [2,4]; this does not prove mutually distant anchor chunks. currentTarget denotes adapter residence goal, not serialized path nodes.
Nether and End each tested before conversation and during actual movement. Identity/generation/checkpoint/facts/work preserved while absent; other dimension has no StoryNpc. Return yields exactly one loaded canonical NPC and natural completion.
CutNether2 PID20904 terminated after durable absent-state save. ResumeNether2 PID34532 loaded in Nether at checkpoint2 with UUID0811d8cc-6eae-40cf-acbe-418425268962, generation0, then returned and completed at4 with the same UUID and instance. This is a real process-restart pair.
Copy: 5 new processes. One saved real NPC copied into A/B with the same UUID/instance. A completes at4 with maintain_return_light, B remains2 SUSPENDED with check_return_light. Both reopen in fresh processes and retain divergent state. Full inactive A/B and seed save trees remain byte-identical during the other world's run.
All clients muted before startup and asserted on DISPLAY1 right secondary. No natural survival test claim. 12 new processes this continuation, 14 total in npc-world.
Travel process durations total=830.555s; copy total=297.797s. These are launch-to-exit durations, not human playtime.

VALIDATION
Baseline runGameTestServer: 58 required tests; exit0 (earlier preserved baseline-tests.log).
Current ./gradlew.bat --offline compileJava runGameTestServer build: All 58 required tests passed :); PASS core assertions=176857; BUILD SUCCESSFUL in 49s; exit0; tests-4.log. Existing tests retained, not replaced with weaker cases.
pwsh -NoProfile -File tools/probe-npc-world-storage.ps1: PASS load guard cases=580 assertions=2161 repeats=20; blocked matrix=19; sourcePreserved=true; actualClient=false; exit0. Fresh execution, including migration 1/2/3 to4, existing format4 and actor extension.
pwsh -NoProfile -File tools/probe-npc-world-facts.ps1: PASS NPC storage cases=8 semanticBlocked=6 normalRoundTrips=2 npcCounterpartRoundTrips=2 originalsPreserved=true actualClient=false; exit0. Six corrupt fixtures stay blocked with original/quarantine byte equality after attempted saves; do not promote this into actual Minecraft corruption PASS.
python tools/verify-npc-world.py: PASS NPC world travel: actual unload/reload 3 checkpoints; Nether/End each 2; Nether cut/fresh process; identity/facts/disk/seed preservation; NPC overall PARTIAL; exit0.
python tools/verify-npc-copy.py: PASS REAL COPY WORLD: one saved NPC split into A/B; same identity; A completed/B suspended; separate fresh processes preserve different facts/work/checkpoint; inactive trees byte-identical; exit0.

FAILURES AND REWORK
1. build.gradle parse failure at line460, tests-2.log, exit1. Cause: tool insertion selected the run-definition block instead of the final task-configuration block and duplicated an invalid outer section. Preserved failed file under failures/. Rebuilt exactly the two bounded blocks from preserved source ZIP, with last matching top-level configuration. No client ran from this configuration.
2. NpcCopySmoke compilation failed with missing GameType symbol, tests-3.log, exit1. Cause: GameType belongs to net.minecraft.world.level, not the imported net.minecraft.world package. Added explicit import, refreshed copy source manifest. tests-4.log then built and passed58/Core176857; all5 copy clients then passed. Two development corrections, no production policy correction.
3. Rollback helper failed to launch in the initial execution context (child exit -1073741502; wrapper exit1). Preserved failures/rollback-launch-context.log, confirmed the target was still the modified JAR copy, and reran the same verified copy-only script with the approved execution context. Final rollback and restored policy check exit0. This is an execution-environment failure, not a story/save regression.
Expected CORRUPT/MANUAL_DIAGNOSTIC probe log errors represent successful fail-closed fixtures, not silent resets. No failures in the final actual client suites.

STORAGE AND MODEL
Format4 / optional actorSchema1 unchanged; migration semantics unchanged. Identity remains independent of runtime path. Fail-closed/write guard unchanged. Original corrupted probe data and same-hash quarantine preserved. Existing death/A-E client PASS is baseline evidence, not rerun this slice. Full final NPC regression awaits environment/corruption work.
Only primary agent performed implementation and review; no Terra/Luna/subagents, Caveman HOLD/off. No model-token attribution or savings experiment. Usage observed at resume: five-hour12%, weekly48%; at copy batch checkpoint: five-hour58%, weekly56%; after copy verification: five-hour66%, weekly57%. Account percentages are not per-model token measurements and no exhaustion claim is made.
JAR rollback is not save downgrade; no world files touched by rollback. No automatic shutdown.

NEXT EXACT START
After usage check, preserve this packaged npc-world baseline. Begin NpcEvents.update environment classification (residence valid/temporarily blocked/unreachable/missing/conflict; path chunk-unavailable vs blocked/invalid/missing), without deleting blocks, moving NPC identity or auto-completing. Add return-light missing/wrong-block/restored and environment save/restart tests. Then actual NPC corruption clients and combined final NPC regression. NPC must remain PARTIAL; composite NOT STARTED until all requested gates pass.

