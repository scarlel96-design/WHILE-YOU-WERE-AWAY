package io.github.whileaway.client;

import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

/** Opt-in fixture gate. Observes only this client's window and audio configuration. */
public final class ClientTestEnvironment {
    private ClientTestEnvironment() {}
    public static String verify(Path evidence, String task) throws java.io.IOException {
        var mc=Minecraft.getInstance();
        Files.createDirectories(evidence);
        Files.writeString(evidence.resolve(task+".pid"),Long.toString(ProcessHandle.current().pid()));
        for(var source:SoundSource.values())
            if(mc.options.getSoundSourceVolume(source)!=0)throw new IllegalStateException("client not muted: "+source);
        int[] x={0},y={0},w={0},h={0};
        GLFW.glfwGetWindowPos(mc.getWindow().getWindow(),x,y);
        GLFW.glfwGetWindowSize(mc.getWindow().getWindow(),w,h);
        int left=Integer.getInteger("whileaway.monitorLeft"),top=Integer.getInteger("whileaway.monitorTop");
        int right=left+Integer.getInteger("whileaway.monitorWidth"),bottom=top+Integer.getInteger("whileaway.monitorHeight");
        if(x[0]<left||y[0]<top||x[0]+w[0]>right||y[0]+h[0]>bottom)
            throw new IllegalStateException("window outside requested right monitor actual="+x[0]+","+y[0]+" size="+w[0]+"x"+h[0]+" expected="+left+","+top+".."+right+","+bottom);
        return "PASS monitor DISPLAY1 right-secondary actual="+x[0]+","+y[0]+" size="+w[0]+"x"+h[0]+"; all sound categories=0; pid="+ProcessHandle.current().pid();
    }
}
