package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in copied-world tests. No NPC teleport, story edits, forced chunk tickets or world rebuild. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcWorldSmoke {
    private static final String MODE=System.getProperty("whileaway.npcWorld","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-world/client"));
    private static boolean launched,setup,stopped;private static volatile boolean queued;
    private static int ticks,phase,wait,unloadedTicks,stable,checkpoint,generation;
    private static UUID uuid,instance;private static Set<String> facts;private static String work,dimension;
    private static BlockPos residence,lastLive,registered,snapshot;private static net.minecraft.world.phys.Vec3 movementStart;
    private static void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static StoryActorRecord record(MinecraftServer s){var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);check(r!=null,"NPC record missing");return r;}
    private static BlockPos snapshot(StoryActorRecord r){var p=r.snapshot.getList("Pos",Tag.TAG_DOUBLE);check(p.size()==3,"snapshot position missing");return BlockPos.containing(p.getDouble(0),p.getDouble(1),p.getDouble(2));}
    private static void remember(StoryActorRecord r){uuid=r.entityId;instance=r.instanceId;generation=r.generation;facts=Set.copyOf(r.facts);work=NpcState.work(r);residence=NpcState.home(r);dimension=r.dimension;checkpoint=r.checkpoint.checkpoint;registered=r.position;snapshot=snapshot(r);}
    private static void identity(StoryActorRecord r){check(r.storyId.equals(NpcEvents.STORY)&&r.entityId.equals(uuid)&&r.instanceId.equals(instance)&&r.generation==generation&&r.dimension.equals(dimension)&&NpcState.home(r).equals(residence),"NPC identity/binding/residence changed");}
    private static void unchanged(StoryActorRecord r){identity(r);check(r.checkpoint.checkpoint==checkpoint&&r.facts.equals(facts)&&NpcState.work(r).equals(work),"absence progressed checkpoint/facts/work");}
    private static void save(MinecraftServer s,String suffix)throws Exception {s.saveEverything(true,true,true);Files.copy(s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve(MODE+suffix+".dat"),StandardCopyOption.REPLACE_EXISTING);}
    private static void back(ServerPlayer p,BlockPos pos){p.setGameMode(GameType.CREATIVE);p.teleportTo(p.getServer().getLevel(CityDistrict.KEY),pos.getX()+2.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0);}
    private static String positions(StoryActorRecord r,ServerPlayer p){return "live="+lastLive+"/"+new ChunkPos(lastLive)+" registered="+r.position+"/"+new ChunkPos(r.position)+" snapshot="+snapshot(r)+"/"+new ChunkPos(snapshot(r))+" residence="+residence+"/"+new ChunkPos(residence)+" currentTarget="+residence+"/"+new ChunkPos(residence)+" returnLight="+residence.offset(2,0,0)+"/"+new ChunkPos(residence.offset(2,0,0))+" player="+p.blockPosition()+"/"+p.chunkPosition()+" playerDimension="+p.level().dimension().location();}
    private static boolean unloaded(ServerLevel l,BlockPos p){return !l.hasChunkAt(p)&&!l.areEntitiesLoaded(new ChunkPos(p).toLong());}
    private static int target(){return Character.digit(MODE.charAt(MODE.length()-1),10);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));mc.createWorldOpenFlows().openWorld("world-"+(MODE.startsWith("Resume")?"CutNether2":MODE),mc::stop);}
            if(mc.screen instanceof BackupConfirmScreen screen)for(var c:screen.children())if(c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>18000)throw new IllegalStateException("NPC_WORLD_TIMEOUT phase="+phase+" wait="+wait);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%5!=0)return;queued=true;var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(id);if(p==null)return;var city=s.getLevel(CityDistrict.KEY);
                if(!setup){setup=true;check(CityDistrict.get(city).ready(),"missing fixture city");
                    if(MODE.startsWith("Resume")){var r=record(s);remember(r);lastLive=r.position;check(p.level().dimension().equals(Level.NETHER)&&checkpoint==2,"fresh process did not resume absent in Nether");note("PASS fresh_start dimension=Nether checkpoint=2 uuid="+uuid);phase=1;}
                    else{check(!NarrativeData.get(s).actors.containsKey(NpcEvents.STORY),"not a pristine copy");back(p,CityDistrict.ARRIVAL);}
                }
                var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);if(r==null)return;
                var found=StoryActors.find(s,r.entityId);var npc=found instanceof StoryNpc n?n:null;
                if(phase==0){
                    if(npc==null)return;
                    if(target()>=2&&r.checkpoint.checkpoint==1){movementStart=npc.position();check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"actual interaction rejected");}
                    if(r.checkpoint.checkpoint!=target())return;
                    if(target()==2&&(movementStart==null||npc.position().distanceToSqr(movementStart)<=.25))return;
                    remember(r);lastLive=npc.blockPosition();save(s,"-before");note("BEFORE "+positions(r,p)+" checkpoint="+checkpoint+" generation="+generation+" actualMovement="+(target()==2));
                    var away=MODE.contains("Nether")?s.getLevel(Level.NETHER):MODE.startsWith("End")?s.getLevel(Level.END):city;
                    p.setGameMode(GameType.SPECTATOR);p.teleportTo(away,4096.5,160,4096.5,Set.of(),0,0);phase=1;wait=0;return;
                }
                if(phase==1){
                    unchanged(r);wait+=5;if(npc!=null)lastLive=npc.blockPosition();
                    if(wait<40)return;
                    check(r.lifecycle==(checkpoint==4?StoryActorRecord.Lifecycle.ACTIVE:StoryActorRecord.Lifecycle.SUSPENDED),"wrong absence lifecycle "+r.lifecycle);
                    if(MODE.startsWith("Chunk")){
                        boolean all=npc==null&&unloaded(city,lastLive)&&unloaded(city,r.position)&&unloaded(city,snapshot(r))&&unloaded(city,residence)&&unloaded(city,residence.offset(2,0,0));
                        if(!all){unloadedTicks=0;check(wait<3000,"actual NPC chunks did not unload "+positions(r,p));return;}
                        check(p.serverLevel().hasChunkAt(p.blockPosition()),"player chunk not loaded");
                        if((unloadedTicks+=5)<100)return;
                        note("PASS actual_unload heldTicks="+unloadedTicks+" entityAbsent=true hasChunk=false entitiesLoaded=false allTrackedPositions=true "+positions(r,p));
                    }else{
                        if(wait<120)return;
                        check(p.serverLevel()!=city&&p.serverLevel().getEntity(uuid)==null,"NPC crossed dimension");
                        long wrong=0;for(var en:p.serverLevel().getAllEntities())if(en instanceof StoryNpc)wrong++;
                        check(wrong==0,"duplicate NPC in other dimension");note("PASS dimension_absence="+p.level().dimension().location()+" checkpoint="+checkpoint+" unchanged=true wrongDimensionNPC=0 heldTicks="+wait);
                    }
                    save(s,"-away");
                    if(MODE.startsWith("Cut")){note("PASS "+MODE+" checkpoint="+checkpoint+" generation="+generation+" uuid="+uuid+" playerDimension=Nether durable=true");Runtime.getRuntime().halt(0);}
                    back(p,lastLive);phase=2;wait=0;return;
                }
                if(phase==2){
                    identity(r);if(npc==null){check((wait+=5)<800,"NPC failed to reconnect");return;}
                    check(ActorCandidates.inspect(s,r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical candidate not unique");
                    long count=0;for(var en:city.getAllEntities())if(en instanceof StoryNpc)count++;check(count==1,"duplicate StoryNpc in loaded city");
                    check(r.facts.containsAll(facts),"return lost facts before resuming");
                    note("PASS reconnected checkpoint="+r.checkpoint.checkpoint+" generation="+generation+" sameUUID="+uuid+" canonical=1 duplicates=0");save(s,"-return");phase=3;
                }
                if(phase==3){
                    if(npc==null)return;
                    if(r.checkpoint.checkpoint==1)check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"return interaction rejected");
                    if(r.checkpoint.checkpoint<4)return;
                    identity(r);check(facts.stream().filter(f->!f.startsWith("npc.work=")).allMatch(r.facts::contains),"permanent facts lost");
                    check(StoryActors.integrity(s).isEmpty()&&NpcState.work(r).equals("maintain_return_light")&&r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE,"completed resident/integrity");
                    var before=Set.copyOf(r.facts);check(!NpcEvents.interact(npc,p)&&before.equals(r.facts),"repeat relationship/completion");
                    check(npc.distanceToSqr(residence.getX()+.5,residence.getY(),residence.getZ()+.5)<=1.4,"NPC did not reach residence naturally");
                    if((stable+=5)<100)return;
                    save(s,"");note("PASS "+MODE+" canonical=1 generation="+generation+" uuid="+uuid+" instance="+instance+" checkpoint=4 work="+NpcState.work(r)+" dimension="+dimension+" stableTicks="+stable);
                    stopped=true;s.halt(false);mc.execute(mc::stop);
                }
            }catch(Exception x){fail(x);}finally{queued=false;}});
        }catch(Exception x){fail(x);}
    }
}
