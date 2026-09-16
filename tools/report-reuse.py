from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parents[1];E=R/'evidence/reuse';D=R/'dist/stability-work'
assert 'PASS final boundary snapshots=11' in (E/'FIXTURE_PROOF.txt').read_text(encoding='utf-8')
assert 'All 36 required tests passed' in (E/'modified-tests.log').read_text(encoding='utf-8')
t=json.loads((E/'transaction.json').read_text(encoding='utf-8'));assert all(c['exit']==0 for c in t['commands'])
gates='''# 안정화 작업본 판정: PARTIAL

기준점: 0.3.1-dev.1. 저장 형식 4. **0.3.2 승격: NOT READY.**

## 이번에 직접 확인한 하위 게이트

| 대상 | 결과 | 정확한 증거 범위 |
|---|---|---|
| 도시 기록 조사: A/B/D/E 종료 → 다음 프로세스 복구 | PASS | 기존 NoteItem.use 어댑터; 예약 0 → 첫 증거 1 → 최종 조건 3 → 완료 4 → 재열기 300 tick. 사실·안내 claim 유지 |
| 신호 복구 연출: D/E 종료 및 재열기 | PASS | 연출 ID·경계·이미 출력한 자막/음향 비트. 레드스톤 소비/장치 블록 변경은 별개 |
| 도시 진입 연출: D/E 종료 및 재열기 | PASS | 실제 도시 차원에서 연출 레코드 복구. 최초 수정 후 시도 중 1회는 정상 종료되어 cut 미도달, 이후 새 격리 월드에서 전체 재실행 |
| 서사 NBT 저장 완료 반환 | PASS | IO 작업자를 막은 상태에서 직접 저장 파일을 읽는 GameTest. 실패 시 dirty 유지·예외 전달도 검사 |
| migration 결정성 | PASS (명시된 입력) | 합성 1/2/3 각 3회; 실제 과거 형식 2/3 NBT 복사본 각각 3회. 기존 파일 해시 유지 |
| 형식 4 디스크 스트레스 | PASS (부분 행렬) | 세 연출 × 다섯 경계 × 30회 = 450회. 전체 요구 행렬 통과와 구분 |
| 실제 Minecraft 자동 테스트 | PASS | 36 required tests. core 176857 assertions은 다수가 기존 레이아웃 검사이며 기능 개수가 아님 |
| 무음 실행 | PASS | 각 실행 전 10개 음량 0 + narrator 0; 전용 프로필만 사용 |
| JAR 롤백 | PASS | 별도 JAR 복사본을 채택된 0.3.1 해시·이전 동작으로 복원. 작업본은 변경 상태 유지. 월드 다운그레이드 미실행 |
| 아트/자료 보존 | PASS | JAR의 69개 자산/데이터 파일 바이트 동일; 도시 revision 2 유지; 미완성 자산 6그룹·떡밥 초안 2건 유지 |

## 전체 안정화를 막는 남은 게이트

| 사용자 요구 | 현재 판정 |
|---|---|
| NPC 중심 사건 A/B/D/E + NPC 간 상호작용 | NOT TESTED |
| 실제 구조 변경 사건 A/B/D/E | NOT TESTED — 신호 **연출** 검증으로 대신하지 않음 |
| 실제 전투/추격 어댑터 전체 중단 복구 | NOT TESTED — 이전 귀로꾼 일부 검증과 구분 |
| NPC + 구조 + 엔티티 + 연출 복합 사건 | NOT TESTED |
| 전체 주요 사건 A/B/D/E | PARTIAL — 조사 사건과 두 연출의 위 범위만 통과 |
| 영구 인과 연결 토큰 | NOT IMPLEMENTED — 접속 lease와 별개 |
| 중요 아이템 소비 원자성·반환·컨테이너·사망 이동 | NOT IMPLEMENTED / NOT TESTED |
| 실제 지급 보상의 중복 방지 트랜잭션 | NOT IMPLEMENTED / NOT TESTED — 안내 claim/다이아몬드 수량 검사로 대체하지 않음 |
| 필수 구조물 fallback 최소 1종 | NOT IMPLEMENTED |
| 개발용 이동·진행 설정을 최소화한 자연 생존 전체 진행 | NOT TESTED |
| 형식 4 전체 손상/토큰/NPC 참조 행렬 | PARTIAL |
| 전체 Story Integrity: NPC·구조·아이템·보상·엔딩·토큰 | PARTIAL — 현재 연출/조사 상태 검사만 존재 |

다음은 실제 월드 변경·전투·NPC 어댑터의 경계 검증과 영구 연결/소비/지급의 저장 책임을 확장하는 단계다. 필수 구조물 대체 경로와 자연 생존을 통과하기 전 버전을 승격하거나 최종 아트로 이동하지 않는다.
'''
(E/'GATES.md').write_text(gates,encoding='utf-8')
paths={'MODIFIED_FILE':D/'whileaway-0.3.1-stability-work.jar','DIFF_FILE':E/'DIFF.patch','VERIFICATION':E/'VERIFICATION.txt','ROLLBACK':E/'ROLLBACK.sh'}
lines=['Current: WhileAway 0.3.1 stability work / PARTIAL / reusable event adapters and transactions remain',
'CHANGED: NarrativeData.save(File,Provider); SceneRecord.instanceId migration; EventCheckpoint.restore; investigation.pendingEvidence/evidence/checkpoint/noticeClaimed.',
'Original adopted 0.3.1 JAR/source preserved; schema=4; art revision=2; no 0.3.2 promotion.']
lines += [k+'='+str(v) for k,v in paths.items()]
lines += ['BASE_SHA256='+t['baseline_hash'],'MOD_SHA256='+t['modified_hash'],'RESTORED_SHA256='+t['restored_hash'],'','ARCHIVE TRANSACTION (cwd='+str(R)+')','WRAPPER COMMAND ./tools/verify-reuse-transaction.ps1','WRAPPER EXIT 0; input is build/rollback-reuse/test.jar, a copy of MODIFIED_FILE']
for c in t['commands']:
    lines += [c['label'],'COMMAND '+c['command'],'INPUT '+c['input'],'LITERAL OUTPUT\n'+c['output'],'EXIT '+str(c['exit'])]
