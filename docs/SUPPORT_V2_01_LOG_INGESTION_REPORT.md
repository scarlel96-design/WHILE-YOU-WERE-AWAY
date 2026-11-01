# GPT SUPPORT REPORT — V2-01 log ingestion continuation

## Baseline

- Main: `work/v2-playable-core` at `062797f54ee63a65b2cc79870a619d1eeea0daa1`.
- Support: `gpt-support/v2-01-observation-client-gate` at `e5c184bc367c7c3f26a1bd2b551075c07edf50ae`.
- Original support implementation: `8231f8bd93626316f9d59639ad95c6cd5d7bce84`.
- Local and remote support heads matched before edits.
- Version remains 0.3.1-dev.1. V2-01 remains PARTIAL.

## Work performed

The evidence validator now reports REAL_CLIENT=NOT_TESTED when every scenario is unexecuted.
The client gate remains PARTIAL because required acceptance evidence is missing.
The previous implementation reported REAL_CLIENT=PARTIAL for the same unexecuted template.

The new log-draft tool prepares a new session directory from one selected log.
It preserves the source log bytes and records their SHA-256.
It records marker counts and bounded line-number lists.
It does not copy log text into the summary.
It leaves all scenario results and human ratings NOT_TESTED.
It rejects an existing output directory and invalid target metadata.
An optional candidate JAR is hashed without claiming that it produced the log.
The declared target commit remains user-supplied metadata.

## MAIN adoption review

The current source diff against main contains five existing production call-site edits and three new helper classes.
The source also contains three added GameTest classes.
The observation extraction preserves range 32, dot 0.35, eight ticks, and the original evaluation order.
The knowledge extraction preserves notice gating and observed/unseen station text selection.
The NPC extraction preserves the completed-event 30/200-tick look habit and resident dialogue delegation.
These changes remain candidates for MAIN adoption after integration review.
This continuation adds no production Java change.

Previous support classification remains valid.
Old tier-A archive contents remain uninspected because the named archives were not located.
Do not merge those archives or apply superseded persistence assertions.
Retain the existing SUPPORT_REBASE_REPORT classification.

## Validation

- BASELINE Python: 19 tests PASS.
- MODIFIED Python: 31 tests PASS, including 12 new tests.
- Previous support GameTest: 130/130 PASS; not rerun in this tooling-only continuation.
- Previous support core assertions: 176858 PASS; not rerun here.
- Previous support Gradle build: PASS; not rerun here.
- New Minecraft client launches: 0.
- Human experience: NOT TESTED.
- Audio readability: NOT HUMAN-VERIFIED.

The inherited `v2-01b-transaction/client-gradle.log` was processed as historical input.
Its SHA-256 is `03d9b71babc7b9487be49e05065822459cee2f7ad2cb820b96a3494f3e373442`.
It contains one sound-engine start, one normal-stop marker, and one Gradle-success marker.
It contains no world-preparation or player-join marker under the tool's narrow matching rules.
Marker absence does not prove that gameplay never occurred.
This input grants no scenario PASS.
The resulting draft and strict summary retain 15 NOT_TESTED scenarios.

## Failure history

An unexecuted REAL_CLIENT template reported REAL_CLIENT=PARTIAL.
The cause was a check for supplied rows instead of executed rows.
The validator now checks for a result other than NOT_TESTED.
Two new tests protect the unexecuted case and the existing partial-execution case.

No new production defect was found in this continuation.
The previous Computer Use kernel-assets failure remains an external environment blocker.
This continuation does not retry Computer Use or classify that error as a Minecraft defect.

## Compatibility and remaining work

Save format 5, optional Actor schema 1, and Return Network schema 1 remain unchanged.
Checkpoint, Actor identity, generation, transaction, and write-guard semantics remain unchanged.
AGENTS.md, resources, and prior packages remain unchanged.
The current candidate JAR is the previously verified support artifact.
This tooling-only continuation creates no replacement game JAR.

Positive survival observation, actual NPC ownership, and player-facing readability remain MAIN client gates.
All original client scenarios still require actual observations and attributable evidence.
The evidence tools validate submitted records. They do not certify the truth of gameplay claims.

## Contribution and model record

Previous, additional, and cumulative milestone percentages remain UNMEASURED.
No measured work denominator supports a 40% claim.
The delivered scope includes three production helpers, three GameTest classes, evidence tools, and adoption documents.
This continuation adds one bounded log-draft utility, a report-status fix, and their tests.
No subagents, Terra, Luna, or Caveman were used in this continuation.
No model-usage savings or measured time allocation is claimed.

## Artifacts and publication

Evidence directory: `evidence/support-v2-01-log-ingestion/`.
Package directory: `dist/support-v2-01-log-ingestion/`.
The evidence includes BASELINE_FILE, MODIFIED_FILE, DIFF_FILE, VERIFICATION.txt, and executable ROLLBACK.sh.
The rollback test restores a disposable tool copy. It does not modify worlds or the active modified tool.
PACKAGE_MANIFEST.json records package members and hashes.
PUBLICATION.txt records the verified remote branch and commits after upload.
This report and its evidence are uploaded only to the support branch.

## MAIN HANDOFF

1. Compare current main and support HEADs.
2. Audit original implementation commit `8231f8bd93626316f9d59639ad95c6cd5d7bce84`.
3. Audit this continuation's tooling and document diff against `e5c184bc367c7c3f26a1bd2b551075c07edf50ae`.
4. Select implementation changes without historical evidence-package commits.
5. Recheck production behavior after adoption.
6. Run the full required GameTest, core, and build gate on integrated main.
7. Run client acceptance with the existing protocol and strict evidence tool.
8. Keep V2-01 PARTIAL until gameplay and human acceptance are established.

GPT SUPPORT IMPLEMENTATION: scoped implementation delivered; historical archive reconciliation remains PARTIAL.
V2-01 AUTOMATED COVERAGE: PASS for executed support scope; survival/client integration remains PARTIAL.
V2-01 CLIENT GATE: NOT TESTED. HUMAN EXPERIENCE: NOT TESTED. REAL CLIENT: NOT TESTED.
