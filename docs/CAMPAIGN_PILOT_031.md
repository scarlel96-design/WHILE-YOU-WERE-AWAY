# Current: 《네가 없는 동안》 0.3.1-dev.1 / campaign-pilot / PARTIAL

## 1. 이번 범위와 결정

입력은 lifecycle-work이며 기존 산출물을 덮어쓰지 않았다. 이번 작업은 Caveman의 제한된 실제 코딩 A/B 시험과, 앞서 진행하던 추격 부재 처리의 검증을 묶은 개발 작업본이다. 0.3.2가 아니며 캠페인 공통 안정화 완료본도 아니다.

**결론: 전면 Caveman 적용 보류.** 코딩 구간의 출력은 13.99% 감소했지만 입력을 포함한 전체 토큰은 30.92% 증가했다. 이는 단일 순차 비교에서 관측한 차이이며 Caveman의 인과적 효과, 요금 절감률 또는 계정 한도 절감률이 아니다. 작은 함수의 정확도는 두 안 모두 검사에 통과했으나, 장기 프로젝트 품질 동등성은 입증하지 않았다. 시험 구간 종료 후 Caveman 스타일도 종료했다. 전역 Skill 설치, Proxy, 설정 변경, 인증 복사, 외부 에이전트 실행은 하지 않았다.

## 2. 기준점

- Minecraft Java 1.21.1 / NeoForge 21.1.249 / Java 21 / mod 0.3.1-dev.1.
- 입력 JAR: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\dist\lifecycle-work\whileaway-0.3.1-lifecycle-work.jar`
- 입력 JAR SHA-256: `07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255`.
- 입력 source ZIP: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\dist\lifecycle-work\whileaway-0.3.1-lifecycle-work-source.zip`
- 입력 source SHA-256: `cc1a1899f1b16c317b8cf4022b406fd4c4e20db51f8fafa662d39647bd3597a0`.
- 저장 형식 4, optional actorSchema=1, 도시 art revision 2 유지.
- 지원 경로 1/2/3 → 4의 구현은 변경하지 않았다. 이번 회차에서 전체 450회 저장 스트레스나 200회 Actor NBT 왕복을 다시 실행했다고 주장하지 않는다.
- 입력 두 파일과 복제 baseline 해시는 재확인했다. 기존 구형 월드 story 파일 2개의 해시도 그대로였다.

## 3. Caveman 출처와 적용 경계

지정 저장소만 사용: https://github.com/JuliusBrussee/caveman

고정 commit: `15581d14007fd01fb3f132016741962f34936ca2`

고정 Skill: https://github.com/JuliusBrussee/caveman/blob/15581d14007fd01fb3f132016741962f34936ca2/skills/caveman/SKILL.md

Skill SHA-256: `c4d7354b4b063d54601fcdd5097a5b1713d1a1a2e386ac39efa438aa1ffef8ce`

업스트림 규칙 원문·라이선스·Honest Numbers를 evidence/caveman-pilot에 보존했다. 이 Skill은 출력 스타일 지침이며 입력 압축 Proxy와 구분된다. 소스/명령/오류/숫자/부정 조건은 보존하고 설명의 군더더기만 줄이는 full 모드를 B 구간에 수동 적용했다. 사용자가 요구한 짧은 진행 로그와 상세 최종 보고는 업스트림의 진행 로그 생략·세션 전체 지속 지침보다 우선한다. 따라서 본 결과는 **사용자 범위에 맞춘 Skill-only 시험**이며 Proxy나 무수정 전체 설치의 성능 결과가 아니다.

공식 API의 usage 설명도 확인했지만 실제 숫자는 API 예제나 README가 아니라 **이 작업의 Codex 세션 token_usage_record**에서 읽었다. 세션 원문·추론 내용은 산출물로 복사하지 않았고 숫자·시각·응답 식별자만 보존했다.

## 4. 실제 코딩 비교 방법

동일한 `StoryActors.ownerPresent` 작업 시작 소스를 A/B에 사용했다. A 종료 후 원본 바이트와 해시를 복원한 뒤 B를 작성했다. 두 안은 다음 같은 실제 요구를 구현했다.

1. 소유자가 없는 actor는 플레이어 조건을 요구하지 않는다.
2. 소유자가 있으면 온라인·생존·동일 차원이어야 한다.
3. 추격 사건만 Home snapshot 또는 등록 위치를 기준으로 42블록 범위를 적용한다.
4. 경계는 포함하고 바로 바깥은 제외한다.
5. NaN·무한·음수 입력은 제한된 범위 판단에서 거부한다.
6. 범위 비제한 사건은 거리 데이터를 요구하지 않는다.
7. 이 함수는 사건 완료·실패·generation·UUID·아이템·저장 상태를 변경하지 않는다.
8. 기존 Minecraft adapter의 null owner/player 처리와 모든 나머지 함수는 보존한다.

