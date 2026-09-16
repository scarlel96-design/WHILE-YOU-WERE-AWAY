package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** Real server entity fixtures. Villagers test a reusable adapter, NOT a shipped story NPC. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ActorGameTests {
    private static StoryActorRecord record() {
        return new StoryActorRecord("test:npc","test:meeting","resident","minecraft:villager","minecraft:overworld",
            UUID.fromString("00000000-0000-0000-0000-000000000001"),null,UUID.fromString("00000000-0000-0000-0000-000000000002"),new BlockPos(0,80,0),false);
    }
    private static Entity villager(GameTestHelper h) {
        var a=EntityType.VILLAGER.create(h.getLevel());var pos=h.absolutePos(new BlockPos(1,2,1));
        h.setBlock(new BlockPos(1,1,1),Blocks.STONE);h.setBlock(new BlockPos(1,2,1),Blocks.AIR);h.setBlock(new BlockPos(1,3,1),Blocks.AIR);
        a.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);((Mob)a).setNoAi(true);return a;
    }
    private static StoryActorRecord prepare(GameTestHelper h,Entity a) {
        return StoryActors.prepare(a,"test:npc:"+UUID.randomUUID(),"test:meeting","resident",UUID.randomUUID(),null,false);
    }
    private static void clean(GameTestHelper h,StoryActorRecord r){StoryActors.abort(h.getLevel().getServer(),r);}
    @GameTest(template="empty")
    public static void actorJournalRoundTripsEveryBoundary(GameTestHelper h) {
        for(int phase=0;phase<=4;phase++) {
            var r=record();for(int n=1;n<=phase;n++)r.checkpoint.advance(n);r.facts.add("dialogue:window");r.facts.add("rescued");
            r.lifecycle=phase==0?StoryActorRecord.Lifecycle.PREPARED:StoryActorRecord.Lifecycle.ACTIVE;
            var input=r.save();CompoundTag normalized=null;
            for(int n=0;n<40;n++) {
                r=StoryActorRecord.load(input);h.assertTrue(r.checkpoint.checkpoint==phase&&r.facts.size()==2,"checkpoint and irreversible facts survive");
                input=r.save();if(normalized==null)normalized=input;else h.assertTrue(normalized.equals(input),"deterministic normalization");
            }
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void actorMalformedIdentityStopsLoad(GameTestHelper h) {
        for(var key:List.of("entityUUID","instanceId","storyId","position")) {
            var t=record().save();t.remove(key);boolean rejected=false;
            try{StoryActorRecord.load(t);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("MANUAL_DIAGNOSTIC");}
            h.assertTrue(rejected,"ambiguous identity not silently replaced: "+key);
        }
        var t=record().save();t.putString("lifecycleState","UNKNOWN");boolean rejected=false;
        try{StoryActorRecord.load(t);}catch(IllegalStateException expected){rejected=true;}h.assertTrue(rejected,"unknown lifecycle diagnosed");h.succeed();
    }
    @GameTest(template="empty")
    public static void npcMissingEntityRebuiltAndOldGenerationRejected(GameTestHelper h) {
        var a=villager(h);var r=prepare(h,a);h.assertTrue(StoryActors.spawnPrepared(h.getLevel(),r,a),"fixture actor spawned");
        StoryActors.checkpoint(a,r,2,"dialogue:already_spoken");var oldSnapshot=r.snapshot.copy();var oldId=r.entityId;
        a.discard();h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==null,"first loaded pass waits for entity joins");
        var replacement=StoryActors.reconcile(h.getLevel().getServer(),r);
        h.assertTrue(replacement!=null&&!replacement.getUUID().equals(oldId)&&r.generation==1,"missing actor recreated once with epoch fence");
        h.assertTrue(r.facts.contains("dialogue:already_spoken")&&r.checkpoint.checkpoint==2,"dialogue and checkpoint not reset");
        var stale=EntityType.VILLAGER.create(h.getLevel());stale.load(oldSnapshot);stale.setUUID(oldId);
        h.assertTrue(!h.getLevel().addFreshEntity(stale),"late old chunk actor rejected");
        h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==replacement,"repeated reconciliation returns same actor");clean(h,r);h.succeed();
    }
    @GameTest(template="empty")
    public static void unloadedActorStorageIsNotAbsence(GameTestHelper h) {
        var r=record();r.position=new BlockPos(12000000,80,12000000);
        var id=r.entityId;for(int i=0;i<10;i++)h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==null,"unloaded location deferred");
        h.assertTrue(!h.getLevel().hasChunkAt(r.position)&&id.equals(r.entityId)&&r.generation==0,"no chunk force-load or blind NPC clone");h.succeed();
    }
    @GameTest(template="empty")
    public static void retiredActorRejectedOnJoin(GameTestHelper h) {
        var a=villager(h);var r=prepare(h,a);r.lifecycle=StoryActorRecord.Lifecycle.RETIRED;
        StoryActors.persist(h.getLevel().getServer());h.assertTrue(!h.getLevel().addFreshEntity(a),"retired actor never joins world");
        h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==null,"terminal actor not recreated");h.succeed();
    }
    @GameTest(template="empty")
    public static void actorIdentityRoleAndInstanceAreRequired(GameTestHelper h) {
        var a=villager(h);var r=prepare(h,a);
        for(var key:List.of("spawnRole","ownerEventId","storyId")) {
            StoryActors.tag(a,r);a.getPersistentData().getCompound(StoryActors.TAG).putString(key,"wrong");
            h.assertTrue(!StoryActors.canonical(a,r),"UUID alone cannot authorize "+key);
        }
        StoryActors.tag(a,r);a.getPersistentData().getCompound(StoryActors.TAG).putUUID("instanceId",UUID.randomUUID());
        h.assertTrue(!StoryActors.canonical(a,r),"instance fence");clean(h,r);h.succeed();
    }
    @GameTest(template="empty")
    public static void copyKeepsActorFactsIndependent(GameTestHelper h) {
        var d=new NarrativeData();var r=record();d.actors.put(r.storyId,r);r.facts.add("promise");
        var copy=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        copy.actors.get(r.storyId).facts.add("rescued");copy.actors.get(r.storyId).generation++;
        h.assertTrue(r.facts.size()==1&&r.generation==0,"world data does not share actor objects");h.succeed();
    }
    @GameTest(template="empty")
    public static void actorDuplicateRegistryIdentityRejected(GameTestHelper h) {
        var t=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());var list=new net.minecraft.nbt.ListTag();list.add(record().save());list.add(record().save());t.put("actors",list);
        boolean rejected=false;try{NarrativeData.load(t,h.getLevel().registryAccess());}catch(IllegalStateException expected){rejected=true;}
        h.assertTrue(rejected,"duplicate logical NPC never silently overrides");h.succeed();
    }
    @GameTest(template="empty")
    public static void actorCollisionDoesNotOverwriteWorld(GameTestHelper h) {
        var a=villager(h);var r=prepare(h,a);var pos=r.position;
        // Fill only the fixture's own volume; production recovery must not remove any of it.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<3;y++)h.getLevel().setBlock(pos.offset(x,y,z),Blocks.IRON_BLOCK.defaultBlockState(),2);
        for(int n=0;n<4;n++)h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==null,"blocked recovery waits");
        h.assertTrue(r.generation==0&&h.getLevel().getBlockState(pos).is(Blocks.IRON_BLOCK),"player geometry preserved");clean(h,r);h.succeed();
    }
    @GameTest(template="empty")
    public static void optionalActorSchemaMigratesWithoutInventingNPCs(GameTestHelper h) {
        for(int schema=1;schema<=4;schema++) {
            var t=new CompoundTag();t.putInt("schema",schema);
            var a=NarrativeData.load(t,h.getLevel().registryAccess()).save(new CompoundTag(),h.getLevel().registryAccess());
            var b=NarrativeData.load(t.copy(),h.getLevel().registryAccess()).save(new CompoundTag(),h.getLevel().registryAccess());
            h.assertTrue(a.equals(b)&&a.getInt("schema")==4&&a.getList("actors",10).isEmpty(),"old save deterministically gains empty optional registry");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void staleChunkAIReconcilesCommittedHunt(GameTestHelper h) {
        var a=WhileAway.WAYFARER.get().create(h.getLevel());var owner=UUID.randomUUID();a.bind(owner,BlockPos.ZERO);
        var d=NarrativeData.get(h.getLevel().getServer());d.entry(owner).encounter=a.getUUID();
        var r=StoryActors.prepare(a,StoryActors.chaseId(owner),StoryActors.CHASE,"pursuer",UUID.randomUUID(),owner,true);
        r.checkpoint.advance(1);r.checkpoint.advance(2);r.facts.add("pursuit_started");r.snapshot.putInt("StoryAge",240);r.snapshot.putInt("HuntTicks",12);
        // Entity has pre-hunt values, but the separate durable facts say the hunt already started.
        a.tick();var saved=new CompoundTag();a.addAdditionalSaveData(saved);
        h.assertTrue(saved.getBoolean("Hunting")&&saved.getInt("StoryAge")==240&&saved.getInt("HuntTicks")==12,"stale entity AI restored to committed checkpoint without aging offline");
        clean(h,r);h.succeed();
    }
    @GameTest(template="empty")
    public static void spawnBeforeCommitReconcilesOnce(GameTestHelper h) {
        var a=villager(h);var r=prepare(h,a);h.assertTrue(h.getLevel().addFreshEntity(a),"world spawn before B write");
        h.assertTrue(r.checkpoint.checkpoint==0,"simulated prepare/spawn/commit gap");
        StoryActors.reconcile(h.getLevel().getServer(),r);
        h.assertTrue(r.checkpoint.checkpoint==1&&r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE,"B finalized from existing canonical actor");
        StoryActors.reconcile(h.getLevel().getServer(),r);h.assertTrue(r.generation==0,"no replacement of an existing actor");clean(h,r);h.succeed();
    }
}
