from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path';F=E/'corrupt-client';h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
def actor(path):
    d=nbt.read(path)['data'];assert d['schema']==4 and d['actorSchema']==1
    assert len(d['actors'])==1 and d['actors'][0]['storyId']=='npc:yeoul'
    return d['actors'][0]
rows=[json.loads(x) for x in (E/'corrupt-commands.jsonl').read_text().splitlines()]
assert [r['mode'] for r in rows]==['Control','MissingHome','MissingExperience','WrongWork','Recover','RecoverReload'] and len({r['pid'] for r in rows})==6
proof=[]
for row in rows:
    m=row['mode'];assert row['pass'] and row['exit']==0 and row['processExited']
    t=(F/(m+'.txt')).read_text();assert 'PASS monitor DISPLAY1' in t and 'all sound categories=0' in t and 'connectedClient=true liveNPC=1' in t and 'saveDispatches=3' in t
    a=actor(F/(m+'-input.dat'));b=actor(F/(m+'.dat'))
    for key in ['entityUUID','instanceId','generation','checkpoint','facts','dimension']:assert a[key]==b[key],(m,key)
    assert b['checkpoint']==4 and b['generation']==0
    if m=='RecoverReload':
        prior=actor(F/'Recover.dat');assert prior==a
    else:
        assert h(F/(m+'-input.dat'))==h(E/('corrupt-'+m+'-prepared.dat'))
        cp=json.loads((E/('corrupt-'+m+'-copy.json')).read_text());source=Path(cp['source'])
        assert {p.relative_to(source).as_posix():h(p) for p in source.rglob('*') if p.is_file()}==cp['manifest']
        if m in ['MissingHome','MissingExperience','WrongWork','Recover']:
            quarantine=Path(cp['target'])/'data/whileaway-quarantine'/(h(F/(m+'-input.dat'))+'.dat')
            assert h(quarantine)==h(F/(m+'-input.dat'))
            proof.append({'mode':m,'original':h(F/(m+'-input.dat')),'quarantine':h(quarantine),'after':h(F/(m+'.dat'))})
        if m in ['MissingHome','MissingExperience','WrongWork']:
            assert h(F/(m+'-input.dat'))==h(F/(m+'.dat')) and 'writeBlocked=true' in t and 'outcome=CORRUPT' in t
        elif m=='Recover':
            assert 'Motion' not in a['snapshot'] and b['snapshot']['Motion']==[0.0,0.0,0.0] and 'outcome=RECOVERABLE_CORRUPTION' in t
for line in (E/'CORRUPTION_RUNTIME_SOURCE.sha256').read_text().splitlines():
    digest,name=line.split('  ',1);assert h(R/name)==digest,name
assert 'All 63 required tests passed' in (E/'tests-5.log').read_text(encoding='utf-8-sig')
(E/'corruption-hashes.json').write_text(json.dumps(proof,indent=2)+'\n')
g=json.loads((E/'GATES.json').read_text());g.update(corruptionActualClient='PASS control/3 blocked/Motion recovery/fresh reload',corruptionClients=6,realClients=17,originalQuarantine='PASS SHA256',writeGuard='PASS 3 actual save dispatches per connected client',finalLifecycleRegression='NOT TESTED',NPC_overall='PARTIAL',campaign='PARTIAL',complex='NOT STARTED',next='Current full NPC lifecycle regression after environment/corruption implementation')
(E/'GATES.json').write_text(json.dumps(g,indent=2)+'\n');print('PASS NPC corruption: 6 actual connected clients; three corrupt stores fail closed; save-dispatch hashes preserved; Motion recovery and fresh LOADED process; NPC overall PARTIAL pending final regression')
