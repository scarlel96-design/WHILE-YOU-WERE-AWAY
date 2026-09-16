# 0.3.1 canonical-recovery — implementation and verification contract

Current: 《네가 없는 동안》 0.3.1-dev.1 / canonical-recovery / campaign PARTIAL

This document describes implemented behavior. Final gate results are generated from the current logs in VERIFICATION.txt; no count in a historical report is reused as current execution evidence.

## Immutable adopted baseline

- dist/load-guard/whileaway-0.3.1-load-guard.jar
- SHA-256 a66cf05706329faab022af12d9ab016dc61b8e719724858f70c3449f5ae8ad03
- dist/load-guard/whileaway-0.3.1-load-guard-source.zip
- SHA-256 c5d0161e2f436dcd8cb6c10e4654c84656e5a91cf1b96991c19907e8aa1f38b0
- Byte-identical copies: evidence/canonical-recovery/baseline/previous.jar and previous-source.zip.
- Minecraft 1.21.1, NeoForge 21.1.249, Java 21.0.5; save format 4, optional actorSchema 1, city art revision 2; no 0.3.2.

## Canonical identity and load boundary

ActorCandidates.identity checks storyId, eventId, instanceId, generation (including NBT integer presence), and role. The candidate query separately checks entity type, dimension, supported lifecycle, and Wayfarer witness. Position/Motion/health never establish identity. The query enumerates currently loaded entities, never force-loads a chunk, and requires registered-position and snapshot-position terrain and entity storage to be ready.

Outcomes: NO_CANDIDATE, EXACT_SINGLE_CANDIDATE, AMBIGUOUS_MULTIPLE_CANDIDATES, STALE_GENERATION_ONLY, UNLOADED_OR_UNCONFIRMED, IDENTITY_CONFLICT. No nearest/first/UUID-sorted winner exists.

CanonicalRecovery attempts missing entityUUID repair only for the currently supported Wayfarer event adapter with a surviving, consistent player encounter ticket. A malformed UUID is still blocked. A valid ticket is used only to construct a private preflight draft; the same strict StoryStorage validator must accept the full data. The draft is not installed in DimensionDataStorage or returned to gameplay. Generation never comes from snapshot tags.

Each missing binding must then have exactly one live candidate with the full identity and the same UUID as the event ticket. Two distinct server-tick observations must agree and both tracked chunks must have completed entity loading. Multiple candidates or identity conflicts latch MANUAL_DIAGNOSTIC for that server session. No candidate or unconfirmed chunks remain blocked without spawning or incrementing generation.

The original hash is checked again before strict final parsing and commit. Original-byte quarantine is verified before guarded atomic write; only then is the recovered data registered. A new process must load the repaired file through LOADED and retain UUID/instance/generation/checkpoint/facts.

StoryStorage preserves ABSENT/LOADED/RECOVERABLE_CORRUPTION/CORRUPT/UNSUPPORTED. ABSENT is the only public new-campaign path. The internal recovery path refuses missing/replaced source files and registered/poisoned live data. Runtime conflicts poison the existing NarrativeData write guard.

## Loading entities without running a broken story

StoryStorage.available checks the real loader; it never supplies substitute data. Story event ticks, scene ticks, city generation/horror, station tick, relevant item/block interactions and city travel pause when blocked. Wayfarer skips AI/tick progression and completion while blocked. Tagged entities may finish deserialization as recovery evidence; they are not activated as story actors before recovery succeeds.

This is a narrow story activation gate, not a promise that every third-party mod or every developer command operates normally in a corrupted world. A poisoned registered SavedData can throw during saveEverything; that failure is intentional and must preserve its previous file. Load-rejected data is never registered and unrelated vanilla save dispatch can proceed.

## Generation reservation, binding, and crash boundaries

The existing PREPARED + generation + entityUUID + event encounter ticket already expresses a durable spawn reservation. No second large journal was added. The defect was retrying that reservation by allocating yet another generation/UUID. PREPARED retries now reuse both, including after owner absence. A non-PREPARED replacement is allowed only after valid dimension, tracked chunk/entity-storage readiness, missing recorded UUID, no exact candidate/conflict, and the loaded reconciliation checks.

Replacement sequence:

1. Confirm missing within the tracked/loaded evidence scope; verify type and a collision-free placement without editing blocks.
2. Boundary 1: before generation reservation.
3. Increment generation once; reserve a UUID; set PREPARED; update the event ticket; persist synchronously.
4. Boundary 2: reservation saved. Old generations are fenced on join.
5. Spawn using that reserved UUID, not a newly assigned binding UUID.
6. Boundary 3: entity added, live binding not yet verified.
7. Verify canonical identity and observe/bind the preallocated UUID.
8. Boundary 4: memory binding verified, durable active binding not committed.
9. Flush Minecraft entity/world storage while the story is still PREPARED. This rare recovery flush is not performed every tick.
10. Set ACTIVE, capture runtime, persist binding. Boundary 5.
11. Restore Wayfarer runtime from checkpoint facts without resetting health/inventory/world blocks. Boundary 6.
12. Verify a single canonical candidate and confirm recovery. Story checkpoint/facts do not advance except the existing initial checkpoint-0 spawn rule.

