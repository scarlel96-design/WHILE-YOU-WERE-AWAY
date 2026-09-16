package io.github.whileaway;

import io.github.whileaway.content.*;
import io.github.whileaway.content.NoteBlock;
import io.github.whileaway.entity.Wayfarer;
import io.github.whileaway.entity.StoryNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

@Mod(WhileAway.ID)
public final class WhileAway {
    public static final String ID = "whileaway";
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final DeferredBlock<StationBlock> STATION = BLOCKS.register("station_anchor", () -> new StationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS).strength(4)));
    public static final DeferredBlock<NoteBlock> WARNING = BLOCKS.register("warning_note", () -> new NoteBlock("warning", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<NoteBlock> EVACUATION = BLOCKS.register("evacuation_note", () -> new NoteBlock("evacuation", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<NoteBlock> PERSONAL = BLOCKS.register("personal_note", () -> new NoteBlock("personal", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<RelayBlock> RELAY = BLOCKS.register("signal_relay", () -> new RelayBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).lightLevel(s -> s.getValue(RelayBlock.LIT)?12:3)));
    public static final DeferredBlock<ReturnLightBlock> RETURN_LIGHT=BLOCKS.register("return_light",()->new ReturnLightBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).strength(-1,3600000).lightLevel(s->15)));
    public static final DeferredBlock<NoteBlock> CITY_INTAKE=BLOCKS.register("city_intake_note",()->new NoteBlock("city_intake",BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<NoteBlock> CITY_HOME=BLOCKS.register("city_home_note",()->new NoteBlock("city_home",BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<NoteBlock> CITY_SIREN=BLOCKS.register("city_siren_note",()->new NoteBlock("city_siren",BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StationEntity>> STATION_ENTITY = BLOCK_ENTITIES.register("station_anchor", () -> BlockEntityType.Builder.of(StationEntity::new, STATION.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<Wayfarer>> WAYFARER = ENTITIES.register("wayfarer", () -> EntityType.Builder.<Wayfarer>of(Wayfarer::new, MobCategory.MONSTER).sized(0.75f, 2.6f).clientTrackingRange(10).build("whileaway:wayfarer"));
    public static final DeferredHolder<EntityType<?>, EntityType<StoryNpc>> STORY_NPC = ENTITIES.register("story_npc", () -> EntityType.Builder.<StoryNpc>of(StoryNpc::new, MobCategory.CREATURE).sized(.6f,1.95f).clientTrackingRange(10).build("whileaway:story_npc"));
    public static final DeferredItem<NoteItem> WARNING_BOOK = ITEMS.register("warning_record", () -> new NoteItem("warning"));
    public static final DeferredItem<NoteItem> EVACUATION_BOOK = ITEMS.register("evacuation_record", () -> new NoteItem("evacuation"));
    public static final DeferredItem<NoteItem> PERSONAL_BOOK = ITEMS.register("personal_record", () -> new NoteItem("personal"));
    public static final DeferredItem<NoteItem> CITY_INTAKE_BOOK=ITEMS.register("city_intake_record",()->new NoteItem("city_intake"));
    public static final DeferredItem<NoteItem> CITY_HOME_BOOK=ITEMS.register("city_home_record",()->new NoteItem("city_home"));
    public static final DeferredItem<NoteItem> CITY_SIREN_BOOK=ITEMS.register("city_siren_record",()->new NoteItem("city_siren"));
    public static final DeferredItem<Item> WAYFARER_EGG = ITEMS.register("wayfarer_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(WAYFARER, 0x23272b, 0xc6bda4, new Item.Properties()));
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOCK = sound("distant_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEPS = sound("returning_steps");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREATH = sound("wayfarer_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATTACK = sound("wayfarer_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIGNAL = sound("signal_wake");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESOLVE = sound("homeward");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNEASE = sound("station_unease");
    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("story", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.whileaway"))
        .icon(() -> WARNING_BOOK.get().getDefaultInstance())
        .displayItems((p, out) -> {
            ITEMS.getEntries().forEach(i -> out.accept(i.get()));
        }).build());
    public WhileAway(IEventBus bus, ModContainer container) {
        BLOCKS.getEntries().forEach(b -> ITEMS.registerSimpleBlockItem(b.getId().getPath(), b));
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus); TABS.register(bus);
        bus.addListener(this::attributes);
        container.registerConfig(ModConfig.Type.SERVER, StoryConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar=event.registrar("2");
            registrar.playToClient(ScenePayload.TYPE,ScenePayload.CODEC,
                (payload,context)->context.enqueueWork(()->io.github.whileaway.client.ClientScene.receive(payload)));
            registrar.playToServer(SceneAck.TYPE,SceneAck.CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof net.minecraft.server.level.ServerPlayer p)SceneRecovery.acknowledge(p,payload);
            }));
        });
        NeoForge.EVENT_BUS.register(new StoryEvents());
        NeoForge.EVENT_BUS.register(new CityDistrict());
        NeoForge.EVENT_BUS.register(new CityHorror());
        NeoForge.EVENT_BUS.register(new SceneRecovery());
        NeoForge.EVENT_BUS.register(new StoryActors());
        NeoForge.EVENT_BUS.register(new CanonicalRecovery());
        NeoForge.EVENT_BUS.register(new NpcEvents());
    }
    private void attributes(EntityAttributeCreationEvent event) { event.put(WAYFARER.get(), Wayfarer.attributes().build()); event.put(STORY_NPC.get(),StoryNpc.attributes().build()); }
}
