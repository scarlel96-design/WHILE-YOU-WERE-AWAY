"""Validate release metadata without launching a client or playing audio."""
import json, sys, tomllib, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    assert z.testzip() is None
    version=tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())['mods'][0]['version']
    key='assets/whileaway/city_art.json'
    revision=json.loads(z.read(key))['revision'] if key in z.namelist() else 1
    assert 'data/whileaway/dimension/quiet_city.json' in z.namelist()
    assert version==sys.argv[2] and revision==int(sys.argv[3]), (version,revision)
    print(f'version={version}; art_revision={revision}; city_dimension=true')
