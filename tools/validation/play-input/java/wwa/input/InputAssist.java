package wwa.input;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated input and optional damage protection. Campaign data is not modified. */
@Mod(value="wwa_play_input",dist=Dist.CLIENT)
public final class InputAssist {
    private final Set<String> seen=new HashSet<>();
    private Set<String> held=Set.of();
    private String dimension="",id="";
    private int remaining;
    private long deadline;
    private boolean pauseAfter;
    private final ArrayDeque<JsonObject> pending=new ArrayDeque<>();
    public InputAssist(){
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::protectPlayer);
    }
    private void protectPlayer(LivingIncomingDamageEvent event){
        if(!Boolean.getBoolean("whileaway.inputAssist")
            ||!Boolean.getBoolean("whileaway.testPlayerProtection"))return;
        if(!(event.getEntity() instanceof ServerPlayer player)||player.server.isDedicatedServer())return;
        String configured=System.getProperty("whileaway.testPlayerProtectionRoot");
        if(configured==null)return;
        Path root=Path.of(configured).toAbsolutePath().normalize();
        Path world=player.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if(!world.startsWith(root))return;
        event.setCanceled(true);
        try{log(root.getParent().resolve("input-assist"),"DAMAGE_BLOCKED source="
            +event.getSource().getMsgId()+" amount="+event.getAmount()+" health="+player.getHealth());}
        catch(Exception ignored){}
    }
    private Map<String,KeyMapping> keys(Minecraft m){return Map.of(
        "forward",m.options.keyUp,"back",m.options.keyDown,"left",m.options.keyLeft,"right",m.options.keyRight,
        "jump",m.options.keyJump,"sprint",m.options.keySprint,"sneak",m.options.keyShift,
        "attack",m.options.keyAttack,"use",m.options.keyUse);}
    private void release(Minecraft m){keys(m).values().forEach(k->k.setDown(false));held=Set.of();remaining=0;}
    private void log(Path dir,String text)throws Exception{Files.writeString(dir.resolve("receipts.log"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private void snapshot(Minecraft m,Path dir,String name){Screenshot.grab(dir.toFile(),name+".png",m.getMainRenderTarget(),c->{});}
    private void tick(ClientTickEvent.Pre event){
        if(!Boolean.getBoolean("whileaway.inputAssist"))return;
        var m=Minecraft.getInstance();var dir=m.gameDirectory.toPath().resolve("input-assist");
        try{
            Files.createDirectories(dir);
            String current=m.level==null?"":m.level.dimension().location().toString();
            boolean available=m.player!=null&&m.player.isAlive()&&m.screen==null&&m.isWindowActive();
            if(!available||!current.equals(dimension)){
                release(m);pending.clear();pauseAfter=false;dimension=current;
            }
            Path command=dir.resolve("command.json");
            if(Files.exists(command)){
                var request=JsonParser.parseString(Files.readString(command)).getAsJsonObject();
                // Keep explicit input queued only behind the normal pause menu.
                if(!available&&m.screen instanceof PauseScreen
                    &&(request.has("actions")||request.has("turnX")))return;
                Files.delete(command);
                String next=request.get("id").getAsString();
                if(!next.matches("[a-zA-Z0-9_-]{1,60}"))throw new IllegalArgumentException("invalid id");
                if(seen.add(next)){
                    id=next;log(dir,"ACCEPT "+id+" screen="+(m.screen==null?"GAME":m.screen.getClass().getSimpleName()));
                    if(request.has("screenshot"))snapshot(m,dir,id);
                    if(request.has("release")){release(m);pending.clear();}
                    if(available){
                        pauseAfter=request.has("pauseAfter")&&request.get("pauseAfter").getAsBoolean();
                        if(request.has("turnX"))m.player.turn(Math.clamp(request.get("turnX").getAsDouble(),-600,600),Math.clamp(request.get("turnY").getAsDouble(),-600,600));
                        if(request.has("actions")){
                            var actions=request.getAsJsonArray("actions");
                            if(actions.size()>2)throw new IllegalArgumentException("maximum two short leases");
                            pending.clear();for(var a:actions)pending.add(a.getAsJsonObject());
                        }
                    }
                }
            }
            if(!available)return;
            if(remaining<=0||System.nanoTime()>deadline){
                if(!pending.isEmpty()){
                    var a=pending.removeFirst();Set<String> next=new HashSet<>();
                    for(var k:a.getAsJsonArray("keys")){String key=k.getAsString();if(!keys(m).containsKey(key))throw new IllegalArgumentException("unsupported key");next.add(key);}
                    if((next.contains("attack")||next.contains("use"))&&!m.mouseHandler.isMouseGrabbed()){
                        m.mouseHandler.grabMouse();log(dir,"MOUSE_GRAB "+id);
                    }
                    for(var k:keys(m).entrySet())k.getValue().setDown(next.contains(k.getKey()));
                    for(var key:next)if(!held.contains(key))KeyMapping.click(keys(m).get(key).getKey());
                    held=Set.copyOf(next);remaining=Math.clamp(a.get("ticks").getAsInt(),1,40);deadline=System.nanoTime()+3_000_000_000L;
                    log(dir,"LEASE "+id+" keys="+held+" ticks="+remaining);
                }else if(!held.isEmpty()){
                    release(m);snapshot(m,dir,id+"-end");log(dir,"RELEASE "+id);
                    if(pauseAfter){pauseAfter=false;m.pauseGame(false);log(dir,"PAUSED "+id);}
                }
            }
            if(remaining>0){
                for(var key:held){
                    var mapping=keys(m).get(key);
                    if(!mapping.isDown())log(dir,"REASSERT "+id+" key="+key);
                    mapping.setDown(true);
                }
                remaining--;
            }
        }catch(Exception ex){release(m);pending.clear();pauseAfter=false;try{log(dir,"ERROR "+ex);}catch(Exception ignored){}}
    }
}
