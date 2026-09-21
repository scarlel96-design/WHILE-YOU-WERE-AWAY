package io.github.whileaway.client;

import io.github.whileaway.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Opt-in copied-world availability probe; no actor relocation or chunk tickets.
 * Only the test player moves. Chunk/entity readiness is read from the real server. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ReturnNetworkClosureProbe {
    private static final String MODE=System.getProperty("whileaway.networkSmoke","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/return-network-closure/client"));
    private static int phase,stable,observations;
    private static CompoundTag frozen;
    private static BlockPos npcPosition;
    private static UUID event,entity,instance;
    private static int generation;
    private static boolean storageGap;
    private static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
    private static void note(String s)throws Exception {Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent e) {
        if(!MODE.equals("Availability")||!e.boundary.equals("RN_A_DISCOVERED")||phase!=0)return;
        try {
            var server=e.player.getServer();var d=NarrativeData.get(server);var r=d.actors.get(NpcEvents.STORY);
            frozen=d.returnNetwork().save();event=d.returnNetwork().instance;entity=r.entityId;instance=r.instanceId;generation=r.generation;
            npcPosition=r.position;
            check(new ChunkPos(npcPosition).toLong()!=new ChunkPos(ReturnNetworkEvents.RELAY).toLong(),"availability fixture requires actual separated approach chunks");
            server.getPlayerList().setViewDistance(2);server.getPlayerList().setSimulationDistance(5);
            phase=1;e.player.setGameMode(GameType.SPECTATOR);
            e.player.teleportTo(e.player.serverLevel(),40.5,90,168.5,Set.of(),0,0);
            note("AVAILABILITY start cp=1 event="+event+" npc="+npcPosition+" npcChunk="+new ChunkPos(npcPosition)+" relayChunk="+new ChunkPos(ReturnNetworkEvents.RELAY)+" cachedWait="+ReturnNetworkEvents.waitState(e.player.serverLevel()));
        }catch(Exception x){throw new IllegalStateException("availability fixture",x);}
    }
    public static boolean step(MinecraftServer server,ServerPlayer p,ServerLevel city,ReturnNetworkState s,StoryActorRecord r)throws Exception {
        if(!MODE.equals("Availability")||phase==0||phase>=3)return false;
        check(frozen.equals(s.save()),"availability changed durable event");
        check(entity.equals(r.entityId)&&instance.equals(r.instanceId)&&generation==r.generation,"availability changed actor identity");
        boolean npcChunk=city.hasChunkAt(npcPosition),npcStore=city.areEntitiesLoaded(new ChunkPos(npcPosition).toLong());
        var actual=city.getEntity(entity);boolean npc=actual!=null&&StoryActors.canonical(actual,r);
        boolean relay=city.hasChunkAt(ReturnNetworkEvents.RELAY);
        if(npcChunk&&!npcStore&&!storageGap){storageGap=true;note("OBSERVED real NPC chunk present / entity storage unavailable cp=1");}
        if(++observations%10==0)note("AVAILABILITY phase="+phase+" npcChunk="+npcChunk+" npcStore="+npcStore+" canonical="+npc+" relayChunk="+relay+" player="+p.position());
        check(observations<240,"availability combination not reproduced within bounded real observation window");
        boolean ready=phase==1?npc&&!relay:!npc&&!npcStore&&relay;
        if(!ready){stable=0;return true;}
        stable+=10;if(stable<80)return true;
        if(phase==2)check(ReturnNetworkEvents.originalEquipment(city),"loaded relay differs from original equipment");
        server.saveEverything(true,true,true);
        Files.copy(server.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve("Availability-phase"+phase+".dat"),StandardCopyOption.REPLACE_EXISTING);
        note("PASS availability phase="+phase+" heldTicks="+stable+" eventUnchanged=true identityUnchanged=true npc="+npc+" relay="+relay+" cachedWait="+ReturnNetworkEvents.waitState(city));
        stable=0;observations=0;phase++;
        if(phase==2)p.teleportTo(city,40.5,90,-55.5,Set.of(),0,0);
        else {note("AVAILABILITY storage-gap="+(storageGap?"OBSERVED":"NOT_REPRODUCED")+" natural return next; no actor teleport/no force chunk");p.setGameMode(GameType.CREATIVE);p.teleportTo(city,45.5,65,47.5,Set.of(),90,0);}
        return true;
    }
}
