# MAIN closure — Phase A audit

Actual baseline: 7aa36c50ea0f13201304e0ad0f25c6b3f7a5edf3 on work/return-network-integration. Local HEAD and remote HEAD matched; initial worktree clean. No new remote commits observed. Original master worktree six staged deletions remain outside this work.

Preserved copies: evidence/return-network-closure/baseline/main.jar (c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162), main-source.zip (c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda), support-source.zip (24b676bef766abddf7a9186284749ddf7613f739fc809be0a80bc59122335318). Hashes rechecked before changes.

| Component | Classification | Reason |
|---|---|---|
| ReturnNetworkSignal semantic plan | SAFE_TO_ADOPT | immutable SHORT SHORT LONG, checked clock arithmetic, session/observation separation |
| Signal runtime scheduling/carrier | REQUIRES_ADAPTATION | current StoryNpc update runs every10 ticks; strict pulse deadlines require a distinct every-tick driver or compatible schedule; relay LIT is structural, not a flash carrier |
| ReturnNetworkLoadAssessment | SAFE_TO_ADOPT as diagnosis only | actual unloaded NPC has no updater; helper cannot prove live wait freshness |
| Seven support GameTests / standalone tests | SAFE_TO_ADOPT | preserve and rerun; they do not replace actual chunk/client proof |
| Static audit / binary preservation probe | REQUIRES_ADAPTATION | designed to forbid all existing-file changes; useful baseline audit but not a final MAIN production-change gate |
| Support package / rollback | REQUIRES_ADAPTATION | source-copy rollback, whereas MAIN must verify previous JAR restoration on a copy |
| Failure matrix / handoff | SAFE_TO_ADOPT | same-chunk, stale wait, structural light and scheduled-tick hazards accurately identify runtime integration work |
| Prior counts and client reports | historical evidence only | PRE-EXISTING MAIN/SUPPORT; no promotion into new final-source proof |

No Support component was blindly rewritten. No persistence/identity/checkpoint/transaction changes are approved by this audit.

Phase B fixture: copied untouched network-seed-preserved; trigger the real discovery boundary while Yeoul is still at home (a different actual chunk from relay). Move only the test player, with view distance2, and observe actual chunk/entity-store availability. Never teleport NPC, force chunk tickets or fabricate availability flags. A missed/unattainable combination is NOT REPRODUCED, never PASS.

Phases E-I remain pending until actual dependency/environment/prerequisite verification. Version0.3.1-dev.1, save5, optionalActor1, network1, art2; Caveman HOLD; next subsystem NOT STARTED.
