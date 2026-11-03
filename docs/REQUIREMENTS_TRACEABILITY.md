# WHILE YOU WERE AWAY — 요구사항 추적표 / Requirements Traceability Register

상태: **86개 발화 구간 전수 매핑 추가(2026-11-02)**. 과거 41,427줄 분량 대화에서 이미 정리·승인된 주요 설계 및 최신 사용자 지시를 일대일로 매핑하는 기본 틀이다. **86개 대화 구간 전수를 매핑했다. 단, 모든 문장을 원자 기능 요구로 승인했거나 게임 구현을 검증한 것은 아니다.** 향후 작업마다 추가·교차 점검한다.

이 문서는 [PROJECT_CONTINUITY.md](PROJECT_CONTINUITY.md), [DECISION_LOG.md](DECISION_LOG.md), [CONTENT_ROADMAP.md](CONTENT_ROADMAP.md), [DESIGN_V05.md](DESIGN_V05.md), [MYSTERY_RESOURCE_EXTENSION.md](MYSTERY_RESOURCE_EXTENSION.md), [foreshadow-registry.json](foreshadow-registry.json), `AGENTS.md`와 함께 읽는다.

## 사용 규칙과 정확한 상태 구분

- 각 항목의 **요구 결정 상태**는 `CONFIRMED`(사용자가 확정), `PROPOSED`(논의/제안), `SUPERSEDED`(나중 결정으로 대체), `UNRESOLVED`(확정되지 않음) 중 하나다.
- **구현/검증 상태**는 별도 관리한다: `DESIGN ONLY`, `PARTIAL`, `IMPLEMENTED UNVERIFIED`, `AUTOMATED VERIFIED`, `HUMAN VERIFIED`, `NOT TESTED`, `UNKNOWN`. 설계 CONFIRMED는 구현 PASS와 다르다.
- 표의 **적용 계획**은 미래 작업의 빠뜨리지 않기 위한 연결이며, 실제 파일 수정이나 코드 구현을 증명하지 않는다.
- 범위 내에 있는 항목은 구현 파일/테스트 로그/커밋 URL까지 추적한다. 범위 밖 항목은 **DEFERRED + 다음 대상**을 기록하며 조용히 삭제하지 않는다.
- 진행 중인 V2-03은 변경하지 않는다. 이 규칙은 **V2-03 이후에 새로 작성하는 작업지시서부터** 적용한다.
- 항목이 제안인지 확정인지 모호하면 `UNRESOLVED`를 사용한다. AI가 임의로 설정을 고정하지 않는다.
- 정확한 엔딩/Actor/Return Network 구현 상태는 최신 원격 소스·검증 증거로 재확인한다. 이전 작업 결과나 ChatGPT 기억을 증거로 대신하지 않는다.

## 요구사항 추적 매트릭스