단순 문장 길이 비교가 아니다. 두 안 모두 Java 코드를 작성하고 실제 adapter에 연결했으며, 같은 독립 테스트 334개와 기존 GameTests 48개를 실행했다. A를 일부러 장황하게 만들지 않았다. B 산출물을 현재 작업본에 남겼다.

### 측정 구간

A: 2026-09-10T10:28:01.858181+00:00 ~ 2026-09-10T10:29:52.540901+00:00

B: 2026-09-10T10:30:00.523797+00:00 ~ 2026-09-10T10:31:44.489319+00:00

| 항목 | A 미적용 | B 제한 적용 | 관측 절감률 (A-B)/A |
|---|---:|---:|---:|
| 전체 토큰 | 441,723 | 578,322 | -30.92% |
| 입력 토큰 | 439,478 | 576,391 | -31.15% |
| 캐시된 입력 | 435,328 | 573,440 | -31.73% |
| 캐시되지 않은 입력 | 4,150 | 2,951 | +28.89% |
| 출력 토큰 | 2,245 | 1,931 | +13.99% |
| reasoning 출력 | 181 | 93 | +48.62% |
| reasoning 제외 출력 | 2,064 | 1,838 | +10.95% |

- 코딩 완료 시간: A 110.683초, B 103.966초. 관측상 6.07% 짧았다.
- Gradle 자체 실행 시간은 A 50초, B 52초였다. 모델 작업 시간 감소와 빌드 시간 감소는 같지 않다.
- 계측된 모델 응답 수: A 4회, B 5회. 도구 대기 확인 과정 차이도 입력 비용에 반영된다.
- 전체 토큰 = 입력 + 출력. 캐시 입력은 입력에 포함되며 reasoning은 출력의 하위 항목이다. 다시 더하지 않았다.
- 출력은 코드·도구 호출·설명·reasoning을 포함하는 실제 기록이다. reasoning 제외 출력도 별도 기재했다.
- 이 표는 **각 코딩 구간 합계**다. 저장소 조사·측정 도구/공통 oracle 작성·규칙 읽기·공통 추가 검증·클라이언트 시험·최종 보고 비용은 구간 밖이다. 전체 파일럿 운영 비용이 표의 합계라고 주장하지 않는다.

### 해석 한계

같은 세션에서 A 다음 B를 수행한 1쌍이다. B는 A의 해결 방법을 이미 본 상태이고, A도 저장소 규칙을 사전 조사한 상태이다. 독립 모델·무작위 순서·동일 초기 문맥·반복 표본이 아니다. 문맥 길이, 캐시, 모델 응답 횟수가 다르다. 이 때문에 총 토큰 +30.92%, 출력 -13.99%, reasoning -48.62%를 Caveman 자체의 확정 효과로 분리할 수 없다. 특히 reasoning 감소를 사고 품질 유지의 근거로 삼지 않는다. 캐시되지 않은 입력 감소 역시 요금 절감률로 환산하지 않았다.

## 5. 정확도·검증·문제 탐지·연속성 평가

| 항목 | A | B | 판정 범위 |
|---|---|---|---|
| Java compile | PASS | PASS | 실제 Minecraft adapter 컴파일 포함 |
| 독립 정책 검사 | 334/334 | 334/334 | 100% / 100% |
| 기존 GameTests | 48/48 | 48/48 | 100% / 100%, 원본 테스트 소스 2개 byte-identical |
| 제한 작업 요구사항 누락 | 발견 0 | 발견 0 | 명시한 8가지 요구에 한정 |
| 소스 디버깅/재작업 | 0/0회 | 0/0회 | 두 코딩 구간 내 실패 없음 |
| 오류 주입 민감도 | 8/8 | 8/8 | 공통 사후 검사이며 모델 탐지 능력 검사가 아님 |
| 실제 클라이언트 비교 | NOT TESTED | 비비교 1회 성공 | A/B 품질 동등성 근거로 사용하지 않음 |
| 장기 세션 연속성 | NOT TESTED | NOT TESTED | 새 세션 복구/장편 캠페인 반복 표본 없음 |
| 모델의 미지 오류 탐지력 | NOT TESTED | NOT TESTED | 블라인드 과제·독립 평가자 없음 |
| 전체 품질 무저하 | PARTIAL | PARTIAL | 단일 작은 함수 검사만으로 일반화하지 않음 |

