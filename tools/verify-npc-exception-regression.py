from pathlib import Path
import hashlib, importlib.util, json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-exceptions'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rows=[json.loads(s) for s in (E/'regression-commands.jsonl').read_text().splitlines()]
expected=[m for c in 'ABCDE' for m in ('Cut'+c,'Resume'+c)]
assert [r['mode'] for r in rows]==expected
assert len({r['pid'] for r in rows})==10
for row in rows:
 assert row['pass'] and row['exit']==0 and row['processExited'],row
 marker=(E/'regression'/(row['mode']+'.txt')).read_text(encoding='utf-8')
 assert 'PASS monitor DISPLAY1' in marker and 'all sound categories=0' in marker and 'FAIL ' not in marker
for cp,c in enumerate('ABCDE'):
 cut=nbt.read(E/f'regression/Cut{c}.dat')['data'];end=nbt.read(E/f'regression/Resume{c}.dat')['data']
 a=next(x for x in cut['actors'] if x['storyId']=='npc:yeoul');b=next(x for x in end['actors'] if x['storyId']=='npc:yeoul')
 assert cut['schema']==end['schema']==4 and cut['actorSchema']==end['actorSchema']==1
 assert a['checkpoint']==cp and b['checkpoint']==4 and b['state']=='COMPLETED' and b['lifecycleState']=='ACTIVE'
 for key in ('storyId','ownerEventId','spawnRole','entityType','dimension','instanceId','entityUUID','generation'):
  assert a[key]==b[key],(c,key)
 assert b['generation']==0 and not b['temporary'] and 'owner' not in b
 assert set(a['facts'])<=set(b['facts']) or set(a['facts'])-set(b['facts'])=={'npc.work=check_return_light'},c
 facts=b['facts'];assert len(facts)==len(set(facts))
 for f in ('npc.schema=1','npc.dialogue=introduction','npc.arrived','npc.observed=return_light','npc.settled','npc.link=return_light_shared','npc.work=maintain_return_light'):assert f in facts,(c,f)
 assert sum(f.startswith('npc.home=') for f in facts)==1
 assert sum(f.startswith('npc.relationship=') and f.endswith('/shared_return_light') for f in facts)==1
 before=json.loads((E/f'Cut{c}-regression-copy.json').read_text());source=Path(before['source'])
 assert {f.relative_to(source).as_posix():sha(f) for f in source.rglob('*') if f.is_file()}==before['manifest'],'seed changed'
for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
 digest,name=line.split('  ',1);assert sha(R/name)==digest,name
assert 'All 58 required tests passed' in (E/'tests-final.log').read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E/'tests-final.log').read_text(encoding='utf-8-sig')
assert 'PASS load guard cases=580 assertions=2161' in (E/'probe.log').read_text(encoding='utf-8-sig')
result={'npc_abcde':'PASS','realClients':10,'processRestartPairs':5,'gameTests':58,'coreAssertions':176857,'storageCases':580,'storageAssertions':2161,'overall':'PARTIAL'}
(E/'REGRESSION.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS current NPC A/B/C/D/E: 5 actual crash/restart pairs; one canonical resident; generation 0; checkpoint 4; facts/residence/receipts verified on disk')
