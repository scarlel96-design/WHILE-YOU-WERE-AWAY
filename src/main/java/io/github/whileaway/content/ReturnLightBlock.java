package io.github.whileaway.content;
import io.github.whileaway.CityTransit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class ReturnLightBlock extends Block {
    public ReturnLightBlock(Properties p){super(p);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player p,BlockHitResult hit) {
        if(p instanceof ServerPlayer sp)CityTransit.leave(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
