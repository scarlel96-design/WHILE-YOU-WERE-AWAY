"""Serial real-client validation; closed-world copy only; no baseline evidence overwrite."""
from pathlib import Path
import argparse, ctypes, datetime, hashlib, json, os, re, shutil, subprocess, time

R=Path(__file__).resolve().parents[1]
E=R/'evidence/wayfarer-stability'
W=(R/'../../work').resolve()
SAVES=W/'wayfarer-exceptions/saves'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()

def stopped(pid):
    kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    kernel.OpenProcess.argtypes=[ctypes.c_uint32,ctypes.c_int,ctypes.c_uint32]
    kernel.OpenProcess.restype=ctypes.c_void_p
    kernel.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_uint32]
    kernel.CloseHandle.argtypes=[ctypes.c_void_p]
    handle=kernel.OpenProcess(0x100000,False,pid)
    if not handle:
        if ctypes.get_last_error()!=87:raise RuntimeError('Process status unknown')
        return True
    try:return kernel.WaitForSingleObject(handle,0)==0
    finally:kernel.CloseHandle(handle)

def run(task,kind):
    folder=E/kind;folder.mkdir(parents=True,exist_ok=True)
    log=folder/(task+'.log')
    if log.exists():raise RuntimeError('Preserve earlier result before retry: '+str(log))
    start=time.time()
    script="$ErrorActionPreference='Stop'; ./tools/select-test-monitor.ps1 -OutputPath evidence/wayfarer-stability/monitor.json; $env:JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot'; $env:GRADLE_USER_HOME=(Resolve-Path ../../work/gradle-home).Path; ./gradlew.bat --offline "+task+"; exit $LASTEXITCODE"
    with log.open('w',encoding='utf-8') as stream:
        result=subprocess.run(['pwsh.exe','-NoProfile','-Command',script],cwd=R,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    text=log.read_text(encoding='utf-8',errors='replace')
    marker=(folder/(task+'.txt')).read_text(encoding='utf-8') if (folder/(task+'.txt')).exists() else ''
    pid=int((folder/(task+'.pid')).read_text()) if (folder/(task+'.pid')).exists() else None
    row={'task':task,'command':'./gradlew.bat --offline '+task,'exit':result.returncode,'seconds':time.time()-start,'pid':pid,
         'process_exited':pid is not None and stopped(pid),'timestamp':datetime.datetime.now(datetime.timezone.utc).isoformat(),
         'pass':result.returncode==0 and 'BUILD SUCCESSFUL' in text and 'PASS '+task in marker and 'FAIL ' not in marker}
    row['pass']=row['pass'] and row['process_exited'] and 'PASS monitor DISPLAY1' in marker
    with (E/'runtime-commands.jsonl').open('a',encoding='utf-8') as stream:stream.write(json.dumps(row)+'\n')
    print(json.dumps(row),flush=True)
    if not row['pass']:raise RuntimeError('Gate failed: '+task+'; see '+str(log))

def manifest(folder):
    files=list(folder.rglob('*'))
    if folder.is_symlink() or any(p.is_symlink() or p.is_junction() for p in files):raise RuntimeError('Unexpected link in test world')
    return {p.relative_to(folder).as_posix():sha(p) for p in files if p.is_file()}

def world(repeat):
    name=(E/'exceptions'/f'exceptions-world-{repeat}.txt').read_text().strip()
    if not re.fullmatch(r'whileaway-exceptions-[12]-[0-9]+',name):raise RuntimeError('Unexpected world name')
    source=(SAVES/name).resolve();target=(SAVES/(name+'-copy')).resolve()
    if source.parent!=SAVES.resolve() or target.parent!=SAVES.resolve():raise RuntimeError('Copy escaped isolated saves')
    return source,target

def copy_world(repeat):
    source,target=world(repeat)
    if target.exists():raise RuntimeError('Copy target already exists')
    before=manifest(source);shutil.copytree(source,target)
    assert before==manifest(target)==manifest(source)
    (E/f'world-copy-{repeat}.json').write_text(json.dumps({'source':str(source),'copy':str(target),'files':before,'byte_identical':True},indent=2),encoding='utf-8')
    print(f'PASS entire closed world copied repeat={repeat} files={len(before)}',flush=True)

parser=argparse.ArgumentParser()
parser.add_argument('suite',choices=['crash','exceptions'])
parser.add_argument('--repeat',type=int,choices=[1,2],required=True)
parser.add_argument('--start',default=None)
args=parser.parse_args()
repeat=args.repeat
if args.suite=='crash':
    modes=['A','B','C','D','E','Verify']
    if args.start:modes=modes[modes.index(args.start):]
    for mode in modes:run(f'runActor{mode}{repeat}','crash')
else:
    modes=['Seed','Original','Copy','Reload','CopyReload']
    if args.start:modes=modes[modes.index(args.start):]
    for mode in modes:
        if mode=='Original':
            source,target=world(repeat)
            if not target.exists():copy_world(repeat)
        guard=None
        if mode!='Seed':
            source,target=world(repeat)
            untouched=source if mode in ('Copy','CopyReload') else target
            guard=manifest(untouched)
        run(f'runActorExceptions{mode}{repeat}','exceptions')
        if guard is not None:
            assert manifest(untouched)==guard,'Other world changed during '+mode
            with (E/f'world-independent-{repeat}.txt').open('a') as out:out.write('PASS '+mode+' leaves entire other world byte-identical\n')
print('PASS suite '+args.suite+' repeat='+str(repeat),flush=True)
