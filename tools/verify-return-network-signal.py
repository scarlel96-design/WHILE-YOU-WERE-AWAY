"""Verify new signal evidence and preserve every failed client attempt."""
from pathlib import Path
import hashlib,importlib.util,json,re,subprocess,sys
sys.dont_write_bytecode=True
R=Path(__file__).resolve().parents[1];E=R/'evidence/return-network-signal'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
sp=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(sp);sp.loader.exec_module(nbt)
rows=[json.loads(v) for v in (E/'commands.jsonl').read_text(encoding='utf-8-sig').splitlines()]
failures=[r for r in rows if not r['pass'] or r['exit']!=0]
expected_failures={('Reopen',4),('Reopen',10),('Nether',1),('CutBefore',1)}
assert {(r['mode'],r['attempt']) for r in failures}==expected_failures,failures
for r in failures:
 assert r['exit']==1 and r['processExited'],r
assert 'Signal client gate incomplete: Reopen' in (E/'client-Reopen-4.log').read_text(encoding='utf-8-sig')
assert 'canonical count not one' in (E/'client-Reopen-10.log').read_text(encoding='utf-8-sig')
assert 'Signal client gate incomplete: Nether' in (E/'client-Nether-1.log').read_text(encoding='utf-8-sig')
# Normal-1 predates the final signal source. CutBefore-1 reached the verified boundary
# but its original wrapper rejected the intentional Windows hard-exit code.
final=[r for r in rows if r['pass'] and (r['exit']==0 or (r['mode'],r['attempt'])==('CutBefore',1))
       and (r['mode'],r['attempt'])!=('Normal',1)]
required={'Normal','Reopen','CutBefore','CutGap1','CutGap2','CutLong','CutEnd','CutE','CutF','CutG','Availability','EnvironmentMatrix','Nether','Chunk','Death4Off','ReturnLight'}
assert required.issubset({r['mode'] for r in final}),required-{r['mode'] for r in final}
def directory(r):return E/'client'/f"{r['mode']}-{r['attempt']}"/('ReturnLight' if r['mode']=='ReturnLight' else '')
def disk(r):
 name='ResumeB' if r['mode']=='ReturnLight' else r['mode']
 return nbt.read(directory(r)/(name+'.dat'))['data']
def actor(d):
 a=[a for a in d['actors'] if a['storyId']=='npc:yeoul'];assert len(a)==1;return a[0]
def identity(d):
 a=actor(d);return (a['entityUUID'],a['instanceId'],a['generation'])
for r in final:
 assert all(r[k] for k in ['pass','processExited','sourceUnchanged','muted','rightMonitor']),r
 if r['mode']=='CutBefore' and r['attempt']==1:
  # Preserve the original wrapper failure; adjudicate only with explicit boundary/disk and fresh recovery below.
  assert r['exit']==1 and r['childExit'] is None
  assert 'non-zero exit value -1073740791' in (E/'client-CutBefore-1.log').read_text(encoding='utf-8-sig')
 else:
  assert r['exit']==0 and (r['childExit']==0 or r['mode'].startswith('Cut') and r['childExit']==-1073740791),r
 # Smoke-only diagnostics evolved after the first hard cuts. All production
 # source and assets must remain byte-identical across the accepted runs.
 for s in json.loads((E/f"sources-{r['mode']}-{r['attempt']}.json").read_text(encoding='utf-8-sig')):
  if s['path'].replace('\\','/').endswith('/client/ReturnNetworkSmoke.java'):continue
  assert h(R/s['path']).upper()==s['hash'],(r['mode'],r['attempt'],s['path'])
 d=disk(r);assert d['schema']==5 and d['actorSchema']==1 and actor(d)['generation']==0
 if r['mode']=='ReturnLight':assert 'returnNetwork' not in d and actor(d)['checkpoint']==4;continue
 event=d['returnNetwork'];assert len(event['facts'])==len(set(event['facts']))
 if not r['mode'].startswith('Cut'):assert event['checkpoint']==6,r
