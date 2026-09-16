from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path/closure/copy';F=E/'copy-client'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
def actor(path):
    d=nbt.read(path)['data'];assert d['schema']==4 and d['actorSchema']==1
    a=[r for r in d['actors'] if r['storyId']=='npc:yeoul'];assert len(a)==1;return a[0]
rows=[json.loads(x) for x in (E/'copy-commands.jsonl').read_text().splitlines()]
assert [r['mode'] for r in rows]==['Seed','AdvanceA','AdvanceB','ReloadA','ReloadB']
assert len({r['pid'] for r in rows})==5
seed=actor(F/'Seed.dat')
assert seed['checkpoint']==1 and seed['generation']==0
for row in rows:
    mode=row['mode'];assert row['pass'] and row['exit']==0 and row['processExited'] and row['protectedTreesUnchanged']
    text=(F/(mode+'.txt')).read_text();assert 'PASS monitor DISPLAY1' in text and 'all sound categories=0' in text
    proof=json.loads((F/(mode+'-isolation.json')).read_text());assert proof['equal'] and proof['before']==proof['after']
    a=actor(F/(mode+'.dat'))
    for key in ['storyId','ownerEventId','instanceId','spawnRole','entityUUID','generation','dimension']:assert a[key]==seed[key],(mode,key)
    assert {f for f in a['facts'] if f.startswith('npc.home=')}=={f for f in seed['facts'] if f.startswith('npc.home=')}
    if mode=='Seed':continue
    expected=4 if mode.endswith('A') else 2;assert a['checkpoint']==expected
    if expected==4:assert a['state']=='COMPLETED' and a['lifecycleState']=='ACTIVE' and 'npc.work=maintain_return_light' in a['facts'] and a['facts'].count('npc.link=return_light_shared')==1
    else:assert a['state']!='COMPLETED' and a['lifecycleState']=='SUSPENDED' and 'npc.work=check_return_light' in a['facts'] and 'npc.link=return_light_shared' not in a['facts']
    if mode.startswith('Reload'):
        prev=actor(F/('Advance'+mode[-1]+'.dat'))
        for key in ['facts','checkpoint','state','entityUUID','instanceId','generation','dimension']:assert prev[key]==a[key],(mode,key)
for line in (E/'COPY_RUNTIME_SOURCE.sha256').read_text().splitlines():
    digest,name=line.split('  ',1);assert h(R/name)==digest
(E/'VERIFIED.json').write_text(json.dumps({'group':'copy','clients':5,'result':'PASS'},indent=2))
print('PASS current isolated NPC copy clients=5; unchanged assertions/identity/facts/disk checks')
