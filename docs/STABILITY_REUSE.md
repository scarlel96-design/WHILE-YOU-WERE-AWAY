# 0.3.1 다음 안정화 작업 — 재사용성과 저장 완료 경계

공식 기준점은 `dist/whileaway-0.3.1-dev.1.jar` 그대로다. 별도 stability-work JAR는 작업본이며 0.3.2 후보 승인이나 전체 안정화 PASS를 의미하지 않는다. 저장 형식 4, 도시 revision 2, 기존 revision 1 보존 정책을 유지한다.

## 재현된 공통 결함

1. **Migration Failure**: 기존 형식에서 만드는 SceneRecord와 누락 UUID 복구가 randomUUID를 사용했다. 같은 입력을 변환해도 ID가 달랐다. 플레이어 UUID + 사건 종류 + 원점을 UTF-8로 직렬화한 고정 키로 이관/복구 ID를 생성한다. 새 게임에서 시작하는 실제 사건 ID와 현재 접속의 lease는 여전히 새 UUID를 쓴다. 월드 이름·절대경로·현재 시각을 migration 입력에 넣지 않는다. 플레이어 저장 순서도 정렬한다.
2. **Framework Failure**: ABORTED/4가 COMPLETED로, COMPLETED/2가 완료 경계 4로 승격됐다. 이제 명시적 중단을 유지하고, 불일치 상태는 영구 사실을 취소하지 않은 채 FAILED_RECOVERABLE의 안전 경계로 보낸다.
3. **Framework Failure — 비동기 저장 반환**: 사용 중인 NeoForge SavedData.save는 IO 작업을 큐에 넣고 dirty를 먼저 해제한다. 이는 체크포인트의 디스크 기록 완료가 아니다. 실제 조사 D 종료 후 세이브에는 3개 중 첫 기록만 남았다. NarrativeData의 파일 저장을 동기식 원자 NBT 교체로 재정의한다. 이 저장이 성공한 뒤에만 dirty를 해제하고 AFTER_COMPLETE를 발행한다. 실패하면 예외를 전달하고 dirty를 유지한다. 다른 모드/바닐라의 저장 방식은 바꾸지 않는다.

저장 보장은 해당 스토리 NBT 파일에 대한 것이다. 인벤토리·엔티티 청크·구조물 파일을 하나의 원자 트랜잭션으로 만든 것이 아니며, 전원 차단·고장난 저장장치 전체를 검증했다는 의미도 아니다.

## 기존 조사 사건 어댑터

새 퍼즐이나 자산을 추가하지 않고 기존 도시 기록물 3종의 실제 NoteItem.use 경로에 `CityInvestigation`을 연결한다.

- 사건 ID: city_records, 플레이어별 월드 SavedData에 귀속.
- 예약된 읽기: pendingEvidence. 검증된 읽기 요청을 먼저 저장한다.
- 확정 증거: evidence 및 기존 cityClues. 예약을 처리한 뒤 증거를 저장하고 예약을 제거한다.
- 안전 경계: 0 / 1개 확인 / 2개 확인 / 3개 확인(RESOLVING) / COMPLETED.
- 기록물의 6가지 순서와 중복 읽기를 허용한다. 마지막 자료만 특정 순서로 강요하지 않는다.
- 완료 안내 claim: noticeClaimed. 완료와 같이 저장하며 동일 안내를 재접속 때 다시 지급하지 않는다. 강제 종료가 저장과 실제 화면 표시 사이에 발생하면 짧은 안내가 생략될 수 있다. 원문 기록물과 발견 사실은 유지한다. 이를 보상 지급 트랜잭션이나 UI exactly-once 전달이라고 부르지 않는다.
- 형식 4의 선택 확장 필드다. 기존 세이브에서 이미 기록 3종을 확인했다면 완료 안내를 다시 재생하지 않는다.

## 관측 지점

RecoveryBoundaryEvent는 서버 스레드에서 PREPARED / FACT_COMMITTED / BEFORE_COMPLETE / AFTER_COMPLETE를 관측한다. 관측 자료는 진행 사실을 변경하는 명령이 아니다. 일반 설치에서는 종료를 일으키는 리스너가 활성화되지 않는다. `whileaway.boundary` 개발 속성이 있는 전용 테스트 JVM만 고의 종료한다.

신호 복구와 도시 진입은 **연출 레코드** D/E 회귀다. 신호 장치의 레드스톤 소비나 블록 변경 자체의 복구 증거로 확대 해석하지 않는다. 실제 조사 어댑터 시험은 NoteItem.use를 통해 다른 순서의 자료를 읽고 저장·재접속을 확인한다.

## 토큰을 구분한다

기존 SceneAck의 lease는 낡은 연결 패킷을 거절하는 휘발성 토큰이다. 사용자가 새로 요구한 사건 A 완료 → B 해금의 영구 인과 연결 토큰과 다르다. 후자는 source event instance, source committed fact/checkpoint, target event, AVAILABLE/BOUND/APPLIED 상태, 소비 주체를 별도 저장해야 한다. 이번 작업에서 기존 lease의 통과를 이 영구 연결 기능의 PASS로 올리지 않는다.

## 남는 순서

NPC·월드 변경·전투·복합 어댑터와 A/B/D/E 전체 → 영구 연결 토큰 → 중요 아이템/보상 트랜잭션 → 필수 구조물 대체 경로 → 자연 생존 처음부터 현재 끝까지 → 전체 회귀. 새 퍼즐 프레임워크, 최종 리소스팩, 모델, 건축, 음향 및 시각 폴리싱은 앞당기지 않는다. 자산 6그룹과 떡밥 초안 2건은 기존 추적 자료를 유지한다.

현재 판정과 실행 증거는 evidence/reuse/GATES.md와 VERIFICATION.txt를 따른다. 테스트 개수는 기능 완료 개수가 아니다.
