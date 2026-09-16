package io.github.whileaway;

import io.github.whileaway.entity.StoryNpc;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** First real campaign NPC adapter: Yeoul chooses a place beside the return light.
 * No item/reward transaction, building edit, time limit, camera lock or forced teleport. */
public final class NpcEvents {
    public static final String ID="yeoul_return_light", STORY="npc:yeoul";
    public static boolean isNpc(StoryActorRecord r){return ID.equals(r.eventId);}
    /** Scene presence is not ownership: a resident survives independently, but an unfinished
     * witnessed event cannot materialize/advance while every eligible witness is absent. */
    public static ServerPlayer witness(ServerLevel level,BlockPos anchor) {
        return level.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.blockPosition().distSqr(anchor)<=24*24)
            .min(Comparator.comparingDouble(p->p.blockPosition().distSqr(anchor))).orElse(null);
    }
    public static boolean hasWitness(net.minecraft.server.MinecraftServer server,StoryActorRecord r) {
        if(r.checkpoint.checkpoint==4)return true; // Completion does not make the resident player-owned.
        for(var level:server.getAllLevels())if(level.dimension().location().toString().equals(r.dimension))
            return witness(level,r.position)!=null;
        return false;
    }
    public static StoryActorRecord begin(ServerPlayer player,BlockPos start,BlockPos home) {
        var server=player.getServer();if(!StoryStorage.available(server))return null;
        var d=NarrativeData.get(server);var existing=d.actors.get(STORY);if(existing!=null)return existing;
        var level=player.serverLevel();var npc=WhileAway.STORY_NPC.get().create(level);
        if(npc==null||!clear(level,start)||!clear(level,home))return null;
        npc.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);
        // Facts enter the same durable PREPARED reservation; no half-initialized NPC record is saved.
        var r=StoryActors.prepare(npc,STORY,ID,"resident",UUID.randomUUID(),null,false,record->NpcState.initialize(record,home));
        RecoveryBoundaryEvent.emit(player,ID,"NPC_A_PREPARED",0);
        if(!hasWitness(server,r))return r; // Keep the durable PREPARED reservation; no generation change.
        if(StoryActors.spawnPrepared(level,r,npc))RecoveryBoundaryEvent.emit(player,ID,"NPC_B_APPEARED",1);
        return r;
    }
    private static boolean clear(ServerLevel level,BlockPos p) {
        return level.hasChunkAt(p)&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
            &&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isCollisionShapeFullBlock(level,p.below());
    }
    public static boolean interact(StoryNpc npc,ServerPlayer p) {
        if(!StoryStorage.available(p.getServer())||npc.level()!=p.level()||npc.distanceToSqr(p)>36||!p.isAlive())return false;
        var r=NarrativeData.get(p.getServer()).actors.get(STORY);
        if(r==null||!StoryActors.canonical(npc,r)||r.checkpoint.terminal())return false;
        if(!NpcState.integrity(r).isEmpty()){StoryStorage.block(p.getServer(),String.join(";",NpcState.integrity(r)));return false;}
        if(r.checkpoint.checkpoint!=1)return false;
        r.facts.add(NpcState.MET+p.getUUID());r.facts.add(NpcState.DIALOGUE);
        r.facts.add(NpcState.relationship("player:"+p.getUUID(),"first_conversation"));
        r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;StoryActors.tag(npc,r);
        StoryActors.checkpoint(npc,r,2,null); // Claim the line durably before presentation: no duplicate confirmation.
        p.displayClientMessage(Component.translatable("story.whileaway.yeoul.introduction"),false);
        RecoveryBoundaryEvent.emit(p,ID,"NPC_C_CONVERSATION_COMMITTED",2);return true;
    }
    public static void update(StoryNpc npc,StoryActorRecord r) {
        var level=(ServerLevel)npc.level();var server=level.getServer();
        var problems=NpcState.integrity(r);
        if(!problems.isEmpty()){StoryStorage.block(server,String.join(";",problems));npc.getNavigation().stop();return;}
        var home=NpcState.home(r);
        var nearby=witness(level,npc.blockPosition());
        // No off-screen auto-failure. NPC's life may continue when loaded; event movement needs a living witness.
        if(r.checkpoint.checkpoint<4&&(nearby==null||!nearby.isAlive())){npc.storyNavigation().externalPause(npc);StoryActors.suspend(npc,r,"npc_no_witness");return;}
        boolean arrived=false;
        if(r.checkpoint.checkpoint>=2) {
            var environment=NpcEnvironment.inspect(level,r.dimension,npc.blockPosition(),home);
            if(!environment.dependenciesReady()) {
                npc.storyNavigation().externalPause(npc);
                npc.storyNavigation().diagnostic(npc,r,environment);
                // Completion/relationships are never revoked by later environmental damage.
                if(r.checkpoint.checkpoint<4)StoryActors.suspend(npc,r,environment.diagnostic());
                return;
            }
            var path=npc.storyNavigation().step(npc,home);
            npc.storyNavigation().diagnostic(npc,r,environment.withPath(path));
            if(path!=NpcEnvironment.Path.AVAILABLE&&path!=NpcEnvironment.Path.ARRIVED){
                if(path!=NpcEnvironment.Path.TEMPORARY_FAILURE&&r.checkpoint.checkpoint<4)StoryActors.suspend(npc,r,environment.withPath(path).diagnostic());
                return;
            }
            arrived=path==NpcEnvironment.Path.ARRIVED;
        }
        if(r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED) {
            r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;StoryActors.tag(npc,r);
            StoryActors.checkpoint(npc,r,r.checkpoint.checkpoint,null);
        }
        if(r.checkpoint.checkpoint==1) {
            npc.getNavigation().stop();if(nearby!=null)npc.getLookControl().setLookAt(nearby);return;
        }
        if(!arrived)return;
        npc.getNavigation().stop();
        // Look at the light instead of delivering explanatory monologues. The object must actually exist.
        var light=home.offset(2,0,0);
        npc.getLookControl().setLookAt(light.getX()+.5,light.getY()+.5,light.getZ()+.5);
        if(r.checkpoint.checkpoint==2) {
            if(!NpcEnvironment.isReturnLight(level.getBlockState(light)))return;
            r.facts.add(NpcState.ARRIVED);r.facts.add(NpcState.OBSERVED);
            StoryActors.checkpoint(npc,r,3,null);
            if(nearby!=null)RecoveryBoundaryEvent.emit(nearby,ID,"NPC_D_BEFORE_COMPLETE",3);
            return;
        }
        if(r.checkpoint.checkpoint==3) {
            NpcState.settle(r);r.facts.add(NpcState.LINK);
            for(var fact:List.copyOf(r.facts))if(fact.startsWith(NpcState.MET))
                r.facts.add(NpcState.relationship("player:"+fact.substring(NpcState.MET.length()),"shared_return_light"));
            StoryActors.checkpoint(npc,r,4,null); // Facts + connection receipt + ongoing residence in ONE story commit.
            if(nearby!=null) {
                RecoveryBoundaryEvent.emit(nearby,ID,"NPC_E_AFTER_COMPLETE",4);
                nearby.displayClientMessage(Component.translatable("story.whileaway.yeoul.settled"),false);
            }
        }
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        var server=event.getServer();if(server.getTickCount()%40!=0||!StoryStorage.available(server))return;
        if(NarrativeData.get(server).actors.containsKey(STORY))return;
        var level=server.getLevel(CityDistrict.KEY);if(level==null||!CityDistrict.get(level).ready())return;
        for(var p:level.players())if(p.isAlive()&&!p.isSpectator()&&p.blockPosition().distSqr(CityDistrict.ARRIVAL)<144) {
            begin(p,CityDistrict.ARRIVAL.offset(2,0,0),CityDistrict.RETURN_LIGHT.offset(-2,0,0));break;
        }
    }
}
