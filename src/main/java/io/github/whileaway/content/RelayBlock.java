package io.github.whileaway.content;
import io.github.whileaway.*;
import io.github.whileaway.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class RelayBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public RelayBlock(Properties p) { super(p);registerDefaultState(stateDefinition.any().setValue(LIT,false)); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> b){b.add(LIT);}
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            if(!StoryStorage.available(server.getServer()))return InteractionResult.FAIL;
            var data = NarrativeData.get(server.getServer()); var e = data.entry(player.getUUID());
            if (e.progress.has(Clue.SIGNAL_RESTORED)) CityTransit.enter(server,pos);
            else if (!Director.canRestore(e.progress) || e.station == null || e.station.distSqr(pos) > 32 * 32 || level.dimension() != Level.OVERWORLD)
                player.displayClientMessage(Component.translatable("story.whileaway.read_first"), true);
            else {
                int slot = -1;
                for (int i=0; i<player.getInventory().items.size(); i++) if (player.getInventory().items.get(i).is(Items.REDSTONE)) { slot=i; break; }
                if (slot < 0 && !player.isCreative()) player.displayClientMessage(Component.translatable("story.whileaway.need_redstone"), true);
                else {
                    if (!player.isCreative()) player.getInventory().items.get(slot).shrink(1);
                    data.discover(player.getUUID(), Clue.SIGNAL_RESTORED);
                    level.setBlock(pos,state.setValue(LIT,true),3);
                    var city=server.getServer().getLevel(CityDistrict.KEY);
                    if(city!=null)CityDistrict.get(city).start();
                    e.progress = e.progress.defer(level.getGameTime() + 400);
                    data.setDirty();
                    player.displayClientMessage(Component.translatable("story.whileaway.signal"), false);
                    StoryEvents.sound(server, WhileAway.SIGNAL.get(), SoundSource.BLOCKS, pos, 0.7f);
                    SceneRecovery.begin(server,1,pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
