> **요구사항 추적 의무 — 2026-11-02 추가:** [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)와 [CONTINUITY_AUDIT.md](CONTINUITY_AUDIT.md)를 먼저 읽는다. 모든 신규 WO는 적용할 WWA-* 요구 ID를 IN/OUT/DEFERRED로 매핑한다. 종료 시 코드 위치·증거·검증 상태·누락 GAP을 갱신한다. **진행 중 V2-03에는 소급 적용하지 않는다.** 이 추적표가 과거 세션 원문을 100% 전수 검증한 것처럼 표시해서는 안 된다.

# WHILE YOU WERE AWAY — 미래 작업지시서·인수인계 규약

적용: **현재 V2-03 완료 이후 신규 작업지시서부터**. 메인 구현 담당: GPT-6.1 SOL / Codex. 이 문서는 새 기능 구현이나 진행률 승격 증거가 아니다.

## 1. 시작 시 필수 재조회

1. GitHub 저장소 `scarlel96-design/WHILE-YOU-WERE-AWAY`의 최신 브랜치·HEAD를 확인한다. 예전 SHA를 무조건 최신값으로 쓰지 않는다.
2. 루트 `AGENTS.md`, `docs/PROJECT_CONTINUITY.md`, `docs/DECISION_LOG.md`, `docs/CONTENT_ROADMAP.md`, `docs/DESIGN_V05.md`, `docs/MYSTERY_RESOURCE_EXTENSION.md` 및 필요한 최신 evidence를 읽는다.
3. 진행 중인 작업·구현 상태·저장 format·Actor/Return Network schema·도시 construction plan·JAR·CI를 확인한다.
4. 새 요구가 과거 사용자 결정과 충돌하는지 판단한다. 명시적 사용자 승인 없이 현재 진행 작업을 소급 변경하지 않는다.
5. **설계 / 구현 / 자동 검증 / 인간 플레이**를 서로 다른 상태로 명시한다.

## 2. 신규 작업지시서의 최소 항목

- 제목, 목적과 본 작업이 어떤 **플레이 가능한 콘텐츠**를 늘리는지.
- 정확한 branch/HEAD 및 기준 버전과 immutable 입력.
- 범위: IN / OUT / NEXT. 과도하게 넓으면 고품질 작업 단위로 분해한다.
- 기존 v0.5 스토리·세 겹 미스터리·귀로꾼/Observer 성격·NPC 자율성과의 연결.
- 자연 발견·장소 생활감·구조물 미학·공포·음향·퍼즐·복선에 대한 실제 구현 요구. 해당 WO에 해당하지 않는 영역은 후속 후보로 남긴다.
- 저장·세대/UUID·트랜잭션·구조물 버전·이미 생성된 월드 보호 규칙.
- 테스트: build, 해당 타깃 GameTest, 리소스/배포 JAR, world preservation. 과거 전체 실클라이언트 장시간 생존 루프를 완료 조건으로 반복하지 않는다.
- 성능, 접근성, 무음 개발 클라이언트, 사용자 실제 플레이 분리.
- 산출물, GitHub 원격 업로드 여부, 정확한 검증 결과와 해시.
- 완료 / PARTIAL / NOT TESTED, 사용자 직접 검증이 필요한 항목, 다음 시작점.

## 3. 종료와 다음 작업 사이 필수 갱신

1. 작업 결과에서 새 구현·검증·미완료·바뀐 사용자 결정이 무엇인지 구분한다.
2. `docs/CONTENT_ROADMAP.md`를 실제 구현/검증 상태에 맞게 갱신한다. 가중 분모가 확정되기 전 %를 만들어내지 않는다.
3. 우선순위 또는 범위 결정을 변경한 경우 `docs/DECISION_LOG.md`에 날짜·이전 정책·새 정책·적용 시점·근거를 적는다.
4. 기존 v0.5 세계관과 충돌하는 창작적 변경은 사용자 결정이 있을 때만 공식화한다. 제안은 PROPOSED로 둔다.
5. 새로운 핵심 복선은 `docs/foreshadow-registry.json`에 ID/초기노출/실제 의미/회수/상태/검증을 연결한다. 완전히 구현되지 않았으면 IMPLEMENTED로 올리지 않는다.
6. 이슈/제약/다음 시작점은 `evidence/` 또는 상태 문서에 보존한다. 과거 실패 로그와 baseline을 지우지 않는다.
7. 문서 수정·코드·검증 결과를 GitHub에 게시하고 SHA와 정확한 반영 경로를 최종 보고한다.
8. 새 작업지시서 작성자는 갱신된 기록을 다시 읽고 그다음 범위를 정한다.

## 4. 금지하는 잘못된 완료 판정

- 과거 빌드 PASS를 현재 코드에서 재실행한 것으로 취급.
- 5개 클라이언트 프로세스의 정상 종료를 5회 자연 플레이 완주로 집계.
- 텍스처 파일이 존재함을 고품질 괴물 디자인 HUMAN PASS로 주장.
- 사용자 테스트 없이 무서움·사운드 위치감·퍼즐 가독성 합격 선언.
- 레버/책의 존재만으로 조사·퍼즐·전용 아이템 전체 구현 완료 선언.
- 오버월드 생활을 대기시간으로, 저녁골을 모드 전체의 유일 무대로 축소.
- Shadow/Observer를 본편의 만능 원인이나 실체적 보스로 고정.
- 새 아트·움직이는 지형을 이유로 사용자 블록을 파괴·덮어쓰기.
- 과거 "보조 GPT 40%" 분담, 실클라이언트 자동 탐험 우선, Caveman HOLD 해제 등을 승인 없이 부활.
- 장기 내용을 줄여 45~90분짜리 데모를 전체 본편의 플레이타임으로 대체.

## 5. 미래 작업지시서 시작 문구(템플릿)

> PROJECT: WHILE YOU WERE AWAY / MAIN GPT-6.1 SOL  
> SOURCE OF TRUTH: repository at verified current branch/HEAD  
> CONTINUITY: read AGENTS.md, PROJECT_CONTINUITY.md, DECISION_LOG.md, CONTENT_ROADMAP.md, DESIGN_V05.md  
> PRIORITY: playable content first; strong fear + deep mystery + Minecraft life; user owns first major human playtest at roughly 50–60% implemented content  
> SAFETY: do not damage player worlds, saves, actor identity or checkpoints; preserve automated build/targeted tests  
> REPORT: exact code/evidence/remaining/next, no unsupported PASS or release promotion

## 6. 외부 채팅 기록과 프라이버시

이 문서는 사용자가 제공한 긴 대화의 **핵심 결정만** 정리한 것이다. 대화 원본 1.25MB 파일은 저장소에 업로드하지 않는다. 개인 경로·사용량·개인 정보·비공개 대화는 설계 문서에 복사하지 않는다. 필요한 정보가 문서에 없으면 최신 코드와 사용자의 새 지시에서 확인하고, 추측해 과거의 합의로 꾸미지 않는다.
