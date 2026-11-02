package io.github.whileaway.content;

import com.mojang.serialization.MapCodec;
import io.github.whileaway.WhileAway;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Seven solid beam blocks share one distant visual pivot. Collision stays static. */
public final class RailBeamBlock extends BaseEntityBlock {
    public static final MapCodec<RailBeamBlock> CODEC=simpleCodec(RailBeamBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SEGMENT=IntegerProperty.create("segment",0,6);
    public RailBeamBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.EAST).setValue(SEGMENT,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,SEGMENT);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RailBeamEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        return l.isClientSide?createTickerHelper(t,WhileAway.RAIL_BEAM_ENTITY.get(),RailBeamEntity::clientTick):null;
    }
}
