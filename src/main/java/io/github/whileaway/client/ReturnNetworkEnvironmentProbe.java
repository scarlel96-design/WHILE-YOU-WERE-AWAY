package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import io.github.whileaway.content.RelayBlock;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Copied-world fault injection only; never activated by normal gameplay. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ReturnNetworkEnvironmentProbe {
    private static final boolean ENABLED="EnvironmentMatrix".equals(System.getProperty("whileaway.networkSmoke",""));
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/return-network-closure/client"));
    private static final Map<BlockPos,BlockState> saved=new LinkedHashMap<>(), expected=new LinkedHashMap<>();
    private static int phase,variant=-1;
    private static long began;
    private static CompoundTag frozen;
    private static Vec3 collisionStart;
    private static void check(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    private static void note(String s)throws Exception {Files.writeString(E.resolve("EnvironmentMatrix.txt"),s+"\n",StandardOpenOption.APPEND);}
    private static void put(ServerLevel l,BlockPos p,BlockState b){check(l.hasChunkAt(p),"fixture chunk unavailable");saved.putIfAbsent(p,l.getBlockState(p));expected.put(p,b);l.setBlock(p,b,3);}
    private static void restore(ServerLevel l){saved.forEach((p,b)->l.setBlock(p,b,3));saved.clear();expected.clear();}
    private static void untouched(ServerLevel l){expected.forEach((p,b)->check(l.getBlockState(p).equals(b),"production modified fixture block "+p));}
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent e){
        if(!ENABLED)return;
        var l=e.player.serverLevel();var d=NarrativeData.get(e.player.getServer());
        if(e.boundary.equals("RN_A_DISCOVERED")&&phase==0){
            var a=l.getEntity(d.actors.get(NpcEvents.STORY).entityId);check(a instanceof StoryNpc,"actual NPC required");
            var at=a.blockPosition();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)for(int y=0;y<2;y++)put(l,at.offset(x,y,z),Blocks.BARRIER.defaultBlockState());
            frozen=d.returnNetwork().save();began=l.getGameTime();phase=1;
        }else if(e.boundary.equals("RN_D_INTERVENTION")&&phase==4){
            frozen=d.returnNetwork().save();put(l,ReturnNetworkEvents.STAND,Blocks.DIAMOND_BLOCK.defaultBlockState());phase=5;
        }
    }
    public static boolean step(MinecraftServer server,ServerPlayer p,ServerLevel l,ReturnNetworkState s,StoryActorRecord r)throws Exception {
        if(!ENABLED||phase==0||phase==7)return false;
        var entity=l.getEntity(r.entityId);check(entity instanceof StoryNpc,"fixture NPC disappeared");var npc=(StoryNpc)entity;
        if(phase==1){
            check(frozen.equals(s.save()),"closed route advanced checkpoint");untouched(l);
            if(l.getGameTime()-began<220)return true;
            check(npc.storyNavigation().state()==NpcEnvironment.Path.UNREACHABLE||npc.storyNavigation().state()==NpcEnvironment.Path.TEMPORARY_FAILURE,"expected bounded path failure");
            note("PASS closed NPC route realPath="+npc.storyNavigation().state()+" attempts="+npc.storyNavigation().attempts()+" cp=1 unchanged=true noObstacleDeletion=true");
            restore(l);phase=2;return true;
        }
        if(phase==2){if(s.stage!=ReturnNetworkState.Stage.NPC_JOINED)return false;note("PASS path restored NPC walked naturally to relay same identity");frozen=s.save();phase=3;}
        if(phase==3){
            check(frozen.equals(s.save()),"environment variant advanced durable state");
            if(variant<0){variant=0;beginVariant(l,p,npc);return true;}
            untouched(l);if(l.getGameTime()-began<100)return true;
            if(variant<=4)check(!ReturnNetworkEvents.environmentReady(l,s),"bad environment treated ready");
            if(variant==5)check(!ReturnNetworkEvents.interactEquipment(p,ReturnNetworkEvents.RELAY),"out-of-reach equipment interaction accepted");
            if(variant==6)note("COLLISION actual NPC displacementSquared="+npc.position().distanceToSqr(collisionStart));
            if(variant==7)check(ReturnNetworkEvents.environmentReady(l,s),"unrelated lighting changed structural validity");
            note("PASS environment variant="+variant+" cp=2 durableUnchanged=true playerBlocksPreserved=true");
            restore(l);p.teleportTo(l,45.5,65,47.5,Set.of(),90,0);
            if(++variant<8){beginVariant(l,p,npc);return true;}
            phase=4;return true;
        }
        if(phase==4)return false;
        if(phase==5){
            check(frozen.equals(s.save())&&ReturnNetworkEvents.activatedEquipment(l),"intervention physical application did not follow durable commit");
            restore(l);put(l,ReturnNetworkEvents.RELAY,WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT,false));
            began=l.getGameTime();phase=6;return true;
        }
        check(phase==6&&frozen.equals(s.save()),"unlit relay advanced response");untouched(l);
        if(l.getGameTime()-began<120)return true;
        check(!ReturnNetworkEvents.environmentReady(l,s),"unlit structural relay accepted");
        note("PASS structural LIT loss cp=4 unchanged; restore original activated relay; production never flickered it");
        restore(l);phase=7;p.teleportTo(l,45.5,65,47.5,Set.of(),90,0);return true;
    }
    private static void beginVariant(ServerLevel l,ServerPlayer p,StoryNpc npc){
        began=l.getGameTime();var stand=ReturnNetworkEvents.STAND;var relay=ReturnNetworkEvents.RELAY;
        switch(variant){
            case 0->put(l,stand,Blocks.DIAMOND_BLOCK.defaultBlockState());
            case 1->put(l,stand,Blocks.WATER.defaultBlockState());
            case 2->put(l,stand.above(),Blocks.DIAMOND_BLOCK.defaultBlockState());
            case 3->put(l,relay,Blocks.AIR.defaultBlockState());
            case 4->put(l,relay,Blocks.REDSTONE_LAMP.defaultBlockState());
            case 5->p.teleportTo(l,relay.getX()+8.5,65,relay.getZ()+.5,Set.of(),90,0);
            case 6->{collisionStart=npc.position();p.teleportTo(l,npc.getX()+.2,npc.getY(),npc.getZ(),Set.of(),90,0);}
            case 7->put(l,relay.above(),Blocks.SEA_LANTERN.defaultBlockState());
            default->throw new IllegalStateException("variant");
        }
    }
}
