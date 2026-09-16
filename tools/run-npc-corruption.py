from pathlib import Path
import ctypes, hashlib, json, shutil, subprocess, sys, time
R=Path(__file__).resolve().parents[1];E=R/'evidence/npc-path';W=(R/'../../work/npc-corruption/saves').resolve()
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def manifest(p):return {f.relative_to(p).as_posix():sha(f) for f in p.rglob('*') if f.is_file()}
W.mkdir(parents=True,exist_ok=True)
modes=sys.argv[1:] or ['Control','MissingHome','MissingExperience','WrongWork','Recover','RecoverReload']
for mode in modes:
    if (E/'PAUSE_REQUESTED').exists() and mode!='RecoverReload':
        print('PAUSED_AT_SAFE_BOUNDARY before '+mode+'; no new client launched',flush=True)
        sys.exit(0)
    source=(R/'../../work/npc-path/saves/env-MissingHome').resolve();before=None
    if mode!='RecoverReload':
        before=manifest(source);target=W/('corrupt-'+mode)
        if target.exists():raise RuntimeError('Preserve existing fixture before retry: '+str(target))
        shutil.copytree(source,target);assert manifest(target)==before
        (E/('corrupt-'+mode+'-copy.json')).write_text(json.dumps({'source':str(source),'target':str(target),'manifest':before},indent=2))
    if mode!='RecoverReload':
        f=target/'data/whileaway_story.dat'
        shutil.copyfile(f,E/('corrupt-'+mode+'-normal.dat'))
        cmd=['pwsh.exe','-NoProfile','-File','tools/prepare-npc-corruption.ps1','-File',str(f),'-Mode',mode]
        prepared=subprocess.run(cmd,cwd=R,capture_output=True,text=True,creationflags=subprocess.CREATE_NO_WINDOW)
        (E/('corrupt-'+mode+'-fixture.log')).write_text(prepared.stdout+prepared.stderr)
        assert prepared.returncode==0,'fixture preparation failed'
        shutil.copyfile(f,E/('corrupt-'+mode+'-prepared.dat'))
    folder=E/'corrupt-client' ;folder.mkdir(exist_ok=True);log=folder/(mode+'.log')
    if log.exists():raise RuntimeError('Preserve previous attempt before retry: '+mode)
    script="$ErrorActionPreference='Stop'; ./tools/select-test-monitor.ps1 -OutputPath evidence/npc-path/monitor.json; $env:JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot'; $env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path; ./gradlew.bat --offline runNpcCorruption"+mode+"; exit $LASTEXITCODE"
    start=time.time()
    with log.open('w',encoding='utf-8') as out: result=subprocess.run(['pwsh.exe','-NoProfile','-Command',script],cwd=R,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    marker=(folder/(mode+'.txt')).read_text(encoding='utf-8') if (folder/(mode+'.txt')).exists() else ''
    pid=int((folder/(mode+'.pid')).read_text()) if (folder/(mode+'.pid')).exists() else 0
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.OpenProcess.restype=ctypes.c_void_p;k.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_uint32];k.CloseHandle.argtypes=[ctypes.c_void_p]
    handle=k.OpenProcess(0x100000,False,pid);stopped=bool(pid) and ((not handle and ctypes.get_last_error()==87) or (bool(handle) and k.WaitForSingleObject(handle,0)==0))
    if handle:k.CloseHandle(handle)
    good=result.returncode==0 and 'PASS '+mode in marker and 'FAIL ' not in marker and stopped
    row={'mode':mode,'command':'./gradlew.bat --offline runNpcCorruption'+mode,'exit':result.returncode,'pid':pid,'processExited':bool(stopped),'seconds':time.time()-start,'pass':good}
    with (E/'corrupt-commands.jsonl').open('a') as out:out.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True)
    if before is not None:assert manifest(source)==before,'Seed world changed'
    assert good,mode+' failed'
print('PASS requested NPC clients; copied worlds isolated',flush=True)
