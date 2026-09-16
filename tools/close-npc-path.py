"""Close only the explicitly exercised NPC matrix. No campaign/version promotion."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path';D=R/'dist/npc-path'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
g=json.loads((E/'GATES.json').read_text())
assert g['corruptionActualClient'].startswith('PASS') and g['environment'].startswith('PASS')
for name,count in [('lifecycle',10),('death',8),('travel',6),('copy',5)]:
    p=E/'closure'/name;v=json.loads((p/'VERIFIED.json').read_text());assert v==dict(group=name,clients=count,result='PASS')
    for line in (p/'RUNTIME_SOURCE.sha256').read_text().splitlines():
        digest,f=line.split('  ',1);assert h(R/f)==digest
log=(E/'tests-final.log').read_text(encoding='utf-8-sig')
assert 'All 63 required tests passed' in log and 'PASS core assertions=176857' in log
assert 'PASS load guard cases=580 assertions=2161' in (E/'probe.log').read_text(encoding='utf-8-sig')
assert 'PASS NPC storage cases=8' in (E/'npc-facts-probe.log').read_text(encoding='utf-8-sig')
rows=[]
for folder,ledger in [('', 'client-commands.jsonl'),('', 'corrupt-commands.jsonl'),('closure/lifecycle','regression-commands.jsonl'),('closure/death','client-commands.jsonl'),('closure/travel','client-commands.jsonl'),('closure/copy','copy-commands.jsonl')]:
    for line in (E/folder/ledger).read_text().splitlines():
        row=json.loads(line);assert row['pass'] and row['exit']==0 and row['processExited'];rows.append(dict(group=folder or ledger,**row))
assert len(rows)==46
g.update(finalLifecycleRegression='PASS risk-selected current matrix: A-E10/death8/travel6/copy5',NPC_overall='PASS current Yeoul lifecycle acceptance matrix',campaign='PARTIAL',complex='NOT STARTED',realClients=46,gameTests=63,coreAssertions=176857,loadGuardCases=580,loadGuardAssertions=2161,npcStorageCases=8,next='Preserve npc-path closure baseline; first small complex campaign event, then its own crash/restart matrix. No 0.3.2 or art promotion.')
(E/'GATES.json').write_text(json.dumps(g,indent=2)+'\n')
(E/'ALL_CLIENTS.json').write_text(json.dumps(rows,indent=2)+'\n')
base=E/'baseline/previous.jar';source=E/'baseline/previous-source.zip';mod=D/'whileaway-0.3.1-npc-path.jar';target=R/'build/rollback-npc-path/test.jar'
assert h(base)==h(target) and h(mod)!=h(base)
assert 'ROLLBACK PASS: npc-environment JAR restored; world files untouched' in (E/'rollback-retry.log').read_text()
changes=json.loads((E/'archive-delta.json').read_text());hashes=json.loads((E/'corruption-hashes.json').read_text())
report=f'''Current: 《네가 없는 동안》 0.3.1-dev.1 / npc-path / campaign PARTIAL / current Yeoul NPC lifecycle matrix PASS.
Minecraft1.21.1 / NeoForge21.1.249 / save4 / optional actorSchema1 / art revision2.
No 0.3.2 promotion. Complex event NOT STARTED. Art, resource pack, full Horror Director and Caveman remain HOLD.

BASELINE
Immediate input JAR: {base}
SHA256 {h(base)}
Immediate input source ZIP: {source}
SHA256 {h(source)}
These are npc-environment, the preserved bounded continuation of user npc-world. Original npc-world and npc-environment distribution files were not replaced.
Original npc-world JAR9096e4dc9276c8d1eae7aaf2446950e4f76edc5f0f58ee906d7ae70d7268530f; source519e7f5b12098357628b311ffa8b9ad5cd1974a2e5474751255c978318fcdc87 (adopted prior baseline, not a new test).

ACTUAL CHANGES / REASONS
NpcPathBudget: per-entity ephemeral navigation retry controller. RETRY_TICKS60; FAILURE_LIMIT3; BACKOFF_TICKS200; STALL_TICKS160. Failure is not an identity/lifecycle fact. Actual movement clears failure streak; no wall-clock aging.
NpcNavigation.step: real createPath/canReach/moveTo, ARRIVED within squared distance1.4, local distance ceiling48, bounded chunk-metadata corridor with8-block margin. Does not request new chunks. A single null path becomes TEMPORARY_FAILURE; repeated failures become reversible UNREACHABLE. No teleport, terrain writes, generation increment or replacement actor. externalPause resets transient navigation; diagnostic logs only state changes.
NpcEnvironment: residence UNREACHABLE and explicit AVAILABLE/ARRIVED/TEMPORARY_FAILURE/UNREACHABLE/OUT_OF_RANGE path outcomes. withPath combines actual navigation assessment with read-only geometry/light observations. Local geometry VALID alone is never path proof.
StoryNpc: owns one NpcNavigation object. No static world cache and no new NBT field.
NpcEvents.update: dependency inspection and bounded navigation precede event resume/advancement. First transient path failure does not newly suspend; confirmed repeated failure suspends unfinished checkpoint. Existing SUSPENDED record is reused. Home/light repair permits actual walking and existing commit sequence. Completed resident keeps permanent completion, relationships and residence through later light damage.
Existing commit sequence unchanged: initial durable Actor reservation -> conversation fact/checkpoint2 -> arrival/observed facts checkpoint3 -> settle/link/relationship receipt checkpoint4 in one story commit. No new transaction journal or migration.
NpcEnvironmentSmoke: opt-in actual-client fixture runner, saves/restores only explicit fixture blocks. No edits to NPC identity/facts/checkpoint to manufacture completion. Path obstruction uses33blocks; no NPC teleport. Path/light halt-resume-reload tests inspect actual disk NBT.
NpcCorruptionSmoke/NpcCorruptionFixture: opt-in controlled copies only. Missing residence, completed experience and contradictory work must fail closed; missing snapshot.Motion alone uses existing recovery. Connected clients hold100ticks, dispatch saveEverything3times, compare original/final/quarantine bytes and count actual loaded NPC. Corrupt cases do not claim a valid accepted canonical binding; they prove blocked activation and preserved single existing entity/record.
NpcEnvironmentGameTests: adds2 tests to previous61: bounded budget/backoff and unloaded corridor inspection without loading. Existing61 retained.
StoryGameTests/build.gradle: independent GameTest directory each invocation, explicit immutable legacy-fixture root and invocation-local output root; migration test creates its own output directory instead of depending on another test's order. Production Actor capacity256 unchanged.
tools: isolated environment/corruption/current lifecycle/death/travel/copy runners and disk verifiers; serial process/PID exit verification and source-copy manifests. Current source hashes remain fixed during each client batch. Only opt-in QA/test-code changes separate the initial environment batch from final runtime; navigation/event/entity production code is identical.
Archive delta: {json.dumps(changes)}

VALIDATION
AUTOMATED GAME TEST: baseline61 PASS; modified63 PASS (tests-1.log and tests-5.log), final63 PASS (tests-final.log). Counts describe63distinct tests, not their sum. Core assertions176857 PASS per successful build.
STORAGE PROBE: load-guard580cases/2161assertions;19blocked categories repeated20, current run; migration1/2/3/4, ABSENT, normal, recovered and protected corrupt states. Output: PASS load guard cases=580 assertions=2161 repeats=20; blocked matrix=19; sourcePreserved=true; actualClient=false
NPC STORAGE PROBE:8cases,6semantic blocks,2normal/NPC-counterpart round trips. Output: PASS NPC storage cases=8 semanticBlocked=6 normalRoundTrips=2 npcCounterpartRoundTrips=2 originalsPreserved=true actualClient=false
REAL CLIENT:46 new launches, all exited0 with verified termination, mute and DISPLAY1 right-secondary actual2680,120 size854x480.
Environment11: MissingHome/BlockedHome/ConflictHome/WrongLight/CompletedLight; CutPath/ResumePath/ReloadPath; CutLight/ResumeLight/ReloadLight. Canonical1, generation0, identity and permanent facts maintained; restored geometry causes real walking and cp4. Completed light damage never revokes completion.
Corruption6: Control; MissingHome; MissingExperience; WrongWork; Recover; RecoverReload.3manual blocks;1Motion recovery plus fresh LOADED process. No empty fallback;3save dispatches per connected client; originals/quarantines checked by SHA256.
Lifecycle10: CutA/ResumeA through CutE/ResumeE;5actual process-crash/new-process pairs; cp0..4 restored to completed resident, no duplicate confirmed dialogue, shared relationship or link.
Death8: cp2/3/4 x KeepInventoryOFF/ON, plus completed fresh reload for each. Moving/precommit/completed branches selected because changed environment logic begins atcp2. cp0/1 real-death re-sampling omitted deliberately; existing automated tests retained. Actual remote respawn and expected inventory loss/preservation verified.
Travel6: Chunk2/Chunk4 actual entity/chunk unload and reload; Nether2/End2; CutNether2/ResumeNether2. No absent-dimension progress, wrong-dimension NPC or generation change.
Copy5: Seed/AdvanceA/AdvanceB/ReloadA/ReloadB. Full real save directory split, Acompleted4/Bsuspended2, same original identity with independent facts, inactive trees byte-identical.
Total measured client execution seconds {sum(r['seconds'] for r in rows):.3f}; this is sum of runner durations, not Caveman performance comparison.

FAILURE HISTORY
1. tests-2.log: NPC missing-rebuild and offline Wayfarer tests failed with MANUAL_DIAGNOSTIC Actor capacity reached. Shared old GameTest save contained256retained randomized test reservations. Fix isolated per-invocation test worlds, not production cap increase. Old accumulated NBT preserved as accumulated-gametest-story.dat hash60055902447febe8150f7354a9f22c4198f5aa273ea2db25fef88bf6b514a616.
2. tests-3.log: actualLegacyMigrationTriplicates failed because its ../evidence fixture path assumed old working directory. Fix explicit whileaway.testFixtures and whileaway.testEvidence paths.
3. tests-4.log: migration output directory absent when sibling stress test had not run first. Fix local createDirectories in that test. Tests/assertions retained; tests-5 and final full63 PASS.
4. Git Bash initial rollback launch failed before script logic, exit-1073741502, fatal signal-pipe Win32error5. Same inspected copy-only script retried in approved execution context: exit0. Not a mod/code failure.
No actual-client failures in these46 validated launches. Expected corruption diagnostics are test inputs, not silently repaired defects.

STORAGE / INTEGRITY
Save4 and actorSchema1 retained: no persistent field or meaning changes. Navigation state ephemeral; current geometry re-inspected after load. Existing permanent NPC facts/checkpoint/residence/relationships preserved. No original world rewrites, downgrade or auto-campaign reset. StoryStorage, generation/canonical code and NpcState semantics unchanged. Load integrity/semantic integrity plus current NPC_ENV diagnostics remain distinct; a temporary missing light is not a lost shared experience. NPC-to-NPC relation proof is storage round-trip only, not autonomous social simulation.
Observed corruption hashes (original, quarantine, after):
{json.dumps(hashes,indent=2)}

MODEL / USAGE
Primary agent only; no Terra/Luna delegation. Caveman not used, no new token/quality savings experiment. No exact input/output/reasoning token telemetry measured. Account windows only were sampled; at5h89/weekly76 a safe pause marker was set, then after user continuation and actual window refresh to5h2/weekly78 the marker was released. No computer shutdown. Test-harness correction had3failed full-suite attempts before passing; actual NPC runtime policy needed no follow-up correction during clients.

EVIDENCE / ROLLBACK
Working directory for commands: {R}
BASELINE command: ./gradlew.bat --offline runGameTestServer; input preserved npc-environment source; literal All 61 required tests passed; exit0 (baseline-tests.log).
MODIFIED command: ./gradlew.bat --offline compileJava runGameTestServer build; literal All 63 required tests passed and PASS core assertions=176857; exit0 (tests-5.log, tests-final.log).
Package probes command: java -cp build/npc-world-archive-probe PresenceArchiveProbe <explicit JAR> present; baseline/modified/restored expected literal PASS archive policy=PRESENT checks=8; these8checks are packaged policy, not Minecraft gameplay.
ROLLBACK command: C:/Program Files/Git/bin/bash.exe -lc 'chmod +x evidence/npc-path/ROLLBACK.sh && test -x evidence/npc-path/ROLLBACK.sh && evidence/npc-path/ROLLBACK.sh build/rollback-npc-path/test.jar'
Input modified copy {target}; output literal ROLLBACK PASS: npc-environment JAR restored; world files untouched; retry exit0. Restored hash{h(target)} equals baseline. Modified JAR stays changed. Restored behavior is byte-identical prior npc-environment JAR/policy, not save downgrade support.
MODIFIED_FILE={mod}
SHA256={h(mod)}
DIFF_FILE={E/'DIFF.patch'}
VERIFICATION={E/'VERIFICATION.txt'}
ROLLBACK={E/'ROLLBACK.sh'}
Exact archived package command/results and all actual-client commands/PIDs/durations follow:
{(E/'package-commands.jsonl').read_text()}
{json.dumps(rows,indent=2)}

REMAINING / NEXT
NPC lifecycle PASS applies to the current implemented Yeoul campaign event and requested representative environment/corruption matrix, not arbitrary new NPCs or every possible damaged NBT. Campaign PARTIAL. Complex event NOT STARTED; Item/Reward transactions, Structure Fallback, full investigation/puzzles/endings/natural survival and final art are not newly implemented or validated.
Next exact start: preserve npc-path JAR/source hashes, record current NPC lifecycle closure, then select the first small post-return-light complex campaign event. Define its independent facts/execution/Actor/location/evidence/scene/checkpoint state before implementation; use at least4existing systems, and require its own A-E plus world/restart tests. Do not claim current NPC PASS covers a future composite adapter.0.3.2 remains gated on the broader campaign milestones.
'''
(E/'VERIFICATION.txt').write_text(report,encoding='utf-8')
print('PASS current Yeoul NPC lifecycle acceptance matrix:46 actual clients; campaign PARTIAL; complex NOT STARTED')
