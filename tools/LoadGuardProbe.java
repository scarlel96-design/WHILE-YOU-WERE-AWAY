package io.github.whileaway;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Actual storage/NBT/atomic-write regression; no real client is implied by this probe. */
public final class LoadGuardProbe {
    private static int assertions,cases;
    private static final Set<String> DIMS=Set.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end");
    private static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
    private static DimensionDataStorage storage(Path p){return new DimensionDataStorage(p.toFile(),null,null);}
    private static CompoundTag actor(CompoundTag r){return r.getCompound("data").getList("actors",10).getCompound(0);}
    private static void write(Path dir,CompoundTag root)throws Exception{Files.createDirectories(dir);NbtIo.writeCompressed(root,dir.resolve("whileaway_story.dat"));}
    public static void main(String[] args)throws Exception {
        if(args[0].equals("--mutate")) {
            Path path=Path.of(args[1]);var root=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());
            switch(args[2]) {
                case "Recover"->actor(root).getCompound("snapshot").remove("Motion");
                case "Blocked"->actor(root).remove("entityUUID");
                case "Malformed"->actor(root).putString("entityUUID","not-a-uuid");
                case "Unsupported"->root.getCompound("data").putInt("actorSchema",2);
                default->throw new IllegalArgumentException("Unknown fixture mutation");
            }
            NbtIo.writeCompressed(root,path);System.out.println("MUTATED copied fixture "+args[2]);System.exit(0);
        }
        SharedConstants.tryDetectVersion();Path source=Path.of(args[0]),base=Path.of(args[1]);Files.createDirectories(base);
        byte[] original=Files.readAllBytes(source);var valid=NbtIo.readCompressed(source,NbtAccounter.unlimitedHeap());
        Map<String,Consumer<CompoundTag>> bad=new LinkedHashMap<>();
        bad.put("missing_uuid",r->actor(r).remove("entityUUID"));
        bad.put("malformed_uuid",r->actor(r).putString("entityUUID","broken-uuid"));
        bad.put("negative_generation",r->actor(r).putInt("generation",-4));
        bad.put("missing_generation",r->actor(r).remove("generation"));
        bad.put("missing_storyId",r->actor(r).remove("storyId"));
        bad.put("missing_eventId",r->actor(r).remove("ownerEventId"));
        bad.put("missing_instance",r->actor(r).remove("instanceId"));
        bad.put("unknown_lifecycle",r->actor(r).putString("lifecycleState","INVALID"));
        bad.put("invalid_checkpoint",r->actor(r).putInt("checkpoint",77));
        bad.put("wrong_dimension",r->actor(r).putString("dimension","minecraft:missing_dimension"));
        bad.put("missing_position",r->actor(r).remove("position"));
        bad.put("malformed_snapshot",r->actor(r).putString("snapshot","broken"));
        bad.put("missing_snapshot",r->actor(r).remove("snapshot"));
        bad.put("missing_snapshot_position",r->actor(r).getCompound("snapshot").remove("Pos"));
        bad.put("duplicate_actor",r->r.getCompound("data").getList("actors",10).add(actor(r).copy()));
        bad.put("unsupported_actorSchema",r->r.getCompound("data").putInt("actorSchema",2));
        bad.put("unsupported_saveFormat",r->r.getCompound("data").putInt("schema",5));
        bad.put("malformed_actor_list",r->r.getCompound("data").putString("actors","broken"));
        for(int repeat=0;repeat<20;repeat++) {
            for(var test:bad.entrySet()) {
                Path dir=base.resolve(test.getKey()+"-"+repeat);var root=valid.copy();test.getValue().accept(root);write(dir,root);
                byte[] before=Files.readAllBytes(dir.resolve("whileaway_story.dat"));var store=storage(dir);
                boolean blocked=false;try{StoryStorage.open(store,dir,null,DIMS);}catch(StoryStorage.Blocked ex){
                    blocked=true;check(!ex.status.writable(),test.getKey()+" write guard");
                    check(ex.status.outcome()==(test.getKey().startsWith("unsupported")?StoryStorage.Outcome.UNSUPPORTED:StoryStorage.Outcome.CORRUPT),test.getKey()+" classification");
                }
                check(blocked,test.getKey()+" must not return empty campaign");
                store.save(); // Vanilla/NeoForge SavedData autosave dispatch, with no poisoned object registered.
                check(Arrays.equals(before,Files.readAllBytes(dir.resolve("whileaway_story.dat"))),test.getKey()+" autosave preservation");
                check(Arrays.equals(before,Files.readAllBytes(dir.resolve("whileaway-quarantine/"+StoryStorage.digest(before)+".dat"))),"backup exact");cases++;
            }
            Path truncated=base.resolve("truncated-"+repeat);Files.createDirectories(truncated);
            byte[] damaged=Arrays.copyOf(original,original.length/2);Files.write(truncated.resolve("whileaway_story.dat"),damaged);
            var broken=storage(truncated);boolean blocked=false;try{StoryStorage.open(broken,truncated,null,DIMS);}catch(StoryStorage.Blocked e){blocked=true;}
            check(blocked,"partial NBT blocked");broken.save();check(Arrays.equals(damaged,Files.readAllBytes(truncated.resolve("whileaway_story.dat"))),"partial preserved");cases++;
            Path absent=base.resolve("absent-"+repeat);Files.createDirectories(absent);var emptyStore=storage(absent);
            var fresh=StoryStorage.open(emptyStore,absent,null,DIMS);check(StoryStorage.status(emptyStore).outcome()==StoryStorage.Outcome.ABSENT,"absent classified");
            fresh.setDirty();fresh.save(absent.resolve("whileaway_story.dat").toFile(),null);
            check(StoryStorage.open(storage(absent),absent,null,DIMS).actors.isEmpty(),"new saved/loaded");cases++;
            Path normal=base.resolve("normal-"+repeat);write(normal,valid);var n=StoryStorage.open(storage(normal),normal,null,DIMS);
            check(n.actors.size()==1,"normal actor preserved");n.setDirty();n.save(normal.resolve("whileaway_story.dat").toFile(),null);
            check(StoryStorage.open(storage(normal),normal,null,DIMS).actors.size()==1,"normal roundtrip");cases++;
            Path recover=base.resolve("recover-"+repeat);var fix=valid.copy();actor(fix).getCompound("snapshot").remove("Motion");write(recover,fix);
            var rs=storage(recover);var repaired=StoryStorage.open(rs,recover,null,DIMS);
            check(StoryStorage.status(rs).outcome()==StoryStorage.Outcome.RECOVERABLE_CORRUPTION,"recover classified");
            var a=repaired.actors.values().iterator().next();check(a.generation==actor(valid).getInt("generation"),"recovery keeps generation");
            check(a.entityId.equals(actor(valid).getUUID("entityUUID")),"recovery not a new actor");
            var re=storage(recover);check(StoryStorage.open(re,recover,null,DIMS).actors.size()==1&&StoryStorage.status(re).outcome()==StoryStorage.Outcome.LOADED,"recovery save/reload");cases++;
            Path changed=base.resolve("external-change-"+repeat);write(changed,valid);var guarded=StoryStorage.open(storage(changed),changed,null,DIMS);
            Files.write(changed.resolve("whileaway_story.dat"),damaged);guarded.setDirty();boolean denied=false;
            try{guarded.save(changed.resolve("whileaway_story.dat").toFile(),null);}catch(java.io.UncheckedIOException expected){denied=true;}
            check(denied&&!guarded.storySaveWritable(),"loaded write guard");check(Arrays.equals(damaged,Files.readAllBytes(changed.resolve("whileaway_story.dat"))),"external changed file preserved");cases++;
            Path partial=base.resolve("snapshot-partial-"+repeat);var partialTag=valid.copy();actor(partialTag).getCompound("snapshot").remove("Motion");write(partial,partialTag);
            var partialData=StoryStorage.open(storage(partial),partial,null,DIMS);var partialActor=partialData.actors.values().iterator().next();
            check(partialActor.entityId.equals(actor(valid).getUUID("entityUUID"))&&partialActor.checkpoint.checkpoint==actor(valid).getInt("checkpoint"),"optional snapshot Motion does not reset identity/facts");cases++;
            Path unreadable=base.resolve("not-regular-"+repeat);Files.createDirectories(unreadable.resolve("whileaway_story.dat"));
            boolean refused=false;try{StoryStorage.open(storage(unreadable),unreadable,null,DIMS);}catch(StoryStorage.Blocked e){refused=true;}
            check(refused&&Files.isDirectory(unreadable.resolve("whileaway_story.dat")),"existing non-readable-as-file is not absent");cases++;
            for(int version=1;version<=4;version++) {
                var legacy=valid.copy();var data=legacy.getCompound("data");data.putInt("schema",version);data.remove("actorSchema");data.remove("actors");
                for(var p:data.getList("players",10))((CompoundTag)p).remove("encounter");
                Path dir=base.resolve("legacy-"+version+"-"+repeat);write(dir,legacy);var migrated=StoryStorage.open(storage(dir),dir,null,DIMS);
                var one=migrated.save(new CompoundTag(),null);var two=NarrativeData.load(one.copy(),null).save(new CompoundTag(),null);
                check(one.equals(two)&&one.getInt("schema")==4&&one.getInt("actorSchema")==1,"deterministic migration "+version);cases++;
            }
        }
        check(Arrays.equals(original,Files.readAllBytes(source)),"original source preserved");
        System.out.println("PASS load guard cases="+cases+" assertions="+assertions+" repeats=20; blocked matrix="+(bad.size()+1)+"; sourcePreserved=true; actualClient=false");
        System.exit(0);
    }
}