| ID | 확정 요구·비협상 제약 | 요구 상태 | 설계·결정 출처 | 구현 증거·현재 검증 상태 | 다음 단계/누락 위험 |
|---|---|---|---|---|---|
| WWA-CORE-01 | 오버월드 생존·건축·채광·제작·농사·탐험을 기본 경험으로 유지 | CONFIRMED | PROJECT_CONTINUITY 2, DESIGN_V05 1–2 | 부분 구현; 자연 전체 플레이 NOT TESTED | 오버월드 구조물과 생활 루프 |
| WWA-CORE-02 | 저녁골은 중요하지만 월드 전체를 대체하는 고립된 어드벤처 맵이 아님 | CONFIRMED | FUNCTION_FIRST, DESIGN_V05 | 도시/오버월드 부분 연결 | 돌아갈 이유, 자유 이동 |
| WWA-CORE-03 | 작품은 Fear, Mystery, Character, Minecraft Life 네 축; Fear는 스토리 부속품이 아님 | CONFIRMED | DESIGN_V05 0–2, PROJECT_CONTINUITY 2 | 디자인 확정, 인간 공포 검증 NOT TESTED | Fear 콘텐츠 우선 |
| WWA-ARC-01 | 프롤로그+7장 및 장편 캠페인 | CONFIRMED | DESIGN_V05 | 본편 후속 챕터 설계·구현 부분 | 장별 콘텐츠 매핑 |
| WWA-ARC-02 | 일반 엔딩은 여러 시간, 거의 모든 비밀은 여러 날 탐구 규모 | CONFIRMED 목표 | DESIGN_V05, DECISION_LOG | 실측 없음 | 실시간 강제 대기 없음 |
| WWA-ARC-03 | BAD 2, NORMAL 2, HIDDEN HAPPY 1 및 엔딩 후 월드 변화 | CONFIRMED | DESIGN_V05, PROJECT_CONTINUITY 3 | 엔딩 설계, 실제 완결 NOT TESTED | 조건/후일담 개별 추적 |
| WWA-ARC-04 | 히든 엔딩은 선악 숫자·100% 수집이 아닌 조사/실험/관계/정착 준비 | CONFIRMED | DESIGN_V05, MYSTERY_RESOURCE_EXTENSION 53, 75 | 설계 | 주요 선택·불변식 |
| WWA-LORE-01 | 연속기는 Continuity Graph와 빈자리 관계를 보존하려 함 | CONFIRMED | DESIGN_V04, DESIGN_V05 | 설계 | 관계 모델과 엔딩 |
| WWA-LORE-02 | 마지막 정상 하루는 실재하지 않은 여러 날의 합성본 | CONFIRMED | DESIGN_V04, DESIGN_V05 | 설계 | 기록·환경의 사전 반증 |
| WWA-LORE-03 | 현재 인격의 변화할 권리; 완전한 원본 부활 불가 | CONFIRMED | DESIGN_V05 | 설계 | NPC와 결말에서 보존 |
| WWA-MYS-01 | 저녁골/연속기 A Story의 인과·비밀 층위 | CONFIRMED | DESIGN_V05 | 일부 실게임 단서 | 장별 복선·회수 |
| WWA-MYS-02 | Player Past: 1인칭 기억 파편, 기억상실, 오버월드 전환과 후반 재해석 | CONFIRMED | DESIGN_V05, DECISION_LOG | Opening 일부 구현; 종합 HUMAN NOT TESTED | 향후 구조물과 오프닝 복선 연결 |
| WWA-MYS-03 | Observer/Shadow Mystery는 별도 계층·만능 원인/전투 보스 금지 | CONFIRMED | DESIGN_V05, PROJECT_CONTINUITY 3 | 주로 설계 | 독립적 징후와 미회수 경계 |
| WWA-MYS-04 | A/B/C 등급 복선; 노출→반증→회수→재해석, 재방문 | CONFIRMED | MYSTERY_RESOURCE_EXTENSION 59–81 | foreshadow-registry 일부 PARTIAL | ID로 구조물·NPC·사운드 매핑 |
| WWA-MYS-05 | 외부 공략 없이 접근 가능한 증거 및 중복 단서·재확인 경로 | CONFIRMED | DESIGN_V05, MYSTERY_RESOURCE_EXTENSION 38–41 | 전체 조사 체계 미완성 | 독립 경로/소프트락 검사 |
| WWA-PUZZLE-01 | 공간·기록·시간·음향·레드스톤·Minecraft 시스템 기반 퍼즐 | CONFIRMED | MYSTERY_RESOURCE_EXTENSION 35–39 | DESIGN ONLY 또는 기초 단서 | 실제 퍼즐·복구 계약 |
| WWA-PUZZLE-02 | 전용 아이템·장비·조사수첩·장비 개량·추론 UI, 정답 자동 표시 금지 | CONFIRMED | MYSTERY_RESOURCE_EXTENSION 40–47 | 본격 프레임워크 미완료 | 아이템·Evidence 구조 |
| WWA-WORLD-01 | 작은 규모도 고품질 건축: 실루엣·동선·내부·생활 흔적·지형 결합 | CONFIRMED | PROJECT_CONTINUITY 4, DECISION_LOG | V2-03 폐역 템플릿 개선 기록; 인간 아트 QA NOT TESTED | 구조물 품질 검수 |
| WWA-WORLD-02 | 지상/채굴/철도/마을 등 여러 자연 발견 경로, 필수 체크리스트 금지 | CONFIRMED | PROJECT_CONTINUITY 4 | V2-03 폐역·관리소·통로 자연 생성 연결과 rail_site 필터. seed=203 후보 1/37 허용, 실제 배치·발견 NOT TESTED. [검토](V2_03_REVIEW_20261103.md) | 다수 시드/바이옴 실제 배치·발견성 확보 |
| WWA-WORLD-03 | 한 장소의 생활·재난·현재·재방문 네 의미 | CONFIRMED | DESIGN_V05 52 | 장소별 구현 미확인 | Three Lives Rule 정합성 |
| WWA-WORLD-04 | 소규모 공간 이상과 대형 월드 세트피스를 별도 종류로 유지 | CONFIRMED | PROJECT_CONTINUITY 4, DECISION_LOG | V2-03 관리소 원거리 가로보 소형 이상현상 구현. 대형 지속 상흔은 DESIGN ONLY; 시각 체감 NOT TESTED | 대형 세트피스 별도 구현 |
| WWA-WORLD-05 | 대형 사건은 사전 징후·운동·음향·위기·사건 후 안전한 흔적을 결합 | CONFIRMED 방향 | DESIGN_V05, PROJECT_CONTINUITY | DESIGN ONLY | Persistent World-Scar 패턴 |
| WWA-WORLD-06 | 사용자 월드/집/아이템 보존, 실제 지형 변형은 통제·복구 가능해야 함 | CONFIRMED | AGENTS, PROJECT_CONTINUITY | 관련 기존 검증 일부; 신규 효과 검증 필요 | 저장·청크·멀티플레이 |
| WWA-THREAT-01 | 귀로꾼은 귀환 경로를 따르는 존재; 방해·경고·위험·회복 인과 유지 | CONFIRMED | DESIGN_V04, DESIGN_V05 | 기초 기능 부분 구현; 최종 아트 미완료 | AI 계약 + 모델·소리 |
| WWA-THREAT-02 | 괴물마다 다른 실루엣·동작·음향·대응 규칙; 단순 몬스터 수 채우기 금지 | CONFIRMED | PROJECT_CONTINUITY 4, DESIGN_V05 | 귀로꾼 중심 PARTIAL | Threat 정체성·AI |
| WWA-THREAT-03 | 공포는 실제로 강해야 함; 생존 위협·추격·규칙 있는 점프스케어도 필요 | CONFIRMED | DESIGN_V05, DECISION_LOG | 인간 공포 체감 NOT TESTED | 대표 공포·위험·회복 QA |
| WWA-FEAR-01 | Fear Director의 휴식·전조·강도·발견 깊이·반복 피로 관리 | CONFIRMED | DESIGN_V05 | 구현 범위 최신 확인 필요 | 상태/쿨다운/중복 억제 |
| WWA-FEAR-02 | 모든 불안이 공격으로 이어지지 않으며 안전한 휴식처가 실제로 존재 | CONFIRMED | DESIGN_V05, PROJECT_CONTINUITY | 전체 플레이 NOT TESTED | 공포 템포·거점 보호 |
| WWA-AUDIO-01 | BGM·공간 음향·환경음·Presence/Threat·침묵·모티프 통합 | CONFIRMED | DESIGN_V05, PROJECT_CONTINUITY 4 | 프로토타입 자산, 청감 NOT TESTED | 동적 오디오 디렉션 |
| WWA-AUDIO-02 | 실제 경고 소리는 신뢰 가능, 청각 대체 단서/자막 제공 | CONFIRMED | DESIGN_V05, MYSTERY_RESOURCE_EXTENSION | 전반 검증 미완료 | 위험 접근성 |
| WWA-AUDIO-03 | 시나리오 인물의 말은 Minecraft 채팅 텍스트; 음성 녹음 필수 아님 | CONFIRMED | DECISION_LOG | 구체 연출별 확인 필요 | 대사/자막 표준 |
| WWA-ART-01 | 최종 리소스팩은 적법한 기존 자산 선택 또는 전용 제작; 전체 최종 폴리싱은 후반 | CONFIRMED | MYSTERY_RESOURCE_EXTENSION 33–34, PROJECT_CONTINUITY 6 | asset-contract 임시 자산 | 저작권·리소스 계약 |
| WWA-CINE-01 | 1인칭/조작 가능 기억·회상·장면 전환·NPC 블로킹·조명·사운드 브리지 | CONFIRMED | PROJECT_CONTINUITY 4, DESIGN_V05 | Opening 일부 구현, 최종 품질 미검증 | 인게임 시네마틱 |
| WWA-NPC-01 | 여울·도윤·미루는 플레이어 없이도 일상·선호·의견·동선을 가진다 | CONFIRMED | DESIGN_V05, PROJECT_CONTINUITY 3 | 여울 일부, 도윤·미루 확장 미완 | 인물별 행동/관계 |
| WWA-NPC-02 | 대화/선물 점수만이 아닌 공동 작업·건축·실제 현재의 선택으로 관계 형성 | CONFIRMED | DESIGN_V05 | 부분 | 생활과 엔딩 연결 |
| WWA-TECH-01 | Actor identity, schema, checkpoints, transactions, migrations, 구조물 버전·월드 안전 | CONFIRMED | AGENTS, 최신 evidence | 일부 자동 검증; 전체 완료 미선언 | 모든 신규 변경의 필수 회귀 게이트 |
| WWA-TECH-02 | 공포 연출로 OS·개인 파일·오디오 시스템 설정·게임 저장을 실제로 망가뜨리지 않음 | CONFIRMED | PROJECT_CONTINUITY 7 | 정책 | 코드 검토/패키지 검증 |
| WWA-DEV-01 | 메인 GPT-6.1 SOL/Codex 단독 구현 작업선, 별도 40% 보조 작업선 폐지 | CONFIRMED | DECISION_LOG | 정책 | 지시서 시작시 점검 |
| WWA-DEV-02 | 실제 장시간 자동 생존 탐험 반복 금지; 사용자가 50–60% 시점 대규모 플레이 담당 | CONFIRMED | PROJECT_CONTINUITY 6 | V2-01E 종료; % UNKNOWN | 새 WO 자동 QA 분리 |
| WWA-DEV-03 | 컴파일·타깃 GameTest·기본 저장·월드 보존 검증 지속 | CONFIRMED | WORK_ORDER_PROTOCOL | 새 WO마다 확인 | 자동 검증 증거 |
| WWA-DEV-04 | 현재 V2-03 미수정; 새로운 방향은 이후 작업지시서부터 적용 | CONFIRMED | DECISION_LOG | 적용 정책 | 차기 WO에서 적용 |
| WWA-DEV-05 | 요구/결정/콘텐츠/검증 증거는 GitHub에서 지속 추적 | CONFIRMED | PROJECT_CONTINUITY, WORK_ORDER_PROTOCOL | 문서화 진행 중 | 매 WO 갱신 의무 |

