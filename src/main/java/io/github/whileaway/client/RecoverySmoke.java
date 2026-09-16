package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.core.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in fault injection in isolated saves. Never enabled in an ordinary installed client. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class RecoverySmoke {
    private static final String MODE=System.getProperty("whileaway.recovery","");
    private static final String TASK=System.getProperty("whileaway.recoveryTask","");
    private static final int REPEAT=Integer.getInteger("whileaway.repeat",1);
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/stability"));
    private static boolean launched,setup,queued,captured;
    private static volatile int phase;
    private static int ticks,wait,scenario;
    private static volatile int frozen;
    private static void check(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    private static void note(String s){
        try{Files.writeString(E.resolve(TASK+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception ex){throw new IllegalStateException(ex);}
        LogUtils.getLogger().info("WHILEAWAY_RECOVERY {}",s);
    }
    private static void fail(Exception ex){LogUtils.getLogger().error("WHILEAWAY_RECOVERY_FAILED",ex);phase=-1;Minecraft.getInstance().stop();}
    private static void server(java.util.function.Consumer<ServerPlayer> action) {
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();queued=true;
        mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}
            catch(Exception ex){mc.execute(()->fail(ex));}finally{queued=false;}});
    }
    private static NarrativeData.Entry entry(ServerPlayer p){return NarrativeData.get(p.getServer()).entry(p.getUUID());}
    private static void city(ServerPlayer p) {
        var level=p.getServer().getLevel(CityDistrict.KEY);level.getChunkAt(CityHorror.STAGE);
        p.teleportTo(level,40.5,65,25.5,java.util.Set.of(),180,0);p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.fallDistance=0;
    }
    private static void begin(ServerPlayer p) {
        var e=entry(p);e.cityShockTriggered=false;e.scenes.remove(3);e.readingUntil=0;e.transitAfter=0;
        check(CityHorror.tryStart(p),"eligible event start rejected");
        check(!CityHorror.tryStart(p),"duplicate event start accepted");
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||phase<0)return;
        var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);
                check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"client not muted");
                if(MODE.equals("cut")) {
                    String world="whileaway-recovery-"+REPEAT+"-"+System.currentTimeMillis();Files.writeString(E.resolve("world-"+REPEAT+".txt"),world);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("WhileAway recovery fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                } else if(MODE.equals("legacy")) {
                    String world=Files.readString(E.resolve("legacy-world.txt")).trim();
                    check(world.matches("whileaway-legacy-copy-[0-9]+"),"unexpected legacy fixture path");
                    mc.createWorldOpenFlows().openWorld(world,mc::stop);
                } else {
                    String world=Files.readString(E.resolve("world-"+REPEAT+".txt")).trim();
                    check(world.matches("whileaway-recovery-[12]-[0-9]+"),"unexpected fixture path");
                    if(MODE.equals("copy"))world+="-copy";
                    mc.createWorldOpenFlows().openWorld(world,mc::stop);
                }
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())
                if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){button.onPress();break;}
            if(++ticks>12000)throw new IllegalStateException("recovery timeout phase="+phase+" scenario="+scenario);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(!MODE.equals("cut")&&!captured&&ClientScene.active()&&ClientScene.checkpoint()==2&&mc.screen==null) {
                captured=true;Screenshot.grab(E.toFile(),"recovery-"+MODE+"-"+REPEAT+".png",mc.getMainRenderTarget(),c->note("CAPTURE resumed checkpoint 2"));
            }
            if(!setup&&!queued) {
                setup=true;server(p->{
                    if(MODE.equals("cut")) {
                        var e=entry(p);for(var clue:Clue.values())NarrativeData.get(p.getServer()).discover(p.getUUID(),clue);
                        e.progress=e.progress.complete();e.cityVisited=true;e.cityClues=1;
                        p.getInventory().add(new net.minecraft.world.item.ItemStack(Items.DIAMOND,27));
                        var district=CityDistrict.get(p.getServer().getLevel(CityDistrict.KEY));district.start();phase=1;
                    } else if(MODE.equals("legacy")) {
                        var e=entry(p);var city=p.getServer().getLevel(CityDistrict.KEY);var district=CityDistrict.get(city);
                        check(!e.cityShockTriggered,"schema 2 missing shock flag did not default false");
                        check(e.cityVisited&&e.cityClues==7&&e.returnPosition!=null&&e.station!=null,"legacy progression/positions lost");
                        check(district.layoutVersion()==1&&district.cursor()==20988&&district.ready(),"legacy district changed");
                        check(district.advance(city,256)==0,"legacy rebuilt");
                        check(city.getBlockState(new BlockPos(42,64,68)).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),"legacy player edit lost");
                        check(e.scenes.get(1).progress.terminal()&&e.scenes.get(2).progress.terminal(),"old completed presentations replayed");
                        check(NarrativeData.get(p.getServer()).save(new net.minecraft.nbt.CompoundTag(),city.registryAccess()).getInt("schema")==4,"migration not written as 4");
                        note("PASS actual schema 2 save copy: migration to 4, missing flag false, revision 1 and player edit preserved");phase=9;
                    } else {
                        var e=entry(p);var r=e.scenes.get(3);check(r!=null&&r.progress.checkpoint==2&&!r.progress.terminal(),"mid-scene checkpoint lost");
                        try{check(r.id.toString().equals(Files.readString(E.resolve("event-"+REPEAT+".txt")).trim()),"event identity changed");}catch(java.io.IOException ex){throw new IllegalStateException(ex);}
                        check(e.cityClues==1&&!e.cityShockTriggered,"facts mixed or premature completion");
                        note("PASS persisted midpoint identity and facts after non-graceful JVM halt");phase=3;
                    }
                });
            }
            if(queued||ticks%10!=0)return;
            if(phase==1)server(p->{
                if(!CityDistrict.get(p.getServer().getLevel(CityDistrict.KEY)).ready())return;
                city(p);p.getServer().saveEverything(true,true,true); // Fixture baseline, BEFORE the tested event starts.
                begin(p);
                try{Files.writeString(E.resolve("event-"+REPEAT+".txt"),entry(p).scenes.get(3).id.toString());}catch(Exception ex){throw new IllegalStateException(ex);}
                phase=2;
            });
            if(phase==2&&ClientScene.checkpoint()==2&&ClientScene.apparitionCount()==7)server(p->{
                if(entry(p).scenes.get(3).progress.checkpoint<2)return; // Wait for the queued C2S acknowledgement, not just the render clock.
                check(entry(p).scenes.get(3).progress.checkpoint==2,"client checkpoint not durable on server");
                note("PASS "+TASK);note("FAULT: Runtime.halt at checkpoint 2; no shutdown save after event start");
                // Only this opt-in test JVM. No other process or normal save is touched.
                Runtime.getRuntime().halt(0);
            });
            if(phase==3)server(p->{
                var r=entry(p).scenes.get(3);if(!r.progress.terminal())return;
                check(r.progress.checkpoint==4&&entry(p).cityShockTriggered,"resume did not commit completion");
                check(SceneRecovery.integrity(entry(p)).isEmpty(),"integrity after resume");
                check(p.getInventory().countItem(Items.DIAMOND)==27,"inventory changed during cosmetic event");
                note("PASS resumed midpoint to completion without inventory change");
                if(MODE.equals("copy")){entry(p).cityClues=7;SceneRecovery.persist(p.getServer(),NarrativeData.get(p.getServer()));phase=9;}
                else{scenario=0;phase=6;}
            });
            if(phase==6&&!ClientScene.active())server(p->{
                begin(p);phase=7;
            });
            if(phase==7&&ClientScene.checkpoint()==2&&ClientScene.apparitionCount()==7)server(p->{
                if(entry(p).scenes.get(3).progress.checkpoint<2)return;
                frozen=entry(p).scenes.get(3).progress.checkpoint;wait=0;
                int test=scenario%3;
                if(test==0)p.kill();
                else {
                    p.setGameMode(GameType.SPECTATOR);
                    var dest=test==1?p.serverLevel():p.getServer().getLevel(scenario<3?Level.NETHER:Level.END);
                    p.teleportTo(dest,2048.5,128,2048.5,java.util.Set.of(),0,0);
                }
                phase=4;
            });
            if(phase==4) {
                if(mc.screen instanceof DeathScreen){mc.player.respawn();return;}
                wait+=10;
                if(wait>=(scenario%3==1?420:80))server(p->{
                    var r=entry(p).scenes.get(3);check(r.progress.checkpoint==frozen&&!r.progress.terminal(),"absence advanced scene");
                    check(r.progress.state==EventCheckpoint.State.SUSPENDED,"absence not suspended");
                    if(scenario%3==1&&p.serverLevel().hasChunkAt(CityHorror.STAGE)) {
                        check(wait<1200,"origin chunk did not unload within 60 seconds");return;
                    }
                    check(ClientScene.apparitionCount()==0,"actors leaked outside scene context");
                    city(p);phase=5;
                });
            }
            if(phase==5)server(p->{
                if(!entry(p).scenes.get(3).progress.terminal())return;
                check(entry(p).cityShockTriggered&&SceneRecovery.integrity(entry(p)).isEmpty(),"completion/integrity after interruption");
                check(p.getInventory().countItem(Items.DIAMOND)==27,"inventory duplicated or lost (keepInventory fixture)");
                note("PASS interruption="+(scenario%3==0?"death_respawn":scenario%3==1?"chunk_unload_reload":scenario<3?"nether_return":"end_return")+" cycle="+(scenario/3+1));
                scenario++;phase=scenario<6?6:9;
            });
            if(phase==9&&!ClientScene.active()) {
                check(ClientScene.apparitionCount()==0,"final actors leaked");
                check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"mute changed");
                note("PASS "+TASK);phase=-1;mc.stop();
            }
        }catch(Exception ex){fail(ex);}
    }
}
