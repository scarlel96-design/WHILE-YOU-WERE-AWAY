package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Real connected-client save dispatch after explicit corrupt fixture load. No fail-open path. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcCorruptionSmoke {
    private static final String MODE=System.getProperty("whileaway.npcCorruption","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-path/corrupt-client"));
    private static boolean launched,stopped;private static volatile boolean queued,ready;
    private static int ticks,held,generation;private static UUID uuid,instance;private static byte[] original;
    private static CompoundTag inputActor;private static Set<String> facts;
    private static boolean blockedMode(){return Set.of("MissingHome","MissingExperience","WrongWork").contains(MODE);}
    private static void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");}
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(MODE.isEmpty())return;
        try {
            var s=event.getServer();original=Files.readAllBytes(file(s));var root=NbtIo.readCompressed(file(s),NbtAccounter.unlimitedHeap());var list=root.getCompound("data").getList("actors",10);
            check(list.size()==1,"fixture actor count");inputActor=list.getCompound(0).copy();check(inputActor.getString("storyId").equals(NpcEvents.STORY),"fixture identity");
            uuid=inputActor.getUUID("entityUUID");instance=inputActor.getUUID("instanceId");generation=inputActor.getInt("generation");facts=new HashSet<>();inputActor.getList("facts",8).forEach(t->facts.add(t.getAsString()));
            Files.write(E.resolve(MODE+"-input.dat"),original);
            try {
                var d=NarrativeData.get(s);check(!blockedMode(),"corruption returned normal campaign");
                var status=StoryStorage.status(s.overworld().getDataStorage());
                check(status.outcome()==(MODE.equals("Recover")?StoryStorage.Outcome.RECOVERABLE_CORRUPTION:StoryStorage.Outcome.LOADED),"load outcome");
                check(d.actors.size()==1,"actor discarded");note("PASS load "+status.diagnostic());
            }catch(StoryStorage.Blocked ex){check(blockedMode()&&!ex.status.writable()&&ex.status.outcome()==StoryStorage.Outcome.CORRUPT,"unexpected write-block result");note("PASS load "+ex.status.diagnostic());}
            ready=true;
        }catch(Exception ex){fail(ex);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));mc.createWorldOpenFlows().openWorld("corrupt-"+(MODE.equals("RecoverReload")?"Recover":MODE),mc::stop);}
            if(mc.screen instanceof BackupConfirmScreen screen)for(var c:screen.children())if(c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>12000)throw new IllegalStateException("NPC_CORRUPTION_TIMEOUT");
            if(!ready||mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%10!=0)return;queued=true;var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{try{
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(id);if(p==null)return;
                var found=StoryActors.find(s,uuid);if(!(found instanceof StoryNpc npc))return;
                var tag=npc.getPersistentData().getCompound(StoryActors.TAG);
                check(tag.getString("storyId").equals(NpcEvents.STORY)&&tag.getUUID("instanceId").equals(instance)&&tag.getInt("generation")==generation,"live identity changed");
                long count=0;for(var level:s.getAllLevels())for(var en:level.getAllEntities())if(en instanceof StoryNpc)count++;check(count==1,"duplicate loaded NPC");
                if(blockedMode()){
                    check(!StoryStorage.available(s),"write guard dropped");
                    check(NpcEvents.begin(p,p.blockPosition(),p.blockPosition())==null,"blocked store created replacement NPC");
                    check(StoryActors.integrity(s).stream().anyMatch(x->x.contains("writeBlocked=true")),"missing load integrity");
                }else{
                    var d=NarrativeData.get(s);var r=d.actors.get(NpcEvents.STORY);check(r!=null&&d.actors.size()==1&&r.entityId.equals(uuid)&&r.instanceId.equals(instance)&&r.generation==generation&&r.checkpoint.checkpoint==4&&r.facts.equals(facts),"resident fact/identity drift");
                    check(StoryActors.canonical(npc,r)&&StoryActors.integrity(s).isEmpty(),"canonical/integrity");
                }
                if((held+=10)<100)return;
                for(int n=0;n<3;n++)s.saveEverything(true,true,true);
                String inputHash=StoryStorage.digest(original);byte[] after=Files.readAllBytes(file(s));
                if(blockedMode())check(Arrays.equals(original,after),"corrupt story overwritten by real save");
                if(blockedMode()||MODE.equals("Recover"))check(Arrays.equals(original,Files.readAllBytes(file(s).getParent().resolve("whileaway-quarantine/"+inputHash+".dat"))),"original quarantine missing");
                Files.write(E.resolve(MODE+".dat"),after);
                note("PASS "+MODE+" connectedClient=true liveNPC=1 identityUnchanged=true generation="+generation+" checkpoint=4 saveDispatches=3 writeBlocked="+blockedMode()+" originalHash="+inputHash+" finalHash="+StoryStorage.digest(after)+" emptyCampaignReturned=false heldTicks="+held);
                stopped=true;s.halt(false);mc.execute(mc::stop);
            }catch(Exception ex){fail(ex);}finally{queued=false;}});
        }catch(Exception ex){fail(ex);}
    }
}