> 이 표의 구현 상태는 **보수적인 요약**이다. 진행 중 개발이 있을 수 있으며, 다음 작업 시작 전 현재 소스·증거를 실제로 검증해 최신 상태로 바꿔야 한다.

## 이전 세션 전수 대조로 추가한 항목

| ID | 요구 | 결정 상태 | 증거 | 구현 | 후속 |
|---|---|---|---|---|---|
| WWA-ARC-05 | 싱글플레이를 우선 완성하고 협동 플레이는 별도 확장·검증으로 둔다. | CONFIRMED 설계 방향 | T002 / DESIGN_V05 | HUMAN/멀티플레이 전체 검증 없음 | DEFERRED |
| WWA-MYS-06 | World·Character·Player Past·Shadow의 네 질문 축 보존. 세 묶음 요약으로 Character 삭제 금지. | CONFIRMED | T030,T051 / DESIGN_V05 13-A-1 | 본편 PARTIAL | 장별 서사 |
| WWA-FEAR-03 | 플레이 날짜만으로 위협 확대하지 않고 구조물·조사·설비·관계 등 의미 있는 접촉 깊이로 공포 활성화. | CONFIRMED | T018,T031–034 / DESIGN_V05 | 최신 런타임 확인 필요 | Fear Director |
| WWA-TEST-01 | 주요 심리 공포는 No-Spawn·No-Stinger·Free-Look에서 일부 성립. 별도로 강한 위기와 점프스케어도 허용. | CONFIRMED 품질 목표 | T020,T021,T033 / DESIGN_V04,V05 | HUMAN NOT TESTED | 공포 QA |
| WWA-CINE-02 | 1인칭 손상 기억 오프닝: 긴박함·흐릿함·자연 오버월드 전환·짧은 두통/혼란 채팅. 몬스터 돌파는 예시이지 확정 장면 아님. | CONFIRMED 원칙, 장면 세부 PROPOSED | T044–048 / DESIGN_V05 | Opening 일부 구현, HUMAN NOT TESTED | 시네마틱 |
| WWA-REFER-01 | 참고작의 공포·복선·연출 연구는 검증·비평하고 고유 자산/장면을 복제하지 않는다. | CONFIRMED | T006,T019–021,T031–035 | 비교 연구/구현 분리 | 연구 |
| WWA-OPS-01 | BASELINE/MODIFIED/ROLLBACK과 SHA·패키지·월드 복구를 구별한다. 소스 복원이 월드 롤백은 아님. | CONFIRMED 운영 계약 | T003–004,T008,T010,T012 / AGENTS | 이전 evidence 존재 | 이후 WO |
| WWA-OPS-03 | 자족적 상세 인수인계. 별도 채팅 원문을 재첨부하지 않아도 새 작업에 필요한 사실·증거·다음 시작점 제공. | CONFIRMED | T009,T022 | 보고 품질은 신규 WO별 검증 | 모든 WO |
| WWA-OPS-04 | 복사하기 쉬운 텍스트 블록/문서 형식으로 작업지시서를 제공한다. | CONFIRMED | T025 | 형식 규칙 | 이후 지시서 |
| WWA-OPS-05 | Caveman은 검증 후 HOLD. 유의미한 총사용량 이득/동등한 품질 증거 없이 재개·전면 적용 금지. | CONFIRMED 최신 보류 | T013–014,T023,T042 / CAMPAIGN_PILOT_031 | 과거 제한 비교 | 승인 전 HOLD |
| WWA-OPS-06 | 5시간·주간 사용량을 고려해 SAFE PAUSE와 정확한 재개 지점 유지. 임의 한도 소진·자동 PC 종료 금지. | CONFIRMED 운영 계약 | T040–043,T049–050 / AGENTS | 주기적 확인 | 이후 WO |
| WWA-OPS-07 | GitHub를 공식 작업·증거/문서 소스로 쓰고 원격/로컬 차이·정확한 SHA를 표시한다. | CONFIRMED | T056,T060,T073 / AGENTS | 현재 저장소 확인 가능 | 모든 WO |
| WWA-OPS-08 | GitHub Actions build/GameTest/Windows/Persistence 검증 상태를 PASS/CANCELLED/WAITING/NOT RUN 등으로 구별한다. | CONFIRMED | T067–075 / workflow,evidence | 현재 CI 결과 별도 확인 | 자동 검증 |

