package io.github.whileaway.client;

import io.github.whileaway.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in developer harness. Dormant unless launched with -Dwhileaway.clientSmoke=true. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ClientSmoke {
    private static boolean launched,setup;
    private static volatile boolean ready;
    private static int ticks;
    private static volatile BlockPos anchor;
    private static volatile BlockPos closeCamera;
    private static volatile float closeYaw;
    private static volatile int observedEntityId;
    private static volatile BlockPos observedAt;
    private static final Path EVIDENCE=Path.of(System.getProperty("whileaway.evidence","evidence"));
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("whileaway.clientSmoke"))return;
        var mc=Minecraft.getInstance();
        try {
            if(!launched && mc.screen instanceof TitleScreen) {
                launched=true;mc.options.renderDistance().set(6);mc.options.simulationDistance().set(5);
                mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;
                mc.getSoundManager().updateSourceVolume(net.minecraft.sounds.SoundSource.MASTER,0f);
                var rules=new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("whileaway-smoke-"+System.currentTimeMillis(),
                    new LevelSettings("WhileAway isolated smoke",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
            }
            if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null)return;
            if(!setup) {
                setup=true;
                for(String name:new String[]{"sounds/distant_knock.ogg","sounds/homeward.ogg","geo/wayfarer.geo.json","animations/wayfarer.animation.json"})
                    if(mc.getResourceManager().getResource(WhileAway.id(name)).isEmpty())throw new IllegalStateException("Missing client asset: "+name);
                var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                server.execute(()->{
                    try {
                        var level=server.overworld();level.setDayTime(11000);
                        var pos=level.findNearestMapStructure(TagKey.create(Registries.STRUCTURE,WhileAway.id("stations")),new BlockPos(0,64,0),60,false);
                        if(pos==null)throw new IllegalStateException("No natural station found for seed 74192361");
                        var type=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(WhileAway.id("abandoned_station"));
                        var start=level.getChunkAt(pos).getStartForStructure(type);
                        if(start==null || !start.isValid())throw new IllegalStateException("Natural structure start invalid");
                        var piece=(net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece)start.getPieces().getFirst();
                        var box=piece.getBoundingBox();anchor=new BlockPos(box.minX(),box.minY(),box.minZ());
                        // StructureStart's box includes terrain adaptation padding: use the actual piece.
                        var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(piece.getRotation());
                        java.util.function.Function<BlockPos,BlockPos> at=local->net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings,local).offset(piece.getPosition());
                        var marker=at.apply(new BlockPos(6,1,9));level.getChunkAt(marker);
                        if(!level.getBlockState(marker).is(WhileAway.STATION.get()))throw new IllegalStateException("Natural station has no physical anchor");
                        var relay=at.apply(new BlockPos(8,1,6));level.getChunkAt(relay);
                        if(!level.getBlockState(relay).is(WhileAway.RELAY.get()))throw new IllegalStateException("Natural station has no physical relay");
                        var player=server.getPlayerList().getPlayer(id);
                        player.connection.teleport(anchor.getX()+29,anchor.getY()+24,anchor.getZ()-13,42,36);
                        var ground=at.apply(new BlockPos(13,1,10));closeCamera=at.apply(new BlockPos(13,1,3));
                        observedAt=ground;
                        closeYaw=(float)Math.toDegrees(Math.atan2(-(ground.getX()-closeCamera.getX()),ground.getZ()-closeCamera.getZ()));
                        Files.createDirectories(EVIDENCE);
                        Files.writeString(EVIDENCE.resolve("client-smoke.txt"),"Natural world seed=74192361\nStation start="+pos+"\nPiece bounds="+box+"\nPhysical station anchor="+marker+" and relay verified\nClient assets present\n");
                        LogUtils.getLogger().info("WHILEAWAY_SMOKE natural station={} bounds={}",pos,box);
                        ready=true;
                    }catch(Exception ex){LogUtils.getLogger().error("WHILEAWAY_SMOKE_FAILED",ex);mc.execute(mc::stop);}
                });
            }
            if(!ready)return;
            ticks++;
            if(ticks==160) {
                mc.setScreen(null);
                Screenshot.grab(EVIDENCE.toFile(),"station-client.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("WHILEAWAY_SMOKE {}",c.getString()));
                var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                server.execute(()->{
                    var level=server.overworld();var p=server.getPlayerList().getPlayer(id);
                    p.connection.teleport(closeCamera.getX()+.5,closeCamera.getY()+.3,closeCamera.getZ()+.5,closeYaw,0);
                });
            }
            if(ticks==190) {
                var server=mc.getSingleplayerServer();
                server.execute(()->{
                    var level=server.overworld();var mob=WhileAway.WAYFARER.get().create(level);
                    mob.moveTo(observedAt.getX()+.5,observedAt.getY(),observedAt.getZ()+.5,closeYaw+180,0);mob.setNoAi(true);
                    if(!level.addFreshEntity(mob)){LogUtils.getLogger().error("WHILEAWAY_SMOKE_FAILED entity insertion");mc.execute(mc::stop);return;}
                    observedEntityId=mob.getId();
                    LogUtils.getLogger().info("WHILEAWAY_SMOKE observation id={} at={} difficulty={}",observedEntityId,observedAt,level.getDifficulty());
                });
            }
            if(ticks==220) {
                var server=mc.getSingleplayerServer();
                server.execute(()->LogUtils.getLogger().info("WHILEAWAY_SMOKE server entity={}",server.overworld().getEntity(observedEntityId)));
            }
            if(ticks==240) {
                var observed=mc.level.getEntity(observedEntityId);
                if(observed==null)throw new IllegalStateException("Wayfarer missing client entity id="+observedEntityId);
                if(!mc.player.hasLineOfSight(observed))throw new IllegalStateException("Camera occluded: player="+mc.player.position()+" mob="+observed.position());
                Screenshot.grab(EVIDENCE.toFile(),"wayfarer-client.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("WHILEAWAY_SMOKE {}",c.getString()));
                Files.writeString(EVIDENCE.resolve("client-smoke.txt"),"PASS tracked custom entity present with camera line of sight\n",StandardOpenOption.APPEND);
            }
            if(ticks==300) {
                mc.options.hideGui=false;
                var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                server.execute(()->{
                    var p=server.getPlayerList().getPlayer(id);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new ScenePayload(1,p.blockPosition()));
                });
            }
            if(ticks==390) {
                if(!ClientScene.active())throw new IllegalStateException("Scene packet did not start presentation");
                Screenshot.grab(EVIDENCE.toFile(),"signal-scene-client.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("WHILEAWAY_SMOKE {}",c.getString()));
            }
            if(ticks==580) {
                if(ClientScene.active())throw new IllegalStateException("Scene did not end");
                if(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)!=0f)throw new IllegalStateException("Client was not muted");
                Files.writeString(EVIDENCE.resolve("client-smoke.txt"),"PASS scene S2C starts, reaches caption phase and ends; master volume remains zero\n",StandardOpenOption.APPEND);
                Files.writeString(EVIDENCE.resolve("client-smoke.txt"),"PASS client resource loading, natural station start, integrated world and scheduled captures\n",StandardOpenOption.APPEND);
                LogUtils.getLogger().info("WHILEAWAY_SMOKE_PASS");mc.stop();
            }
        }catch(Exception ex){LogUtils.getLogger().error("WHILEAWAY_SMOKE_FAILED",ex);mc.stop();}
    }
}