8개 주입 오류는 경계 `<` 오염, 온라인 조건 제거, 생존 조건 제거, 차원 조건 제거, unowned 거부, unbounded 거부, 음수 거리 허용, 음수 반경 허용이다. 16개 변형을 모두 컴파일한 후 독립 테스트가 모두 exit 1로 잡았다. 실제 오류 문자열은 다음처럼 보존했다.

```text
Exception in thread "main" java.lang.AssertionError: ACTOR_PRESENCE_MISMATCH: flags=31 distance=1764.0
```

`StoryActors.java`의 A/B adapter는 바이트 단위로 같다. 정책 함수는 A의 묶인 입력 검사와 B의 분리된 두 입력 검사, 주석 표현이 다르며 동일 oracle을 통과했다. 코드를 축약 문법이나 식별자 변경으로 압축하지 않았다. 양쪽 모두 같은 javac/java/Gradle 명령과 `--offline`, 테스트 클래스명을 사용했다. 전체 로그는 그대로 보존하고 화면 출력만 일부 요약했다. 코드/명령/오류를 줄이지 않았다는 주장은 이 관찰 범위에 한정하며 모든 향후 출력에 대한 보장은 아니다.

최종 보고서와 저장 문서는 정상 문장으로 작성했다. 다만 장기 연속성 향상 또는 무저하를 실제 실험으로 검증한 것은 아니다. 최종 보고서를 보존했다는 사실과 미래 세션의 이해도를 구분한다.

## 6. 실제 프로젝트 변경과 이유

### StoryActors.ownerPresent / ActorPresencePolicy.canRun

사망·접속 종료·차원 이탈·사건 반경 이탈을 결과 확정이 아닌 실행 가능 여부로 판단한다. 순수 함수로 분리하여 테스트할 수 있게 했다. `rangeLimited`는 Wayfarer 추격에만 참이며, `Home` snapshot이 없을 때 등록 position을 사용한다. 저장 필드 변화는 없다.

### StoryActors.reconcile

canonical actor가 있어도 owner가 부재하면 `suspend`한다. actor가 로드되지 않은 상황에서 owner까지 부재하면 준비 pass를 지우고 SUSPENDED를 저장한 뒤 반환한다. 아직 청크가 준비되지 않았거나 플레이어가 부재한 상태를 실제 actor 누락으로 오인해 generation을 올리는 경로를 차단한다. 기존 storageReady와 2회 확인·canonical fencing 구조는 유지했다.

### StoryActors.checkpoint

같은 checkpoint에서 재개할 때 실행 상태가 SUSPENDED로 남지 않게 한다. 0/1/2단계는 PREPARING/PHASE_1/PHASE_2, 나머지는 RESOLVING으로 재설정한 뒤 capture와 동기 persist를 수행한다. 영구 사실이나 완료 단계를 되돌리지 않는다.

### Wayfarer.tick

기존 `distance(home)>42`의 `finish(true)`를 제거했다. 플레이어가 멀리 리스폰하거나 청크를 떠났다는 이유로 완료되는 정책을 수정한 것이다. 이제 navigation을 정지하고 windup을 확보한 뒤 일시중단한다. active-time 2400 / hunt 600 및 전투에 의한 기존 완료 경로는 유지했다.

### 검사·개발 실행

- `ActorExceptionsSmoke`: 실제 Wayfarer를 사용한 개발 전용 opt-in 클라이언트 검증. death cp1/cp2, keepInventory OFF/ON, 청크 이탈, Nether/End 왕복, 저장 snapshot을 기록한다. 원본/복사/재로드 모드도 코드가 있으나 이번에 실행하지 않은 모드는 NOT TESTED다.
- `ActorPresenceTests`: 동일 oracle 334개. `tools/test-core.ps1`에 연결했다.
- `tools/verify-presence-pilot.py`: 공통 사후 변이 검사와 원본 oracle 해시 확인.
- `tools/pilot-meter.py`: 현재 작업 세션 숫자만 읽고 A/B 시작·종료 및 각 응답 usage를 보존한다.
- `tools/MonitorAgent.java`, `tools/select-test-monitor.ps1`, build.gradle: 개발 JVM의 GLFW 창을 사용자 지정 오른쪽 DISPLAY1에 배치. 모드 배포 JAR에 Java agent를 넣지 않는다. 일반 .minecraft와 OS 전역 볼륨은 변경하지 않았다.
- `tools/PresenceArchiveProbe.java`, `tools/package-campaign-pilot.py`: 신규 출력 경로만 사용하며 원본 보존·JAR 동작 probe·rollback 검증·source manifest를 관리한다.

