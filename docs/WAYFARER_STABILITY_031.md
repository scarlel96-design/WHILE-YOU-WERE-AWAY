# Wayfarer 안정화 검증 및 설계 v0.4 인계

Current: 《네가 없는 동안》 0.3.1-dev.1 / wayfarer-stability / PARTIAL / Wayfarer 예외 검증 완료, Actor 손상 로드 결함 재현

## 기준과 범위

- 입력: dist/campaign-pilot/whileaway-0.3.1-campaign-pilot.jar
  SHA-256: 9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8
- 입력 소스: dist/campaign-pilot/whileaway-0.3.1-campaign-pilot-source.zip
  SHA-256: 81b47fcca580ac17102e0135c963bc1b2fdce638544ee5b82571b4b65fdc55ec
- 출력 JAR: dist/wayfarer-stability/whileaway-0.3.1-wayfarer-stability.jar
  SHA-256: 3d3b08beddd7e9fae87c41188c31251e941ee9da9b6697b50bcf96a2fd960766
- Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21.0.5.
- save format 4, optional actorSchema 1, city art revision 2 유지. 0.3.2 승격 없음.
- 이번 변경은 실제 클라이언트 테스트 어댑터·런처·독립 검증기·패키징·설계 문서다. StoryActors, NarrativeData, Wayfarer의 출시 동작을 새로 변경하지 않았다.
- 필드 1·2·3 마이그레이션 지원 구현은 유지하며 이번 48개 기존 회귀를 다시 통과했다. 기존 450/200 스트레스와 과거 실제 0.2 세이브 검증 횟수를 이번 새 실행 횟수로 합산하지 않는다. 이번 손상 로드 결함 때문에 모든 비정상 저장 복구를 PASS로 부르지 않는다.

## 실제 코드 변경과 이유

### ActorSmoke

`client/ActorSmoke.java`의 D 경계를 거리 이탈에서 `actor.hurt(p.damageSources().playerAttack(p), 1000)`에 의한 실제 전투 종료로 바꿨다. 현재 정책은 거리 이탈 시 완료가 아니라 SUSPENDED이므로, 오래된 테스트가 완료 지점에 도달하지 못하는 문제를 테스트 어댑터에서 바로잡았다. 게임 정책을 테스트에 맞춰 되돌리지 않았다.

A/B/C/D/E/Verify를 서로 다른 두 seed에서 각기 새 JVM으로 실행했다. D의 `BEFORE_COMPLETE`는 **첫 완료 사실 쓰기 직전이 아니다**. 현재 구현에서 완료 사실·RETIRED·checkpoint 3이 이미 디스크에 저장된 뒤, checkpoint 4/notice 확정 전의 지점이다. 이 의미를 이름만 보고 확대 해석하지 않는다.

현재 커밋 순서(변경 없음):

`encounter_complete + RETIRED + checkpoint 3 + player complete + encounter 해제` → 동기 저장 → D 중단 → `checkpoint 4 + noticeClaimed` → 동기 저장 → E 중단 → runtime actor 정리.

noticeClaimed는 알림 영수증이며 물리적 아이템 보상 트랜잭션이나 다음 사건 의존 토큰이 아니다.

### ActorExceptionsSmoke

`client/ActorExceptionsSmoke.java`에서 HOME만 검사하던 청크 증거를 분리했다. `lastLivePosition`, `unloadedTicks`를 추가하고 HOME, 저장 position, 실제 actor 위치, snapshot의 Pos, 플레이어 위치를 대조한다. fixture actor를 HOME과 인접한 별도 청크로 이동시킨 뒤 검사하므로 HOME만 언로드한 거짓 양성을 막는다.

HOME/snapshot/last-live 각각에 대해 terrain chunk와 entity storage가 모두 미로딩임을 확인하고, canonical actor 부재와 플레이어 청크 로딩을 함께 확인한 채 100 ticks를 유지했다. 귀환 후 UUID·instanceId·generation·checkpoint와 canonical count=1을 검사한다.