## 미해결·미감사 항목

| GAP-ID | 문제 | 처리 방법 |
|---|---|---|
| GAP-001 | **CLOSED (구간 단위):** 제공 원문 86개 대화 구간 전수 분류·추적 완료. | HISTORICAL_SESSION_AUDIT_20260909_0916.md 참조. 문장 원자화·코드 전수 검증은 별도. |
| GAP-002 | 장별 사건·몬스터 수·차원 수·세트피스 수량 중 초안 제안과 확정 범위가 혼재 | DESIGN_V05와 명시적 최신 사용자 결정을 비교하고 PROPOSED/CONFIRMED 표시 |
| GAP-003 | **CLOSED (보고/변경 상태 수준):** V2-03 최종 HEAD·지형 코드·GameTest 증거·미완료 인간 검증을 2026-11-03 확인 | [V2_03_REVIEW_20261103.md](V2_03_REVIEW_20261103.md). 전체 요구/코드 양방향 감사(GAP-008)는 OPEN |
| GAP-004 | 전체 콘텐츠 50–60%를 계산할 작업 분모/가중치 | 콘텐츠 단위·실제 경로·플레이 비중 확정 후 계산. 그전 UNKNOWN |
| GAP-005 | 퍼즐·아이템·NPC 관계·대형 공포 장면의 acceptance test 세부 기준과 담당 차기 WO | 각 WO 범위를 정할 때 ID별 구체 수용 조건과 실제 시험 증거 링크 연결 |
| GAP-006 | 사용자 아트·사운드·공포 감정 평가 | 첫 대규모 사용자 테스트까지 HUMAN NOT TESTED 유지 |

