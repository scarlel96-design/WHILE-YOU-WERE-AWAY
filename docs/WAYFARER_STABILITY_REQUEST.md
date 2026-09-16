# 《네가 없는 동안》 다음 작업 지시서

## Current

**《네가 없는 동안》 0.3.1-dev.1 / campaign-pilot / PARTIAL**

이번 작업은 반드시 현재 `campaign-pilot` 수정본을 최신 기준점으로 삼아 이어간다.

### 최신 기준 산출물

- 기준 JAR: `whileaway-0.3.1-campaign-pilot.jar`
- 기준 소스: `whileaway-0.3.1-campaign-pilot-source.zip`
- 저장 형식: **4**
- Actor Schema: **optional actorSchema=1**
- 도시 아트: **revision 2**
- 전체 안정화: **PARTIAL**
- 0.3.2 승격: **보류**

### 최신 SHA-256

수정 JAR:
`9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8`

수정 소스:
`81b47fcca580ac17102e0135c963bc1b2fdce638544ee5b82571b4b65fdc55ec`

작업 시작 시 이 두 파일과 해시를 보존하고 새로운 수정본과 섞지 않는다.


# 1. 상위 설계 문서 적용

이번 작업부터 함께 첨부되는 최신 《네가 없는 동안》 통합 설계안을 **상위 설계 기준**으로 취급한다.

해당 설계에는 기존 요구사항뿐 아니라 최근 확정한 다음 방향이 포함되어 있다.

- 관계·역할·빈자리를 중심으로 한 연속기 세계관
- 합성된 ‘마지막 하루’라는 중반 핵심 반전
- DORMANT → CONTACT → AWAKENED 계열 공포 활성화 구조
- No-Spawn / No-Stinger / Free-Look 공포 품질 기준
- Horror Director
- Foreshadow Matrix
- Signature Setpiece
- Three Lives Location
- NPC 독립 생활 및 NPC↔NPC 관계
- 환경·아이템·퍼즐·미스터리·멀티엔딩 통합
- Broken Script, code44.jar, outln.frombelowland 등에서 참고한 현대적 공포 연출 원칙

그러나 **이번 작업에서 이 설계를 이유로 대규모 신규 콘텐츠나 최종 아트 구현을 시작하지 않는다.**

현재 단계는 여전히 기능 안정화다.

상위 설계가 필요로 하는 저장 훅·상태 확장성·이벤트 구조를 훼손하지 않는지만 고려한다.


# 2. 이번 작업의 1순위 — Wayfarer 예외 안정화 묶음 완전히 종료

현재 Wayfarer 예외 검증은 실제 클라이언트 1회 성공했지만 전체 판정은 PARTIAL이다.

이번 작업에서는 이것을 먼저 **PASS 또는 명확한 FAIL**로 닫는다.

새 기능으로 넘어가기 전에 다음을 완료한다.


## 2-1. 실제 Actor Snapshot 청크 검증

현재 검사는 HOME 청크 중심이다.

이를 보강하여 실제 canonical actor의 저장된 `snapshot.position`이 속한 청크도 직접 확인한다.

다음을 구분해야 한다.

1. HOME 청크 로드 상태
2. actor snapshot 청크 로드 상태
3. 실제 actor 엔티티가 존재하는 청크 상태
4. 플레이어가 존재하는 청크 상태

HOME이 언로드됐다는 이유만으로 actor도 언로드됐다고 간주하지 않는다.

반대도 마찬가지다.


## 2-2. 청크 이탈 실제 검증을 최소 2회 반복

서로 다른 새 월드 또는 독립 시드에서 동일 흐름을 최소 2회 수행한다.

**추격 시작  
→ actor 위치 확인  
→ 실제 해당 청크 언로드  
→ 충분히 떨어져 유지  
→ 재접근  
→ 동일 actor 재연결**

검증 항목:

- UUID 유지 여부
- instanceId 유지
- generation 유지
- checkpoint 유지
- 중복 actor 없음
- AI 시간 예산이 부재 중 증가하지 않음
- 아직 로드되지 않은 actor를 missing으로 오판하지 않음


# 3. 기존 ActorSmoke D 경계 갱신

현재 D 검사는 과거 정책인

`42블록 이탈 → 사건 완료`

를 완료 수단으로 사용한다.

새 정책에서는 반경 이탈이 완료가 아니라 SUSPENDED이므로 기존 검사 의도와 구현이 충돌한다.

테스트를 삭제하거나 느슨하게 만들지 않는다.

대신 D 경계의 의미를 유지하면서 **정당한 완료 경로**로 변경한다.

예:

- 전투 종료 조건
- 설계된 active-time 완료
- 실제 사건 목표 충족

중 현재 Wayfarer 설계에 가장 자연스러운 방법을 사용한다.

### D가 증명해야 하는 것

**사건의 실제 완료 조건은 충족되었지만 완료 commit은 아직 확정되기 직전인 상태**

에서 종료했을 때:

- 완료 사실 유실 없음
- actor 중복 없음
- RETIRED/ACTIVE 모순 없음
- 다음 사건 조기 실행 없음
- 완료 보상 중복 없음
- 재접속 후 사건 처음부터 재실행 없음

을 보장해야 한다.


# 4. 기존 A/B/C/D/E 전체 실제 클라이언트 회귀

D 검사 수정 후 기존 Wayfarer A/B/C/D/E를 처음부터 다시 수행한다.

과거 결과를 그대로 PASS로 재사용하지 않는다.

새 `ownerPresent / ActorPresencePolicy / SUSPENDED` 정책을 포함한 현재 코드에서 다시 검증한다.

최소:

- A × 2
- B × 2
- C × 2
- D × 2
- E × 2
- 완료 후 Verify × 2

를 권장한다.

여건상 횟수를 줄여야 한다면 이유를 보고서에 명확히 남기고 PARTIAL 판정을 유지한다.


# 5. 새 프로세스 재접속 검증

동일 JVM 또는 동일 실행 세션 내부의 상태 재구축만으로 PASS를 선언하지 않는다.

실제 클라이언트를 종료한다.

프로세스가 완전히 끝난 것을 확인한다.

새 프로세스로 같은 월드를 연다.

다음을 확인한다.

- story NBT 정상 로드
- actor registry 정상 로드
- canonical actor 결정
- generation 유지
- checkpoint 유지
- lifecycleState 정상
- UUID/실제 엔티티 일치
- SUSPENDED 사건 재개
- 완료 사건 재실행 없음

이 검증은 최소 2개의 서로 다른 경계에서 수행한다.


# 6. 실제 복사 월드 독립성 검증

현재 데이터 객체 수준의 독립성 결과를 실제 세이브 월드 검증으로 확장한다.

### 절차

원본 테스트 월드 A를 만든다.

A를 정상 종료한다.

세이브 디렉터리를 B로 복사한다.

그 후:

### A

Wayfarer 사건을 완료한다.

### B

Wayfarer를 SUSPENDED 상태 또는 다른 checkpoint로 유지한다.

다시 각각 재실행한다.

다음을 확인한다.

- A의 완료가 B에 반영되지 않는다.
- B의 checkpoint가 A를 변경하지 않는다.
- generation 독립
- UUID 처리 독립
- instance 상태 독립
- actor registry 독립
- 월드별 story NBT 독립

실제 월드 검사가 통과해야 `신규 actor 월드 복사` 항목을 PASS로 변경한다.


# 7. KeepInventory·사망 검증 반복

기존 실제 1회 결과를 반복한다.

최소:

- KeepInventory OFF 사망/리스폰
- KeepInventory ON 사망/리스폰

각각 다시 확인한다.

사망이 다음 의미로 해석되지 않아야 한다.

- 추격 완료
- 추격 실패
- actor missing
- generation 교체
- 엔딩/스토리 실패

