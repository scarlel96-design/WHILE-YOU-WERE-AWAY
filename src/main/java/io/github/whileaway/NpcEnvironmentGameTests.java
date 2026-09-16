package io.github.whileaway;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class NpcEnvironmentGameTests {
    @GameTest(template="empty") public static void pathBudgetBackoffWithoutPermanentFailure(GameTestHelper h) {
        var b=new io.github.whileaway.core.NpcPathBudget();long tick=0;
        for(int n=1;n<=3;n++){
            h.assertTrue(b.ready(tick),"attempt due");b.attempted(tick);b.failed(tick);
            h.assertTrue(b.unreachable()==(n==3),"one failure must not suspend permanently");
            int interval=n==3?200:60;h.assertTrue(!b.ready(tick+interval-1)&&b.ready(tick+interval),"bounded retry interval");tick+=interval;
        }
        h.assertTrue(b.attempts()==3,"no hidden attempts");b.progress();h.assertTrue(!b.unreachable(),"actual progress clears failure streak");b.reset();h.assertTrue(b.ready(tick)&&b.failures()==0,"reload/absence uses ephemeral budget");h.succeed();
    }
    @GameTest(template="empty") public static void pathCorridorDoesNotLoadUnobservedChunks(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(2,2,2));var far=p.offset(1000000,0,1000000);
        h.assertTrue(!NpcNavigation.corridorLoaded(h.getLevel(),p,far)&&!h.getLevel().hasChunkAt(far),"bounded metadata check never loads unobserved terrain");h.succeed();
    }
    private static BlockPos setup(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(2,2,2));var l=h.getLevel();
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p.above(),Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p.offset(2,0,0),WhileAway.RETURN_LIGHT.get().defaultBlockState());return p;
    }
    private static NpcEnvironment.Assessment inspect(GameTestHelper h,BlockPos p){return NpcEnvironment.inspect(h.getLevel(),h.getLevel().dimension().location().toString(),p,p);}
    @GameTest(template="empty") public static void environmentDistinguishesMissingBlockedConflict(GameTestHelper h) {
        var p=setup(h);var l=h.getLevel();h.assertTrue(inspect(h,p).dependenciesReady()&&inspect(h,p).path()==NpcEnvironment.Path.NOT_ASSESSED,"valid geometry must not claim a proven path");
        l.setBlockAndUpdate(p.below(),Blocks.AIR.defaultBlockState());h.assertTrue(inspect(h,p).residence()==NpcEnvironment.Residence.MISSING,"missing floor");
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
        h.assertTrue(inspect(h,p).residence()==NpcEnvironment.Residence.TEMPORARILY_BLOCKED,"occupied standing space");
        h.assertTrue(l.getBlockState(p).is(Blocks.STONE),"inspection must preserve player obstruction");
        l.setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());h.assertTrue(inspect(h,p).residence()==NpcEnvironment.Residence.CONFLICT,"flooded destination");
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());h.assertTrue(inspect(h,p).dependenciesReady(),"world repair re-evaluated");h.succeed();
    }
    @GameTest(template="empty") public static void environmentLightMissingWrongRestored(GameTestHelper h) {
        var p=setup(h);var light=p.offset(2,0,0);var l=h.getLevel();
        l.setBlockAndUpdate(light,Blocks.AIR.defaultBlockState());h.assertTrue(inspect(h,p).light()==NpcEnvironment.Light.MISSING&&!inspect(h,p).dependenciesReady(),"missing light waits");
        l.setBlockAndUpdate(light,Blocks.STONE.defaultBlockState());h.assertTrue(inspect(h,p).light()==NpcEnvironment.Light.WRONG_BLOCK&&!inspect(h,p).dependenciesReady(),"same coordinate is not enough");
        l.setBlockAndUpdate(light,WhileAway.RETURN_LIGHT.get().defaultBlockState());h.assertTrue(inspect(h,p).dependenciesReady(),"restored light");h.succeed();
    }
    @GameTest(template="empty") public static void environmentUnloadedNotMissing(GameTestHelper h) {
        var p=setup(h);var l=h.getLevel();var far=p.offset(1000000,0,1000000);h.assertTrue(!l.hasChunkAt(far),"fixture must be genuinely unloaded");
        var a=NpcEnvironment.inspect(l,l.dimension().location().toString(),p,far);
        h.assertTrue(a.residence()==NpcEnvironment.Residence.CHUNK_UNAVAILABLE&&a.path()==NpcEnvironment.Path.CHUNK_UNAVAILABLE&&a.light()==NpcEnvironment.Light.CHUNK_UNAVAILABLE,"unloaded is not destroyed");
        h.assertTrue(!l.hasChunkAt(far),"inspector must not load chunk");
        h.assertTrue(NpcEnvironment.inspect(l,"missing:dimension",p,p).residence()==NpcEnvironment.Residence.CONFLICT,"dimension binding conflict");h.succeed();
    }
}
