from pathlib import Path
import importlib.util,hashlib,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/lifecycle'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
count=0
for repeat in (1,2):
    identity=None;generation=-1
    for cp,mode in enumerate(('A','B','C','D','E')):
        task=f'runActor{mode}{repeat}'
        assert f'PASS {task}' in (E/f'{task}.txt').read_text(encoding='utf-8')
        log=(E/f'{task}.log').read_text(encoding='utf-8')
        assert 'BUILD SUCCESSFUL' in log and 'VERIFIED MUTED before actor client launch' in log and 'EXPECTED_FAULT_PROCESS_EXIT=' in log
        root=nbt.read(E/f'snapshots/actor-{repeat}-{mode}.dat')['data']
        assert root['schema']==4 and root['actorSchema']==1
        actors=root['actors'];assert len(actors)==1
        actor=actors[0];assert actor['ownerEventId']=='wayfarer_first' and actor['spawnRole']=='pursuer'
        assert actor['checkpoint']==cp
        if identity is None:identity=actor['instanceId']
        assert actor['instanceId']==identity and actor['generation']>=generation;generation=actor['generation']
        p=next(p for p in root['players'] if p['id']==actor['owner'])
        if cp<3:assert p['encounter']==actor['entityUUID'] and not p['complete']
        else:assert 'encounter' not in p and p['complete'] and actor['lifecycleState']=='RETIRED' and 'encounter_complete' in actor['facts']
        if cp>=2:assert 'pursuit_started' in actor['facts'] and actor['snapshot']['Hunting']
        if cp==4:assert actor['state']=='COMPLETED' and actor['noticeClaimed']
        count+=1
    task=f'runActorVerify{repeat}'
    assert f'PASS {task} completed state stable 300 ticks' in (E/f'{task}.txt').read_text(encoding='utf-8')
    assert 'BUILD SUCCESSFUL' in (E/f'{task}.log').read_text(encoding='utf-8')
# Frozen runtime source equality, before and after the twelve JVMs.
for line in (E/'RUNTIME_SOURCE.sha256').read_text(encoding='utf-8').splitlines():
    digest,name=line.split('  ',1);assert hashlib.sha256((R/name).read_bytes()).hexdigest()==digest,name
# Read-only check of previously tested original legacy story files.
legacy=json.loads((R/'evidence/reuse/legacy-originals.json').read_text(encoding='utf-8-sig'))
for row in legacy:
    assert hashlib.sha256(Path(row['source']).read_bytes()).hexdigest()==row['sha256']
# Retained baseline archives have not been republished in place.
for name,digest in [('whileaway-0.3.1-stability-work.jar','9062d03914ff6dc8211e77342d785734c6263bd3af5bfb2fa5ce344a50c580d6'),('whileaway-0.3.1-stability-work-source.zip','9f0f464e680ce2cfdde8751777411c1bc264645ed6f70e6d195b063b7ae4e427')]:
    assert hashlib.sha256((R/'dist/stability-work'/name).read_bytes()).hexdigest()==digest
result=f'PASS actor boundary snapshots={count}; final client verifications=2\nPASS frozen runtime source; original legacy story files=2 unchanged; baseline archives=2 unchanged\n'
(E/'FIXTURE_PROOF.txt').write_text(result,encoding='utf-8');print(result)
