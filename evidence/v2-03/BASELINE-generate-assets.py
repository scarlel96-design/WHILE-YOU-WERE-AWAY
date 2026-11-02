"""Deterministic, code-native structure/models plus original synthesized prototype audio.
No downloaded music, textures, or copied Minecraft assets. Python 3.12; numpy + soundfile.
"""
from pathlib import Path
import argparse, gzip, json, struct, hashlib, math

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
def write_json(path, obj):
    p=RES/path; p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Minimal big-endian NBT encoder. Tags are (type, payload) pairs.
def string(v):
    b=v.encode('utf-8'); return struct.pack('>H',len(b))+b
def payload(tag,v):
    if tag==3:return struct.pack('>i',v)
    if tag==8:return string(v)
    if tag==9:
        subtype,items=v
        return bytes([subtype])+struct.pack('>i',len(items))+b''.join(payload(subtype,item) for item in items)
    if tag==10:return b''.join(bytes([t])+string(k)+payload(t,x) for k,(t,x) in v.items())+b'\0'
    raise ValueError(tag)
def nbt(path,obj):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_bytes(gzip.compress(b'\x0a\x00\x00'+payload(10,obj),mtime=0))

def structure():
    blocks={}; palette=[]; palette_index={}
    def setb(x,y,z,name,props=None,extra=None):
        key=(name,tuple(sorted((props or {}).items())))
        if key not in palette_index:
            p={'Name':(8,name)}
            if props:p['Properties']=(10,{k:(8,v) for k,v in props.items()})
            palette_index[key]=len(palette);palette.append(p)
        b={'pos':(9,(3,[x,y,z])),'state':(3,palette_index[key])}
        if extra:b['nbt']=(10,extra)
        blocks[x,y,z]=b
    for x in range(19):
        for z in range(19):
            setb(x,0,z,'minecraft:stone_bricks' if (x+z)%7 else 'minecraft:mossy_stone_bricks')
            for y in range(1,9):setb(x,y,z,'minecraft:air')
    for x in range(2,11):
        for z in range(3,16):
            for y in range(1,6):
                if x in (2,10) or z in (3,15):setb(x,y,z,'minecraft:deepslate_bricks')
            setb(x,6,z,'minecraft:dark_oak_planks')
    for x in (5,6):
        for y in (1,2,3):setb(x,y,3,'minecraft:air')
    for z in (6,7,11,12):
        for y in (3,4):setb(10,y,z,'minecraft:iron_bars')
    for z in range(19):
        setb(14,1,z,'minecraft:rail',{'shape':'north_south','waterlogged':'false'})
        setb(16,1,z,'minecraft:rail',{'shape':'north_south','waterlogged':'false'})
    for z in (2,9,16):
        for y in range(1,6):setb(12,y,z,'minecraft:stripped_dark_oak_log',{'axis':'y'})
    for x in range(11,19):
        for z in range(1,18):setb(x,6,z,'minecraft:dark_oak_slab',{'type':'bottom','waterlogged':'false'})
    for x,z in ((4,5),(8,13)):
        setb(x,5,z,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
    setb(6,1,9,'whileaway:station_anchor',extra={'id':(8,'whileaway:station_anchor')})
    setb(3,1,6,'whileaway:warning_note')
    setb(3,1,11,'whileaway:evacuation_note')
    setb(8,1,13,'whileaway:personal_note')
    setb(8,1,6,'whileaway:signal_relay')
    setb(7,1,13,'minecraft:chest',{'facing':'north','type':'single','waterlogged':'false'},
         {'id':(8,'minecraft:chest'),'LootTable':(8,'whileaway:chests/station')})
    for x in (4,5,6):setb(x,1,12,'minecraft:spruce_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
    root={'DataVersion':(3,3955),'size':(9,(3,[19,10,19])),'palette':(9,(10,palette)),
          'blocks':(9,(10,list(blocks.values()))),'entities':(9,(10,[]))}
    nbt(Path('data/whileaway/structure/abandoned_station.nbt'),root)
    empty={'DataVersion':(3,3955),'size':(9,(3,[24,12,24])),'palette':(9,(10,[{'Name':(8,'minecraft:air')}])),
           'blocks':(9,(10,[])),'entities':(9,(10,[]))}
    nbt(Path('data/whileaway/structure/empty.nbt'),empty)
    write_json(Path('data/whileaway/worldgen/template_pool/station.json'),{
        'name':'whileaway:station','fallback':'minecraft:empty','elements':[{'weight':1,'element':{
            'element_type':'minecraft:single_pool_element','location':'whileaway:abandoned_station',
            'processors':'minecraft:empty','projection':'rigid'}}]})
    write_json(Path('data/whileaway/worldgen/structure/abandoned_station.json'),{
        'type':'minecraft:jigsaw','biomes':'#whileaway:has_station','step':'surface_structures',
        'spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'whileaway:station','size':1,
        'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG',
        'max_distance_from_center':80,'use_expansion_hack':False})
    write_json(Path('data/whileaway/worldgen/structure_set/stations.json'),{
        'structures':[{'structure':'whileaway:abandoned_station','weight':1}],
        'placement':{'type':'minecraft:random_spread','spacing':20,'separation':8,'salt':74192361}})
    write_json(Path('data/whileaway/tags/worldgen/structure/stations.json'),{'replace':False,'values':['whileaway:abandoned_station']})
    write_json(Path('data/whileaway/tags/worldgen/biome/has_station.json'),{'replace':False,'values':[
        'minecraft:plains','minecraft:sunflower_plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga','minecraft:meadow']})
    write_json(Path('data/whileaway/loot_table/chests/station.json'),{'type':'minecraft:chest','pools':[
        {'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:redstone','functions':[{'function':'minecraft:set_count','count':2}]}]},
        {'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:bread','functions':[{'function':'minecraft:set_count','count':3}]}]},
        {'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:torch','functions':[{'function':'minecraft:set_count','count':8}]}]}]})

