package io.github.whileaway.content;

import io.github.whileaway.WhileAway;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Client-local scenery. No story facts or presentation timers are serialized. */
public final class RailBeamEntity extends BlockEntity {
    private Vec3 previous;
    private int walking,still;
    private long nextEcho;
    public RailBeamEntity(BlockPos p,BlockState s){super(WhileAway.RAIL_BEAM_ENTITY.get(),p,s);}
    public BlockPos anchor(){return worldPosition.relative(getBlockState().getValue(RailBeamBlock.FACING).getOpposite(),getBlockState().getValue(RailBeamBlock.SEGMENT));}
    public static float visualAngle(double tick,long seed,double distance){
        if(distance<=24||distance>=96)return 0;
        double phase=((tick+Math.floorMod(seed,1800))%1800+1800)%1800;
        if(phase>=160)return 0;
        return (float)(12*Math.sin(Math.PI*phase/160)*Math.min(1,Math.min((distance-24)/12,(96-distance)/12)));
    }
    public boolean stepEcho(boolean moving,boolean eligible,long tick){
        if(!eligible){walking=still=0;return false;}
        if(moving){walking=Math.min(12,walking+1);still=0;return false;}
        if(walking>=12&&++still==8){walking=0;if(tick>=nextEcho){nextEcho=tick+1200;return true;}}
        return false;
    }
    public static void clientTick(Level level,BlockPos pos,BlockState state,RailBeamEntity beam){
        if(!level.isClientSide||state.getValue(RailBeamBlock.SEGMENT)!=0)return;
        for(var player:level.players())if(player.isLocalPlayer()){
            var now=player.position();
            boolean eligible=player.isAlive()&&!player.isSpectator()&&pos.distSqr(player.blockPosition())<=12*12;
            boolean moving=beam.previous!=null&&now.subtract(beam.previous).horizontalDistanceSqr()>.002;
            beam.previous=now;
            if(beam.stepEcho(moving,eligible&&player.onGround(),level.getGameTime())){
                // One extra footfall comes from the empty service bay after movement stops.
                var source=pos.below(4);
                level.playLocalSound(source.getX()+.5,source.getY()+.5,source.getZ()+.5,SoundEvents.METAL_STEP,SoundSource.BLOCKS,.35f,.75f,false);
            }
            break;
        }
    }
}
