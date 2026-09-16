package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.core.SceneTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Client-only presentation. Never changes camera, keybindings, gamma, or the player's world. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ClientScene {
    private static int age=-1;
    private static int kind=1;
    private static int loadingGrace;
    private static java.util.UUID eventId=ScenePayload.NONE, lease=ScenePayload.NONE;
    private static int checkpoint,shown,hideCaptions;
    private static int received,cues;
    public static int receivedCount(){return received;}
    public static int dispatchedCues(){return cues;}
    public static int checkpoint(){return checkpoint;}
    public static java.util.UUID ownerEventId(){return eventId;}
    private static void acknowledge(boolean suspended) {
        if(!eventId.equals(ScenePayload.NONE)&&Minecraft.getInstance().getConnection()!=null)
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SceneAck(eventId,lease,checkpoint,shown,suspended));
    }
    private static BlockPos origin=BlockPos.ZERO;
    private static ResourceKey<Level> dimension;
    private static final java.util.List<io.github.whileaway.entity.Wayfarer> figures=new java.util.ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel actorWorld;
    public static int apparitionCount(){return figures.size();}
    public static boolean active(){return age>=0;}
    public static void receive(ScenePayload payload) {
        var mc=Minecraft.getInstance();
        if(active()&&eventId.equals(payload.eventId())&&lease.equals(payload.lease())&&!eventId.equals(ScenePayload.NONE))return;
        cancel();
        if((payload.kind()<1||payload.kind()>3)||mc.level==null)return;
        kind=payload.kind();received++;
        loadingGrace=kind==2?200:0;
        origin=payload.origin();dimension=mc.level.dimension();
        eventId=payload.eventId();lease=payload.lease();checkpoint=Math.clamp(payload.checkpoint(),0,4);
        shown=payload.shown()&127;hideCaptions=shown;
        if(checkpoint==4){eventId=ScenePayload.NONE;return;}
        age=io.github.whileaway.core.EventCheckpoint.TICKS[checkpoint];
        if(kind==3&&age>=60&&age<195)spawnFigures();
        mc.getSoundManager().stop(null,SoundSource.MUSIC);
    }
    private static void cancel(){
        if(age>=0)acknowledge(true);
        eventId=ScenePayload.NONE;lease=ScenePayload.NONE;
        var manager=Minecraft.getInstance().getSoundManager();
        for(var sound:java.util.List.of(WhileAway.KNOCK.get(),WhileAway.STEPS.get(),WhileAway.BREATH.get(),WhileAway.ATTACK.get()))
            manager.stop(sound.getLocation(),SoundSource.AMBIENT);
        if(actorWorld!=null)for(var figure:figures)actorWorld.removeEntity(figure.getId(),net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        figures.clear();actorWorld=null;age=-1;dimension=null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(age<0)return;var mc=Minecraft.getInstance();
        if(loadingGrace>0&&mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen){loadingGrace--;return;}
        if(mc.level==null||mc.player==null||!mc.player.isAlive()||mc.level.dimension()!=dimension
                ||mc.screen!=null||mc.isPaused()||mc.player.blockPosition().distSqr(origin)>40*40){cancel();return;}
        age++;
        if(kind==3) {
            if(age==60) {
                spawnFigures();
                play(WhileAway.BREATH.get(),origin,.55f);
            }
            // All seven drop into the lane together, stopping above the ground: the departure never completed.
            if(age>=130&&age<150)for(var figure:figures)figure.setPos(figure.getX(),origin.getY()+3.2-(age-130)*.14,figure.getZ());
            if(age==130)play(WhileAway.ATTACK.get(),origin,.48f);
            if(age==195){for(var figure:figures)actorWorld.removeEntity(figure.getId(),net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);figures.clear();}
        }
        if(age==45)play(WhileAway.KNOCK.get(),origin.offset(8,2,0),.32f);
        if(age==110)play(WhileAway.STEPS.get(),origin.offset(-7,0,5),.3f);
        if(age==150)play(WhileAway.BREATH.get(),origin.offset(0,1,10),.2f);
        if(age>=65&&age<150&&age%5==0) {
            double x=origin.getX()+.5+(age-65)/85.0*5;
            mc.level.addParticle(ParticleTypes.ASH,x,origin.getY()+.2,origin.getZ()+.5,0,.006,0);
        }
        int caption=SceneTimeline.at(age).caption();
        if(caption!=0&&(shown&(1<<(caption-1)))==0){shown|=1<<(caption-1);acknowledge(false);}
        if(checkpoint<4&&age>=io.github.whileaway.core.EventCheckpoint.TICKS[checkpoint+1]){checkpoint++;acknowledge(false);}
        if(age>=SceneTimeline.LENGTH){eventId=ScenePayload.NONE;cancel();}
    }
    private static void spawnFigures() {
        if(!figures.isEmpty())return;
        var mc=Minecraft.getInstance();actorWorld=mc.level;
        for(int i=0;i<7;i++) {
            var entity=WhileAway.WAYFARER.get().create(mc.level);
            int id=-200000-i;if(entity==null||mc.level.getEntity(id)!=null){cancel();return;}
            entity.setId(id);entity.setNoAi(true);entity.setNoGravity(true);
            entity.getPersistentData().putUUID("ownerEventId",eventId);
            double descent=Math.clamp(age-130,0,19)*.14;
            entity.moveTo(origin.getX()-6+i*2+.5,origin.getY()+3.2-descent,origin.getZ()+.5,0,0);
            mc.level.addEntity(entity);figures.add(entity);
        }
    }
    private static void play(SoundEvent sound,BlockPos at,float volume) {
        int bit=switch(age){case 45->4;case 60->8;case 110->16;case 130->32;case 150->64;default->0;};
        if(bit!=0&&(shown&bit)!=0)return;
        shown|=bit;acknowledge(false);cues++;
        var mc=Minecraft.getInstance();
        mc.level.playLocalSound(at.getX()+.5,at.getY()+.5,at.getZ()+.5,sound,SoundSource.AMBIENT,volume,1,false);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        if(age<0||!ClientConfig.CINEMATIC_OVERLAY.get())return;
        var mc=Minecraft.getInstance();if(mc.screen!=null||mc.player==null)return;
        var frame=SceneTimeline.at(age);var g=event.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();
        int alpha=(int)(frame.pressure()*105),dark=(alpha<<24)|0x071015,transparent=0x00071015;
        int edge=Math.max(8,h/5),bar=(int)(h*frame.bars());
        g.fillGradient(0,0,w,edge,dark,transparent);g.fillGradient(0,h-edge,w,h,transparent,dark);
        if(bar>0){g.fill(0,0,w,bar,0xff07090d);g.fill(0,h-bar,w,h,0xff07090d);}
        int captionY=h-(mc.gameMode!=null&&mc.gameMode.canHurtPlayer()?76:38);
        if(frame.caption()!=0&&(hideCaptions&(1<<(frame.caption()-1)))==0)g.drawCenteredString(mc.font,Component.translatable((kind==3?"scene.whileaway.seven.":kind==2?"scene.whileaway.city.":"scene.whileaway.signal.")+frame.caption()),w/2,captionY,0xd9d1bc);
    }
}
