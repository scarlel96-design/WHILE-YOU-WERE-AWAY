"""Check the isolated test-source package. Do not launch Minecraft."""
import hashlib,sys
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[3]
baseline=root/'dist/v2-01e-continuation/whileaway-0.3.1-v2-01e-continuation-source.zip'
mode,target=sys.argv[1:]
assert mode in {'BASELINE','MODIFIED','ROLLBACK'}
with ZipFile(baseline) as old,ZipFile(target) as new:
    assert new.testzip() is None
    assert new.namelist()==old.namelist()
    main=[n for n in old.namelist() if n.startswith('src/main/')]
    assert all(old.read(n)==new.read(n) for n in main)
    s=new.read('tools/validation/play-input/java/wwa/input/InputAssist.java').decode()
    g=new.read('tools/ci/v2-01e.init.gradle').decode()
    protected=mode=='MODIFIED'
    assert ('private void protectPlayer' in s)==protected
    assert ('MOUSE_GRAB ' in s)==protected
    assert ('REASSERT ' in s)==protected
    assert ("systemProperty 'whileaway.testPlayerProtection','true'" in g)==protected
    if protected:
        assert 'instanceof ServerPlayer' in s and 'world.startsWith(root)' in s
        assert 'player.server.isDedicatedServer()' in s
        assert 'maximum two short leases' in s and '3_000_000_000L' in s
print(f'{mode}: package=PASS protection={"ON" if protected else "OFF"} input_guard={"ON" if protected else "OFF"} production=UNCHANGED')
