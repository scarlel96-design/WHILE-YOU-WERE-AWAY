import sys, zipfile, hashlib
from pathlib import Path
p=Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    assert z.testzip() is None
    names=set(z.namelist())
    assert "io/github/whileaway/CityDistrict.class" in names
    print("VALID_JAR living_content="+str("io/github/whileaway/core/CityLivingLayout.class" in names))
print("SHA256="+hashlib.sha256(p.read_bytes()).hexdigest())
