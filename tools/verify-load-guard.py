"""Independently reopen on-disk checkpoints and real closed/copy-world evidence."""
from pathlib import Path
import hashlib, importlib.util, json

R=Path(__file__).resolve().parents[1];E=R/'evidence/load-guard'
spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py')
nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
commands=[json.loads(line) for line in (E/'runtime-commands.jsonl').read_text().splitlines()]
latest={row['task']:row for row in commands}
expected=[f'runActor{mode}{repeat}' for repeat in (1,) for mode in ('A','B','C','D','E','Verify')]
expected += [f'runActorExceptions{mode}{repeat}' for repeat in (1,) for mode in ('Seed','Original','Copy','Reload','CopyReload')]
for task in expected:
    row=latest[task];assert row['pass'] and row['exit']==0 and row['process_exited'],task
assert len({latest[task]['pid'] for task in expected})==11,'Require 11 distinct observed client processes'
crash_snapshots=0;exceptions=[]
for repeat in (1,):
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
for path,digest in [('dist/wayfarer-stability/whileaway-0.3.1-wayfarer-stability.jar','3d3b08beddd7e9fae87c41188c31251e941ee9da9b6697b50bcf96a2fd960766'),
                    ('dist/wayfarer-stability/whileaway-0.3.1-wayfarer-stability-source.zip','21175125bdf2ac451aaabf8aaf51a2cb533f016a39aaf8df740bebbbf61785aa')]:assert sha(R/path)==digest
result={'wayfarer_suite':'PASS','overall_stability':'PARTIAL','distinct_client_processes':11,'crash_snapshots':crash_snapshots,
        'crash_final_verifies':1,'exception_processes':5,'copied_world_pairs':exceptions,
        'scope':'Wayfarer only; no item rewards or persistent next-event dependency token exists in this adapter; not a transaction/NPC/fallback proof.'}

load_rows=[json.loads(line) for line in (E/'load-client-final-commands.jsonl').read_text().splitlines()]
assert len(load_rows)==5 and all(r['pass'] and r['processExited'] and r['exit']==0 for r in load_rows)
assert len({r['pid'] for r in load_rows}|{latest[t]['pid'] for t in expected})==16
for mode in ('Control','Recover','RecoverReload','Blocked','Unsupported'):
    marker=(E/f'client-final/{mode}.txt').read_text()
    assert 'PASS monitor DISPLAY1' in marker and 'all sound categories=0' in marker and 'FAIL ' not in marker
    assert 'PASS '+mode in marker
    if mode in ('Blocked','Unsupported'):
        before=json.loads((E/f'client-final-before-{mode}.json').read_text())
        f=Path(before['target'])/'data/whileaway_story.dat'
        assert sha(f)==before['storyHash']
        assert sha(f.parent/'whileaway-quarantine'/f"{before['storyHash']}.dat")==before['storyHash']
        assert 'actual_server_save_dispatches=3' in marker and 'emptyCampaignReturned=false' in marker
    else:
        a=nbt.read(E/f'client-final/{mode}.dat')['data']['actors']
        assert len(a)==1 and a[0]['generation']==0 and a[0]['checkpoint']==2
        assert 'canonical=1 sameUUID=true generation=0 checkpoint=2 stableTicks=100 integrity=clean' in marker
        if mode=='Recover':
            assert 'RECOVERABLE_CORRUPTION' in marker and 'snapshot.Motion' in marker
            before=json.loads((E/'client-final-before-Recover.json').read_text())
            backup=Path(before['target'])/'data/whileaway-quarantine'/f"{before['storyHash']}.dat"
            assert sha(backup)==before['storyHash']
        else:assert 'outcome=LOADED' in marker
assert 'PASS load guard cases=580 assertions=2161 repeats=20; blocked matrix=19; sourcePreserved=true; actualClient=false' in (E/'probe.log').read_text(encoding='utf-8-sig')
assert 'All 48 required tests passed' in (E/'final-tests.log').read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E/'final-tests.log').read_text(encoding='utf-8-sig')
result.update({'load_guard':'PASS','normal_create':'PASS','blocked_empty_campaign':'PASS','blocked_save_dispatch':'PASS',
    'safe_snapshot_motion_recovery':'PASS','missing_uuid_auto_relink':'NOT IMPLEMENTED; blocked with preserved original',
    'generation_auto_recovery':'NOT IMPLEMENTED; missing/negative blocked',
    'actor_recovery_seal':'PARTIAL','complete_missing_ticket_crash_boundaries':'NOT TESTED',
    'load_clients':5,'wayfarer_clients':11,'total_current_clients':16,'load_probe_cases':580,'load_probe_assertions':2161,
    'scope':'Load fail-closed boundary and bounded Motion recovery. Not a full Actor recovery seal; no NPC expansion.'})
(E/'GATES.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS load guard: 580 cases / 2161 assertions; 5 load clients + 11 Wayfarer regression clients; source and corrupt files preserved')
print('PARTIAL Actor recovery seal and campaign; missing UUID/generation deliberately blocked, missing-ticket crash boundaries pending')
