# 귀환망의 첫 응답 — integration 작업 기록

## Current
- `0.3.1-dev.1 / return-network-integration / PARTIAL`.
- 작업 경로: `C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration`.
- Git branch: `work/return-network-integration`.
- 소스 기준: 원격 master `570c4f821c21aa98e4f2ab479dca50a688d7b6d5` (`Repair schema 5 persistence boundary`). 로컬 원본의 staged pycache 삭제 6건은 별도 작업이며 변경하지 않았다.
- 원래 npc-path 지시서 이후 원격에 반영된 return-network 계약 및 저장 트랜잭션을 이어받았다. 따라서 저장 형식은 이미 **5**, Actor schema는 optional **1**, 사건 내부 schema는 **1**, 도시 art revision은 **2**이다. 이번 작업이 형식 4에서 새로 승격한 것이 아니다.
- 마지막 패키지 기준 `dist/return-network-contract`: JAR `bf8e234052c8a33ea49cb02c3cabcd941515e29fc126280fdcfa62d4ba0db9c0`, source ZIP `a44e33064860f13256127a3928d17049c54289a4671796f594b95d8774fb4887`. 이 패키지는 schema 4 시점이므로 위 원격 소스 기준과 동일 시점이 아니다. `evidence/return-network-integration/baseline`에 원본을 별도 보존했다.
- 설계 v0.5와 Player Past Mystery는 변경하지 않았다. 기존 작업 지시서의 구현을 먼저 진행한다. 0.3.2/Item/Reward/Fallback/최종 아트/Caveman 확대는 하지 않는다.

## 실제 플레이 계약
사건 ID는 `return_network_first_response`이다. 여울의 `yeoul_return_light`가 checkpoint 4, COMPLETED이고 공동 경험 및 관계 사실이 존재해야 한다. boolean prerequisite만 만들어 전달해서 시작할 수 없다.

revision-2 폐전차의 기존 구리 설비 `(42,68,46)`에 접근하면 발견한다. 같은 UUID/instance/generation의 여울이 `(44,65,47)`로 걸어서 합류한다. 설비를 사용하여 패턴을 조사하고, 웅크려 사용하여 연결을 활성화한다. 조용한 응답과 접근성용 문장이 나타난 뒤 실제 여울 상호작용으로 공동 경험을 확정한다. 설비의 불과 증거, 후속 거주 행동은 남는다. 새 몬스터·강제 카메라·아이템 소비·보상 지급은 없다.

| Checkpoint | 확정 사실 / 경계 |
|---|---|
| 0 | 미시작; 실제 귀환등 완료가 선행 |
| 1 | DISCOVERED / A / 설비 위치와 사건 instance |
| 2 | NPC_JOINED / B / 기존 여울 participant binding |
| 3 | SIGNAL_OBSERVED / C / OLD_PATTERN_EVIDENCE |
| 4 | INTERVENTION_COMMITTED / D / 먼저 영구 개입 의도 저장 |
| 4 runtime | PRESENTING / 현장·목격자·NPC 조건을 만족할 때만 지연 |
| 5 | RESPONSE_OBSERVED / E / RESPONSE + PATTERN_MATCH_EVIDENCE |
| 5 | F / SHARED_EXPERIENCE + NPC shared receipt를 같은 파일에 저장 |
| 5→6 | G 직전/직후 / COMPLETE + AFTERMATH + NEXT_PREREQUISITE + NPC routine receipt |

실행 중 대기 상태와 navigation/presentation timer는 checkpoint를 후퇴시키지 않는다. 사망, 부재, 타 차원, 미로드, 환경 장애를 실패나 엔딩으로 바꾸지 않는다. 응답은 현장의 살아 있는 비관전자와 실제 참가 NPC가 준비되어야 진행한다.

