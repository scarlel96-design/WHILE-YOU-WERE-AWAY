package io.github.whileaway;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A one-shot consequence of the intake ledger, not a random screen jump-scare. */
public final class CityHorror {
    public static final BlockPos STAGE=new BlockPos(40,65,11);
    public static boolean tryStart(ServerPlayer p) {
        if(!p.level().dimension().equals(CityDistrict.KEY)||!StoryStorage.available(p.getServer()))return false;
        var d=NarrativeData.get(p.getServer());var e=d.entry(p.getUUID());
        long now=p.getServer().overworld().getGameTime();
        if(!e.cityVisited||(e.cityClues&1)==0||e.cityShockTriggered||!p.isAlive()||p.isSleeping()||p.isSpectator()||p.isCreative()
            ||p.getHealth()<8||p.serverLevel().getDifficulty()==Difficulty.PEACEFUL||!StoryConfig.ENCOUNTERS.get()
            ||now<e.readingUntil||now<e.transitAfter||p.blockPosition().distSqr(STAGE)>14*14)return false;
        if(CityDistrict.get(p.serverLevel()).layoutVersion()!=2||!CityDistrict.get(p.serverLevel()).ready())return false;
        return SceneRecovery.begin(p,3,STAGE);
    }
    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer p&&p.tickCount%20==0)tryStart(p);
    }
}