def models():
    for name,texture in {'station_anchor':'chiseled_stone_bricks','warning_note':'cartography_table_top',
                         'evacuation_note':'bookshelf','personal_note':'chiseled_bookshelf_occupied',
                         'signal_relay':'copper_block'}.items():
        # Use verified native texture names for the prototype block art.
        if name=='personal_note':texture='bookshelf'
        write_json(Path(f'assets/whileaway/blockstates/{name}.json'),{'variants':{'':{'model':f'whileaway:block/{name}'}}})
        write_json(Path(f'assets/whileaway/models/block/{name}.json'),{'parent':'minecraft:block/cube_all','textures':{'all':f'minecraft:block/{texture}'}})
        write_json(Path(f'assets/whileaway/models/item/{name}.json'),{'parent':f'whileaway:block/{name}'})
        write_json(Path(f'data/whileaway/loot_table/blocks/{name}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'whileaway:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for name in ['warning','evacuation','personal']:
        write_json(Path(f'assets/whileaway/models/item/{name}_record.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/written_book'}})
    write_json(Path('assets/whileaway/models/item/wayfarer_spawn_egg.json'),{'parent':'minecraft:item/template_spawn_egg'})
    bones=[]
    def bone(name,parent,pivot,origin=None,size=None,rotation=None):
        b={'name':name,'pivot':pivot}
        if parent:b['parent']=parent
        if rotation:b['rotation']=rotation
        if origin:b['cubes']=[{'origin':origin,'size':size,'uv':[0,0]}]
        bones.append(b)
    bone('root',None,[0,0,0])
    bone('body','root',[0,23,0],[-4,17,-3],[8,16,6],[27,0,0])
    bone('neck','body',[0,31,0],[-2,30,-1],[4,6,4],[45,0,0])
    bone('head','neck',[0,34,0],[-4,33,-3],[8,7,7],[65,0,0])
    bone('left_leg','root',[3,19,0],[1,1,-2],[3,18,3])
    bone('right_leg','root',[-3,19,0],[-4,1,-2],[3,18,3])
    bone('left_foot','left_leg',[2,2,0],[0,0,-5],[5,2,8])
    bone('right_foot','right_leg',[-2,2,0],[-5,0,-3],[5,2,8],[0,180,0])
    bone('left_arm','body',[5,29,0],[4,8,-1],[2,22,2],[12,0,-10])
    bone('right_arm','body',[-5,29,0],[-6,8,-1],[2,22,2],[-12,0,10])
    write_json(Path('assets/whileaway/geo/wayfarer.geo.json'),{'format_version':'1.12.0','minecraft:geometry':[{'description':{
        'identifier':'geometry.wayfarer','texture_width':64,'texture_height':32,'visible_bounds_width':3,'visible_bounds_height':4,'visible_bounds_offset':[0,1.5,0]},'bones':bones}]})
    animations={
        'animation.wayfarer.idle':{'loop':True,'animation_length':4,'bones':{'body':{'rotation':['math.sin(query.anim_time*90)*2',0,0]},'head':{'rotation':[0,'math.sin(query.anim_time*45)*7',0]}}},
        'animation.wayfarer.walk':{'loop':True,'animation_length':2,'bones':{'left_leg':{'rotation':['math.sin(query.anim_time*180)*20',0,0]},'right_leg':{'rotation':['-math.sin(query.anim_time*180)*20',0,0]},'body':{'rotation':['math.sin(query.anim_time*180-35)*4',0,0]},'left_arm':{'rotation':['-math.sin(query.anim_time*180-40)*12',0,0]},'right_arm':{'rotation':['math.sin(query.anim_time*180-40)*12',0,0]}}},
        'animation.wayfarer.hunt':{'loop':True,'animation_length':1,'bones':{'left_leg':{'rotation':['math.sin(query.anim_time*360)*35',0,0]},'right_leg':{'rotation':['-math.sin(query.anim_time*360)*35',0,0]},'body':{'rotation':[20,0,0]},'left_arm':{'rotation':[-65,0,-15]},'right_arm':{'rotation':[-65,0,15]}}}}
    write_json(Path('assets/whileaway/animations/wayfarer.animation.json'),{'format_version':'1.8.0','animations':animations})
    write_json(Path('data/whileaway/tags/block/mineable/pickaxe.json'),{'replace':False,'values':[]})
    write_json(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['whileaway:station_anchor','whileaway:signal_relay']})

