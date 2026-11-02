[MAIN GPT-6.1 SOL RESULT — V2-01E]

CURRENT STATUS
버전 0.3.1-dev.1. 작업 브랜치 work/v2-playable-core.
V2-01 PARTIAL. V2-02 PARTIAL. 0.3.2 NOT PROMOTED. Caveman HOLD.
Save format 5. Actor schema optional 1. Return Network schema 1. Construction plan 3.

GAME CONTENT / BUILD WORK
이번 재개에서는 production 콘텐츠를 추가하지 않았다.
기존 Opening 완료 월드의 격리 복사본에서 생존과 탐험을 계속했다.
양 한 마리를 사냥했다. 양고기 1개와 양털 1개를 회수했다.
동굴에서 심층암 1개를 채굴하고 조약돌 심층암 1개를 회수했다.
이 결과는 실제 화면, Minecraft 통계, 저장 NBT로 확인했다.
나무 채집, 도구 제작, 이전 돌 채굴은 PRE-EXISTING EVIDENCE다.
완성된 거점, 조리, 역 자연 발견, 전체 캠페인 완주는 이번에 확인하지 않았다.

BASELINE
실제 시작 local/remote HEAD: 7620506f4450c622e21dc3d877c413a3e4067a39.
기준 production JAR: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e\whileaway-0.3.1-v2-01e.jar
JAR SHA-256: 5adb5f964d42e1e9355a79fc491db8c32340de5e9a172748a26c8d926c7bcc4a.
JAR size: 704237 bytes.
기준 source ZIP: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\whileaway-0.3.1-v2-01e-continuation-source.zip
Source SHA-256: f3ab52c1fa83b896b89a9cc72765ab2019374d7f8e27c58ed2868d678878a77c.
Source size: 934282 bytes.
기준 ZIP을 BASELINE-source.zip으로 별도 보존했다.
원본 V2-01E Signal Fix 월드의 47개 파일 해시가 모두 동일하다.

INFRASTRUCTURE IMPLEMENTED
수정 파일 1: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\tools\validation\play-input\java\wwa\input\InputAssist.java
InputAssist.protectPlayer는 명시적으로 켠 테스트 프로퍼티를 확인한다.
대상은 지정한 saves 경로 아래의 integrated-server ServerPlayer다.
전용 서버와 지정 경로 밖의 월드는 보호 대상에서 제외한다.
LivingIncomingDamageEvent를 취소하고 피해 종류와 체력을 기록한다.
실제 낙하 61.0, 용암, 화염 피해에서 health=20을 확인했다.
이 결과를 모든 사망 원인에 대한 무적 판정으로 확대하지 않는다.
입력 lease의 마우스 캡처와 눌림 상태 재확인도 보완했다.
입력 보완은 컴파일됐으나 연속 채굴 문제의 해결 판정은 PARTIAL이다.

수정 파일 2: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\tools\ci\v2-01e.init.gradle
whileaway.testPlayerProtection=true를 격리 실행에만 설정했다.
whileaway.testPlayerProtectionRoot는 v2-01e-development-client/saves다.
기존 무음 설정과 오른쪽 DISPLAY1 배치를 유지했다.
Production JAR에는 wwa/input helper가 없다.

VALIDATION WORK / ACTUAL CLIENT
새 development-classpath 프로세스 5개를 실행하고 모두 정상 종료했다.
각 Gradle 실행은 BUILD SUCCESSFUL과 exit 0을 반환했다.
이 다섯 번을 end-to-end 완주 PASS로 집계하지 않는다.
client.log: 보호 설정 전 복사본에서 일반 낙하 사망을 확인했다.
protected-client.log: 새 복사본에서 낙하·용암·화염 피해 차단을 확인했다.
held-input-client.log: 별도 Surface Route 복사본에서 탐험과 식량 확보를 수행했다.
mouse-input-client.log: 새 프로세스에서 식량과 도구 보존을 확인했다.
final-client.log: 최종 helper 소스에서 재접속과 자원 회수를 수행했다.
최종 두 프로세스에서는 새 피해 원인을 추가로 검증하지 않았다.
피해 차단 함수는 앞서 검증한 구현과 동일하다.

최종 저장 월드: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\v2-01e-development-client\saves\V2-01E Surface Route
상태: Overworld / Survival / Peaceful / health 20 / food 20.
Opening stage: COMPLETED. cityVisited=0. actors=[].
현재 위치: [-318.3623485030904, 4.162141421026987, -118.94921650013264].
이 좌표는 마지막 상태 인수인계용이다. 역 탐색에 사용하지 않았다.
현재 플레이어는 동굴의 물가 발판에 있다. 지상 탈출은 미완료다.
인벤토리: 묘목 1, 제작대 1, 나무 곡괭이 1, 나무 도끼 1, 참나무 원목 1,
흙 19, 조약돌 3, 흰 양털 1, 양고기 1, 조약돌 심층암 1.
원래 지상 체크포인트와 실패·보호 복사본도 각각 보존했다.

