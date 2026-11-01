[MAIN CODEX/ASTRA RESULT — V2-01D / V2-02]

## CURRENT STATUS

Version: 0.3.1-dev.1.
MAIN branch: work/v2-playable-core.
Implementation commit: f2a979470443f001d80e3722fcabd051db63a950.
V2-01: PARTIAL.
V2-02: PARTIAL. The first living-town increment is implemented.
Save format: 5. Actor schema: optional 1. Return Network schema: 1.
Art revision: 2. Initial construction plan: 3.
Caveman: HOLD. Final art: DEFERRED. 0.3.2: NOT PROMOTED.

## BASELINE AND SUPPORT SELECTION

Verified starting local and remote MAIN HEAD: 062797f54ee63a65b2cc79870a619d1eeea0daa1.
Verified support HEAD: 613ff381665d0915ce8c378f1271fb08a3c17615.
Reviewed support implementation 8231f8bd and tooling 76125c53 through their support reports and diff.
Adopted the five evidence-tool files and the manual client protocol.
Did not merge the support branch, its three production helpers, sixteen GameTests, or old Tier-A bundles.
Those helpers repeat existing MAIN decisions without a demonstrated production gap.
The support total of 130 tests is not MAIN evidence.

Preserved baseline JAR: evidence/v2-01d/BASELINE.jar.
JAR SHA-256: 8cd59926e20cfa32ffe4861546fe36d7285ac8306b8b9a5ff3161127132cffd7.
Preserved selected source/build snapshot: evidence/v2-01d/BASELINE_SOURCE.zip.
Snapshot SHA-256: 78d51267d2d010a5409a8bd9e24a64913185b7cc5abd88f2af6608665acbbfff.
The snapshot comes from the verified starting commit. It is not a full repository archive.

## ACTUAL GAMEPLAY

Primary client: one fresh isolated survival world, Normal difficulty, commands disabled.
Computer Use created the world through the visible Minecraft menus.
The player entered the first-person memory corridor, moved forward, saved, and quit through the UI.
No debug command advanced this primary run.
Opening completion, Overworld survival, Yeoul, Return Network, Returner, Crisis, recovery, and revisit remain NOT TESTED in this primary run.
Strict primary evidence: one PASS, fourteen NOT_TESTED scenarios.
V2_01_CLIENT_GATE=PARTIAL. HUMAN_EXPERIENCE=NOT_TESTED.

Separate content preview: three isolated creative fixture launches.
Two fixture attempts failed. The final fixture passed.
The fixture prepared Opening through its existing API. It positioned the fixture player and changed fixture time.
It observed actual NPC navigation. It did not teleport the NPC.
Yeoul reached the rest place, home, and workshop through production navigation.
UUID: 42c597b5-7a6e-4a26-8e82-767cfc17f439.
Instance: 53a42547-6fa9-4645-90ea-9f840890d886.
Generation: 0. Return-light checkpoint: 4.
The fixture verified unchanged identity and facts across those movements.
It operated the real vanilla lever and observed the powered floor lamp.
Saved NBT contains one Actor record with that UUID, instance, generation, and checkpoint.
Saved story SHA-256: 22a714c34a43dd114069868eff99c2e181a0524824fc608613cf1cbaf8b264b0.
This disk readback is not a fresh-process reopen test.
The native screenshots were reopened and visually inspected.

Client classification: GRADLE EXPLODED DEVELOPMENT CLASSPATH.
The final corresponding JAR contains all 157 runtime classes with identical SHA-256 values.
The packaged JAR was not installed into a launcher profile for these runs.
All sound categories were zero. Fear pacing and audio readability remain NOT TESTED.
Previous MAIN real-client results remain inherited evidence only.

## GAME CONTENT IMPLEMENTED

Yeoul now uses the approach, return-light work, workshop, rest place, and home.
The workshop interval is 10000–11499. Rest uses 11500–12999.
These are ordinary daily routines, not mandatory story-day triggers.
Yeoul performs small work gestures and gives contextual mundane dialogue.
Missing return-light equipment produces a contextual response.
Active Return Network navigation retains its existing ownership.
Completed events continue to permit resident life and ordinary interaction.

Residential contains a caretaker cottage, family-house furniture, and a boarding-house common room.
Crafting tables, empty barrels, tables, seats, and sleeping mats support ordinary use.
Several spaces have no clue.
The caretaker home keeps the existing home/light bindings.
Home: (38,65,72). Rest: (37,65,73). Workshop destination: (60,65,48).

Maintenance contains actual workstations, storage, benches, and a working vanilla switch/light circuit.
Switch: (65,65,46). Floor lamp: (65,64,46). Equipment label: (66,65,46).
The space uses the existing station/rail layout. It is not a new dungeon or framework.

Discovery A: a worn residential return threshold at (60,64,73).
Its observation describes the physical trace before interpretation.
Discovery B: the maintenance label describes a small record/equipment discrepancy.
Its response depends on the actual switch and lamp states.
Changed equipment yields uncertainty, not an invented story fact.
Both interactions are optional. They use no quest marker or fixed play-day requirement.
They create no durable fact, reward, checkpoint, or forced world mutation.

New code: CityLivingLayout, LifeObservationBlock, LivingTownGameTests, opt-in LivingTownPreview.
Modified production: CityDistrict, CityHorror, NpcEvents, ReturnNetworkEvents, WhileAway, CityLayout.
Added two block/model/item definition sets and matching English/Korean text.
Modified StoryGameTests only to retain legacy plan checks and cover the new default.
Added isolated launch scripts, evidence tools, and client documentation.
Full exact changes are in DIFF.patch.

