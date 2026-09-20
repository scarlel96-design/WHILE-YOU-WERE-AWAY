from pathlib import Path
import hashlib,importlib.util,json,sys
sys.dont_write_bytecode=True
R=Path(__file__).resolve().parents[1]; E=R/'evidence/return-network-integration'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rows=[json.loads(l) for l in (E/'client-final-commands.jsonl').read_text().splitlines()]
expected=['CutA','CutB','CutC','CutD','CutE','CutF','CutGPre','CutG','Reopen','CopyHold','CopyReopen','CorruptBinding','CorruptShared','Normal','Environment','Nether','End','Chunk','Death2Off','Death4On','Death5Off']
history=rows
failures=[r for r in history if not r['pass'] or r['exit']!=0]
assert all(r['mode']=='Environment' and r['pid']==31596 and r['processExited'] for r in failures), 'unreviewed failed attempt'
assert (E/'failed-environment/Environment.txt').exists() if failures else True
rows=[r for r in history if r not in failures]
assert [r['mode'] for r in rows]==expected, 'incomplete or repeated successful client ledger'
assert len({r['pid'] for r in rows})==len(rows), 'fresh processes required'
identity=None; states={}; proofs=[]
for row in rows:
 m=row['mode'];t=(E/'client'/f'{m}.txt').read_text(encoding='utf-8-sig')
 assert row['exit']==0 and row['pass'] and row['processExited'], row
 assert row['childExit']==0 or m.startswith('Cut') and row['childExit']==-1073740791,row
 assert f'PASS {m} ' in t and 'FAIL ' not in t and 'PASS monitor DISPLAY1' in t and 'all sound categories=0' in t,m
 f=E/'client'/f'{m}.dat';data=nbt.read(f)['data'];assert data['schema']==5 and data['actorSchema']==1
 if m.startswith('Corrupt'):
  assert h(f)==h(E/'client'/f'{m}-input.dat') and 'saveDispatches=3 writeBlocked=true emptyCampaign=false' in t
  proofs.append({'mode':m,'originalHash':h(f),'afterHash':h(f),'blocked':True});continue
 actors=[a for a in data['actors'] if a['storyId']=='npc:yeoul'];assert len(actors)==1
 a=actors[0];key=[a['entityUUID'],a['instanceId'],a['generation']]
 if identity is None:identity=key
 assert key==identity and a['checkpoint']==4,m
 event=data['returnNetwork'];states[m]=event
 assert len(set(event['facts']))==len(event['facts']),m
 if event['checkpoint']>=2:
  p=event['participant'];assert p['entity']==a['entityUUID'] and p['instance']==a['instanceId'] and p['generation']==a['generation'],m
 assert ('npc.shared=return_network_first_response' in a['facts'])==('SHARED_EXPERIENCE' in event['facts']),m
 assert ('npc.routine=check_old_signal' in a['facts'])==('AFTERMATH' in event['facts']),m
 if m in ['Environment','Nether','End','Chunk','Death2Off','Death4On','Death5Off']:assert f'PASS exception {m}' in t,m
 proofs.append({'mode':m,'checkpoint':event['checkpoint'],'hash':h(f),'generation':a['generation'],'childExit':row['childExit']})
cuts=['CutA','CutB','CutC','CutD','CutE','CutF','CutGPre','CutG']
assert [states[m]['checkpoint'] for m in cuts]==[1,2,3,4,5,5,5,6]
for left,right in zip(cuts,cuts[1:]):
 assert set(states[left]['facts'])<=set(states[right]['facts'])
 assert states[left]['instance']==states[right]['instance']
assert 'SHARED_EXPERIENCE' not in states['CutE']['facts'] and 'SHARED_EXPERIENCE' in states['CutF']['facts']
assert states['CutF']==states['CutGPre']
assert states['CutG']==states['Reopen']
assert states['CutC']==states['CopyHold']==states['CopyReopen']
for m in ['Normal','Environment','Nether','End','Chunk','Death2Off','Death4On','Death5Off']:assert states[m]['checkpoint']==6
copy=json.loads((E/'copy-created.json').read_text(encoding='utf-8-sig'));assert copy['sourceStoryHash']==copy['copyStoryHash']
assert h(Path(copy['target'])/'data/whileaway_story.dat')==h(E/'client/CopyReopen.dat')
result={'realClientAttempts':len(history),'failedAttempts':failures,'realClientRuns':len(rows),'freshPids':len(rows),'hardCuts':8,'diskAssertions':'PASS','compoundEvent':'PARTIAL','reason':'separate entity/equipment chunk separation, broader environment variants and presentation usability remain','proofs':proofs}
(E/'CLIENT_VERIFICATION.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'PASS {len(rows)} real client runs; 8 hard cuts; same Yeoul UUID/instance/generation; monotonic facts; independent copy; corrupt overwrite blocked')