AUTOMATED VALIDATION
이번 새 full GameTest / coreTest / build task: NOT RUN.
기존 120 GameTests / 176858 core assertions / build PASS는 PRE-EXISTING EVIDENCE다.
새 실행은 main compileJava UP-TO-DATE 및 playInput 컴파일을 확인했다.
최종 JAR의 157개 runtime class와 78개 resource가 현재 build 출력과 byte-identical이다.
새 source ZIP의 src/main 내용도 baseline ZIP과 byte-identical이다.
새 ZIP은 같은 374개 source/tools/docs/build 파일을 담는다.
이 ZIP에는 별도 evidence 폴더와 dist 패키지를 중복 포함하지 않는다.

OPENING / SURVIVAL / RETURN NETWORK / RETURNER
Opening 전체 조작 완주는 이전 재개에서 확인됐다. 이번에는 완료 상태 재접속을 확인했다.
식량 확보와 제한된 탐험·채굴: 보호된 자동 조작으로 확인했다.
Normal 난이도 생존 압력과 위협 피해 공정성: NOT TESTED.
역 자연 발견, 여울, Return Network, NoEvent, Returner, Crisis, 역 재방문: NOT REACHED.
이 항목을 기존 자동 검사나 과거 클라이언트 결과로 PASS 처리하지 않는다.
quiet_city 장기 체류, Residential/Maintenance 실제 사용성, plan 1·2 호환 재검증: NOT TESTED.
HUMAN EXPERIENCE: NOT TESTED. AUDIO READABILITY: NOT TESTED.
현재 실클라이언트는 무음이다. Computer Use 초기화 오류는 이번에 발생하지 않았다.

FAILURE HISTORY
1. 증상: Peaceful에서 낙하 사망.
재현: V2-01E Resume 2에서 짧은 이동 lease가 협곡을 넘었다.
원인: 일반 Minecraft 낙하 피해와 자동 조작의 지형 판단 한계.
수정: 테스트 전용 IncomingDamage 취소. Production 게임 규칙은 그대로다.
재검증: 새 복사본의 같은 낙하 경로와 용암·화염에서 health 20.
판정: 해당 피해 차단 PASS. Production 결함으로 분류하지 않는다.

2. 증상: 일부 연속 채굴 lease에서 블록 파괴가 완료되지 않았다.
확인: Survival, mayBuild=1, OnGround=1, active effects 없음. 대상은 deepslate다.
게임 화면을 직접 클릭한 뒤 한 번의 채굴과 자원 회수는 성공했다.
입력 helper에 마우스 캡처와 눌림 재확인을 추가했다.
최종 실행의 자동 연속 채굴은 안정적인 재현 결과를 확보하지 못했다.
원인 확정과 helper 수정 효과는 PARTIAL이다. 게임 저장 결함으로 단정하지 않는다.

3. 실행 도구: nbtlib import가 ModuleNotFoundError로 끝났다.
프로젝트의 기존 tools/read-world-nbt.py로 저장 자료를 정상 조회했다.
새 라이브러리는 설치하지 않았다.
보고서 재조회에서 기본 cp949 디코딩이 실패했다. UTF-8을 명시해 정상 재조회했다.

SAVE SAFETY
Schema, migration, Actor identity, checkpoint, transaction, write guard는 변경하지 않았다.
Production 코드 수정: NONE. 기존 사용자 월드 변경: NONE.
복사 월드의 정상 진행과 vanilla 저장만 기록했다.
아이템, 스토리 사실, 위치를 주입하지 않았다. 텔레포트를 사용하지 않았다.
원본은 해시로 보존을 확인했다. 테스트 보호는 최종 production JAR에 포함되지 않는다.
Rollback은 source/tool 패키지 복원이다. 월드·세이브·진행도 downgrade가 아니다.

ARTIFACTS / CHANGED BRANCH AND FIELDS
Branch: work/v2-playable-core.
Fields: whileaway.testPlayerProtection, whileaway.testPlayerProtectionRoot.
Functions: InputAssist.protectPlayer; InputAssist.tick bounded mouse/key handling.
MODIFIED_FILE: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-protected\MODIFIED-source.zip
DIFF_FILE: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-protected\DIFF.patch
VERIFICATION: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-protected\VERIFICATION.txt
ROLLBACK: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-protected\ROLLBACK.sh
MODIFIED source SHA-256: 9dd481cf175aa616063a40b8119d9bbf69e5ac18742b612dfe598ee5c3260e70.
MODIFIED source size: 934799 bytes. Production JAR SHA는 baseline과 동일하다.
다른 패키지 해시와 크기는 같은 폴더의 SHA256SUMS.json에 기록한다.

