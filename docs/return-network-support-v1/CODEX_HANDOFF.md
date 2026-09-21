# Codex / Astra handoff — support V1

## Protected baseline

Remote branch was queried at support start: `work/return-network-integration` = `b5d26787e4532cffd6379a6ea1e4cc8b6898ebe8`; the commit exists locally and the worktree was clean. No later remote commit was observed at that check. `570c4f8` is inherited historical input only.

Main evidence remains PRE-EXISTING: 95 GameTests, 176857 Core assertions, 21 PASS / 22 real-client attempts, A–G cuts. The support report lists fresh automation separately. NEW SUPPORT REAL CLIENT = NOT RUN.

## Change classification

- SAFE_SUPPORT_IMPLEMENTATION: new pure signal planner/observer/session, new derived load diagnostic, standalone tests, new GameTests, static audit, package/probe scripts, this matrix/report.
- PROPOSED_PRODUCTION_PATCH: none applied. The helpers have no runtime callers and introduce no story fields.
- CODEX_ONLY_CHANGE: actual signal carrier integration, live readiness observations, production update/commit placement, world-state policy, actual-client validation, seal/release decision.

## Integration hazards to review first

1. **Same chunk geometry:** relay and arrived NPC stand are both `(2,2)`. Distinguish entity-storage readiness from block-chunk readiness. Do not claim model truth tables prove physical isolation.
2. **Stale cached wait:** no live NPC means no `ReturnNetworkEvents.update` invocation; cached wait can be stale. Support diagnostics explicitly cannot confirm production enum state.
3. **Lighting is partly structural:** `activatedEquipment` requires `LIT=true`. Pulsing that property OFF would fail environment readiness. Select an independently transient carrier; do not alter the stored physical meaning.
4. **No implicit new observation semantics:** signal command emitted is not perceived; full observer match is only a transient observation result. Only existing MAIN code owns cp3/cp5 commits. Do not add durable EMITTED/OBSERVED flags from this helper.
5. **Clock semantics:** support ticks are injected tuning values, not Minecraft canon or wall-clock timestamps. Readiness loss restarts a whole semantic presentation rather than serializing half a pulse. Evaluate render/server tick cadence and dropped frames in a real client.
6. **Commit order stays unchanged:** event/NPC envelope → sync write → readback → hash → memory publish. No support helper calls commitReturnNetwork or writes a world block.
7. **Exact dispatch contract:** a missed planned pulse start enters RESTART_REQUIRED; it never emits a burst of overdue pulses. Use a new session identity and a full new timeline. Call session.observeMatch only after COMPLETED; the static matchExact is a structural comparison, not proof of completed perception.

## Resume sequence

1. Read REPORT.md and artifact SHA256; inspect support diff against the recorded remote baseline.
2. Run `python tools/ci/audit-return-network-closure-support.py --self-test`, then the audit; run the documented Java tests and Gradle checks on the selected branch tip.
3. Execute independent NPC/relay loading recipe from FAILURE_MATRIX.md with actual chunk/entity evidence; test environmental/path/lighting variants without force loading or deleting player blocks.
4. Execute existing return-light actual-client regression: first interaction, home arrival, shared_return_light, relationship, maintenance, fresh-process reload and relevant death/chunk edges.
5. Integrate and tune SHORT → SHORT → LONG using the helper only after reviewing its API and replay policy. Preserve accessible non-audio information, no camera control or new monster.
6. Test first-short/second-short/pre-long interruption, cp5 committed reconnect, completed aftermath and sound/visual failure without durable duplication.
7. Review scene readability and response observation, run final risk-based regression, then decide first-event seal. Support completion is not event closure or 0.3.2 approval.

Item/Reward/Structure Fallback, Scenario/Memory Presentation, threat foundation and opening production remain subsequent MAIN roadmap decisions, not changes in this support bundle.
