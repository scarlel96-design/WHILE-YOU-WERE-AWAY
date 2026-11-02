package io.github.whileaway;

import io.github.whileaway.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.neoforge.gametest.*;

/** Use actual templates, placement processors, registries and block entities. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class RailSiteGameTests {
    @GameTest(template="empty")
    public static void signalOfficeRotatesWithoutLosingBeamIdentity(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(20,1,2));
        var template=h.getLevel().getStructureManager().get(WhileAway.id("rail_signal_office")).orElseThrow();
        h.assertTrue(template.placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90),h.getLevel().getRandom(),Block.UPDATE_ALL),"office placed");
        var anchor=origin.offset(-8,6,10);
        for(int i=0;i<7;i++){
            var pos=anchor.south(i);var state=h.getLevel().getBlockState(pos);
            h.assertTrue(state.is(WhileAway.RAIL_BEAM.get())&&state.getValue(RailBeamBlock.SEGMENT)==i,"segment preserved");
            var beam=(RailBeamEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(beam!=null&&beam.anchor().equals(anchor),"rotated segments share one anchor");
            h.assertTrue(state.getCollisionShape(h.getLevel(),pos).bounds().getYsize()==1,"solid collision remains stationary");
        }
        h.assertTrue(h.getLevel().getBlockState(origin.offset(-10,1,7)).is(Blocks.CRAFTING_TABLE),"usable work table");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(-4,1,4)).isAir(),"entry stays open");
        var lever=origin.offset(-6,1,6);
        ((LeverBlock)Blocks.LEVER).pull(h.getLevel().getBlockState(lever),h.getLevel(),lever,null);
        h.assertTrue(h.getLevel().getBlockState(origin.offset(-6,1,7)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"ordinary lever operates signal lamp");h.succeed();
    }
    @GameTest(template="empty")
    public static void maintenancePassageProvidesStairsAndWorkSpace(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(1,1,1));
        var template=h.getLevel().getStructureManager().get(WhileAway.id("rail_maintenance_passage")).orElseThrow();
        h.assertTrue(template.placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings(),h.getLevel().getRandom(),Block.UPDATE_ALL),"passage placed");
        for(var step:new BlockPos[]{new BlockPos(8,3,1),new BlockPos(8,2,2),new BlockPos(8,1,3),new BlockPos(8,0,4)}){
            h.assertTrue(h.getLevel().getBlockState(origin.offset(step)).is(Blocks.STONE_BRICK_STAIRS),"surface stairs");
            h.assertTrue(h.getLevel().getBlockState(origin.offset(step).above(2)).isAir(),"stair headroom");
        }
        h.assertTrue(h.getLevel().getBlockState(origin.offset(2,1,10)).is(Blocks.CRAFTING_TABLE),"work table");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(2,1,12)).is(Blocks.BARREL),"usable storage");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(5,1,10)).isAir(),"side room reachable");
        var lever=origin.offset(10,1,7);
        ((LeverBlock)Blocks.LEVER).pull(h.getLevel().getBlockState(lever),h.getLevel(),lever,null);
        h.assertTrue(h.getLevel().getBlockState(origin.offset(10,1,8)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"maintenance light works");h.succeed();
    }
    @GameTest(template="empty")
    public static void protectedPlacementPreservesInventoryAndPlayerWall(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(1,1,1));var chest=origin.offset(7,1,10);var wall=origin.offset(4,1,4);
        h.getLevel().setBlock(chest,Blocks.CHEST.defaultBlockState(),Block.UPDATE_ALL);
        ((ChestBlockEntity)h.getLevel().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,3));
        h.getLevel().setBlock(wall,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
        var settings=new StructurePlaceSettings().addProcessor(new ProtectedBlockProcessor(TagKey.create(Registries.BLOCK,WhileAway.id("rail_site_protected"))));
        var template=h.getLevel().getStructureManager().get(WhileAway.id("rail_signal_office")).orElseThrow();
        template.placeInWorld(h.getLevel(),origin,origin,settings,h.getLevel().getRandom(),Block.UPDATE_ALL);
        h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.BRICKS),"player wall must survive even across entrance");
        h.assertTrue(h.getLevel().getBlockEntity(chest) instanceof ChestBlockEntity,"chest preserved");
        var stack=((ChestBlockEntity)h.getLevel().getBlockEntity(chest)).getItem(0);
        h.assertTrue(stack.is(Items.DIAMOND)&&stack.getCount()==3,"inventory preserved");h.succeed();
    }
    @GameTest(template="empty")
    public static void placementRespectsChunkWorkBounds(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(1,1,1));var outside=origin.offset(7,1,10);
        h.getLevel().setBlock(outside,Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_ALL);
        var box=new BoundingBox(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+4,origin.getY()+9,origin.getZ()+16);
        var template=h.getLevel().getStructureManager().get(WhileAway.id("rail_signal_office")).orElseThrow();
        template.placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings().setBoundingBox(box),h.getLevel().getRandom(),Block.UPDATE_ALL);
        h.assertTrue(h.getLevel().getBlockState(outside).is(Blocks.DIAMOND_BLOCK),"placement must not write outside its work bounds");h.succeed();
    }
    @GameTest(template="empty")
    public static void bothOptionalSitesProduceActualJigsawPieces(GameTestHelper h){
        var level=h.getLevel();var access=level.registryAccess();var generator=level.getChunkSource().getGenerator();
        for(String name:new String[]{"rail_signal_office","rail_maintenance_passage"}){
            h.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(WhileAway.id(name)),"placement registered");
            var structure=access.registryOrThrow(Registries.STRUCTURE).get(WhileAway.id(name));
            var context=new Structure.GenerationContext(access,generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),74192362L,new net.minecraft.world.level.ChunkPos(0,0),level,b->true);
            var point=((net.minecraft.world.level.levelgen.structure.structures.JigsawStructure)structure).findGenerationPoint(context);
            h.assertTrue(point.isPresent()&&!point.orElseThrow().getPiecesBuilder().build().pieces().isEmpty(),"optional site emits real pieces");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void beamPresentationNeverChangesServerBlocksOrSave(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));var state=WhileAway.RAIL_BEAM.get().defaultBlockState();
        h.getLevel().setBlock(pos,state,Block.UPDATE_ALL);var beam=(RailBeamEntity)h.getLevel().getBlockEntity(pos);
        var before=beam.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(RailBeamEntity.visualAngle(80,0,48)>11,"distant tilt is visible");
        h.assertTrue(Math.abs(RailBeamEntity.visualAngle(80,0,90)-6)<.001&&RailBeamEntity.visualAngle(80,0,95)<1.01,"far edge fades instead of snapping");
        h.assertTrue(RailBeamEntity.visualAngle(80,0,24)==0&&RailBeamEntity.visualAngle(80,0,96)==0,"near and far views remain straight");
        h.assertTrue(RailBeamEntity.visualAngle(160,0,48)==0&&RailBeamEntity.visualAngle(900,0,48)==0,"effect finishes and rests");
        for(int i=0;i<12;i++)beam.stepEcho(true,true,i);
        for(int i=0;i<8;i++)beam.stepEcho(false,true,12+i);
        RailBeamEntity.clientTick(h.getLevel(),pos,state,beam);
        h.assertTrue(h.getLevel().getBlockState(pos).equals(state),"no physical change");
        h.assertTrue(before.equals(beam.saveWithFullMetadata(h.getLevel().registryAccess())),"no presentation progress saved");
        h.assertTrue(WhileAway.RAIL_BEAM.get().getTicker(h.getLevel(),state,WhileAway.RAIL_BEAM_ENTITY.get())==null,"no server presentation ticker");h.succeed();
    }
    @GameTest(template="empty")
    public static void extraFootfallRequiresMovementStopAndCooldown(GameTestHelper h){
        var beam=new RailBeamEntity(BlockPos.ZERO,WhileAway.RAIL_BEAM.get().defaultBlockState());
        for(int i=0;i<12;i++)h.assertTrue(!beam.stepEcho(true,true,i),"walking does not echo");
        for(int i=12;i<19;i++)h.assertTrue(!beam.stepEcho(false,true,i),"stop delay");
        h.assertTrue(beam.stepEcho(false,true,19),"one extra step after stop");
        for(int i=20;i<32;i++)beam.stepEcho(true,true,i);
        for(int i=32;i<40;i++)h.assertTrue(!beam.stepEcho(false,true,i),"cooldown blocks repeat");
        for(int i=1220;i<1232;i++)beam.stepEcho(true,true,i);
        beam.stepEcho(false,false,1232);
        for(int i=1233;i<1250;i++)h.assertTrue(!beam.stepEcho(false,true,i),"lost eligibility cancels armed echo");h.succeed();
    }
}
