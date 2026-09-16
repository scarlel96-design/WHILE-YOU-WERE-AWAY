package io.github.whileaway;
import io.github.whileaway.core.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class NarrativeData extends SavedData {
public static final int SCHEMA = 5;
private java.nio.file.Path storageFile;
private String expectedHash;
private boolean storySaveWritable=true;
private ReturnNetworkState returnNetwork=ReturnNetworkState.notStarted();

@FunctionalInterface
interface StoryWriter {
void write(CompoundTag root, java.nio.file.Path file) throws java.io.IOException;
}
private StoryWriter storyWriter=net.neoforged.neoforge.common.IOUtilities::writeNbtCompressed;

public enum ReturnNetworkBoundary {
NO_CHANGE,
DISCOVERED,
NPC_JOINED,
SIGNAL_OBSERVED,
INTERVENTION_COMMITTED,
PRESENTATION_STARTED,
RESPONSE_OBSERVED,
SHARED_EXPERIENCE,
COMPLETED
}

public record ReturnNetworkCommit(
ReturnNetworkBoundary boundary,
ReturnNetworkState state,
String durableHash,
boolean wrote
) {}

void bindStorage(java.nio.file.Path file,String hash){storageFile=file;expectedHash=hash;}
public boolean storySaveWritable(){return storySaveWritable;}
void blockStoryWrites(){storySaveWritable=false;}
void setStoryWriterForTest(StoryWriter writer){storyWriter=Objects.requireNonNull(writer);}

private final Map<UUID, Entry> players = new TreeMap<>();
public final Map<String,StoryActorRecord> actors=new TreeMap<>();

public static final class Entry {
public Progress progress = Progress.empty();
public BlockPos station;
public UUID encounter;
public long readingUntil;
public boolean cityVisited;
public int cityClues;
public boolean cityShockTriggered;
public CityInvestigation investigation=new CityInvestigation();
public final Map<Integer,SceneRecord> scenes=new TreeMap<>();
public BlockPos returnPosition;
public float returnYaw,returnPitch;
public long transitAfter;
}

public Entry entry(UUID id) { return players.computeIfAbsent(id, ignored -> new Entry()); }

public synchronized ReturnNetworkState returnNetwork(){return returnNetwork;}

public void discover(UUID id, Clue clue) {
var e = entry(id); e.progress = e.progress.discover(clue); setDirty();
}

public static NarrativeData get(MinecraftServer server) {
return StoryStorage.open(server.overworld().getDataStorage(),server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data"),
server.registryAccess(),server.levelKeys().stream().map(k->k.location().toString()).collect(java.util.stream.Collectors.toSet()));
}

static NarrativeData load(CompoundTag root, HolderLookup.Provider registry) {
int schema=root.getInt("schema");
if (schema > SCHEMA) throw new IllegalStateException("Story save uses a newer schema; restore the matching mod version.");
var data = new NarrativeData();

if(schema<5 && root.contains("returnNetwork"))
throw new IllegalStateException("field=returnNetwork incompatible_format");
if(schema>=5 && root.contains("returnNetwork")) {
if(!root.contains("returnNetwork",Tag.TAG_COMPOUND))
throw new IllegalStateException("field=returnNetwork missing_or_wrong_type");
var parsed=ReturnNetworkState.load(root.getCompound("returnNetwork"));
if(parsed.stage==ReturnNetworkState.Stage.NOT_STARTED)
throw new IllegalStateException("field=returnNetwork materialized_not_started");
data.returnNetwork=parsed;
}

var list = root.getList("players", Tag.TAG_COMPOUND);
for (int i = 0; i < list.size(); i++) {
var t = list.getCompound(i);
if (!t.hasUUID("id")) continue;
var e = data.entry(t.getUUID("id"));
e.progress = new Progress(t.getInt("clues"), t.getLong("next"), t.getInt("events"), t.getBoolean("complete"));
if (t.contains("station")) e.station = BlockPos.of(t.getLong("station"));
if (t.hasUUID("encounter")) e.encounter = t.getUUID("encounter");
e.readingUntil = t.getLOng("readingUntil");
e.cityVisited=t.getBoolean("cityVisited");e.cityClues=t.getInt("cityClues")&7;
e.cityShockTriggered=t.getBoolean("cityShockTriggered");
e.investigation=CityInvestigation.load(t.getCompound("investigation"),e.cityClues,t.contains("investigation",Tag.TAG_COMPOUND));
e.cityClues=e.investigation.evidence();
var scenes=t.getList("scenes",Tag.TAG_COMPOUND);
for(int j=0;j<scenes.size();j++) {
var scene=SceneRecord.load(scenes.getCompound(j),t.getUUID("id"));
if(e.scenes.putIfAbsent(scene.kind,scene)!=null)throw new IllegalStateException("Duplicate persisted scene kind="+scene.kind);
}
if(e.cityShockTriggered&&!e.scenes.containsKey(3)) {
var legacy=SceneRecord.migrated(3,CityHorror.STAGE,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,3,0);e.scenes.put(3,legacy);
}
if(schema<4) {
if(e.progress.has(Clue.SIGNAL_RESTORED)&&e.station!=null&&!e.scenes.containsKey(1)) {
var legacy=SceneRecord.migrated(1,e.station,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,127,0);e.scenes.put(1,legacy);
}
if(e.cityVisited&&!e.scenes.containsKey(2)) {
var legacy=SceneRecord.migrated(2,CityDistrict.ARRIVAL,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,127,0);e.scenes.put(2,legacy);
}
}
if(t.contains("returnPosition"))e.returnPosition=BlockPos.of(t.getLOng("returnPosition"));
e.returnYaw=t.getFloat("returnYaw");e.returnPitch=t.getFloat("returnPitch");e.transitAfter=Math.max(0,t.getLOng("transitAfter"));
}

if(root.getInt("actorSchema")>1)throw new IllegalStateException("MANUAL_DIAGNOSTIC Unsupported actor schema");
var actors=root.getList("actors",Tag.TAG_COMPOUND);
if(actors.size()>256)throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor registry exceeds bounded capacity");
for(int i=0;i<actors.size();i++) {
var actor=StoryActorRecord.load(actors.getCompound(i));
if(data.actors.putIfAbsent(actor.storyId,actor)!=null)throw new IllegalStateException("MANUAL_DIAGNOSTIC Duplicate story actor "+actor.storyId);
}
if(!data.save(new CompoundTag(),registry).equals(root))data.setDirty();
return data;
}

private void verifySourceUnchanged() throws java.io.IOException {
if(storageFile==null)return;
if(expectedHash==null?!java.nio.file.Files.notExists(storageFile):!expectedHash.equals(StoryStorage.digest(java.nio.file.Files.readAllBytes(storageFile))))
throw new java.io.IOException("source_changed_since_load");
}

private ReturnNetworkBoundary validateDirectTransition(ReturnNetworkState before, ReturnNetworkState after) {
if(before.save().equals(after.save()))return ReturnNetworkBoundary.NO_CHANGE;

ReturnNetworkState expected;
ReturnNetworkBoundary boundary;
try {
switch(before.stage) {
case NOT_STARTED -> {
expected=before.discover(new ReturnNetworkState.Prerequisite(4,true,true,true),after.instance,after.location);
boundary=ReturnNetworkBoundary.DISCOVERED;
}
case DISCOVERED -> {
expected=before.join(Objects.requireNonNull(after.participant,"participant"));
boundary=ReturnNetworkBoundary.NPC_JOINED;
}
case NPC_JOINED -> {
expected=before.observePattern();
boundary=ReturnNetworkBoundary.SIGNAL_OBSERVED;
}
case SIGNAL_OBSERVED -> {
expected=before.intervene();
boundary=ReturnNetworkBoundary.INTERVENTION_COMMITTED;
}
case INTERVENTION_COMMITTED -> {
if(before.presentation==ReturnNetworkState.Presentation.NONE) {
expected=before.beginPresentation();
boundary=ReturnNetworkBoundary.PRESENTATION_STARTED;
} else if(before.presentation==ReturnNetworkState.Presentation.PRESENTING) {
expected=before.observeResponse();
boundary=ReturnNetworkBoundary.RESPONSE_OBSERVED;
} else throw new IllegalStateException("unexpected_intervention_presentation="+before.presentation);
}
case RESPONSE_OBSERVED -> {
if(!before.facts.contains(ReturnNetworkState.Fact.SHARED_EXPERIENCE)) {
expected=before.shareExperience();
boundary=ReturnNetworkBoundary.SHARED_EXPERIENCE;
} else {
expected=before.complete();
boundary=ReturnNetworkBoundary.COMPLETED;
}
}
case COMPLETED -> throw new IllegalStateException("completed_is_terminal");
default -> throw new IllegalStateException("unknown_return_network_stage");
}
} catch(RuntimeException ex) {
throw new IllegalStateException("RETURN_NETWORK_ILLEGAL_TRANSITION "+ex.getMessage(),ex);
}
if(!expected.save().equals(after.save()))
throw new IllegalStateException("RETURN_NETWORK_ILLEGAL_TRANSITION expected="+boundary);
return boundary;
}

public synchronized ReturnNetworkCommit commitReturnNetwork(ReturnNetworkState expected,ReturnNetworkState draft,HolderLookup.Provider registry) {
if(!storySaveWritable)throw new IllegalStateException("STORY_WRITE_BLOCKED previous_guard_failure");
Objects.requireNonNull(expected,"expected");
Objects.requireNonNull(draft,"draft");
if(storageFile==null)throw new IllegalStateException("RETURN_NETWORK_STORAGE_NOT_BOUND");
if(!returnNetwork.save().equals(expected.save()))throw new IllegalStateException("RETURN_NETWORK_STALE_DRAFT");

var validated=ReturnNetworkState.load(draft.save());
var boundary=validateDirectTransition(returnNetwork,validated);
try {
verifySourceUnchanged();
} catch(java.io.IOException ex) {
storySaveWritable=false;
throw new java.io.UncheckedIOException("STORY_WRITE_BLOCKED original preserved",ex);
}

if(boundary==ReturnNetworkBoundary.NO_CHANGE)
return new ReturnNetworkCommit(boundary,returnNetwork,expectedHash,false);

var envelope=new CompoundTag();
envelope.put("data",savePayload(new CompoundTag(),registry,validated));
NbtUtils.addCurrentDataVersion(envelope);
try {
storyWriter.write(envelope,storageFile);
String durableHash=StoryStorage.digest(java.nio.file.Files.readAllBytes(storageFile));
expectedHash=durableHash;
returnNetwork=validated;
setDirty(false);
return new ReturnNetworkCommit(boundary,validated,durableHash,true);
} catch(java.io.IOException ex) {
storySaveWritable=false;
throw new java.io.UncheckedIOException("RETURN_NETWORK_COMMIT_FAILED original_or_atomic_target_preserved",ex);
}
}

@Override public synchronized void save(java.io.File file,HolderLookup.Provider registry) {
if(!storySaveWritable)throw new IllegalStateException("STORY_WRITE_BLOCKED previous_guard_failure");
if(!isDirty())return;
if(storageFile!=null)try {
if(!storageFile.equals(file.toPath().toAbsolutePath().normalize()))throw new java.io.IOException("storage_target_mismatch");
verifySourceUnchanged();
} catch(java.io.IOException ex){storySaveWritable=false;throw new java.io.UncheckedIOException("STORY_WRITE_BLOCKED original preserved",ex);}
var root=new CompoundTag();root.put("data",save(new CompoundTag(),registry));
NbtUtils.addCurrentDataVersion(root);
try {
storyWriter.write(root,file.toPath());
if(storageFile!=null)expectedHash=StoryStorage.digest(java.nio.file.Files.readAllBytes(storageFile));
setDirty(false);
} catch(java.io.IOException ex) {
storySaveWritable=false;
throw new java.io.UncheckedIOException("Story checkpoint commit failed: "+file,ex);
}
}

@Override public synchronized CompoundTag save(CompoundTag root, HolderLookup.Provider registry) {
return savePayload(root,registry,returnNetwork);
}

private CompoundTag savePayload(CompoundTag root,HolderLookup.Provider registry,ReturnNetworkState returnNetworkSnapshot) {
root.putInt("schema", SCHEMA);
var list = new ListTag();
players.forEach((id, e) -> {
var t = new CompoundTag();
t.putUUID("id", id); t.putInt("clues", e.progress.clues());
t.putLong("next", e.progress.nextEventTick()); t.putInt("events", e.progress.eventsPlayed());
t.putBoolean("complete", e.progress.encounterComplete()); t.putLong("readingUntil", e.readingUntil);
if (e.station != null) t.putLong("station", e.station.asLong());
if (e.encounter != null) t.putUUID("encounter", e.encounter);
t.putBoolean("cityVisited",e.cityVisited);t.putInt("cityClues",e.cityClues&7);
t.putBoolean("cityShockTriggered",e.cityShockTriggered);
t.put("investigation",e.investigation.save());
var scenes=new ListTag();e.scenes.values().forEach(r->scenes.add(r.save()));t.put("scenes",scenes);
if(e.returnPosition!=null)t.putLong("returnPosition",e.returnPosition.asLong());
t.putFloat("returnYaw",e.returnYaw);t.putFloat("returnPitch",e.returnPitch);t.putLong("transitAfter",e.transitAfter);
list.add(t);
});
root.put("players", list);
root.putInt("actorSchema",1);
var actorList=new ListTag();actors.values().forEach(a->actorList.add(a.save()));root.put("actors",actorList);
if(returnNetworkSnapshot.stage==ReturnNetworkState.Stage.NOT_STARTED)root.remove("returnNetwork");
else root.put("returnNetwork",returnNetworkSnapshot.save());
return root;
}
}
