package io.github.whileaway;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Explicit world-local load boundary. A rejected file is never registered as empty SavedData. */
public final class StoryStorage {
    public static final String KEY="whileaway_story";
    public enum Outcome { ABSENT, LOADED, RECOVERABLE_CORRUPTION, CORRUPT, UNSUPPORTED }
    public enum Recovery { SAFE_AUTO_RECOVER, RECOVER_WITH_WARNING, MANUAL_DIAGNOSTIC }
    public record Status(Outcome outcome,Recovery recovery,int saveFormat,int actorSchema,int actorCount,
                         boolean writable,String originalHash,String detail) {
        public String diagnostic(){return "STORY_LOAD outcome="+outcome+" recovery="+recovery+" saveFormat="+saveFormat
            +" actorSchema="+actorSchema+" actors="+actorCount+" writeBlocked="+!writable+" "+detail;}
    }
    public static final class Blocked extends IllegalStateException {
        public final Status status;
        Blocked(Status status,Throwable cause){super(status.diagnostic(),cause);this.status=status;}
    }
    private record Slot(NarrativeData data,Status status) {}
    private static final Map<DimensionDataStorage,Slot> states=new WeakHashMap<>();
    private StoryStorage() {}
    public static boolean available(net.minecraft.server.MinecraftServer server) {
        try { NarrativeData.get(server); return true; } catch(Blocked rejected) { return false; }
    }
    public static synchronized void block(net.minecraft.server.MinecraftServer server,String reason) {
        var storage=server.overworld().getDataStorage();var slot=states.get(storage);
        if(slot==null)return;
        if(slot.data!=null)slot.data.blockStoryWrites();
        var old=slot.status;
        states.put(storage,new Slot(slot.data,new Status(Outcome.CORRUPT,Recovery.MANUAL_DIAGNOSTIC,
            old.saveFormat,old.actorSchema,old.actorCount,false,old.originalHash,reason)));
        com.mojang.logging.LogUtils.getLogger().error(reason);
    }
    /** Explicit recovery only. Callers must provide live single-candidate evidence, never a random UUID. */
    static synchronized NarrativeData recoveryRead(net.minecraft.server.MinecraftServer server,Map<String,UUID> bindings,boolean inspect) {
        var storage=server.overworld().getDataStorage();
        return openInternal(storage,server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data"),
            server.registryAccess(),server.levelKeys().stream().map(k->k.location().toString()).collect(java.util.stream.Collectors.toSet()),bindings,inspect);
    }
    public static synchronized Status status(DimensionDataStorage storage){
        var s=states.get(storage);if(s==null)return null;
        if(s.data!=null&&!s.data.storySaveWritable())return new Status(Outcome.CORRUPT,Recovery.MANUAL_DIAGNOSTIC,
            s.status.saveFormat,s.status.actorSchema,s.status.actorCount,false,s.status.originalHash,"LOAD_CORRUPT_BLOCKED write_guard_poisoned "+s.status.detail);
        return s.status;
    }