사망은 필요하다면 사건의 일시중단 원인이지 자동적인 서사 판정이 아니다.


# 8. Nether / End 왕복 반복 검증

Nether와 End 왕복을 각각 추가 검증한다.

왕복 중:

- actor가 다른 차원에 생성되지 않음
- 원래 dimension이 유지됨
- 사건 시간이 부재 중 임의 진행되지 않음
- generation 증가 없음
- 귀환 후 canonical actor가 하나만 존재
- checkpoint가 유지됨

을 확인한다.


# 9. Wayfarer 안정화 PASS 기준

다음이 모두 통과해야 해당 묶음을 PASS로 변경한다.

| 항목 | 필요 |
|---|---|
| A/B/C/D/E 최신 정책 회귀 | PASS |
| 실제 actor 청크 이탈/귀환 | PASS |
| 사망/리스폰 OFF | PASS |
| 사망/리스폰 ON | PASS |
| Nether | PASS |
| End | PASS |
| 새 프로세스 재접속 | PASS |
| 실제 복사 월드 | PASS |
| 중복 actor 검사 | PASS |
| generation 안정성 | PASS |

하나라도 실제 검증이 남으면 PARTIAL을 유지한다.


# 10. Actor 손상 복구 단계 시작

Wayfarer 예외 묶음이 닫힌 다음 Actor 손상 복구로 넘어간다.

정상 데이터만 다루지 않는다.

격리된 복사 월드에서 actor NBT를 의도적으로 손상시킨다.

최소 대상:

- UUID 누락
- 잘못된 UUID
- generation 누락
- 음수 generation
- lifecycleState 불명 값
- checkpoint 범위 오류
- instanceId 누락
- ownerEventId 불일치
- dimension 불일치
- position/snapshot 일부 누락
- 같은 storyId 복수 레코드
- actorSchema 불명 값


# 11. 손상 복구는 세 등급으로 분류

### SAFE_AUTO_RECOVER

정답이 사실상 하나뿐이고 플레이어 진행을 훼손하지 않는 경우.

### RECOVER_WITH_WARNING

합리적으로 복구 가능하지만 데이터 이상 사실을 반드시 기록해야 하는 경우.

### MANUAL_DIAGNOSTIC

자동 결정하면 정상 진행을 덮어쓸 위험이 있는 경우.

**모든 손상을 무조건 알아서 고치는 것이 좋은 안정성이 아니다.**

잘못된 자동 복구보다 명확한 진단이 안전할 수 있다.


# 12. 완전히 사라진 Actor Ticket 복구

다음 상태를 강제로 구성한다.

- 사건 저널 ACTIVE
- actor record 존재
- 기존 entityUUID의 엔티티 없음
- 해당 generation actor 없음
- 관련 청크가 실제 로드됐음을 확인
- 이전 generation도 canonical 후보가 아님

이때에만 실제 missing으로 판정한다.

복구:

**missing 확정  
→ 다음 generation 예약 저장  
→ 이전 generation 폐기 사실 저장  
→ 새 actor 생성  
→ 실제 UUID 확인  
→ 생성 확정 저장  
→ checkpoint 재연결**

이 순서 중 각 경계에서 종료해도 canonical generation이 두 개 생기면 안 된다.


# 13. Actor 손상 복구의 가장 중요한 금지사항

다음 편법은 사용하지 않는다.

- actor 없음 → 즉시 새로 생성
- UUID 오류 → 기존 정보 전체 폐기
- dimension 이상 → 현재 플레이어 차원으로 강제 이동
- 위치 막힘 → 주변 블록 삭제
- actor 두 개 → 무조건 먼저 찾은 하나 사용

항상 영구 사실과 사건 저널을 우선한다.


# 14. Story Integrity 확대

Actor 손상 검증과 함께 Integrity를 확장한다.

최소 진단 항목:

