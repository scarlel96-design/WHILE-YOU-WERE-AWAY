# [MAIN GPT-6.1 SOL RESULT — V2-03]

## CURRENT

0.3.1-dev.1 / work/v2-playable-core / V2-03 PARTIAL.
V2-01·V2-02·캠페인도 PARTIAL이다. 0.3.2 NOT PROMOTED.
Save 5 / Actor optional 1 / Return Network 1 / city plan 3 / Caveman HOLD.

## CONTENT / INFRASTRUCTURE / VALIDATION

콘텐츠 보강: 관리소와 정비 통로가 물·급경사 후보를 건너뛴다.
전용 생성 처리: RailSiteStructure 한 클래스가 두 시설의 기존 Jigsaw 배치에 지형 검사를 붙인다.
검증: 기존 127개와 신규 2개 GameTest를 포함한 통합 게이트를 한 번 실행했다.
기존 폐역·귀로꾼·환경 장면·음향 자산은 그대로 보존했다.
대형 범용 프레임워크나 장시간 자동 플레이는 추가하지 않았다.
실측 시간·토큰 비중 자료는 없다.

## BASELINE

로컬·원격 기준 HEAD: 06229b928586550a7985220d2d465c0faf4232f8.
기준 JAR: dist/v2-03-terrain/BASELINE.jar
SHA-256: 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05
기준 소스 ZIP: dist/v2-03-terrain/BASELINE-source.zip
SHA-256: ec5237fb1821028c01e32c7c38349f84ca564f23a1799e6a00bed4d1fd6622f4
이전 dist/v2-03-closure 산출물은 덮어쓰지 않았다.

## ACTUAL CHANGES

RailSiteStructure는 먼저 기존 Jigsaw의 회전과 단일 piece를 선택한다.
실제 회전된 footprint의 5×5 지점을 검사한다.
WORLD_SURFACE_WG와 OCEAN_FLOOR_WG가 다르면 해당 후보를 보류한다.
표본의 최대 높이 차이가 2블록을 넘으면 후보를 보류한다.
범위가 32블록을 넘거나 piece가 여러 개면 보류한다.
검사 통과 후 이미 선택한 builder를 반환한다. RNG로 회전을 다시 선택하지 않는다.
청크를 강제로 로드하거나 실제 블록을 조회·이동·삭제하지 않는다.

새 시설 두 정의는 whileaway:rail_site type과 내부 layout 필드를 사용한다.
내부 설정·구조물 ID·pool·템플릿·생성 간격·바이옴은 그대로다.
기존에 생성한 시설은 저장된 piece 좌표를 사용한다. 소급 재생성은 없다.
신규 후보는 줄어든다. 실제 자연 발견 빈도는 아직 미검증이다.

수정 파일:
- src/main/java/io/github/whileaway/content/RailSiteStructure.java
- src/main/java/io/github/whileaway/WhileAway.java
- src/main/java/io/github/whileaway/RailSiteGameTests.java
- src/main/resources/data/whileaway/worldgen/structure/rail_signal_office.json
- src/main/resources/data/whileaway/worldgen/structure/rail_maintenance_passage.json
- tools/generate-rail-sites.py
- tools/package-v2-03-closure.py
- docs/V2_03_CONTENT_STATUS.md
- .gitattributes

## VALIDATION

Compile/build: PASS. GameTest 129/129 PASS. Core assertions 176,858 PASS.
통합 게이트는 이번 작업에서 1회 실행했다.
새 검사는 물·경사·표본 상한과 실제 오버월드 noise 계산을 다룬다.
기존 배치 검사는 builder 재사용과 structure-start NBT 재로딩까지 확장했다.
PASS rail terrain noise seed=203 candidates=37 accepted=1 rejected=36; chunk placement=NONE
이 결과는 전체 생성 확률이 아니다. 실제 자연 청크 배치 성공으로 재분류하지 않는다.

PASS baseline classes byte-identical=159/161
Changed baseline classes: ['io/github/whileaway/RailSiteGameTests.class', 'io/github/whileaway/WhileAway.class']
PASS storage, Actor identity, Wayfarer AI and Return Network class bytes unchanged
PASS all existing structure NBT, textures and OGG bytes unchanged
PASS all final packaged resources match final inputs
PASS source ZIP CRC and working-tree manifest files=415
PASS restored JAR lacks new terrain guard; modified JAR remains changed
PASS reverse patch check; exit=0

## STORAGE / COMPATIBILITY

저장 형식·Actor identity·generation·checkpoint·transaction 계약은 변경하지 않았다.
기존 템플릿 NBT·텍스처·음향과 귀로꾼 AI는 바이트가 같다.
structure-start ID와 piece 좌표는 실제 Minecraft NBT 저장·재로딩 검사에서 유지됐다.
기존 실제 사용자 월드와 구버전 JAR로의 월드 다운그레이드는 NOT TESTED다.
ROLLBACK은 이전 JAR 바이트 복원이다. 월드 진행을 되돌리지 않는다.

## FAILURES

일반 Git 원격 조회가 getaddrinfo thread startup 오류로 실패했다.
승인된 호스트 실행에서 동일 브랜치 HEAD를 확인했다. 게임 결함과 구분한다.
이번 작업에서 컴파일·GameTest·빌드 실패는 없었다.
지형 결함은 기존 설정의 검사 부재를 확인한 것이다. 실플레이 재현으로 주장하지 않는다.

