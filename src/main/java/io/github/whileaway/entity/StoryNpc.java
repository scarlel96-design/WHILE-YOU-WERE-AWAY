package io.github.whileaway.entity;

import io.github.whileaway.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Campaign entity, not a Villager test double. Appearance is explicitly provisional. */
public final class StoryNpc extends PathfinderMob {
    private final NpcNavigation storyNavigation=new NpcNavigation();
    public NpcNavigation storyNavigation(){return storyNavigation;}
    public StoryNpc(EntityType<? extends PathfinderMob> type,Level level) {
        super(type,level);setPersistenceRequired();setCustomName(Component.translatable("entity.whileaway.story_npc"));
    }
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean canBeLeashed(){return false;}
    @Override public boolean hurt(DamageSource source,float amount){return false;} // Essential resident; no player-combat story failure.
    @Override public void tick() {
        if(level() instanceof ServerLevel level) {
            if(!StoryStorage.available(level.getServer())){getNavigation().stop();return;}
            var r=NarrativeData.get(level.getServer()).actors.get(getPersistentData().getCompound(StoryActors.TAG).getString("storyId"));
            if(r==null||!NpcEvents.isNpc(r)||!StoryActors.canonical(this,r)||r.lifecycle==StoryActorRecord.Lifecycle.RETIRED){getNavigation().stop();return;}
            super.tick();
            if(tickCount%10==0)NpcEvents.update(this,r);
            if(tickCount%100==0&&!blockPosition().equals(r.position)&&StoryStorage.available(level.getServer())){StoryActors.capture(this,r);StoryActors.persist(level.getServer());}
        }else super.tick();
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(player instanceof ServerPlayer p)return NpcEvents.interact(this,p)?InteractionResult.CONSUME:InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }
}
