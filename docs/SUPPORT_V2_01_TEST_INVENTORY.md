# V2-01 support test inventory

## Existing baseline inspected before implementation

| Existing coverage | Reuse decision |
| --- | --- |
| `ActorGameTests.unobservedArrivalTerminatesWithoutPlayerKnowledge` | Reuse termination and Actor NBT separation. Do not duplicate the test. |
| `NpcGameTests.settledResidentHasDayWorkNightRestWithoutNewFacts` | Reuse daily time boundaries and ordinary-talk cooldown. |
| `ReturnNetworkIntegrationGameTests.networkAllDurableBoundariesWithNpcReceipts` | Reuse checkpoint, identity, receipt, disk readback, and duplicate completion tests. |
| `networkCorruptionMatrixRejectsPermanentInference` | Reuse permanent-fact corruption rejection. |
| `ReturnNetworkSupportGameTests` | Reuse diagnostic load matrix and immutable identity/facts. Do not call these actual chunk tests. |
| `ReturnNetworkPlaybackGameTests` and pure signal tests | Reuse emission, observation, restart, and replay contracts. |
| Schema5, transaction, preservation, Opening tests | Retain all existing tests and protected storage code. |

Fresh support baseline: 114/114 GameTests, 176858 core assertions, and build PASS.

## Added gaps

- Nine observation-policy GameTests cover positive accumulation, tick-eight confirmation, range, facing, LOS, interruption, and original predicate equivalence.
- Three knowledge GameTests cover notification eligibility, idempotent observation persistence, and neutral/read-only revisit selection.
- Three ownership GameTests cover completed delegation, ordinary/context dialogue, and unchanged participant/facts.
- One headless negative integration GameTest uses the actual Wayfarer and Minecraft's supported creative mock ServerPlayer.
- Twenty-one evidence-validator tests cover completeness, statuses, metadata, references, atomic output, and human-field protection.
- Ten log-draft tests cover byte preservation, target hashes, existing-output protection, and prevention of inferred acceptance.

The expected GameTest count is 130. Only the execution log establishes the actual count and result.

## Automation limits

Policy tests prove calculations and read-only selection. They do not prove a person's screen view or understanding.
The entity test proves the creative guard through actual ticks and Actor NBT persistence. Its connection is not a real client.
The supported mock overrides `isCreative()` to true. Changing game mode does not produce a survival observer.
An initial positive integration fixture failed for this reason. Its failed log is retained.
No custom fake-player subclass or internal player-list hack was introduced to bypass that guard.
Positive survival observation remains a client gate; the shared calculation is covered by policy tests.
The ownership tests do not prove live NPC navigation or actual dialogue delivery.
Station selector tests prove no record mutation. Production StationBlock diff review checks the absence of new world writes.
Actual occlusion/view handling, route readability, warnings, crisis, recovery, and human/audio experience remain client gates.

## Log ingestion continuation, 2026-11-01

The baseline Python gate passed 19 tests before this continuation.
The updated Python gate passed 31 tests.
The source and resources remain unchanged from support HEAD `e5c184bc367c7c3f26a1bd2b551075c07edf50ae`.
The prior 130 GameTest result remains previous support evidence. This continuation does not rerun that gate.
The inherited startup log produces 15 NOT_TESTED scenarios and no human or client acceptance claim.
