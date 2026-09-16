package io.github.whileaway.content;
import io.github.whileaway.NarrativeData;
import io.github.whileaway.core.Clue;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public final class NoteItem extends WrittenBookItem {
    private final Clue clue;
    private final int cityBit;
    public NoteItem(String kind) {
        super(new Item.Properties().stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough("While You Were Away"), "저녁골", 0,
                List.of(Filterable.passThrough(Component.translatable("record.whileaway." + kind + ".1")),
                        Filterable.passThrough(Component.translatable("record.whileaway." + kind + ".2"))), true)));
        clue = switch(kind) { case "evacuation" -> Clue.EVACUATION_READ; case "personal" -> Clue.PERSONAL_READ; default -> Clue.WARNING_READ; };
        cityBit=switch(kind){case "city_intake"->1;case "city_home"->2;case "city_siren"->4;default->0;};
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            if(!io.github.whileaway.StoryStorage.available(server.getServer()))return InteractionResultHolder.fail(player.getItemInHand(hand));
            var data = NarrativeData.get(server.getServer());
            var e = data.entry(player.getUUID());
            if(cityBit==0) {
                if(e.progress.has(Clue.STATION))data.discover(player.getUUID(),clue);
            } else if(e.cityVisited&&level.dimension().equals(io.github.whileaway.CityDistrict.KEY)) {
                io.github.whileaway.CityInvestigation.read(server,cityBit);
            }
            long now = server.getServer().overworld().getGameTime();
            e.readingUntil = now + 1200;
            e.progress = e.progress.defer(e.readingUntil);
            data.setDirty();
        }
        return super.use(level, player, hand);
    }
}
