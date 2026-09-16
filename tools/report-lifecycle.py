from pathlib import Path
import json
R=Path(__file__).resolve().parents[1];E=R/'evidence/lifecycle';D=R/'dist/lifecycle-work'
assert 'PASS actor boundary snapshots=10; final client verifications=2' in (E/'FIXTURE_PROOF.txt').read_text(encoding='utf-8')
t=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'))
paths={'MODIFIED_FILE':D/'whileaway-0.3.1-lifecycle-work.jar','DIFF_FILE':E/'DIFF.patch','VERIFICATION':E/'VERIFICATION.txt','ROLLBACK':E/'ROLLBACK.sh','SOURCE':D/'whileaway-0.3.1-lifecycle-work-source.zip'}
report='Current: WhileAway 0.3.1 lifecycle-work / PARTIAL / NPC-composite-item-structure stabilization remains\n'
report+='VERSION=0.3.1-dev.1; SAVE_SCHEMA=4; optional ACTOR_SCHEMA=1; ART_REVISION=2; overall=PARTIAL; no 0.3.2 promotion\n'
report+='BASELINE_JAR_SHA256='+t['baseline_hash']+'\nMODIFIED_JAR_SHA256='+t['modified_hash']+'\nRESTORED_JAR_SHA256='+t['restored_hash']+'\n'
report+='BASELINE_SOURCE_SHA256=9f0f464e680ce2cfdde8751777411c1bc264645ed6f70e6d195b063b7ae4e427\n'
report+='Git state: not a Git repository; immutable archives and source manifests identify this work.\n'
for key,p in paths.items():report+=key+'='+str(p.resolve())+'\n'
report+='\nCHANGES AND REMAINING SCOPE\n'+(R/'docs/ACTOR_LIFECYCLE_031.md').read_text(encoding='utf-8')
report+='\nOBSERVED COMMANDS / INPUT / OUTPUT / EXIT\n'
report+='BASELINE command: ./gradlew.bat --offline runGameTestServer\ninput: unchanged adopted stability-work source; existing 36 Minecraft tests\noutput: All 36 required tests passed :) | BUILD SUCCESSFUL in 57s\nexit=0\n'
report+='MODIFIED command: ./gradlew.bat --offline build runGameTestServer\ninput: frozen lifecycle-work source; 36 retained + 12 new Minecraft tests\noutput: PASS core assertions=176857 | All 48 required tests passed :) | BUILD SUCCESSFUL in 58s\nexit=0\n'
for c in t['commands']:
    report+='\n'+c['label']+' command: '+c['command']+'\ninput: '+c['input']+'\noutput:\n'+c['output']+'\nexit='+str(c['exit'])+'\n'
report+='\nCLIENT COMMANDS (scripted real Minecraft client, not natural survival)\n'+(E/'runtime-commands.txt').read_text(encoding='utf-8-sig')
for repeat in (1,2):
    for mode in ('A','B','C','D','E','Verify'):
        report+='\n'+(E/f'runActor{mode}{repeat}.txt').read_text(encoding='utf-8-sig')
report+='\nThe ten intended Runtime.halt cuts report EXPECTED_FAULT_PROCESS_EXIT=-1073740791 on this Windows host; each Gradle gate exits0 only after its PASS cut marker. Next-process reload and raw NBT verification are also required and passed.\n'
report+='Two Verify processes each checked300 ticks after E. All twelve used ten muted audio categories and narrator0 before startup. No OS volume change.\n'
report+='Runtime uses the development classpath; the packaged JAR was independently reopened and all58 compiled classes matched it. No normal-profile installation test is claimed.\n'
for name in ('FIXTURE_PROOF.txt','assets.txt','design.txt','mute.txt','ARCHIVE_PROOF.txt'):
    report+='\n'+name+'\n'+(E/name).read_text(encoding='utf-8-sig')
