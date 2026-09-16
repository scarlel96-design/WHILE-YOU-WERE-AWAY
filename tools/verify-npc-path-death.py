from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path/closure/death'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def actor(p):
 d=nbt.read(p)['data'];assert d['schema']==4 and d['actorSchema']==1
 return next(a for a in d['actors'] if a['storyId']=='npc:yeoul')
rows=[json.loads(x) for x in (E/'client-commands.jsonl').read_text().splitlines()]
expected=[f'Death{n}{k}' for k in ('Off','On') for n in range(3,6)]+['ReloadOff','ReloadOn']
assert set(r['mode'] for r in rows)==set(expected) and len(rows)==8
assert len({r['pid'] for r in rows})==8
for row in rows:
 assert row['pass'] and row['exit']==0 and row['processExited'],row
 m=row['mode'];text=(E/'client'/(m+'.txt')).read_text(encoding='utf-8');assert 'PASS monitor DISPLAY1' in text and 'all sound categories=0' in text
 end=actor(E/'client'/(m+'.dat'));assert end['checkpoint']==4 and end['state']=='COMPLETED' and end['lifecycleState']=='ACTIVE' and end['generation']==0
 assert end['dimension']=='whileaway:quiet_city'
 if m.startswith('Death'):
  cp=int(m[5])-1;a=actor(E/'client'/(m+'-before.dat'));b=actor(E/'client'/(m+'-away.dat'))
  assert a['checkpoint']==b['checkpoint']==cp
  assert a['facts']==b['facts']
  for key in ('storyId','ownerEventId','spawnRole','entityType','dimension','instanceId','entityUUID','generation'):
   assert a[key]==b[key]==end[key],(m,key)
  assert set(a['facts'])-set(end['facts'])<= {'npc.work=check_return_light'}
  assert 'PASS remote_respawn' in text and ('diamonds=7' if m.endswith('On') else 'diamonds=0') in text
  if cp==2:assert 'actualMovement=true' in text
  copy=json.loads((E/(m+'-copy.json')).read_text());source=Path(copy['source']);assert {p.relative_to(source).as_posix():h(p) for p in source.rglob('*') if p.is_file()}==copy['manifest']
 else:
  old=actor(E/'client'/('Death5'+m[6:]+'.dat'))
  for key in ('entityUUID','instanceId','generation','facts','checkpoint','state','dimension'):assert old[key]==end[key]
for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
 digest,name=line.split('  ',1);assert h(R/name)==digest
assert 'All 63 required tests passed' in (E.parent.parent/'tests-5.log').read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E.parent.parent/'tests-5.log').read_text(encoding='utf-8-sig')
(E/'VERIFIED.json').write_text(json.dumps({'group':'death','clients':8,'result':'PASS'},indent=2))
print('PASS current isolated NPC death clients=8; unchanged assertions/identity/facts/disk checks')
