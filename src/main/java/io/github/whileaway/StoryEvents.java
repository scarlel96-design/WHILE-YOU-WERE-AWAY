package io.github.whileaway;

import io.github.whileaway.core.*;
import io.github.whileaway.entity.Wayfarer;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class StoryEvents {
    private final Map<UUID, Vec3> positions=new HashMap<>();
    private final Map<UUID, Long> activeUntil=new HashMap<>();
    public static void sound(ServerPlayer p, SoundEvent event, SoundSource source, BlockPos pos, float volume) {
        p.connection.send(new ClientboundSoundPacket(Holder.direct(event),source,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,
            volume*StoryConfig.SOUND_GAIN.get().floatValue(),1f,p.getRandom().nextLong()));
    }
    public static void music(ServerPlayer p, SoundEvent event, float volume) {
        // One short, original cue. Stop an existing music track to avoid competing mixes.
        p.connection.send(new ClientboundStopSoundPacket(null,SoundSource.MUSIC));
        sound(p,event,SoundSource.MUSIC,p.blockPosition(),volume);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer p) {
            if(!StoryStorage.available(p.getServer())){p.sendSystemMessage(Component.literal("WhileAway: story load blocked; original preserved; recovery pending."));return;}
            var d=NarrativeData.get(p.getServer()); var e=d.entry(p.getUUID());
            e.progress=e.progress.defer(p.getServer().overworld().getGameTime()+600); d.setDirty();
            p.sendSystemMessage(Component.translatable("story.whileaway.welcome"));
        }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        positions.remove(event.getEntity().getUUID()); activeUntil.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || p.tickCount%20!=0 || p.level().dimension()!=Level.OVERWORLD) return;
        if(!StoryStorage.available(p.getServer()))return;
        var server=p.serverLevel(); long now=server.getGameTime(); UUID id=p.getUUID();
        Vec3 old=positions.put(id,p.position());
        if(old!=null && old.distanceToSqr(p.position())>.03) activeUntil.put(id,now+100);
        var data=NarrativeData.get(p.getServer()); var e=data.entry(id);
        if(SceneRecovery.busy(p))return;
        if(e.station==null) return;
        boolean near=p.blockPosition().distSqr(e.station)<32*32;
        boolean hostile=!server.getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(12),m->m.getTarget()==p).isEmpty();
        boolean eligible=!p.isCreative()&&!p.isSpectator()&&p.isAlive()&&!p.isSleeping()&&p.getHealth()>=8
            &&server.getDifficulty()!=Difficulty.PEACEFUL&&StoryConfig.ENCOUNTERS.get();
        boolean reading=now<e.readingUntil;
        // Book screen itself sends no movement. A 60s grace plus activity gating protects reading.
        var ctx=new Director.Context(now,near,activeUntil.getOrDefault(id,0L)>now,eligible,reading,hostile,e.encounter!=null);
        var cue=Director.choose(e.progress,ctx);
        if(cue==Director.Cue.NONE)return;
        switch(cue) {
            case DISTANT_KNOCK -> sound(p,WhileAway.KNOCK.get(),SoundSource.AMBIENT,e.station.offset(7,2,3),.65f);
            case RETURNING_STEPS -> {
                sound(p,WhileAway.STEPS.get(),SoundSource.AMBIENT,e.station.offset(-7,0,5),.6f);
                if(StoryConfig.MUSIC.get())music(p,WhileAway.UNEASE.get(),.3f);
            }
            case MANIFEST -> { if(!manifest(p,e,server)) {e.progress=e.progress.defer(now+200);data.setDirty();return;} }
            default -> {}
        }
        e.progress=e.progress.eventPlayed(now,StoryConfig.COOLDOWN_SECONDS.get()*20L); data.setDirty();
    }
    private boolean manifest(ServerPlayer p,NarrativeData.Entry e,ServerLevel level) {
        // Search only loaded ground and never place in a wall, on the player, or in a fluid.
        for(int[] offset:new int[][]{{12,0},{-12,0},{0,12},{0,-12},{10,10},{-10,-10}}) {
            BlockPos rough=e.station.offset(offset[0],0,offset[1]);
            if(!level.hasChunkAt(rough))continue;
            BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,rough);
            if(Math.abs(pos.getY()-e.station.getY())>10 || p.blockPosition().distSqr(pos)<8*8
                || !level.getFluidState(pos).isEmpty() || !level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below()))continue;
            Wayfarer mob=WhileAway.WAYFARER.get().create(level);
            if(mob==null)return false;
            mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            if(!level.noCollision(mob) || !level.isUnobstructed(mob))continue;
            mob.bind(p.getUUID(),e.station);
            if(!StoryActors.beginChase(p,mob))return false;
            sound(p,WhileAway.BREATH.get(),SoundSource.HOSTILE,pos,.7f);
            p.displayClientMessage(Component.translatable("story.whileaway.arrival"),true);
            return true;
        }
        return false;
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("whileaway")
            .then(Commands.literal("debug").requires(s->Boolean.getBoolean("whileaway.debug")&&s.hasPermission(2))
                .then(Commands.literal("load").executes(c->{
                    var status=StoryStorage.status(c.getSource().getServer().overworld().getDataStorage());
                    c.getSource().sendSuccess(()->Component.literal(status==null?"STORY_LOAD not_attempted":status.diagnostic()),false);return 1;
                }))
                .then(Commands.literal("events").executes(c->{
                    var p=c.getSource().getPlayerOrException();var e=NarrativeData.get(p.getServer()).entry(p.getUUID());
                    e.scenes.values().forEach(r->c.getSource().sendSuccess(()->Component.literal(r.eventId()+" "+r.id+" "+r.progress.state+" checkpoint="+r.progress.checkpoint+" reason="+r.progress.reason),false));return 1;
                }))
                .then(Commands.literal("integrity").executes(c->{
                    var p=c.getSource().getPlayerOrException();var issues=new ArrayList<>(StoryActors.integrity(p.getServer()));
                    if(StoryStorage.available(p.getServer()))issues.addAll(SceneRecovery.integrity(NarrativeData.get(p.getServer()).entry(p.getUUID())));
                    c.getSource().sendSuccess(()->Component.literal(issues.isEmpty()?"PASS scene integrity":String.join("; ",issues)),false);return issues.isEmpty()?1:0;
                })))
            .then(Commands.literal("return").executes(c->CityTransit.leave(c.getSource().getPlayerOrException())?1:0))
            .then(Commands.literal("status").executes(c->{
                var p=c.getSource().getPlayerOrException();var e=NarrativeData.get(p.getServer()).entry(p.getUUID());
                c.getSource().sendSuccess(()->Component.literal("WhileAway stage="+e.progress.stage()+" clues="+e.progress.distinctClues()+" encounter="+(e.encounter!=null)+" complete="+e.progress.encounterComplete()+" city="+e.cityVisited+" cityClues="+Integer.bitCount(e.cityClues)),false);return 1;
            }))
            .then(Commands.literal("recover").requires(s->s.hasPermission(2)).executes(c->{
                var p=c.getSource().getPlayerOrException();var data=NarrativeData.get(p.getServer());var e=data.entry(p.getUUID());
                var actorRecord=data.actors.get(StoryActors.chaseId(p.getUUID()));
                if(actorRecord!=null) {
                    StoryActors.reconcile(p.getServer(),actorRecord);
                    c.getSource().sendSuccess(()->Component.literal("Actor recovery inspected: "+actorRecord.lifecycle+" checkpoint="+actorRecord.checkpoint.checkpoint+"; unloaded storage is deferred"),false);return 1;
                }
                // Legacy operator path; unknown/unloaded actors are not blindly recreated.
                if(e.encounter!=null) {
                    var entity=p.getServer().overworld().getEntity(e.encounter);
                    if(entity!=null)entity.discard();
                    else {c.getSource().sendFailure(Component.translatable("story.whileaway.recovery_unloaded"));return 0;}
                }
                e.encounter=null;e.progress=e.progress.defer(p.getServer().overworld().getGameTime()+600);data.setDirty();
                c.getSource().sendSuccess(()->Component.translatable("story.whileaway.recovered"),false);return 1;
            })));
    }
}
