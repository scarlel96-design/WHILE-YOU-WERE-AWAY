package io.github.whileaway.content;
import io.github.whileaway.WhileAway;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class NoteBlock extends Block {
    private final String kind;
    public NoteBlock(String kind, Properties p) { super(p); this.kind = kind; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            var item = switch(kind) { case "evacuation" -> WhileAway.EVACUATION_BOOK.get(); case "personal" -> WhileAway.PERSONAL_BOOK.get();
                case "city_intake" -> WhileAway.CITY_INTAKE_BOOK.get();case "city_home" -> WhileAway.CITY_HOME_BOOK.get();case "city_siren" -> WhileAway.CITY_SIREN_BOOK.get();
                default -> WhileAway.WARNING_BOOK.get(); };
            if (!player.getInventory().contains(new ItemStack(item))) {
                var stack = new ItemStack(item);
                if (!player.addItem(stack)) player.drop(stack, false);
            }
            player.displayClientMessage(Component.translatable("story.whileaway.record_received"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