def language():
    ko={
      'scene.whileaway.signal.1':'신호는 돌아왔다.', 'scene.whileaway.signal.2':'돌아올 사람의 이름은 없었다.',
      'itemGroup.whileaway':'네가 없는 동안', 'entity.whileaway.wayfarer':'귀로꾼',
      'item.whileaway.wayfarer_spawn_egg':'귀로꾼 관찰용 생성 알',
      'story.whileaway.welcome':'[네가 없는 동안] 숲과 평원의 폐역을 찾아보세요. 기록 블록을 우클릭하면 책을 얻습니다.',
      'story.whileaway.station':'철로는 끊겼는데, 돌아오는 길은 남아 있다.',
      'story.whileaway.record_received':'기록을 챙겼습니다. 인벤토리의 책을 손에 들고 우클릭해 읽으세요.',
      'story.whileaway.read_first':'먼저 이 역의 경고 기록을 읽어야 합니다.',
      'story.whileaway.need_redstone':'신호기에 레드스톤 가루 1개가 필요합니다. 역의 상자를 살펴보세요.',
      'story.whileaway.already_restored':'신호는 이미 돌아왔다.',
      'story.whileaway.signal':'이제 신호가 간다. 누가 받는지는 적혀 있지 않다.',
      'story.whileaway.arrival':'길을 비켜라. 아직 도착하지 못한 것이 있다.',
      'story.whileaway.after':'발소리가 멀어졌다. 남겨진 기록은 아직 역에 있다. — 첫 개발 구간 종료',
      'story.whileaway.recovery_unloaded':'조우 개체가 현재 로드되어 있지 않습니다. 폐역으로 돌아온 뒤 다시 시도하세요.',
      'story.whileaway.recovered':'조우를 정리했습니다. 기존 단서는 유지되고 30초의 준비 시간이 생깁니다.',
      'record.whileaway.warning.1':'귀환 지점\n\n두 번째는 열어 주지 마.\n발자국이 향한 곳을 믿지 마.\n\n길을 막지 마라.\n저것은 아직 도착하지 못했다.',
      'record.whileaway.warning.2':'신호기는 아직 죽어 있다.\n상자에 붉은 가루를 남겼다.\n\n깨우기 전에 나갈 길부터 봐 둬.\n가까이 다가가지 말고, 역을 벗어나라.\n\n― 마지막 야간 근무자',
      'record.whileaway.evacuation.1':'세 번째 객차는 비워 둘 것.\n걷지 못하는 사람부터 태운다.\n짐은 다음 편으로 보낸다.\n\n다음 편이 오지 않는다.',
      'record.whileaway.evacuation.2':'의자를 뜯어서 들것을 만들었다.\n천이 더 필요하다.\n\n사람이 있다고 전해 줘.\n소리가 나는 게 아니라\n사람이 있다.',
      'record.whileaway.personal.1':'당신 장갑은 여기 있어.\n왼손 엄지는 또 꿰맸어.\n이번에는 가져가.',
      'record.whileaway.personal.2':'문은 끝내 열리지 않았다.\n우리는 서로 등을 돌리고 앉았다.\n\n기대라고.'}
    en={
      'scene.whileaway.signal.1':'The signal returned.', 'scene.whileaway.signal.2':'No name remained for the one returning.',
      'itemGroup.whileaway':'While You Were Away','entity.whileaway.wayfarer':'Wayfarer',
      'item.whileaway.wayfarer_spawn_egg':'Wayfarer Observation Egg',
      'story.whileaway.welcome':'[While You Were Away] Find an abandoned station in plains or forests. Right-click record blocks to collect books.',
      'story.whileaway.station':'The tracks are broken. The way home remains.',
      'story.whileaway.record_received':'Record collected. Hold the book and right-click to read it.',
      'story.whileaway.read_first':'Read this station\'s warning record first.',
      'story.whileaway.need_redstone':'The relay needs 1 redstone dust. Check the station chest.',
      'story.whileaway.already_restored':'The signal is already awake.',
      'story.whileaway.signal':'The signal leaves. No one wrote who receives it.',
      'story.whileaway.arrival':'Leave the path clear. Something has not arrived yet.',
      'story.whileaway.after':'The footsteps recede. The records remain at the station. — End of first development slice',
      'story.whileaway.recovery_unloaded':'Encounter entity is not loaded. Return to the station and retry.',
      'story.whileaway.recovered':'Encounter cleared. Clues preserved; 30 seconds of grace.',
      'record.whileaway.warning.1':'Return point\n\nDo not open for the second knock.\nDo not trust where the footprints point.\n\nLeave the path clear.\nIt has not arrived yet.',
      'record.whileaway.warning.2':'The relay is still dead.\nI left red dust in the chest.\n\nFind the exit before waking it.\nKeep your distance. Leave the station.\n\n— Last night shift',
      'record.whileaway.evacuation.1':'Keep the third carriage empty.\nThose who cannot walk board first.\nBaggage goes on the next train.\n\nThe next train has not come.',
      'record.whileaway.evacuation.2':'We broke the chairs into stretchers.\nWe need more cloth.\n\nTell them there are people here.\nNot noises.\nPeople.',
      'record.whileaway.personal.1':'Your gloves are here.\nI mended the left thumb again.\nTake them this time.',
      'record.whileaway.personal.2':'The door never opened.\nWe sat with our backs to each other.\n\nSo there was someone to lean on.'}
    for name,k,e in [('station_anchor','귀환 표석','Return Marker'),('warning_note','경고 기록함','Warning Record'),('evacuation_note','대피 기록함','Evacuation Record'),('personal_note','남겨진 기록함','Personal Record'),('signal_relay','낡은 신호기','Old Signal Relay')]:
        ko['block.whileaway.'+name]=k;en['block.whileaway.'+name]=e
    for name,k,e in [('warning','야간 근무자의 경고','Night Shift Warning'),('evacuation','마지막 대피 지시','Last Evacuation Order'),('personal','꿰맨 장갑','Mended Gloves')]:
        ko[f'item.whileaway.{name}_record']=k;en[f'item.whileaway.{name}_record']=e
    for name,k,e in [('distant_knock','멀리서 문 두드리는 소리','Distant knocking'),('returning_steps','늦게 따라오는 발소리','Dragging footsteps'),('wayfarer_breath','접힌 숨소리','Folded breathing'),('wayfarer_attack','마른 관절이 펼쳐지는 소리','Dry joints unfolding'),('signal_wake','오래된 신호기가 깨어나는 소리','Old relay waking'),('homeward','귀환의 여운','Homeward'),('station_unease','어긋난 역의 선율','An unsettled station')]:
        ko['subtitles.whileaway.'+name]=k;en['subtitles.whileaway.'+name]=e
    write_json(Path('assets/whileaway/lang/ko_kr.json'),ko);write_json(Path('assets/whileaway/lang/en_us.json'),en)

