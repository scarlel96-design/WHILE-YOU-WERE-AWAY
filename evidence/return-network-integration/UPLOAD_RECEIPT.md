# GitHub 업로드 확인 / return-network-integration

- 저장소: https://github.com/scarlel96-design/WHILE-YOU-WERE-AWAY
- 브랜치: `work/return-network-integration`
- 코드·패키지·검증 증거 커밋: `79a0d47d7b8cf403c701bde81ae1e2453cf2e370`
- `git push -u origin work/return-network-integration`: exit 0.
- `git ls-remote origin refs/heads/work/return-network-integration`: 위 커밋과 일치, exit 0.
- 업로드 경로: `src/`, `build.gradle`, `docs/RETURN_NETWORK_INTEGRATION_031.md`, 이번 tools 4개, `dist/return-network-integration/`, `evidence/return-network-integration/`, baseline 실행이 생성한 `evidence/return-network-contract/gametest/711dd3e3-3ecb-4880-904c-6494f5f23602/`.
- 최종 client ledger: 22회 시도 / 21 PASS / 환경 harness 1회 실패 후 수정 통과. 각 PID 종료 확인. 개발 초기 실행은 별도 보존.
- 최종 자동: GameTest 95 PASS, Core assertions 176857 PASS, build exit 0. tests-4.log.
- 전체/복합 사건 PARTIAL. 별도 NPC/설비 청크 상태 및 추가 경로 장애, 이전 NPC 실제 회귀, 실제 리듬/조명의 기능적 가독성 검증이 남아 있다.
- 새 JAR SHA-256: c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162
- 새 source ZIP SHA-256: c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda
- 패키지의 src와 현재 working src 모든 파일 bytes 일치. ZIP CRC, 5개 산출물 hash 및 Git blob bytes 일치를 재확인했다.
- ZIP 생성 후 추가된 것은 업로드 메타데이터(.gitattributes, 이 영수증)이다. 코드·빌드·테스트 내용은 바뀌지 않았다.
- Windows Git의 긴 파일 경로로 최초 stage 실패: 명령별 `-c core.longpaths=true`로 해결. 전역 설정 변경 없음.
- 원본 로그의 trailing spaces 때문에 전체 diff whitespace 검사 실패: 로그는 정제하지 않고 원본을 보존. 소스/문서/도구는 EOF 빈 줄만 허용한 whitespace 검사를 통과했다.
- `.gitattributes`는 이번 evidence/dist만 bytes 보존하며 ROLLBACK.sh는 LF 유지. executable Git mode 100755. 원격 전달에서도 hash가 변하지 않는다.
- 원본 repo의 사용자가 staged한 pycache 삭제 6건은 유지했고 master는 변경하지 않았다. 기존 패키지/원본 월드 보존.
- 롤백은 disposable JAR copy만 bf8e234052c8a33ea49cb02c3cabcd941515e29fc126280fdcfa62d4ba0db9c0 으로 복원. 새 배포 JAR 유지, 세이브 downgrade 없음.
- Caveman HOLD. 하위 모델/에이전트 사용 없음. 사용량 비교 실험 없음.

다음 시작점: 이 브랜치에서 npc-only / equipment-only 로딩 조합과 경로 장애를 분리 검증한 뒤, 단일 bell/문장 placeholder를 실제 관찰 가능한 짧음-짧음-김 신호로 기능 완성한다. Item/Reward/Fallback 단계로 아직 진입하지 않는다.
