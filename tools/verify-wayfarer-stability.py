"""Independently reopen on-disk checkpoints and real closed/copy-world evidence."""
from pathlib import Path
import hashlib, importlib.util, json

R=Path(__file__).resolve().parents[1];E=R/'evidence/wayfarer-stability'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py')
nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
commands=[json.loads(line) for line in (E/'runtime-commands.jsonl').read_text().splitlines()]
latest={row['task']:row for row in commands}
expected=[f'runActor{mode}{repeat}' for repeat in (1,2) for mode in ('A','B','C','D','E','Verify')]
expected += [f'runActorExceptions{mode}{repeat}' for repeat in (1,2) for mode in ('Seed','Original','Copy','Reload','CopyReload')]
for task in expected:
    row=latest[task];assert row['pass'] and row['exit']==0 and row['process_exited'],task
assert len({latest[task]['pid'] for task in expected})==22,'Require distinct observed client processes'
crash_snapshots=0;exceptions=[]
for repeat in (1,2):
    instance=None;generation=-1
    for cp,mode in enumerate(('A','B','C','D','E')):
        data=nbt.read(E/f'crash/snapshots/actor-{repeat}-{mode}.dat')['data']
        assert data['schema']==4 and data['actorSchema']==1 and len(data['actors'])==1
        actor=data['actors'][0];assert actor['checkpoint']==cp
        if instance is None:instance=actor['instanceId']
        assert actor['instanceId']==instance and actor['generation']>=generation
        generation=actor['generation']
        player=next(p for p in data['players'] if p['id']==actor['owner'])
        if cp<3:assert player['encounter']==actor['entityUUID'] and not player['complete']
        else:assert 'encounter' not in player and player['complete'] and actor['lifecycleState']=='RETIRED' and 'encounter_complete' in actor['facts']
        if cp>=2:assert 'pursuit_started' in actor['facts']
        if cp==4:assert actor['state']=='COMPLETED' and actor['noticeClaimed']
        marker=(E/f'crash/runActor{mode}{repeat}.txt').read_text()
        assert 'FAULT Runtime.halt after durable story write' in marker
        crash_snapshots+=1
    assert 'completed state stable 300 ticks' in (E/f'crash/runActorVerify{repeat}.txt').read_text()
    seed_marker=(E/f'exceptions/runActorExceptionsSeed{repeat}.txt').read_text()
    for needle in ('PASS death_respawn keepInventory=false','PASS death_respawn keepInventory=true',
                   'all hasChunk=false entitiesLoaded=false; canonical_absent=true','held_ticks=100',
                   'PASS dimension_absence=Nether','PASS dimension_absence=End','PASS return scenario=1','PASS return scenario=2','PASS return scenario=3'):
        assert needle in seed_marker,needle
    states={}
    for mode in ('Seed','Original','Copy','Reload','CopyReload'):
        data=nbt.read(E/f'exceptions/runActorExceptions{mode}{repeat}.dat')['data']
        assert data['schema']==4 and data['actorSchema']==1 and len(data['actors'])==1
        actor=data['actors'][0]
        states[mode]=actor
        player=next(p for p in data['players'] if p['id']==actor['owner'])
        if mode in ('Original','Reload'):
            assert actor['checkpoint']==4 and actor['state']=='COMPLETED' and actor['lifecycleState']=='RETIRED'
            assert player['complete'] and 'encounter' not in player and actor['noticeClaimed']
        else:
            assert actor['checkpoint']==2 and actor['state']=='SUSPENDED' and actor['lifecycleState']=='SUSPENDED'
            assert not player['complete'] and player['encounter']==actor['entityUUID'] and not actor['noticeClaimed']
        for key in ('storyId','instanceId','entityUUID','generation','ownerEventId','dimension'):
            assert actor[key]==states['Seed'][key],(repeat,mode,key)
    proof=json.loads((E/f'world-copy-{repeat}.json').read_text());assert proof['byte_identical'] and 'level.dat' in proof['files'] and 'data/whileaway_story.dat' in proof['files']
    independent=(E/f'world-independent-{repeat}.txt').read_text()
    for mode in ('Original','Copy','Reload','CopyReload'):
        assert f'PASS {mode} leaves entire other world byte-identical' in independent
    for mode in ('Original','Copy','CopyReload'):
        assert 'PASS fresh_process_reconnected same UUID/instance/generation/checkpoint; ACTIVE; canonical_count=1' in (E/f'exceptions/runActorExceptions{mode}{repeat}.txt').read_text()
    exceptions.append({'repeat':repeat,'original_checkpoint':states['Reload']['checkpoint'],'copy_checkpoint':states['CopyReload']['checkpoint'],
                       'generation':states['Seed']['generation'],'same_identity_within_each_world':True,'independent_worlds':True})
for line in (E/'RUNTIME_SOURCE.sha256').read_text().splitlines():
    digest,name=line.split('  ',1);assert sha(R/name)==digest,'runtime source drift '+name
for path,digest in [('dist/campaign-pilot/whileaway-0.3.1-campaign-pilot.jar','9c5f78105b92a1afae46b4d488fc67aff2e0288ca78fd90f84aacb9b32efedf8'),
                    ('dist/campaign-pilot/whileaway-0.3.1-campaign-pilot-source.zip','81b47fcca580ac17102e0135c963bc1b2fdce638544ee5b82571b4b65fdc55ec')]:assert sha(R/path)==digest
result={'wayfarer_suite':'PASS','overall_stability':'PARTIAL','distinct_client_processes':22,'crash_snapshots':crash_snapshots,
        'crash_final_verifies':2,'exception_processes':10,'copied_world_pairs':exceptions,
        'scope':'Wayfarer only; no item rewards or persistent next-event dependency token exists in this adapter; not a transaction/NPC/fallback proof.'}
probe=E/'saved-data-probe.log'
if probe.exists():
    diagnostic=probe.read_text(encoding='utf-8-sig')
    assert 'STORAGE_PROBE case=control expectedActors=1 loadedActors=1 fileUnchanged=true' in diagnostic
    assert 'CHARACTERIZATION_COMPLETE' in diagnostic
    assert 'FAIL FAIL_CLOSED:' in diagnostic
    result['actor_corruption_fail_closed']='FAIL (actual storage-library probe; production fix pending)'
    result['actor_corruption_actual_client']='NOT TESTED'
(E/'GATES.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS Wayfarer current policy: 22 distinct exited clients; 10 crash snapshots + 2 Verify; 2 actual actor-chunk/death/dimension flows; 2 copied-world pairs reopened independently')
print('PARTIAL overall campaign; actor corruption, NPC, composite, transactions and fallback are separate gates')
if probe.exists():print('FAIL Actor missing-UUID load becomes empty NarrativeData through Minecraft storage fallback; source file preserved; actual client recovery NOT TESTED')
