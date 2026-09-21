# Return Network closure support — failure matrix

Baseline: `b5d26787e4532cffd6379a6ea1e4cc8b6898ebe8`, branch `work/return-network-integration`.
This is a support test specification, not a new production policy. Final execution counts are in REPORT.md.

## Existing behavior inspected before implementation

- `ReturnNetworkEvents.update` runs through the live StoryNpc; no loaded NPC means no execution of that updater. A cached `waitState` may remain stale. A derived diagnostic of WAITING_NPC is NOT evidence that production actually wrote that enum.
- A loaded NPC first requires a living eligible witness, then loaded/valid environment, then path arrival. Navigation AVAILABLE waits for NPC; other non-arrival paths wait for environment. No permanent state is rolled back by these branches.
- `environmentReady` reads chunk readiness first. Before cp4 it requires original copper; from cp4 it requires the existing relay with `LIT=true`.
- RELAY `(42,68,46)` and arrival STAND `(44,65,47)` both occupy chunk `(2,2)`. At arrival, NPC-only/relay-only physical chunk states cannot be represented by pretending those are separate chunks. Entity-storage readiness can still differ from block readiness.
- Current cp5 response facts are committed before the bell. The support observer never turns receipt of a sound command into human observation or a new story commit.

## Independent loading

All rows preserve the exact event instance, participant binding, generation, facts and checkpoint. Pure/model GameTests demonstrate this contract, NOT actual entity-chunk deserialization or forced-unload behavior.

| ID | Components / transition | Expected diagnostic / resume condition | Durable mutation | Physical client |
|---|---|---|---|---|
| L-A | eligible player + NPC/entity ready + relay ready | readiness only; production path/environment still gate advancement | none from assessment | NOT RUN |
| L-B | player + NPC ready, relay unloaded | environment dependency wait | none | NOT RUN |
| L-C | player + relay ready, NPC/entity unconfirmed | NPC dependency wait; cached production wait may be stale | none | NOT RUN |
| L-D | player ready, neither dependency ready | wait with both missing reasons | none | NOT RUN |
| L-E | both missing → NPC first → relay | remain waiting until both ready | none | NOT RUN |
| L-F | both missing → relay first → NPC | remain waiting until canonical entity confirmed | none | NOT RUN |
| L-G | player returns before NPC | no fabricated NPC/binding | none | NOT RUN |
| L-H | player returns before relay | no fabricated equipment/response | none | NOT RUN |
| L-I | NPC chunk ready, entity storage not ready | unconfirmed is not confirmed loss | none | NOT RUN |
| L-J | entity present, identity not confirmed | diagnostic only; never select or spawn an actor | none | NOT RUN |

## Path, environment and light

| ID | Failure surface | Existing expected runtime behavior | Resume | Additional proof needed |
|---|---|---|---|---|
| P-1 | temporary block/player construction at stand | WAITING_ENVIRONMENT; preserve block | player removes obstruction | actual approach/path |
| P-2 | closed door / narrow passage | actual navigation result, not inferred from door appearance | real traversable path returns | actual path fixture |
| P-3 | unavailable route / temporary path failure | pause, bounded retry; no teleport | navigation becomes available | actual route changes |
| P-4 | 49-block target | OUT_OF_RANGE; never widen 48-block rule | target legitimately within range | existing 30/49 GameTest retained |
| P-5 | player outside interaction reach | no intervention commit | player approaches again | MAIN fixed harness, not reach rule |
| P-6 | player pushes/blocks NPC | do not replace identity or remove blocks | route/arrival restored | actual collision test |
| P-7 | chunk edge / incomplete corridor | CHUNK_UNAVAILABLE, no forced chunk request | natural dependency load | actual chunk lifecycle |
| E-1 | missing relay / wrong block | environment wait and diagnostic, no regeneration | explicit user restoration/reapply | block GameTest + actual follow-up |
| E-2 | obstructed feet/head or water | environment wait, no deletion | user clears obstruction | block GameTest + actual follow-up |
| E-3 | activated-looking relay before cp4 | physical look does not invent story facts | restore expected environment | existing GameTest retained |
| V-1 | activated relay unlit | currently invalid environment, NOT a harmless cosmetic failure | restore valid relay through existing policy | block GameTest |
| V-2 | relay block unloaded | wait without reading/creating missing terrain | natural load | actual unload needed |
| V-3 | unrelated player light interference | do not rewrite player lighting; no new facts | observation/readability assessment | NEW REAL CLIENT NOT RUN |
| V-4 | presentation carrier unavailable | discard/restart transient presentation only | carrier ready and explicit replay policy | pure signal tests; runtime integration deferred |

## Semantic recovery and existing NPC

| Situation | Checkpoint / durable mutation | Resume policy / status |
|---|---|---|
| interruption after first SHORT | no timer/pulse serialized | fresh support session restarts full pattern |
| interruption after second SHORT / before LONG | no partial match | new observation collection; never concatenate sessions |
| disconnect/death/dimension departure | support model changes no event state | carrier readiness supplied by MAIN; existing 21 clients are prior MAIN evidence |
| cp5 already durable | response/evidence/shared facts unchanged | optional explicit aftermath presentation, no story callback |
| duplicate poll/interaction | no duplicate semantic facts | pure sequence emission dedup; existing commit idempotency untouched |
| completed-event interaction | no new campaign sequence | standard replay suppressed; explicit ambient replay separately identified |
| shared_return_light / first dialogue / relationship / home / work | original receipts retained | serializer/Integrity GameTests only; new actual NPC client NOT RUN |
| participant/shared/aftermath inconsistency | existing rejection remains authoritative | no inference or repair by support |

## Actual-client fixture recipe — AUTHORED / NOT RUN

1. Copy an isolated MAIN world; record world-local NBT hash, NPC UUID/instance/generation and event cp before mutation. Never use the user's original save.
2. For physical NPC-only/relay-only combinations, first prove distinct relevant chunk coordinates. At the current arrival location they coincide; use the earlier natural approach stage when spatially separate, or label entity-storage fault injection explicitly. Do not relabel same-chunk block availability as two independent chunk unloads.
3. Observe `hasChunkAt`, `areEntitiesLoaded`, actual canonical lookup and real player witness eligibility over multiple loaded observations. Do not force load, replace the NPC, or mock isAlive as proof of unload.
4. If normal player tickets make the requested combination unattainable, record NOT REPRODUCED and have MAIN choose a legitimate fixture; do not falsify flags or weaken the production guard to make a PASS.
5. Hold the valid partial-loaded condition, verify exact unchanged NBT facts and identity, let the missing dependency naturally return, then prove progression and exactly one canonical actor.
6. Repeat reload order, player return order, actual approach obstruction, closed door/narrow route, changed relay, and source lighting interference. Restore only blocks the fixture itself changed, never production auto-repair.
7. Complete, exit, verify PID termination, fresh-process reopen, compare disk and Integrity. Right DISPLAY1, muted isolated profile. These steps are authored handoff, not executed support client evidence.
