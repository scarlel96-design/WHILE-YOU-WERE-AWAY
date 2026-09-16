"""Data-driven dimension and bilingual city records, called after the original asset generator."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(name,obj):
    p=R/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def generate():
    write('assets/whileaway/city_art.json',{'revision':2,'new_world_plan':'CityArtLayout','legacy_plan_preserved':True,'scene':'seven_departures'})
    write('data/whileaway/dimension_type/quiet_city.json',{
        'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':True,'has_ceiling':False,
        'ambient_light':0.12,'fixed_time':18000,'piglin_safe':False,'bed_works':False,'respawn_anchor_works':False,
        'has_raids':False,'logical_height':384,'min_y':-64,'height':384,'infiniburn':'#minecraft:infiniburn_overworld',
        'effects':'minecraft:overworld','monster_spawn_block_light_limit':0,'monster_spawn_light_level':0})
    write('data/whileaway/dimension/quiet_city.json',{'type':'whileaway:quiet_city','generator':{'type':'minecraft:flat','settings':{
        'biome':'whileaway:quiet_city','lakes':False,'features':False,'structure_overrides':[],
        'layers':[{'height':1,'block':'minecraft:bedrock'},{'height':127,'block':'minecraft:deepslate'}]}}})
    write('data/whileaway/worldgen/biome/quiet_city.json',{
        'has_precipitation':False,'temperature':0.4,'downfall':0.0,'spawners':{},'spawn_costs':{},'carvers':{},'features':[],
        'effects':{'fog_color':2633534,'water_color':2966354,'water_fog_color':1646632,'sky_color':526862,
                   'grass_color':5659482,'foliage_color':5002066}})
    for name,texture in {'return_light':'sea_lantern','city_intake_note':'chiseled_bookshelf_empty','city_home_note':'bookshelf','city_siren_note':'note_block'}.items():
        write(f'assets/whileaway/blockstates/{name}.json',{'variants':{'':{'model':f'whileaway:block/{name}'}}})
        write(f'assets/whileaway/models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'minecraft:block/{texture}'}})
        write(f'assets/whileaway/models/item/{name}.json',{'parent':f'whileaway:block/{name}'})
        if name!='return_light':
            write(f'data/whileaway/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'whileaway:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for kind in ['intake','home','siren']:
        write(f'assets/whileaway/models/item/city_{kind}_record.json',{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/written_book'}})
    write('assets/whileaway/blockstates/signal_relay.json',{'variants':{'lit=false':{'model':'whileaway:block/signal_relay'},'lit=true':{'model':'whileaway:block/signal_relay_lit'}}})
    write('assets/whileaway/models/block/signal_relay_lit.json',{'parent':'minecraft:block/cube_bottom_top','textures':{
        'side':'minecraft:block/copper_block','bottom':'minecraft:block/copper_block','top':'minecraft:block/sea_lantern'}})
    ko={
      'block.whileaway.return_light':'돌아가는 불빛','block.whileaway.city_intake_note':'접수대 장부',
      'block.whileaway.city_home_note':'식탁 아래 편지','block.whileaway.city_siren_note':'방송실 교대표',
      'item.whileaway.city_intake_record':'접수대에서 찢어진 장부','item.whileaway.city_home_record':'차려 둔 저녁',
      'item.whileaway.city_siren_record':'마지막 교대',
      'city.whileaway.locked':'발소리가 멎은 뒤, 남겨진 개인 기록을 읽으세요. 신호는 주소를 기다립니다.',
      'city.whileaway.preparing':'선로 너머에서 불이 하나씩 켜집니다. 잠시 뒤 신호기를 다시 만져 보세요.',
      'city.whileaway.unavailable':'신호가 닿는 곳이 아직 월드에 없습니다. 도시 차원 데이터를 확인해 주세요.',
      'city.whileaway.obstructed':'발을 놓을 자리가 없습니다. 출입구 주변을 비운 뒤 다시 시도하세요.',
      'city.whileaway.entered':'저녁골 · 역전 지구. 뒤에 남은 불빛은 돌아가는 길입니다. /whileaway return 으로도 복귀합니다.',
      'city.whileaway.returned':'숲은 아무 일도 없었다는 듯 흔들립니다.',
      'city.whileaway.records_complete':'장부에는 자리가 없었고, 식탁에는 자리가 남았다. 북쪽 방송실이 문을 닫은 이유를 이제 압니다.',
      'scene.whileaway.city.1':'이 도시는 저녁을 넘기지 못했다.',
      'scene.whileaway.city.2':'돌아갈 불빛은 뒤에 남겨 두었다.',
      'record.whileaway.city_intake.1':'접수 마감 18:40\n\n의자는 스물넷. 이름은 서른하나. 빈칸 일곱 개를 지우라는 지시가 내려왔다. 나는 연필을 부러뜨렸다.',
      'record.whileaway.city_intake.2':'북쪽 방송실에 전화를 걸었다. 차를 한 번만 더 보내 달라고. 수화기에서는 출발음만 났다.\n\n우리는 사람 대신 의자를 세기 시작했다.',
      'record.whileaway.city_home.1':'밥은 두 그릇만 했다. 셋째 그릇까지 식으면 네가 정말 늦는 것 같아서.\n\n현관 신발은 돌려놓지 않았다. 돌아오는 발이 헷갈릴까 봐.',
      'record.whileaway.city_home.2':'남쪽 동의 마지막 불을 끄라는 방송이 왔다. 나는 식탁 아래 작은 등을 숨겼다.\n\n돌아오는 사람은 높은 창보다 낮은 불빛을 먼저 본다.',
      'record.whileaway.city_siren.1':'18:43 / 마지막 교대\n\n문을 열면 선로까지 사람이 찬다. 문을 닫으면 접수대의 일곱 이름이 남는다.\n\n열쇠가 이렇게 무거운 줄 몰랐다.',
      'record.whileaway.city_siren.2':'나는 문을 닫았다. 출발 방송은 끄지 않았다. 기다림이 끝나는 소리를 내가 낼 수 없었다.\n\n퇴근 도장은 찍지 말아 주세요. 아직 여기 있습니다.'}
    en={
      'block.whileaway.return_light':'Return Light','block.whileaway.city_intake_note':'Intake Ledger',
      'block.whileaway.city_home_note':'Letter beneath the Table','block.whileaway.city_siren_note':'Broadcast Shift Log',
      'item.whileaway.city_intake_record':'Torn Intake Ledger','item.whileaway.city_home_record':'Dinner Kept Warm','item.whileaway.city_siren_record':'The Last Shift',
      'city.whileaway.locked':'After the footsteps end, read the personal record. The signal is waiting for an address.',
      'city.whileaway.preparing':'Lights wake beyond the tracks. Touch the relay again in a moment.',
      'city.whileaway.unavailable':'The signal has no destination in this world. Check the city dimension data.',
      'city.whileaway.obstructed':'There is nowhere to stand. Clear the arrival area and try again.',
      'city.whileaway.entered':'Evening Hollow / Station District. The light behind you leads home. /whileaway return also returns you.',
      'city.whileaway.returned':'The forest moves as if nothing happened.',
      'city.whileaway.records_complete':'No room in the ledger. An empty place at the table. Now you know why the northern broadcast room closed its doors.',
      'scene.whileaway.city.1':'This city never made it past evening.',
      'scene.whileaway.city.2':'I left a light behind, to find my way home.',
      'record.whileaway.city_intake.1':'Intake closed, 18:40\n\nTwenty-four chairs. Thirty-one names. They told me to erase seven spaces. I broke my pencil.',
      'record.whileaway.city_intake.2':'I called the northern broadcast room. One more train, I begged. Only the departure chime answered.\n\nWe began counting chairs instead of people.',
      'record.whileaway.city_home.1':'I made two bowls of rice. If a third bowl went cold, you would truly be late.\n\nI left your shoes facing the door, so your returning feet would know the way.',
      'record.whileaway.city_home.2':'The announcement ordered the last light in the southern block turned off. I hid a lamp under the table.\n\nPeople coming home see low lights before high windows.',
      'record.whileaway.city_siren.1':'18:43 / Last shift\n\nOpen the doors and the crowd reaches the tracks. Close them and seven names remain at intake.\n\nI never knew a key could weigh so much.',
      'record.whileaway.city_siren.2':'I closed the doors. I left the departure announcement on. I could not be the one to end their waiting.\n\nDo not clock me out. I am still here.'}
    for lang,extra in [('ko_kr',ko),('en_us',en)]:
        extra.update({'scene.whileaway.seven.1':'장부에서 지운 일곱 이름.' if lang=='ko_kr' else 'Seven names erased from the ledger.',
                      'scene.whileaway.seven.2':'출발음은 아직 끝나지 않았다.' if lang=='ko_kr' else 'The departure chime never ended.'})
        path=R/f'assets/whileaway/lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(extra);write(f'assets/whileaway/lang/{lang}.json',data)
    print('PASS city dimension/biome/models/bilingual records generated')
if __name__=='__main__':generate()
