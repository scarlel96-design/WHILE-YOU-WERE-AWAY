package io.github.whileaway;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkIntegrationGameTests {
    static Path file() throws Exception {
        var root=Path.of(System.getProperty("whileaway.testEvidence"),"integration");Files.createDirectories(root);
        return Files.createTempDirectory(root,"story-").resolve("whileaway_story.dat").toAbsolutePath();
    }
    static NarrativeData bound(GameTestHelper h,Path p) {
        var d=new NarrativeData();ReturnNetworkFixtures.resident(d);d.bindStorage(p,null);d.setDirty();d.save(p.toFile(),h.getLevel().registryAccess());return d;
    }
    static ReturnNetworkState discovery(NarrativeData d) {
        return d.returnNetwork().discover(new ReturnNetworkState.Prerequisite(4,true,true,true),UUID.randomUUID(),new ReturnNetworkState.Location("rail_relay","minecraft:overworld",new BlockPos(12,64,-8).asLong()));
    }
    static CompoundTag disk(Path p)throws Exception{return NbtIo.readCompressed(p,NbtAccounter.unlimitedHeap()).getCompound("data");}
    static void commit(GameTestHelper h,NarrativeData d,ReturnNetworkState next){d.commitReturnNetwork(d.returnNetwork(),next,h.getLevel().registryAccess());}
    @GameTest(template="empty") public static void networkAllDurableBoundariesWithNpcReceipts(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);var r=d.actors.get(NpcEvents.STORY);var identity=r.entityId;var instance=r.instanceId;int gen=r.generation;
        var s=discovery(d);var binding=new ReturnNetworkState.Participant(r.storyId,r.instanceId,r.entityId,r.generation);
        var steps=List.of(s,s.join(binding),s.join(binding).observePattern(),s.join(binding).observePattern().intervene(),
            s.join(binding).observePattern().intervene().beginPresentation(),
            s.join(binding).observePattern().intervene().beginPresentation().observeResponse(),
            s.join(binding).observePattern().intervene().beginPresentation().observeResponse().shareExperience(),
            s.join(binding).observePattern().intervene().beginPresentation().observeResponse().shareExperience().complete());
        for(var next:steps) {
            commit(h,d,next);var fresh=NarrativeData.load(disk(p),h.getLevel().registryAccess());
            h.assertTrue(fresh.returnNetwork().save().equals(next.save()),"disk reload exact at every semantic boundary");
            h.assertTrue(ReturnNetworkIntegrity.issues(fresh.returnNetwork(),fresh.actors).isEmpty(),"event and NPC ledger consistent");
            var actor=fresh.actors.get(NpcEvents.STORY);h.assertTrue(actor.entityId.equals(identity)&&actor.instanceId.equals(instance)&&actor.generation==gen,"same resident");
            fresh.bindStorage(p,StoryStorage.digest(Files.readAllBytes(p)));d=fresh;
        }
        byte[] before=Files.readAllBytes(p);var duplicate=d.commitReturnNetwork(d.returnNetwork(),d.returnNetwork().complete(),h.getLevel().registryAccess());
        h.assertTrue(!duplicate.wrote()&&Arrays.equals(before,Files.readAllBytes(p)),"repeat completion neither saves nor replays");h.succeed();
    }
    @GameTest(template="empty") public static void networkFabricatedPrerequisiteRejected(GameTestHelper h)throws Exception {
        var p=file();var d=new NarrativeData();d.bindStorage(p,null);d.setDirty();d.save(p.toFile(),h.getLevel().registryAccess());
        byte[] before=Files.readAllBytes(p);boolean rejected=false;
        try{commit(h,d,discovery(d));}catch(IllegalStateException e){rejected=e.getMessage().contains("RN_PREREQUISITE");}
        h.assertTrue(rejected&&d.returnNetwork().stage==ReturnNetworkState.Stage.NOT_STARTED&&Arrays.equals(before,Files.readAllBytes(p)),"booleans cannot manufacture actual completed NPC");h.succeed();
    }
    @GameTest(template="empty") public static void networkSilentWriterNeverPublishes(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);byte[] before=Files.readAllBytes(p);d.setStoryWriterForTest((root,target)->{});boolean rejected=false;
        try{commit(h,d,discovery(d));}catch(java.io.UncheckedIOException e){rejected=e.getCause().getMessage().contains("readback_mismatch");}
        h.assertTrue(rejected&&!d.storySaveWritable()&&d.returnNetwork().stage==ReturnNetworkState.Stage.NOT_STARTED&&Arrays.equals(before,Files.readAllBytes(p)),"silent writer poisons guard without publishing");h.succeed();
    }
    @GameTest(template="empty") public static void networkUncheckedWriterNeverPublishes(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);d.setStoryWriterForTest((root,target)->{throw new IllegalStateException("injected unchecked writer");});boolean rejected=false;
        try{commit(h,d,discovery(d));}catch(IllegalStateException e){rejected=true;}
        h.assertTrue(rejected&&!d.storySaveWritable()&&d.returnNetwork().stage==ReturnNetworkState.Stage.NOT_STARTED,"unchecked failure poisons guard");h.succeed();
    }
    @GameTest(template="empty") public static void networkCorruptBindingQuarantinedAndAutosaveBlocked(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);commit(h,d,discovery(d));var a=d.actors.get(NpcEvents.STORY);
        commit(h,d,d.returnNetwork().join(new ReturnNetworkState.Participant(a.storyId,a.instanceId,a.entityId,a.generation)));
        var root=disk(p);root.getCompound("returnNetwork").getCompound("participant").putUUID("entity",UUID.randomUUID());
        var envelope=new CompoundTag();envelope.put("data",root);NbtIo.writeCompressed(envelope,p);byte[] before=Files.readAllBytes(p);
        var storage=new DimensionDataStorage(p.getParent().toFile(),h.getLevel().getServer().getFixerUpper(),h.getLevel().registryAccess());boolean blocked=false;
        try{StoryStorage.open(storage,p.getParent(),h.getLevel().registryAccess(),Set.of("minecraft:overworld"));}catch(StoryStorage.Blocked e){blocked=e.status.detail().contains("RN_PARTICIPANT_IDENTITY_MISMATCH");}
        storage.save();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
        h.assertTrue(blocked&&Arrays.equals(before,Files.readAllBytes(p)),"no empty campaign or overwrite");
        var backup=p.getParent().resolve("whileaway-quarantine").resolve(StoryStorage.digest(before)+".dat");
        h.assertTrue(Files.exists(backup)&&Arrays.equals(before,Files.readAllBytes(backup)),"original preserved");h.succeed();
    }
    @GameTest(template="empty") public static void networkSharedCommitFailurePreservesNpcAndStory(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);var a=d.actors.get(NpcEvents.STORY);
        commit(h,d,discovery(d));commit(h,d,d.returnNetwork().join(new ReturnNetworkState.Participant(a.storyId,a.instanceId,a.entityId,a.generation)));
        commit(h,d,d.returnNetwork().observePattern());commit(h,d,d.returnNetwork().intervene());commit(h,d,d.returnNetwork().beginPresentation());commit(h,d,d.returnNetwork().observeResponse());
        byte[] before=Files.readAllBytes(p);var facts=Set.copyOf(a.facts);d.setStoryWriterForTest((root,target)->{throw new java.io.IOException("shared commit failure");});
        try{commit(h,d,d.returnNetwork().shareExperience());}catch(java.io.UncheckedIOException expected){}
        h.assertTrue(a.facts.equals(facts)&&!d.returnNetwork().facts.contains(ReturnNetworkState.Fact.SHARED_EXPERIENCE)&&Arrays.equals(before,Files.readAllBytes(p)),"both halves unchanged on failed F");h.succeed();
    }
    @GameTest(template="empty") public static void networkCorruptionMatrixRejectsPermanentInference(GameTestHelper h)throws Exception {
        var p=file();var d=bound(h,p);var a=d.actors.get(NpcEvents.STORY);
        commit(h,d,discovery(d));commit(h,d,d.returnNetwork().join(new ReturnNetworkState.Participant(a.storyId,a.instanceId,a.entityId,a.generation)));
        commit(h,d,d.returnNetwork().observePattern());commit(h,d,d.returnNetwork().intervene());commit(h,d,d.returnNetwork().beginPresentation());
        commit(h,d,d.returnNetwork().observeResponse());commit(h,d,d.returnNetwork().shareExperience());commit(h,d,d.returnNetwork().complete());
        var pristine=disk(p);
        for(int variant=0;variant<8;variant++) {
            var bad=pristine.copy();var event=bad.getCompound("returnNetwork");
            switch(variant) {
                case 0 -> event.remove("participant");
                case 1 -> event.getList("facts",8).removeIf(t->t.getAsString().equals("OLD_PATTERN_EVIDENCE"));
                case 2 -> event.getList("facts",8).removeIf(t->t.getAsString().equals("RESPONSE"));
                case 3 -> event.putString("structure","INTACT");
                case 4 -> bad.getList("actors",10).getCompound(0).getList("facts",8).removeIf(t->t.getAsString().equals(ReturnNetworkIntegrity.SHARED));
                case 5 -> event.putInt("checkpoint",99);
                case 6 -> event.getCompound("participant").putInt("generation",a.generation+1);
                case 7 -> event.putString("dimension","minecraft:the_nether");
            }
            boolean rejected=false;try{NarrativeData.load(bad,h.getLevel().registryAccess());}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"permanent contradiction rejected variant="+variant);
            h.assertTrue(pristine.equals(disk(p)),"load probe never changes original variant="+variant);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void networkWorldEquipmentNeverInfersStory(GameTestHelper h)throws Exception {
        var l=h.getLevel();var relay=ReturnNetworkEvents.RELAY;var stand=ReturnNetworkEvents.STAND;
        var positions=List.of(relay,stand,stand.above(),stand.below());var old=positions.stream().map(l::getBlockState).toList();
        try {
            l.setBlock(stand,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            l.setBlock(stand.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            l.setBlock(stand.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            l.setBlock(relay,net.minecraft.world.level.block.Blocks.OXIDIZED_CUT_COPPER.defaultBlockState(),3);
            var s=ReturnNetworkState.notStarted();h.assertTrue(ReturnNetworkEvents.environmentReady(l,s),"authored equipment readable");
            l.setBlock(stand,net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(l,s)&&l.getBlockState(stand).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK),"obstruction not removed");
            l.setBlock(stand,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            l.setBlock(relay,WhileAway.RELAY.get().defaultBlockState().setValue(io.github.whileaway.content.RelayBlock.LIT,true),3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(l,s)&&s.stage==ReturnNetworkState.Stage.NOT_STARTED,"player-built lookalike does not manufacture intervention");
        }finally {for(int i=0;i<positions.size();i++)l.setBlock(positions.get(i),old.get(i),3);}
        h.succeed();
    }
    @GameTest(template="empty") public static void npcNavigationHonorsItsOwn48BlockLimit(GameTestHelper h) {
        var l=h.getLevel();var start=new BlockPos(10000,80,10000);var end=start.offset(30,0,0);
        for(int x=(start.getX()-8)>>4;x<=(end.getX()+8)>>4;x++)for(int z=(start.getZ()-8)>>4;z<=(start.getZ()+8)>>4;z++)l.getChunk(x,z);
        for(int x=-1;x<=32;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)l.setBlock(start.offset(x,y,z),(y==-1?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),2);
        var npc=WhileAway.STORY_NPC.get().create(l);npc.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);npc.setOnGround(true);
        h.assertTrue(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)==24,"fixture keeps actual resident attribute");
        var shortPath=npc.getNavigation().createPath(end,0);
        h.assertTrue(shortPath==null||!shortPath.canReach(),"reproduce vanilla 24-block bound");
        h.assertTrue(npc.storyNavigation().step(npc,end)==NpcEnvironment.Path.AVAILABLE,"30-block target within story range must be reachable");
        h.assertTrue(npc.storyNavigation().step(npc,start.offset(49,0,0))==NpcEnvironment.Path.OUT_OF_RANGE,"48-block story guard retained");h.succeed();
    }

}