    public static synchronized NarrativeData open(DimensionDataStorage storage,Path directory,HolderLookup.Provider registry,Set<String> dimensions) {
        return openInternal(storage,directory,registry,dimensions,Map.of(),false);
    }
    private static NarrativeData openInternal(DimensionDataStorage storage,Path directory,HolderLookup.Provider registry,
            Set<String> dimensions,Map<String,UUID> bindings,boolean inspect) {
        var cached=states.get(storage);
        if(cached!=null&&bindings.isEmpty()){if(cached.data==null||!cached.data.storySaveWritable())throw new Blocked(status(storage),null);return cached.data;}
        Path file=directory.resolve(KEY+".dat").toAbsolutePath().normalize();
        int format=-1,schema=-1,count=-1;String hash="unavailable";byte[] original=null;
        Outcome failure=Outcome.CORRUPT;
        try {
            if(Files.notExists(file)) {
                if(!bindings.isEmpty())throw new IOException("recovery_source_disappeared");
                var data=new NarrativeData();data.bindStorage(file,null);
                var status=new Status(Outcome.ABSENT,Recovery.SAFE_AUTO_RECOVER,4,1,0,true,"absent","LOAD_OK new_world");
                states.put(storage,new Slot(data,status));storage.set(KEY,data);return data;
            }
            // exists=false also covers access failures; only notExists=true permits creation.
            if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("field=file not_regular_or_unreadable");
            if(Files.size(file)>16*1024*1024)throw new IOException("field=file exceeds_bounded_input");
            original=Files.readAllBytes(file);hash=digest(original);
            if(!bindings.isEmpty()&&(cached==null||cached.data!=null||!hash.equals(cached.status.originalHash)))
                throw new IOException("recovery_source_changed_or_not_blocked");
            CompoundTag envelope;
            try(var bytes=new ByteArrayInputStream(original)) {
                if(original.length>=2&&(original[0]&255)==31&&(original[1]&255)==139)
                    envelope=NbtIo.readCompressed(bytes,NbtAccounter.create(64*1024*1024));
                else try(var input=new DataInputStream(bytes)){envelope=NbtIo.read(input,NbtAccounter.create(64*1024*1024));}
            }
            if(envelope==null||!envelope.contains("data",Tag.TAG_COMPOUND))throw new IllegalStateException("field=data invalid_envelope");
            var root=envelope.getCompound("data");
            if(!root.contains("schema",Tag.TAG_INT))throw new IllegalStateException("field=schema missing_or_wrong_type");
            format=root.getInt("schema");schema=root.contains("actorSchema")?root.getInt("actorSchema"):0;
            if(format<1||format>4||schema<0||schema>1){failure=Outcome.UNSUPPORTED;throw new IllegalStateException("field=schema unsupported_version");}
            if(root.contains("actorSchema")&&!root.contains("actorSchema",Tag.TAG_INT))throw new IllegalStateException("field=actorSchema wrong_type");
            listType(root,"players",Tag.TAG_COMPOUND);listType(root,"actors",Tag.TAG_COMPOUND);
            var playerIds=new HashSet<UUID>();
            for(var raw:root.getList("players",Tag.TAG_COMPOUND)) {
                var player=(CompoundTag)raw;
                if(!player.hasUUID("id")||!playerIds.add(player.getUUID("id")))throw new IllegalStateException("field=players.id invalid_or_duplicate");
                if(player.contains("encounter")&&!player.hasUUID("encounter"))throw new IllegalStateException("field=players.encounter malformed");
            }
            var actors=root.getList("actors",Tag.TAG_COMPOUND);count=actors.size();
            if(count>256)throw new IllegalStateException("field=actors capacity_exceeded");
            var repaired=root.copy();var notes=new ArrayList<String>();var seen=new HashSet<String>();var ids=new HashSet<UUID>();
            for(var raw:repaired.getList("actors",Tag.TAG_COMPOUND)) {
                var t=(CompoundTag)raw;
                String context="storyId="+t.getString("storyId")+" eventId="+t.getString("ownerEventId")
                    +" instanceId="+(t.hasUUID("instanceId")?t.getUUID("instanceId"):"invalid")+" generation="+t.getInt("generation");
                try {
                    if(!t.contains("entityUUID")&&bindings.containsKey(t.getString("storyId"))) {
                        t.putUUID("entityUUID",bindings.get(t.getString("storyId")));
                        notes.add(context+" field=entityUUID exact_live_candidate_relinked");
                    }
                    for(String key:List.of("storyId","ownerEventId","spawnRole","entityType","dimension","lifecycleState","state"))
                        if(!t.contains(key,Tag.TAG_STRING)||t.getString(key).isBlank())throw new IllegalStateException("field="+key+" missing_or_wrong_type");
                    for(String key:List.of("entityUUID","instanceId"))if(!t.hasUUID(key))throw new IllegalStateException("field="+key+" missing_or_malformed");
                    if(t.contains("owner")&&!t.hasUUID("owner"))throw new IllegalStateException("field=owner malformed");
                    if(!seen.add(t.getString("storyId"))||!ids.add(t.getUUID("entityUUID")))throw new IllegalStateException("field=identity duplicate_actor");
                    if(ResourceLocation.tryParse(t.getString("dimension"))==null||(dimensions!=null&&!dimensions.contains(t.getString("dimension"))))
                        throw new IllegalStateException("field=dimension invalid_binding");
                    if(!t.contains("position",Tag.TAG_LONG))throw new IllegalStateException("field=position missing_or_wrong_type");
                    if(t.contains("snapshot")&&!t.contains("snapshot",Tag.TAG_COMPOUND))throw new IllegalStateException("field=snapshot wrong_type");
                    // A snapshot alone cannot prove that its generation is the latest epoch.
                    // Do not downgrade the admission fence before a live-entity/journal recovery exists.
                    if(!t.contains("generation"))throw new IllegalStateException("field=generation missing_requires_live_reconciliation");
                    if(!t.contains("generation",Tag.TAG_INT)||t.getInt("generation")<0)throw new IllegalStateException("field=generation invalid");
                    if(!t.contains("checkpoint",Tag.TAG_INT)||t.getInt("checkpoint")<0||t.getInt("checkpoint")>4)
                        throw new IllegalStateException("field=checkpoint invalid");
                    StoryActorRecord.Lifecycle.valueOf(t.getString("lifecycleState"));
                    io.github.whileaway.core.EventCheckpoint.State.valueOf(t.getString("state"));
                    if(!t.getString("lifecycleState").equals("RETIRED")) {
                        var pos=t.getCompound("snapshot").getList("Pos",Tag.TAG_DOUBLE);
                        if(pos.size()!=3)throw new IllegalStateException("field=snapshot.Pos missing_or_malformed");
                        for(int axis=0;axis<3;axis++)if(!Double.isFinite(pos.getDouble(axis)))throw new IllegalStateException("field=snapshot.Pos non_finite");
                    }
                    listType(t,"facts",Tag.TAG_STRING);
                    var parsed=StoryActorRecord.load(t);
                    if(parsed.checkpoint.state==io.github.whileaway.core.EventCheckpoint.State.FAILED_RECOVERABLE)
                        throw new IllegalStateException("field=checkpoint "+parsed.checkpoint.reason);
                    var snapshot=t.getCompound("snapshot");
                    if(!snapshot.isEmpty()&&!snapshot.contains("Motion")) {
                        var motion=new ListTag();for(int axis=0;axis<3;axis++)motion.add(DoubleTag.valueOf(0));
                        snapshot.put("Motion",motion);notes.add(context+" field=snapshot.Motion restored_stationary_runtime_default");
                    }
                } catch(RuntimeException ex){throw new IllegalStateException(context+" "+ex.getMessage(),ex);}
            }
            var data=NarrativeData.load(repaired,registry);
            // Validate the durable chase link without spawning, loading chunks or choosing entity candidates.
            for(var actor:data.actors.values())if(actor.eventId.equals(StoryActors.CHASE)&&actor.owner!=null) {
                var p=data.entry(actor.owner);
                if(actor.lifecycle!=StoryActorRecord.Lifecycle.RETIRED&&!actor.entityId.equals(p.encounter))
                    throw new IllegalStateException("storyId="+actor.storyId+" field=encounter journal_identity_mismatch");
                if(actor.checkpoint.state==io.github.whileaway.core.EventCheckpoint.State.COMPLETED&&!p.progress.encounterComplete())
                    throw new IllegalStateException("storyId="+actor.storyId+" field=complete journal_fact_mismatch");
            }
            for(var actor:data.actors.values())if(NpcEvents.isNpc(actor)) {
                var issues=NpcState.integrity(actor);
                if(!issues.isEmpty())throw new IllegalStateException(String.join(";",issues));
            }
            if(inspect)return data; // Private candidate, never installed or written.
            data.bindStorage(file,hash);
            if(!notes.isEmpty()) {
                backup(file,original,hash); // Durable original first; no live file edits before semantic checks.
                data.setDirty();data.save(file.toFile(),registry); // Explicit guarded atomic commit.
            }
            var status=new Status(notes.isEmpty()?Outcome.LOADED:Outcome.RECOVERABLE_CORRUPTION,
                notes.isEmpty()?Recovery.SAFE_AUTO_RECOVER:Recovery.RECOVER_WITH_WARNING,format,schema,count,true,hash,
                notes.isEmpty()?"LOAD_OK":"LOAD_RECOVERED "+String.join(";",notes));
            states.put(storage,new Slot(data,status));storage.set(KEY,data);
            if(!notes.isEmpty())com.mojang.logging.LogUtils.getLogger().warn(status.diagnostic());
            return data;
        } catch(Exception ex) {
            if(inspect)throw new IllegalStateException("RECOVERY_PREFLIGHT_REJECTED "+ex.getMessage(),ex);
            String backup="original_untouched";
            if(original!=null)try{backup(file,original,hash);backup="original_quarantined";}catch(Exception e){backup="backup_failed_original_untouched:"+e.getClass().getSimpleName();}
            var status=new Status(failure,Recovery.MANUAL_DIAGNOSTIC,format,schema,count,false,hash,
                "phase=load "+backup+" reason="+ex.getMessage());
            states.put(storage,new Slot(null,status));
            com.mojang.logging.LogUtils.getLogger().error(status.diagnostic());
            throw new Blocked(status,ex);
        }
    }
    private static void listType(CompoundTag t,String key,int type) {
        if(!t.contains(key))return;
        if(!(t.get(key) instanceof ListTag list)||(!list.isEmpty()&&list.getElementType()!=type))throw new IllegalStateException("field="+key+" malformed_list");
    }
    private static void backup(Path file,byte[] bytes,String hash)throws IOException {
        Path dir=file.getParent().resolve("whileaway-quarantine");
        if(Files.isSymbolicLink(dir))throw new IOException("quarantine_symlink");Files.createDirectories(dir);
        Path target=dir.resolve(hash+".dat");
        if(Files.exists(target)){if(!digest(Files.readAllBytes(target)).equals(hash))throw new IOException("quarantine_hash_mismatch");}
        else Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        if(!digest(Files.readAllBytes(target)).equals(hash))throw new IOException("quarantine_verification_failed");
    }
    public static String digest(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
}
