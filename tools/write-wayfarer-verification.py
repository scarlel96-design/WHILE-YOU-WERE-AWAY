"""Assemble the handoff from reopened runtime evidence; do not infer campaign closure."""
from pathlib import Path
import hashlib,json,re,zipfile

R=Path(__file__).resolve().parents[1];E=R/'evidence/wayfarer-stability'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
g=json.loads((E/'GATES.json').read_text())
assert g['wayfarer_suite']=='PASS' and g['overall_stability']=='PARTIAL'
assert g['actor_corruption_fail_closed'].startswith('FAIL')
transaction=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'))
assert all(row['exit']==0 for row in transaction['commands'])
for name in ('baseline-tests.log','modified-tests.log'):
    assert 'All 48 required tests passed' in (E/name).read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E/'build.log').read_text(encoding='utf-8-sig')
design=(R/'docs/DESIGN_V04.md').read_text(encoding='utf-8')
assert [int(n) for n in re.findall(r'^## (\d+)\.',design,re.M)]==list(range(1,26))
quality=['No-Spawn','No-Stinger','Free-Look','Causality','Fairness','Persistence','Recontext','Second Run','Foreshadow','Voluntary Revisit']
assert all('| '+name+' |' in design for name in quality)
with zipfile.ZipFile(E/'baseline/previous-source.zip') as old:
    for name in ('docs/asset-contract.json','docs/foreshadow-registry.json'):
        assert old.read('while-you-were-away/'+name)==(R/name).read_bytes()
assert len(json.loads((R/'docs/asset-contract.json').read_text())['items'])==6
assert len(json.loads((R/'docs/foreshadow-registry.json').read_text())['entries'])==2
design_result='PASS design document has ordered sections 1-25 and 10 quality gates; prior placeholder groups=6 and foreshadow drafts=2 unchanged. This is document validation, not gameplay/art PASS.'
(E/'design-contract-check.txt').write_text(design_result+'\n',encoding='utf-8')
text=(R/'docs/WAYFARER_STABILITY_031.md').read_text(encoding='utf-8')
text+='\n\n# 절대 경로 및 실행 증거\n\n'
text+='Workspace: '+str(R)+'\n'
for key,p in [('INPUT_JAR',R/'dist/campaign-pilot/whileaway-0.3.1-campaign-pilot.jar'),
              ('INPUT_SOURCE',R/'dist/campaign-pilot/whileaway-0.3.1-campaign-pilot-source.zip'),
              ('MODIFIED_FILE',R/'dist/wayfarer-stability/whileaway-0.3.1-wayfarer-stability.jar'),
              ('DIFF_FILE',E/'DIFF.patch'),('VERIFICATION',E/'VERIFICATION.txt'),('ROLLBACK',E/'ROLLBACK.sh'),
              ('DESIGN_V04',R/'docs/DESIGN_V04.md')]:
    text+=key+': '+str(p)+'\n'
    if key in ('INPUT_JAR','INPUT_SOURCE','MODIFIED_FILE'):text+='SHA256: '+sha(p)+'\n'
text+='\n환경: JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot; GRADLE_USER_HOME='+str((R/'../../work/gradle-home').resolve())+'\n'
text+='명령은 위 Workspace에서 실행한다. Windows Bash는 C:\\Program Files\\Git\\bin\\bash.exe, PowerShell은 C:\\Program Files\\PowerShell\\7\\pwsh.exe다.\n'
text+='\nBASELINE GameTest: ./gradlew.bat --offline runGameTestServer\nInput: 수정 전 campaign-pilot 소스\nOutput: All 48 required tests passed :) / BUILD SUCCESSFUL in 37s\nExit: 0\n'
text+='\nMODIFIED GameTest: ./gradlew.bat --offline compileJava runGameTestServer\nInput: 현재 wayfarer-stability 소스\nOutput: All 48 required tests passed :) / BUILD SUCCESSFUL in 44s\nExit: 0\n'
text+='\nMODIFIED build: ./gradlew.bat --offline build\nInput: RUNTIME_SOURCE.sha256로 고정한 현재 소스\nOutput: PASS core assertions=176857 / BUILD SUCCESSFUL in 13s\nExit: 0\n'
for row in transaction['commands']:
    text+='\n'+row['label']+'\nCommand: '+row['command']+'\nInput: '+row['input']+'\nLiteral output:\n'+row['output']+'\nExit: '+str(row['exit'])+'\n'
