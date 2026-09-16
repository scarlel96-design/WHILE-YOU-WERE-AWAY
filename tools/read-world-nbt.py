"""Read-only fixture evidence: standard compressed NBT, no client launch or audio."""
import gzip,struct,sys,json
from pathlib import Path
def read(path):
    raw=Path(path).read_bytes()
    if raw[:2]==b'\x1f\x8b':raw=gzip.decompress(raw)
    at=0
    def take(n):
        nonlocal at
        b=raw[at:at+n];at+=n
        if len(b)!=n:raise ValueError('truncated NBT')
        return b
    def num(fmt):return struct.unpack('>'+fmt,take(struct.calcsize('>'+fmt)))[0]
    def text():return take(num('H')).decode('utf-8')
    def payload(t):
        if 1<=t<=6:return num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7:return list(take(num('i')))
        if t==8:return text()
        if t==9:
            kind=num('B');count=num('i');return [payload(kind) for _ in range(count)]
        if t==10:
            out={}
            while (kind:=num('B'))!=0:
                key=text();out[key]=payload(kind)
            return out
        if t in (11,12):return [num('i' if t==11 else 'q') for _ in range(num('i'))]
        raise ValueError('unknown NBT tag '+str(t))
    kind=num('B');text();out=payload(kind)
    if at!=len(raw):raise ValueError('trailing NBT')
    return out
if __name__=='__main__':print(json.dumps(read(sys.argv[1]),ensure_ascii=False,indent=2))
