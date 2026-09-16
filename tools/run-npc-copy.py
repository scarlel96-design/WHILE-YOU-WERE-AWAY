from pathlib import Path
import ctypes,hashlib,json,shutil,subprocess,sys,time
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-world';W=(R/'../../work/npc-copy/saves').resolve()
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def manifest(p):return {f.relative_to(p).as_posix():h(f) for f in p.rglob('*') if f.is_file()}
W.mkdir(parents=True,exist_ok=True);F=E/'copy-client';F.mkdir(exist_ok=True)
modes=sys.argv[1:] or ['Seed','AdvanceA','AdvanceB','ReloadA','ReloadB']
for mode in modes:
    assert mode in ['Seed','AdvanceA','AdvanceB','ReloadA','ReloadB']
    if (E/'PAUSE_REQUESTED').exists() and not mode.startswith('Reload'):
        print('PAUSED_AT_SAFE_BOUNDARY before '+mode,flush=True);sys.exit(0)
    source=(R/'../../work/npc-lifecycle/saves/npc-Seed').resolve()
    if mode=='Seed':
        target=W/'copy-Seed';assert not target.exists();before=manifest(source);shutil.copytree(source,target);assert before==manifest(target)
        (E/'copy-source.json').write_text(json.dumps({'source':str(source),'manifest':before},indent=2))
    if mode=='AdvanceA':
        seed=W/'copy-Seed';before=manifest(seed)
        for name in ['copy-A','copy-B']:
            assert not (W/name).exists();shutil.copytree(seed,W/name);assert before==manifest(W/name)
        (E/'copy-split.json').write_text(json.dumps({'seed':str(seed),'A':str(W/'copy-A'),'B':str(W/'copy-B'),'manifest':before},indent=2))
    protected=[source]+([W/'copy-Seed',W/('copy-B' if mode.endswith('A') else 'copy-A')] if mode!='Seed' else [])
    before={str(p):manifest(p) for p in protected}
    log=F/(mode+'.log');assert not log.exists(),'preserve existing attempts'
    script="$ErrorActionPreference='Stop'; ./tools/select-test-monitor.ps1 -OutputPath evidence/npc-world/monitor.json; $env:JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot'; $env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path; ./gradlew.bat --offline runNpcCopy"+mode+"; exit $LASTEXITCODE"
    start=time.time()
    with log.open('w',encoding='utf-8') as out:result=subprocess.run(['pwsh.exe','-NoProfile','-Command',script],cwd=R,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    marker=(F/(mode+'.txt')).read_text(encoding='utf-8') if (F/(mode+'.txt')).exists() else ''
    pid=int((F/(mode+'.pid')).read_text()) if (F/(mode+'.pid')).exists() else 0
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.OpenProcess.restype=ctypes.c_void_p;k.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_uint32];k.CloseHandle.argtypes=[ctypes.c_void_p]
    handle=k.OpenProcess(0x100000,False,pid);stopped=bool(pid) and ((not handle and ctypes.get_last_error()==87) or (bool(handle) and k.WaitForSingleObject(handle,0)==0))
    if handle:k.CloseHandle(handle)
    after={str(p):manifest(p) for p in protected};preserved=before==after
    (F/(mode+'-isolation.json')).write_text(json.dumps({'before':before,'after':after,'equal':preserved},indent=2))
    good=result.returncode==0 and 'PASS '+mode in marker and 'FAIL ' not in marker and stopped and preserved
    row={'mode':mode,'command':'./gradlew.bat --offline runNpcCopy'+mode,'exit':result.returncode,'pid':pid,'processExited':bool(stopped),'seconds':time.time()-start,'protectedTreesUnchanged':preserved,'pass':good}
    with (E/'copy-commands.jsonl').open('a') as out:out.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True);assert good,mode+' failed'
print('PASS requested copy clients; inactive world trees unchanged',flush=True)
