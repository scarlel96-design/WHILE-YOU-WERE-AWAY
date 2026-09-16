"""Inspect release archives, not running client state."""
import json,sys,tomllib,zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    assert z.testzip() is None
    version=tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())['mods'][0]['version']
    journal='io/github/whileaway/SceneRecovery.class' in z.namelist()
    assert version==sys.argv[2] and journal==bool(int(sys.argv[3])),(version,journal)
    assert json.loads(z.read('assets/whileaway/city_art.json'))['revision']==2
    print(f'version={version}; checkpoint_journal={str(journal).lower()}; art_revision=2')
