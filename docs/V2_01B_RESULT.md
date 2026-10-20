# [MAIN CODEX/ASTRA RESULT — V2-01B]

## Current

- Version: `0.3.1-dev.1`.
- Branch: `work/v2-playable-core`.
- Baseline: `47b440befc881fc25b60374360ba4b07c88dbb4a`.
- Implementation: `0c1a97e823a0ef9974db6d9a07b2ee1eb04b3c8d`.
- V2-01: **PARTIAL**. Campaign: **PARTIAL**.
- Return Network retains its previously reported status. This run did not repeat its client matrix.
- Save format: 5. Actor schema: optional 1. Return Network schema: 1. Art revision: 2.
- Caveman: HOLD. Version 0.3.2: NOT PROMOTED. V2-02: NOT STARTED.

## 1. Game content implemented

여울의 일상 동선에 오전 진입로 점검을 추가했다. 오후에는 귀환등을 돌보고, 이후에는 집으로 돌아간다.
Return Network가 시작된 뒤에도 일반 생활 대화를 제공한다. 완료 이후에는 웅크린 상호작용으로 사건 맥락 대화를 제공한다.
역 재방문 문구는 긁힌 자국과 발자국의 방향을 전달한다. 플레이어 대신 Returner의 의도를 확정하지 않는다.

## 2. Fixed gameplay defects

### Returner 종료와 목격

`Wayfarer.tick`은 실제 엔티티 종료와 별도로 `player_observed_returner` 사실을 기록한다.
조건은 32블록 이내, 차폐 없음, 시선 방향 내 노출, 8회의 연속 서버 tick이다.
시선 방향은 정규화 벡터의 내적 `>=0.35`로 판정한다. 이는 인지 기회의 근거이며 인간의 실제 인지를 증명하지 않는다.
소유자 부재·사망·차원 이동·지역 이탈·비활성 조건에서는 누적 노출 시간을 초기화한다.
부분 노출 시간은 저장하지 않는다.

`StoryActors.confirmChase`는 목격 사실이 있는 경우에만 후속 알림과 음악을 제공한다.
도착 또는 수명 종료는 기존처럼 encounter를 정리한다. 미목격을 이유로 무한 재생성하지 않는다.
`StationBlock.useWithoutItem`은 미목격 방문에 별도 중립 문구를 제공한다.

### 최초 Presence 문구

`StoryEvents.manifest`에서 `story.whileaway.arrival` 알림 호출을 제거했다.
기존 경로 차단 경고, 반응 시간, 추격과 이탈 처리는 유지했다.

### 여울 대화와 재방문

`NpcEvents.interact`에서 Return Network 미시작 조건을 제거했다.
`StoryNpc.mobInteract`의 기존 호출 순서가 필수 사건 상호작용을 먼저 처리한다.
`ReturnNetworkEvents.interactNpc`는 완료 사건에서 일반 대화로 처리를 넘긴다.
일반 대화는 NPC 사실, 관계, checkpoint를 변경하지 않는다.
역 상호작용은 반복 입자를 제거했다. 새 아이템이나 중복 영구 사실을 만들지 않는다.

## 3. End-to-end playability

이번 실행에서 새 월드부터 마지막 재방문까지의 연속 플레이는 **NOT TESTED**다.
개발 중 격리 클라이언트 1회는 모드와 사운드 엔진을 로드한 뒤 정상 종료했다.
이 결과는 시작 연기 검사이며 실제 플레이 PASS가 아니다. 최종 소스의 클라이언트 검증으로도 집계하지 않는다.
오른쪽 보조 DISPLAY1 선택과 시작 좌표 설정은 기록됐다. 실제 창 경계와 조작 성공은 확인되지 않았다.

Computer Use 초기화는 초기 실행과 커널 초기화 후 재시도에서 같은 오류를 반환했다.

`failed to write kernel assets: 지정된 경로를 찾을 수 없습니다. (os error 3)`

사용자 일반 프로필과 전역 음량은 변경하지 않았다. 격리 프로필은 시작 전 무음 설정을 사용했다.

