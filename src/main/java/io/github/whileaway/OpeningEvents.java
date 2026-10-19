package io.github.whileaway;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** A short playable memory route. Only semantic transitions enter story storage. */
public final class OpeningEvents {
    public static final ResourceKey<Level> MEMORY=ResourceKey.create(Registries.DIMENSION,WhileAway.id("memory_route"));
    private static final int WALK_Y=129;
    private static final BlockPos LEVER=new BlockPos(12,WALK_Y,1);
    private static final BlockPos MARKER=new BlockPos(1,WALK_Y+3,3);
    private final Map<UUID,Integer> shown=new HashMap<>();

    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        shown.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%5!=0||!StoryStorage.available(p.getServer()))return;
        var server=p.getServer();
        var data=NarrativeData.get(server);
        var entry=data.entry(p.getUUID());
        switch(entry.openingStage) {
            case NOT_STARTED -> {
                if(p.level().dimension()!=Level.OVERWORLD||!p.isAlive())return;
                var memory=server.getLevel(MEMORY);
                if(memory==null)return;
                data.commitOpening(p.getUUID(),OpeningStage.NOT_STARTED,p.blockPosition(),p.getYRot(),p.getXRot(),server.registryAccess());
                build(memory);
                enter(p,memory);
            }
            case STARTED -> {
                var memory=server.getLevel(MEMORY);
                if(memory==null)return;
                build(memory);
                if(p.level().dimension()!=MEMORY) {
                    if(p.isAlive())enter(p,memory);
                    return;
                }
                advance(p,memory,data);
            }
            case TRANSITION -> {
                if(p.level().dimension()==MEMORY) {
                    leave(p,entry);
                } else if(p.level().dimension()==Level.OVERWORLD) {
                    data.commitOpening(p.getUUID(),OpeningStage.TRANSITION,null,0,0,server.registryAccess());
                }
            }
            case OVERWORLD_COMMITTED -> {
                if(p.level().dimension()==MEMORY) {
                    leave(p,entry);
                } else if(p.level().dimension()==Level.OVERWORLD) {
                    data.commitOpening(p.getUUID(),OpeningStage.OVERWORLD_COMMITTED,null,0,0,server.registryAccess());
                    p.sendSystemMessage(Component.translatable("story.whileaway.opening.overworld"));
                }
            }
            case COMPLETED -> shown.remove(p.getUUID());
        }
    }

    private void enter(ServerPlayer p,ServerLevel memory) {
        if(p.teleportTo(memory,2.5,WALK_Y,0.5,Set.of(),-90,0)) {
            p.setDeltaMovement(0,0,0);
            p.fallDistance=0;
            say(p,1,"story.whileaway.opening.route");
        }
    }

    private void advance(ServerPlayer p,ServerLevel memory,NarrativeData data) {
        if(p.getZ()<-2||p.getZ()>3||p.getY()<WALK_Y-2||p.getY()>WALK_Y+4)return;
        int x=p.blockPosition().getX();
        if(x>=7)say(p,2,"story.whileaway.opening.rail");
        if(x>=11&&!powered(memory))say(p,3,"story.whileaway.opening.signal");
        if(!powered(memory))return;
        for(int z=-2;z<=2;z++)for(int y=WALK_Y;y<=WALK_Y+2;y++)
            memory.setBlock(new BlockPos(16,y,z),Blocks.AIR.defaultBlockState(),2);
        if(x>=19&&x<23&&shown.getOrDefault(p.getUUID(),0)<4) {
            shown.put(p.getUUID(),4);
            p.teleportTo(memory,23.5,WALK_Y,0.5,Set.of(),-90,0);
            p.setDeltaMovement(0,0,0);
            say(p,5,"story.whileaway.opening.gap");
            return;
        }
        if(x>=27)say(p,6,"story.whileaway.opening.voice");
        if(x>=33&&x<36&&shown.getOrDefault(p.getUUID(),0)<7) {
            shown.put(p.getUUID(),7);
            p.teleportTo(memory,36.5,WALK_Y,0.5,Set.of(),-90,0);
            p.setDeltaMovement(0,0,0);
            say(p,8,"story.whileaway.opening.last_gap");
            return;
        }
        if(x>=39) {
            var entry=data.entry(p.getUUID());
            data.commitOpening(p.getUUID(),OpeningStage.STARTED,null,0,0,p.getServer().registryAccess());
            leave(p,entry);
        }
    }

    private boolean powered(ServerLevel level) {
        var state=level.getBlockState(LEVER);
        return state.is(Blocks.LEVER)&&state.getValue(LeverBlock.POWERED);
    }

    private void leave(ServerPlayer p,NarrativeData.Entry entry) {
        var overworld=p.getServer().overworld();
        var landing=CityTransit.findLanding(overworld,entry.openingReturnPosition,8);
        if(landing==null)return;
        if(p.teleportTo(overworld,landing.getX()+.5,landing.getY(),landing.getZ()+.5,Set.of(),entry.openingReturnYaw,entry.openingReturnPitch)) {
            p.setDeltaMovement(0,0,0);
            p.fallDistance=0;
        }
    }

    private void say(ServerPlayer p,int marker,String key) {
        if(shown.getOrDefault(p.getUUID(),0)>=marker)return;
        shown.put(p.getUUID(),marker);
        p.displayClientMessage(Component.translatable(key),true);
        if(marker==2||marker==5||marker==8)
            p.level().playSound(null,p.blockPosition(),SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.BLOCKS,.18f,
                marker==8?.75f:1.25f);
    }

    private void build(ServerLevel level) {
        if(level.getBlockState(MARKER).is(Blocks.CHISELED_DEEPSLATE))return;
        for(int x=0;x<=41;x++)for(int z=-3;z<=3;z++) {
            var floor=new BlockPos(x,WALK_Y-1,z);
            level.setBlock(floor,(z==0?Blocks.POLISHED_DEEPSLATE:Blocks.DEEPSLATE_BRICKS).defaultBlockState(),2);
            for(int y=WALK_Y;y<=WALK_Y+2;y++) {
                var pos=new BlockPos(x,y,z);
                var wall=Math.abs(z)==3;
                level.setBlock(pos,wall?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            }
        }
        for(int x=3;x<=10;x++)level.setBlock(new BlockPos(x,WALK_Y,-1),Blocks.RAIL.defaultBlockState(),2);
        for(int z=-2;z<=2;z++)for(int y=WALK_Y;y<=WALK_Y+2;y++)
            level.setBlock(new BlockPos(16,y,z),Blocks.IRON_BARS.defaultBlockState(),2);
        level.setBlock(new BlockPos(11,WALK_Y,2),Blocks.REDSTONE_LAMP.defaultBlockState(),2);
        level.setBlock(LEVER,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.FLOOR)
            .setValue(LeverBlock.FACING,Direction.NORTH),2);
        level.setBlock(new BlockPos(28,WALK_Y,2),Blocks.OAK_SIGN.defaultBlockState(),2);
        level.setBlock(MARKER,Blocks.CHISELED_DEEPSLATE.defaultBlockState(),2);
    }
}
