"""Prepare one pinned production client. Do not read launcher accounts or user worlds."""
import hashlib,json,os,re,shutil,urllib.request,zipfile
from pathlib import Path

repo=Path(__file__).resolve().parents[2]
runtime=repo.parent/'v2-01e-installed-runtime'/'minecraft'
profile=repo.parent/'v2-01e-installed-client'
official=Path(os.environ['APPDATA'])/'.minecraft'
version='neoforge-21.1.249'
child=json.loads((runtime/'versions'/version/(version+'.json')).read_text(encoding='utf-8-sig'))
base=json.loads((official/'versions/1.21.1/1.21.1.json').read_text(encoding='utf-8-sig'))
libraries=runtime/'libraries';natives=profile/'natives';natives.mkdir(parents=True,exist_ok=True)
def allowed(item):
    result=not item.get('rules')
    for rule in item.get('rules',[]):
        system=rule.get('os',{})
        if system.get('name','windows')!='windows':continue
        if system.get('arch') and not re.fullmatch(system['arch'],'amd64'):continue
        if system.get('version') and not re.search(system['version'],'10.0'):continue
        if any(rule.get('features',{}).values()):continue
        result=rule['action']=='allow'
    return result
merged={}
for lib in base['libraries']+child['libraries']:
    parts=lib['name'].split(':');merged[':'.join(parts[:2]+parts[3:])]=lib
paths=[]
for lib in merged.values():
    if not allowed(lib):continue
    item=lib.get('downloads',{}).get('artifact')
    if item is None:raise RuntimeError('Missing artifact metadata: '+lib['name'])
    dest=libraries/item['path'];dest.parent.mkdir(parents=True,exist_ok=True)
    if not dest.exists():
        cached=official/'libraries'/item['path']
        if cached.exists():shutil.copyfile(cached,dest)
        else:urllib.request.urlretrieve(item['url'],dest)
    if item.get('sha1'):assert hashlib.sha1(dest.read_bytes()).hexdigest()==item['sha1'],dest
    paths.append(str(dest.resolve()))
    if 'natives-windows' in lib['name']:
        with zipfile.ZipFile(dest) as z:
            for name in z.namelist():
                if name.endswith('.dll'):(natives/Path(name).name).write_bytes(z.read(name))
gamejar=runtime/'versions'/version/(version+'.jar')
shutil.copyfile(official/'versions/1.21.1/1.21.1.jar',gamejar)
assert hashlib.sha1(gamejar.read_bytes()).hexdigest()==base['downloads']['client']['sha1']
paths.append(str(gamejar.resolve()))
mods=profile/'mods';mods.mkdir(parents=True,exist_ok=True)
candidate=repo/'evidence/v2-01e/BASELINE.jar'
assert hashlib.sha256(candidate.read_bytes()).hexdigest()=='3c92f0fb7cf1d7981d3bef1b6cc14787c6eff408d8166cdf87a694c8f6050460'
shutil.copyfile(candidate,mods/'whileaway.jar')
cache=repo.parent/'gradle-home/caches/modules-2/files-2.1/software.bernie.geckolib/geckolib-neoforge-1.21.1/4.9.2'
gecko=next(p for p in cache.rglob('*.jar') if '-sources' not in p.name)
shutil.copyfile(gecko,mods/gecko.name)
assets=repo.parent/'gradle-home/caches/neoformruntime/assets'
values={'auth_player_name':'Dev','version_name':version,'game_directory':str(profile),'assets_root':str(assets),
        'assets_index_name':base['assetIndex']['id'],'auth_uuid':'380df991f603344ca090369bad2a924a',
        'auth_access_token':'0','clientid':'0','auth_xuid':'0','user_type':'legacy','version_type':'release',
        'natives_directory':str(natives),'launcher_name':'V2-01E isolated production smoke','launcher_version':'1',
        'classpath':os.pathsep.join(paths),'classpath_separator':os.pathsep,'library_directory':str(libraries)}
def expand(text):
    for key,value in values.items():text=text.replace('${'+key+'}',value)
    if '${' in text:raise RuntimeError('Unresolved launch argument')
    return text
def arguments(items):
    result=[]
    for item in items:
        if isinstance(item,str):result.append(expand(item))
        elif allowed(item):
            value=item['value'];result.extend(expand(x) for x in (value if isinstance(value,list) else [value]))
    return result
args=['-Xmx3G','--add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
      '-javaagent:'+str(repo/'build/monitor-agent.jar')+'=2680,120']
args+=arguments(base['arguments']['jvm'])+arguments(child['arguments']['jvm'])
args+=[child['mainClass']]+arguments(base['arguments']['game'])+arguments(child['arguments']['game'])
args+=['--width','854','--height','480']
profile.mkdir(exist_ok=True)
(profile/'launch.args').write_text('\n'.join('"'+x.replace('\\','\\\\').replace('"','\\"')+'"' for x in args)+'\n',encoding='utf-8')
options=['narrator:0','onboardAccessibility:false','tutorialStep:none','pauseOnLostFocus:true','lang:ko_kr','renderDistance:8','simulationDistance:5','fullscreen:false']
options+=['soundCategory_'+c+':0.0' for c in ['master','music','record','weather','block','hostile','neutral','player','ambient','voice']]
(profile/'options.txt').write_text('\n'.join(options)+'\n',encoding='utf-8')
summary={'launch_target':'forgeclient','development_classpath':False,'input_helper':False,'version':version,
         'jar_sha256':hashlib.sha256(candidate.read_bytes()).hexdigest(),'mods':[p.name for p in mods.glob('*.jar')],
         'profile':str(profile),'libraries':len(paths),'assets':str(assets),'status':'PREPARED_NOT_LAUNCHED'}
(repo/'evidence/v2-01e/installed-preparation.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print('PINNED_PRODUCTION_CLIENT_PREPARED libraries='+str(len(paths))+'; NOT LAUNCHED')
