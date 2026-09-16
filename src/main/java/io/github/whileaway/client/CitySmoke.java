package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.core.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in isolated world test, including a second-process reload. All sound is muted by Gradle before launch. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class CitySmoke {
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/city"));
    private static final boolean RELOAD=Boolean.getBoolean("whileaway.cityReload");
    private static final boolean ART=Boolean.getBoolean("whileaway.artSmoke");
    private static final String TASK=RELOAD?"runCityReload":"runCitySmoke";
    private static boolean launched,setup;
    private static volatile boolean pending;
    private static volatile int phase;
    private static int ticks,frames;
    private static volatile BlockPos relay,returnPoint;
    private static final BlockPos MARKER=new BlockPos(42,64,68);
    private static void check(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    private static void note(String message) {
        try{Files.writeString(E.resolve(TASK+".txt"),message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception ex){throw new IllegalStateException(ex);}
        LogUtils.getLogger().info("WHILEAWAY_CITY {}",message);
    }
    private static void failure(Exception ex){LogUtils.getLogger().error("WHILEAWAY_CITY_FAILED",ex);Minecraft.getInstance().stop();}
    private static void onServer(java.util.function.Consumer<ServerPlayer> task) {
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();
        mc.getSingleplayerServer().execute(()->{
            try{task.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}
            catch(Exception ex){mc.execute(()->failure(ex));}
        });
    }
    private static void capture(String name) {
        var mc=Minecraft.getInstance();Screenshot.grab(E.toFile(),name,mc.getMainRenderTarget(),c->note("CAPTURE "+name));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("whileaway.citySmoke"))return;
        var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);
                mc.options.renderDistance().set(8);mc.options.simulationDistance().set(5);mc.options.pauseOnLostFocus=false;
                mc.options.hideGui=false;
                check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"prelaunch mute missing");
                if(RELOAD) {
                    var world=Files.readString(E.resolve("smoke-world.txt")).trim();
                    check(world.matches("whileaway-city-[0-9]+"),"unexpected reload world name");
                    mc.createWorldOpenFlows().openWorld(world,mc::stop);
                } else {
                    var world="whileaway-city-"+System.currentTimeMillis();Files.writeString(E.resolve("smoke-world.txt"),world);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("WhileAway city verification",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }
            }
            if(!launched)return;
            // Only the explicitly named isolated smoke save is opened here. Preserve a backup on its first experimental-dimension reload.
            if(RELOAD&&mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen screen) {
                for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button
                    &&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())) {
                    note("INFO isolated smoke world backup confirmed");button.onPress();break;
                }
            }
            if(++ticks>4000)throw new IllegalStateException("City smoke timed out: phase="+phase+" screen="+mc.screen);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(mc.screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen)mc.setScreen(null);
            if(!setup) {
                setup=true;
                onServer(p->{
                    var city=p.getServer().getLevel(CityDistrict.KEY);check(city!=null,"quiet_city dimension missing");
                    var ledger=NarrativeData.get(p.getServer());var entry=ledger.entry(p.getUUID());
                    if(RELOAD) {
                        check(p.level().dimension().equals(CityDistrict.KEY),"player dimension lost on reload");
                        check(entry.cityVisited&&entry.cityClues==7&&entry.returnPosition!=null,"story state lost on reload");
                        if(ART)check(entry.cityShockTriggered,"horror one-shot flag lost on reload");
                        var district=CityDistrict.get(city);check(district.ready(),"district completion lost on reload");
                        int cursor=district.cursor();district.start();check(district.advance(city,256)==0&&district.cursor()==cursor,"reload rebuilt city");
                        check(city.getBlockState(MARKER).is(Blocks.GOLD_BLOCK),"player edit lost on reload");
                        check(city.getBlockState(CityDistrict.RETURN_LIGHT).is(WhileAway.RETURN_LIGHT.get()),"return light lost");
                        returnPoint=entry.returnPosition;
                        note("PASS disk reload: player dimension, return point, 3 records, construction completion and player edit preserved");phase=5;
                    } else {
                        returnPoint=p.blockPosition();relay=returnPoint.offset(2,0,0);
                        p.serverLevel().setBlock(relay,WhileAway.RELAY.get().defaultBlockState().setValue(io.github.whileaway.content.RelayBlock.LIT,true),3);
                        entry.station=relay;
                        check(!CityTransit.enter(p,relay),"locked player entered city");
                        for(var clue:Clue.values())ledger.discover(p.getUUID(),clue);
                        entry.progress=entry.progress.complete();ledger.setDirty();
                        var district=CityDistrict.get(city);district.start();district.advance(city,10);
                        var loaded=CityDistrict.load(district.save(new CompoundTag(),city.registryAccess()),city.registryAccess());
                        check(loaded.cursor()==district.cursor(),"partial construction cursor did not round-trip");
                        note("PASS locked access and partial construction cursor round-trip");phase=1;
                    }
                });
            }
            if(phase==1&&!pending&&ticks%20==0) {
                pending=true;onServer(p->{
                    var city=p.getServer().getLevel(CityDistrict.KEY);var d=CityDistrict.get(city);
                    if(d.ready()) {
                        check(city.getBlockState(new BlockPos(18,65,47)).is(WhileAway.CITY_INTAKE.get()),"intake record missing");
                        check(CityTransit.enter(p,relay),"city entry failed");
                        check(p.level().dimension().equals(CityDistrict.KEY),"wrong arrival dimension");
                        note("PASS actual server transfer to quiet_city; cells="+d.cursor());phase=2;
                    }
                    pending=false;
                });
            }
            if(phase==2&&mc.level.dimension().equals(CityDistrict.KEY)&&mc.screen==null) {
                frames++;
                if(frames==70)mc.gui.getChat().clearMessages(false);
                if(frames==80){check(ClientScene.active(),"city arrival scene absent");mc.gui.getChat().clearMessages(false);capture("city-street.png");}
                if(frames==180)onServer(p->p.connection.teleport(94,110,104,136,35));
                if(frames==260) {
                    capture("city-overview.png");phase=3;frames=0;
                    onServer(p->{
                        var city=p.serverLevel();city.setBlock(MARKER,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                        var d=CityDistrict.get(city);check(d.advance(city,512)==0,"finished city changed on revisit request");
                        for(var item:java.util.List.of(WhileAway.CITY_INTAKE_BOOK.get(),WhileAway.CITY_HOME_BOOK.get(),WhileAway.CITY_SIREN_BOOK.get())) {
                            p.setItemInHand(InteractionHand.MAIN_HAND,item.getDefaultInstance());item.use(city,p,InteractionHand.MAIN_HAND);
                        }
                        check(NarrativeData.get(p.getServer()).entry(p.getUUID()).cityClues==7,"city books did not record readings");
                        check(CityTransit.leave(p),"return failed");check(p.blockPosition().equals(returnPoint),"return position changed");
                        note("PASS three book interactions and return to original overworld position");
                    });
                }
            }
            if(phase==3&&mc.level.dimension()==Level.OVERWORLD&&mc.screen==null) {
                if(++frames==80) {
                    phase=4;onServer(p->{
                        check(CityTransit.enter(p,relay),"reentry failed");
                        check(p.serverLevel().getBlockState(MARKER).is(Blocks.GOLD_BLOCK),"reentry overwrote player edit");
                        note("PASS reentry preserves player edits");
                    });
                }
            }
            if(phase==4&&mc.level.dimension().equals(CityDistrict.KEY)&&mc.screen==null) {
                if(!ART)finish();
                else {
                    phase=7;frames=0;
                    onServer(p->{
                        var e=NarrativeData.get(p.getServer()).entry(p.getUUID());e.readingUntil=Long.MAX_VALUE;
                        p.connection.teleport(40.5,65,25.5,180,0);p.setGameMode(GameType.SURVIVAL);
                        check(!CityHorror.tryStart(p),"scene interrupted reading grace");
                    });
                }
            }
            if(phase==7&&mc.level.dimension().equals(CityDistrict.KEY)&&mc.screen==null) {
                frames++;
                if(frames==60)onServer(p->{
                    var e=NarrativeData.get(p.getServer()).entry(p.getUUID());e.readingUntil=0;e.transitAfter=0;
                    check(CityHorror.tryStart(p),"eligible intake-ledger scene did not start");
                    check(!CityHorror.tryStart(p),"scene repeated");
                    note("PASS ledger-linked one-shot scene; reading grace protected");
                });
                if(frames==145)mc.gui.getChat().clearMessages(false);
                if(frames==160){check(ClientScene.apparitionCount()==7,"seven apparition actors not present");capture("seven-waiting.png");}
                if(frames==215){check(ClientScene.apparitionCount()==7,"apparitions ended before synchronized descent");capture("seven-descending.png");}
                if(frames==340) {
                    check(ClientScene.apparitionCount()==0&&!ClientScene.active(),"apparitions leaked beyond scene");
                    note("PASS seven apparition render, descent phase and cleanup");finish();
                }
            }
            if(phase==5&&mc.level.dimension().equals(CityDistrict.KEY)&&mc.screen==null) {
                if(++frames==100) {
                    capture("city-reloaded.png");phase=6;
                    onServer(p->{check(CityTransit.leave(p),"return after reload failed");check(p.blockPosition().equals(returnPoint),"saved return position changed");note("PASS return after process restart");});
                }
            }
            if(phase==6&&mc.level.dimension()==Level.OVERWORLD&&mc.screen==null)finish();
        }catch(Exception ex){failure(ex);}
    }
    private static void finish() {
        var mc=Minecraft.getInstance();check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"mute lost");
        phase=-1;note("PASS "+TASK);mc.stop();
    }
}
