# 귀환망의 첫 응답 — contract slice (not a playable event yet)

Current: 0.3.1-dev.1 / return-network-contract / PARTIAL.
Input: immutable npc-path JAR db567920ac9240a375a68170868cc6410dcde0a397d541a805e345e3ae88501a,
source 5b5460ee0eab81fffe8cadbe20b3a446b2af91bad385abfd32f52aca47d695c1.

## Scope and activation gate
ReturnNetworkState implements an immutable semantic state + strict standalone NBT codec.
It is NOT registered in NarrativeData, StoryStorage, NPC AI, world generation, or interaction handlers.
No player can trigger it in this slice. Save format4, actorSchema1, art2 and every existing production
class remain unchanged. Contract schema1 is local to the unregistered codec, NOT a deployed save extension.
Do not claim contract tests prove actual player interaction, crash durability or live corruption handling.

## Meaning and intended play
After Yeoul's return-light checkpoint4, shared experience and relationship are confirmed, the player
finds an old small railway relay, joins the same Yeoul, compares its rhythm with the return light,
intervenes through an actual lever/control, then observes a distant response. A responded relay and
a small Yeoul maintenance/reaction change remain. No item ownership, reward delivery, replacement
structure, camera lock, monster spawn or loud stinger is involved.

Evidence A: OLD_PATTERN_EVIDENCE — the old contact repeats a pattern; its author remains uncertain.
Evidence B: PATTERN_MATCH_EVIDENCE — the current return-light rhythm corresponds to the response.
Neither identifies the Continuity Graph, synthetic last day or the nature of Yeoul.

Foreshadow candidate: RETURN_NETWORK_CURRENT_PATTERN.
First reading: an old installation happens to echo Yeoul's signal.
Later reading: a new present-day relationship/action is being registered by the continuity network.
No Foreshadow Registry or final text/asset is implemented in this slice.

## Checkpoints and crash contract
|Checkpoint|Stage|Durable meaning|
|---|---|---|
|0|NOT_STARTED|No event/location/participant/facts|
|1 / A|DISCOVERED|Event instance, location identity, discovered signal|
|2 / B|NPC_JOINED|Yeoul storyId, instance, entity UUID, generation|
|3 / C|SIGNAL_OBSERVED|Old pattern evidence; structure expected READY|
|4 / D|INTERVENTION_COMMITTED|Intervention fact; structure expected ACTIVATED|
|4 substep|PRESENTING|Presentation has begun; response observation is NOT inferred|
|5 / E|RESPONSE_OBSERVED|Response and correspondence evidence; structure RESPONDED|
|5 substep / F|SHARED_EXPERIENCE fact|Shared experience committed but event NOT yet completed|
|6 / G|COMPLETED|Completion + aftermath + next prerequisite in one draft|

F deliberately does not advance the checkpoint. This prevents confusing Yeoul's shared-experience
commit with the later terminal commit. Stage, checkpoint, required facts, structure and presentation
must agree on load. Missing evidence is rejected, never reconstructed from the checkpoint integer.
Repeated already-completed transitions are no-ops; participant rebinding is rejected.

## Permanent vs runtime
Permanent draft: event/location identity, participant tuple, facts, expected structural state,
presentation receipt state. `PRESENTING` is a receipt, not an animation frame or timer.
Runtime-only: navigation nodes/retry clock, scene elapsed ticks, particles, player proximity,
WAITING_NPC / WAITING_ENVIRONMENT / WRONG_DIMENSION / PLAYER_ABSENT.
The `waiting` method is read-only. It neither decrements checkpoints nor changes generation.

## Mandatory next integration work
1. Introduce a world-local optional record in NarrativeData only after designing strict extension
   schema handling in StoryStorage. Unsupported/corrupt data must remain guarded/quarantined.
2. Build a draft -> validate participant and event -> durable atomic save -> publish/effect boundary.
   Do not mutate the live committed object before successful persistence. This slice has NO disk commit API.
3. Validate draft structure expectations against actual loaded blocks. Physical mismatch is waiting,
   not automatic fact rollback or player-block overwrite. Reserve/commit world effects explicitly.
4. Resolve Yeoul through current Actor identity and NpcState facts; do not trust caller booleans as
   sufficient production proof. Use NpcNavigation without allowing return-home and relay goals to fight.
5. Define the relay location using existing city/rail infrastructure. No automatic fallback or art revision migration.
6. Add actual block interaction, two accessible evidence cues, restrained sound/light and persistent aftermath.
7. Only then run actual A-G/new-process, death, chunk, dimension, environment, copy and corrupt-store tests.

## Gates
Contract/GameTest PASS can be recorded separately. All actual campaign integration, A-G process
crashes, physical structure commits, aftermath and live Load Guard tests remain NOT TESTED.
First complex event PARTIAL. Item/Reward transactions, Structure Fallback, final art,0.3.2 and Caveman HOLD.