`CopyReload`를 추가했다. Original은 전투 완료(cp4), Copy는 이탈 중단(cp2)으로 서로 다르게 진행한다. 이후 둘을 각각 새 클라이언트로 다시 열어 원본은 완료 유지, 복사본은 같은 actor 재연결 후 중단 유지임을 확인했다. 복사 독립성 시험에 generation 재생성 실험을 섞지 않았다.

### ClientTestEnvironment / 실행 도구

- `ClientTestEnvironment.verify`: 모든 SoundSource 음량 0, 실제 GLFW 창의 오른쪽 DISPLAY1 영역 포함 여부, PID 기록. 테스트 opt-in에서만 호출한다.
- `build.gradle`: 새 격리 profile과 evidence 경로, 실행 전 무음/narrator0, 모니터 agent 적용, 작업별 PASS sentinel. 22개 클라이언트 동안 main source manifest를 고정했다.
- `build-monitor-agent.ps1`: Java21용 별도 실행 준비 agent 빌드. 모드 JAR에 번들하지 않는다.
- `run-wayfarer-stability.py`: 직렬 실행, 이전 로그 덮어쓰기 거부, Windows 프로세스 종료 확인, 전체 세이브 폴더 복사와 해시 대조, 다른 월드 전체 파일의 비변경 확인.
- `verify-wayfarer-stability.py`: 10개 crash snapshot, 10개 exception snapshot, 두 Verify 로그, 두 복사 manifest, 프로세스 및 source hash를 다시 읽는 독립 검사.
- `WayfarerArchiveProbe`: 실제 JAR의 순수 presence policy 8개와 fixture revision 확인. 게임 플레이 검증을 대신하지 않는다.
- `package-wayfarer-stability.py`, `verify-wayfarer-transaction.ps1`: 채택된 baseline 보존, 변경 JAR 별도 생성, diff/source archive, JAR-only rollback 및 복원 후 재검사.
- `SavedDataLoadProbe`, `probe-saved-data-load.ps1`: 실제 NeoForge/Minecraft 저장 라이브러리에 정상/UUID 누락 NBT 복사본을 넣어 다음 단계 결함을 재현. 별도 클라이언트 실행이나 복구 성공 시험이 아니다.

## 검증 결과와 의미

| 항목 | 결과 | 실제 증거 |
|---|---|---|
| baseline GameTest | PASS | 48개, 기존 소스 기준 재실행 |
| modified GameTest | PASS | 동일 48개 유지, 기존 테스트 두 파일 byte-identical |
| core assertions / build | PASS | 176857 assertions, build exit 0 |
| 현재 D 포함 A/B/C/D/E/Verify | PASS | 두 seed, 12개 독립 프로세스, 저장 파일 재검사 |
| 사망/리스폰 | PASS — Wayfarer 한정 | KeepInventory OFF/ON 각각 두 월드, cp1/cp2, generation 0 유지 |
| 실제 actor 청크 언로드/귀환 | PASS — Wayfarer 한정 | 서로 다른 HOME/actor 청크, entity storage까지 언로드, 100 ticks 유지, 두 월드 |
| Nether/End 왕복 | PASS — Wayfarer 한정 | 각 차원 두 월드, 부재 중 중단, 귀환 시 동일 정체성 |
| 실제 세이브 폴더 복사 독립성 | PASS — Wayfarer 한정 | 두 쌍, 66/67개 전체 파일 동일 복사, 분기 진행·각 재접속·타 월드 해시 비변경 |
| 오른쪽 보조 DISPLAY1 / 무음 | PASS | 실제 22개 창 bounds 및 모든 sound category 0 |
| JAR rollback | PASS | 검증 복사본만 baseline hash/패키지 동작 복원, MODIFIED_FILE 유지 |
| UUID 누락 시 안전한 로드 중단 | **FAIL — 저장 라이브러리 수준** | parser 오류 후 빈 NarrativeData 생성 재현 |
| 손상된 월드 실제 클라이언트 복구 | NOT TESTED | 위 라이브러리 결과를 클라이언트 복구로 승격하지 않음 |
| 실제 NPC/복합 사건/아이템/보상/fallback | NOT TESTED | Wayfarer fixture는 대체 증거가 아님 |
| 자연 생존 전체 진행 | NOT TESTED | 22회는 개발 트리거 기반 실제 클라이언트 자동화임 |
| 설계 v0.4 문서 채택 | 반영 | 25항목 및 10개 장면 품질 게이트, runtime 변경 없음 |
| 설계 v0.4 장면·감정·연출 품질 | NOT TESTED | 문서만으로 공포/감동 효과를 PASS 처리하지 않음 |
| 전체 캠페인 안정화 | **PARTIAL** | Actor 손상 로드 결함과 후속 주요 기능이 남음 |

