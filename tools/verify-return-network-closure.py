"""Current MAIN preflight evidence only; never promote this into signal integration/sealing."""
from pathlib import Path
import hashlib, importlib.util, json, subprocess, sys
sys.dont_write_bytecode=True
R=Path(__file__).resolve().parents[1]; E=R/'evidence/return-network-closure'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rows=[json.loads(s) for s in (E/'commands.jsonl').read_text(encoding='utf-8-sig').splitlines()]
assert {r['mode'] for r in rows}=={'Availability','Reopen','Seed','ReturnLight'}
for r in rows:
    assert r['pass'] and r['exit']==0 and r['childExit']==0 and r['processExited'] and r['sourceUnchanged'] and r['muted'] and r['rightMonitor'],r
    for p in json.loads((E/f"sources-{r['mode']}-{r['attempt']}.json").read_text(encoding='utf-8-sig')):
        assert h(R/p['path']).upper()==p['hash'], 'source changed after client run '+p['path']
def read(name):return nbt.read(E/'client'/name)['data']
def actor(d):
    a=[v for v in d['actors'] if v['storyId']=='npc:yeoul'];assert len(a)==1;return a[0]
def identity(d):
    a=actor(d);return [a['entityUUID'],a['instanceId'],a['generation']]
def state(d):return d['returnNetwork']
def facts(d):return set(actor(d)['facts'])
availability=read('Availability.dat');environment=read('EnvironmentMatrix.dat');reopen=read('Reopen.dat')
for d in [availability,environment,reopen]:
    assert d['schema']==5 and d['actorSchema']==1 and state(d)['checkpoint']==6
    assert actor(d)['generation']==0 and actor(d)['checkpoint']==4
    assert len(state(d)['facts'])==len(set(state(d)['facts']))
    assert identity(d)==identity(availability)
    assert 'npc.shared=return_network_first_response' in facts(d)
    assert 'npc.routine=check_old_signal' in facts(d)
assert state(environment)==state(reopen) and facts(environment)==facts(reopen)
one=read('Availability-phase1.dat');two=read('Availability-phase2.dat')
assert state(one)==state(two) and state(one)['checkpoint']==1
assert identity(one)==identity(two)==identity(availability)
assert state(one)['instance']==state(availability)['instance']
at=(E/'client/Availability.txt').read_text()
assert 'npc=true relay=false' in at and 'npc=false relay=true' in at
et=(E/'client/EnvironmentMatrix.txt').read_text()
for marker in ['PASS closed NPC route realPath=UNREACHABLE','PASS path restored','PASS structural LIT loss','PASS EnvironmentMatrix cp=6']:
    assert marker in et,marker
for i in range(8):assert f'PASS environment variant={i} ' in et
collision=float(et.split('COLLISION actual NPC displacementSquared=')[1].splitlines()[0]);assert collision>0
seed=read('Seed.dat');resident=read('ReturnLight/ResumeB.dat')
assert identity(seed)==identity(resident) and facts(seed)==facts(resident)
assert actor(resident)['checkpoint']==4 and actor(resident)['generation']==0
assert 'npc.dialogue=introduction' in facts(resident)
assert any(f.endswith('/first_conversation') for f in facts(resident))
assert any(f.endswith('/shared_return_light') for f in facts(resident))
assert 'npc.work=maintain_return_light' in facts(resident)
# NarrativeData.savePayload omits the optional extension exactly at NOT_STARTED.
assert 'returnNetwork' not in seed and 'returnNetwork' not in resident
log=(E/'tests-final.log').read_text(encoding='utf-8-sig')
assert 'All 102 required tests passed' in log and 'PASS core assertions=176857' in log and 'BUILD SUCCESSFUL' in log
result={'status':'PARTIAL','phaseA':'AUDITED','phaseB':'independent availability and return verified; present-chunk/pending-store transition NOT REPRODUCED','phaseC':'PASS scripted real-client matrix and fresh-process reopen','phaseD':'PASS new world prerequisite completion and fresh-process resident regression','phaseE':'NOT STARTED','phasesFtoI':'NOT STARTED / NOT SEALED','gameTests':102,'coreAssertions':176857,'newClientPasses':6,'newClientAttempts':8,'finalSourceClientPasses':5,'historicalThisMilestonePass':'Availability attempt3 superseded by final-source attempt4','failedFixtureAttempts':['Availability1 (both loaded)','Availability2 (both unloaded)'],'clientRows':rows,'collisionDisplacementSquared':collision,'diskEvidence':[{'file':str(p.relative_to(E)),'sha256':h(p)} for p in sorted((E/'client').rglob('*.dat'))],'campaign':'PARTIAL','version':'0.3.1-dev.1','saveFormat':5,'actorSchema':1,'returnNetworkSchema':1,'nextSubsystem':'NOT STARTED','pause':'usage-aware safe boundary before production signal integration'}
(E/'GATES.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS preflight: independent real availability, environment+reopen, new return-light+reopen, disk identity/facts, 102 GameTests, Core176857; event PARTIAL')