| GAP-007 | 전체 AI 설계 초안의 모든 문장과 기능의 세부 원자화 검증 미완료. | 제안/확정 구분과 장별 상세 수용기준 후속 분해 |
| GAP-008 | 모든 요구 ID와 최신 Java/JSON/자산·CI 증거의 양방향 1:1 연결 미완료. | 차기 작업별 실제 증거·커밋에 연결 |
| GAP-009 | 이 대화 파일 밖의 다른 세션과 첨부 자료 전수 감사 범위 외. | 별도 원본·SHA 확보 시 재감사 |

| GAP-010 | V2-03 rail_site 후보 37개 중 1개만 허용; 실제 자연 청크 배치·발견 밀도 UNKNOWN. 물/식생/높이맵 원인 미구분 | V2-04 제한적 멀티시드 월드젠 통합 검사, 안전한 발견 단서·구조물 개선 |

## 새 요구 추가 템플릿

| ID | 사용자 결정 한 문장 | CONFIRMED/PROPOSED/SUPERSEDED/UNRESOLVED | 날짜·출처 링크 | 디자인·코드·시험 위치 | 단계 / 검증 / 해당 WO |
|---|---|---|---|---|---|
| WWA-AREA-NNN | 구체 행동 또는 불변식 | 상태 | 결정 출처 | 경로·SHA | PLANNED/PARTIAL/검증 증거 |

## 작업 종료 시 누락 방지 체크

- 이번 작업의 **IN 항목 ID**를 모두 기록했는가? 구현 위치·테스트·증거가 있는가?
- **OUT/DEFERRED 항목 ID**가 다음 작업까지 살아 있는가? 사라진 항목은 없는가?
- 새 결정은 DECISION_LOG에 추가하고 이전 결정의 효력을 표기했는가?
- 새 구조물·기록·음향·괴물에 기존 장기 복선/스토리의 연결 ID가 있는가?
- 최신 코드에 존재하지 않는 기능을 자동/인간 VERIFIED로 올리지 않았는가?
- V2-03과 같이 **이미 진행 중인 작업의 범위를 새 문서로 소급 변경하지 않았는가?**
- 실측하지 않은 공포·청각·미학·완성률을 사실로 주장하지 않았는가?

일치하지 않는 항목이 있으면 누락·충돌을 명시하고, **완료 보고에서 열린 GAP-ID를 남긴다**. 문서를 조용히 삭제하지 않는다.