### 저장 순서

부재 감지 시 lifecycle=SUSPENDED, checkpoint.suspend(reason), 필요 시 actor capture, 동기 story persist 순서이다. actor가 없는 부재 경로는 generation/UUID를 예약하거나 새 엔티티를 생성하지 않는다. 재개는 이미 확정된 사실을 유지한 채 같은 checkpoint의 실행 상태만 복원한다. 아이템/보상/구조물 transaction은 이번에 추가하지 않았다.

## 7. 실제 Minecraft 검증: 자동 서버와 클라이언트 구분

- baseline `./gradlew.bat --offline runGameTestServer`: 48 PASS, BUILD SUCCESSFUL in 51s, exit 0.
- A/B 각각 `./gradlew.bat --offline compileJava runGameTestServer`: 48 PASS, exit 0.
- `./tools/test-core.ps1`: `PASS core assertions=176857`, `PASS actor presence policy checks=334`, exit 0. 176857은 주로 기존 core/layout 단언 수이며 신규 기능 수가 아니다.
- 최종 `./gradlew.bat --offline build`: BUILD SUCCESSFUL in 15s, exit 0.
- `./gradlew.bat --offline runActorExceptionsSeed1`: 실제 클라이언트 1회, BUILD SUCCESSFUL in 2m 15s, exit 0.

클라이언트 실제 결과:

```text
RUNNING
PASS monitor DISPLAY1 right-secondary actual=2680,120 size=854x480; master=0
PASS death_respawn keepInventory=false checkpoint=1 generation=0
PASS death_respawn keepInventory=true checkpoint=2 generation=0
PASS actual_chunk_unloaded hasChunk=false entitiesLoaded=false generation=0
PASS return scenario=1 canonical_count=1 same UUID/instance/generation/checkpoint
PASS dimension_absence=Nether generation=0
PASS return scenario=2 canonical_count=1 same UUID/instance/generation/checkpoint
PASS dimension_absence=End generation=0
PASS return scenario=3 canonical_count=1 same UUID/instance/generation/checkpoint
PASS runActorExceptionsSeed1 checkpoint=2 generation=0 instance=a7398828-cefb-4a28-8f36-8093b3473242 actor=a6fc2711-4f84-4415-8bcf-3a0a28b164f8
```

이는 새 시험 월드 한 개에서 cp1/cp2 사망, keepInventory OFF/ON, 청크 이탈 및 Nether/End 왕복 후 같은 UUID·instance·generation·checkpoint와 근처 actor 1개가 유지됨을 보여준다. 부재 중 snapshot의 StoryAge/HuntTicks가 증가하지 않음도 단언했다. 저장 파일은 schema4/actorSchema1, checkpoint2, generation0, SUSPENDED로 다시 읽었다.

청크 언로드 gate는 HOME 청크의 `hasChunk=false`, `areEntitiesLoaded=false`를 관찰했다. 실제 이동한 actor의 snapshot position 청크도 별도로 단언하는 보강은 아직 필요하다. 따라서 모든 actor 청크 저장 상황까지 완전히 검증했다고 확대하지 않는다.

이 실행은 순간이동·강제 사망을 사용하는 **스크립트형 실제 클라이언트**이다. 자연 생존 플레이가 아니며, 새 프로세스로 다시 여는 검사·복사 월드·각 시나리오 2회 연속은 아직 수행하지 않았다. 이전 lifecycle의 12회 성공은 과거 증거이며 현재 수정본의 새 12회로 합산하지 않는다.

화면 실제 좌표 2680,120, 창 크기 854×480이 오른쪽 DISPLAY1 bounds 2560,0,2560,1440 안에 있음을 GLFW에서 확인했다. 창 크기는 요청한 내부 기본값과 달랐지만 사용자 요구인 오른쪽 모니터 배치는 통과했다. 실행 전 음량 10종=0.0, narrator=0; TitleScreen master=0을 확인했다. 실제 음향을 듣는 검증은 하지 않았다.

## 8. 발견 문제와 수정·재검증 이력

