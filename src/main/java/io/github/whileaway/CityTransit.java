package io.github.whileaway;

import io.github.whileaway.core.CityAccess;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CityTransit {
    private CityTransit() {}
    public static boolean enter(ServerPlayer p,BlockPos relay) {
        if(!StoryStorage.available(p.getServer()))return false;
        var data=NarrativeData.get(p.getServer());var e=data.entry(p.getUUID());
        if(p.level().dimension()!=Level.OVERWORLD || e.station==null || e.station.distSqr(relay)>32*32
            || p.blockPosition().distSqr(relay)>8*8 || !p.serverLevel().getBlockState(relay).is(WhileAway.RELAY.get()))return false;
        if(!CityAccess.canEnter(e.progress)||e.encounter!=null){message(p,"city.whileaway.locked");return false;}
        if(!eligible(p,e))return false;
        var city=p.getServer().getLevel(CityDistrict.KEY);
        if(city==null){message(p,"city.whileaway.unavailable");return false;}
        var district=CityDistrict.get(city);district.start();
        if(!district.ready()){message(p,"city.whileaway.preparing");return false;}
        var landing=findLanding(city,CityDistrict.ARRIVAL,5);
        if(landing==null){message(p,"city.whileaway.obstructed");return false;}
        var returnPos=p.blockPosition();float yaw=p.getYRot(),pitch=p.getXRot();
        if(!p.teleportTo(city,landing.getX()+.5,landing.getY(),landing.getZ()+.5,Set.of(),180,0))return false;
        p.setDeltaMovement(0,0,0);p.fallDistance=0;
        e.returnPosition=returnPos;e.returnYaw=yaw;e.returnPitch=pitch;e.cityVisited=true;
        e.transitAfter=p.getServer().overworld().getGameTime()+40;data.setDirty();
        message(p,"city.whileaway.entered");
        SceneRecovery.begin(p,2,landing);return true;
    }
    public static boolean leave(ServerPlayer p) {
        if(!StoryStorage.available(p.getServer()))return false;
        if(!p.level().dimension().equals(CityDistrict.KEY))return false;
        var data=NarrativeData.get(p.getServer());var e=data.entry(p.getUUID());if(!eligible(p,e))return false;
        var overworld=p.getServer().overworld();
        BlockPos landing=e.returnPosition==null?null:findLanding(overworld,e.returnPosition,6);
        if(landing==null) {
            var spawn=overworld.getSharedSpawnPos();
            var surface=overworld.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn);
            landing=findLanding(overworld,surface,8);
        }
        if(landing==null){message(p,"city.whileaway.obstructed");return false;}
        if(!p.teleportTo(overworld,landing.getX()+.5,landing.getY(),landing.getZ()+.5,Set.of(),e.returnYaw,e.returnPitch))return false;
        p.setDeltaMovement(0,0,0);p.fallDistance=0;e.transitAfter=overworld.getGameTime()+40;
        e.progress=e.progress.defer(overworld.getGameTime()+600);data.setDirty();
        message(p,"city.whileaway.returned");return true;
    }
    private static boolean eligible(ServerPlayer p,NarrativeData.Entry e) {
        return p.isAlive()&&!p.isPassenger()&&!p.isVehicle()&&!p.isSleeping()
            &&p.getServer().overworld().getGameTime()>=e.transitAfter;
    }
    /** Search without breaking blocks or moving a player's construction. */
    public static BlockPos findLanding(ServerLevel level,BlockPos origin,int radius) {
        for(int r=0;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            for(int dy:new int[]{0,1,-1,2,-2,3,-3}) {
                var p=origin.offset(dx,dy,dz);level.getChunkAt(p);
                if(p.getY()<=level.getMinBuildHeight()||p.getY()+2>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(p))continue;
                if(!level.getBlockState(p.below()).isCollisionShapeFullBlock(level,p.below()))continue;
                if(!level.getFluidState(p).isEmpty()||!level.getFluidState(p.above()).isEmpty())continue;
                if(level.noCollision(new AABB(p.getX()+.2,p.getY(),p.getZ()+.2,p.getX()+.8,p.getY()+1.9,p.getZ()+.8)))return p;
            }
        }
        return null;
    }
    private static void message(ServerPlayer p,String key){p.displayClientMessage(Component.translatable(key),false);}
}