## 4. Player knowledge integrity

새 GameTest는 도착 종료가 `encounter_complete`만 기록하고 목격 사실은 생성하지 않음을 확인했다.
같은 테스트는 Actor NBT 왕복 후 두 사실이 계속 분리됨을 확인했다.
양의 목격 판정과 차폐·시선 이탈·노출 중단은 실제 클라이언트에서 추가 확인해야 한다.
옛 세이브에는 목격 사실을 소급 생성하지 않는다. 해당 세이브는 중립 재방문 문구를 사용한다.

## 5. Minecraft survival compatibility

이번 변경은 생존 행동, 플레이어 건축, 인벤토리 또는 월드 블록을 변경하지 않는다.
채굴·제작·건축·탐험을 포함한 연속 생존 경로의 실제 플레이 판정은 **NOT TESTED**다.

## 6. Validation

| 검사 | 결과 | 증명 범위 |
| --- | --- | --- |
| 최종 GameTest | 114/114 PASS | 기존 113개 유지. 미목격 종료·NBT 분리 검사 1개 추가 |
| 최종 Core assertions | 176,858 PASS | 기존 core 계약 회귀 |
| 최종 Gradle build | PASS | 최종 production source 컴파일·패키징 |
| 기준/수정 소스·JAR 검사 | PASS | 10개 소스 hash, JSON 2개, JAR 구조·상수 |
| 소스 ZIP | PASS | 174개 파일이 최종 working source bytes와 일치 |
| 별도 JAR 복사본 rollback | PASS | 이전 기준 JAR hash 및 class bytes 복원 |
| 개발 클라이언트 시작 | 1회 정상 종료 | 모드·사운드 엔진 초기화만 확인 |
| 최종 실제 클라이언트 주요 경로 | NOT TESTED | 새 월드, 정상 대응, Crisis, 회복, 재방문 필요 |
| 인간 체감 | NOT TESTED | 플레이 흐름·공포·가독성 미확정 |

최종 명령은 저장소 루트에서 실행했다. `GRADLE_USER_HOME=..\gradle-home`을 사용했다.

```powershell
.\gradlew.bat --offline --no-daemon runGameTestServer coreTest build --console=plain
```

주요 출력:

```text
114 GAME TESTS COMPLETE IN 12.06 s
All 114 required tests passed :)
PASS core assertions=176858
BUILD SUCCESSFUL in 1m 11s
FINAL_GRADLE_EXIT=0
```

전체 로그: `evidence/v2-playable-core/v2-01b-transaction/final-gradle-host.log`.
앞선 격리 실행은 Gradle daemon 연결에서 실패했다. 그 로그는 `final-gradle.log`에 보존했다.

## 7. Human experience

`HUMAN EXPERIENCE VERIFIED = NOT TESTED`.
`AUDIO READABILITY = NOT HUMAN-VERIFIED`.
이번 작업은 새 음원이나 신호 timing을 변경하지 않았다.

## 8. Save / Actor integrity

스키마와 checkpoint 의미는 유지한다. 새 정보는 기존 Actor `facts` 집합의 선택적 문자열 1개다.
기존 format 5 저장을 그대로 읽는다. 별도 migration 또는 downgrade를 수행하지 않는다.
`StoryStorage`, transaction, quarantine, write guard 구현은 변경하지 않았다.
기존 Actor identity, generation, UUID, event instance 규칙은 유지한다.
원본 세이브 또는 사용자 월드에는 rollback을 실행하지 않았다.

## 9. Failure history and known issues

