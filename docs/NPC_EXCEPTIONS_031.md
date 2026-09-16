# NPC exception stability — death/respawn working slice
Current: 0.3.1-dev.1 / npc-exceptions / PARTIAL. NPC exception sealing remains PARTIAL; composite NOT STARTED.

## Immutable input
npc-lifecycle JAR 80a80cf1dd741744c4b6c7c2bed21264437d651c5421d9198f13b40e3ca79e51; source eba768dec2a1784f71fe371573a92daa9f1cc92b8f3a5cb40463cf9d958ff736. Both copied and rehashed under evidence/npc-exceptions/baseline. Do not run package-npc.py against this adopted baseline again. Only package-npc-exceptions.py publishes this working slice.
Minecraft 1.21.1 / NeoForge 21.1.249 / save format 4 / optional actorSchema 1 / city art revision 2. No final art/resources/content expansion.

## Reproduced production defect
Actual Death1Off, NPC_A_PREPARED: player killed at checkpoint 0, remote respawn saw checkpoint 1. Literal failure: death advanced checkpoint 0 -> 1. Root cause: NpcEvents.begin continued spawn after reservation regardless of living witness; common StoryActors.ownerPresent treated owner=null as unconditional permission to reconstruct an unfinished NPC. This conflated player-independent identity with event execution eligibility.

Fix: NpcEvents.witness selects a living nonspectator in the actor's actual dimension within 24 blocks. NpcEvents.hasWitness bypasses presence only for completed checkpoint 4 residents. NpcEvents.begin checks this after reservation, retaining PREPARED/UUID/generation when no witness remains. StoryActors.ownerPresent delegates only the existing NPC adapter to this context gate before the generic null-owner rule. Reconciliation still distinguishes unloaded storage from confirmed loss and keeps generation reservation semantics. NpcEvents.update uses the same eligibility rule rather than a nearest player that could be dead/spectator. No player ownership was attached to Yeoul and no NPC was moved to the respawn point.

NpcState, StoryActorRecord, serialization and migration meanings unchanged. Identity/experience/relationship/residence persist. Runtime path is not serialized as permanent history. The existing work transition check_return_light -> maintain_return_light remains. General future routine switching and detailed residence/path obstacle classification are not implemented by this slice.

## Harness and evidence
NpcExceptionSmoke is opt-in, isolated. D1..D5 target reservation, appearance, actual movement after committed dialogue, observation committed before completion, and completed life. Both KeepInventory settings use actual kill/DeathScreen respawn plus actual diamond inventory count. Respawn is in Overworld at a fixture platform far from the quiet_city NPC; the NPC retains whileaway:quiet_city binding, not a fabricated Overworld origin. NPC is never teleported. Before death, remote absence and completed story NBT are separate snapshots. A living remote respawn is held 120 observed server ticks without checkpoint/fact/work changes. Return must complete through NPC navigation and actual return-light observation. D5 worlds are opened by a new PID, canonical/identity/facts checked again. Repeated completed interaction must not append history.
Two new GameTests cover absent PREPARED reservation and active resident suspension without facts/generation changes. Existing 56 tests remain unchanged. New separate regression tasks reuse NpcSmoke A-E in new profiles/evidence only; old evidence is not overwritten.

ClientTestEnvironment now records PID before environment validation and includes actual/expected bounds on failure. It does not weaken mute or right DISPLAY1 requirements. All client runs remain serial and pre-muted.

## Development/environment history
Pre-fix Death1Off failed as expected and is preserved in development-death1-before-fix, with the original failed world outside the final profile. The first post-fix launch ended before title/environment gate; marker remained RUNNING and PID was not recorded. Startup log/world preserved in startup-interrupted-attempt; root cause unconfirmed and not a completed gameplay test. At usage 92% a safe pause was announced. The packaging permission review was then rejected because usage was exhausted; that tool action did not execute. After explicit user go and usage reset (five-hour 0%, weekly 31%), the same copy-only rollback request was approved and executed. No workaround bypass and no computer shutdown.

## Required remaining gates
Until current evidence proves them: actual NPC chunk unload/reload, Nether/End at multiple checkpoints, dimension+restart, divergent NPC world copy, residence missing/blocked/conflict/unreachable, path obstruction/reopening, missing/replaced/restored return light, representative NPC-corruption clients, broader runtime work transitions remain NOT TESTED or NOT IMPLEMENTED. Do not promote death tests into those gates. No NPC overall PASS or composite implementation before the complete exception table is closed.

## Routing and usage
No Terra/Luna delegation or routing pilot. No Caveman. No per-model token savings measured. Check both usage windows at major boundaries; near depletion finish a coherent save/recovery boundary, save artifacts and exact next step, then pause. Automatic shutdown is disabled.
