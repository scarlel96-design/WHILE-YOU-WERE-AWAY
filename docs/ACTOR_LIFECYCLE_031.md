# 0.3.1 lifecycle-work — 추격 사건 복구 일반화 작업본

## 기준과 판정
- 채택한 기준: 0.3.1 stability-work, JAR SHA256 `9062d03914ff6dc8211e77342d785734c6263bd3af5bfb2fa5ce344a50c580d6`.
- 작업 버전: 0.3.1-dev.1. 0.3.2로 승격하지 않는다. 전체 안정화는 PARTIAL.
- 저장 형식4, 도시 아트 revision2, 기존 6개 임시 자산 그룹과 2개 떡밥 초안을 유지한다.
- 이번 구현은 기존 Wayfarer 추격에 공통 actor registry를 연결한다. 완성형 NPC 사건/복합 사건/아이템·보상 트랜잭션/구조물 fallback을 구현했다고 해석하지 않는다.

## 저장 계약
NarrativeData의 Overworld `whileaway_story.dat` 안에 선택적 `actorSchema=1`, `actors` 목록을 추가한다. 별도 전역 파일이나 원본 월드 절대경로를 참조하지 않는다. 이전 형식1/2/3 및 actors가 없는 기존 형식4는 빈 레지스트리로 결정적으로 로드된다. 이전 scene ID 및 도시 데이터는 바꾸지 않는다.

각 레코드는 storyId, ownerEventId, spawnRole, entityType, dimension, instanceId, owner, entityUUID, generation, position, temporary, lifecycleState, state, checkpoint, interruptions, reason, snapshot, facts, noticeClaimed를 저장한다. facts는 정렬된 집합이다. snapshot은 재구축용 엔티티 NBT이며 UUID/Passengers/Leash를 복제하지 않는다. 중요 아이템 인벤토리 원자성을 제공하는 구조가 아니다.

생명주기 PREPARED / ACTIVE / SUSPENDED / RETIRED와 사건 체크포인트는 분리한다. 체크포인트0=예약,1=등장 확정,2=추격 시작,3=완료 사실·퇴역 예약,4=완료 확정이다. 죽음/다른 차원/오프라인 동안 Wayfarer의 AI 활성 시간은 진행하지 않는다. 이 조건을 일반 NPC의 모든 행동에 자동으로 적용했다고 주장하지 않는다.

## 실제 변경 위치
- StoryActorRecord: 저장/읽기, 정렬된 영구 사실, actor 고유성 필드. 필수 식별자 누락/중복 logical ID/미지원 actorSchema/잘못된 lifecycle는 진단 오류로 남긴다.
- NarrativeData: actors를 기존 동기적 원자 교체 저장에 포함한다. 파일 저장 실패 때 dirty를 유지하는 기준본 동작을 보존한다.
- StoryActors.prepare/beginChase: 사건 티켓과 actor 예약을 먼저 저장한다. 그 뒤에 엔티티를 생성한다.
- StoryActors.reconcile/storageReady/find: 로드된 차원들의 UUID 조회와 해당 청크의 entity storage 로드 여부를 확인한다. 두 번의 로드 확인 전에는 누락으로 판정하지 않는다. 월드 전체 엔티티 검색이나 청크 강제 로딩을 하지 않는다.
- StoryActors.joining/canonical: storyId + event ID + instance ID + role + UUID + generation + entity type을 확인한다. 이미 퇴역했거나 이전 세대인 엔티티의 로드를 차단한다. 알 수 없는 소유 기록을 임의로 삭제하지 않는다. join 콜백에서 청크 로딩이나 생성/저장을 하지 않는다.
- 재생성: 최신 UUID를 저장하고 generation을 증가시킨 뒤 생성한다. 예전 청크의 엔티티가 뒤늦게 나타나도 새 canonical actor와 공존하지 않도록 fencing한다. 안전한 체크포인트 인근9개 위치만 검사하며 블록을 바꾸지 않는다. 막힌 위치는 기다린다.
- Wayfarer.tick: 기존 적의 실제 AI를 연결했다. 엔티티 청크 저장이 뒤처져도 저널의 추격 시작 사실로 AI를 복원한다. 현재 엔티티의 체력/인벤토리/좌표를 무조건 덮어쓰지 않는다. 실제 추격이 시작될 때 phase2를 저장한다.
- StoryActors.completeChase/confirmChase: encounterComplete, encounter 티켓 제거, RETIRED, 완료 사실을 같은 저장으로 확정한 뒤 phase4를 확정한다. 완료 통지 receipt는 at-most-once이며 소리/통지의 정확히 한 번 전달이나 아이템 지급 트랜잭션 증거가 아니다.
- StoryEvents.manifest: spawn-then-ticket 순서를 reserve-then-spawn으로 교체한다. 관리되는 actor의 운영자 recover는 티켓을 무작정 지우지 않고 상태를 재조정한다.
- debug integrity: 기존 scene 검사에 actor 티켓/UUID/차원/완료 사실/잔존 엔티티 검사를 추가한다. SAFE_AUTO_RECOVER, RECOVER_WITH_WARNING, MANUAL_DIAGNOSTIC을 구분한다. 개발 옵션 및 권한 제한을 유지한다.
- ActorGameTests: 실제 Minecraft 서버의 Villager를 사용한 재사용 가능한 lifecycle 검사다. NPC 대화/관계/공동 행동이 있는 실제 캠페인 사건의 완료 증거가 아니다.
- ActorSmoke/build.gradle: 무음 격리 클라이언트 A/B/C/D/E/Verify. 테스트용 이동/평지 생성/사건 시작 호출을 사용하므로 자연 생존 테스트가 아니다.