- duplicate canonical actor
- stale generation
- impossible lifecycle/checkpoint
- invalid dimension
- orphaned event actor
- ACTIVE event + RETIRED actor
- COMPLETED event + ACTIVE actor
- actor record + entity absence
- entity + actor record absence
- instance mismatch

각 오류에는 가능하면:

- diagnostic code
- storyId
- eventId
- instanceId
- generation
- 현재 상태
- 기대 상태
- 자동복구 여부

를 기록한다.


# 15. 그 다음 실제 NPC 사건으로 이동

Actor 손상 복구 핵심이 통과하면 **실제 캠페인 NPC 사건**을 연결한다.

Villager mock으로 최종 PASS를 대신하지 않는다.

아트는 임시 자산을 사용해도 된다.

NPC 사건은 최소 다음을 포함한다.

**NPC 등장  
→ 위치 이동  
→ 플레이어 또는 다른 NPC와 상호작용  
→ 영구 사실 변경  
→ 사건 진행  
→ 사건 종료  
→ 사건 이후 일상 상태**

NPC는 사건이 끝난 후 그냥 사라지지 않고 새로운 생활 상태를 가져야 한다.


# 16. NPC의 영구 사실과 Runtime을 분리

예:

### Persistent Facts

- player와 만났음
- 사건을 목격함
- 특정 사람을 구조함
- 새로운 거주지 선택
- 특정 관계 사건 경험

### Runtime

- 현재 path
- 현재 look target
- 임시 대화 단계
- animation
- 사건 중 임시 목적지

재접속 시 Runtime을 정확히 프레임 단위 복원하려 하지 않는다.

영구 사실을 기준으로 안전하게 재구축한다.


# 17. NPC↔NPC 확장 가능성 유지

현재 구현부터 데이터 모델을 `NPC → Player` 관계 하나에 고정하지 않는다.

향후:

- NPC A → NPC B
- NPC B → NPC A
- 공동 작업
- 갈등
- 약속
- 생활 장소
- 독립 목표

를 저장할 수 있도록 확장성을 유지한다.

단, 이번 작업에서 완성형 NPC 사회 시뮬레이션까지 구현하지 않는다.


# 18. NPC 사건 A/B/C/D/E

NPC 사건도 동일 경계 검증을 통과해야 한다.

특히:

- NPC 중복
- NPC 증발
- 두 위치 동시 존재
- 대화 중복 확정
- relationship event 중복
- 거주지 롤백
- 다음 사건 token 중복

을 집중 검사한다.


# 19. 실제 복합 사건 준비

NPC 사건 다음에는 최소 4요소 이상을 포함하는 복합 사건으로 진행한다.

예:

**NPC + Actor + Structure + Evidence + Item**

또는

**NPC + Structure + Item + Reward + Sound/Event**

새 장편 콘텐츠를 만들 필요는 없다.

현재 캠페인에 자연스럽게 들어갈 작은 실제 사건으로 검증한다.


# 20. 중요 Item/Reward Transaction은 복합 사건 직전 또는 함께 구현

중요 아이템은 단순 인벤토리 존재 여부로 판정하지 않는다.

추천 원칙:

`VALIDATE → RESERVE → PERSIST → APPLY → COMMIT → CONFIRM`

보상 역시 고유 transaction ID를 가진다.

반드시 검증할 것:

- 적용 직전 종료
- 적용 직후 종료
- commit 직전 종료
- commit 직후 종료
- 인벤토리 가득 참
- 사망
- 드롭
- 상자 이동
- 재접속

Double Ownership과 Lost Ownership 모두 FAIL이다.


# 21. 필수 구조물 fallback은 그 다음

NPC/복합 사건과 transaction 안정화 이후 실제 필수 구조물 fallback 최소 1종을 구현한다.

이 작업 역시 crash-safe하게:

**위치 예약  
→ 저장  
→ 생성  
→ 검증  
→ registry commit  
→ story link commit**

구조를 사용한다.