def audio():
    import numpy as np
    import soundfile as sf
    rate=44100;rng=np.random.default_rng(74192361)
    dest=RES/'assets/whileaway/sounds';dest.mkdir(parents=True,exist_ok=True)
    manifest={};sounds={}
    def save(name,signal,music=False):
        signal=np.asarray(signal,dtype=np.float64)
        signal=signal/max(1.,float(np.max(np.abs(signal)))/.6)
        fade=min(441,len(signal)//4);signal[:fade]*=np.linspace(0,1,fade);signal[-fade:]*=np.linspace(1,0,fade)
        # Chunked writes avoid a native encoder stack overflow on long Windows Vorbis writes.
        with sf.SoundFile(dest/f'{name}.ogg','w',samplerate=rate,channels=1,format='OGG',subtype='VORBIS') as output:
            for begin in range(0,len(signal),4096): output.write(signal[begin:begin+4096])
        decoded,sr=sf.read(dest/f'{name}.ogg'); assert sr==rate and np.all(np.isfinite(decoded)) and np.max(np.abs(decoded)) < .95
        print('Encoded and decoded', name, flush=True)
        manifest[name]={'seconds':round(len(signal)/rate,3),'channels':1,'peak':round(float(np.max(np.abs(decoded))),5),'origin':'original deterministic synthesis; prototype'}
        sounds[name]={'subtitle':f'subtitles.whileaway.{name}','sounds':[{'name':f'whileaway:{name}','stream':music}]}
    def hit(freq,length=.3):
        t=np.arange(int(length*rate))/rate
        return (np.sin(2*np.pi*freq*t)*.6+rng.normal(0,.12,len(t)))*np.exp(-t*18)*np.minimum(1,t*800)
    def place(out,clip,seconds,gain=1):
        at=int(seconds*rate);end=min(len(out),at+len(clip));out[at:end]+=clip[:end-at]*gain
    a=np.zeros(rate*4)
    for t,f in [(0.3,130),(0.74,119),(2.3,85)]:place(a,hit(f),t,.7)
    save('distant_knock',a)
    a=np.zeros(rate*6)
    for t in [.2,1.2,2.3,3.5,4.0]:
        place(a,hit(80,.35),t,.5);place(a,hit(145,.6),t+.18,.13)
    save('returning_steps',a)
    t=np.arange(rate*3)/rate;noise=rng.normal(0,1,len(t));smooth=np.convolve(noise,np.ones(25)/25,mode='same')
    save('wayfarer_breath',smooth*np.sin(np.pi*t/3)**2*.65+np.sin(2*np.pi*73*t)*np.sin(np.pi*t/3)**2*.06)
    a=np.zeros(rate*2)
    for t in [0,.065,.14,.31]:place(a,hit(160-t*200,.55),t,.5)
    save('wayfarer_attack',a)
    a=np.zeros(rate*5)
    for i,f in enumerate([261.63,311.13,392]):place(a,hit(f,1.5),.25+i*.63,.5)
    save('signal_wake',a)
    def score(notes,duration):
        out=np.zeros(rate*duration)
        for onset,freq,length,gain in notes:
            t=np.arange(int(length*rate))/rate
            tone=(np.sin(2*np.pi*freq*t)+.18*np.sin(2*np.pi*freq*2*t))*np.exp(-t/1.5)*np.minimum(1,t*25)
            place(out,tone,onset,gain)
        return out
    save('station_unease',score([(0,130.81,5,.12),(2,155.56,5,.12),(5,196,6,.1),(8,195.3,7,.08),(12,138.59,5,.09)],20),True)
    save('homeward',score([(0,261.63,5,.16),(3,311.13,5,.15),(6,392,6,.14),(9,349.23,5,.1),(12,261.63,6,.12)],22),True)
    write_json(Path('assets/whileaway/sounds.json'),sounds)
    (ROOT/'evidence/audio-assets.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print('PASS audio: 7 original OGG assets decoded; peak ceiling checked')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--no-audio',action='store_true');args=parser.parse_args()
    structure();models();language()
    from city_assets import generate
    generate()
    if not args.no_audio:audio()
    print('PASS assets: station + worldgen + model + animations + ko_kr/en_us generated')
