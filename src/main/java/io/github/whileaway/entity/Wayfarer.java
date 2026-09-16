package io.github.whileaway.entity;

import io.github.whileaway.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** First encounter: a warned, localized patrol. Never griefs blocks or teleports. */
public final class Wayfarer extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HUNTING = SynchedEntityData.defineId(Wayfarer.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wayfarer.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.wayfarer.walk");
    private static final RawAnimation HUNT = RawAnimation.begin().thenLoop("animation.wayfarer.hunt");
    private UUID witness;
    private BlockPos home;
    private int lifetime, windup, attackCooldown, huntTicks;
    private boolean checkpointRestored;
    public Wayfarer(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); xpReward = 0; }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.25);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(HUNTING, false); }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); }
    public void bind(UUID owner, BlockPos station) { witness=owner; home=station.immutable(); }
    public UUID witness() { return witness; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public void tick() {
        if(level() instanceof ServerLevel level&&!StoryStorage.available(level.getServer())){getNavigation().stop();return;}
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (home == null) home=blockPosition();
        // A development spawn egg creates a passive demonstrator, not an unowned killer.
        if (witness == null) return;
        var ledger=NarrativeData.get(server.getServer());var entry=ledger.entry(witness);
        if(entry.progress.encounterComplete()||(entry.encounter!=null&&!getUUID().equals(entry.encounter))){discard();return;}
        var actorRecord=StoryActors.attach(this);
        if(actorRecord==null){discard();return;}
        if(!server.dimension().location().toString().equals(actorRecord.dimension)){getNavigation().stop();return;}
        restoreCheckpointRuntime(actorRecord);
        var player = server.getServer().getPlayerList().getPlayer(witness);
        if (player == null) { getNavigation().stop(); StoryActors.suspend(this,actorRecord,"owner_offline"); return; }
        // Absence, death and dimension travel suspend AI and active-time budgets; they are not story failures.
        if (player.level() != level() || player.isDeadOrDying()) { getNavigation().stop();windup=Math.max(windup,30);StoryActors.suspend(this,actorRecord,"owner_dead_or_other_dimension");return; }
        if (!StoryActors.ownerPresent(server.getServer(),actorRecord)) {getNavigation().stop();windup=Math.max(windup,30);StoryActors.suspend(this,actorRecord,"owner_left_region");return;}
        if (player.isCreative() || player.isSpectator() || server.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                || !StoryConfig.ENCOUNTERS.get()) { getNavigation().stop(); entityData.set(HUNTING, false); windup=0; return; }
        if(actorRecord.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED) {
            actorRecord.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;StoryActors.checkpoint(this,actorRecord,actorRecord.checkpoint.checkpoint,null);
        }
        if(tickCount%100==0)StoryActors.checkpoint(this,actorRecord,actorRecord.checkpoint.checkpoint,null);
        lifetime++;
        if (lifetime > 2400) { finish(true); return; }
        if (lifetime % 110 == 0) StoryEvents.sound(player, WhileAway.BREATH.get(), SoundSource.HOSTILE, blockPosition(), .5f);
        boolean hunting=entityData.get(HUNTING);
        if (!hunting && lifetime > 200 && distanceToSqr(player) < 5*5 && hasLineOfSight(player)) {
            entityData.set(HUNTING,true); windup=30;
            StoryActors.checkpoint(this,actorRecord,2,"pursuit_started");
            StoryEvents.sound(player, WhileAway.ATTACK.get(), SoundSource.HOSTILE, blockPosition(), .65f);
        }
        if (entityData.get(HUNTING)) {
            huntTicks++;
            if (huntTicks > 600) { finish(true); return; }
            if (windup > 0) { windup--; getNavigation().stop(); getLookControl().setLookAt(player); return; }
            if (tickCount % 10 == 0) getNavigation().moveTo(player, 1.25);
            if (attackCooldown > 0) attackCooldown--;
            if (distanceToSqr(player) < 2.8 && hasLineOfSight(player) && attackCooldown == 0) {
                doHurtTarget(player); attackCooldown=60; windup=20;
                StoryEvents.sound(player, WhileAway.ATTACK.get(), SoundSource.HOSTILE, blockPosition(), .5f);
            }
        } else if (tickCount % 60 == 0) {
            int side=(lifetime / 240) % 2 == 0 ? -1 : 1;
            getNavigation().moveTo(home.getX()+side*8+.5, home.getY(), home.getZ()+3.5, .65);
        }
    }
    public void restoreCheckpointRuntime(StoryActorRecord actorRecord) {
        if(!StoryActors.canonical(this,actorRecord))throw new IllegalStateException("RUNTIME_IDENTITY_CONFLICT");
        if(!checkpointRestored) {
            // Chunk NBT may lag behind the story-file commit. Restore ONLY encounter AI,
            // not health, physical inventory, or a position which might now contain a player's blocks.
            var saved=actorRecord.snapshot;
            lifetime=Math.max(0,saved.getInt("StoryAge"));huntTicks=Math.max(0,saved.getInt("HuntTicks"));
            entityData.set(HUNTING,actorRecord.facts.contains("pursuit_started")||saved.getBoolean("Hunting"));
            windup=Math.max(30,saved.getInt("Windup"));attackCooldown=Math.max(20,saved.getInt("AttackCooldown"));
            checkpointRestored=true;
        }
    }
    private void finish(boolean complete) {
        if(level() instanceof ServerLevel s && witness!=null){if(StoryStorage.available(s.getServer()))StoryActors.completeChase(this);}
        else discard();
    }
    @Override public void die(DamageSource source) { super.die(source); finish(true); }
    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t); if(witness!=null)t.putUUID("Witness",witness);
        if(witness!=null)t.putString("ownerEventId","wayfarer_first:"+witness);
        if(home!=null)t.putLong("Home",home.asLong()); t.putInt("StoryAge",lifetime);
        t.putBoolean("Hunting",entityData.get(HUNTING)); t.putInt("HuntTicks",huntTicks);
        t.putInt("Windup",windup); t.putInt("AttackCooldown",attackCooldown);
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t); checkpointRestored=false; if(t.hasUUID("Witness"))witness=t.getUUID("Witness");
        if(t.contains("Home"))home=BlockPos.of(t.getLong("Home")); lifetime=Math.max(0,t.getInt("StoryAge"));
        entityData.set(HUNTING,t.getBoolean("Hunting")); huntTicks=Math.max(0,t.getInt("HuntTicks"));
        windup=Math.max(30,t.getInt("Windup")); attackCooldown=Math.max(20,t.getInt("AttackCooldown"));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this,"body",5,state -> state.setAndContinue(entityData.get(HUNTING)?HUNT:state.isMoving()?WALK:IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
