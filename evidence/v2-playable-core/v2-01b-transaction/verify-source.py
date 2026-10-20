"""Check package bytes and source contracts. This is not a gameplay test."""
from pathlib import Path
import hashlib
import json
import sys
import zipfile

folder = Path(__file__).resolve().parent
mode = sys.argv[1]
assert mode in ("Baseline", "Modified")
root = folder / ("baseline-source" if mode == "Baseline" else "modified-source")
manifest = json.loads((folder / "SOURCE-HASHES.json").read_text(encoding="utf-8"))
for item in manifest:
    actual = hashlib.sha256((root / item["path"]).read_bytes()).hexdigest()
    assert actual == item[mode.lower() + "_sha256"], item["path"]
def source(name):
    return (root / "src/main/java/io/github/whileaway" / name).read_text(encoding="utf-8-sig")
actors = source("StoryActors.java")
events = source("StoryEvents.java")
npc = source("NpcEvents.java")
wayfarer = source("entity/Wayfarer.java")
texts = [json.loads((root / "src/main/resources/assets/whileaway/lang" / name).read_text(encoding="utf-8-sig"))
         for name in ("en_us.json", "ko_kr.json")]
if mode == "Baseline":
    assert "CHASE_OBSERVED" not in actors
    assert 'Component.translatable("story.whileaway.arrival")' in events
    assert 'Stage.NOT_STARTED)return false' in npc
    jar = folder / "BASELINE.jar"
else:
    assert 'CHASE_OBSERVED="player_observed_returner"' in actors
    assert 'r.facts.contains(CHASE_OBSERVED)' in actors
    assert 'observationTicks>=8' in wayfarer
    assert 'player.hasLineOfSight(this)' in wayfarer
    assert 'player.getLookAngle().dot' in wayfarer
    assert 'Component.translatable("story.whileaway.arrival")' not in events
    assert 'Stage.NOT_STARTED)return false' not in npc
    assert all('story.whileaway.station_revisit_unseen' in t for t in texts)
    jar = folder / "MODIFIED.jar"
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    actor_class = archive.read("io/github/whileaway/StoryActors.class")
    assert (b"player_observed_returner" in actor_class) == (mode == "Modified")
print(f"{mode.upper()}_SOURCE_PACKAGE_PASS files={len(manifest)} json=2 jar=valid")
