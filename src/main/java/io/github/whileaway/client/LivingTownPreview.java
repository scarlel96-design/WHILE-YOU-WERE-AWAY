package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in authored-content fixture. This is not a natural-survival acceptance run. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class LivingTownPreview {
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/v2-01d/preview"));
    private static boolean started,queued,done;
    private static volatile int phase;
    private static int ticks,frames;
    private static UUID actor,instance;
    private static Set<String> facts;
    private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static void note(String text) throws Exception {
        Files.createDirectories(E);Files.writeString(E.resolve("preview.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("whileaway.townPreview")||done)return;
        var mc=Minecraft.getInstance();
        try {
            if(!started&&mc.screen instanceof TitleScreen) {
                started=true;note(ClientTestEnvironment.verify(E,"living-preview"));
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("living-preview-"+System.currentTimeMillis(),
                    new LevelSettings("V2-02 scripted content preview",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(420142L,false,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
            }
            if(++ticks>5000)throw new IllegalStateException("PREVIEW_TIMEOUT phase="+phase);
            if(mc.player==null||mc.getSingleplayerServer()==null)return;
            if(phase==6) {
                if(++frames==30)Screenshot.grab(E.toFile(),"maintenance-living.png",mc.getMainRenderTarget(),c->{});
                if(frames<50)return;
                done=true;mc.getSingleplayerServer().execute(()->{
                    try{note("PASS SCRIPTED_REAL_CLIENT living spaces, actual NPC rest/home/work route, same identity, circuit; NOT HUMAN PLAYTEST");}
                    catch(Exception ex){ex.printStackTrace();}
                    mc.getSingleplayerServer().saveEverything(true,true,true);mc.getSingleplayerServer().halt(false);mc.execute(mc::stop);
                });return;
            }
            if(queued||ticks%10!=0)return;
            queued=true;mc.getSingleplayerServer().execute(()->{
                try {
                    var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());
                    if(p==null||!StoryStorage.available(server))return;
                    var data=NarrativeData.get(server);var city=server.getLevel(CityDistrict.KEY);
                    if(phase==0) {
                        var entry=data.entry(p.getUUID());
                        while(entry.openingStage!=OpeningStage.COMPLETED)
                            data.commitOpening(p.getUUID(),entry.openingStage,p.blockPosition(),0,0,server.registryAccess());
                        CityDistrict.get(city).start();phase=1;note("FIXTURE opening completed through existing commit API; not acceptance evidence");return;
                    }
                    if(phase==1) {
                        if(!CityDistrict.get(city).ready())return;
                        check(CityDistrict.get(city).livingContent(),"old construction plan selected");
                        if(!p.teleportTo(city,41.5,65,68.5,Set.of(),45,0))return;
                        server.overworld().setDayTime(0);phase=2;return;
                    }
                    if(p.level()!=city)return;
                    var r=data.actors.get(NpcEvents.STORY);
                    if(r==null) {
                        r=NpcEvents.begin(p,new BlockPos(42,65,68),new BlockPos(38,65,72));
                        if(r==null) {
                            if(ticks%100==0)note("NPC_WAIT start="+city.getBlockState(new BlockPos(42,65,68))+" home="+city.getBlockState(new BlockPos(38,65,72))+" floor="+city.getBlockState(new BlockPos(38,64,72)));
                            return;
                        }
                    }
                    var npc=(StoryNpc)city.getEntity(r.entityId);if(npc==null)return;
                    if(phase==2) {
                        if(r.checkpoint.checkpoint==1)NpcEvents.interact(npc,p);
                        if(r.checkpoint.checkpoint!=4)return;
                        actor=r.entityId;instance=r.instanceId;facts=Set.copyOf(r.facts);
                        mc.execute(()->Screenshot.grab(E.toFile(),"caretaker-home.png",mc.getMainRenderTarget(),c->{}));
                        check(city.getBlockState(new BlockPos(60,64,73)).is(WhileAway.ROUTE_TRACE.get()),"residential observation absent");
                        check(city.getBlockState(new BlockPos(54,65,64)).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE),"usable residential table absent");
                        server.overworld().setDayTime(12000);phase=3;note("PASS resident settled; residential floor trace and workbench exist");return;
                    }
                    check(actor.equals(r.entityId)&&instance.equals(r.instanceId)&&r.generation==0&&facts.equals(r.facts),"daily routine changed identity or facts");
                    var target=phase==3?new BlockPos(37,65,73):phase==4?new BlockPos(38,65,72):new BlockPos(60,65,48);
                    if(npc.distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5)>1.4)return;
                    if(phase==3){server.overworld().setDayTime(14000);phase=4;note("PASS actual navigation to rest place");return;}
                    if(phase==4){server.overworld().setDayTime(10500);phase=5;note("PASS actual navigation home");return;}
                    var lever=io.github.whileaway.content.LifeObservationBlock.SWITCH;
                    city.getBlockState(lever).useWithoutItem(city,p,new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(lever),net.minecraft.core.Direction.UP,lever,false));
                    check(city.getBlockState(lever.below()).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),"service switch failed");
                    note("PASS actual navigation workshop UUID="+actor+" instance="+instance+" generation="+r.generation+" checkpoint="+r.checkpoint.checkpoint);
                    p.teleportTo(city,60.5,66,51.5,Set.of(),180,10);phase=6;
                }catch(Exception ex){done=true;ex.printStackTrace();try{note("FAIL "+ex);}catch(Exception ignored){}mc.execute(mc::stop);}
                finally{queued=false;}
            });
        }catch(Exception ex){done=true;ex.printStackTrace();mc.stop();}
    }
}
