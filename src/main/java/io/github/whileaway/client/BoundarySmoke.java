package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.core.*;
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

/** Explicit opt-in isolated JVM fault tests. Not natural survival or general NPC/transaction QA. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class BoundarySmoke {
    private static final String MODE=System.getProperty("whileaway.boundary","");
    private static final int KIND=Integer.getInteger("whileaway.boundaryKind",1);
    private static final String ID=KIND==1?"signal_wake":KIND==2?"city_arrival":"city_records";
    private static final String TASK="runBoundary"+KIND+MODE;
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/reuse"));
    private static boolean launched,setup;
    private static volatile boolean queued;
    private static volatile int phase;
    private static int ticks,stable;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void note(String s){
        try {Files.writeString(E.resolve(TASK+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception ex){throw new IllegalStateException(ex);}
        com.mojang.logging.LogUtils.getLogger().info("WHILEAWAY_BOUNDARY {}",s);
    }
    private static NarrativeData.Entry entry(ServerPlayer p){return NarrativeData.get(p.getServer()).entry(p.getUUID());}
    private static void failure(Exception ex){com.mojang.logging.LogUtils.getLogger().error("BOUNDARY_FAILED",ex);phase=-1;Minecraft.getInstance().stop();}
    private static void server(java.util.function.Consumer<ServerPlayer> f) {
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();queued=true;
        mc.getSingleplayerServer().execute(()->{try{f.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}
            catch(Exception ex){mc.execute(()->failure(ex));}finally{queued=false;}});
    }
    private static void facts(ServerPlayer p) {
        var e=entry(p);
        check(p.getInventory().countItem(Items.DIAMOND)==27,"fixture inventory drift");
        check(e.progress.has(Clue.SIGNAL_RESTORED)&&e.progress.encounterComplete(),"permanent story facts lost");

    }
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent event) {
        if(MODE.isEmpty()||!event.eventId.equals(ID))return;
        try {
            if(MODE.equals("Verify")&&event.boundary.equals("AFTER_COMPLETE"))throw new IllegalStateException("completed event replayed");
            if((MODE.equals("A")&&event.boundary.equals("PREPARED"))||(MODE.equals("B")&&event.boundary.equals("FACT_COMMITTED"))
                ||(MODE.equals("D")&&event.boundary.equals("BEFORE_COMPLETE"))||(MODE.equals("E")&&event.boundary.equals("AFTER_COMPLETE"))) {
                facts(event.player);
                check(event.checkpoint==switch(MODE){case "A"->0;case "B"->1;case "D"->3;default->4;},"wrong fault boundary");
                if(KIND==4) {
                    var r=entry(event.player).investigation;
                    if(MODE.equals("A"))check(r.pendingEvidence()==4&&r.evidence()==0,"A intent not reserved");
                    else if(MODE.equals("B"))check(r.pendingEvidence()==0&&r.evidence()==4,"B intent lost or duplicated");
                    else check(r.evidence()==7&&entry(event.player).cityClues==7,"investigation lost evidence at completion boundary");
                }
                if(KIND<4) {
                    var r=entry(event.player).scenes.get(KIND);
                    check(r.id.toString().equals(Files.readString(E.resolve("boundary-event-"+KIND+".txt")).trim()),"scene instance changed");
                    check((r.progress.shown&87)==87,"already shown cue facts missing");
                    if(MODE.equals("E"))check(ClientScene.dispatchedCues()==0,"D resume replayed earlier sound cues");
                }
                note("PASS "+TASK+" event="+ID+" boundary="+event.boundary+" checkpoint="+event.checkpoint);
                note("FAULT Runtime.halt; no shutdown save after tested boundary");
                Runtime.getRuntime().halt(0);
            }
        }catch(Exception ex){failure(ex);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||phase<0)return;
        var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);
                check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"not muted before opening world");
                if(MODE.equals("A")||(MODE.equals("D")&&KIND<4)) {
                    String world="whileaway-boundary-"+KIND+"-"+System.currentTimeMillis();Files.writeString(E.resolve("boundary-world-"+KIND+".txt"),world);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("Boundary fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                } else {
                    var world=Files.readString(E.resolve("boundary-world-"+KIND+".txt")).trim();check(world.matches("whileaway-boundary-[124]-[0-9]+"),"unexpected fixture path");
                    mc.createWorldOpenFlows().openWorld(world,mc::stop);
                }
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())
                if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){button.onPress();break;}
            if(++ticks>9000)throw new IllegalStateException("timeout phase="+phase+" screen="+mc.screen);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(mc.screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen)mc.setScreen(null);
            if(!setup&&!queued) {
                setup=true;server(p->{
                    if(MODE.equals("A")||(MODE.equals("D")&&KIND<4)) {
                        var e=entry(p);for(var clue:Clue.values())NarrativeData.get(p.getServer()).discover(p.getUUID(),clue);
                        e.progress=e.progress.complete();e.station=new BlockPos(0,81,0);e.cityVisited=KIND!=1;
                        p.getInventory().add(new ItemStack(Items.DIAMOND,27));
                        if(KIND!=1)CityDistrict.get(p.getServer().getLevel(CityDistrict.KEY)).start();phase=1;
                    } else {
                        facts(p);
                        if(MODE.equals("Verify")) {
                            if(KIND<4) {
                                var r=entry(p).scenes.get(KIND);check(r.progress.state==EventCheckpoint.State.COMPLETED,"E completion not durable");
                                check(!SceneRecovery.begin(p,KIND,r.origin),"duplicate scene accepted after E reload");
                            } else {
                                check(entry(p).cityClues==7&&entry(p).investigation.evidence()==7,"final evidence lost");
                                check(entry(p).investigation.progress.checkpoint==4&&entry(p).investigation.noticeClaimed(),"E investigation not durable");
                                for(int bit:new int[]{4,1,2,2,4,1})CityInvestigation.read(p,bit);
                            }
                            check(SceneRecovery.integrity(entry(p)).isEmpty(),"integrity after E reload");phase=5;
                        } else if(KIND==4&&MODE.equals("D")) {
                            check(entry(p).investigation.evidence()==4&&entry(p).investigation.pendingEvidence()==0,"B recovery missing first clue");
                            phase=2;
                        } else if(KIND<4) {
                            var r=entry(p).scenes.get(KIND);check(r!=null&&r.progress.checkpoint==3&&!r.progress.terminal(),"D checkpoint not durable");
                            note("PASS D persisted identity/checkpoint; waiting for real client acknowledgement");phase=4;
                        }
                    }
                });
            }
            if(queued||ticks%10!=0)return;
            if(phase==1)server(p->{
                var level=KIND==1?p.getServer().overworld():p.getServer().getLevel(CityDistrict.KEY);
                if(KIND!=1&&!CityDistrict.get(level).ready())return;
                var origin=KIND==1?new BlockPos(0,81,0):CityDistrict.ARRIVAL;
                if(KIND==1)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {
                    level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
                    level.setBlock(origin.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);level.setBlock(origin.offset(x,1,z),Blocks.AIR.defaultBlockState(),2);
                }
                p.teleportTo(level,origin.getX()+.5,origin.getY(),origin.getZ()+.5,java.util.Set.of(),180,0);
                p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.fallDistance=0;
                p.getInventory().selected=8;
                p.getServer().saveEverything(true,true,true); // Fixture baseline only, before the tested operation.
                if(KIND<4) {
                    check(SceneRecovery.begin(p,KIND,origin),"first scene rejected");
                    try {Files.writeString(E.resolve("boundary-event-"+KIND+".txt"),entry(p).scenes.get(KIND).id.toString());}catch(Exception ex){throw new IllegalStateException(ex);}
                    phase=4;
                } else phase=2;
            });
            if(phase==2)server(p->{
                // Exercise the existing item-use adapter and deliberately use a non-narrative order.
                for(var item:java.util.List.of(WhileAway.CITY_SIREN_BOOK.get(),WhileAway.CITY_INTAKE_BOOK.get(),WhileAway.CITY_HOME_BOOK.get())) {
                    p.setItemInHand(InteractionHand.MAIN_HAND,item.getDefaultInstance());
                    item.use(p.level(),p,InteractionHand.MAIN_HAND);
                }
                phase=4;
            });
            if(phase==5&&mc.screen==null) {
                stable+=10;
                check(!ClientScene.active()&&ClientScene.apparitionCount()==0&&ClientScene.receivedCount()==0,"E reload replayed presentation or leaked actors");
                if(stable>=300) {
                    check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"mute changed");
                    note("PASS "+TASK+" completed state stable for 300 ticks; duplicate input suppressed; no scene replay");phase=-1;mc.stop();
                }
            }
        }catch(Exception ex){failure(ex);}
    }
}
