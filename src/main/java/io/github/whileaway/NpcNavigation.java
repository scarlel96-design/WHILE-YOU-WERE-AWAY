package io.github.whileaway;

import io.github.whileaway.core.NpcPathBudget;
import io.github.whileaway.entity.StoryNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Bounded navigation of an existing NPC. This layer never owns identity, generation or blocks. */
public final class NpcNavigation {
    private final NpcPathBudget budget=new NpcPathBudget();
    private BlockPos target;private Vec3 progressPosition;private long progressTick;
    private NpcEnvironment.Path state=NpcEnvironment.Path.NOT_ASSESSED;
    private String lastDiagnostic="";
    public NpcEnvironment.Path state(){return state;}
    public int attempts(){return budget.attempts();}
    public void externalPause(StoryNpc npc){npc.getNavigation().stop();budget.reset();target=null;progressPosition=null;state=NpcEnvironment.Path.NOT_ASSESSED;}
    public void diagnostic(StoryNpc npc,StoryActorRecord r,NpcEnvironment.Assessment environment){
        String code=environment.diagnostic();
        if(!code.equals(lastDiagnostic)){
            com.mojang.logging.LogUtils.getLogger().debug("{} storyId={} instance={} generation={} checkpoint={}",code,r.storyId,r.instanceId,r.generation,r.checkpoint.checkpoint);
            lastDiagnostic=code;
        }
    }
    public static boolean corridorLoaded(ServerLevel level,BlockPos from,BlockPos to){
        // Bounded chunk metadata reads, never a world scan or synchronous chunk load.
        int x0=(Math.min(from.getX(),to.getX())-8)>>4,x1=(Math.max(from.getX(),to.getX())+8)>>4;
        int z0=(Math.min(from.getZ(),to.getZ())-8)>>4,z1=(Math.max(from.getZ(),to.getZ())+8)>>4;
        if(x1-x0>5||z1-z0>5)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(!level.hasChunk(x,z))return false;
        return true;
    }
    public NpcEnvironment.Path step(StoryNpc npc,BlockPos destination){
        var level=(ServerLevel)npc.level();long now=level.getGameTime();var nav=npc.getNavigation();
        if(!destination.equals(target)){externalPause(npc);target=destination.immutable();progressPosition=npc.position();progressTick=now;}
        if(npc.distanceToSqr(destination.getX()+.5,destination.getY(),destination.getZ()+.5)<=1.4){nav.stop();budget.progress();return state=NpcEnvironment.Path.ARRIVED;}
        if(npc.blockPosition().distSqr(destination)>48*48){nav.stop();return state=NpcEnvironment.Path.OUT_OF_RANGE;}
        if(!corridorLoaded(level,npc.blockPosition(),destination)){nav.stop();budget.reset();progressPosition=npc.position();progressTick=now;return state=NpcEnvironment.Path.CHUNK_UNAVAILABLE;}
        if(progressPosition==null||npc.position().distanceToSqr(progressPosition)>.0625){progressPosition=npc.position();progressTick=now;budget.progress();}
        if(!nav.isDone()){
            if(now-progressTick<NpcPathBudget.STALL_TICKS)return state=NpcEnvironment.Path.AVAILABLE;
            nav.stop();budget.failed(now);progressTick=now;
            return state=budget.unreachable()?NpcEnvironment.Path.UNREACHABLE:NpcEnvironment.Path.TEMPORARY_FAILURE;
        }
        if(!budget.ready(now))return state=budget.unreachable()?NpcEnvironment.Path.UNREACHABLE:NpcEnvironment.Path.TEMPORARY_FAILURE;
        budget.attempted(now);
        var path=nav.createPath(destination,0);
        if(path==null||!path.canReach()||!nav.moveTo(path,.65)){
            nav.stop();budget.failed(now);
            return state=budget.unreachable()?NpcEnvironment.Path.UNREACHABLE:NpcEnvironment.Path.TEMPORARY_FAILURE;
        }
        progressPosition=npc.position();progressTick=now;
        return state=NpcEnvironment.Path.AVAILABLE;
    }
}