1. 종료를 지식으로 취급함 → 미목격 도착 → completion만 존재 → 목격 사실과 알림을 분리함 → 새 GameTest·NBT 왕복 PASS → 실제 양의 목격 경로 미검증.
2. 생활 대화 소실 → Return Network 시작 → NOT_STARTED guard → 일반 대화 guard 제거·필수 사건 우선 유지 → 빌드와 기존 GameTest PASS → 실제 대화·동선 미검증.
3. Gradle daemon 연결 실패 → 격리 실행 → 내부 연결 환경 문제 → 별도 실행 권한으로 재실행 → 최종 114개 PASS → 코드 결함으로 분류하지 않음.
4. Git 원격 조회 실패 → `getaddrinfo() thread failed to start` → 격리 네트워크 환경 → 별도 실행 권한 사용 → 기준 remote SHA 확인 성공.
5. Computer Use 초기화 실패 → 초기 실행·reset 후 재시도 → kernel assets 경로 오류 → 실제 조작 단계 미진입 → 클라이언트 플레이 gate 미완료.
6. 최초 ZIP 대조 실패 → Git archive의 LF와 working CRLF 비교 → 줄바꿈 차이 → working bytes로 ZIP 생성 → 174개 파일 byte-match PASS.

V2-01 완료를 막는 항목은 최종 실제 클라이언트 주요 경로 검증이다.
시야 조건은 인지 기회의 근사치다. 플레이어 체감의 최종 판정을 대신하지 않는다.
오전 목적지의 실제 이동성과 일반 대화의 맥락도 새 월드 플레이에서 확인해야 한다.

## 10. GitHub and artifacts

원격 대상: `scarlel96-design/WHILE-YOU-WERE-AWAY`, branch `work/v2-playable-core`.
구현 commit: `0c1a97e823a0ef9974db6d9a07b2ee1eb04b3c8d`.
업로드 결과와 최종 remote SHA는 `evidence/v2-playable-core/v2-01b-transaction/PUBLICATION.txt`에 기록한다.
원래 작업 폴더/master의 기존 변경은 건드리지 않았다. 현재 checkout의 기존 untracked 자료도 유지했다.

새 산출물:

- `dist/v2-playable-core-v2-01b/whileaway-0.3.1-v2-01b.jar`
- `dist/v2-playable-core-v2-01b/whileaway-0.3.1-v2-01b-source.zip`
- `dist/v2-playable-core-v2-01b/DIFF.patch`
- `evidence/v2-playable-core/v2-01b-transaction/MODIFIED_FILE`
- `evidence/v2-playable-core/v2-01b-transaction/DIFF_FILE`
- `evidence/v2-playable-core/v2-01b-transaction/VERIFICATION.txt`
- `evidence/v2-playable-core/v2-01b-transaction/ROLLBACK.sh`

JAR SHA-256: `8cd59926e20cfa32ffe4861546fe36d7285ac8306b8b9a5ff3161127132cffd7`.
Source ZIP SHA-256: `758ff53b2a039ec4c13b88a40737ba3dd63d04662e36d8747361df15c6900f38`.
기존 `dist/v2-playable-core` JAR·ZIP은 보존했다.
기준 JAR: `b73f009f52564c7cb495603f51fe549716642a1d4e8a903b36b84933d7f194b4`.
기준 ZIP: `cc204f8877d2950b53e4da10c3784cf8b26ead4c47c7909642ea3a107ab089cf`.

## 11. Model and work balance

메인 세션이 production 수정과 검수를 수행했다. Terra/Luna 위임과 Caveman 사용은 없었다.
정확한 모델별 토큰 또는 작업 시간 자료는 수집하지 않았다. 절감률을 추정하지 않는다.
BUILD: 목격·대화·생활 동선·재방문 수정. QA: 기존 gate 1회 재실행과 패키지·복구 검증.
FIX/DEBUG: Gradle/Git 실행 환경 오류와 ZIP 줄바꿈 차이를 처리했다.

## 12. Next build work

첫 시작점은 Computer Use kernel assets 경로 오류 해소 후 DISPLAY1 격리 클라이언트 새 월드 플레이다.
Opening → 생존 → 여울 → Return Network → NoEvent → Returner → 회복 → 재방문을 진행한다.
정상 경로와 경고 무시 경로를 나누어 확인한다. 미목격·재접속·목격 중단도 필요한 범위만 확인한다.
클라이언트 주요 경로가 PASS된 뒤 V2-01 완료를 판단한다.
V2-02 첫 콘텐츠 후보는 여울 생활 다양성과 Residential/Maintenance 생활 기능이다. 아직 시작하지 않았다.
