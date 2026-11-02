package io.github.whileaway.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.whileaway.content.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Draw the distant tilt without moving blocks, collision or the camera. */
public final class RailBeamRenderer implements BlockEntityRenderer<RailBeamEntity> {
    public RailBeamRenderer(BlockEntityRendererProvider.Context context){}
    @Override public int getViewDistance(){return 128;}
    @Override public boolean shouldRender(RailBeamEntity beam,Vec3 camera){return Vec3.atCenterOf(beam.anchor()).closerThan(camera,128);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(RailBeamEntity beam){return new net.minecraft.world.phys.AABB(beam.getBlockPos()).inflate(2);}
    @Override public void render(RailBeamEntity beam,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(beam.getLevel()==null)return;
        var state=beam.getBlockState();var facing=state.getValue(RailBeamBlock.FACING);
        int segment=state.getValue(RailBeamBlock.SEGMENT);
        var anchor=beam.anchor();var center=Vec3.atCenterOf(anchor);
        double distance=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(center);
        float angle=RailBeamEntity.visualAngle(beam.getLevel().getGameTime()+partial,anchor.asLong(),distance);
        double x=segment*facing.getStepX(),z=segment*facing.getStepZ();
        pose.pushPose();pose.translate(.5-x,.5,.5-z);
        if(facing.getAxis()==Direction.Axis.X)pose.mulPose(Axis.ZP.rotationDegrees(angle*facing.getStepX()));
        else pose.mulPose(Axis.XP.rotationDegrees(-angle*facing.getStepZ()));
        pose.translate(x-.5,-.5,z-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.WEATHERED_CUT_COPPER.defaultBlockState(),pose,buffers,light,overlay);
        pose.popPose();
    }
}