플레이어 생활권과 기존 건축물을 덮어쓰지 않는다.


# 22. 최신 공포 설계는 지금 ‘구현’이 아니라 ‘호환성’만 보장

이번 상위 설계에는 다음 신규 시스템이 향후 들어간다.

- HorrorActivationState
- Horror Director
- Foreshadow Registry
- Investigation/Evidence
- Puzzle State
- Signature Setpiece Director
- 관계/빈자리 기반 세계관
- 멀티엔딩 knowledge/world-state 판정

현재 저장·Actor/NPC/Transaction 구조를 설계할 때 **이 시스템들이 나중에 들어오지 못하게 막는 결합**을 만들지 않는다.

그러나 지금 바로 구현하지 않는다.


# 23. 공포 Activation의 기반 필드만 필요하면 고려 가능

현재 구조를 건드리지 않고 optional하게 넣을 필요가 실제로 생긴다면 다음 개념을 수용할 수 있도록 한다.

- activation state
- activation cause
- world awareness
- trigger evidence
- activation gameTime

하지만 지금 테스트에 필요하지 않으면 굳이 새 저장 필드를 추가하지 않는다.

기능 안정화가 우선이다.


# 24. 최종 아트 계속 금지

현재 도시 화면은 기능 검증용이다.

이번 단계에서는:

- 도시 재건축
- 건물 디테일 강화
- 고급 인테리어
- 최종 리소스팩
- 최종 NPC 모델
- 최종 BGM
- 고급 세트피스

작업을 시작하지 않는다.

최종 기능 봉인 이후 별도 대규모 아트 폴리싱 단계에서 수행한다.

그 단계에서는 기존에 확정한 대로 인터넷에서 높은 평가를 받는 Minecraft 건축물에 준하는 수준을 품질 기준으로 적용한다.


# 25. Caveman 판단 — 현재 HOLD

이번 실측 결과:

- 출력 토큰: **13.99% 감소**
- reasoning 출력: **48.62% 감소**
- 작업 시간: 약 **6.07% 감소**
- 전체 토큰: **30.92% 증가**
- 제한 코드 품질 차이: 관측되지 않음

따라서 현재 목적에서 Caveman 전면 확대 근거가 없다.

**이번 다음 작업에서는 Caveman을 전체 작업에 적용하지 않는다.**

특히 다음 고위험 작업에서는 사용하지 않는다.

- Actor reconciliation
- 데이터 손상 복구
- NPC lifecycle
- item/reward transaction
- migration
- 구조물 fallback
- Story Integrity
- 복합 사건


# 26. Caveman 재시험은 지금 하지 않아도 됨

이번 안정화 작업의 핵심 경로를 Caveman 실험 때문에 다시 나누지 않는다.

다음 시험을 하더라도 별도 저위험 작업 경계에서 한다.

재시험 시 이전 실험의 혼선을 줄이기 위해 가능하면:

- 독립 작업 컨텍스트
- 동일 시작 상태
- 동일 응답 수 또는 최대한 유사한 조건
- 순서 교차 A/B 또는 복수 반복
- 캐시 입력과 비캐시 입력 분리
- 총 토큰을 1차 지표로 사용

한다.

**출력 토큰 절감만으로 확대하지 않는다.**


# 27. 모델 라우팅 정책 적용

기본 주력은 **GPT-6 + 높은 추론**으로 유지한다.

이번 작업의 다음 항목은 GPT-6 영역이다.

- Actor 손상 복구
- reconciliation
- lifecycle
- NPC 사건 설계
- 복합 사건
- transaction
- structure fallback
- 저장 의미 변경
- Story Integrity
- 최종 검증 판단

Terra/Luna는 게임 품질과 정확도에 영향이 없는 저위험 작업만 맡길 수 있다.


# 28. 이번 작업에서 Terra/Luna 시험 가능한 후보

모델 라우팅 시험을 한다면 다음 정도로 제한한다.