lines += ['','BASELINE REPRODUCTION','COMMAND ./gradlew.bat --offline runGameTestServer','INPUT adopted 0.3.1 production source plus three added regression tests','LITERAL OUTPUT: 3 required tests failed :(','EXIT Gradle=1; GameTest JVM=3; tests=28 (existing 25 passed).',
'PRE-BARRIER REPRODUCTION','COMMAND ./gradlew.bat --offline runGameTestServer','INPUT queued-IO worker held by latch; baseline inherited SavedData.save behavior','LITERAL OUTPUT: Framework Failure: checkpoint save returned before queued disk write','LITERAL OUTPUT: 1 required tests failed :(','EXIT Gradle=1; GameTest JVM=1; tests=35.',
'MODIFIED REGRESSION','COMMAND ./gradlew.bat --offline build runGameTestServer','INPUT current source; real Minecraft registry/NBT; held IO worker; deliberately invalid file target; six investigation orders; saved copies','LITERAL OUTPUT: All 36 required tests passed :)','LITERAL OUTPUT: PASS core assertions=176857','EXIT 0',
'COMMAND python tools/verify-assets.py dist/stability-work/whileaway-0.3.1-stability-work.jar','LITERAL OUTPUT: '+(E/'assets.txt').read_text(encoding='utf-8').strip(),'EXIT 0',
'COMMAND python tools/verify-design-contract.py','LITERAL OUTPUT: '+(E/'design.txt').read_text(encoding='utf-8').strip(),'EXIT 0',
'COMMAND ./tools/verify-mute.ps1 -OptionsPath ../../work/boundary-smoke/options.txt','LITERAL OUTPUT: '+(E/'mute.txt').read_text(encoding='utf-8').strip(),'EXIT 0',
'COMMAND python tools/verify-reuse-fixtures.py','INPUT all eleven final boundary snapshots, per-process sentinels, old-save triplicates, source hashes','LITERAL OUTPUT\n'+(E/'FIXTURE_PROOF.txt').read_text(encoding='utf-8'),'EXIT 0',
'\nFINAL CLIENT COMMANDS: each runs in the isolated, pre-muted boundary-smoke profile. Synthetic preparation is not natural survival.']
for kind,modes in [(4,['A','B','D','E','Verify']),(1,['D','E','Verify']),(2,['D','E','Verify'])]:
    for mode in modes:
        task=f'runBoundary{kind}{mode}';lines+=['COMMAND ./gradlew.bat --offline '+task,'INPUT '+(E/f'boundary-world-{kind}.txt').read_text(encoding='utf-8').strip()+'; event kind='+str(kind)+'; boundary='+mode]
        lines+=['LITERAL OUTPUT '+s for s in (E/(task+'.txt')).read_text(encoding='utf-8').splitlines() if s.startswith('PASS ') or s.startswith('FAULT ')]
        lines+=['EXIT Gradle=0; JVM='+('0 (normal completed verification)' if mode=='Verify' else '-1073740791 (intentional Runtime.halt fault; requires next-process proof)')]
lines += ['\nOBSERVED FAILURES RETAINED',
'First investigation D halt log claimed checkpoint 3, but its actual disk snapshot had evidence=4, checkpoint=1. Following E startup failed: investigation lost evidence at completion boundary. Classified Framework Failure, fixed in common NarrativeData file-save path.',
'An overlapping Gradle incremental compilation failed to resolve existing project classes. Classified test tooling collision; subsequent builds and clients run serially.',
'One Groovy test-run configuration syntax error was corrected before final compilation.',
'One post-fix city client exited normally after context suspensions without the D sentinel. No PASS awarded; new isolated D/E/Verify sequence ran successfully.',
'Git Bash first failed to start in the sandbox (exit -1073741502); approved retry executed only the JAR-copy rollback and passed.',
'\nRESTORED STATUS: adopted JAR SHA and prior checkpoint behavior restored on build/rollback-reuse/test.jar only. MODIFIED_FILE remains changed. No save downgrade or rollback world operations.',
'OVERALL PARTIAL. NPC/structural/chase/composite adapters, persistent causal links, item/reward transactions, essential structure fallback, full integrity and natural survival remain open. See GATES.md.']
(E/'VERIFICATION.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('PASS evidence report created; overall PARTIAL; 0.3.2 NOT READY')