for cut in [r for r in final if r['mode'].startswith('Cut')]:
 resumes=[r for r in final if r['mode']=='Reopen' and r['world']==cut['world']]
 assert resumes,cut
 # A later fresh process reopens the same world after a failed diagnostic or
 # an already successful recovery; compare against the latest valid readback.
 resume=max(resumes,key=lambda r:r['attempt'])
 before=disk(cut);after=disk(resume);assert identity(before)==identity(after)
 a=before['returnNetwork'];b=after['returnNetwork'];assert a['instance']==b['instance'] and a['checkpoint']<=b['checkpoint']==6 and set(a['facts'])<=set(b['facts'])
 if cut['mode'] in {'CutBefore','CutGap1','CutGap2','CutLong','CutEnd'}:
  assert a['checkpoint']==4
  old=(directory(cut)/(cut['mode']+'-signal.txt')).read_text();fresh=(directory(resume)/'Reopen-signal.txt').read_text()
  oldIds=set(re.findall(r'PREPARED_RESPONSE.*?session=([^ ]+)',old));newIds=set(re.findall(r'PREPARED_RESPONSE.*?session=([^ ]+)',fresh))
  assert len(oldIds)==len(newIds)==1 and oldIds.isdisjoint(newIds)
normal=next(r for r in final if r['mode']=='Normal');lines=(directory(normal)/'Normal-signal.txt').read_text()
for purpose in ['OLD_PATTERN','RESPONSE']:
 on=[int(re.search(rf'RN_SIGNAL_ON_{purpose}_{i} tick=(\d+)',lines)[1]) for i in range(3)]
 off=[int(re.search(rf'RN_SIGNAL_OFF_{purpose}_{i} tick=(\d+)',lines)[1]) for i in range(1,4)]
 assert [off[i]-on[i] for i in range(3)]==[12,12,36]
 assert [on[i+1]-off[i] for i in range(2)]==[16,16]
assert 'emitted_without_observation cp=2 heldTicks=40' in lines and 'emitted_without_observation cp=4 heldTicks=40' in lines
quiet=next(r for r in final if r['mode']=='Reopen' and r['world']==normal['world']);assert disk(normal)['returnNetwork']==disk(quiet)['returnNetwork'] and identity(disk(normal))==identity(disk(quiet))
q=directory(quiet)/'Reopen-signal.txt';assert not q.exists() or 'RN_SIGNAL_ON_' not in q.read_text()
log=(E/'tests-2.log').read_text(encoding='utf-8-sig');assert 'All 108 required tests passed' in log and 'PASS core assertions=176857' in log and 'BUILD SUCCESSFUL' in log
assert 'RESULT PASS 27/27' in (E/'pure-signal-final.log').read_text() and 'assertions=53' in (E/'pure-load-final.log').read_text()
protected=['NarrativeData.java','StoryStorage.java','ReturnNetworkState.java','ReturnNetworkIntegrity.java','StoryActors.java','StoryActorRecord.java','NpcNavigation.java']
for name in protected:
 p='src/main/java/io/github/whileaway/'+name
 assert subprocess.check_output(['git','diff','2bbef42','--',p],cwd=R)==b'',p
result=dict(status='PARTIAL',productionSignal='CONNECTED',timing=dict(short=12,long=36,gap=16,leadIn=10),finalSourceClientPasses=len(final),attempts=len(rows),failedAttempts=failures,gameTests=108,coreAssertions=176857,signalPureTests=27,loadAssertions=53,saveFormat=5,actorSchema=1,returnNetworkSchema=1,protectedFilesUnchanged=protected,newClientRows=final,visualFrames='captured and reviewed: pulses and gaps visible',humanUnaidedReadability='NOT TESTED',audibleListening='NOT RUN (muted policy)',firstCompoundEvent='PARTIAL pending full scene readability/seal review',nextSubsystem='NOT STARTED',diskHashes=[dict(run=f"{r['mode']}-{r['attempt']}",sha256=h(directory(r)/(('ResumeB' if r['mode']=='ReturnLight' else r['mode'])+'.dat'))) for r in final])
(E/'GATES.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(f'PASS signal integration verification: {len(final)} accepted client runs / {len(rows)} attempts; {len(failures)} retained failed command rows; 108 GameTests; Core176857; exact12/12/36 and16-tick gaps; unique fresh sessions; no completed replay; schema unchanged. Overall PARTIAL.')