1. **반경 이탈이 완료 처리됨**: 멀리 리스폰/이탈하는 상황의 코드검토에서 `distance>42 -> finish(true)` 정책 충돌 발견. `Wayfarer.tick`을 suspend로 바꾸고 StoryActors owner gate를 공통화했다. 새 코드의 실제 사망/이탈/귀환은 성공했다. 구판 JAR로 동일 실패를 재현한 것으로 주장하지 않는다.
2. **재개 후 checkpoint 실행 상태 잔류 위험**: 같은 단계 저장 시 SUSPENDED가 유지되는 분기 발견. `StoryActors.checkpoint`에서 같은 phase 재개 상태를 복원했다. 서버 회귀와 새 클라이언트 경로를 통과했으나 모든 사건 adapter 검증은 남았다.
3. **계측 준비 중 다운로드 연결 거절**: 로컬 Python의 GitHub 요청 exit 1. 승인된 네트워크 실행으로 동일 지정 저장소의 고정 commit 세 파일을 내려받아 해시를 기록했다. 전역 설치는 하지 않았다.
4. **실행 준비 shell 상태값 문제**: PowerShell 스크립트 뒤의 미설정 LASTEXITCODE를 검사해 첫 명령이 클라이언트를 시작하기 전에 끝났다. PowerShell 오류는 ErrorActionPreference=Stop으로 처리하고 실제 native Gradle exit만 검사하여 재실행했다. 이 준비 실패를 Minecraft 실행 횟수에 넣지 않았다.
5. **rollback 실행 환경 오류**: Git Bash가 `fatal error - couldn't create signal pipe, Win32 error 5`, exit -1073741502로 시작 실패. 같은 검증용 JAR 복사본에 승인된 실행 문맥으로 재실행해 exit 0·원본 해시·기능 probe 복원을 확인했다. 최초 실패 로그는 별도 보존했다. World 파일은 건드리지 않았다.
6. **기존 ActorSmoke D와 정책 충돌**: 기존 D fixture가 반경 이탈을 완료 수단으로 삼는다. 현재 부재=suspend 정책과 맞지 않으므로 다음 전체 ABCDE 재검증 전에 전투 종료 같은 정당한 완료 경로로 fixture를 갱신해야 한다. 이번 회차에서 그 fixture를 통과했다고 주장하지 않는다.

A/B 코딩 구간 자체는 둘 다 컴파일/테스트 실패·소스 재작업 0회였다. 위 준비/통합 문제를 A/B 성공 숫자로 숨기거나 각 안의 코드 결함으로 혼동하지 않는다.

## 9. 상태표와 잔여 작업

| 항목 | 상태 | 남은 조건 |
|---|---|---|
| 동일 작업 Skill-only 파일럿 | PARTIAL | 1쌍 관측 완료, 독립·반복·순서 교차 비교 없음 |
| 작은 정책 코드 자동 검증 | PASS | 334+48 양쪽 통과, 8변이 양쪽 탐지 |
| Wayfarer 사망/차원/청크 | PARTIAL | 실제 1회 성공, actor 위치 청크 보강 및 2회 반복/재접속 필요 |
| actor 복사 월드 독립성 | NOT TESTED | 새 adapter 기준 실제 폴더 복사 및 양쪽 실행 필요 |
| Actor 손상 복구 | NOT TESTED | UUID/schema/dimension/negative generation 등 분류·복구 필요 |
| 옛 actor 완전 소실 티켓 | NOT TESTED | 실제 누락 확인·generation 예약 crash boundary 필요 |
| 실제 NPC ABCDE / NPC↔NPC | NOT TESTED | campaign NPC adapter 미구현 |
| 실제 복합 사건 ABCDE | NOT TESTED | 4요소 이상 연결 필요 |
| 중요 아이템 / 보상 transaction | NOT TESTED | 소유권 ledger·crash boundary·소실 복구 필요 |
| 필수 구조물 fallback | NOT TESTED | registry·생성 예약·접근성·생활권 보호 필요 |
| 캠페인 공통 Integrity | PARTIAL | 기존 actor/event 검사만 존재, 전체 통합 미완성 |
| 구형 schema migration | PARTIAL | 기존 코드/회귀 유지, 이번 전체 스트레스 재실행 없음 |
| 현재 수정본 전체 ABCDE client 회귀 | NOT TESTED | 기존 D fixture 완료 수단 수정 후 재실행 |
| 자연 생존 전체 진행 | NOT TESTED | 앞 안정화 완료 후 수행 |
| 최종 아트/리소스팩/퍼즐 확장 | 보류 | 현재 69 asset/data 항목 바이트 동일 |
| 전체 안정화 / 0.3.2 승격 | PARTIAL / 보류 | 사용자 지정 모든 게이트 충족 전 승격 금지 |

