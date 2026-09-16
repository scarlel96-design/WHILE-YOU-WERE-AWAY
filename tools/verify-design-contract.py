from pathlib import Path
import json,re,sys
r=Path(__file__).resolve().parents[1]
doc=(r/'docs/MYSTERY_RESOURCE_EXTENSION.md').read_text(encoding='utf-8')
numbers=[int(x) for x in re.findall(r'^\| (\d+) \|',doc,re.M)]
assert numbers==list(range(33,82)),numbers
assets=json.loads((r/'docs/asset-contract.json').read_text(encoding='utf-8'))
assert not assets['final_pack_selected'] and not assets['external_packs_bundled']
for a in assets['items']:
    assert a['marker'] in (r/a['source']).read_text(encoding='utf-8'),a['id']
    assert a['status'] in ('placeholder','prototype')
seeds=json.loads((r/'docs/foreshadow-registry.json').read_text(encoding='utf-8'))['entries']
required={'id','tier','first_chapter','first_exposure','likely_initial_interpretation','actual_meaning','reinforcement',
    'related_npcs','related_places','related_items','related_puzzles','related_endings','payoff_timing','payoff_method',
    'post_payoff_change','intentionally_unresolved','quality_gates'}
for seed in seeds:assert required<=seed.keys() and len(seed['quality_gates'])==8
print(f'PASS design requirements=49; tracked unfinished asset groups={len(assets["items"])}; foreshadow seeds={len(seeds)}')
if '--final-release' in sys.argv:
    print('NOT READY final art: tracked placeholders/prototypes remain');sys.exit(2)
