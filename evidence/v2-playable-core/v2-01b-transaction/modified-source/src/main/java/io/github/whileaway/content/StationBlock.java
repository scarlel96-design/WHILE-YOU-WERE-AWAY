package io.github.whileaway.content;
import com.mojang.serialization.MapCodec;
import io.github.whileaway.WhileAway;
import io.github.whileaway.NarrativeData;
import io.github.whileaway.StoryStorage;
import io.github.whileaway.StoryActors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class StationBlock extends BaseEntityBlock {
    public static final MapCodec<StationBlock> CODEC = simpleCodec(StationBlock::new);
    public StationBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StationEntity(pos, state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!StoryStorage.available(serverPlayer.getServer())) return InteractionResult.FAIL;
        var entry = NarrativeData.get(serverPlayer.getServer()).entry(serverPlayer.getUUID());
        if (!entry.progress.encounterComplete() || !pos.equals(entry.station)) return InteractionResult.PASS;
        var actor=NarrativeData.get(serverPlayer.getServer()).actors.get(StoryActors.chaseId(serverPlayer.getUUID()));
        boolean observed=actor!=null&&actor.facts.contains(StoryActors.CHASE_OBSERVED);
        serverPlayer.displayClientMessage(Component.translatable(observed
            ?"story.whileaway.station_revisit":"story.whileaway.station_revisit_unseen"), false);
        return InteractionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, WhileAway.STATION_ENTITY.get(), StationEntity::tick);
    }
}
