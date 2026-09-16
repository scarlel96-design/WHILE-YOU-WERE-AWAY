package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.Wayfarer;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
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
import org.lwjgl.glfw.GLFW;

/** Opt-in real-client lifecycle exceptions; coordinates/movement belong only to this fixture. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ActorExceptionsSmoke {
    private static final String MODE=System.getProperty("whileaway.actorExceptions","");
    private static final int REPEAT=Integer.getInteger("whileaway.actorRepeat",1);
    private static final String TASK="runActorExceptions"+MODE+REPEAT;
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/wayfarer-stability/exceptions"));
    private static final BlockPos HOME=new BlockPos(1024,130,1024),RESPAWN=HOME.offset(128,0,0);
    private static boolean launched,setup,monitorChecked;
    private static volatile boolean queued;
    private static volatile int phase;
    private static int ticks,wait,scenario,cp,generation,pausedAge,pausedHunt,inventoryBefore;
    private static UUID actorId,instance;
    private static BlockPos lastLivePosition;
    private static int unloadedTicks;
    private static void check(boolean ok,String text){if(!ok)throw new IllegalStateException(text);}
    private static StoryActorRecord record(ServerPlayer p){return NarrativeData.get(p.getServer()).actors.get(StoryActors.chaseId(p.getUUID()));}
    private static void note(String text){try{Files.writeString(E.resolve(TASK+".txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new IllegalStateException(ex);}com.mojang.logging.LogUtils.getLogger().info("ACTOR_EXCEPTIONS {}",text);}
    private static void fail(Exception ex){note("FAIL "+ex);com.mojang.logging.LogUtils.getLogger().error("ACTOR_EXCEPTION_FAILURE",ex);phase=-1;Minecraft.getInstance().stop();}
    private static void server(java.util.function.Consumer<ServerPlayer> action){
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();queued=true;
        mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);if(p!=null)action.accept(p);}catch(Exception ex){mc.execute(()->fail(ex));}finally{queued=false;}});
    }
    private static void pad(ServerLevel level,BlockPos pos,int radius){for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=-1;y<=4;y++)level.setBlock(pos.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
    private static void home(ServerPlayer p){p.teleportTo(p.getServer().overworld(),HOME.getX()+.5,HOME.getY(),HOME.getZ()+.5,Set.of(),0,0);p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.fallDistance=0;p.getAbilities().invulnerable=true;}
    private static void away(ServerPlayer p,ServerLevel level){p.setGameMode(GameType.SPECTATOR);p.teleportTo(level,HOME.getX()+4096.5,160,HOME.getZ()+4096.5,Set.of(),0,0);}
    private static void remember(ServerPlayer p){var r=record(p);actorId=r.entityId;instance=r.instanceId;generation=r.generation;cp=r.checkpoint.checkpoint;}
    private static void identity(ServerPlayer p){var r=record(p);check(r.instanceId.equals(instance)&&r.entityId.equals(actorId)&&r.generation==generation,"canonical identity changed during absence");check(r.checkpoint.checkpoint==cp&&!NarrativeData.get(p.getServer()).entry(p.getUUID()).progress.encounterComplete(),"absence completed/advanced encounter");}
    private static Wayfarer actor(ServerPlayer p){return (Wayfarer)StoryActors.find(p.getServer(),record(p).entityId);}
    private static void done(ServerPlayer p){
        StoryActors.persist(p.getServer());p.getServer().saveEverything(true,true,true);
        try{Files.copy(p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve(TASK+".dat"),StandardCopyOption.REPLACE_EXISTING);}catch(Exception ex){throw new IllegalStateException(ex);}
        note("PASS "+TASK+" checkpoint="+record(p).checkpoint.checkpoint+" generation="+record(p).generation+" instance="+record(p).instanceId+" actor="+record(p).entityId);
        phase=-1;Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(MODE.isEmpty()||phase<0)return;var mc=Minecraft.getInstance();
        try{
            if(!launched&&mc.screen instanceof TitleScreen){
                launched=true;Files.createDirectories(E);
                check(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"client not muted");
                note(ClientTestEnvironment.verify(E,TASK));monitorChecked=true;
                if(MODE.equals("Seed")){
                    String world="whileaway-exceptions-"+REPEAT+"-"+System.currentTimeMillis();Files.writeString(E.resolve("exceptions-world-"+REPEAT+".txt"),world);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("Actor exceptions fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(74192361L+REPEAT,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }else{
                    String world=Files.readString(E.resolve("exceptions-world-"+REPEAT+".txt")).trim();check(world.matches("whileaway-exceptions-[12]-[0-9]+"),"unexpected fixture path");
                    if(MODE.equals("Copy")||MODE.equals("CopyReload"))world+="-copy";mc.createWorldOpenFlows().openWorld(world,mc::stop);
                }
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){button.onPress();break;}
            if(++ticks>18000)throw new IllegalStateException("timeout phase="+phase+" scenario="+scenario);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued)return;
            if(mc.screen instanceof DeathScreen){mc.player.respawn();return;}
            if(ticks%10!=0)return;
            if(!setup){setup=true;server(p->{
                if(MODE.equals("Seed")){
                    var l=p.getServer().overworld();pad(l,HOME,31);pad(l,RESPAWN,3);home(p);
                    p.setRespawnPosition(Level.OVERWORLD,RESPAWN,0,true,false);
                    NarrativeData.get(p.getServer()).entry(p.getUUID()).station=HOME;
                    var a=WhileAway.WAYFARER.get().create(l);a.bind(p.getUUID(),HOME);a.moveTo(HOME.getX()+8.5,HOME.getY(),HOME.getZ()+.5,0,0);
                    check(StoryActors.beginChase(p,a),"chase start rejected");phase=1;wait=0;
                }else{
                    var r=record(p);check(r!=null,"copied/reloaded actor ledger missing");remember(p);
                    if(MODE.equals("Reload")){check(r.checkpoint.checkpoint==4&&r.lifecycle==StoryActorRecord.Lifecycle.RETIRED,"original completion changed by copied world");phase=9;wait=0;}
                    else{check(r.checkpoint.checkpoint==2&&r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED,"seed/copied branch not independently suspended");home(p);phase=8;wait=0;}
                }
            });return;}
            if(phase==1)server(p->{
                var a=actor(p);if(a==null)return;
                if(scenario==1&&record(p).checkpoint.checkpoint<2){p.teleportTo(p.serverLevel(),a.getX()+3,a.getY(),a.getZ(),Set.of(),0,0);return;}
                remember(p);p.getInventory().clearContent();p.getInventory().add(new ItemStack(Items.DIAMOND,27));inventoryBefore=p.getInventory().countItem(Items.DIAMOND);
                p.getServer().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(scenario==1,p.getServer());
                p.kill();phase=2;wait=0;
            });
            if(phase==2)server(p->{
                if(!p.isAlive())return;wait+=10;if(wait<80)return;
                identity(p);check(record(p).lifecycle==StoryActorRecord.Lifecycle.SUSPENDED,"death/remote respawn not suspended");
                check(p.getInventory().countItem(Items.DIAMOND)==(scenario==1?inventoryBefore:0),"keepInventory fixture mismatch");
                note("PASS death_respawn keepInventory="+(scenario==1)+" checkpoint="+cp+" generation="+generation);home(p);phase=3;wait=0;
            });
            if(phase==3)server(p->{
                identity(p);var a=actor(p);if(a==null){check((wait+=10)<400,"canonical actor failed to reload");return;}
                check(a.getUUID().equals(actorId),"different actor after respawn");
                if(scenario==0){scenario=1;phase=1;return;}
                if(scenario<4){
                    if(scenario==1){
                        // Deliberately separate HOME from the actor chunk; never infer one from the other.
                        a.teleportTo(HOME.getX()+24.5,HOME.getY(),HOME.getZ()+.5);
                        StoryActors.checkpoint(a,record(p),record(p).checkpoint.checkpoint,null);
                    }
                    lastLivePosition=a.blockPosition();unloadedTicks=0;
                    note("CHUNKS before HOME="+new ChunkPos(HOME)+" snapshot="+new ChunkPos(record(p).position)+" live="+new ChunkPos(lastLivePosition)+" player="+p.chunkPosition());
                    remember(p);away(p,scenario==1?p.serverLevel():p.getServer().getLevel(scenario==2?Level.NETHER:Level.END));phase=4;wait=0;pausedAge=-1;
                }
                else{away(p,p.getServer().overworld());phase=6;wait=0;}
            });
            if(phase==4)server(p->{
                wait+=10;identity(p);var r=record(p);
                if(wait<40)return;
                check(r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED,"absence lifecycle not suspended");
                if(pausedAge<0){pausedAge=r.snapshot.getInt("StoryAge");pausedHunt=r.snapshot.getInt("HuntTicks");}
                check(r.snapshot.getInt("StoryAge")==pausedAge&&r.snapshot.getInt("HuntTicks")==pausedHunt,"AI budget aged while absent");
                if(scenario==1){
                    var l=p.getServer().overworld();var live=actor(p);
                    if(live!=null)lastLivePosition=live.blockPosition();
                    boolean homeLoaded=l.hasChunkAt(HOME)||l.areEntitiesLoaded(new ChunkPos(HOME).toLong());
                    boolean snapshotLoaded=l.hasChunkAt(r.position)||l.areEntitiesLoaded(new ChunkPos(r.position).toLong());
                    boolean liveLoaded=l.hasChunkAt(lastLivePosition)||l.areEntitiesLoaded(new ChunkPos(lastLivePosition).toLong());
                    var pos=r.snapshot.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);
                    check(pos.size()==3,"actor snapshot Pos missing");
                    var snapshotPos=BlockPos.containing(pos.getDouble(0),pos.getDouble(1),pos.getDouble(2));
                    check(snapshotPos.equals(r.position),"snapshot Pos and registered position disagree");
                    check(!new ChunkPos(HOME).equals(new ChunkPos(r.position)),"fixture failed to separate HOME and snapshot chunks");
                    if(homeLoaded||snapshotLoaded||liveLoaded||live!=null){unloadedTicks=0;check(wait<2400,"actual actor chunk did not unload");return;}
                    check(p.serverLevel().hasChunkAt(p.blockPosition()),"player chunk not loaded");
                    if((unloadedTicks+=10)<100)return;
                    note("PASS actual_chunk_unloaded HOME="+new ChunkPos(HOME)+" snapshot="+new ChunkPos(snapshotPos)+" last_live="+new ChunkPos(lastLivePosition)+" all hasChunk=false entitiesLoaded=false; canonical_absent=true; player="+p.chunkPosition()+" loaded=true; held_ticks="+unloadedTicks+" generation="+generation);
                }else{
                    if(wait<120)return;check(p.serverLevel().getEntity(actorId)==null,"actor spawned in wrong dimension");
                    check(r.dimension.equals("minecraft:overworld"),"dimension binding changed");note("PASS dimension_absence="+(scenario==2?"Nether":"End")+" generation="+generation);
                }
                home(p);phase=5;wait=0;
            });
            if(phase==5)server(p->{
                identity(p);if(actor(p)==null){check((wait+=10)<600,"return actor not reconnected");return;}
                var count=p.serverLevel().getEntitiesOfClass(Wayfarer.class,new net.minecraft.world.phys.AABB(HOME).inflate(96),a->p.getUUID().equals(a.witness())).size();check(count==1,"duplicate actor after return");
                note("PASS return scenario="+scenario+" canonical_count="+count+" same UUID/instance/generation/checkpoint");scenario++;phase=3;
            });
            if(phase==6)server(p->{if((wait+=10)<60)return;check(record(p).lifecycle==StoryActorRecord.Lifecycle.SUSPENDED,"seed not paused");done(p);});
            if(phase==8)server(p->{
                identity(p);var a=actor(p);if(a==null){check((wait+=10)<600,"branch actor did not load");return;}
                check(StoryActors.canonical(a,record(p)),"restarted entity not canonical");
                if(record(p).lifecycle!=StoryActorRecord.Lifecycle.ACTIVE||record(p).checkpoint.state==io.github.whileaway.core.EventCheckpoint.State.SUSPENDED){check((wait+=10)<600,"restarted runtime did not resume");return;}
                long count=p.serverLevel().getEntitiesOfClass(Wayfarer.class,new net.minecraft.world.phys.AABB(HOME).inflate(96),w->p.getUUID().equals(w.witness())).size();
                check(count==1,"duplicate actor in restarted branch");
                note("PASS fresh_process_reconnected same UUID/instance/generation/checkpoint; ACTIVE; canonical_count=1");
                if(MODE.equals("Original")){a.hurt(p.damageSources().playerAttack(p),1000);check(record(p).checkpoint.checkpoint==4,"original combat completion failed");done(p);}
                else if(MODE.equals("Copy")||MODE.equals("CopyReload")){away(p,p.serverLevel());phase=6;wait=0;}
                else{
                    var r=record(p);var oldSnapshot=a.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                    a.discard();p.getServer().saveEverything(true,true,true);
                    try{Files.writeString(E.resolve("stale-"+REPEAT+".snbt"),oldSnapshot.toString());}catch(Exception ex){throw new IllegalStateException(ex);}
                    phase=10;wait=0;
                }
            });
            if(phase==10)server(p->{
                var r=record(p);if(r.generation==generation){check((wait+=10)<600,"confirmed missing actor did not recover");return;}
                check(r.generation==generation+1&&!r.entityId.equals(actorId)&&r.instanceId.equals(instance)&&r.checkpoint.checkpoint==cp,"copy recovery changed wrong facts");
                var a=actor(p);if(a==null)return;
                try{var stale=WhileAway.WAYFARER.get().create(p.serverLevel());stale.load(net.minecraft.nbt.TagParser.parseTag(Files.readString(E.resolve("stale-"+REPEAT+".snbt"))));check(!p.serverLevel().addFreshEntity(stale),"old generation joined after recreation");}catch(java.io.IOException|com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalStateException(ex);}
                note("PASS copy independent branch: generation+1; old generation rejected; event still checkpoint2");away(p,p.serverLevel());phase=6;wait=0;
            });
            if(phase==9)server(p->{check(record(p).checkpoint.checkpoint==4&&record(p).generation==generation&&StoryActors.find(p.getServer(),actorId)==null,"copy leaked into original");if((wait+=10)>=200)done(p);});
        }catch(Exception ex){fail(ex);}
    }
}
