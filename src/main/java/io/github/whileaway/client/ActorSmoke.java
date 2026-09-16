package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.Wayfarer;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in actual-client crash chain for the shipped pursuer. Test movement is not natural survival QA. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ActorSmoke {
    private static final String MODE=System.getProperty("whileaway.actorBoundary","");
    private static final int REPEAT=Integer.getInteger("whileaway.actorRepeat",1);
    private static final String TASK="runActor"+MODE+REPEAT;
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/wayfarer-stability/crash"));
    private static boolean launched,setup;
    private static volatile boolean queued;
    private static volatile int phase;
    private static int ticks,stable;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void note(String s) {
        try{Files.writeString(E.resolve(TASK+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new IllegalStateException(ex);}
        com.mojang.logging.LogUtils.getLogger().info("WHILEAWAY_ACTOR {}",s);
    }
    private static StoryActorRecord record(ServerPlayer p){return NarrativeData.get(p.getServer()).actors.get(StoryActors.chaseId(p.getUUID()));}
    private static void failure(Exception ex){com.mojang.logging.LogUtils.getLogger().error("ACTOR_FAILED",ex);note("FAIL "+ex);phase=-1;Minecraft.getInstance().stop();}
    private static void server(java.util.function.Consumer<ServerPlayer> f) {
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();queued=true;
        mc.getSingleplayerServer().execute(()->{try{f.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}
            catch(Exception ex){mc.execute(()->failure(ex));}finally{queued=false;}});
    }
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent event) {
        if(MODE.isEmpty()||!event.eventId.equals(StoryActors.CHASE))return;
        try {
            boolean target=switch(MODE){case "A"->event.boundary.equals("PREPARED");case "B"->event.boundary.equals("FACT_COMMITTED");
                case "C"->event.boundary.equals("CHECKPOINT_COMMITTED")&&event.checkpoint==2;case "D"->event.boundary.equals("BEFORE_COMPLETE");
                case "E"->event.boundary.equals("AFTER_COMPLETE");default->false;};
            if(MODE.equals("Verify")&&event.boundary.equals("AFTER_COMPLETE"))throw new IllegalStateException("completion replayed");
            if(!target)return;
            var p=event.player;var r=record(p);int expected=switch(MODE){case "A"->0;case "B"->1;case "C"->2;case "D"->3;default->4;};
            check(r.checkpoint.checkpoint==expected,"wrong boundary");
            if(MODE.equals("A"))Files.writeString(E.resolve("actor-instance-"+REPEAT+".txt"),r.instanceId.toString());
            check(r.instanceId.toString().equals(Files.readString(E.resolve("actor-instance-"+REPEAT+".txt")).trim()),"logical event instance drift");
            check(p.getInventory().countItem(Items.DIAMOND)==27,"fixture inventory changed");
            if(expected>=2)check(r.facts.contains("pursuit_started"),"hunt fact missing");
            if(expected>=3)check(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED&&NarrativeData.get(p.getServer()).entry(p.getUUID()).progress.encounterComplete(),"retirement not atomic with completion facts");
            var disk=p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/whileaway_story.dat");
            Files.createDirectories(E.resolve("snapshots"));Files.copy(disk,E.resolve("snapshots/actor-"+REPEAT+"-"+MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
            note("PASS "+TASK+" boundary="+event.boundary+" checkpoint="+expected+" generation="+r.generation+" instance="+r.instanceId);
            note("FAULT Runtime.halt after durable story write; no shutdown save");Runtime.getRuntime().halt(0);
        }catch(Exception ex){failure(ex);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||phase<0)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"client not muted");
                note(ClientTestEnvironment.verify(E,TASK));
                if(MODE.equals("A")) {
                    String world="whileaway-actor-"+REPEAT+"-"+System.currentTimeMillis();Files.writeString(E.resolve("actor-world-"+REPEAT+".txt"),world);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("Actor recovery fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(74192361L+REPEAT,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }else {
                    var world=Files.readString(E.resolve("actor-world-"+REPEAT+".txt")).trim();check(world.matches("whileaway-actor-[12]-[0-9]+"),"unexpected fixture world");
                    mc.createWorldOpenFlows().openWorld(world,mc::stop);
                }
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())
                if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){button.onPress();break;}
            if(++ticks>7500)throw new IllegalStateException("timeout phase="+phase+" mode="+MODE);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||queued||ticks%10!=0)return;
            if(!setup) {
                setup=true;server(p->{
                    if(MODE.equals("A")) {
                        var pos=new BlockPos(0,81,0);var level=p.serverLevel();
                        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<=3;y++)level.setBlock(pos.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                        p.teleportTo(level,.5,81,.5,java.util.Set.of(),0,0);p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.fallDistance=0;
                        var e=NarrativeData.get(p.getServer()).entry(p.getUUID());e.station=pos;
                        p.getInventory().add(new ItemStack(Items.DIAMOND,27));StoryActors.persist(p.getServer());
                        p.getServer().saveEverything(true,true,true); // ONLY fixture baseline, before any tested operation.
                        var actor=WhileAway.WAYFARER.get().create(level);actor.moveTo(3.5,81,.5,0,0);actor.bind(p.getUUID(),pos);
                        check(StoryActors.beginChase(p,actor),"start rejected");
                    }else {
                        var r=record(p);check(r!=null,"reservation missing after crash");
                        check(r.instanceId.toString().equals(readInstance()),"instance lost on reload");
                        check(p.getInventory().countItem(Items.DIAMOND)==27,"fixture inventory lost");
                        note("PASS reload "+MODE+" checkpoint="+r.checkpoint.checkpoint+" facts="+r.facts);
                        phase=MODE.equals("Verify")?3:2;
                    }
                });return;
            }
            if(phase==2)server(p->{
                var r=record(p);var actor=StoryActors.find(p.getServer(),r.entityId);
                if(actor==null)return;
                if(MODE.equals("C")) {
                    p.teleportTo(p.serverLevel(),actor.getX()+3,actor.getY(),actor.getZ(),java.util.Set.of(),180,0);p.setHealth(20);p.fallDistance=0;
                }else if(MODE.equals("D")) {
                    check(r.facts.contains("pursuit_started")&&r.checkpoint.checkpoint==2,"C fact not durable");
                    // Real damage enters Wayfarer.die -> completeChase. The D cut remains before final confirmation.
                    actor.hurt(p.damageSources().playerAttack(p),1000);
                }
            });
            if(phase==3)server(p->{
                var r=record(p);var e=NarrativeData.get(p.getServer()).entry(p.getUUID());
                check(r.checkpoint.checkpoint==4&&r.noticeClaimed&&e.encounter==null&&e.progress.encounterComplete(),"E completion lost");
                check(StoryActors.find(p.getServer(),r.entityId)==null,"retired pursuer remains");
                check(StoryActors.integrity(p.getServer()).isEmpty(),"actor integrity failed");
                stable+=10;if(stable>=300){note("PASS "+TASK+" completed state stable 300 ticks; no actor or completion replay; integrity clean");phase=-1;mc.execute(mc::stop);}
            });
        }catch(Exception ex){failure(ex);}
    }
    private static String readInstance(){try{return Files.readString(E.resolve("actor-instance-"+REPEAT+".txt")).trim();}catch(Exception ex){throw new IllegalStateException(ex);}}
}