저장 형식과 기존 필드 의미를 이번에 변경하지 않았으며 save downgrade를 추가하지 않았다. JAR rollback은 save downgrade와 별개이다. 기존 0.2 도시와 0.3 art revision 2 보존 정책은 그대로다. 새 중요 아이템/보상/구조물 schema는 아직 없다.

## 10. 산출물과 재현

- MODIFIED_FILE: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\dist\campaign-pilot\whileaway-0.3.1-campaign-pilot.jar`
- MODIFIED SHA-256: `9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8`
- SOURCE_ZIP: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\dist\campaign-pilot\whileaway-0.3.1-campaign-pilot-source.zip`
- DIFF_FILE: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\DIFF.patch`
- VERIFICATION: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\VERIFICATION.txt`
- ROLLBACK: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\ROLLBACK.sh`
- 파일럿 원시 숫자: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\caveman-pilot\usage.json`
- 파일럿 oracle/변이 근거: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\caveman-pilot\quality.json`

작업 디렉터리: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away`

환경: JAVA_HOME=`C:/Program Files/Eclipse Adoptium/jdk-21.0.5.11-hotspot`, GRADLE_USER_HOME=`C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\gradle-home`. Gradle은 이 checkout에서 직렬로 실행한다.

### BASELINE
명령: `java -cp build/presence-probe PresenceArchiveProbe evidence/campaign/baseline/previous.jar absent`

입력: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\baseline\previous.jar`

실제 출력:
```text
PASS archive policy=ABSENT (baseline/rollback)
```
exit code: 0

결과 SHA-256: `07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255`

### MODIFIED
명령: `java -cp build/presence-probe PresenceArchiveProbe dist/campaign-pilot/whileaway-0.3.1-campaign-pilot.jar present`

입력: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\dist\campaign-pilot\whileaway-0.3.1-campaign-pilot.jar`

실제 출력:
```text
PASS archive policy=PRESENT checks=8
```
exit code: 0

결과 SHA-256: `9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8`

### ROLLBACK
명령: `"C:/Program Files/Git/bin/bash.exe" evidence/campaign/ROLLBACK.sh evidence/campaign/rollback-target.jar`

입력: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\rollback-target.jar`

실제 출력:
```text
ROLLBACK PASS: lifecycle-work JAR restored; world files untouched
```
exit code: 0

결과 SHA-256: `07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255`

### ROLLBACK_VERIFY
명령: `java -cp build/presence-probe PresenceArchiveProbe evidence/campaign/rollback-target.jar absent`

입력: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\outputs\while-you-were-away\evidence\campaign\rollback-target.jar`

실제 출력:
```text
PASS archive policy=ABSENT (baseline/rollback)
```
exit code: 0

결과 SHA-256: `07e888a09229b5830e2e1cf866c34d89dac7d7b0c246608b6e6dcf31ee75c255`

Archive probe는 순수 정책 클래스 존재와 8개 호출을 확인한다. 이것을 Minecraft 전체 플레이나 rollback 후 월드 downgrade 성공으로 대체하지 않는다. rollback은 별도 JAR copy만 원래 lifecycle JAR로 되돌렸고, MODIFIED_FILE은 변경된 상태로 남았다.

## 11. 다음 시작점

**첫 번째:** `ActorExceptionsSmoke`의 청크 gate에 실제 actor snapshot 위치의 청크 확인을 추가하고, `ActorSmoke` D를 새 suspend 정책에 맞는 실제 전투 완료 경로로 갱신한다. 이전 evidence/lifecycle을 덮어쓰지 않는 새 프로필·증거 경로와 오른쪽 모니터·무음 설정을 사용한다. 새 프로세스 재접속 및 실제 복사 월드 Original/Copy/Reload를 실행하고 두 번 반복한 뒤 4축을 판정한다.

그 다음 Actor 손상 복구/옛 소실 티켓, 실제 NPC, 복합 사건, 아이템, 보상, 구조물 fallback, 공통 Integrity, 자연 생존 순서를 유지한다. 현재 새 모드 기능을 확장하거나 0.3.2로 올리지 않는다.

Caveman은 전면 적용하지 않는다. 추가 비교를 한다면 사용자 결정 후 동일 시작 문맥의 독립 반복/교차순서 및 블라인드 결함·인수인계 과제가 필요하다. 이번 보고만으로 장기 품질 무저하 또는 확정 절감률을 선언하지 않는다.