The UUID in PREPARED is a reservation, not proof that an entity exists. At boundaries 3/4 the test deliberately flushes the real entity while leaving the story PREPARED to exercise the dangerous existing-entity/unconfirmed-binding branch. The six cut runs are paired with separate resume processes and disk checks, not inferred from the older A/B/C/D/E suite.

The exact Minecraft entity-storage API was inspected locally: processPendingLoads adds deserialized entities before setting ChunkLoadStatus.LOADED; areEntitiesLoaded tests that status; saveAll flushes permanent entity storage. Evidence: entity-storage-api.txt. The loaded query is not an exhaustive disk search of every unexplored chunk. Late entities outside the tracked positions are subject to the persistent generation fence and conflict guard, not silently adopted.

## Late stale entities and same-generation duplicates

Joining known older-family generations is canceled before AI or event ownership. Retired actor joins are also canceled. A conflicting same-generation UUID instead blocks story writes and retains evidence rather than choosing a winner. Other noncanonical identity conflicts block rather than silently becoming a replacement.

The late-load fixture is written offline to a separate copied world's entity region at chunk (600,600), using Minecraft RegionFile/NBT APIs. Its generation-0 actor is then actually deserialized when the real client visits that chunk after a generation-1 recovery. This is distinct from calling addFreshEntity on a stale object. The original generation-1 world remains unmodified by preparing this copy.

## Storage compatibility

No permanent field meaning changed and no new recovery schema is required: PREPARED already meant a persisted not-yet-confirmed spawn. Reusing its UUID/generation fixes retry semantics. Save format 4 and actorSchema 1 remain. Legacy 1/2/3 and actor-free 4 use the existing migration; missing/malformed generation remains blocked rather than inferred. Snapshot Motion default recovery and all prior fail-closed cases remain covered by the unchanged storage probe.

JAR rollback is tested on a JAR copy only. It restores load-guard behavior (including its missing-UUID manual block and old generation retry), not a world downgrade.

## Code changes and reasons

- ActorCandidates.java: read-only typed candidate evidence; separates identity/context/spatial readiness.
- CanonicalRecovery.java: private validated draft, live candidate/ticket agreement, distinct loaded observations, original-hash recheck, guarded commit; no spawn operation.
- StoryStorage.java: private preflight/final recovery path; public load remains fail-closed; activation query and runtime conflict guard.
- NarrativeData.java: explicit write poisoning hook; previous file/hash and atomic-save safeguards retained.
- StoryActors.java: candidate conflict guard, two-position readiness, reservation reuse, six generation boundaries, durable entity flush before ACTIVE commit, stale admission fence, duplicate diagnostics.
- Wayfarer.java: pause before AI during blocked load; extract idempotent checkpoint runtime restoration with canonical identity validation.
- StoryEvents/SceneRecovery/CityHorror/CityDistrict/CityTransit/StationEntity/NoteItem/RelayBlock: pause story entry points while the vanilla entity loader gathers recovery evidence.
- WhileAway.java: register recovery coordinator.
- CanonicalGameTests.java: four new tests; original ActorGameTests and StoryGameTests remain byte-identical.
- CanonicalSmoke.java, build.gradle, tools/run-canonical-*.py, LateActorFixture.java: isolated muted real clients, actual process boundaries and copied-world corruption injection.

## Failure and review history

- Missing UUID previously blocked startup before entity evidence could be loaded. Fixed by retaining load/write blocking while pausing story activation and allowing vanilla deserialization; no empty NarrativeData was introduced.
- PREPARED retry previously incremented generation again. Fixed by treating the already-durable identity/UUID as a reservation and persisting actual entity storage before ACTIVE binding commit.
- Owner absence could turn a pending PREPARED reservation into SUSPENDED, losing the reservation distinction. PREPARED is now preserved while absent.
- Same-generation conflicts were previously rejected on join without preserving an explicit ambiguity diagnosis. They now poison writes and pause AI; genuine older generations are separately fenced.
- Local tooling had a PowerShell brace-list parse error and a Python default cp949 decoding error. They were corrected without broad filesystem changes; source edits were checked and the full final compile/tests rerun. These are tooling failures, not a repaired-gameplay PASS claim.
- Six early development clients are preserved in canonical/ and canonical-commands.jsonl. They are not included in the final canonical-final suite.

## Unchanged scope and next stage

Design v0.4 remains the higher narrative contract. No new story chapters, NPC system, transactions, structure fallback, Horror Activation, final art, resource pack, sound, or setpiece was introduced. Caveman is HOLD/off; no Terra/Luna delegation or model-switch experiment occurred. No token savings are claimed.

Do not promote 0.3.2. Only after the final required Actor gates pass is the next implementation task a minimal real campaign NPC event, with the same lifecycle and A/B/C/D/E proof. Campaign stabilization remains PARTIAL even if the current Actor recovery gate passes.