EVIDENCE / EXACT COMMANDS
모든 명령의 cwd: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration
BASELINE command: python evidence/v2-01e/resume-2/package-check.py BASELINE dist/v2-01e-protected/BASELINE-source.zip
Input: 위 baseline ZIP.
Literal output: BASELINE: package=PASS protection=OFF input_guard=OFF production=UNCHANGED
Exit: 0.
MODIFIED command: python evidence/v2-01e/resume-2/package-check.py MODIFIED dist/v2-01e-protected/MODIFIED-source.zip
Input: 위 modified ZIP.
Literal output: MODIFIED: package=PASS protection=ON input_guard=ON production=UNCHANGED
Exit: 0.
ROLLBACK command (PowerShell): & 'C:\Program Files\Git\bin\bash.exe' 'C:/Users/scarl/Documents/Codex/2026-09-08/new-chat/work/return-network-integration/dist/v2-01e-protected/ROLLBACK.sh' 'C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\evidence\v2-01e\resume-2\ROLLBACK-copy.zip'
Input: MODIFIED-source.zip의 별도 복사본 ROLLBACK-copy.zip.
Literal output: ROLLBACK: source-package=PASS world-files-touched=0
Exit: 0.
Restored validation command: python evidence/v2-01e/resume-2/package-check.py ROLLBACK evidence/v2-01e/resume-2/ROLLBACK-copy.zip
Literal output: ROLLBACK: package=PASS protection=OFF input_guard=OFF production=UNCHANGED
Exit: 0.
Restored hash: f3ab52c1fa83b896b89a9cc72765ab2019374d7f8e27c58ed2868d678878a77c.
Restored status: baseline source/tool package, test protection OFF, production unchanged.
Rollback 뒤 실제 Minecraft 프로세스 재실행: NOT RUN.
MODIFIED-source.zip은 변경된 상태로 별도 보존했다.

CLIENT COMMAND
$env:GRADLE_USER_HOME='C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\gradle-home'
.\gradlew.bat --offline --no-daemon --init-script tools/ci/v2-01e.init.gradle runV2E --console=plain
각 실행 출력은 resume-2의 해당 *client.log에 보존했다.
Literal completion: BUILD SUCCESSFUL. Exit: 0, 다섯 프로세스 모두.
새 일반 설치형 JAR smoke: NOT RUN. 이전 설치형 검증은 PRE-EXISTING EVIDENCE다.

MODEL / USAGE
현재 메인 모델이 단독으로 작업했다. Terra/Luna 또는 하위 에이전트는 사용하지 않았다.
정확한 토큰·작업시간 비중은 측정하지 않았다.
활동은 protected client 플레이, 테스트 helper 보완, 원본 보존, 패키지 검증으로 구분했다.
Caveman은 사용하지 않았다.
사용량은 주요 경계에서 조회했다. 10% 이하에서 새 테스트를 시작하지 않는 지침을 유지한다.
자동 PC 종료와 reset credit 사용은 수행하지 않았다.

GITHUB
구현·산출물 업로드 확인: 1f84a9a9937552c7b77d9ece92eb564e895fe1a5. git push exit 0. git ls-remote 일치.
후속 커밋은 이 업로드 기록과 문서 메타데이터만 갱신한다.
Source ZIP 374개 항목은 구현 HEAD와 줄바꿈 정규화 후 일치한다.
원시 바이트의 줄바꿈 차이는 source-head-comparison.json에 기록했다.
기록 전체 whitespace 검사는 raw 로그와 patch context의 후행 공백을 보고했다.
로그·patch를 제외한 staged whitespace 검사는 exit 0이었다.
업로드 대상: 위 테스트 helper 2개, dist/v2-01e-protected, evidence/v2-01e/resume-2.
기존 unrelated untracked 15개는 그대로 보존한다.

REMAINING / NEXT START
V2-01 = PARTIAL. V2-02 = PARTIAL. 자연스러운 end-to-end 완주 = NOT TESTED.
다음 첫 단계는 저장된 Surface Route의 지상 복귀 또는 별도 지상 복사본의 자연 탐험이다.
현재 동굴 복사본과 식량·자원 증거는 보존한다.
연속 채굴이 필요하면 기존 helper의 실제 mouse capture와 lease 동작을 짧게 확인한다.
그 다음 역을 자연 발견하고 여울 및 RN으로 연결한다.
상충하는 Returner 경로는 별도 복사 월드에서 검증한다.
피해·Crisis 수용성 검증에는 보호 설정을 해제한 별도 복사본이 필요하다.
사람의 공포·이해·청각 평가는 기술적 자동 조작과 분리한다.
V2-01 핵심 실제 플레이 게이트 전에는 도윤·미루 신규 콘텐츠로 넘어가지 않는다.