text+='Restored hash: '+transaction['restored_hash']+'\nModified hash retained: '+transaction['modified_hash']+'\nWorld operations by ROLLBACK.sh: none. Save downgrade: not supported.\n'
text+='\n# 실제 클라이언트 22회 실행표\n\n'
text+='실행 orchestrator: python tools/run-wayfarer-stability.py crash --repeat 1, crash --repeat 2, exceptions --repeat 1, exceptions --repeat 2. 각 하위 작업은 직렬 실행한다.\n'
text+='| Command | 초 | PID | Exit | 종료 확인 | 결과 |\n|---|---:|---:|---:|---|---|\n'
rows=[json.loads(line) for line in (E/'runtime-commands.jsonl').read_text().splitlines()]
assert len(rows)==22
for row in rows:
    assert row['pass'] and row['process_exited']
    text+=f"| {row['command']} | {row['seconds']:.2f} | {row['pid']} | {row['exit']} | true | PASS |\n"
text+='\nInputs: crash 월드 두 개(각 다른 seed), exception 월드 두 개(각 다른 seed)와 전체 복사본 두 개. GameTest 전용 preset 월드와 별개다.\n'
for repeat in (1,2):
    proof=json.loads((E/f'world-copy-{repeat}.json').read_text())
    text+=f"\nPair {repeat}\nOriginal: {proof['source']}\nCopy: {proof['copy']}\nByte-identical at copy: {len(proof['files'])} files\n"
    text+=(E/f'world-independent-{repeat}.txt').read_text()
    text+='\nLiteral Seed output:\n'+(E/f'exceptions/runActorExceptionsSeed{repeat}.txt').read_text()
text+='\nIndependent reopen command: python tools/verify-wayfarer-stability.py\nExit: 0 (evidence reader completed; overall campaign remains PARTIAL and corruption gate FAIL)\nLiteral output:\n'+(E/'reopened-world-gates.log').read_text(encoding='utf-8-sig')
text+='\n# 손상 로드 재현\n\nCommand: pwsh -NoProfile -File tools/probe-saved-data-load.ps1\nInput: exceptions/runActorExceptionsSeed1.dat 정상 복사본 및 entityUUID만 제거한 복사본\nFinal process exit: 0 (characterization completed, NOT recovery PASS)\nLiteral output:\n'+(E/'saved-data-probe.log').read_text(encoding='utf-8-sig')
text+='\n원본 checkpoint SHA256: 970410183549f8c6987cb5be5ad3860d3ac19cdb0623a381528d72c9505e7693\n실제 클라이언트에서 손상 세이브를 열거나 저장한 검사는 아직 없다. 정상 22회 결과로 이 FAIL을 상쇄하지 않는다.\n'
text+='\n# 재개·배포 상태\n\n'+design_result+'\n'
text+='패키징: python tools/package-wayfarer-stability.py --prepare → pwsh -NoProfile -File tools/verify-wayfarer-transaction.ps1 → python tools/package-wayfarer-stability.py.\n'
text+='source ZIP hash는 순환 참조를 피하기 위해 최종 dist/wayfarer-stability/SHA256SUMS.txt에서 확인한다. ZIP에는 이 보고서·diff·원본 JAR/source·수정 JAR·스크립트·진단 로그·소스 manifest가 포함된다.\n'
text+='다음 첫 수정: NarrativeData.get의 load-failed/new-world 경계. 현재 산출물은 정상 Wayfarer 경로 검증용 PARTIAL 개발본이며 손상 데이터 자동 복구 완료본이 아니다.\n'
(E/'VERIFICATION.txt').write_text(text,encoding='utf-8')
print('PASS verification assembled from reopened evidence; design 25 sections / 10 gates; overall PARTIAL; corruption gate FAIL explicitly retained')
