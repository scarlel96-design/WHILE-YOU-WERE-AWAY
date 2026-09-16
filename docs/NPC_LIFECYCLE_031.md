# NPC lifecycle slice — implementation contract

Current: 0.3.1-dev.1 / npc-lifecycle / PARTIAL. The previous usage-threshold shutdown was revoked. A later instruction adds usage-aware safe pause without shutdown. No automatic shutdown remains armed.

## Baseline and scope
Input is immutable canonical-recovery JAR 82b0201c0067bc50fe931e0daf24b54c2bef31cd07e2cc63845ae4677beca542 and source ZIP 987953a2ceaa5edd0a97f5c285015d3e5611e685566182618b6910d6857eaaee, copied to evidence/npc-lifecycle/baseline. New distributions belong only in dist/npc-lifecycle.
Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21; save format 4; actorSchema optional 1; art revision 2. No 0.3.2.

## Real event
Yeoul is a registered whileaway:story_npc PathfinderMob, not a Villager fixture. When a living non-spectator approaches the completed city's arrival area, the world-local npc:yeoul reservation is created once. The NPC waits for interaction, walks to a place beside the existing return light, observes that actual block, and starts a new maintenance routine. The block must actually exist; no success by elapsed timer. No block edits or forced NPC teleporting are used to clear a route.
Placeholder appearance references Minecraft's villager texture/model without copying the asset. The placeholder register includes its final replacement requirement. No final skin/model/art production occurred.

## State and commit sequence
0 PREPARED: UUID/instance/home/goal/work reserved atomically before spawn.
1 PHASE_1: actual entity appeared; waits for player interaction.
2 PHASE_2: introduction receipt and met-player/shared-conversation relation committed before the line; actual pathfinding to residence.
3 RESOLVING: arrival and actual return-light observation committed; completion has not yet been confirmed.
4 COMPLETED: settled fact, shared-event relation receipt, next-link receipt, and new work state committed together. Actor stays ACTIVE, non-temporary and unowned; finishing the event does not retire the resident.

NpcState stores its contract in the existing actor facts set. npc.home/goal/work are single-valued state fields; met/dialogue/arrival/observed/relationship/settled/link are event receipts. Work deliberately changes from check_return_light to maintain_return_light. It is not a lost permanent event fact. NPC and player counterpart identifiers share the relationship key format, but no full NPC-to-NPC simulation is claimed.
The initial facts initializer is a common StoryActors.prepare overload. It runs before the actor is registered or any reservation is saved, so failure cannot leave a half-initialized NPC. Existing call sites use an empty initializer and retain their former behavior.

## Recovery and integrity
Identity, generation, spawn retry and stale-generation decisions remain in the existing Actor layer. NPC code does not select canonical entities. StoryStorage applies NPC semantic validation after normal parsing and blocks contradictory residence/completion facts. StoryActors.integrity includes those diagnostics. AI and interactions respect the existing write/activation guard.
No new top-level schema was introduced. Existing actor facts serialization retains these strings; new readers add NPC-specific validation. NPC-free legacy worlds do not invent NPC records on load. They may discover the event normally on entering the city. JAR rollback is copy-only; removing a registered entity type from an NPC world is not a supported save downgrade.

The essential resident currently rejects ordinary damage. This is an explicit provisional gameplay policy, not evidence for void/command removal, every modded damage source, or a finished NPC death/rescue system. Absence suspends event movement without failing the story. Completed residents can keep their loaded routine without a player owner. Obstructed routes wait rather than destroying player builds; robust alternate residence/path recovery is still pending.

## Verification separation
Four new GameTests exercise relation/residence NBT, conflicting home/premature link detection, initializer failure atomicity, and a completed real StoryNpc remaining canonical. All 52 prior GameTests remain unchanged. The storage probe reruns prior 580 cases; those are not 580 new NPC cases.
NpcSmoke is a scripted integrated-client fixture. Seed constructs the actual city as a spectator (no NPC trigger). Five copies independently run A/B/C/D/E cuts, each followed by a new process. Gameplay NPC creation uses the normal proximity hook; interaction calls the actual mobInteract entry point. Only the fixture player is positioned by the harness. This is not natural survival, human mouse-input QA, or general NPC exception certification.
Normal milestone reports must distinguish game tests, actual clients, PID exits, saved NBT, and copied seed preservation. Never promote the historical canonical 35 processes into this slice's current evidence.

## Remaining and next
NPC A/B/C/D/E is one bounded milestone. Overall NPC stability stays PARTIAL until NPC-specific player death, chunk unload/reload, Nether/End travel, divergent copy-world progression, missing/blocked residence/return-light recovery, and load-guard/canonical integration risks are tested. Natural survival and composite/item/reward/fallback systems remain NOT STARTED/NOT TESTED as applicable. Next work starts with the real NPC exception matrix, not a composite event or final art.
Caveman remains HOLD. No Terra/Luna delegation or model-routing experiment; no token savings measured.

## Usage-aware pause
User added safe pause near either active usage limit. Observed five-hour usage 96%, weekly 15%. Complete the in-flight C recovery, then stop before D; preserve all evidence. The Gradle gate PAUSED_AT_SAFE_BOUNDARY deliberately prevents a new client, and is not a gameplay test failure. Automatic computer shutdown remains disabled. Resume only after checking available usage and removing the explicit pause marker; D/E are unverified until actually run.

Observed pause correction: D was already dispatched before the newly edited Gradle gate was loaded. CutD and ResumeD finished normally; CutE was then blocked before any client PID existed. Original command history and pause log are retained. Resume on 2026-09-14 found five-hour usage 2%, weekly 16%; remaining work is CutE/ResumeE. The runner now also checks the pause marker before dispatching a new pair (in addition to the Gradle gate).

## Additional environment attempt
First ResumeE launch failed before world opening: window outside requested right monitor. Failure recorded no PID (PID recording follows environment verification); this was a launched client, not a blocked dispatch. The failing bounds were not captured, so displacement cause is unconfirmed. No java processes remained at inspection. Story file still matched the CutE snapshot (5810ecef0dafdbbcffe346fbb5a2d512a938ef408534c594cbed65153daaa6e0). The failure and unchanged disk proof are retained under environment-resumeE-attempt1. Retest uses the same world and unmodified runtime/strict environment gate.

## Verified closeout on 2026-09-14
CutE and the repeated ResumeE completed. Final evidence now contains 11 successful distinct exited client PIDs: Seed plus five cut/restart pairs. The first ResumeE environment failure remains separately preserved (not counted as PASS). All five pairs passed independent saved-NBT checks and unchanged seed manifests; final NPC UUID/instance/generation survived, generation remained zero, checkpoint reached four and current work became maintain_return_light. NPC A/B/C/D/E PASS; NPC full stability and campaign PARTIAL. Next first task is NPC-specific player death/respawn, followed by actual chunk unload, Nether/End and divergent copied-world progression.
