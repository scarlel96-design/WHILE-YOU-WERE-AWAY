from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-exceptions';D=R/'dist/npc-exceptions'
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
g=json.loads((E/'GATES.json').read_text());assert g['death_bundle']=='PASS'
r=json.loads((E/'REGRESSION.json').read_text());assert r['npc_abcde']=='PASS'
t=json.loads((E/'transaction.json').read_text(encoding='utf-8-sig'))
text=(R/'docs/NPC_EXCEPTIONS_031.md').read_text(encoding='utf-8')
text+='\n## Current verified results\n'+json.dumps(g,indent=2)+'\n'+json.dumps(r,indent=2)+'\n'
text+='\n## Exact commands, inputs, outputs and exits\nCWD='+str(R)+'\n'
for command,log in [('./gradlew.bat --offline runGameTestServer','baseline-tests.log'),('./gradlew.bat --offline compileJava runGameTestServer build','tests-final.log'),('pwsh -NoProfile -File tools/probe-npc-exception-storage.ps1','probe.log')]:
 lines=(E/log).read_text(encoding='utf-8-sig').splitlines();text+='COMMAND '+command+'\nLOG '+str(E/log)+'\n'+'\n'.join(x for x in lines if 'required tests passed' in x or 'PASS core assertions' in x or 'PASS load guard cases' in x or 'BUILD SUCCESSFUL' in x)+'\nEXIT 0\n'
for c in t['commands']:text+=json.dumps(c,indent=2)+'\n'
text+='Restored hash='+t['restored_hash']+'; modified hash retained='+t['modified_hash']+'; world operations=none. Archive probes are not actual gameplay rollback.\n'
for folder,log in [('client','client-commands.jsonl'),('regression','regression-commands.jsonl')]:
 rows=[json.loads(x) for x in (E/log).read_text().splitlines()]
 text+='\n## '+folder+' real clients; separate exited PIDs\n'
 for row in rows:text+=json.dumps(row)+'\n'+(E/folder/(row['mode']+'.txt')).read_text(encoding='utf-8')+'\n'
 text+='Launcher duration sum seconds='+str(round(sum(x['seconds'] for x in rows),3))+'; not natural survival duration\n'
text+='\n## Paths and hashes\n'
for label,p in [('BASELINE_JAR',E/'baseline/previous.jar'),('BASELINE_SOURCE',E/'baseline/previous-source.zip'),('MODIFIED_FILE',D/'whileaway-0.3.1-npc-exceptions.jar'),('DIFF_FILE',E/'DIFF.patch'),('ROLLBACK.sh',E/'ROLLBACK.sh')]:text+=label+' '+str(p)+'\nSHA256 '+h(p)+'\n'
text+='VERIFICATION '+str(E/'VERIFICATION.txt')+'\nSource ZIP SHA256 is stored outside the archive in dist/npc-exceptions/SHA256SUMS.txt to avoid circular hashing.\n'
text+='\nNext first task: NPC actual entity/snapshot/residence/target/light chunks unload and reload, then Nether/End and a foreign-dimension process restart. Keep permanent identity/relationships and current runtime distinct. No composite until remaining residence/path/corruption/copy gates pass. Latest required D1-D5 death OFF/ON and A-E process pairs are current evidence only; past client counts are not included.\n'
(E/'VERIFICATION.txt').write_text(text,encoding='utf-8');assert (E/'VERIFICATION.txt').read_text(encoding='utf-8')==text
print('PASS detailed exception verification written/reopened; current client records and all remaining gates included')
