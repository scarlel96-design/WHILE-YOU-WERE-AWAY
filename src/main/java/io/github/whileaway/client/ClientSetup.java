package io.github.whileaway.client;

import io.github.whileaway.WhileAway;
import io.github.whileaway.entity.Wayfarer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid=WhileAway.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WhileAway.WAYFARER.get(), Renderer::new);
        event.registerEntityRenderer(WhileAway.STORY_NPC.get(), NpcRenderer::new);
    }
    public static final class NpcRenderer extends net.minecraft.client.renderer.entity.MobRenderer<io.github.whileaway.entity.StoryNpc,net.minecraft.client.model.VillagerModel<io.github.whileaway.entity.StoryNpc>> {
        public NpcRenderer(EntityRendererProvider.Context context){super(context,new net.minecraft.client.model.VillagerModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.VILLAGER)),.45f);}
        @Override public ResourceLocation getTextureLocation(io.github.whileaway.entity.StoryNpc entity){return ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");}
    }
    public static final class Model extends GeoModel<Wayfarer> {
        @Override public ResourceLocation getModelResource(Wayfarer entity) { return WhileAway.id("geo/wayfarer.geo.json"); }
        @Override public ResourceLocation getTextureResource(Wayfarer entity) {
            if(entity.getId()<0)return ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
            // Native game texture reference, not a redistributed Mojang asset. Art pass remains pending.
            return ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");
        }
        @Override public ResourceLocation getAnimationResource(Wayfarer entity) { return WhileAway.id("animations/wayfarer.animation.json"); }
    }
    public static final class Renderer extends GeoEntityRenderer<Wayfarer> {
        public Renderer(EntityRendererProvider.Context context) { super(context,new Model()); shadowRadius=.55f; }
    }
}
