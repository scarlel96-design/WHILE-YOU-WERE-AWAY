package io.github.whileaway.client;

import io.github.whileaway.NarrativeData;
import io.github.whileaway.OpeningEvents;
import io.github.whileaway.OpeningStage;
import io.github.whileaway.StoryStorage;
import io.github.whileaway.WhileAway;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in real-client check for the first opening route and its saved return. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class OpeningSmoke {
    private static final String MODE=System.getProperty("whileaway.openingSmoke","");
    private static final String WORLD=MODE.equals("Cut")||MODE.equals("Resume")
        ?"opening-v2-resume-fixture":"opening-v2-fixture-2";
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/v2-opening/client"));
    private static boolean launched;
    private static volatile boolean queued,finished;
    private static int ticks,step;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void note(String line) throws Exception {
        Files.createDirectories(E);
        Files.writeString(E.resolve(MODE+".txt"),line+"\n",java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||finished)return;
        var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;note(ClientTestEnvironment.verify(E,MODE));
                if(MODE.equals("Fresh")||MODE.equals("Cut")) {
                    check(Files.notExists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD)),"fixture world already exists");
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings("Opening V2 fixture",GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                } else mc.createWorldOpenFlows().openWorld(WORLD,mc::stop);
            }
            if(++ticks>12000)throw new IllegalStateException("opening timeout step="+step);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%10!=0)return;
            queued=true;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var server=mc.getSingleplayerServer();
                    ServerPlayer p=server.getPlayerList().getPlayer(mc.player.getUUID());
                    if(p==null||!StoryStorage.available(server))return;
                    var data=NarrativeData.get(server);var state=data.entry(p.getUUID()).openingStage;
                    var memory=server.getLevel(OpeningEvents.MEMORY);
                    check(memory!=null,"memory dimension absent");
                    if(MODE.equals("Reopen")) {
                        check(state==OpeningStage.COMPLETED&&p.level().dimension()==Level.OVERWORLD,"completed opening replayed");
                        if(step++<5)return;
                        note("PASS Reopen stage="+state+" dimension=overworld stableTicks=50");
                        finish(mc,server);return;
                    }
                    if(MODE.equals("Resume")&&step==0) {
                        check(state==OpeningStage.STARTED&&p.level().dimension()==OpeningEvents.MEMORY,
                            "interrupted opening did not resume in memory");
                        check(p.getX()>=27&&p.getX()<33,"first gap replay moved player backward");
                        p.teleportTo(memory,33.5,129,.5,Set.of(),-90,0);step=3;
                        return;
                    }
                    if(step>=4&&state==OpeningStage.COMPLETED&&p.level().dimension()==Level.OVERWORLD) {
                        var file=server.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");
                        var disk=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getCompound("data");
                        var records=disk.getList("players",10);boolean found=false;
                        for(int i=0;i<records.size();i++) {
                            var record=records.getCompound(i);
                            if(record.hasUUID("id")&&record.getUUID("id").equals(p.getUUID()))
                                found=record.getString("openingStage").equals("COMPLETED");
                        }
                        check(found,"completed stage absent on disk");
                        note("PASS "+MODE+" stage=COMPLETED dimension=overworld gate=15_blocks lever=player_use gaps=2 diskHash="+
                            StoryStorage.digest(Files.readAllBytes(file)));
                        finish(mc,server);return;
                    }
                    if(state!=OpeningStage.STARTED||p.level().dimension()!=OpeningEvents.MEMORY)return;
                    switch(step) {
                        case 0 -> {
                            check(memory.getBlockState(new BlockPos(16,129,0)).is(net.minecraft.world.level.block.Blocks.IRON_BARS),"service gate absent");
                            for(int y=129;y<=131;y++)for(int z=-2;z<=2;z++)
                                check(memory.getBlockState(new BlockPos(16,y,z)).is(net.minecraft.world.level.block.Blocks.IRON_BARS),"service gate bypass");
                            p.teleportTo(memory,12.5,129,.5,Set.of(),-90,0);step++;
                        }
                        case 1 -> {
                            var lever=new BlockPos(12,129,1);
                            p.gameMode.useItemOn(p,memory,p.getMainHandItem(),InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(lever),Direction.NORTH,lever,false));
                            check(memory.getBlockState(lever).getValue(net.minecraft.world.level.block.LeverBlock.POWERED),"player block use did not power signal");
                            p.teleportTo(memory,19.5,129,.5,Set.of(),-90,0);step++;
                        }
                        case 2 -> {
                            if(p.getX()<23)return;
                            if(MODE.equals("Cut")) {
                                p.teleportTo(memory,29.5,129,.5,Set.of(),-90,0);
                                note("PASS Cut stage=STARTED dimension=memory_route x=29 after_first_gap");
                                finish(mc,server);return;
                            }
                            p.teleportTo(memory,33.5,129,.5,Set.of(),-90,0);step++;
                        }
                        case 3 -> {if(p.getX()<36)return;p.teleportTo(memory,39.5,129,.5,Set.of(),-90,0);step++;}
                        case 4 -> {check(state==OpeningStage.STARTED,"opening stage changed early");step++;}
                        default -> {}
                    }
                }catch(Exception ex){fail(ex);}
                finally {queued=false;}
            });
        }catch(Exception ex){fail(ex);}
    }
    private static void finish(Minecraft mc,net.minecraft.server.MinecraftServer server) throws Exception {
        finished=true;server.saveEverything(true,true,true);server.halt(false);mc.execute(mc::stop);
    }
    private static void fail(Exception ex) {
        try{note("FAIL "+ex);}catch(Exception ignored){}
        ex.printStackTrace();Runtime.getRuntime().halt(8);
    }
}