## 코드와 저장 순서
- `ReturnNetworkEvents`: 실제 접근, 블록 사용 이벤트, 같은 NPC의 경로, 응답, NPC 상호작용, 완료 후 대사/시선, 환경 진단. 기존 도시 배치 데이터는 수정하지 않는다.
- `NpcEvents.update`: 완료된 귀환등 NPC가 복합 사건에 참여하는 동안만 이동 제어를 넘긴다. 완료 후 기존 생활 경로로 복귀한다.
- `StoryNpc.mobInteract`: 복합 사건의 상호작용을 우선 연결한다. 기존 NPC 생성이나 UUID/generation 변경은 추가하지 않는다.
- `ReturnNetworkIntegrity`: 실제 선행 사건, participant identity/dimension, event shared/aftermath와 NPC receipt의 양방향 일치를 검사한다.
- `NarrativeData.load`: 사건을 읽은 뒤 actor와 함께 의미 검증한다. 손상된 사건을 삭제하거나 빈 캠페인을 만들지 않는다.
- `NarrativeData.commitReturnNetwork`: expected-state 확인 → 직접 전이 확인 → 원본 hash 확인 → NPC facts 사본 준비 → 기존/후보 Integrity → 하나의 NBT envelope 동기 atomic write → 디스크 readback 전체 비교 → expected hash 갱신 → 실제 메모리 상태 공개. writer의 조용한 no-op과 unchecked exception도 write guard를 차단한다.
- F/G에서 NPC facts를 먼저 live record에 추가하지 않는다. 쓰기가 실패하면 NPC와 사건의 반쪽 성공을 공개하지 않는다.
- `NpcNavigation.step`: 허용 거리는 기존 48블록인데 기본 Minecraft API가 FOLLOW_RANGE=24로 검색하던 불일치를 수정했다. 실제 1.21.1 API `createPath(destination,0,48)`을 사용한다. 경로 budget, loaded corridor, OUT_OF_RANGE는 유지하며 `PathNavigationRegion.getChunkNow`를 사용하므로 미로드 청크를 강제 로드하지 않는다.
- `StoryEvents`의 개발용 integrity 검사에 사건과 실제 설비 진단을 추가했다.
- 한글/영문 언어 파일에 필요한 기능 문구만 추가했다. 나머지 아트 자산은 패키지 검증으로 동일성을 검사한다.

### 블록 저장과 story 저장 사이의 경계
Minecraft 블록 저장과 story 파일은 하나의 원자적 파일이 아니다. D는 **개입 의도**가 durable해진 직후이며 실제 블록 변경 이전이다. 이때 종료하거나 뒤에서 블록 저장이 유실되면 checkpoint를 되돌리지 않는다. 자동으로 플레이어 블록을 덮어쓰지도 않는다. 원래 구리 설비에서 플레이어가 다시 웅크려 사용하면 이미 확정된 작업만 재적용한다. 이 재적용은 NPC가 아직 멀리 있어도 가능하여 `설비가 필요해서 NPC가 못 오고, NPC가 없어서 설비를 못 고치는` 교착을 방지한다. 알 수 없는 블록/완전 소실/접근 장애는 WAITING_ENVIRONMENT 또는 Integrity 진단을 유지한다. 구조물 fallback은 별도 후속 단계이다.

## 증거와 테스트 분리
- 자동: 실제 Minecraft GameTest + Core; `tests-4.log`가 최종 소스 재실행 자동 회귀이다. 기존 원격 86개를 유지하고 9개를 추가하여 95개. Core 176,857 assertions.
- 실제 클라이언트: `tools/run-return-network-clients.ps1`, `ReturnNetworkSmoke`, `client-final-commands.jsonl`. 무음, 오른쪽 DISPLAY1, 서로 다른 PID 및 종료 확인. A–G는 world flush 없이 Runtime.halt를 실행한다. 고의 종료 시 Windows가 `-1073740791`을 반환할 수 있다. 숫자만 무시하지 않고 해당 경계 marker, 디스크 NBT, 다음 프로세스 재개까지 함께 확인한다.
- 개발 중 실행/실패는 `development-CutA`, `development-B`, 각 로그에 남긴다. 최종 실행 ledger와 합쳐 PASS 횟수를 부풀리지 않는다.
- client harness는 개발용 플레이어 이동/직접 사용 패킷 경로를 쓴다. 자연 생존이나 사람의 공포/사운드 체감 평가로 표현하지 않는다.
- `tools/verify-return-network-clients.py`가 독립 NBT 파서로 facts/identity/generation/복사 월드/손상 원본 hash를 대조한다. 검증이 끝난 항목만 최종 VERIFICATION 및 완료 보고서에서 승격한다.

