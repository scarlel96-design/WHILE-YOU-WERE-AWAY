package io.github.whileaway;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Real StoryStorage calls on isolated NBT fixtures, not a real-client claim. */
public final class NpcFactProbe {
    private static final Set<String> DIMS=Set.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end","whileaway:quiet_city");
    private static CompoundTag actor(CompoundTag root){return root.getCompound("data").getList("actors",10).getCompound(0);}
    private static void remove(CompoundTag t,String prefix){t.getList("facts",8).removeIf(x->x.getAsString().startsWith(prefix));}
    private static void add(CompoundTag t,String fact){t.getList("facts",8).add(StringTag.valueOf(fact));}
    private static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception {
        SharedConstants.tryDetectVersion();Path source=Path.of(args[0]),base=Path.of(args[1]);byte[] original=Files.readAllBytes(source);
        var preparing=NbtIo.readCompressed(source,NbtAccounter.unlimitedHeap());
        var complete=preparing.copy();var t=actor(complete);
        t.putInt("checkpoint",4);t.putString("state","COMPLETED");t.putString("lifecycleState","ACTIVE");remove(t,"npc.work=");
        for(String f:List.of("npc.met="+UUID.randomUUID(),NpcState.DIALOGUE,NpcState.ARRIVED,NpcState.OBSERVED,NpcState.SETTLED,NpcState.LINK,"npc.work=maintain_return_light"))add(t,f);
        Map<String,Consumer<CompoundTag>> invalid=new LinkedHashMap<>();
        invalid.put("missing_home",a->remove(a,"npc.home="));
        invalid.put("multiple_home",a->add(a,"npc.home=123"));
        invalid.put("malformed_home",a->{remove(a,"npc.home=");add(a,"npc.home=not-a-position");});
        invalid.put("missing_dialogue",a->remove(a,"npc.dialogue="));
        invalid.put("missing_work",a->remove(a,"npc.work="));
        invalid.put("premature_link",a->{a.putInt("checkpoint",1);a.putString("state","PHASE_1");});
        int cases=0;
        for(var entry:invalid.entrySet()) {
            var root=complete.copy();entry.getValue().accept(actor(root));Path dir=base.resolve(entry.getKey());Files.createDirectories(dir);
            Path f=dir.resolve("whileaway_story.dat");NbtIo.writeCompressed(root,f);byte[] before=Files.readAllBytes(f);
            var store=new DimensionDataStorage(dir.toFile(),null,null);boolean blocked=false;
            try{StoryStorage.open(store,dir,null,DIMS);}catch(StoryStorage.Blocked ex){blocked=!ex.status.writable()&&ex.status.outcome()==StoryStorage.Outcome.CORRUPT;}
            check(blocked,"NPC semantic corruption must fail closed: "+entry.getKey());store.save();
            check(Arrays.equals(before,Files.readAllBytes(f)),"write protection "+entry.getKey());
            check(Arrays.equals(before,Files.readAllBytes(dir.resolve("whileaway-quarantine/"+StoryStorage.digest(before)+".dat"))),"quarantine "+entry.getKey());cases++;
        }
        for(var root:List.of(preparing,complete)) {
            Path dir=base.resolve("normal-"+cases);Files.createDirectories(dir);Path f=dir.resolve("whileaway_story.dat");NbtIo.writeCompressed(root,f);
            var loaded=StoryStorage.open(new DimensionDataStorage(dir.toFile(),null,null),dir,null,DIMS);var r=loaded.actors.get(NpcEvents.STORY);
            check(r!=null&&NpcState.integrity(r).isEmpty(),"valid NPC rejected");loaded.setDirty();loaded.save(f.toFile(),null);
            var again=StoryStorage.open(new DimensionDataStorage(dir.toFile(),null,null),dir,null,DIMS).actors.get(NpcEvents.STORY);
            check(again.facts.equals(r.facts)&&again.entityId.equals(r.entityId)&&again.generation==r.generation,"NPC reload drift");cases++;
        }
        check(Arrays.equals(original,Files.readAllBytes(source)),"fixture source altered");
        System.out.println("PASS NPC storage cases="+cases+" semanticBlocked=6 normalRoundTrips=2 originalsPreserved=true actualClient=false");System.exit(0);
    }
}
