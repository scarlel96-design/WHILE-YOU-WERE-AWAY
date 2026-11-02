# [MAIN GPT-6.1 SOL RESULT — V2-03]

## CURRENT

0.3.1-dev.1 / work/v2-playable-core / V2-03 PARTIAL.
V2-01, V2-02와 캠페인도 PARTIAL이다. 0.3.2 NOT PROMOTED.
Save 5 / Actor optional 1 / Return Network 1 / city plan 3.
Caveman HOLD. 최종 아트 보류.

## BASELINE

시작 로컬 ffc08db에서 원격의 문서 전용 6개 커밋을 감사했다.
실제 기준은 82beffac6347c60258e4b9db0098eecad423b176다. 원격과 로컬이 일치한 상태에서 구현을 시작했다.
개선된 폐역 JAR와 기준 소스 ZIP을 별도 보존했다.
원래 master 작업 폴더와 기존 무관한 untracked 파일은 유지했다.

## IMPLEMENTED GAME CONTENT

- 개선된 폐역 템플릿과 기존 story anchor·relay·기록·상자를 보존했다.
- 선택형 자연 생성 관리소와 낮은 정비 통로를 추가했다.
- 두 시설에 작업대·보관통과 실제 작동하는 레버·조명을 넣었다.
- 끊어진 지상 선로와 낮은 정비 입구로 서로 다른 발견 경로를 마련했다.
- 관리소의 고정 가로보 7구간을 멀리서만 기울여 렌더한다.
- 걸음을 멈춘 뒤 빈 정비 공간에서 한 발소리를 낸다.
- 귀로꾼에 고유 작업복 atlas, 팔꿈치·옷자락과 비대칭 자세를 연결했다.
- 정지·보행·회전·경고·위기 애니메이션 5개를 연결했다.
- 원본 마찰 걸음·전용 경고·멀어지는 발소리 3개를 추가했다.

## STRUCTURES / GENERATION

관리소 19×10×17: spacing 32 / separation 12 / salt 74192362.
정비 통로 17×9×21: spacing 40 / separation 16 / salt 74192363.
바이옴은 plains, forest, birch_forest, taiga다.
마을 후보 6청크 제외 조건을 적용한다. 고정 생성 확률은 주장하지 않는다.
새 청크에만 표준 jigsaw 배치를 수행한다. 기존 역과 도시는 재건하지 않는다.
지형 아래 입구는 표면 투영 -3 높이와 계단을 사용한다.
자연 동굴 연결과 연속된 지리적 철도 연결은 보장하지 않는다.

## PRESENTATION

시각 가로보는 24~96블록에서 작동한다. 양쪽 거리 경계에서 부드럽게 줄어든다.
최대 12도, 160틱 작동 후 정지한다. 전체 주기는 1,800틱이다.
렌더 한도는 128블록, 시설당 7개 구간이다.
실제 블록·충돌·카메라·서버 상태를 이동하지 않는다.
전체 철교 연출은 미구현이다.

추가 발소리는 12틱 이동 후 8틱 정지에 반응한다.
거리 자격은 12블록이다. 재생 제한은 1,200틱이다.
죽음·관전자·이탈은 준비 상태를 취소한다.
클라이언트별로 계산한다. 영구 연출 타이머와 player knowledge를 저장하지 않는다.

경고 자세·자막은 음향 대체 단서다.
전용 경고음은 경로 방해 경고에만 사용한다.
새 음향은 44,100Hz mono Vorbis의 원본 합성이다. 실제 청감은 NOT TESTED다.
상세 조건과 프롬프트는 docs/V2_03_CONTENT_STATUS.md 및 V2_03_ASSET_PROVENANCE.md에 기록했다.

## PRODUCTION FILES

- WhileAway.java: 가로보 Block/BlockEntity 및 음향 3종 등록.
- RailBeamBlock.java: facing·segment, 고정 충돌과 클라이언트 ticker.
- RailBeamEntity.java: 거리별 visualAngle과 이동·정지 echo.
- RailBeamRenderer.java: 공유 회전축과 제한된 렌더 범위.
- ClientSetup.java: renderer 등록과 고유 atlas 연결.
- Wayfarer.java: 일시적 WARNING, 회전·경고 자세, 상황별 음향.
- RailSiteGameTests.java: 신규 구조물·보호·연출 7개 검사.
- 새 worldgen JSON·NBT·모델·텍스처·OGG와 언어 리소스.
- 생성 도구·자산 검사·패키지 도구 및 콘텐츠 현황 문서.

## BUILD / QA

