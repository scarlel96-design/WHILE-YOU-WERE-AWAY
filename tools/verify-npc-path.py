from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path';F=E/'client'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
def actor(path):
    d=nbt.read(path)['data'];assert d['schema']==4 and d['actorSchema']==1
    a=[r for r in d['actors'] if r['storyId']=='npc:yeoul'];assert len(a)==1;return a[0]
rows=[json.loads(x) for x in (E/'client-commands.jsonl').read_text().splitlines()]
expected=['MissingHome','BlockedHome','ConflictHome','WrongLight','CompletedLight','CutPath','ResumePath','ReloadPath','CutLight','ResumeLight','ReloadLight']
assert [r['mode'] for r in rows]==expected and len({r['pid'] for r in rows})==len(rows)
for row in rows:
    m=row['mode'];assert row['pass'] and row['exit']==0 and row['processExited'],row
    text=(F/(m+'.txt')).read_text(encoding='utf-8');assert 'all sound categories=0' in text and 'PASS monitor DISPLAY1' in text
    if m.startswith(('Reload','Resume')): source='Cut'+m[6:]
    else:source=m
    a=actor(F/(source+'-before.dat'))
    if not m.startswith('Reload'):
        b=actor(F/(m+'-blocked.dat'));assert b['checkpoint']==a['checkpoint'] and b['facts']==a['facts']
        assert b['lifecycleState']==('ACTIVE' if m=='CompletedLight' else 'SUSPENDED')
        if source=='CutPath':assert 'residence=UNREACHABLE path=UNREACHABLE' in text and 'heldTicks=100' in text
    else:b=actor(F/(m+'-input.dat'));assert b['checkpoint']==4
    for key in ['entityUUID','instanceId','generation','storyId','ownerEventId','spawnRole','dimension']:assert a[key]==b[key],(m,key)
    assert b['generation']==0
    if not m.startswith('Cut'):
        c=actor(F/(m+'.dat'));assert c['state']=='COMPLETED' and c['checkpoint']==4 and c['lifecycleState']=='ACTIVE'
        for key in ['entityUUID','instanceId','generation','dimension']:assert a[key]==c[key]
        assert set(a['facts'])-set(c['facts'])<= {'npc.work=check_return_light'}
        assert c['facts'].count('npc.link=return_light_shared')==1 and 'npc.work=maintain_return_light' in c['facts']
        if m.startswith('Reload'):assert b['facts']==c['facts']
    if not m.startswith(('Resume','Reload')):
        copy=json.loads((E/(m+'-copy.json')).read_text());p=Path(copy['source']);assert {f.relative_to(p).as_posix():h(f) for f in p.rglob('*') if f.is_file()}==copy['manifest']
# Runtime coverage excludes GameTest fixtures: later isolation fixes changed only QA code.
# Still enumerate every difference; production code/resources must remain byte-identical.
qa_changes=[]
for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
    digest,name=line.split('  ',1)
    if h(R/name)!=digest:
        assert name.endswith('GameTests.java'),name
        qa_changes.append({'file':name,'tested':digest,'current':h(R/name)})
(E/'environment-source-coverage.json').write_text(json.dumps({'productionUnchanged':True,'qaChanges':qa_changes},indent=2))
log=(E/'tests-1.log').read_text(encoding='utf-8-sig');assert 'All 63 required tests passed' in log and 'PASS core assertions=176857' in log
g={'version':'0.3.1-dev.1','work':'npc-path','saveFormat':4,'actorSchema':1,'artRevision':2,'gameTests':63,'coreAssertions':176857,'realEnvironmentClients':11,'environment':'PASS scoped fixture matrix','pathRetry':'PASS bounded retry/actual unreachable/restored','residenceMissing':'PASS','residenceBlocked':'PASS','residenceConflict':'PASS','pathUnreachable':'PASS','pathRestored':'PASS','lightMissing':'PASS','lightWrong':'PASS','lightRestored':'PASS','environmentFreshProcess':'PASS path/light cut-resume-completed-reload','completedResidentDamage':'PASS preserves completion','corruptionActualClient':'NOT TESTED','finalLifecycleRegression':'NOT TESTED','NPC_overall':'PARTIAL','campaign':'PARTIAL','complex':'NOT STARTED','next':'Actual NPC corrupt world clients, then current full NPC regression'}
(E/'GATES.json').write_text(json.dumps(g,indent=2)+'\n')
print('PASS NPC environment: 11 real clients; bounded path failure/restoration; residence/light; path+light fresh processes; unchanged identities/facts; original seed manifests; NPC overall PARTIAL')