22개 클라이언트의 런처 포함 합계는 1500.923초(약 25분 1초)다. 이것은 전체 개발 시간이나 자연 플레이 시간, Caveman 성능 비교가 아니다. 모두 서로 다른 PID의 종료까지 확인했다. A–E 강제 중단 JVM의 Windows exit -1073740791은 주입한 halt의 결과이며, Gradle exit 0만으로 통과시키지 않았다. named marker, 디스크 checkpoint, 다음 프로세스 복구를 함께 요구했다.

사망·차원·일반 청크 부재에서는 generation=0이 유지됐다. 반면 A/B/C의 엔티티가 디스크에 쓰이기 전 강제 종료되는 생성 경계에서는 재접속 시 실제 누락 재구성으로 generation이 증가한다. 모든 crash에서 UUID/generation이 불변이라고 주장하지 않는다. 오래된 generation의 모든 지연 로드·완전 티켓 소실·손상 복구 조합은 별도 미완료 축이다.

## 발견·수정 이력

1. **D에 도달하지 않는 과거 테스트** → 42블록 이탈 → 현재 정책의 suspend와 과거 완료 oracle 불일치 → ActorSmoke D → 실제 전투 종료로 교체 → 두 ABCDE/Verify 연속 실행 → PASS. 게임 규칙은 유지했다.
2. **HOME만 비어 있는 것을 actor 언로드로 오판할 수 있는 검증 공백** → actor가 HOME 바깥 청크에 존재 → 등록/실제/스냅샷 위치 구분 부족 → ActorExceptionsSmoke → 별도 청크 이동 + terrain/entity storage/UUID/100-tick 확인 → 두 seed 실제 왕복 → PASS.
3. **복사 월드 검증 PARTIAL** → 단순 NBT 왕복이나 같은 방향 진행으로 독립성 과대평가 가능 → 전체 디렉터리 복사/재접속 증거 부족 → runner 및 CopyReload → 원본완료/복사중단 후 각각 다시 열기 + 다른 전체 월드 해시 비교 → 두 쌍 PASS.
4. **ROLLBACK 첫 실행 실패** → Windows Bash 프로세스 초기화 exit -1073741502 → 검증 스크립트 본문 실행 전 도구 실행 환경 실패 → 원래 실패 로그 보존 후 동일 JAR-copy 스크립트를 적절한 실행 권한으로 재시도 → exit 0, baseline 해시/동작 복원 PASS. 게임 코드 수정으로 해결한 문제가 아니다.
5. **저장 손상을 빈 상태로 바꾸는 결함 — 미수정** → 실제 Seed1 checkpoint 복사본에서 entityUUID 제거 → StoryActorRecord.load가 예외를 던지나 DimensionDataStorage.readSavedData가 catch 후 null 반환, computeIfAbsent가 새 NarrativeData 생성 → 수정 대상 NarrativeData.get의 default-constructor/fail-closed 경계 → 정상 control actor1, 손상 input actor1에서 loadedActors0 재현 → FAIL. probe는 저장을 호출하지 않았으며 입력/복사 NBT 파일 비변경을 확인했다. 실제 게임의 디스크 덮어쓰기까지 관측했다는 주장은 하지 않는다.
6. **진단 프로그램 종료 지연** → 라이브러리 검사 완료 후 Minecraft 유틸리티 background pool 잔존 → standalone probe에 명시적 정상 종료 추가 → 이전 로그 보존/해당 실행 중단 후 재실행 → 종료 0. 진단 프로그램 정상 종료는 발견된 저장 결함의 PASS가 아니다.

## 설계 v0.4 채택

