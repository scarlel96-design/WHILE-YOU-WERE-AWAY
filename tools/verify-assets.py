"""Independent offline checks of shipped data, generated NBT, localization and archive contents."""
from pathlib import Path
import json, gzip, struct, re, sys, zipfile
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';checks=0
def check(ok,msg):
    global checks;checks+=1
    if not ok: raise AssertionError(msg)
def readnbt(path):
    raw=gzip.decompress(path.read_bytes());offset=0
    def take(n):
        nonlocal offset
        x=raw[offset:offset+n];offset+=n;return x
    def text():return take(struct.unpack('>H',take(2))[0]).decode('utf-8')
    def val(t):
        if t==3:return struct.unpack('>i',take(4))[0]
        if t==8:return text()
        if t==9:
            subtype=take(1)[0];count=struct.unpack('>i',take(4))[0]
            return [val(subtype) for _ in range(count)]
        if t==10:
            d={}
            while True:
                tag=take(1)[0]
                if tag==0:return d
                key=text();d[key]=val(tag)
        raise AssertionError('unsupported NBT tag '+str(t))
    check(take(1)==b'\x0a','compound root');text();result=val(10)
    check(offset==len(raw),'complete NBT parse');return result
for p in R.rglob('*.json'):
    json.loads(p.read_text(encoding='utf-8'));check(True,'JSON '+str(p))
ko=json.loads((R/'assets/whileaway/lang/ko_kr.json').read_text(encoding='utf-8'))
en=json.loads((R/'assets/whileaway/lang/en_us.json').read_text(encoding='utf-8'))
check(ko.keys()==en.keys(),'language parity')
vanilla=None
for p in (ROOT/'src/main/java').rglob('*.java'):
    for key in re.findall(r'Component\.translatable\("([a-zA-Z0-9_.]+)"',p.read_text(encoding='utf-8')):
        if key.endswith('.'):continue
        if key in ko:
            check(True,'mod language key '+key)
        elif 'whileaway' in key:
            check(False,'missing language key '+key)
        else:
            if vanilla is None:
                resource_jar=next((ROOT/'build/moddev/artifacts').glob('*client-extra-aka-minecraft-resources.jar'))
                with zipfile.ZipFile(resource_jar) as native:vanilla=json.loads(native.read('assets/minecraft/lang/en_us.json'))
            check(key in vanilla,'missing native language key '+key)
sounds=json.loads((R/'assets/whileaway/sounds.json').read_text())
for name,event in sounds.items():
    check(event['subtitle'] in ko,'sound subtitle '+name)
    for sample in event['sounds']:
        p=R/('assets/'+sample['name'].replace(':','/sounds/')+'.ogg')
        check(p.is_file() and p.stat().st_size>4000,'OGG present '+name)
        check(p.read_bytes().startswith(b'OggS'),'OGG magic '+name)
station=readnbt(R/'data/whileaway/structure/abandoned_station.nbt')
check(station['size']==[19,10,19],'station extent')
palette=station['palette'];placed={}
for b in station['blocks']:
    check(all(0<=b['pos'][i]<station['size'][i] for i in range(3)),'block in bounds')
    check(0<=b['state']<len(palette),'palette index')
    placed[tuple(b['pos'])]=palette[b['state']]['Name']
for p,name in [((6,1,9),'station_anchor'),((8,1,6),'signal_relay'),((3,1,6),'warning_note'),((3,1,11),'evacuation_note'),((8,1,13),'personal_note')]:
    check(placed[p]=='whileaway:'+name,'story object '+name)
for p in [(5,1,3),(6,1,3),(5,2,3),(6,2,3)]:check(placed[p]=='minecraft:air','entry is open')
geo=json.loads((R/'assets/whileaway/geo/wayfarer.geo.json').read_text())['minecraft:geometry'][0]
bones={b['name'] for b in geo['bones']}
for b in geo['bones']:check(not b.get('parent') or b['parent'] in bones,'bone parent')
animations=json.loads((R/'assets/whileaway/animations/wayfarer.animation.json').read_text())['animations']
for name,a in animations.items():
    for bone in a['bones']:check(bone in bones,'animation bone '+bone)
check(set(animations)=={'animation.wayfarer.idle','animation.wayfarer.walk','animation.wayfarer.hunt'},'AI animation coverage')
if len(sys.argv)>1:
    with zipfile.ZipFile(sys.argv[1]) as archive:
        for p in R.rglob('*'):
            if p.is_file():check(p.relative_to(R).as_posix() in archive.namelist(),'shipped resource '+str(p))
        check('io/github/whileaway/WhileAway.class' in archive.namelist(),'compiled entry point')
        check(not any('/minecraft/' in n and n.endswith('.ogg') for n in archive.namelist()),'no bundled original soundtrack')
print(f'PASS asset assertions={checks}; station/animation/localization/OGG'+('/JAR' if len(sys.argv)>1 else ''))
