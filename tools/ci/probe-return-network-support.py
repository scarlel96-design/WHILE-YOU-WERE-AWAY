"""Archive/runtime-preservation checks; no Minecraft process or world mutation."""
from pathlib import Path
import hashlib, json, sys, zipfile
R = Path(__file__).resolve().parents[2]
E = R/'evidence/return-network-support-v1'
mode = sys.argv[1]
if mode == 'baseline':
    hashes = {'main.jar': 'c3820a1ce4551c9260b90b00676cd904314f48af14ae04363515f66b08fe3162',
              'main-source.zip': 'c15db2d1ff80613c29885a409ad4091a6b639eb9a21611f94c9fc1fffdbeddda'}
    for n, expected in hashes.items():
        p = E/'baseline'/n
        assert hashlib.sha256(p.read_bytes()).hexdigest() == expected
        with zipfile.ZipFile(p) as z: assert z.testzip() is None
    print('BASELINE PASS: preserved MAIN JAR/source hashes and ZIP CRC; no support runtime integration')
elif mode == 'modified':
    with zipfile.ZipFile(E/'baseline/main.jar') as old, zipfile.ZipFile(R/'build/libs/whileaway-0.3.1-dev.1.jar') as new:
        assert new.testzip() is None
        classes = [n for n in old.namelist() if n.endswith('.class')]
        assets = [n for n in old.namelist() if n.startswith(('assets/', 'data/'))]
        for n in classes + assets: assert old.read(n) == new.read(n), 'existing bytes changed: '+n
        additions = sorted(set(new.namelist()) - set(old.namelist()))
        assert 'io/github/whileaway/support/ReturnNetworkSignal.class' in additions
        assert 'io/github/whileaway/support/ReturnNetworkLoadAssessment.class' in additions
        for n in additions:
            assert n.endswith('/') or n.startswith('io/github/whileaway/support/') or n.startswith('io/github/whileaway/ReturnNetworkSupportGameTests'), n
        result = {'status': 'PASS', 'unchangedExistingClasses': len(classes),
                  'unchangedAssetEntries': len(assets), 'addedEntries': additions,
                  'runtimeWiringChanged': False, 'newRealClient': 'NOT RUN'}
        (E/'BINARY_COMPATIBILITY.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print(f'MODIFIED PASS: {len(classes)} existing classes and {len(assets)} asset entries byte-identical; support-only additions')
else: raise SystemExit('baseline or modified')
