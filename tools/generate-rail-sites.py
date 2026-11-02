"""Build two optional rail sites. Do not change existing station templates or saves."""
from pathlib import Path
import json,runpy
ROOT=Path(__file__).resolve().parents[1]
g=runpy.run_path(str(ROOT/'tools/generate-assets.py'));nbt=g['nbt'];write=g['write_json']

def site(name,size,passage=False):
    blocks={};palette=[];indexes={}
    def put(x,y,z,name,props=None,extra=None):
        assert all(0<=a<b for a,b in zip((x,y,z),size))
        key=(name,tuple(sorted((props or {}).items())))
        if key not in indexes:
            entry={'Name':(8,name)}
            if props:entry['Properties']=(10,{k:(8,str(v)) for k,v in props.items()})
            indexes[key]=len(palette);palette.append(entry)
        item={'pos':(9,(3,[x,y,z])),'state':(3,indexes[key])}
        if extra:item['nbt']=(10,extra)
        blocks[x,y,z]=item
    def box(x0,x1,y0,y1,z0,z1,block,props=None):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):put(x,y,z,block,props)
    def sign(x,y,z,lines):
        put(x,y,z,'minecraft:oak_sign',{'rotation':'0','waterlogged':'false'},
            {'id':(8,'minecraft:sign'),'front_text':(10,{'messages':(9,(8,[json.dumps({'text':s},ensure_ascii=False) for s in lines])),'color':(8,'black')})})
    if not passage:
        box(2,8,0,0,4,12,'minecraft:stone_bricks')
        box(2,8,1,4,4,12,'minecraft:bricks');box(3,7,1,4,5,11,'minecraft:air')
        box(4,5,1,3,4,4,'minecraft:air')
        box(4,6,0,0,1,3,'minecraft:gravel')
        for x in (2,8):
            for z in (4,12):box(x,x,1,4,z,z,'minecraft:stripped_spruce_log',{'axis':'y'})
            for z in (7,8):put(x,3,z,'minecraft:glass_pane',{'north':'true','south':'true','east':'false','west':'false','waterlogged':'false'})
        for x in range(1,10):
            height=5+min(x-1,9-x,3)
            for z in range(3,14):
                put(x,height,z,'minecraft:dark_oak_stairs',{'facing':'east' if x<5 else 'west','half':'bottom','shape':'straight','waterlogged':'false'})
            for z in (4,12):
                for y in range(5,height):put(x,y,z,'minecraft:bricks')
        put(7,1,10,'minecraft:crafting_table');put(3,1,10,'minecraft:barrel',{'facing':'up','open':'false'})
        put(6,1,11,'minecraft:spruce_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
        box(3,7,5,5,9,9,'minecraft:stripped_spruce_log',{'axis':'x'})
        put(5,4,9,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
        put(7,1,6,'minecraft:weathered_copper_bulb',{'lit':'false','powered':'false'})
        put(6,1,6,'minecraft:lever',{'face':'floor','facing':'east','powered':'false'})
        sign(3,1,6,['S-04','구내 신호표','저녁 순회','복귀 확인'])
        for x in (12,14):
            for z in range(17):
                put(x,0,z,'minecraft:gravel')
                if z not in (5,6,13):put(x,1,z,'minecraft:rail',{'shape':'north_south','waterlogged':'false'})
        for x in (10,16):
            put(x,0,8,'minecraft:stone_bricks');box(x,x,1,5,8,8,'minecraft:stripped_dark_oak_log',{'axis':'y'})
        for n in range(7):put(10+n,6,8,'whileaway:rail_memory_beam',{'facing':'east','segment':str(n)})
    else:
        box(5,11,0,0,4,16,'minecraft:stone_bricks')
        box(5,11,1,4,6,16,'minecraft:stone_bricks');box(6,10,1,3,6,16,'minecraft:air')
        box(5,11,5,5,7,16,'minecraft:moss_block')
        box(6,10,1,6,0,5,'minecraft:air')
        for z,y in [(0,3),(1,3),(2,2),(3,1),(4,0),(5,0)]:
            for x in range(7,10):
                put(x,y,z,'minecraft:stone_brick_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
                for below in range(y):put(x,below,z,'minecraft:cobblestone')
        box(1,4,0,0,8,13,'minecraft:cobblestone')
        box(1,4,1,3,8,13,'minecraft:stone_bricks');box(2,4,1,2,9,12,'minecraft:air')
        box(1,5,4,4,8,13,'minecraft:stone_bricks');box(5,5,1,2,10,11,'minecraft:air')
        box(5,5,1,4,12,14,'minecraft:cracked_stone_bricks')
        put(2,1,10,'minecraft:crafting_table');put(2,1,12,'minecraft:barrel',{'facing':'up','open':'false'})
        put(8,3,9,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
        put(3,3,10,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
        for z in (6,7,9,10,11,14):put(8,1,z,'minecraft:rail',{'shape':'north_south','waterlogged':'false'})
        box(7,9,1,2,17,17,'minecraft:coal_ore')
        put(10,1,8,'minecraft:weathered_copper_bulb',{'lit':'false','powered':'false'})
        put(10,1,7,'minecraft:lever',{'face':'floor','facing':'south','powered':'false'})
        sign(10,1,12,['S-04','정비 통로','외측 보수 17','내측 각인 16'])
    root={'DataVersion':(3,3955),'size':(9,(3,list(size))),'palette':(9,(10,palette)),'blocks':(9,(10,list(blocks.values()))),'entities':(9,(10,[]))}
    nbt(Path('data/whileaway/structure/'+name+'.nbt'),root)
    write(Path('data/whileaway/worldgen/template_pool/'+name+'.json'),{'name':'whileaway:'+name,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'whileaway:'+name,'processors':'whileaway:rail_sites','projection':'rigid'}}]})
    write(Path('data/whileaway/worldgen/structure/'+name+'.json'),{'type':'minecraft:jigsaw','biomes':'#whileaway:has_rail_site','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'whileaway:'+name,'size':1,'start_height':{'absolute':-3 if passage else 0},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':32,'use_expansion_hack':False})
    write(Path('data/whileaway/worldgen/structure_set/'+name+'.json'),{'structures':[{'structure':'whileaway:'+name,'weight':1}],'placement':{'type':'minecraft:random_spread','spacing':40 if passage else 32,'separation':16 if passage else 12,'salt':74192363 if passage else 74192362,'exclusion_zone':{'other_set':'minecraft:villages','chunk_count':6}}})
    print(name,len(blocks),'blocks')

if __name__=='__main__':
    site('rail_signal_office',(19,10,17))
    site('rail_maintenance_passage',(17,9,21),True)
    write(Path('data/whileaway/tags/worldgen/biome/has_rail_site.json'),{'replace':False,'values':['minecraft:plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga']})
    write(Path('data/whileaway/worldgen/processor_list/rail_sites.json'),{'processors':[{'processor_type':'minecraft:protected_blocks','value':'#whileaway:rail_site_protected'}]})
    write(Path('data/whileaway/tags/block/rail_site_protected.json'),{'replace':False,'values':['#minecraft:features_cannot_replace','#minecraft:planks','#minecraft:logs','#minecraft:beds','#minecraft:wool','minecraft:chest','minecraft:trapped_chest','minecraft:barrel','minecraft:furnace','minecraft:blast_furnace','minecraft:smoker','minecraft:crafting_table','minecraft:stone_bricks','minecraft:bricks','minecraft:glass','minecraft:glass_pane','minecraft:cobblestone','minecraft:mossy_cobblestone','minecraft:oak_door','minecraft:spruce_door','minecraft:iron_door','whileaway:warning_note','whileaway:evacuation_note','whileaway:personal_note','whileaway:city_intake_note','whileaway:city_home_note','whileaway:city_siren_note','whileaway:return_route_trace','whileaway:maintenance_tag','whileaway:station_anchor','whileaway:signal_relay','whileaway:return_light','whileaway:rail_memory_beam']})
    write(Path('assets/whileaway/blockstates/rail_memory_beam.json'),{'variants':{'':{'model':'whileaway:block/rail_memory_beam'}}})
    write(Path('assets/whileaway/models/block/rail_memory_beam.json'),{'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/weathered_cut_copper'}})
    write(Path('assets/whileaway/models/item/rail_memory_beam.json'),{'parent':'whileaway:block/rail_memory_beam'})
    for lang,label in [('en_us','Weathered Signal Beam'),('ko_kr','낡은 신호 가로보')]:
        path=ROOT/'src/main/resources/assets/whileaway/lang'/f'{lang}.json';data=json.loads(path.read_text(encoding='utf-8'));data['block.whileaway.rail_memory_beam']=label;path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
