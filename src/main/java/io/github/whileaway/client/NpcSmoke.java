package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Scripted integrated-client test. No production decisions depend on mode/world/fixture coordinates. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcSmoke {
    private static final String MODE=System.getProperty("whileaway.npcSmoke","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-lifecycle/client"));
    private static boolean launched,setup,stopped;private static volatile boolean queued;
    private static int ticks,stable;private static UUID previousUUID,previousInstance;private static int previousGeneration;
    private static Set<String> previousFacts;
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean v,String s){if(!v)throw new IllegalStateException(s);}
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static void finish(MinecraftServer s){stopped=true;s.saveEverything(true,true,true);s.halt(false);Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());}
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent event) {
        if(!MODE.startsWith("Cut")||!event.eventId.equals(NpcEvents.ID)||!event.boundary.startsWith("NPC_"+MODE.substring(3)+"_"))return;
        try {
            var s=event.player.getServer();var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);
            check(NpcState.integrity(r).isEmpty(),"cut NPC integrity");s.saveEverything(true,true,true);
            Files.copy(file(s),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
            note("PASS "+MODE+" boundary="+event.boundary+" checkpoint="+r.checkpoint.checkpoint+" generation="+r.generation+" uuid="+r.entityId+" facts="+r.facts);
            Runtime.getRuntime().halt(0);
        }catch(Exception e){fail(e);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                if(MODE.equals("Seed")) {
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel("npc-Seed",new LevelSettings("NPC lifecycle fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }else mc.createWorldOpenFlows().openWorld("npc-Cut"+MODE.substring(MODE.length()-1),mc::stop);
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>12000)throw new IllegalStateException("NPC timeout "+MODE);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%10!=0)return;queued=true;
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(mc.player.getUUID());if(p==null)return;
                var city=s.getLevel(CityDistrict.KEY);check(city!=null,"missing city dimension");
                if(!setup) {
                    setup=true;
                    if(MODE.equals("Seed")){CityDistrict.get(city).start();p.setGameMode(GameType.SPECTATOR);}
                    else {
                        check(CityDistrict.get(city).ready(),"seed city incomplete");
                        if(MODE.startsWith("Resume")) {
                            var before=NarrativeData.get(s).actors.get(NpcEvents.STORY);check(before!=null,"NPC record lost");
                            previousUUID=before.entityId;previousInstance=before.instanceId;previousGeneration=before.generation;previousFacts=Set.copyOf(before.facts);
                            note("INPUT checkpoint="+before.checkpoint.checkpoint+" generation="+previousGeneration+" uuid="+previousUUID);
                        }
                        p.setGameMode(GameType.CREATIVE);
                        p.teleportTo(city,CityDistrict.ARRIVAL.getX()+.5,CityDistrict.ARRIVAL.getY(),CityDistrict.ARRIVAL.getZ()+.5,Set.of(),0,0);
                    }
                }
                if(MODE.equals("Seed")) {
                    if(!CityDistrict.get(city).ready())return;
                    check(!NarrativeData.get(s).actors.containsKey(NpcEvents.STORY),"spectator must not start NPC event");
                    p.teleportTo(city,CityDistrict.ARRIVAL.getX()+.5,CityDistrict.ARRIVAL.getY(),CityDistrict.ARRIVAL.getZ()+.5,Set.of(),0,0);
                    note("PASS Seed real city ready; no NPC; artRevision="+CityDistrict.get(city).layoutVersion());finish(s);return;
                }
                var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);if(r==null)return;
                var entity=StoryActors.find(s,r.entityId);if(!(entity instanceof StoryNpc npc))return;
                if(r.checkpoint.checkpoint==1)check(npc.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real NPC interaction rejected");
                if(r.checkpoint.checkpoint<4)return;
                check(MODE.startsWith("Resume"),"cut marker was not reached");
                check(r.entityId.equals(previousUUID)&&r.instanceId.equals(previousInstance)&&r.generation==previousGeneration,"NPC identity changed on restart");
                check(previousFacts.stream().filter(f->!f.startsWith("npc.work=")).allMatch(r.facts::contains),"permanent facts lost");
                check(r.facts.stream().filter(f->f.startsWith("npc.work=")).count()==1&&NpcState.work(r).equals("maintain_return_light"),"incorrect new work state");
                check(NpcState.integrity(r).isEmpty(),"NPC semantic integrity");
                var candidates=ActorCandidates.inspect(s,r);check(candidates.outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"NPC uniqueness");
                check(r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE&&!npc.isRemoved(),"completed resident retired");
                check(npc.distanceToSqr(NpcState.home(r).getX()+.5,NpcState.home(r).getY(),NpcState.home(r).getZ()+.5)<=1.4,"NPC did not walk to residence");
                var facts=Set.copyOf(r.facts);check(!NpcEvents.interact(npc,p)&&r.facts.equals(facts),"completed dialogue repeated");
                stable+=10;if(stable<120)return;
                s.saveEverything(true,true,true);Files.copy(file(s),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
                note("PASS "+MODE+" canonical=1 generation="+r.generation+" uuid="+r.entityId+" checkpoint=4 life="+NpcState.work(r)+" facts="+r.facts+" stableTicks="+stable);finish(s);
            }catch(Exception e){fail(e);}finally{queued=false;}});
        }catch(Exception e){fail(e);}
    }
}
