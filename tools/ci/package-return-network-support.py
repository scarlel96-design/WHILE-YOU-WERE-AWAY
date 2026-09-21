"""Immutable support source bundle, not a release JAR or save downgrade."""
from pathlib import Path
import difflib, hashlib, json, shutil, subprocess, sys, zipfile
R = Path(__file__).resolve().parents[2]; E = R/'evidence/return-network-support-v1'
D = R/'dist/return-network-support-v1'; BASE = 'b5d26787e4532cffd6379a6ea1e4cc8b6898ebe8'
bundle = D/'whileaway-gpt-support-return-network-closure-v1.zip'
h = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

if sys.argv[1] == 'prepare':
    audit=json.loads((E/'STATIC_AUDIT.json').read_text())
    assert audit['status'] == 'STATIC_PASS'
    assert all(h(R/p)==value for p,value in audit['sourceSha256'].items()), 'stale source audit'
    assert json.loads((E/'BINARY_COMPATIBILITY.json').read_text())['runtimeWiringChanged'] is False
    diff = ''
    sources = sorted([*R.glob('src/main/java/io/github/whileaway/support/*.java'),
                      *R.glob('src/test/java/io/github/whileaway/support/*.java'),
                      R/'src/main/java/io/github/whileaway/ReturnNetworkSupportGameTests.java'])
    for p in sources:
        diff += ''.join(difflib.unified_diff([], p.read_text(encoding='utf-8').splitlines(True),
                        fromfile='/dev/null', tofile='b/'+p.relative_to(R).as_posix()))
    (E/'DIFF.patch').write_text(diff, encoding='utf-8', newline='\n')
    # The rollback operates only on a disposable support ZIP copy; never src, JAR or saves.
    script = '''#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:?Pass a disposable support ZIP copy}"
[[ "$TARGET" == *.zip && -f "$TARGET" && ! -L "$TARGET" ]] || exit 2
ROOT="$(cd -- "$HERE/../.." && pwd)"
ALLOWED="$(realpath -- "$ROOT/build/return-network-support-rollback")"
TARGET="$(realpath -- "$TARGET")"
[[ "$TARGET" == "$ALLOWED/"* ]] || exit 5
BASE="$HERE/baseline/main-source.zip"
EXPECTED="$(cat "$HERE/BUNDLE.sha256")"
[[ "$(sha256sum "$BASE" | cut -d' ' -f1)" == "c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda" ]] || exit 3
[[ "$(sha256sum "$TARGET" | cut -d' ' -f1)" == "$EXPECTED" ]] || exit 4
cp -- "$BASE" "$TARGET"
echo 'ROLLBACK PASS: disposable support ZIP restored to MAIN source bundle; production source, JAR and worlds untouched'
'''
    (E/'ROLLBACK.sh').write_text(script, encoding='utf-8', newline='\n')
    print('PREPARE PASS: DIFF and executable rollback content created')
elif sys.argv[1] == 'pack':
    assert not bundle.exists(), 'never overwrite a published support artifact'
    assert (E/'VERIFICATION.txt').stat().st_size > 1000
    D.mkdir(parents=True, exist_ok=True)
    additions = [*R.glob('src/main/java/io/github/whileaway/support/*.java'),
                 *R.glob('src/test/java/io/github/whileaway/support/*.java'),
                 R/'src/main/java/io/github/whileaway/ReturnNetworkSupportGameTests.java',
                 *R.glob('tools/ci/*return-network*'), *R.glob('docs/return-network-support-v1/*')]
    # Final archive hash/receipts are sidecars, not stale preflight self-hashes.
    additions += [p for p in E.rglob('*') if p.is_file() and 'baseline' not in p.relative_to(E).parts
                  and p.name not in {'BUNDLE.sha256', 'ARTIFACTS.json', 'SOURCE_MANIFEST.sha256', 'POST_PACKAGE.json', 'UPLOAD.json'}]
    additions = sorted(set(additions))
    manifest = E/'SOURCE_MANIFEST.sha256'
    manifest.write_text(''.join(h(p)+'  '+p.relative_to(R).as_posix()+'\n' for p in additions), encoding='utf-8')
    additions.append(manifest)
    # Baseline files are copied byte-for-byte, then only new support entries are added.
    with zipfile.ZipFile(E/'baseline/main-source.zip') as old, zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as z:
        for info in old.infolist(): z.writestr(info, old.read(info.filename))
        for p in additions:
            name='while-you-were-away/'+p.relative_to(R).as_posix()
            assert name not in old.namelist(), 'support must not overwrite baseline entries: '+name
            info=zipfile.ZipInfo(name);info.create_system=3
            info.external_attr=(0o100755 if p.name=='ROLLBACK.sh' else 0o100644)<<16
            z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
    with zipfile.ZipFile(bundle) as z, zipfile.ZipFile(E/'baseline/main-source.zip') as old:
        assert z.testzip() is None
        for name in old.namelist(): assert z.read(name)==old.read(name)
        for p in additions: assert z.read('while-you-were-away/'+p.relative_to(R).as_posix())==p.read_bytes()
    (E/'BUNDLE.sha256').write_text(h(bundle)+'\n',encoding='ascii')
    target=R/'build/return-network-support-rollback/test.zip';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(bundle,target)
    print('PACK PASS: baseline entries preserved; support source/CRC verified; sha256='+h(bundle))
elif sys.argv[1] == 'verify':
    restored=R/'build/return-network-support-rollback/test.zip'
    assert h(restored)==h(E/'baseline/main-source.zip')
    assert h(bundle)==(E/'BUNDLE.sha256').read_text().strip()
    files=[bundle,E/'DIFF.patch',E/'VERIFICATION.txt',E/'ROLLBACK.sh']
    records=[{'path':str(p),'bytes':len(p.read_bytes()),'sha256':h(p)} for p in files]
    (E/'ARTIFACTS.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
    print(json.dumps(records,indent=2));print('VERIFY PASS: all artifacts reopened; rollback copy restored; modified bundle retained')
else: raise SystemExit('prepare, pack or verify')