report+='\nFAILURES / FIXES / LIMITS\n'
report+='Observed tooling failure: first Git Bash rollback launch exited -1073741502 (outer PowerShell1); approved execution-context retry passed all archive probes and rollback with exit0. No game gate is credited from the failed attempt.\n'
report+='Code-review failure windows, not claimed baseline client reproductions: spawn before ticket save; old entity AI NBT behind committed hunt; spawn succeeds before B write. Changes: durable reserve, actor generation fence, encounter-only AI reconciliation, existing-canonical B finalization. New Minecraft regression tests passed.\n'
report+='Initial A1 preflight passed on the intermediate46-test snapshot and is preserved in attempt-1 but excluded from the final12-client evidence. The final chain used frozen48-test source.\n'
report+='NPC Villager fixtures prove lifecycle primitives only, not named NPC campaign movement/dialogue/relationships. NPC campaign and four-component composite event recovery remain NOT TESTED.\n'
report+='Critical-item consumption/ownership and reward atomicity, essential structure fallback, permanent causal links, natural survival completion: NOT TESTED / not implemented in this slice. Notice receipt is not an item reward transaction.\n'
report+='Actor world-copy test is object/serialization isolation only. Copied-save client independence for this new actor extension remains NOT TESTED. Previous actual copy tests covered earlier code.\n'
report+='New chase adapter death/respawn, actual chunk travel, Nether/End roundtrips remain NOT TESTED; old seven-figures evidence is not promoted to these gates.\n'
report+='Full actor-field corruption stress and legacy encounter tickets whose old entity is also missing remain incomplete. Live matching legacy actors are deterministically adopted. Generic NPC inventories are not an atomic item ledger.\n'
report+='Full Story Integrity remains PARTIAL: actor/scene checks exist, item/reward/structure/exclusive-choice/causal-link checks remain.\n'
report+='Schema1/2/3 and actor-less4 normalize deterministically. Existing450 scene disk cycles,200 actor in-memory roundtrips, and6 actual legacy2/3 fixture migrations passed in the48-test suite. These counts do not mean full campaign completion.\n'
report+='Old published baseline JAR/source hashes unchanged; the two original legacy story NBT files were read-only verified unchanged. Only isolated new actor worlds were launched. No old city rebuilt; asset/data69 entries unchanged.\n'
report+='ROLLBACK restored ONLY a JAR copy to stability-work. Restored archive has actorRegistry=false/chaseAdapter=false and preserved pure checkpoint behavior. The modified JAR remains changed. Save downgrade is neither supported nor verified.\n'
report+='Reporting tool first-write SyntaxError from nested string delimiters was corrected before execution; it did not run or alter production source.\n'
gates='''# 0.3.1 lifecycle-work quality gates — overall PARTIAL

| 항목 | 결과 | 실제 방법 | 횟수 | 경계 |
|---|---|---|---:|---|
| 기준본 회귀 | PASS | Minecraft GameTest | 36 / 1회 | 기존 stability-work |
| 수정본 서버 회귀 | PASS | Minecraft 서버 GameTest | 48 / 1회 | 기존36 + 신규12 |
| 추격 A/B/C/D/E | PASS | 무음 실제 클라이언트 강제 종료/다음 JVM 재개 | 각2회, 총10컷 | 실제 Wayfarer AI; 테스트용 이동/시작 |
| E 이후 재접속 | PASS | 300tick 완료 안정성/등록 actor 제거/무결성 | 2회 | 같은 소스, 서로 다른 새 월드 |
| NPC lifecycle 기초 | PASS | Villager 서버 fixture | 신규12개 검사 묶음에 포함 | 실제 캠페인 NPC 사건 아님 |
| NPC 중심 캠페인 사건 | NOT TESTED | 실제 캠페인 미연결 | 0 | 대화/관계/독립 행동 미완성 |
| 복합 캠페인 사건 | NOT TESTED | 4요소 이상 사건 미구현 | 0 | 별도 gate |
| 기존 scene 저장 스트레스 | PASS | NBT 디스크 쓰기/읽기 | 450회 | 3종 x5경계 x30 |
| actor 저장 정규화 | PASS | NBT 메모리 roundtrip | 200회 | 5경계 x40; 디스크 crash 아님 |
| 실제 구형 NBT migration | PASS | 보존 복사본의 결정적 변환 | 6개 | 형식2/3 각3개 |
| 신규 actor 월드 복사 | PARTIAL | object/serialization 분리 | 1개 테스트 | 실제 세이브 복사 클라이언트 미검증 |
| 신규 actor 사망/청크/차원 예외 | NOT TESTED | 신규 adapter 실제 클라이언트 미실행 | 0 | 옛 seven 사건 증거와 분리 |
| 중요 아이템/보상 트랜잭션 | NOT TESTED | 미구현 | 0 | 알림 중복 억제와 구분 |
| 필수 구조물 fallback | NOT TESTED | 미구현 | 0 | actor 위치9점 탐색과 구분 |
| 영구 인과 연결 토큰 | NOT TESTED | 미구현 | 0 | SceneAck lease와 구분 |
| 전체 Story Integrity | PARTIAL | actor/scene 일부 검사 | 클라이언트2회 포함 | 아이템/보상/구조물/선택 전체 검사 남음 |
| 자연 생존 전체 진행 | NOT TESTED | 이번 client는 scripted fixture | 0 | 생존 QA 남음 |
| 아트/리소스 보존 | PASS | 기존 JAR와 byte 비교 | 69 entries | 도시 revision2 유지 |
| 자산 계약 | PASS | archive/resource 검사 | 6704 assertions | 새 아트 없음 |
| 설계 계약 | PASS | 요구사항/임시자산/떡밥 등록 확인 | 49 / 6 / 2 | 완성 콘텐츠 수 아님 |
| JAR 롤백 | PASS | 복사본 hash 및 archive/순수 checkpoint 검사 | 성공1회 | 세이브 downgrade 미지원 |

다음 첫 작업: 신규 adapter의 사망/청크/차원/복사 클라이언트 검증 및 손상 필드/옛 누락 티켓 진단을 보강한다. 그 뒤 실제 NPC와 4요소 이상 복합 사건 A/B/C/D/E, 아이템/보상 트랜잭션, 필수 구조물 fallback을 구현/검증한다. 이전 실제 클라이언트 시나리오를 새 코드로 회귀하고 자연 생존 진행을 확인한다. 요구된 gate 묶음 전에는0.3.2 및 전체 PASS로 승격하지 않는다.

다음 작업본: dist/lifecycle-work JAR/source.zip. dist/stability-work는 계속 immutable baseline. 최종 아트는 보류한다.
'''
(E/'GATES.md').write_text(gates,encoding='utf-8');report+='\n'+gates
(E/'VERIFICATION.txt').write_text(report,encoding='utf-8')
print('PASS lifecycle report generated from verified runtime and rollback evidence; overall remains PARTIAL')
