from pathlib import Path
import ctypes,hashlib,json,shutil,subprocess,time
R=Path(__file__).resolve().parents[1];E=R/'evidence/load-guard';W=(R/'../../work/load-guard-client-final/saves').resolve()
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=Path(json.loads((R/'evidence/wayfarer-stability/world-copy-1.json').read_text())['copy'])
def manifest(p):return {f.relative_to(p).as_posix():sha(f) for f in p.rglob('*') if f.is_file()}
before=manifest(source)
cp=(R/'build/load-guard-probe/run.args').read_text(encoding='utf-8-sig').splitlines()[1]
W.mkdir(parents=True,exist_ok=True)
for mode in ('Control','Recover','Blocked','Unsupported'):
    target=W/('load-'+mode)
    if target.exists():continue
    shutil.copytree(source,target)
    assert manifest(target)==before
    if mode!='Control':
        args=E/f'mutate-{mode}.args'
        args.write_text('-cp\n'+cp+'\nio.github.whileaway.LoadGuardProbe\n--mutate\n"'+str(target/'data/whileaway_story.dat').replace('\\','/')+'"\n'+mode+'\n',encoding='utf-8')
        subprocess.run([r'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe','@'+str(args)],cwd=R,check=True)
    (E/f'client-final-before-{mode}.json').write_text(json.dumps({'source':str(source),'target':str(target),'storyHash':sha(target/'data/whileaway_story.dat'),'sourceFiles':before},indent=2))
for mode in ('Control','Recover','RecoverReload','Blocked','Unsupported'):
    log=E/f'client-final/{mode}.log';log.parent.mkdir(exist_ok=True)
    if log.exists():continue
    script="$ErrorActionPreference='Stop'; ./tools/select-test-monitor.ps1 -OutputPath evidence/load-guard/monitor.json; $env:JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot'; $env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path; ./gradlew.bat --offline runLoadGuard"+mode+"; exit $LASTEXITCODE"
    start=time.time()
    with log.open('w',encoding='utf-8') as out: result=subprocess.run(['pwsh.exe','-NoProfile','-Command',script],cwd=R,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    marker=(E/f'client-final/{mode}.txt').read_text();pid=int((E/f'client-final/{mode}.pid').read_text())
    kernel=ctypes.WinDLL('kernel32',use_last_error=True);kernel.OpenProcess.restype=ctypes.c_void_p;kernel.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_uint32];kernel.CloseHandle.argtypes=[ctypes.c_void_p]
    handle=kernel.OpenProcess(0x100000,False,pid)
    stopped=(not handle and ctypes.get_last_error()==87) or (bool(handle) and kernel.WaitForSingleObject(handle,0)==0)
    if handle:kernel.CloseHandle(handle)
    good=result.returncode==0 and 'PASS '+mode in marker and 'FAIL ' not in marker and stopped
    row={'mode':mode,'command':'./gradlew.bat --offline runLoadGuard'+mode,'pid':pid,'processExited':bool(stopped),'exit':result.returncode,'seconds':time.time()-start,'pass':good}
    with (E/'load-client-final-commands.jsonl').open('a') as out:out.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True)
    assert good,mode+' client failed'
    if mode in ('Blocked','Unsupported'):
        previous=json.loads((E/f'client-final-before-{mode}.json').read_text())
        assert sha(W/('load-'+mode)/'data/whileaway_story.dat')==previous['storyHash']
    assert manifest(source)==before,'Original copied-world baseline modified'
print('PASS actual load clients=5; original source world unchanged',flush=True)

