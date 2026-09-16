package io.github.whileaway;

import io.github.whileaway.core.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Overworld SavedData survives death, logout and dimension travel. Records are player-isolated. */
public final class NarrativeData extends SavedData {
    public static final int SCHEMA = 4;
    private java.nio.file.Path storageFile;
    private String expectedHash;
    private boolean storySaveWritable=true;
    void bindStorage(java.nio.file.Path file,String hash){storageFile=file;expectedHash=hash;}
    public boolean storySaveWritable(){return storySaveWritable;}
    void blockStoryWrites(){storySaveWritable=false;}
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
    public void discover(UUID id, Clue clue) {
        var e = entry(id); e.progress = e.progress.discover(clue); setDirty();
    }
    public static NarrativeData get(MinecraftServer server) {
        return StoryStorage.open(server.overworld().getDataStorage(),server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data"),
            server.registryAccess(),server.levelKeys().stream().map(k->k.location().toString()).collect(java.util.stream.Collectors.toSet()));
    }
    static NarrativeData load(CompoundTag root, HolderLookup.Provider registry) {
        if (root.getInt("schema") > SCHEMA) throw new IllegalStateException("Story save uses a newer schema; restore the matching mod version.");
        var data = new NarrativeData();
        var list = root.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var t = list.getCompound(i);
            if (!t.hasUUID("id")) continue;
            var e = data.entry(t.getUUID("id"));
            e.progress = new Progress(t.getInt("clues"), t.getLong("next"), t.getInt("events"), t.getBoolean("complete"));
            if (t.contains("station")) e.station = BlockPos.of(t.getLong("station"));
            if (t.hasUUID("encounter")) e.encounter = t.getUUID("encounter");
            e.readingUntil = t.getLong("readingUntil");
            e.cityVisited=t.getBoolean("cityVisited");e.cityClues=t.getInt("cityClues")&7;
            e.cityShockTriggered=t.getBoolean("cityShockTriggered");
            e.investigation=CityInvestigation.load(t.getCompound("investigation"),e.cityClues,t.contains("investigation",Tag.TAG_COMPOUND));
            e.cityClues=e.investigation.evidence();
            var scenes=t.getList("scenes",Tag.TAG_COMPOUND);
            for(int j=0;j<scenes.size();j++) {
                var scene=SceneRecord.load(scenes.getCompound(j),t.getUUID("id"));
                if(e.scenes.putIfAbsent(scene.kind,scene)!=null)throw new IllegalStateException("Duplicate persisted scene kind="+scene.kind);
            }
            // Legacy schema 3 recorded started-as-complete; preserve that fact rather than surprise-replay old worlds.
            if(e.cityShockTriggered&&!e.scenes.containsKey(3)) {
                var legacy=SceneRecord.migrated(3,CityHorror.STAGE,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,3,0);e.scenes.put(3,legacy);
            }
            if(root.getInt("schema")<4) {
                if(e.progress.has(Clue.SIGNAL_RESTORED)&&e.station!=null&&!e.scenes.containsKey(1)) {
                    var legacy=SceneRecord.migrated(1,e.station,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,127,0);e.scenes.put(1,legacy);
                }
                if(e.cityVisited&&!e.scenes.containsKey(2)) {
                    var legacy=SceneRecord.migrated(2,CityDistrict.ARRIVAL,t.getUUID("id"));legacy.progress.restore("COMPLETED",4,127,0);e.scenes.put(2,legacy);
                }
            }
            if(t.contains("returnPosition"))e.returnPosition=BlockPos.of(t.getLong("returnPosition"));
            e.returnYaw=t.getFloat("returnYaw");e.returnPitch=t.getFloat("returnPitch");
            e.transitAfter=Math.max(0,t.getLong("transitAfter"));
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
    /** NeoForge's default SavedData.save queues an asynchronous write and clears dirty immediately.
     * A checkpoint acknowledgement must instead return only after OUR atomic NBT write finishes.
     * Other SavedData and vanilla chunk/player writes keep their normal behavior. */
    @Override public synchronized void save(java.io.File file,HolderLookup.Provider registry) {
        if(!storySaveWritable)throw new IllegalStateException("STORY_WRITE_BLOCKED previous_guard_failure");
        if(!isDirty())return;
        if(storageFile!=null)try {
            if(!storageFile.equals(file.toPath().toAbsolutePath().normalize()))throw new java.io.IOException("storage_target_mismatch");
            if(expectedHash==null?!java.nio.file.Files.notExists(storageFile):!expectedHash.equals(StoryStorage.digest(java.nio.file.Files.readAllBytes(storageFile))))
                throw new java.io.IOException("source_changed_since_load");
        } catch(java.io.IOException ex){storySaveWritable=false;throw new java.io.UncheckedIOException("STORY_WRITE_BLOCKED original preserved",ex);}
        var root=new CompoundTag();root.put("data",save(new CompoundTag(),registry));
        NbtUtils.addCurrentDataVersion(root);
        try {
            net.neoforged.neoforge.common.IOUtilities.writeNbtCompressed(root,file.toPath());
            if(storageFile!=null)expectedHash=StoryStorage.digest(java.nio.file.Files.readAllBytes(storageFile));
            setDirty(false);
        } catch(java.io.IOException ex) {
            // Keep dirty and report failure to the caller: no false durable acknowledgement.
            throw new java.io.UncheckedIOException("Story checkpoint commit failed: "+file,ex);
        }
    }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registry) {
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
            // Legacy direct setters remain readable, but never manufacture an unconfirmed journal.
            t.put("investigation",e.investigation.save());
            var scenes=new ListTag();e.scenes.values().forEach(r->scenes.add(r.save()));t.put("scenes",scenes);
            if(e.returnPosition!=null)t.putLong("returnPosition",e.returnPosition.asLong());
            t.putFloat("returnYaw",e.returnYaw);t.putFloat("returnPitch",e.returnPitch);t.putLong("transitAfter",e.transitAfter);
            list.add(t);
        });
        root.put("players", list);
        root.putInt("actorSchema",1);var actorList=new ListTag();actors.values().forEach(a->actorList.add(a.save()));root.put("actors",actorList);
        return root;
    }
}
