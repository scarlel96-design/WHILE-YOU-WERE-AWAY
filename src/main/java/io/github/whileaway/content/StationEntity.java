package io.github.whileaway.content;
import io.github.whileaway.*;
import io.github.whileaway.core.Clue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class StationEntity extends BlockEntity {
    public StationEntity(BlockPos pos, BlockState state) { super(WhileAway.STATION_ENTITY.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, StationEntity self) {
        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 40 != 0) return;
        if(!StoryStorage.available(server.getServer()))return;
        var data = NarrativeData.get(server.getServer());
        for (ServerPlayer p : server.players()) {
            if (p.isSpectator() || p.blockPosition().distSqr(pos) > 20 * 20) continue;
            var e = data.entry(p.getUUID());
            if (!e.progress.has(Clue.STATION)) {
                e.station = pos.immutable();
                data.discover(p.getUUID(), Clue.STATION);
                e.progress = e.progress.defer(level.getGameTime() + 200);
                p.displayClientMessage(Component.translatable("story.whileaway.station"), true);
            }
        }
    }
}