## EVIDENCE

BASELINE
Command: python tools/verify-assets.py dist/v2-03-terrain/BASELINE.jar
Input: preserved published JAR; SHA-256 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05
Literal output: PASS asset assertions=10874; station/animation/localization/OGG/JAR
Exit status: 0
Baseline source ZIP SHA-256: ec5237fb1821028c01e32c7c38349f84ca564f23a1799e6a00bed4d1fd6622f4
Original dist/v2-03-closure artifacts retained unchanged.

MODIFIED
Command: python tools/verify-assets.py dist/v2-03-terrain/whileaway-0.3.1-v2-03-terrain.jar
Input: final modified JAR; SHA-256 bbc8ffdf438d9394fcfb57fe1103c4677f1deb1ad394fe415424410ee17b4283
Literal output: PASS asset assertions=10874; station/animation/localization/OGG/JAR
Exit status: 0
Command: .\gradlew.bat --offline --no-daemon compileJava coreTest build runGameTestServer --console=plain
Environment: GRADLE_USER_HOME=C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\gradle-home
Input: final source/resources; new isolated GameTest world.
Literal outputs:
PASS core assertions=176858
PASS rail terrain noise seed=203 candidates=37 accepted=1 rejected=36; chunk placement=NONE
[12:44:50] [Server thread/INFO] [minecraft/GameTestServer]: All 129 required tests passed :)
BUILD SUCCESSFUL in 1m 1s
Exit status: 0
Integrated gate executions this continuation: 1. Unique GameTests: 127 retained + 2 new.
Real Overworld generator height calculations were used. Natural chunk placement was not executed by the noise probe.
Source ZIP SHA-256: 9af36e2aad15002a79d13466fb06ce86c2472584c9567cc6d07cb034414e6314

ROLLBACK
Preparation: copy MODIFIED_FILE to dist/v2-03-terrain/rollback-test.jar.
Command: & 'C:\Program Files\Git\bin\bash.exe' dist/v2-03-terrain/ROLLBACK.sh dist/v2-03-terrain/rollback-test.jar
Input: separate modified JAR copy, SHA-256 bbc8ffdf438d9394fcfb57fe1103c4677f1deb1ad394fe415424410ee17b4283
Literal output: ROLLBACK PASS sha256=72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05 worlds=UNCHANGED
Exit status: 0
Restored behavior/status: previous verified package bytes restored; RailSiteStructure.class absent in restored JAR.
Restored SHA-256: 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05
World files unchanged. No world downgrade or real-client rollback run.
MODIFIED_FILE remains changed, SHA-256 bbc8ffdf438d9394fcfb57fe1103c4677f1deb1ad394fe415424410ee17b4283.



## ARTIFACTS

- BASELINE-source.zip: 3656106 bytes / SHA-256 `ec5237fb1821028c01e32c7c38349f84ca564f23a1799e6a00bed4d1fd6622f4`
- BASELINE.jar: 3344137 bytes / SHA-256 `72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05`
- DIFF.patch: 20963 bytes / SHA-256 `d2cbb9e3ef47159bc5051354c9f32676954c6fb676ba63b4aebba3f0c4177668`
- ROLLBACK.ps1: 1549 bytes / SHA-256 `a7e81ab29dbe2a9dedfd93e41b4f7db9427758504c59be5619fec9411a5a623e`
- ROLLBACK.sh: 264 bytes / SHA-256 `d47323525a91a10a0b25c1a4386639ea5e6d45f2c35a2ba8323465bf9bb521ef`
- SOURCE_MANIFEST.json: 51194 bytes / SHA-256 `bba0ae1f37467a5530bb3c5ecb44fc384e90c7a4b865fd75a4a1079d7183c74d`
- VERIFICATION.txt: 5771 bytes / SHA-256 `f7e16ba74744120e4cd48364ada24e51ee89dc6939256ee5ba4d966d8bf5b1f5`
- whileaway-0.3.1-v2-03-terrain-source.zip: 3659361 bytes / SHA-256 `9af36e2aad15002a79d13466fb06ce86c2472584c9567cc6d07cb034414e6314`
- whileaway-0.3.1-v2-03-terrain.jar: 3350093 bytes / SHA-256 `bbc8ffdf438d9394fcfb57fe1103c4677f1deb1ad394fe415424410ee17b4283`

## GITHUB / NEXT

구현 및 소스 스냅샷: 43339bf9576c31d68f5f4d63a4a6ff829195e491.
브랜치: work/v2-playable-core. 최종 원격 확인값은 완료 응답에 기록한다.
후속 증거 커밋은 production source를 변경하지 않는다.
새 실클라이언트 0회. HUMAN EXPERIENCE·ART QUALITY·AUDIO EXPERIENCE는 NOT TESTED다.
전체 완성률은 분모 미확정으로 UNKNOWN이다. GAP-007·008·009는 열려 있다.
다음 시작점은 사용자 지형별 배치·발견 빈도·장면 가독성 피드백이다.
표본 사이의 작은 골짜기와 다른 구조물 중첩은 남은 검증 항목이다.
기존 미추적 항목 18개와 이번 원시 증거 폴더를 보존한다.
Caveman·0.3.2 승격·장시간 V2-01E 자동 플레이는 진행하지 않는다.
