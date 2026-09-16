package io.github.whileaway.client;

import io.github.whileaway.WhileAway;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Story music follows the listener; diegetic warning sounds remain positional. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ClientAudio {
    @SubscribeEvent public static void play(PlaySoundEvent event) {
        var s=event.getSound();
        if(s==null)return;
        if(ClientScene.active()&&s.getSource()==SoundSource.MUSIC&&!s.getLocation().getNamespace().equals(WhileAway.ID)) {event.setSound(null);return;}
        if(!s.getLocation().getNamespace().equals(WhileAway.ID))return;
        var path=s.getLocation().getPath();
        if(path.equals("homeward")||path.equals("station_unease")) {
            event.setSound(new SimpleSoundInstance(s.getLocation(),SoundSource.MUSIC,s.getVolume(),s.getPitch(),
                SoundInstance.createUnseededRandom(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true));
        }
    }
}
