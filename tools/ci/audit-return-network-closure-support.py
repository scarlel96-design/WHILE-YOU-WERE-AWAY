"""Conservative support-scope audit. Lexical checks are not runtime certification."""
from pathlib import Path
import argparse, hashlib, json, re, subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = 'b5d26787e4532cffd6379a6ea1e4cc8b6898ebe8'
JAVA = {
    'src/main/java/io/github/whileaway/support/ReturnNetworkSignal.java',
    'src/main/java/io/github/whileaway/support/ReturnNetworkLoadAssessment.java',
    'src/test/java/io/github/whileaway/support/ReturnNetworkSignalTests.java',
    'src/test/java/io/github/whileaway/support/ReturnNetworkLoadTests.java',
    'src/main/java/io/github/whileaway/ReturnNetworkSupportGameTests.java',
}
PREFIXES = ('docs/return-network-support-v1/', 'evidence/return-network-support-v1/',
            'dist/return-network-support-v1/')
TOOLS = {'tools/ci/audit-return-network-closure-support.py',
         'tools/ci/return-network-support.init.gradle',
         'tools/ci/package-return-network-support.py',
         'tools/ci/probe-return-network-support.py'}
FORBIDDEN = {
    'teleport': r'\b(?:teleportTo|teleport|moveTo)\s*\(',
    'force_chunk': r'\b(?:getChunk|getChunkAt|setChunkForced|addRegionTicket)\s*\(',
    'global_scan': r'\b(?:getAllEntities|getAllLevels|getEntitiesOfClass|getBlocks)\s*\(',
    'destroy_block': r'\b(?:destroyBlock|removeBlock)\s*\(',
    'persistence_write': r'\b(?:commitReturnNetwork|setDirty|bindStorage|spawnPrepared|prepareReplacement)\s*\(',
    'wall_clock': r'\b(?:currentTimeMillis|nanoTime|sleep)\s*\(',
    'new_subsystem': r'\b(?:RewardTransaction|ItemTransaction|StructureFallback)\b',
}

def git(*args):
    return subprocess.check_output(['git', '-c', 'core.longpaths=true', *args], cwd=ROOT)

def code_only(text):
    # Keep string literals while removing comments; avoid treating a comment marker in a string as syntax.
    return re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: '' if m[0].startswith(('//', '/*')) else m[0], text)

def scan(text, fixture=False, pure_test=False):
    code = code_only(text)
    if pure_test:
        code = code.replace("import java.io.Serializable;", "")  # reflection-only negative serialization assertion
    issues = [name for name, pattern in FORBIDDEN.items() if re.search(pattern, code)]
    if not fixture and re.search(r'\b(?:net\.minecraft|net\.neoforged|java\.io|java\.nio|setBlock|NarrativeData|StoryActors|StoryStorage)\b', code):
        issues.append('helper_not_pure')
    return issues

def scope_errors(tracked, changed):
    errors = ['EXISTING_FILE_CHANGED '+name for name in sorted(set(changed).intersection(tracked))]
    for name in sorted(changed):
        if name not in JAVA | TOOLS and not name.startswith(PREFIXES):
            errors.append('OUTSIDE_SUPPORT_ALLOWLIST '+name)
    return errors

def self_test():
    samples = ['x.teleportTo(a);', 'world.getChunk(0, 0);', 'x.getAllEntities();',
               'x.destroyBlock(p, false);', 'data.commitReturnNetwork(a,b,c);',
               'System.nanoTime();', 'new ItemTransaction();']
    assert all(scan(s) for s in samples)
    assert not scan('// teleportTo(a)\nlong tick = 0; /* getChunk() */')
    assert scan('world.setBlock(p, b, 3);') == ['helper_not_pure']
    assert not scan('world.setBlock(p, b, 3);', fixture=True)
    protected = ['src/main/java/io/github/whileaway/'+n+'.java' for n in
                 ['NarrativeData', 'ReturnNetworkState', 'StoryStorage', 'StoryActorRecord', 'NpcNavigation']]
    assert len([e for e in scope_errors(protected, protected) if e.startswith('EXISTING_FILE_CHANGED')]) == 5
    assert not scope_errors(protected, JAVA)
    assert scope_errors(protected, {'src/main/java/io/github/whileaway/NewReward.java'})
    assert not scan('import java.io.Serializable;', pure_test=True)
    assert scan('import java.io.Serializable;')
    print('PASS audit self-test: forbidden APIs, helper world writes, comment handling')

def run(output):
    errors = []
    # Every pre-existing tracked file is protected, including schemas/runtime/resources/build/old evidence.
    tracked = git('ls-tree', '-r', '--name-only', BASE).decode().splitlines()
    changed = set(git('diff', '--no-ext-diff', '--name-only', BASE).decode().splitlines())
    changed.update(git('ls-files', '--others', '--exclude-standard').decode().splitlines())
    errors.extend(scope_errors(tracked, changed))
    source_hashes = {}
    for name in sorted(JAVA):
        p = ROOT/name
        if not p.exists():
            errors.append('SUPPORT_FILE_MISSING '+name); continue
        source_hashes[name] = hashlib.sha256(p.read_bytes()).hexdigest()
        for issue in scan(p.read_text(encoding='utf-8-sig'), name.endswith('GameTests.java'), name.startswith('src/test/')):
            errors.append(issue+' '+name)
    result = {'baseline': BASE, 'status': 'STATIC_PASS' if not errors else 'FAIL',
              'protectedExistingFiles': len(tracked), 'sourceSha256': source_hashes,
              'errors': errors,
              'limits': 'Allowlist + normalized baseline hashes + lexical checks; no proof of real chunk unload, human observation or event closure.'}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps(result, indent=2))
    return bool(errors)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--output', type=Path, default=ROOT/'evidence/return-network-support-v1/STATIC_AUDIT.json')
    args = parser.parse_args()
    if args.self_test: self_test()
    else: raise SystemExit(run(args.output))
