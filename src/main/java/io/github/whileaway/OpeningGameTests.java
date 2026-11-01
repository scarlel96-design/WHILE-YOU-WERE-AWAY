package io.github.whileaway;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class OpeningGameTests {
    @GameTest(template="empty") public static void openingSignalRespondsToActualLeverUse(GameTestHelper h) {
        var lever=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlock(lever.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        OpeningEvents.placeSignal(h.getLevel(),lever);
        var player=h.makeMockServerPlayerInLevel();
        h.getLevel().getBlockState(lever).useWithoutItem(h.getLevel(),player,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lever),
                net.minecraft.core.Direction.UP,lever,false));
        h.runAfterDelay(4,()->{
            h.assertTrue(h.getLevel().getBlockState(lever.south()).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                "the authored Opening signal must visibly respond to its real lever");
            player.discard();h.succeed();
        });
    }

    private static Path file() throws Exception {
        var root=Path.of(System.getProperty("whileaway.testEvidence"),"opening");
        Files.createDirectories(root);
        return Files.createTempDirectory(root,"story-").resolve("whileaway_story.dat").toAbsolutePath();
    }

    private static CompoundTag disk(Path file) throws Exception {
        return NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getCompound("data");
    }

    @GameTest(template="empty") public static void openingStagesSurviveDiskReload(GameTestHelper h) throws Exception {
        var file=file();var id=UUID.randomUUID();var origin=new BlockPos(73,82,-46);
        var data=new NarrativeData();data.entry(id);data.bindStorage(file,null);
        for(var stage:OpeningStage.values()) {
            if(stage==OpeningStage.COMPLETED)break;
            data.commitOpening(id,stage,origin,37.5f,-12.25f,h.getLevel().registryAccess());
            var loaded=NarrativeData.load(disk(file),h.getLevel().registryAccess());
            var entry=loaded.entry(id);
            h.assertTrue(entry.openingStage==stage.next(),"opening stage must be durable and monotonic");
            h.assertTrue(origin.equals(entry.openingReturnPosition),"return location must survive reload");
            h.assertTrue(entry.openingReturnYaw==37.5f&&entry.openingReturnPitch==-12.25f,"view must survive reload");
            loaded.bindStorage(file,StoryStorage.digest(Files.readAllBytes(file)));
            data=loaded;
        }
        byte[] before=Files.readAllBytes(file);boolean rejected=false;
        try{data.commitOpening(id,OpeningStage.NOT_STARTED,origin,0,0,h.getLevel().registryAccess());}
        catch(IllegalStateException ex){rejected=ex.getMessage().contains("OPENING_STALE_STAGE");}
        h.assertTrue(rejected&&Arrays.equals(before,Files.readAllBytes(file)),"stale opening transition leaves disk intact");
        h.succeed();
    }

    @GameTest(template="empty") public static void openingWriterFailureBlocksLivePublish(GameTestHelper h) throws Exception {
        var file=file();var id=UUID.randomUUID();var data=new NarrativeData();data.entry(id);data.bindStorage(file,null);
        data.setStoryWriterForTest((root,target)->{});boolean rejected=false;
        try{data.commitOpening(id,OpeningStage.NOT_STARTED,new BlockPos(0,70,0),0,0,h.getLevel().registryAccess());}
        catch(java.io.UncheckedIOException ex){rejected=true;}
        h.assertTrue(rejected&&!data.storySaveWritable()&&data.entry(id).openingStage==OpeningStage.NOT_STARTED
            &&Files.notExists(file),"failed write must retain origin state and block later writes");
        h.succeed();
    }

    @GameTest(template="empty") public static void legacyPlayerDoesNotReplayOpening(GameTestHelper h) throws Exception {
        var id=UUID.randomUUID();var data=new NarrativeData();data.entry(id);
        var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());
        saved.getList("players",10).getCompound(0).remove("openingStage");
        saved.getList("players",10).getCompound(0).remove("openingReturnYaw");
        saved.getList("players",10).getCompound(0).remove("openingReturnPitch");
        var loaded=NarrativeData.load(saved,h.getLevel().registryAccess());
        h.assertTrue(loaded.entry(id).openingStage==OpeningStage.COMPLETED,"legacy player must enter current gameplay");
        h.succeed();
    }
}