### Terra 후보

- GPT-6가 명세를 확정한 뒤 추가 GameTest 구현
- 반복되는 진단 assertion 확장
- 이미 설계된 테스트 harness 보강
- 단순 serialization round-trip 테스트 추가
- 로그 포맷 정리

### Luna 후보

- 테스트 데이터 fixture 생성
- 상수/리소스/명백한 boilerplate
- 기계적인 파일 정리
- 결과 표 생성

단순해 보이더라도 상태 의미를 판단해야 하면 GPT-6로 승격한다.


# 29. 모델 변경 때문에 작업을 끊지 말 것

모델 전환은 반드시 **독립 하위 작업 경계**에서만 한다.

예:

`GPT-6가 Actor 손상 정책 설계·구현·1차 검증 완료`
→
`Terra가 명세가 고정된 추가 테스트 작성`
→
`GPT-6가 전체 회귀와 판정`

가능.

반대로:

`reconcile 수정 중`
→ 모델 변경
→ 다른 모델이 이어서 절반 수정

금지.

모델 전환 자체를 이유로 사용자에게 중간보고하거나 작업을 멈추지 않는다.


# 30. Terra/Luna 확대 기준

향후 저비용 모델 사용 범위는 고정하지 않는다.

보고서를 누적해 다음을 확인한다.

- 최초 성공률
- 테스트 통과율
- 요구사항 누락
- GPT-6 재검수 수정량
- 버그 유입
- 재작업 횟수
- 총 사용량
- 작업 시간
- 장기 연속성

여러 작업에서 **품질·기능 정확도·검증 수준 저하가 없고 순사용량 절감이 확실하면 적용 범위를 단계적으로 더 확대한다.**

반대로 재작업이나 누락이 늘면 해당 작업군을 다시 GPT-6로 돌린다.


# 31. 모델별 성능 비교를 보고서에 남길 것

Terra/Luna를 사용했다면 최종 보고서에 반드시 다음을 기록한다.

| Task | Model | 선정 이유 | 최초 성공 | 테스트 | 재작업 | GPT-6 검수 결과 | 사용량 |
|---|---|---|---|---|---|---|---|

모델 자체의 마케팅상 성능이 아니라 **이 프로젝트에서 실제 결과**로 확대 여부를 판단한다.


# 32. 테스트 통과를 위해 기존 테스트를 훼손하지 말 것

현재 48개 GameTest를 유지한다.

기존 검사가 새 정책과 충돌하면:

**기존 검사가 잘못된 과거 정책을 검증하는 것인지**
또는
**새 코드가 기존 요구사항을 깨뜨린 것인지**

먼저 판단한다.

D처럼 정책이 명확하게 바뀐 경우에는 **검증 의도를 유지하면서 테스트 방법을 갱신**한다.

단순히 PASS를 만들기 위해 assertion을 삭제하지 않는다.


# 33. 테스트 숫자보다 실패 표면 확대

새 테스트는 가능하면 기존과 다른 실패를 잡아야 한다.

이번 단계에서 특히 중요한 실패 표면:

- loaded/unloaded ambiguity
- stale generation
- duplicate canonical actor
- corrupted actor metadata
- process restart
- copy-world isolation
- suspended runtime restore
- NPC duplication
- transaction double commit
- transaction lost commit

이다.


# 34. 자연 생존 테스트는 아직 뒤 단계

Wayfarer → Actor 손상 → NPC → 복합 사건 → Transaction → Fallback이 먼저다.

그 이후 자연 생존 전체 진행을 수행한다.

현재 기능 검증을 위해 순간이동/직접 사건 호출을 사용하는 것은 허용하지만 자연 생존 테스트와 혼동하지 않는다.


# 35. 이번 작업의 버전 정책

지금은 계속 `0.3.1-dev.1` 작업 계열에서 진행한다.

단순 테스트 추가나 일부 안정화 성공만으로 `0.3.2`로 올리지 않는다.

