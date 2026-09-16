# Return Network Persistence Foundation

Status: READY FOR IMPLEMENTATION / NOT TESTED  
Baseline: `master` after the v0.5 prologue/Hard Breach documentation alignment  
Scope: persistence only. No world/NPC adapter, no presentation, no sound/light, no automatic event activation.

## Goal

Persist the existing immutable `ReturnNetworkState` as world-local narrative state and make every permanent transition obey:

`draft -> integrity validation -> durable atomic story write -> publish/effect`

A presentation or world/NPC effect must never become visible before the corresponding durable state has been committed.

## Required schema change

- `NarrativeData.SCHEMA`: `4 -> 5`.
- `StoryStorage` must accept save formats `1..5`.
- A new world reports save format `5`.
- `actorSchema=1`, art revision 2, and mod version `0.3.1-dev.1` remain unchanged.
- Schema 1..4 worlds migrate with `ReturnNetworkState.notStarted()`.
- Schema 5 may contain a world-local `returnNetwork` compound encoded by `ReturnNetworkState.save()`.
- If `returnNetwork` exists with the wrong NBT type or fails `ReturnNetworkState.load()`, loading must fail closed and preserve/quarantine the original through the existing StoryStorage guard.
- Unknown newer save formats remain `UNSUPPORTED`.

Why schema 5 is required: an older schema-4 build does not understand `returnNetwork`; allowing the new field under schema 4 would let an older build load and later rewrite the file while silently deleting Return Network progress.

## NarrativeData contract

Add one world-local value:

```java
private ReturnNetworkState returnNetwork = ReturnNetworkState.notStarted();
```

Provide a read accessor. Do not expose mutable collections or mutable internal state.

Loading:
- schema `< 5`: use `notStarted()`.
- schema `5` + no `returnNetwork`: allow `notStarted()` only as an explicit optional/unstarted state.
- schema `5` + `returnNetwork`: require `TAG_COMPOUND`, then use `ReturnNetworkState.load()`.
- Any semantic contradiction from the Return Network codec is a load failure, not a repair opportunity.

Saving:
- always emit schema `5`;
- persist `returnNetwork.save()` under `returnNetwork`.

## Durable commit boundary

Do not add a plain setter that immediately publishes live progress.

Implement an explicit commit path whose semantics are:

1. caller supplies the current expected Return Network state and an immutable draft;
2. verify the live state still equals the caller's expected durable state (stale-draft/CAS guard);
3. validate the draft by round-tripping or equivalent direct semantic validation;
4. stage the draft in NarrativeData and mark dirty;
5. invoke the existing guarded synchronous `NarrativeData.save(...)`;
6. only after the atomic write returns successfully may the new state be treated as committed and any presentation/world/NPC effect be triggered;
7. on write failure, do not acknowledge the transition and do not trigger effects; leave recovery state explicit and fail closed.

No attempt in this milestone may infer Yeoul identity, repair structures, move NPCs, or manufacture missing permanent facts.

## Load/store invariants

- World-local, not per-player.
- One active Return Network record.
- `ReturnNetworkState` remains the canonical semantic validator.
- `NOT_STARTED` has no event identity/location/participant.
- Once discovered, event instance and location never rebind.
- Once Yeoul joins, participant identity/generation never rebinds.
- Wait states are observations only and never roll back durable facts.
- Unknown optional keys inside the Return Network record remain tolerated only where the existing codec already permits them.
- No partial checkpoint is promoted merely because a client saw an effect.

## Required tests

Keep the existing seven Return Network contract GameTests.

Add persistence tests covering at minimum:

1. schema-4 story data loads with Return Network `NOT_STARTED`, then saves as schema 5;
2. every permanent Return Network semantic boundary round-trips through `NarrativeData`;
3. schema-5 malformed/non-compound `returnNetwork` is rejected;
4. semantically contradictory Return Network NBT is rejected through the normal fail-closed load path;
5. newer save format (`>5`) is `UNSUPPORTED`;
6. failed durable write never reports a successful Return Network commit;
7. stale expected-state/double-submit cannot overwrite a newer committed state;
8. successful commit is visible after a fresh process/world reload, not merely from the in-memory object.

## Required verification before PASS

Run Gradle tasks serially, following `AGENTS.md`.

At minimum:
- compile/build;
- all existing GameTests;
- new Return Network persistence tests;
- existing load-guard/recovery regression tests.

A process exit code alone is not PASS. Preserve named gate markers and evidence.

This milestone remains PARTIAL until a fresh-process reload proves committed Return Network state survives disk persistence.

## Explicit non-goals

Do not implement in this milestone:
- relay block discovery from the live world;
- real Yeoul/NpcState cross-check;
- NpcNavigation arbitration;
- player interaction;
- chat dialogue;
- sound, lighting, particles or camera effects;
- Fear Director / Audio Director;
- Scenario / Memory Presentation;
- opening cinematic;
- item/reward transactions;
- structure fallback.

The next milestone after persistence PASS is `Return Network World/NPC Adapter`.