## OBSERVED DEFECTS

No new critical production save or identity defect was demonstrated.
A static review found that quiet_city disables beds.
The new furnishing uses sleeping mats. No bed explosion occurred during testing.

Fixture compile failure: the Minecraft 1.21.1 createFreshLevel call required a fifth Screen argument.
Fixed LivingTownPreview with the actual API signature. Compile then passed.
Fixture clock failure: setting the derived dimension time did not change the shared world time.
Fixed the fixture to set Overworld time. Actual rest/home/work navigation then passed.
Fixture readiness failure: the fixture checked the NPC before the dimension transfer completed.
Fixed the fixture to wait for the city context and retry existing NPC initialization.
The final run passed. These failures do not establish production lifecycle defects.
A zero client exit alone never counted as a fixture PASS.

## CLIENT ENVIRONMENT

Computer Use initialized successfully through the installed Windows skill.
The prior kernel-assets initialization error did not recur. No internal runtime repair was applied.
Clients stayed on the right secondary DISPLAY1. Existing user worlds and global audio were not changed.
Manual profile: ../v2-01d-manual-client.
Preview profile: ../v2-02-preview-client.
Run tools/play-v2.ps1 for manual play. Create a new survival world for construction plan 3.
Run tools/play-v2.ps1 -Preview for the separate scripted fixture.
The preview is disabled during normal launches.

## BUILD WORK AND VALIDATION WORK

BUILD: three usable living spaces, maintenance workstations/circuit, NPC daily destinations, contextual talk, and two optional discoveries.
VALIDATION: five new meaningful GameTests, strict evidence tooling, one primary UI run, and the isolated content fixture.
FIX/DEBUG: the three fixture issues above.
No reliable per-category duration measurement exists. No time or token ratio is estimated.

## VALIDATION

Final source gate: 119/119 GameTests PASS. Core assertions: 176858 PASS. Gradle build: PASS.
The baseline count of 114 is inherited. Five tests were added; existing tests were not deleted.
The five tests cover plan/cursor loading, player-block preservation/idempotence, routes, equipment-state text, and the actual lever/lamp circuit.
Final output: All 119 required tests passed :). PASS core assertions=176858. BUILD SUCCESSFUL in 1m 26s.
Earlier content gate also passed 119 tests. It is not counted as additional coverage.
Adopted evidence-tool tests: 31 PASS.
Primary strict evidence validator exited 1 because fourteen scenarios remain NOT_TESTED. This is expected.
One successful SCRIPTED REAL CLIENT content run is separate from the incomplete primary survival gate.
Human experience: NOT TESTED. Audio readability: NOT TESTED.
New content death/dimension/crash/copy-world/reopen gates remain NOT TESTED in real clients.
No claim imports the support branch's actual test execution as MAIN validation.

## SAVE SAFETY

Story format 5, Actor schema optional 1, and Return Network schema 1 remain unchanged.
NarrativeData, StoryStorage, Actor identity/generation, checkpoint meanings, and transactions were not edited.
Construction plan 3 appends living cells after the exact revision-2 plan.
Saved plan versions 1 and 2 keep their existing cells and cursor meanings.
Existing built cities are not retrofitted. New layout-3 cities receive the added content.
New living placements compare expected underlay before replacement.
Conflicting blocks remain untouched. The cursor skips them without regenerating the structure.
No runtime force-load, NPC teleport, obstacle deletion, new threat, reward, or framework was added.
The opt-in creative fixture teleports only its fixture player.
Rollback restores the prior package only. It does not downgrade worlds or story saves.
Do not open a layout-3 world using older code that supports only plans 1 and 2.
Original user worlds were not used.

## GITHUB

Implementation f2a979470443f001d80e3722fcabd051db63a950 was pushed to work/v2-playable-core and verified through git ls-remote.
Support remains 613ff381665d0915ce8c378f1271fb08a3c17615.
The following report/package commit is metadata only. Compare src/tools against f2a979470443f001d80e3722fcabd051db63a950 before resuming.
Final publication HEAD is recorded in the session's remote verification.
Master and its unrelated staged changes were not touched.
Pre-existing untracked files remain preserved. Generated raw test fixtures are also archived in automation-fixtures.zip.

## ARTIFACTS

MODIFIED_FILE: dist/v2-01d/whileaway-0.3.1-v2-01d-living-town.jar.
Modified JAR SHA-256: 3c92f0fb7cf1d7981d3bef1b6cc14787c6eff408d8166cdf87a694c8f6050460.
Source ZIP: dist/v2-01d/whileaway-v2-01d-source.zip.
Source ZIP SHA-256: ec83d8df59bdc50bb37d77049aa3665bea5050db4f26e41847766ee47f59e4ca.
The source ZIP represents implementation f2a979470443f001d80e3722fcabd051db63a950. It excludes generated worlds and caches.
DIFF_FILE: dist/v2-01d/DIFF.patch.
Verification: dist/v2-01d/VERIFICATION.txt.
Rollback: dist/v2-01d/ROLLBACK.sh.
Evidence: evidence/v2-01d.
The combined package includes the source, JAR, baseline, diff, report, verification, rollback, and evidence.
SHA256SUMS.json records sizes and hashes.

## NEXT PLAYABLE CONTENT

First run the packaged candidate in a fresh survival world through the complete V2-01 route.
Use the strict evidence protocol for the incomplete scenarios. Keep muted audio unverified.
Then expand ordinary residential use and the workshop's natural travel connections.
Add Doyun and Miru's present-life behavior after review of this living-town increment.
Do not infer V2-01 completion from this fixture or from the test count.
