# 0.3.1 lifecycle-work quality gates — overall PARTIAL

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
