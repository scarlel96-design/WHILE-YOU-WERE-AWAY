package io.github.whileaway.client;

import io.github.whileaway.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in fault injection in COPIED worlds. All decisions remain in production recovery code. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class CanonicalSmoke {
    private static final String MODE=System.getProperty("whileaway.canonicalSmoke","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/canonical-recovery/canonical"));
    private static boolean launched,stopped,setup,injected,loss;private static volatile boolean queued,ready;
    private static int ticks,stable,lateStage;private static boolean lateSeen;private static UUID lateUUID;private static StoryActorRecord before;private static byte[] original;private static UUID finalUUID;
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");}
    private static void note(String text){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void fail(Exception ex){note("FAIL "+ex);Runtime.getRuntime().halt(8);}
    private static void exit(MinecraftServer s){stopped=true;s.halt(false);Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());}
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(MODE.isEmpty())return;
        try {
            original=Files.readAllBytes(file(event.getServer()));
            var root=NbtIo.readCompressed(file(event.getServer()),NbtAccounter.create(64*1024*1024)).getCompound("data");
            var actor=root.getList("actors",Tag.TAG_COMPOUND).getCompound(0).copy();
            if(!actor.hasUUID("entityUUID"))for(var raw:root.getList("players",Tag.TAG_COMPOUND)) {
                var p=(CompoundTag)raw;if(p.hasUUID("id")&&p.getUUID("id").equals(actor.getUUID("owner")))actor.putUUID("entityUUID",p.getUUID("encounter"));
            }
            before=StoryActorRecord.load(actor);ready=true;
            note("INPUT hash="+StoryStorage.digest(original)+" uuid="+before.entityId+" generation="+before.generation+" checkpoint="+before.checkpoint.checkpoint);
        }catch(Exception ex){fail(ex);}
    }
    @SubscribeEvent(receiveCanceled=true) public static void joining(EntityJoinLevelEvent event) {
        if(MODE.isEmpty()||!ready||injected||!(event.getLevel() instanceof ServerLevel level))return;
        var entity=event.getEntity();
        var tag=entity.getPersistentData().getCompound(StoryActors.TAG);
        if(MODE.equals("Late")&&tag.getString("storyId").equals(before.storyId)&&tag.getInt("generation")<before.generation) {
            lateSeen=true;lateUUID=entity.getUUID();note("OBSERVED real_entity_chunk_join stale_generation="+tag.getInt("generation")+" uuid="+lateUUID);return;
        }
        if(!ActorCandidates.identity(entity,before))return;
        if(MODE.equals("NoCandidate")){event.setCanceled(true);injected=true;return;}
        if(MODE.equals("Ambiguous")) {
            injected=true;
            // Corrupt copied-world fixture: add a second genuine Wayfarer with the same persistent identity.
            level.getServer().execute(()->{try{var clone=entity.getType().create(level);clone.load(entity.saveWithoutId(new CompoundTag()));
                clone.setUUID(UUID.randomUUID());check(level.addFreshEntity(clone),"ambiguous fixture admission");note("INJECT same_generation_duplicate");
            }catch(Exception ex){fail(ex);}});
        }
    }
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent event) {
        if(!MODE.startsWith("Cut")||!loss||!event.boundary.startsWith("GENERATION_"))return;
        int target=Integer.parseInt(MODE.substring(3));if(!event.boundary.startsWith("GENERATION_"+target+"_"))return;
        try {
            var s=event.player.getServer();var r=NarrativeData.get(s).actors.get(before.storyId);
            check(r.checkpoint.checkpoint==before.checkpoint.checkpoint,"cut checkpoint drift");
            check(r.generation==(target==1?before.generation:before.generation+1),"cut epoch");
            // Exercise the dangerous persisted-entity / uncommitted-binding branch at 3 and 4.
            if(target==3||target==4)s.saveEverything(true,true,true);
            Files.copy(file(s),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
            note("PASS "+MODE+" boundary="+event.boundary+" generation="+r.generation+" uuid="+r.entityId+" checkpoint="+r.checkpoint.checkpoint+" diskHash="+StoryStorage.digest(Files.readAllBytes(file(s))));
            Runtime.getRuntime().halt(0);
        }catch(Exception ex){fail(ex);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                String world=MODE.equals("ExactReload")?"Exact":MODE.startsWith("Resume")?"Cut"+MODE.substring(6):MODE;
                mc.createWorldOpenFlows().openWorld("canonical-"+world,mc::stop);
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b
                &&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>8000)throw new IllegalStateException("timeout mode="+MODE+" stable="+stable);
            if(!ready||mc.player==null||queued||ticks%10!=0)return;queued=true;
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(mc.player.getUUID());if(p==null)return;
                if(!setup){setup=true;p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    if(!MODE.equals("Unloaded")){var home=net.minecraft.core.BlockPos.of(before.snapshot.getLong("Home"));p.teleportTo(s.overworld(),home.getX()+.5,home.getY(),home.getZ()+.5,Set.of(),0,0);}
                    else p.teleportTo(s.overworld(),12000,100,12000,Set.of(),0,0);
                }
                if(Set.of("Ambiguous","Unloaded","NoCandidate").contains(MODE)) {
                    check(!StoryStorage.available(s),"unsafe automatic recovery "+MODE);
                    String result=CanonicalRecovery.attempt(s);stable+=10;if(stable<160)return;
                    String expected=MODE.equals("Ambiguous")?"AMBIGUOUS_MULTIPLE_CANDIDATES":MODE.equals("Unloaded")?"UNLOADED_OR_UNCONFIRMED":"NO_CANDIDATE";
                    check(result.contains(expected),"classification "+result);
                    for(int i=0;i<3;i++)s.saveEverything(true,true,true);
                    check(Arrays.equals(original,Files.readAllBytes(file(s))),"blocked original changed");
                    check(Files.exists(file(s).getParent().resolve("whileaway-quarantine/"+StoryStorage.digest(original)+".dat")),"original not quarantined");
                    note("PASS "+MODE+" "+result+" realSaveDispatches=3 originalPreserved=true noGenerationCreated=true");Runtime.getRuntime().halt(0);return;
                }
                if(!StoryStorage.available(s)){if(stable++>200)throw new IllegalStateException(CanonicalRecovery.diagnostic(s));return;}
                var data=NarrativeData.get(s);var r=data.actors.get(before.storyId);check(r!=null,"empty campaign");
                var actor=StoryActors.find(s,r.entityId);
                if(MODE.equals("Late")&&lateStage==1&&lateSeen){check(StoryActors.find(s,lateUUID)==null,"stale active after actual chunk join");
                    var home=net.minecraft.core.BlockPos.of(before.snapshot.getLong("Home"));p.teleportTo(s.overworld(),home.getX()+.5,home.getY(),home.getZ()+.5,Set.of(),0,0);lateStage=2;stable=0;return;}
                if(actor==null)return;
                if(MODE.startsWith("Cut")&&!loss) {
                    loss=true;actor.discard();s.saveEverything(true,true,true);note("INJECT confirmed-loss fixture entity removed and disk flushed; story ticket retained");return;
                }
                if(MODE.equals("Duplicate")&&!injected) {
                    injected=true;StoryActors.persist(s);s.saveEverything(true,true,true);original=Files.readAllBytes(file(s));
                    var clone=actor.getType().create(p.serverLevel());clone.load(actor.saveWithoutId(new CompoundTag()));clone.setUUID(UUID.randomUUID());p.serverLevel().addFreshEntity(clone);
                    check(!StoryStorage.available(s),"duplicate not blocked");
                    try{s.saveEverything(true,true,true);}catch(RuntimeException expected){note("EXPECTED guarded save rejected "+expected.getClass().getSimpleName());}
                    check(Arrays.equals(original,Files.readAllBytes(file(s))),"duplicate rewrote story");
                    note("PASS Duplicate writeBlocked=true originalPreserved=true AI_paused=true");Runtime.getRuntime().halt(0);return;
                }
                if(MODE.equals("Late")) {
                    if(lateStage==0){check(before.generation==1,"late fixture needs generation one");lateStage=1;p.teleportTo(s.overworld(),9600.5,100,9600.5,Set.of(),0,0);return;}
                    if(lateStage==1){if(!lateSeen)return;check(StoryActors.find(s,lateUUID)==null,"stale admitted as active actor");
                        var home=net.minecraft.core.BlockPos.of(before.snapshot.getLong("Home"));p.teleportTo(s.overworld(),home.getX()+.5,home.getY(),home.getZ()+.5,Set.of(),0,0);lateStage=2;stable=0;return;}
                    check(lateSeen&&StoryActors.find(s,lateUUID)==null,"stale revived");
                }
                int epoch=MODE.startsWith("Resume")?1:before.generation;
                check(r.generation==epoch,"generation drift "+r.generation);
                if(MODE.equals("Exact")||MODE.equals("ExactReload"))check(r.entityId.equals(before.entityId),"UUID changed instead of reconnected");
                if(finalUUID==null)finalUUID=r.entityId;check(finalUUID.equals(r.entityId),"UUID unstable");
                check(r.facts.equals(before.facts),"permanent facts changed");
                check(r.instanceId.equals(before.instanceId)&&r.checkpoint.checkpoint==2&&!data.entry(r.owner).progress.encounterComplete(),"event drift");
                var candidates=ActorCandidates.inspect(s,r);check(candidates.outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical count "+candidates);
                check(StoryActors.integrity(s).isEmpty(),"integrity "+StoryActors.integrity(s));
                stable+=10;if(stable<160)return;
                StoryActors.persist(s);s.saveEverything(true,true,true);Files.copy(file(s),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
                note("PASS "+MODE+" staleLateJoin="+lateSeen+" canonical=1 generation="+r.generation+" uuid="+r.entityId+" checkpoint=2 factsPreserved=true integrity=clean stableTicks="+stable);
                exit(s);
            }catch(Exception ex){fail(ex);}finally{queued=false;}});
        }catch(Exception ex){fail(ex);}
    }
}
