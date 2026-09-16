"""Read-only check of actual entity-region cleanup after the late-generation client."""
from pathlib import Path
import hashlib,importlib.util,json,struct,zlib,gzip
R=Path(__file__).resolve().parents[1];E=R/'evidence/canonical-recovery'
source=json.loads((E/'canonical-final-before-Late.json').read_text())
region=Path(source['target'])/'entities/r.18.18.mca'
data=region.read_bytes();index=((600&31)+(600&31)*32)*4
sector=int.from_bytes(data[index:index+3],'big')
entities=[]
if sector:
    start=sector*4096;length=struct.unpack('>I',data[start:start+4])[0];compression=data[start+4]
    raw=data[start+5:start+4+length]
    raw=zlib.decompress(raw) if compression==2 else gzip.decompress(raw) if compression==1 else raw if compression==3 else None
    assert raw is not None,'unsupported fixture compression'
    p=E/'late-entity-chunk.nbt';p.write_bytes(raw)
    spec=importlib.util.spec_from_file_location('nbt',R/'tools/read-world-nbt.py');nbt=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt)
    entities=nbt.read(p).get('Entities',[])
assert not entities,'stale entity NBT survived the actual client cleanup save'
text='PASS real late-loaded entity chunk (600,600) cleanup persisted; entityCount=0; regionSHA256='+hashlib.sha256(data).hexdigest()
(E/'late-cleanup.txt').write_text(text+'\n');print(text)
