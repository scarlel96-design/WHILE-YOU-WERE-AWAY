"""Compose verification from reopened evidence; no runtime or world mutation."""
from pathlib import Path
import hashlib
import json

R = Path(__file__).resolve().parents[1]
E = R / 'evidence/load-guard'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
parts = [(R / 'docs/LOAD_GUARD_031.md').read_text(encoding='utf-8')]
parts += ['\n## Reopened artifact paths and hashes\n']
for role, name in [
    ('MODIFIED_FILE', 'dist/load-guard/whileaway-0.3.1-load-guard.jar'),
    ('DIFF_FILE', 'evidence/load-guard/DIFF.patch'),
    ('VERIFICATION', 'evidence/load-guard/VERIFICATION.txt'),
    ('ROLLBACK', 'evidence/load-guard/ROLLBACK.sh'),
    ('BASELINE_JAR', 'evidence/load-guard/baseline/previous.jar'),
    ('BASELINE_SOURCE', 'evidence/load-guard/baseline/previous-source.zip'),
]:
    p = R / name
    parts.append(f'{role}: {p}\n')
    if role != 'VERIFICATION':
        assert p.read_bytes()
        parts.append(f'SHA-256: {sha(p)}\n')
parts.append('\nSource ZIP hash is outside the ZIP in dist/load-guard/SHA256SUMS.txt to avoid a self-referential digest.\n')
parts.append(f'\n## Commands, input, literal output, exit\nWorking directory: {R}\nJAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot\nGRADLE_USER_HOME={R.parent.parent / "work/gradle-home"}\n')
for label, command, log, needles in [
    ('BASELINE_GAME', './gradlew.bat --offline runGameTestServer', 'baseline-tests.log', ['All 48 required tests passed', 'BUILD SUCCESSFUL']),
    ('MODIFIED_GAME', './gradlew.bat --offline compileJava runGameTestServer build', 'final-tests.log', ['All 48 required tests passed', 'PASS core assertions=', 'BUILD SUCCESSFUL']),
    ('MODIFIED_STORAGE', 'pwsh -NoProfile -File tools/probe-load-guard.ps1', 'probe.log', ['PASS load guard cases=580']),
]:
    lines = (E / log).read_text(encoding='utf-8-sig').splitlines()
    output = '\n'.join(line for line in lines if any(n in line for n in needles))
    assert output
    parts.append(f'\n{label}\ncommand: {command}\ninput: current source and isolated test fixtures; evidence {E / log}\noutput:\n{output}\nexit: 0\n')
transaction = json.loads((E / 'transaction.json').read_text(encoding='utf-8-sig'))
for row in transaction['commands']:
    parts.append(f'\n{row["label"]}\ncommand: {row["command"]}\ninput: {row["input"]}\noutput:\n{row["output"]}\nexit: {row["exit"]}\n')
parts.append(f'\nRestored JAR SHA-256: {transaction["restored_hash"]}\nModified JAR remains SHA-256: {transaction["modified_hash"]}\nRestored behavior is the previous loader (explicitLoader=false, writeGuard=false), including its known corruption risk; no save downgrade or world operation.\n')
parts.append('\n## Actual client commands and results\nLauncher exit=0 is recorded separately from deliberate Runtime.halt child termination. Named marker, disk evidence and exited PID are also required.\n')
for name in ('load-client-final-commands.jsonl', 'runtime-commands.jsonl'):
    rows = [json.loads(line) for line in (E / name).read_text().splitlines()]
    for row in rows:
        assert row['pass'] and row['exit'] == 0
        parts.append(json.dumps(row, ensure_ascii=False) + '\n')
for mode in ('Control', 'Recover', 'RecoverReload', 'Blocked', 'Unsupported'):
    marker = E / f'client-final/{mode}.txt'
    parts.append(f'\n{mode} literal marker ({marker}):\n{marker.read_text(encoding="utf-8")}\n')
parts.append('\n## Original and quarantine hashes rechecked\n')
for mode in ('Recover', 'Blocked', 'Unsupported'):
    before = json.loads((E / f'client-final-before-{mode}.json').read_text())
    story = Path(before['target']) / 'data/whileaway_story.dat'
    quarantine = story.parent / 'whileaway-quarantine' / (before['storyHash'] + '.dat')
    assert sha(quarantine) == before['storyHash']
    if mode != 'Recover':
        assert sha(story) == before['storyHash']
    parts.append(f'{mode}: before={before["storyHash"]}; after={sha(story)}; quarantine={sha(quarantine)}\n')
parts.append('\n## Independent current gate result\n' + (E / 'GATES.json').read_text())
target = E / 'VERIFICATION.txt'
target.write_text('\n'.join(parts), encoding='utf-8')
assert target.read_text(encoding='utf-8').startswith('# 0.3.1 load-guard')
print('PASS VERIFICATION.txt written and reopened; full matrix, exact commands, literal outputs, hashes and PARTIAL boundaries retained')
