package io.github.whileaway.client;

import io.github.whileaway.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in copied-world load and actual server save-dispatch tests. No production failure bypass. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class LoadGuardSmoke {
    private static final String MODE=System.getProperty("whileaway.loadSmoke","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/load-guard/client"));
    private static boolean launched,stopped;private static volatile boolean queued,ready;
    private static int ticks,stable;private static UUID uuid,instance;private static int generation,checkpoint;
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");}
    private static void exit(MinecraftServer s){stopped=true;s.halt(false);Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());}
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(MODE.isEmpty())return;var s=event.getServer();
        try {
            byte[] before=Files.readAllBytes(file(s));
            try {
                var data=NarrativeData.get(s);
                check(!MODE.equals("Blocked")&&!MODE.equals("Unsupported"),"corrupt save returned a campaign");
                var status=StoryStorage.status(s.overworld().getDataStorage());
                check(data.actors.size()==1,"actor count at load");var a=data.actors.values().iterator().next();
                uuid=a.entityId;instance=a.instanceId;generation=a.generation;checkpoint=a.checkpoint.checkpoint;
                check(status.outcome()==(MODE.equals("Recover")?StoryStorage.Outcome.RECOVERABLE_CORRUPTION:StoryStorage.Outcome.LOADED),"load outcome");
                note("PASS load "+status.diagnostic());ready=true;
            } catch(StoryStorage.Blocked blocked) {
                check(MODE.equals("Blocked")||MODE.equals("Unsupported"),"unexpected load block");
                check(!blocked.status.writable(),"write guard missing");
                check(blocked.status.outcome()==(MODE.equals("Unsupported")?StoryStorage.Outcome.UNSUPPORTED:StoryStorage.Outcome.CORRUPT),"blocked classification");
                for(int i=0;i<3;i++)s.saveEverything(true,true,true);
                check(Arrays.equals(before,Files.readAllBytes(file(s))),"corrupt overwritten by real server save dispatch");
                String digest=StoryStorage.digest(before);
                check(Arrays.equals(before,Files.readAllBytes(file(s).getParent().resolve("whileaway-quarantine/"+digest+".dat"))),"quarantine missing");
                check(StoryActors.integrity(s).stream().anyMatch(x->x.contains("writeBlocked=true")),"load integrity missing");
                note("PASS "+MODE+" actual_server_save_dispatches=3 originalHash="+digest+" writeBlocked=true emptyCampaignReturned=false");
                // Startup has no connected player. Do not deadlock the client's startup progress loop
                // by waiting for a queued main-thread stop before the server becomes ready.
                Runtime.getRuntime().halt(0);
            }
        } catch(Exception e){note("FAIL "+e);exit(s);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){
                launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                mc.createWorldOpenFlows().openWorld("load-"+(MODE.equals("RecoverReload")?"Recover":MODE),mc::stop);
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button
                &&button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){button.onPress();break;}
            if(++ticks>12000)throw new IllegalStateException("client load timeout");
            if(!ready||mc.player==null||queued||ticks%10!=0)return;queued=true;
            mc.getSingleplayerServer().execute(()->{try{
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(mc.player.getUUID());if(p==null)return;
                var d=NarrativeData.get(s);var a=d.actors.values().iterator().next();
                check(a.entityId.equals(uuid)&&a.instanceId.equals(instance)&&a.generation==generation&&a.checkpoint.checkpoint==checkpoint,"identity/checkpoint drift");
                if(stable==0){var home=net.minecraft.core.BlockPos.of(a.snapshot.getLong("Home"));p.teleportTo(s.overworld(),home.getX()+.5,home.getY(),home.getZ()+.5,Set.of(),0,0);p.getAbilities().invulnerable=true;}
                var actor=StoryActors.find(s,uuid);if(actor==null)return;
                check(StoryActors.canonical(actor,a),"noncanonical actor");
                check(s.overworld().getEntitiesOfClass(io.github.whileaway.entity.Wayfarer.class,actor.getBoundingBox().inflate(64)).size()==1,"duplicate actor");
                stable+=10;if(stable<100)return;
                check(StoryActors.integrity(s).isEmpty(),"post-recovery integrity");
                StoryActors.persist(s);s.saveEverything(true,true,true);
                Files.copy(file(s),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
                note("PASS "+MODE+" canonical=1 sameUUID=true generation="+generation+" checkpoint="+checkpoint+" stableTicks="+stable+" integrity=clean");exit(s);
            }catch(Exception e){note("FAIL "+e);exit(mc.getSingleplayerServer());}finally{queued=false;}});
        }catch(Exception e){note("FAIL "+e);stopped=true;mc.stop();}
    }
}
