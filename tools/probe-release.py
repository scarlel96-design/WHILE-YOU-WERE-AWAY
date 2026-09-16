"""Read the built archive only; does not launch Minecraft or play audio."""
import sys,zipfile,tomllib
with zipfile.ZipFile(sys.argv[1]) as z:
    assert z.testzip() is None
    data=tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    version=data['mods'][0]['version']
    city='data/whileaway/dimension/quiet_city.json' in z.namelist()
    assert version==sys.argv[2],(version,sys.argv[2])
    assert city==(version=='0.2.0-dev.1')
    print(f'version={version}; city_dimension={str(city).lower()}')