## 이 단계의 경계와 다음 작업
1. NPC 캠페인 사건 및 4요소 이상 복합 사건은 아직 별도 연결/검증이 필요하다. Villager fixture로 이를 PASS 처리하지 않는다.
2. 신규 추격 adapter의 사망(keepInventory true/false), 실제 청크 이탈/귀환, Nether/End 왕복은 별도 클라이언트 회귀가 필요하다. 이전 seven-figures 결과를 자동으로 재사용하지 않는다.
3. 레지스트리가 없는 이전 버전의 encounter UUID는 실제 엔티티가 로드되어 일치할 때만 결정적으로 채택한다. 엔티티마저 누락된 옛 티켓의 자동 복구는 미완성이다.
4. actor integrity는 전체 Story Integrity의 일부이다. 중요 아이템/보상/구조물/상호배타 선택/causal token 전체 검사는 아직 없다. 손상된 ResourceLocation 등 모든 actor 필드의 스트레스 검증도 남아 있다.
5. 중요 아이템의 실제 소유권 이동 및 중복 보상 방지는 구현하지 않았다. NPC snapshot을 이 용도로 사용하지 않는다.
6. 필수 구조물 대체 경로와 자연 생존 처음부터 현재 끝까지 진행은 미검증이다. 현재 actor의 9점 안전 위치 탐색은 구조물 fallback이 아니다.
7. scene 연결 토큰은 일시적 client acknowledgement lease이다. 영구 사건 A→B 인과 토큰은 별도 구현 대상이다.
8. 다른 엔티티를 통과시키기 위해 플레이어 건축물을 지우거나 월드를 다시 만들도록 처리하지 않는다.
9. JAR rollback만 검증한다. 추가 actor 데이터를 포함한 세이브를 옛 JAR로 읽으면 모르는 필드가 보존된다는 보장이 없다. 세이브 downgrade는 지원/검증하지 않는다.

## 재현 순서
작업 루트에서 Java21과 기존 work/gradle-home을 사용한다.
1. `./gradlew.bat --offline build runGameTestServer`
2. `./tools/run-actor-boundaries.ps1` — 같은 소스로 두 월드, 각 A B C D E Verify 순서.
3. `python tools/verify-actor-evidence.py`
4. `python tools/package-lifecycle.py --prepare`
5. `./tools/verify-lifecycle-transaction.ps1` — Git Bash, JAR 복사본만 복원.
6. evidence/lifecycle/VERIFICATION.txt와 GATES.md를 현재 실측 결과로 갱신한 후 `python tools/package-lifecycle.py`.
이전 package-reuse.py는 보존 중인 baseline을 대상으로 하므로 실행하지 않는다.