docs/DESIGN_V04.md가 상위 세계관 계약이다. 현재가 달라질 권리, 관계·빈자리 보존, 합성된 마지막 하루, 인간 비상규칙의 집합인 기록관을 채택한다. 일곱 형체는 일곱 고정 희생자라는 초기 결론을 피한다. Director는 의미/표현/위협/회복을 분리하고 NoEvent와 실제 휴식을 허용한다. 주요 심리 장면은 No-Spawn/No-Stinger/Free-Look를 함께 목표로 한다.

프롤로그+7장, 조용한 「내일의 자리」, Three Lives Rule, 도구의 세 번째 파형, 10개 장면 품질 게이트를 기록했다. 교정 전선은 플레이어 창작물을 임의 삭제하는 허가가 아니며, 서사적 기억 교정과 실제 저장 손상을 구분한다. 연속기의 관계 정체성과 현재 actor runtime 정체성을 같은 필드로 취급하지 않는다.

기존 임시 자산 6그룹·떡밥 초안 2건·69개 asset/data 파일은 유지한다. 리소스팩·최종 아트·퍼즐·고급 연출·Horror Director 전체 구현을 앞당기지 않았다. 외부 작품/연구에 대한 사용자 제공 참고 주장은 이번에 외부 검증하지 않았다.

## 모델 / Caveman

세션 기록 model=gpt-6-astra, effort=high. Terra/Luna 위임 없음. Caveman HOLD/off, 새 A/B 실험 없음. 이번 입력/출력/reasoning/전체 토큰과 절감률을 새 실험 수치로 측정하지 않았다. 과거 절감률을 이번 작업의 절감률로 재사용하지 않는다. 최종 보고 축약 없음.

## 다음 시작점과 미완료 항목

**가장 먼저 NarrativeData.get의 실패한 기존 파일을 신규 상태로 바꾸는 경로를 막는다.** 파일 부재와 파일 로드 실패를 구분하고, 기존 데이터가 있으면 재해석 없는 MANUAL_DIAGNOSTIC 또는 명시적 복구 정책으로 진행한다. 기존 정상 파일·파일 부재·읽기 실패·신규 schema를 별도 검사한다. 다음 실제 클라이언트 손상 복사본에서도 원본 보존과 재접속/재검사까지 증명한다.

그 후 entityUUID 누락/오류, generation 누락/음수, lifecycle/checkpoint/instance 오류, dimension/position/snapshot 오류, storyId 중복, 미지원 actorSchema를 등급별로 처리한다. 완전 누락 티켓의 generation 예약→이전 세대 폐기→생성→UUID 확정→checkpoint 연결 중단 경계를 검사한다. 이후 실제 NPC → 복합 사건 → 중요 아이템 → 보상 → 필수 구조물 fallback → 통합 Integrity → 자연 생존 순서다.

NPC A–E, NPC↔NPC 관계, 복합 사건 A–E, item ownership/소실 및 crash boundary, reward ledger/가득 찬 인벤토리, fallback 생성 중 종료, 전체 migration 결정성/확대 스트레스, 모든 엔딩/후일담, 자연 생존 1차·반복 QA는 이번 증거로 완료되지 않았다. 출시 상태는 계속 PARTIAL이며, 0.3.2와 최종 아트로 넘어가지 않는다.

## 재개용 작업 지시

```text
dist/wayfarer-stability와 evidence/wayfarer-stability를 보존한다.
현재 0.3.1-dev.1 / PARTIAL을 기준으로 docs/DESIGN_V04.md를 상위 설계로 사용한다.
NarrativeData.get에서 실제 기존 파일 로드 실패가 빈 NarrativeData로 바뀌는 경로부터 수정한다.
saved-data-probe.log의 정상1/손상0 재현을 회귀로 전환하되 오류를 단순 정상화하지 않는다.
원본 세이브는 건드리지 말고 복사본에서 Detect → Recover/명시적 중단 → Save/보존 → Reload → Integrity 재검사를 수행한다.
새 런타임 코드의 회귀와 필요한 실제 클라이언트 증거를 새 경로에 남긴다.
오른쪽 DISPLAY1, 무음, 직렬 실행을 유지한다. Caveman은 HOLD, 최종 아트는 보류한다.
```