0.3.2 승격은 기존에 합의한 주요 기능 게이트가 실제로 닫힌 뒤 결정한다.


# 36. 이번 단계 종료 시 필요한 판정표

최소한 다음을 정확히 판정한다.

| 항목 | 판정 |
|---|---|
| Wayfarer 최신 ABCDE | PASS/PARTIAL/FAIL |
| 사망 OFF |  |
| 사망 ON |  |
| actor 실제 청크 이탈 |  |
| Nether |  |
| End |  |
| 새 프로세스 재접속 |  |
| 실제 복사 월드 |  |
| Actor 손상 복구 |  |
| 완전 소실 ticket |  |
| NPC 실제 사건 |  |
| 복합 사건 |  |
| Item transaction |  |
| Reward transaction |  |
| Structure fallback |  |
| Story Integrity |  |
| 저장 migration |  |
| 자연 생존 |  |
| 전체 안정화 |  |

수행하지 않은 것은 반드시 `NOT TESTED`로 표시한다.


# 37. 완료 보고서 — 별도 첨부 없이 인수인계 가능해야 함

기존 상세 보고 규칙을 계속 적용한다.

Caveman이나 저비용 모델을 사용했다고 보고서를 축약하지 않는다.

최종 보고서 본문만 새 세션에 전달해도 다음 작업을 시작할 수 있어야 한다.

반드시 포함:

- Current 한 줄
- 최신 버전/작업본
- 최신 기준 JAR/소스
- SHA-256
- 저장 형식/schema/art revision
- 이번 작업 목표
- 실제 변경 사항
- 수정 파일/클래스/핵심 함수
- 상태 머신 변화
- 저장 의미 변화
- migration 변화
- 기능별 PASS/PARTIAL/FAIL/NOT TESTED
- 자동 테스트와 실제 클라이언트 테스트 구분
- 실제 검증 횟수
- 발견한 문제
- 문제의 근본 원인
- 수정 방법
- 재검증 결과
- 모델별 사용 내역
- Caveman 사용 여부
- 사용량 변화
- 원본 보존 여부
- BASELINE/MODIFIED/ROLLBACK
- 주요 명령과 exit code
- 최신 산출물
- 잔여 문제
- 다음 작업의 첫 시작점
- 다음 버전 승격 조건


# 38. 작업 실패도 보고할 것

최종적으로 고쳤다고 실패 기록을 지우지 않는다.

특히:

- 테스트가 처음 실패했던 이유
- 코드 결함
- 환경 오류
- 잘못된 가정
- 복구 실패
- 모델 변경으로 인한 재작업

을 구분한다.

그래야 다음 세션에서 같은 문제를 반복하지 않는다.


# 39. 이번 작업에서 절대 하지 않을 것

- 최종 도시 리빌드
- 리소스팩 본격 제작
- 대규모 세트피스 구현
- 새 몬스터 대량 추가
- 스토리 전체 재작성
- Foreshadow 콘텐츠 대량 구현
- 퍼즐 콘텐츠 대량 구현
- Horror Director 완성
- 단순 사용량 절감을 이유로 고위험 코드를 Terra/Luna/Caveman에 넘김
- 안정성 검증을 생략하고 0.3.2 승격


# 40. 이번 작업의 종료 목표

1차 목표:

> **Wayfarer가 종료·사망·청크 이탈·차원 왕복·프로세스 재시작·월드 복사에서도 동일한 단일 canonical actor와 사건 상태를 유지한다.**

2차 목표:

> **Actor 저장이 일부 손상되거나 실제 actor가 완전히 소실되어도 잘못된 복제·월드 훼손·진행 소실 없이 진단 또는 안전 복구된다.**

여기까지 확실히 닫은 후에야 실제 NPC/복합 사건으로 범위를 확대한다.

전체 캠페인 안정화는 그 이후에도 **PARTIAL**일 수 있다.

성급하게 PASS로 올리지 않는다.