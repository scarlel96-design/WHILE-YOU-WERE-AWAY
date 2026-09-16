package io.github.whileaway;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only, local dependency inspection. Geometry validity is NOT evidence of path reachability.
 * Results are current world observations, never identity or permanent NPC facts. */
public final class NpcEnvironment {
    public enum Residence { VALID, TEMPORARILY_BLOCKED, UNREACHABLE, MISSING, CONFLICT, CHUNK_UNAVAILABLE }
    public enum Path { NOT_ASSESSED, AVAILABLE, ARRIVED, TEMPORARY_FAILURE, UNREACHABLE, OUT_OF_RANGE, CHUNK_UNAVAILABLE, DESTINATION_BLOCKED, DESTINATION_INVALID, DESTINATION_MISSING }
    public enum Light { VALID, MISSING, WRONG_BLOCK, CHUNK_UNAVAILABLE }
    public record Assessment(Residence residence,Path path,Light light) {
        public boolean dependenciesReady(){return residence==Residence.VALID&&light==Light.VALID;}
        public String diagnostic(){return "NPC_ENV residence="+residence+" path="+path+" light="+light;}
        public Assessment withPath(Path value){return new Assessment(value==Path.UNREACHABLE?Residence.UNREACHABLE:residence,value,light);}
    }
    private NpcEnvironment() {}
    /** Logical dependency seam; final artwork can change its binding without changing event code. */
    public static boolean isReturnLight(BlockState state){return state.is(WhileAway.RETURN_LIGHT.get());}
    public static Assessment inspect(ServerLevel level,String dimension,BlockPos actor,BlockPos home) {
        BlockPos light=home.offset(2,0,0);
        if(!level.dimension().location().toString().equals(dimension)||level.isOutsideBuildHeight(home.below())||level.isOutsideBuildHeight(home.above()))
            return new Assessment(Residence.CONFLICT,Path.DESTINATION_INVALID,Light.CHUNK_UNAVAILABLE);
        Light ls=!level.hasChunkAt(light)?Light.CHUNK_UNAVAILABLE:isReturnLight(level.getBlockState(light))?Light.VALID:level.getBlockState(light).isAir()?Light.MISSING:Light.WRONG_BLOCK;
        // Never generate/load a chunk merely to answer this observation.
        if(!level.hasChunkAt(actor)||!level.hasChunkAt(home))return new Assessment(Residence.CHUNK_UNAVAILABLE,Path.CHUNK_UNAVAILABLE,ls);
        BlockPos floor=home.below();var ground=level.getBlockState(floor);
        if(ground.isAir())return new Assessment(Residence.MISSING,Path.DESTINATION_MISSING,ls);
        if(!level.getFluidState(home).isEmpty()||!level.getFluidState(floor).isEmpty()||!ground.isCollisionShapeFullBlock(level,floor))
            return new Assessment(Residence.CONFLICT,Path.DESTINATION_INVALID,ls);
        if(!level.getBlockState(home).getCollisionShape(level,home).isEmpty()||!level.getBlockState(home.above()).getCollisionShape(level,home.above()).isEmpty())
            return new Assessment(Residence.TEMPORARILY_BLOCKED,Path.DESTINATION_BLOCKED,ls);
        return new Assessment(Residence.VALID,Path.NOT_ASSESSED,ls);
    }
}
