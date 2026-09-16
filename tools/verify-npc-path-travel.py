from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path/closure/travel';h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
def actor(p):
 d=nbt.read(p)['data'];assert d['schema']==4 and d['actorSchema']==1
 return next(a for a in d['actors'] if a['storyId']=='npc:yeoul')
rows=[json.loads(x) for x in (E/'client-commands.jsonl').read_text().splitlines()]
expected=['Chunk2','Chunk4','Nether2','End2','CutNether2','ResumeNether2']
assert [r['mode'] for r in rows]==expected and len({r['pid'] for r in rows})==6
for row in rows:
 m=row['mode'];assert row['pass'] and row['exit']==0 and row['processExited'],row
 text=(E/'client'/(m+'.txt')).read_text(encoding='utf-8');assert 'all sound categories=0' in text and 'PASS monitor DISPLAY1' in text
 a=actor(E/'client'/((m if not m.startswith('Resume') else 'CutNether2')+'-before.dat'));b=actor(E/'client'/(m+'-away.dat'))
 assert a['checkpoint']==b['checkpoint']==int(m[-1]);assert a['facts']==b['facts']
 for key in ('entityUUID','instanceId','generation','storyId','ownerEventId','spawnRole','dimension'):assert a[key]==b[key],(m,key)
 assert b['generation']==0 and b['dimension']=='whileaway:quiet_city'
 assert b['lifecycleState']==('ACTIVE' if int(m[-1])==4 else 'SUSPENDED')
 if m.startswith('Chunk'):assert 'PASS actual_unload heldTicks=100' in text and 'entitiesLoaded=false allTrackedPositions=true' in text
 else:assert 'wrongDimensionNPC=0' in text
 if not m.startswith('Cut'):
  c=actor(E/'client'/(m+'.dat'));assert c['checkpoint']==4 and c['state']=='COMPLETED' and c['lifecycleState']=='ACTIVE'
  for key in ('entityUUID','instanceId','generation','dimension'):assert a[key]==c[key]
  assert set(a['facts'])-set(c['facts'])<= {'npc.work=check_return_light'}
  assert c['facts'].count('npc.link=return_light_shared')==1
 if m.startswith('Resume'):assert 'PASS fresh_start dimension=Nether checkpoint=2' in text
 else:
  copy=json.loads((E/(m+'-copy.json')).read_text());source=Path(copy['source']);assert {p.relative_to(source).as_posix():h(p) for p in source.rglob('*') if p.is_file()}==copy['manifest']
for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
 digest,name=line.split('  ',1);assert h(R/name)==digest
assert 'All 63 required tests passed' in (E.parent.parent/'tests-5.log').read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E.parent.parent/'tests-5.log').read_text(encoding='utf-8-sig')
(E/'VERIFIED.json').write_text(json.dumps({'group':'travel','clients':6,'result':'PASS'},indent=2))
print('PASS current isolated NPC travel clients=6; unchanged assertions/identity/facts/disk checks')