BUILD: 실제 구조물 2개와 상호작용, 환경 장면 2개, 귀로꾼 자산을 구현했다.
QA: 새 배치·회전·상자 보존·레버·연출 검사 7개와 기존 게이트를 실행했다.
FIX: 컴파일 API 위치, 검사 classpath, 생성 도구 인코딩과 패키지 입력을 정정했다.
실측 작업 시간과 토큰 사용량 자료는 없다. 비율 수치를 추정하지 않는다.

자동 통합 실행은 총 3회다. 고유 GameTest 수는 127개다.
최종 source/resources에서 GameTest 127/127 PASS, Core 176,858 PASS, build PASS다.
자산/JAR 10,874 checks, OGG 10개 디코딩, 소스 ZIP 414파일 일치를 확인했다.
새 실클라이언트 실행은 0회다. 실제 설치형 JAR 실행도 NOT TESTED다.
HUMAN EXPERIENCE / ART QUALITY / AUDIO EXPERIENCE는 모두 NOT TESTED다.

## SAVE / WORLD PROTECTION

기존 저장·Actor·generation·checkpoint·transaction 의미는 그대로다.
NarrativeData·StoryStorage·StoryActors·ReturnNetworkEvents·CityDistrict 클래스는 바이트가 같다.
기준 클래스 157개 중 151개는 바이트가 같다.
변경된 6개 중 NpcRenderer·Renderer는 명령어가 같고 메타데이터만 다르다.
기존 폐역 NBT도 기준 JAR와 바이트가 같다.
보호 processor와 배치 경계는 상자 내용·사용자 벽·작업 범위를 보존한다.
다른 시드·다른 모드와 모든 바닐라 구조물 중첩은 미검증이다.
기존 사용자 월드는 열거나 수정하지 않았다. 기존 plan 1·2·3 코드도 그대로다.
새 가로보 블록을 가진 월드의 구버전 JAR 다운그레이드는 미지원이다.
ROLLBACK은 패키지만 복원한다. 월드나 진행을 되돌리지 않는다.

## FAILURES

컴파일의 잘못된 BlockEntity override → NeoForge renderer extension으로 이동 → 최종 컴파일 PASS.
JUnit의 Minecraft classpath 누락 → 해당 미실행 검사를 제거 → 실제 GameTest로 거리 경계 검사 PASS.
생성 도구의 기본 cp949 읽기 → UTF-8 명시 → 태그 재생성 PASS.
이전 보호 태그가 들어간 후보 JAR → 최종 리소스 동결·재빌드 → 리소스 바이트 일치 PASS.
패키지의 legacy-v1 잘못된 가정 → 실제 v2/v3와 음향 메타데이터 포함 → ZIP 414파일 검사 PASS.
실제 Minecraft 플레이 중 발견한 새 결함으로 분류할 항목은 없다. 새 플레이를 실행하지 않았다.

## ARTIFACTS

경로: dist/v2-03-closure. 모든 크기는 bytes다.

| 파일 | 크기 | SHA-256 |
|---|---:|---|
| BASELINE-source.zip | 982951 | `c7d47351c297a1647eed6216a6172bd78839152d2cc28195eb29099d7bcdaa9d` |
| BASELINE.jar | 705191 | `7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6` |
| DIFF.patch | 3425577 | `9408352e3629e39c1b8b57dbb1881eaacc2f0a1331ea288b7276297585ecd6e7` |
| ROLLBACK.ps1 | 1549 | `b74f60ce5da055626a04e814ef968ca20e86ba5f19105d39678df06362288515` |
| ROLLBACK.sh | 264 | `d47323525a91a10a0b25c1a4386639ea5e6d45f2c35a2ba8323465bf9bb521ef` |
| SOURCE_MANIFEST.json | 51053 | `38b768666b783005960b867a2188786886afdfe000802d419e91c5ba6f945077` |
| VERIFICATION.txt | 7288 | `c3cfc19d4c874f47a9510f7dde7926a53263589e0c0bd0493be09d279ca3f514` |
| whileaway-0.3.1-v2-03-content-source.zip | 3656106 | `ec5237fb1821028c01e32c7c38349f84ca564f23a1799e6a00bed4d1fd6622f4` |
| whileaway-0.3.1-v2-03-content.jar | 3344137 | `72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05` |

## EXECUTION EVIDENCE

BASELINE
Command: python tools/verify-assets.py dist/v2-03-closure/BASELINE.jar
Input: preserved baseline JAR; SHA-256 7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6
Execution: before source changes, using the baseline verifier.
Literal output: PASS asset assertions=6820; station/animation/localization/OGG/JAR
Exit status: 0
Baseline source ZIP SHA-256: c7d47351c297a1647eed6216a6172bd78839152d2cc28195eb29099d7bcdaa9d