## 실패 이력
1. 원격 baseline 86개 중 3개 실패 → schema 5 코드와 schema 4 기대값 불일치 → ActorGameTests/StoryGameTests의 출력 형식 기대값만 수정 → 기존 durability/facts 검사는 유지 → 이후 전체 회귀 통과.
2. 신규 클라이언트 harness 컴파일 실패 → StoryStorage.status에 server 전달 → 실제 signature는 DimensionDataStorage → overworld().getDataStorage()로 수정 → 컴파일/실제 시작 확인.
3. 최초 강제 종료 marker는 성공했지만 Gradle 실패 → Runtime.halt의 Windows native 종료 코드 → 고의 Cut 태스크에 한해서 종료 코드를 기록하고 marker/다음 프로세스/디스크를 필수화. 일반 종료 실패는 허용하지 않는다.
4. B에서 실제 NPC가 집에 정체 → WAITING_ENVIRONMENT, UNREACHABLE, attempts=3, followRange=24 진단 → 거리 계약 불일치 → NpcNavigation 공통 수정 및 30/49블록 회귀 추가. NPC 순간이동, generation 증가, 특정 테스트 좌표 예외 처리는 하지 않았다.
5. Environment 장애/복원 자체는 통과했으나 그 뒤 cp3에서 시간 초과 → 장애물/엔티티 접촉으로 검사 플레이어가 밀려 설비 거리 제곱 25.27(허용 25)을 초과 → ReturnNetworkSmoke의 검사 위치를 경로 옆으로 옮기고 장애물 복원 후 검사 플레이어만 다시 배치 → 생산 코드의 거리/경로/NPC는 변경하지 않음. 실패 PID31596의 로그와 ledger는 보존하며 새 격리 복사 월드 world-EnvironmentRetry / PID12424에서 완료 cp6·canonical 1·generation 0 및 디스크 저장을 재검증하여 통과했다.
6. 최초 Gradle 실행의 기본 cache 권한/오프라인 plugin 미존재는 환경 문제이다. 프로젝트의 기존 `work/gradle-home` cache로 정상 실행했다.

## 장기 복선 후보
`RETURN_NETWORK_CURRENT_PATTERN`
- 첫 노출: 귀환등 완료 이후 폐전차 설비의 같은 간격.
- 초기 해석: 오래된 설비가 여울의 신호를 우연히 반복한다.
- 향후 의미: 현재의 관계/행동이 continuity network에 등록되고 있다.
- 연결: 여울, 귀환등, 폐전차, OLD_PATTERN_EVIDENCE, PATTERN_MATCH_EVIDENCE.
- 회수/엔딩: 아직 구현하지 않음. 현재 인격/빈자리/합성된 과거의 정답을 이번 사건에서 설명하지 않는다.
- No-Spawn / No-Stinger / Free-Look / Persistence를 기능 구조로 지원한다. 주관적 공포 품질은 별도 플레이 평가 대상이다.

## 이번 검증 결과
- 최종 client ledger는 22회 시도 중 21회 통과, Environment 1회 실패 후 수정·재실행 통과이다. 개발 초기 실행들은 이 수에 더하지 않았다.
- 8개 실제 강제 종료(A/B/C/D/E/F/G직전/G직후), 완료 새 프로세스 재접속, 독립 복사본 2회, 손상 차단 2회, 정상 완주, 환경 장애/복원, Nether, End, 실제 청크 unload/reload, cp2 KeepInventory OFF/cp4 ON/cp5 OFF 사망 복구를 구분했다.
- 모든 유효 세계에서 UUID b576b8cc-0a7d-45ea-8216-52f25bc0fc8a / instance 2ee78da8-6cfd-490b-8a2f-f037a46b5add / generation 0을 유지했다. 원본 cp6과 복사본 cp3은 서로 독립적이다.
- 손상 2종은 각 3회 실제 saveEverything 호출 뒤에도 원본/격리본 bytes를 유지했다. 잘못된 participant나 shared receipt를 추정하여 채우지 않았다.
- 독립 디스크 검증: `python tools/verify-return-network-clients.py`, exit 0. 21개 새 PID/실제 종료, 저장 NBT, actor identity, checkpoint 단조성, 원본 보존을 검사했다.
- 현재 response는 기능용 단일 저음량 bell과 패턴 설명 문장이다. 실제 짧음-짧음-김 리듬/분산 조명과 일치하는 관찰 연출은 아직 충분하지 않다. 이것은 최종 아트가 아니라 후속 기능 가독성 게이트로 남긴다.
- **첫 복합 사건 PARTIAL / 전체 캠페인 PARTIAL**. 제한된 게이트의 PASS를 전체 봉인으로 승격하지 않는다.

## 후속 게이트
사건 전체가 봉인되기 전 Item Transaction으로 넘어가지 않는다. 추가로 개별 NPC/설비 청크 경계, 다양한 설비 손상/접근 경로, 이전 사건의 위험 기반 실제 클라이언트 회귀, 장면 가독성을 확인한다. 자연 생존 전체 진행, Item/Reward/Fallback 및 전체 통합은 아직 해당 단계가 아니다.

## GitHub
업로드 대상: `https://github.com/scarlel96-design/WHILE-YOU-WERE-AWAY`, branch `work/return-network-integration`.
소스, 이 문서, 실행·실패 증거, 검증 도구, candidate JAR/source ZIP, DIFF/VERIFICATION/ROLLBACK을 포함한다. 원격 반영 여부와 실제 커밋은 최종 업로드 확인 후 `UPLOAD_RECEIPT.md` 및 최종 응답에 기록한다. 이 문서의 목적은 완료된 기능 범위를 과장하지 않고 현재 작업을 인수인계하는 것이다.
