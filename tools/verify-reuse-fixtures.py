from pathlib import Path
import hashlib,importlib.util,json,re
R=Path(__file__).resolve().parents[1];E=R/'evidence/reuse'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
proof=[];count=0
last={}
for task,code in re.findall(r'COMMAND ./gradlew.bat --offline (runBoundary\w+)\s+EXIT (\d+)',(E/'runtime-commands.txt').read_text(encoding='utf-8')):last[task]=int(code)
for kind in [4,1,2]:
    modes=['A','B','D','E','Verify'] if kind==4 else ['D','E','Verify'];previous=None;identity=None
    for mode in modes:
        task=f'runBoundary{kind}{mode}';assert last.get(task)==0,(task,'last exit not zero')
        log=(E/(task+'.log')).read_text(encoding='utf-8');marker=(E/(task+'.txt')).read_text(encoding='utf-8')
        assert f'PASS {task}' in marker and 'VERIFIED MUTED before boundary client launch' in log,task
        if mode!='Verify':assert 'FAULT Runtime.halt' in log and 'EXPECTED_FAULT_PROCESS_EXIT=' in log,task
        saved=nbt.read(E/f'snapshots/kind-{kind}-{mode}.dat')['data'];assert saved['schema']==4 and len(saved['players'])==1
        p=saved['players'][0];assert p['complete']==1
        if kind==4:
            r=p['investigation'];bits,cp,pending,notice={'A':(0,0,4,0),'B':(4,1,0,0),'D':(7,3,0,0),'E':(7,4,0,1),'Verify':(7,4,0,1)}[mode]
            assert (p['cityClues'],r['evidence'],r['checkpoint'],r.get('pendingEvidence',0),r['noticeClaimed'])==(bits,bits,cp,pending,notice),(task,p)
            assert r['eventId']=='city_records'
            if mode=='Verify':assert r==previous,'completed investigation changed on reload'
        else:
            r=next(s for s in p['scenes'] if s['kind']==kind)
            assert r['checkpoint']==(3 if mode=='D' else 4)
            assert r['state']==('RESOLVING' if mode=='D' else 'COMPLETED')
            assert r['shown']&87==87,'committed captions/cues missing'
            if identity is None:identity=r['instanceId']
            else:assert identity==r['instanceId'],'scene identity changed across process'
            if mode=='Verify':assert r==previous,'completed scene changed on reload'
        previous=r;count+=1;proof.append(f'PASS {task} disk checkpoint={r["checkpoint"]}; state={r["state"]}')
for version in [2,3]:
    copies=[nbt.read(E/f'nbt-stress/legacy-v{version}-copy-{i}.dat') for i in range(3)]
    assert copies[0]==copies[1]==copies[2];proof.append(f'PASS actual schema {version} identical copies=3')
for original in json.loads((E/'legacy-originals.json').read_text(encoding='utf-8')):
    assert sha(Path(original['source']))==original['sha256'];assert sha(Path(original['copy']))==original['sha256']
proof.append('PASS original legacy story files unchanged')
for line in (E/'RUNTIME_SOURCE.sha256').read_text(encoding='utf-8').splitlines():
    digest,name=line.split('  ',1);assert sha(R/name)==digest
proof.append(f'PASS final boundary snapshots={count}; runtime source unchanged; overall remains PARTIAL')
(E/'FIXTURE_PROOF.txt').write_text('\n'.join(proof)+'\n',encoding='utf-8');print('\n'.join(proof))
