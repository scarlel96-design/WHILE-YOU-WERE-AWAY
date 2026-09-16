"""Serial isolated real clients; immutable source world; no mock recovery decisions."""
from pathlib import Path
import ctypes, hashlib, json, shutil, subprocess, sys, time
R=Path(__file__).resolve().parents[1];E=R/'evidence/canonical-recovery';W=(R/'../../work/canonical-client-final/saves').resolve()
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=Path(json.loads((R/'evidence/load-guard/client-final-before-Control.json').read_text())['source'])
def manifest(p):return {f.relative_to(p).as_posix():sha(f) for f in p.rglob('*') if f.is_file()}
before=manifest(source);W.mkdir(parents=True,exist_ok=True)
modes=sys.argv[1:] or ['Exact','ExactReload','Ambiguous','Unloaded','NoCandidate','Duplicate']+[m for n in range(1,7) for m in ('Cut'+str(n),'Resume'+str(n))]
for mode in modes:
    world='Exact' if mode=='ExactReload' else 'Cut'+mode[6:] if mode.startswith('Resume') else mode
    target=W/('canonical-'+world)
    if not target.exists():
        copy_source=W/'canonical-Cut6' if mode=='Late' else source
        copy_before=manifest(copy_source)
        shutil.copytree(copy_source,target);assert manifest(target)==copy_before
        if mode=='Late':
            cp=(R/'build/load-guard-probe/run.args').read_text(encoding='utf-8-sig').splitlines()[1]
            # Compile/run offline RegionFile fixture helper against actual Minecraft classes.
            compile_args=E/'late-compile.args';compile_args.write_text('-encoding\nUTF-8\n-cp\n'+cp+'\n-d\nbuild/load-guard-probe\ntools/LateActorFixture.java\n')
            java=Path(r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin')
            subprocess.run([str(java/'javac.exe'),'@'+str(compile_args)],cwd=R,check=True)
            args=E/'late-run.args';args.write_text('-cp\n'+cp+'\nio.github.whileaway.LateActorFixture\n"'+str(target).replace('\\','/')+'"\nevidence/load-guard/client-final/Control.dat\n')
            subprocess.run([str(java/'java.exe'),'@'+str(args)],cwd=R,check=True)
            assert manifest(copy_source)==copy_before
        if mode in ('Exact','Ambiguous','Unloaded','NoCandidate'):
            cp=(R/'build/load-guard-probe/run.args').read_text(encoding='utf-8-sig').splitlines()[1]
            arg=E/('mutate-canonical-'+mode+'.args')
            arg.write_text('-cp\n'+cp+'\nio.github.whileaway.LoadGuardProbe\n--mutate\n"'+str(target/'data/whileaway_story.dat').replace('\\','/')+'"\nBlocked\n')
            subprocess.run([r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe','@'+str(arg)],cwd=R,check=True)
        (E/('canonical-final-before-'+world+'.json')).write_text(json.dumps({'source':str(source),'target':str(target),'storyHash':sha(target/'data/whileaway_story.dat'),'sourceFiles':before},indent=2))
    folder=E/'canonical-final';folder.mkdir(exist_ok=True);log=folder/(mode+'.log')
    if log.exists():raise RuntimeError('Preserve previous attempt before retry: '+mode)
    script="$ErrorActionPreference='Stop'; ./tools/select-test-monitor.ps1 -OutputPath evidence/canonical-recovery/monitor.json; $env:JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot'; $env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path; ./gradlew.bat --offline runCanonical"+mode+"; exit $LASTEXITCODE"
    start=time.time()
    with log.open('w',encoding='utf-8') as out:result=subprocess.run(['pwsh.exe','-NoProfile','-Command',script],cwd=R,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    marker=(folder/(mode+'.txt')).read_text() if (folder/(mode+'.txt')).exists() else ''
    pid=int((folder/(mode+'.pid')).read_text()) if (folder/(mode+'.pid')).exists() else 0
    kernel=ctypes.WinDLL('kernel32',use_last_error=True);kernel.OpenProcess.restype=ctypes.c_void_p;kernel.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_uint32];kernel.CloseHandle.argtypes=[ctypes.c_void_p]
    handle=kernel.OpenProcess(0x100000,False,pid)
    stopped=bool(pid) and ((not handle and ctypes.get_last_error()==87) or (bool(handle) and kernel.WaitForSingleObject(handle,0)==0))
    if handle:kernel.CloseHandle(handle)
    good=result.returncode==0 and 'PASS '+mode in marker and 'FAIL ' not in marker and stopped
    row={'mode':mode,'command':'./gradlew.bat --offline runCanonical'+mode,'exit':result.returncode,'pid':pid,'processExited':bool(stopped),'seconds':time.time()-start,'pass':good}
    with (E/'canonical-final-commands.jsonl').open('a') as out:out.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True)
    assert manifest(source)==before,'Original world changed'
    assert good,mode+' failed'
print('PASS requested canonical clients and original world preservation',flush=True)