MODIFIED
Command: python tools/verify-assets.py dist/v2-03-closure/whileaway-0.3.1-v2-03-content.jar
Input: final modified JAR; SHA-256 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05
Literal output: PASS asset assertions=10874; station/animation/localization/OGG/JAR
Exit status: 0
Command: .\gradlew.bat --offline --no-daemon compileJava coreTest build runGameTestServer --console=plain
Environment: GRADLE_USER_HOME=C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\gradle-home
Input: final production source and resources; isolated GameTest world.
Literal outputs:
PASS core assertions=176858
[02:42:23] [Server thread/INFO] [minecraft/GameTestServer]: All 127 required tests passed :)
BUILD SUCCESSFUL in 1m
Exit status: 0
Final gate log: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\evidence\v2-03-closure\final-gate.log
Unique GameTests: 120 existing + 7 new = 127. Three integrated executions; final source result is 127 PASS.
Audio decode command: bundled Python 3.12 tools/verify-audio.py with PYTHONPATH=../audio-deps
Literal output: PASS audio files=10; decoded/finite/duration/mono/44100Hz/peak<0.95; playback=NONE
Exit status: 0
Source ZIP SHA-256: ec5237fb1821028c01e32c7c38349f84ca564f23a1799e6a00bed4d1fd6622f4

ROLLBACK
Preparation: copy MODIFIED_FILE to dist/v2-03-closure/rollback-test.jar.
Command: & 'C:\Program Files\Git\bin\bash.exe' dist/v2-03-closure/ROLLBACK.sh dist/v2-03-closure/rollback-test.jar
Input: separate modified JAR copy, SHA-256 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05
Literal output: ROLLBACK PASS sha256=7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6 worlds=UNCHANGED
Exit status: 0
Restored SHA-256: 7d60cc29edea2b4cd653454c9006e75ebf4fddef999105d2953864c189081cf6
Restored behavior/status: previous verified package bytes restored; new rail-site/beam resources absent in restored JAR.
Scope: package restoration only. No Minecraft process or world downgrade test.
World files: unchanged. MODIFIED_FILE remains changed, SHA-256 72fa3adc7e35802145d9e67b7d3d2eedd8d0da7e863fc4ca7e56de9072d75b05.



## GITHUB

구현 커밋: 32cf3a4c7f39e71c18c9d93e938bb3ac9f84c4a4.
소스 스냅샷: 7ff9a930a490f5758d61c0eec8ec6247047c3aec.
해당 스냅샷의 GitHub 업로드와 원격 HEAD 일치를 source-upload.txt 및 source-remote.txt로 확인했다.
그 뒤의 증거·산출물 커밋은 production source를 바꾸지 않는다.
브랜치: work/v2-playable-core. master는 변경하지 않는다.
최종 게시 커밋은 완료 응답의 실제 원격 확인값을 따른다.

## REMAINING / NEXT

V2-03 PARTIAL: 콘텐츠 후보 구현과 자동 검사는 완료했다. 화면·청감·미적 수용은 대기한다.
모든 지형과 구조물 중첩에 대한 전수 검증은 수행하지 않았다.
전체 콘텐츠 완성률은 UNKNOWN이다. 콘텐츠 분모와 플레이 비중을 먼저 확정해야 한다.
50~60% 대규모 사용자 플레이테스트 목표에 도달했다고 주장하지 않는다.
GAP-007·008·009는 계속 열려 있다.
다음 시작점은 이 빌드의 지형별 배치·연출·경고 가독성 피드백이다.
후속 콘텐츠 후보는 간접 탐색 보조와 생활 기반 재방문이다.
Caveman·새 Threat·최종 아트·0.3.2 승격은 진행하지 않았다.

## 게시 환경 정정

Windows Git이 긴 quarantine 경로의 직접 추가를 거부했다.
게임 결함으로 분류하지 않는다. 운영체제 설정도 변경하지 않았다.
세 실행의 원시 파일 126개를 gametest-evidence.zip에 보존했다.
ZIP 내부의 원래 경로와 파일별 SHA-256을 검사했다. 모든 CRC와 해시가 일치했다.
원시 로컬 폴더 3개는 그대로 남겼다. 기존 무관한 untracked 15개도 보존했다.
배포 파일의 바이트와 ROLLBACK.sh의 LF·실행 비트는 .gitattributes로 유지한다.
