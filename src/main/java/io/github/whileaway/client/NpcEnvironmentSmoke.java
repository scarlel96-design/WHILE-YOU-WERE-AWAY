package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicit isolated QA only. Mutates named fixture blocks; never changes NPC identity/facts/position. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcEnvironmentSmoke {
    private static final String MODE=System.getProperty("whileaway.npcEnvironment","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-path/client"));
    private static final Map<BlockPos,BlockState> originals=new LinkedHashMap<>(),damaged=new LinkedHashMap<>();
    private static boolean launched,setup,stopped;private static volatile boolean queued;
    private static int ticks,phase,held,stable,checkpoint,generation;
    private static UUID uuid,instance;private static Set<String> facts;private static BlockPos home,start;
    private static void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static String source(){return MODE.startsWith("Resume")?"Cut"+MODE.substring(6):MODE.startsWith("Reload")?"Cut"+MODE.substring(6):MODE;}
    private static void remember(StoryActorRecord r){uuid=r.entityId;instance=r.instanceId;generation=r.generation;facts=Set.copyOf(r.facts);checkpoint=r.checkpoint.checkpoint;home=NpcState.home(r);start=r.position;}
    private static void identity(StoryActorRecord r){check(r.entityId.equals(uuid)&&r.instanceId.equals(instance)&&r.generation==generation&&NpcState.home(r).equals(home)&&r.dimension.equals("whileaway:quiet_city"),"identity/generation/residence changed");}
    private static void save(MinecraftServer s,String suffix)throws Exception {s.saveEverything(true,true,true);Files.copy(s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve(MODE+suffix+".dat"));}
    private static void set(ServerLevel level,BlockPos p,BlockState state){originals.putIfAbsent(p.immutable(),level.getBlockState(p));damaged.put(p.immutable(),state);level.setBlockAndUpdate(p,state);}
    private static void damage(ServerLevel level,StoryNpc npc)throws Exception {
        if(MODE.equals("MissingHome"))set(level,home.below(),Blocks.AIR.defaultBlockState());
        else if(MODE.equals("BlockedHome")){set(level,home,Blocks.STONE.defaultBlockState());set(level,home.above(),Blocks.STONE.defaultBlockState());}
        else if(MODE.equals("ConflictHome"))set(level,home.below(),Blocks.OAK_FENCE.defaultBlockState());
        else if(MODE.equals("CutPath")){
            var p=npc.blockPosition();
            for(int y=0;y<=3;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0||y==3)set(level,p.offset(x,y,z),Blocks.STONE.defaultBlockState());
        }else set(level,home.offset(2,0,0),MODE.equals("WrongLight")?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        var root=new CompoundTag();var list=new ListTag();
        for(var entry:originals.entrySet()) {var t=new CompoundTag();t.putLong("pos",entry.getKey().asLong());t.put("before",NbtUtils.writeBlockState(entry.getValue()));t.put("after",NbtUtils.writeBlockState(damaged.get(entry.getKey())));list.add(t);}
        root.put("blocks",list);NbtIo.writeCompressed(root,E.resolve(MODE+"-blocks.nbt"));
    }
    private static void loadBlocks(ServerLevel level)throws Exception {
        var root=NbtIo.readCompressed(E.resolve(source()+"-blocks.nbt"),NbtAccounter.unlimitedHeap());
        for(var raw:root.getList("blocks",10)){var t=(CompoundTag)raw;var p=BlockPos.of(t.getLong("pos"));var lookup=level.registryAccess().lookupOrThrow(Registries.BLOCK);originals.put(p,NbtUtils.readBlockState(lookup,t.getCompound("before")));damaged.put(p,NbtUtils.readBlockState(lookup,t.getCompound("after")));}
    }
    private static void blocksMatch(ServerLevel level,Map<BlockPos,BlockState> expected){for(var e:expected.entrySet())check(level.getBlockState(e.getKey()).equals(e.getValue()),"fixture block changed without repair "+e.getKey());}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));mc.createWorldOpenFlows().openWorld("env-"+source(),mc::stop);}
            if(mc.screen instanceof BackupConfirmScreen screen)for(var c:screen.children())if(c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>15000)throw new IllegalStateException("ENV_TIMEOUT phase="+phase);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%5!=0)return;queued=true;var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(id);if(p==null)return;var city=s.getLevel(CityDistrict.KEY);
                if(!setup){setup=true;check(city!=null&&CityDistrict.get(city).ready(),"fixture city unavailable");
                    if(MODE.startsWith("Resume")||MODE.startsWith("Reload")){
                        var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);check(r!=null,"existing NPC data lost");remember(r);loadBlocks(city);
                        check(checkpoint==(MODE.startsWith("Reload")?4:2),"persisted checkpoint drift");phase=MODE.startsWith("Reload")?2:1;save(s,"-input");note("PASS fresh_input checkpoint="+checkpoint+" uuid="+uuid+" generation="+generation);
                    }else {check(!NarrativeData.get(s).actors.containsKey(NpcEvents.STORY),"not a pristine world copy");p.setGameMode(GameType.CREATIVE);p.teleportTo(city,CityDistrict.ARRIVAL.getX()+.5,CityDistrict.ARRIVAL.getY(),CityDistrict.ARRIVAL.getZ()+.5,Set.of(),0,0);}
                }
                var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);if(r==null)return;var found=StoryActors.find(s,r.entityId);if(!(found instanceof StoryNpc npc))return;
                if(phase==0){
                    if(MODE.equals("CompletedLight")){if(r.checkpoint.checkpoint==1)check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"interaction");if(r.checkpoint.checkpoint!=4)return;}
                    else if(r.checkpoint.checkpoint!=1)return;
                    remember(r);save(s,"-original");damage(city,npc);
                    if(checkpoint==1)check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real interaction rejected");
                    remember(r);save(s,"-before");phase=1;return;
                }
                identity(r);
                check(ActorCandidates.inspect(s,r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical NPC not unique");
                long count=0;for(var en:city.getAllEntities())if(en instanceof StoryNpc)count++;check(count==1,"duplicate StoryNpc");
                if(phase==1){
                    check(r.checkpoint.checkpoint==checkpoint&&r.facts.equals(facts),"damage advanced or erased facts/checkpoint");blocksMatch(city,damaged);
                    var env=NpcEnvironment.inspect(city,r.dimension,npc.blockPosition(),home);var nav=npc.storyNavigation();boolean ready;
                    if(source().equals("CutPath")){ready=nav.state()==NpcEnvironment.Path.UNREACHABLE;check(npc.blockPosition().distSqr(start)<=2,"NPC escaped sealed obstacle without a route");}
                    else if(MODE.equals("MissingHome"))ready=env.residence()==NpcEnvironment.Residence.MISSING;
                    else if(MODE.equals("BlockedHome"))ready=env.residence()==NpcEnvironment.Residence.TEMPORARILY_BLOCKED;
                    else if(MODE.equals("ConflictHome"))ready=env.residence()==NpcEnvironment.Residence.CONFLICT;
                    else ready=env.light()==(MODE.equals("WrongLight")?NpcEnvironment.Light.WRONG_BLOCK:NpcEnvironment.Light.MISSING);
                    ready&=r.lifecycle==(checkpoint==4?StoryActorRecord.Lifecycle.ACTIVE:StoryActorRecord.Lifecycle.SUSPENDED);
                    if(!ready){held=0;return;}if((held+=5)<100)return;
                    note("PASS blocked "+env.withPath(nav.state()).diagnostic()+" checkpoint="+checkpoint+" canonical=1 generation="+generation+" factsUnchanged=true blocksPreserved="+damaged.size()+" heldTicks="+held+" attempts="+nav.attempts());save(s,"-blocked");
                    if(MODE.startsWith("Cut")){note("PASS "+MODE+" durable=true checkpoint="+checkpoint+" uuid="+uuid+" generation="+generation);Runtime.getRuntime().halt(0);}
                    for(var entry:originals.entrySet())city.setBlockAndUpdate(entry.getKey(),entry.getValue());blocksMatch(city,originals);note("RESTORED original fixture blocks only; npcTeleport=false");phase=2;return;
                }
                blocksMatch(city,originals);
                if(r.checkpoint.checkpoint!=4)return;
                check(facts.stream().filter(f->!f.startsWith("npc.work=")).allMatch(r.facts::contains),"permanent facts lost");
                check(r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE&&NpcState.work(r).equals("maintain_return_light")&&StoryActors.integrity(s).isEmpty(),"resident/integrity");
                check(npc.distanceToSqr(home.getX()+.5,home.getY(),home.getZ()+.5)<=1.4,"did not walk home");var f=Set.copyOf(r.facts);check(!NpcEvents.interact(npc,p)&&f.equals(r.facts),"duplicate relationship receipt");
                if((stable+=5)<100)return;save(s,"");note("PASS "+MODE+" canonical=1 checkpoint=4 generation="+generation+" uuid="+uuid+" work="+NpcState.work(r)+" facts="+r.facts+" actualWalk=true blocksRestored=true stableTicks="+stable);
                stopped=true;s.halt(false);mc.execute(mc::stop);
            }catch(Exception e){fail(e);}finally{queued=false;}});
        }catch(Exception e){fail(e);}
    }
}
