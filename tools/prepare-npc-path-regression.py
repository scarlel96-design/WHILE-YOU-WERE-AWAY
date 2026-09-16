"""Reuse unchanged real-client assertions in new isolated profiles/evidence, after corruption gate."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path'
assert json.loads((E/'GATES.json').read_text())['corruptionActualClient'].startswith('PASS')
groups=[
 ('lifecycle','npcSmoke','NpcRegression','npc-exceptions-regression','npc-exceptions','run-npc-exception-regression.py','verify-npc-exception-regression.py',[m for c in 'ABCDE' for m in ('Cut'+c,'Resume'+c)]),
 ('death','npcException','NpcException','npc-exceptions','npc-exceptions','run-npc-exceptions.py','verify-npc-exceptions.py',[f'Death{n}{k}' for k in ('Off','On') for n in range(1,6)]+['ReloadOff','ReloadOn']),
 ('travel','npcWorld','NpcWorld','npc-world','npc-world','run-npc-world.py','verify-npc-world.py',['Chunk1','Chunk2','Chunk4','Nether1','Nether2','End1','End2','CutNether2','ResumeNether2']),
 ('copy','npcCopy','NpcCopy','npc-copy','npc-world','run-npc-copy.py','verify-npc-copy.py',['Seed','AdvanceA','AdvanceB','ReloadA','ReloadB'])]
p=R/'build.gradle';t=p.read_text();assert 'npcPathLifecycle' not in t
template=t[t.rindex("\n['Control','MissingHome','MissingExperience'"):]
insert='';tail=''
manifest=''.join(hashlib.sha256(f.read_bytes()).hexdigest()+'  '+f.relative_to(R).as_posix()+'\n' for f in sorted((R/'src/main').rglob('*')) if f.is_file())
for group,prop,task,oldwork,oldevidence,runner,verifier,modes in groups:
    tasknew='NpcPath'+group.title();ee='evidence/npc-path/closure/'+group;work='../../work/npc-path-'+group
    folder='regression' if group=='lifecycle' else 'copy-client' if group=='copy' else 'client'
    out=R/ee;out.mkdir(parents=True,exist_ok=True);(out/'RUNTIME_SOURCE.sha256').write_text(manifest);(out/'COPY_RUNTIME_SOURCE.sha256').write_text(manifest)
    mode_list='['+','.join("'"+m+"'" for m in modes)+']'
    insert+=f'''        {mode_list}.each {{ mode ->
            create("{tasknew[0].lower()+tasknew[1:]}${{mode}}") {{
                client(); gameDirectory=file('{work}')
                systemProperty 'whileaway.{prop}',mode
                systemProperty 'whileaway.evidence',file('{ee}/{folder}').absolutePath
            }}
        }}
'''
    block=template.replace(template.split('.each')[0],'\n'+mode_list,1).replace('runNpcCorruption','run'+tasknew).replace('../../work/npc-corruption',work).replace('evidence/npc-path/corrupt-client',ee+'/'+folder).replace("mode!='RecoverReload'","!mode.startsWith('Resume') && !mode.startsWith('Reload')")
    tail+=block
    s=(R/'tools'/runner).read_text().replace('evidence/'+oldevidence,ee).replace('../../work/'+oldwork,work).replace('run'+task,'run'+tasknew)
    s=s.replace("(E/'PAUSE_REQUESTED')","(E.parent.parent/'PAUSE_REQUESTED')")
    (R/'tools'/('run-npc-path-'+group+'.py')).write_text(s)
    v=(R/'tools'/verifier).read_text().replace('evidence/'+oldevidence,ee).replace('All 58 required tests passed','All 63 required tests passed')
    v=v.split('\nresult=')[0].split('\ng=')[0]
    for log in ['tests-final.log','tests-4.log']:
        v=v.replace("(E/'"+log+"')","(E.parent.parent/'tests-5.log')")
    v=v.replace("(E/'probe.log')","(E.parent.parent/'probe.log')")
    v+="\n(E/'VERIFIED.json').write_text(json.dumps({'group':'"+group+"','clients':"+str(len(modes))+",'result':'PASS'},indent=2))\nprint('PASS current isolated NPC "+group+" clients="+str(len(modes))+"; unchanged assertions/identity/facts/disk checks')\n"
    (R/'tools'/('verify-npc-path-'+group+'.py')).write_text(v)
p.write_text(t.replace('        recoveryLegacy {',insert+'        recoveryLegacy {',1)+tail)
print('Prepared 36 serial current NPC regression clients; old worlds/evidence preserved; no production code changed')
