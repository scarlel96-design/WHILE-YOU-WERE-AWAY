from pathlib import Path
import importlib.util,json,hashlib
R=Path(__file__).resolve().parents[1];E=R/'evidence/stability';S=(R/'../../work/recovery-smoke/saves').resolve()
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
proof=[]
for n in (1,2):
    name=(E/f'world-{n}.txt').read_text(encoding='utf-8-sig').strip()
    assert '/' not in name and '\\' not in name and name.startswith(f'whileaway-recovery-{n}-')
    original=nbt.read(S/name/'data/whileaway_story.dat')['data']
    copied=nbt.read(S/(name+'-copy')/'data/whileaway_story.dat')['data']
    a=original['players'][0];b=copied['players'][0]
    assert original['schema']==copied['schema']==4
    assert a['cityClues']==1 and b['cityClues']==7
    assert a['cityShockTriggered']==b['cityShockTriggered']==1
    for p in (a,b):assert p['scenes'][0]['state']=='COMPLETED' and p['scenes'][0]['checkpoint']==4
    line=f'PASS independent original/copy cityClues: 1 / 7; pair={n}; both schema=4 and completed checkpoint=4'
    proof.append(line)
    if n==2:
        (E/'copy-isolation-2.txt').write_text(line+'\nByte-hash-at-copy-close proof was not captured for pair 2: original had already resumed. This assertion compares both independently completed saves; Resume2 also checked original cityClues=1 before advancing.\n',encoding='utf-8')
legacy=(E/'legacy-world.txt').read_text(encoding='utf-8-sig').strip()
old=json.loads((E/'legacy-before.json').read_text(encoding='utf-8-sig'))['data']
new=nbt.read(S/legacy/'data/whileaway_story.dat')['data']
assert old['schema']==2 and new['schema']==4
before=old['players'][0];after=new['players'][0]
for key in ['clues','cityClues','cityVisited','station','returnPosition','returnYaw','returnPitch','complete']:
    assert before[key]==after[key],key
assert after['cityShockTriggered']==0
source=Path((E/'legacy-source-path.txt').read_text(encoding='utf-8-sig').strip())
rows=json.loads((E/'legacy-source-manifest.json').read_text(encoding='utf-8-sig'))
for row in rows:assert hashlib.sha256((source/row['path']).read_bytes()).hexdigest().upper()==row['sha256']
proof.append(f'PASS actual schema 2 -> 4 copy migration; key facts preserved; all {len(rows)} original save files byte-identical')
(E/'FIXTURE_PROOF.txt').write_text('\n'.join(proof)+'\n',encoding='utf-8')
print('\n'.join(proof))
