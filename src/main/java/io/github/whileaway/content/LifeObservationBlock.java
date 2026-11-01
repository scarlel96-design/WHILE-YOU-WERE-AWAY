package io.github.whileaway.content;

import io.github.whileaway.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Optional physical observations. These objects grant no campaign facts or items. */
public final class LifeObservationBlock extends Block {
    public static final BlockPos SWITCH=new BlockPos(65,65,46), LAMP=SWITCH.below();
    private final boolean maintenance;
    public LifeObservationBlock(boolean maintenance,Properties properties){super(properties);this.maintenance=maintenance;}
    public static String maintenanceKey(BlockState lever,BlockState lamp) {
        if(!lever.is(Blocks.LEVER)||!lamp.is(Blocks.REDSTONE_LAMP))return "story.whileaway.life.changed_equipment";
        return lever.getValue(LeverBlock.POWERED)&&lamp.getValue(RedstoneLampBlock.LIT)
            ?"story.whileaway.life.maintenance_response":"story.whileaway.life.maintenance_record";
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer p) {
            if(!StoryStorage.available(p.getServer()))return InteractionResult.FAIL;
            String key="story.whileaway.life.threshold";
            if(maintenance) {
                if(!level.dimension().equals(CityDistrict.KEY)||!pos.equals(new BlockPos(66,65,46))
                    ||!level.hasChunkAt(SWITCH)||!level.hasChunkAt(LAMP))key="story.whileaway.life.changed_equipment";
                else key=maintenanceKey(level.getBlockState(SWITCH),level.getBlockState(LAMP));
            }
            p.displayClientMessage(Component.translatable(key),false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
