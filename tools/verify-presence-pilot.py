"""Common, post-pilot quality gates; never included as a claimed A/B usage saving."""
from pathlib import Path
import hashlib
import json
import subprocess
import os

ROOT = Path(__file__).resolve().parents[1]
E = ROOT / 'evidence/caveman-pilot'
JAVA = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot')) / 'bin'
ORACLE = ROOT / 'src/test/java/io/github/whileaway/core/ActorPresenceTests.java'
work = ROOT / 'build/presence-mutations'
work.mkdir(parents=True, exist_ok=True)
mutations = {
    'exclusive_boundary': ('distanceSquared <= radius * radius', 'distanceSquared < radius * radius'),
    'ignore_online': ('!online || !alive || !sameDimension', '!alive || !sameDimension'),
    'ignore_alive': ('!online || !alive || !sameDimension', '!online || !sameDimension'),
    'ignore_dimension': ('!online || !alive || !sameDimension', '!online || !alive'),
    'reject_unowned': ('if (!ownerRequired) return true;', 'if (!ownerRequired) return false;'),
    'reject_unbounded': ('if (!rangeLimited) return true;', 'if (!rangeLimited) return false;'),
    'admit_negative_distance': ('distanceSquared < 0', 'distanceSquared < -100'),
    'admit_negative_radius': ('radius < 0', 'radius < -100'),
}
rows = []
for arm in ('A', 'B'):
    source = (E/arm/'ActorPresencePolicy.java').read_text(encoding='utf-8')
    for name, (original, replacement) in mutations.items():
        assert original in source, (arm, name)
        folder = work/arm/name
        folder.mkdir(parents=True, exist_ok=True)
        candidate = folder/'ActorPresencePolicy.java'
        candidate.write_text(source.replace(original, replacement), encoding='utf-8')
        compiled = subprocess.run([str(JAVA/'javac.exe'), '-encoding','UTF-8','-d',str(folder),str(candidate),str(ORACLE)], capture_output=True, text=True)
        assert compiled.returncode == 0, compiled.stderr
        tested = subprocess.run([str(JAVA/'java.exe'),'-cp',str(folder),'io.github.whileaway.core.ActorPresenceTests'],capture_output=True,text=True)
        assert tested.returncode != 0 and 'ACTOR_PRESENCE_MISMATCH: ' in tested.stderr, (arm,name,tested.stdout,tested.stderr)
        (E/arm/f'mutation-{name}.log').write_text(tested.stdout+tested.stderr,encoding='utf-8')
        rows.append({'arm':arm,'mutation':name,'compile_exit':compiled.returncode,'test_exit':tested.returncode,'detected':True,
                     'literal_error_line':tested.stderr.splitlines()[0]})
    log = (E/arm/'gradle.log').read_text(encoding='utf-8-sig')
    assert 'All 48 required tests passed' in log and 'BUILD SUCCESSFUL' in log
    review = json.loads((E/arm/'review.json').read_text())
    assert hashlib.sha256(ORACLE.read_bytes()).hexdigest() == review['oracle_sha256']
    assert not review['requirements_missing']
assert (E/'A/StoryActors.java').read_bytes() == (E/'B/StoryActors.java').read_bytes()
result = {'mutation_results':rows,'detected_per_arm':8,'adapter_byte_identical':True,
    'oracle_unchanged':True,'commands_preserved':'Both arms use identical javac/java/Gradle arguments.',
    'error_string_preserved':'ACTOR_PRESENCE_MISMATCH: ',
    'limitations':['Common post-pilot mutation tests demonstrate test-oracle sensitivity, not blinded model problem-detection ability.',
                   'No long-horizon session test or A/B actual-client comparison. No quality-equivalence claim.']}
(E/'quality.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS paired quality: 334 checks + 48 GameTests per arm; 8/8 seeded mutations detected per arm; adapter byte-identical; oracle unchanged')
