"""Independently reopen on-disk checkpoints and real closed/copy-world evidence."""
from pathlib import Path
import hashlib, importlib.util, json

R=Path(__file__).resolve().parents[1];E=R/'evidence/canonical-recovery'
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
        if mode=='Blocked':assert nbt.read(f)['data']['actors'][0]['entityUUID']=='not-a-uuid','real malformed UUID fixture missing'
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
assert 'All 52 required tests passed' in (E/'final-tests.log').read_text(encoding='utf-8-sig')
assert 'PASS core assertions=176857' in (E/'final-tests.log').read_text(encoding='utf-8-sig')

canonical_rows=[json.loads(line) for line in (E/'canonical-final-commands.jsonl').read_text().splitlines()]
canonical={row['mode']:row for row in canonical_rows}
modes=['Exact','ExactReload','Ambiguous','Unloaded','NoCandidate','Duplicate','Late']+[m for n in range(1,7) for m in ('Cut'+str(n),'Resume'+str(n))]
assert len(canonical_rows)==len(modes)==19
for mode in modes:
    row=canonical[mode];assert row['pass'] and row['processExited'] and row['exit']==0,mode
    marker=(E/f'canonical-final/{mode}.txt').read_text()
    assert 'PASS monitor DISPLAY1' in marker and 'all sound categories=0' in marker and 'FAIL ' not in marker
    assert 'PASS '+mode in marker
all_pids={r['pid'] for r in canonical_rows}|{r['pid'] for r in load_rows}|{latest[t]['pid'] for t in expected}
assert len(all_pids)==35
for mode in ('Exact','ExactReload'):
    data=nbt.read(E/f'canonical-final/{mode}.dat')['data'];actor=data['actors'][0]
    initial=nbt.read(R/'evidence/load-guard/client-final/Control.dat')['data']['actors'][0]
    assert len(data['actors'])==1
    for key in ('storyId','instanceId','ownerEventId','generation','entityUUID','checkpoint','facts'):
        assert actor[key]==initial[key],(mode,key)
for mode,classification in [('Ambiguous','AMBIGUOUS_MULTIPLE_CANDIDATES'),('Unloaded','UNLOADED_OR_UNCONFIRMED'),('NoCandidate','NO_CANDIDATE')]:
    marker=(E/f'canonical-final/{mode}.txt').read_text();assert classification in marker and 'realSaveDispatches=3' in marker
    before=json.loads((E/f'canonical-final-before-{mode}.json').read_text());f=Path(before['target'])/'data/whileaway_story.dat'
    assert sha(f)==before['storyHash']
    assert sha(f.parent/'whileaway-quarantine'/f"{before['storyHash']}.dat")==before['storyHash']
recovery_before=json.loads((E/'canonical-final-before-Exact.json').read_text())
q=Path(recovery_before['target'])/'data/whileaway-quarantine'/f"{recovery_before['storyHash']}.dat"
assert sha(q)==recovery_before['storyHash']
boundaries=[]
for n in range(1,7):
    cut=nbt.read(E/f'canonical-final/Cut{n}.dat')['data'];resumed=nbt.read(E/f'canonical-final/Resume{n}.dat')['data']
    a=cut['actors'][0];b=resumed['actors'][0]
    assert len(cut['actors'])==len(resumed['actors'])==1
    assert a['generation']==(0 if n==1 else 1) and b['generation']==1
    for key in ('storyId','instanceId','ownerEventId','checkpoint','facts'):
        assert a[key]==b[key]==initial[key],(n,key)
    if n>=2:assert a['entityUUID']==b['entityUUID'],'reservation UUID drift'
    for d in (cut,resumed):
        r=d['actors'][0];p=next(p for p in d['players'] if p['id']==r['owner'])
        assert not p['complete'] and p['encounter']==r['entityUUID'] and r['checkpoint']==2
    assert a['lifecycleState']==('SUSPENDED' if n==1 else 'PREPARED' if n<=4 else 'ACTIVE')
    assert b['lifecycleState'] in ('ACTIVE','SUSPENDED')
    if b['lifecycleState']=='SUSPENDED':
        assert b['state']=='SUSPENDED' and b['reason'] in ('owner_absent','owner_left_region','owner_offline','owner_dead_or_other_dimension'), 'unexpected suspension'
    # Creative fixture pauses encounter AI; fresh login can suspend before the test returns to the home chunk.
    boundaries.append({'boundary':n,'cutGeneration':a['generation'],'resumeGeneration':b['generation'],'checkpoint':2,'reservationUUIDPreserved':n!=1,'diskVerified':True})
late=nbt.read(E/'canonical-final/Late.dat')['data']['actors'][0]
resume6=nbt.read(E/'canonical-final/Resume6.dat')['data']['actors'][0]
for key in ('storyId','instanceId','generation','entityUUID','checkpoint','facts'):assert late[key]==resume6[key]
assert 'entityCount=0' in (E/'late-cleanup.txt').read_text()
assert 'OBSERVED real_entity_chunk_join stale_generation=0' in (E/'canonical-final/Late.txt').read_text()
assert 'staleLateJoin=true canonical=1 generation=1' in (E/'canonical-final/Late.txt').read_text()
assert 'writeBlocked=true originalPreserved=true AI_paused=true' in (E/'canonical-final/Duplicate.txt').read_text()
assert sha(R/'dist/load-guard/whileaway-0.3.1-load-guard.jar')=='a66cf05706329faab022af12d9ab016dc61b8e719724858f70c3449f5ae8ad03'
assert sha(R/'dist/load-guard/whileaway-0.3.1-load-guard-source.zip')=='c5d0161e2f436dcd8cb6c10e4654c84656e5a91cf1b96991c19907e8aa1f38b0'
result.update({'load_guard':'PASS','canonical_suite':'PASS','actor_recovery_current_adapter':'PASS',
    'overall_stability':'PARTIAL','npc_stage':'NOT STARTED','generation_boundaries':boundaries,
    'canonical_clients':19,'load_clients':5,'wayfarer_clients':11,'total_current_clients':35,
    'load_probe_cases':580,'load_probe_assertions':2161,'game_tests':52,'original_game_tests':48,
    'scope':'Shipped Wayfarer identity recovery + current generic Actor primitives. Not NPC campaign, transactions, fallback, natural survival or arbitrary corrupt-field auto-repair.'})
(E/'GATES.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS current canonical recovery: 19 canonical clients + 5 load clients + 11 Wayfarer clients = 35 distinct processes; six crash/restart boundaries; current disk and source hashes')
print('PASS 52 GameTests (48 unchanged + 4 new), 176857 core assertions, 580 storage cases / 2161 assertions; campaign PARTIAL; NPC NOT STARTED')
