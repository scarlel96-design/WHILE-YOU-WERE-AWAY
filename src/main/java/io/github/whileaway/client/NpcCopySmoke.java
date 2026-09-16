package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in divergent copies, with real interaction/navigation and no actor/fact edits. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcCopySmoke {
    private static final String MODE=System.getProperty("whileaway.npcCopy","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-world/copy-client"));
    private static boolean launched,setup,stopped;private static volatile boolean queued;
    private static int ticks,stable;private static UUID uuid,instance;private static int generation,initial;
    private static Set<String> facts;private static net.minecraft.core.BlockPos home;
    private static void check(boolean v,String s){if(!v)throw new IllegalStateException(s);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static void snapshot(MinecraftServer s,String suffix)throws Exception {
        s.saveEverything(true,true,true);
        Files.copy(s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve(MODE+suffix+".dat"));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                mc.createWorldOpenFlows().openWorld("copy-"+(MODE.equals("Seed")?"Seed":MODE.substring(MODE.length()-1)),mc::stop);}
            if(mc.screen instanceof BackupConfirmScreen screen)for(var c:screen.children())if(c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>12000)throw new IllegalStateException("NPC_COPY_TIMEOUT "+MODE);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%5!=0)return;queued=true;var playerId=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(playerId);if(p==null)return;
                var city=s.getLevel(CityDistrict.KEY);check(city!=null&&CityDistrict.get(city).ready(),"fixture city missing");
                if(!setup){setup=true;var before=NarrativeData.get(s).actors.get(NpcEvents.STORY);
                    if(MODE.equals("Seed"))check(before==null,"not a pristine seed");
                    else {
                        check(before!=null,"existing copied NPC lost");uuid=before.entityId;instance=before.instanceId;generation=before.generation;facts=Set.copyOf(before.facts);home=NpcState.home(before);initial=before.checkpoint.checkpoint;
                        check(initial==(MODE.startsWith("Advance")?1:MODE.endsWith("A")?4:2),"unexpected persisted checkpoint");
                        note("PASS input checkpoint="+initial+" uuid="+uuid+" instance="+instance+" generation="+generation+" work="+NpcState.work(before));snapshot(s,"-input");
                    }
                    if(!MODE.startsWith("Reload")){
                        p.setGameMode(GameType.CREATIVE);p.teleportTo(city,CityDistrict.ARRIVAL.getX()+2.5,CityDistrict.ARRIVAL.getY(),CityDistrict.ARRIVAL.getZ()+.5,Set.of(),0,0);
                    }
                }
                var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);if(r==null)return;
                if(!MODE.equals("Seed"))check(r.entityId.equals(uuid)&&r.instanceId.equals(instance)&&r.generation==generation&&NpcState.home(r).equals(home)&&r.dimension.equals("whileaway:quiet_city"),"copied identity/residence drift");
                var entity=StoryActors.find(s,r.entityId);if(!(entity instanceof StoryNpc npc))return;
                check(ActorCandidates.inspect(s,r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical not unique");
                long count=0;for(var en:city.getAllEntities())if(en instanceof StoryNpc)count++;check(count==1,"duplicate NPC");
                if(MODE.startsWith("Advance")&&r.checkpoint.checkpoint==1){
                    check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real interaction rejected");
                    if(MODE.endsWith("B"))p.setGameMode(GameType.SPECTATOR); // no living witness; retain unfinished story naturally
                }
                int wanted=MODE.equals("Seed")?1:MODE.endsWith("A")?4:2;
                if(r.checkpoint.checkpoint<wanted)return;
                check(r.checkpoint.checkpoint==wanted,"unwanted progress");
                if(MODE.equals("Seed"))p.setGameMode(GameType.SPECTATOR);
                check(NpcState.integrity(r).isEmpty()&&StoryActors.integrity(s).isEmpty(),"integrity");
                if(MODE.startsWith("Reload"))check(r.facts.equals(facts),"reload changed stored facts/work");
                if(wanted==4){
                    check(NpcState.work(r).equals("maintain_return_light")&&r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE,"resident state");
                    var f=Set.copyOf(r.facts);check(!NpcEvents.interact(npc,p)&&r.facts.equals(f),"duplicate dialogue/relation");
                }else if(wanted==2)check(NpcState.work(r).equals("check_return_light")&&!r.facts.contains(NpcState.LINK),"B inherited A completion");
                if((stable+=5)<120)return;
                if(wanted<4)check(r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED,"unfinished absent witness not suspended");
                snapshot(s,"");note("PASS "+MODE+" checkpoint="+r.checkpoint.checkpoint+" uuid="+r.entityId+" instance="+r.instanceId+" generation="+r.generation+" canonical=1 work="+NpcState.work(r)+" facts="+r.facts+" stableTicks="+stable);
                stopped=true;s.halt(false);mc.execute(mc::stop);
            }catch(Exception e){fail(e);}finally{queued=false;}});
        }catch(Exception e){fail(e);}
    }
}
