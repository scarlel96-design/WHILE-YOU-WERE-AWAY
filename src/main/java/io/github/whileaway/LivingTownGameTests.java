package io.github.whileaway;

import io.github.whileaway.core.*;
import io.github.whileaway.content.LifeObservationBlock;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class LivingTownGameTests {
    @GameTest(template="empty") public static void livingConstructionCursorResumesWithoutReorderingLegacy(GameTestHelper h) {
        var t=new CompoundTag();t.putInt("version",3);t.putBoolean("started",true);
        int cursor=CityArtLayout.CELLS.size()+17;t.putInt("cursor",cursor);
        var d=CityDistrict.load(t,h.getLevel().registryAccess());
        var reloaded=CityDistrict.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(reloaded.cursor()==cursor&&reloaded.plan().get(cursor).equals(CityLivingLayout.ADDITIONS.get(17)),"resume points at the same living-space cell");
        t.putInt("cursor",CityLivingLayout.CELLS.size());
        h.assertTrue(CityDistrict.load(t,h.getLevel().registryAccess()).ready(),"completed living district remains completed");
        t.putInt("cursor",CityLivingLayout.CELLS.size()+1);boolean rejected=false;
        try{CityDistrict.load(t,h.getLevel().registryAccess());}catch(IllegalStateException expected){rejected=true;}
        h.assertTrue(rejected,"out-of-range cursor cannot rebuild the district");h.succeed();
    }
    @GameTest(template="empty") public static void livingPlacementPreservesPlayerBlocksAndAcceptsReplay(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(1,2,1));
        var c=new CityLayout.Cell(p.getX(),p.getY()-CityLayout.BASE_Y,p.getZ(),CityLayout.Kind.WORKBENCH);
        h.getLevel().setBlock(p,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        h.assertTrue(!CityDistrict.placeLivingCell(h.getLevel(),c)&&h.getLevel().getBlockState(p).is(Blocks.DIAMOND_BLOCK),"player block survives authored placement");
        h.getLevel().setBlock(p,CityDistrict.state(CityLivingLayout.underlay(c)),2);
        h.assertTrue(CityDistrict.placeLivingCell(h.getLevel(),c)&&CityDistrict.placeLivingCell(h.getLevel(),c),"same target can be replayed without duplicate placement");
        h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.CRAFTING_TABLE),"functional workbench is placed");h.succeed();
    }
    @GameTest(template="empty") public static void livingDailyRouteKeepsAllAuthoredDestinationsClear(GameTestHelper h) {
        var finalCells=new HashMap<BlockPos,CityLayout.Kind>();
        for(var c:CityLivingLayout.CELLS)finalCells.put(new BlockPos(c.x(),64+c.y(),c.z()),c.kind());
        var home=new BlockPos(38,65,72);
        for(long time:new long[]{0,5000,7500,10500,12000,14000}) {
            var target=NpcEvents.residenceDestination(home,time,true);
            h.assertTrue(finalCells.getOrDefault(target,CityLayout.Kind.AIR)==CityLayout.Kind.AIR
                &&finalCells.getOrDefault(target.above(),CityLayout.Kind.AIR)==CityLayout.Kind.AIR,"routine target remains walkable at "+time);
            h.assertTrue(target.distSqr(home)<48*48,"routine keeps the existing navigation budget");
        }
        h.assertTrue(NpcEvents.residenceDestination(home,10500,false).equals(home.east()),"legacy day route unchanged");
        h.assertTrue(NpcEvents.residenceDestination(home,10500,true).equals(home.offset(22,0,-24)),"new resident visits the real workshop");
        h.succeed();
    }
    @GameTest(template="empty") public static void workshopObservationRequiresActualEquipmentState(GameTestHelper h) {
        var off=Blocks.LEVER.defaultBlockState();var lamp=Blocks.REDSTONE_LAMP.defaultBlockState();
        h.assertTrue(LifeObservationBlock.maintenanceKey(off,lamp).endsWith("maintenance_record"),"record alone never invents a response");
        var on=off.setValue(LeverBlock.POWERED,true);
        h.assertTrue(LifeObservationBlock.maintenanceKey(on,lamp).endsWith("maintenance_record"),"powered switch alone does not prove a lit lamp");
        h.assertTrue(LifeObservationBlock.maintenanceKey(on,lamp.setValue(RedstoneLampBlock.LIT,true)).endsWith("maintenance_response"),"matching physical response permits the observation");
        h.assertTrue(LifeObservationBlock.maintenanceKey(on,Blocks.STONE.defaultBlockState()).endsWith("changed_equipment"),"changed player equipment stays uncertain");h.succeed();
    }
    @GameTest(template="empty") public static void workshopVanillaSwitchActuallyLightsTheFloorLamp(GameTestHelper h) {
        var lamp=h.absolutePos(new BlockPos(2,1,2));var lever=lamp.above();
        h.getLevel().setBlock(lamp,Blocks.REDSTONE_LAMP.defaultBlockState(),3);
        h.getLevel().setBlock(lever,CityDistrict.state(CityLayout.Kind.SERVICE_LEVER),3);
        var player=h.makeMockServerPlayerInLevel();
        h.getLevel().getBlockState(lever).useWithoutItem(h.getLevel(),player,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lever),Direction.UP,lever,false));
        h.runAfterDelay(4,()->{
            h.assertTrue(h.getLevel().getBlockState(lamp).getValue(RedstoneLampBlock.LIT),"real vanilla use powers the adjacent lamp");
            player.discard();h.succeed();
        });
    }
}
