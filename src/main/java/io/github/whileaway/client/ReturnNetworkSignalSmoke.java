package io.github.whileaway.client;
import io.github.whileaway.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in observer/fault injector. Never installed as campaign state. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ReturnNetworkSignalSmoke {
 private static final boolean ENABLED=Boolean.getBoolean("whileaway.signalSmoke");
 private static final String MODE=System.getProperty("whileaway.networkSmoke","");
 private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/return-network-signal/client"));
 private static volatile long captureUntil;private static int frames,clientTicks,hold;private static boolean oldHeld,responseHeld;
 private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+"-signal.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception x){throw new IllegalStateException(x);}}
 @SubscribeEvent public static void boundary(RecoveryBoundaryEvent e){
  if(!ENABLED||!e.eventId.equals(ReturnNetworkState.ID))return;
  var l=e.player.serverLevel();var play=ReturnNetworkEvents.signal(l);
  note("BOUNDARY "+e.boundary+" tick="+l.getGameTime()+" cp="+e.checkpoint+" session="+(play==null?"none":play.id())+" emitted="+(play==null?0:play.emitted()));
  if(e.boundary.startsWith("RN_SIGNAL_PREPARED")&&MODE.equals("Normal")) {
   // Fixture camera only; production never changes camera.
   e.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,net.minecraft.world.phys.Vec3.atCenterOf(ReturnNetworkEvents.RELAY).add(0,1,0));
   captureUntil=System.nanoTime()+8_000_000_000L;
  }
  String target=switch(MODE){case "CutBefore"->"RN_SIGNAL_PREPARED_RESPONSE";case "CutGap1"->"RN_SIGNAL_OFF_RESPONSE_1";case "CutGap2"->"RN_SIGNAL_OFF_RESPONSE_2";case "CutLong"->"RN_SIGNAL_ON_RESPONSE_2";case "CutEnd"->"RN_SIGNAL_OFF_RESPONSE_3";default->"";};
  if(!target.equals(e.boundary))return;
  try {
   var server=l.getServer();var d=NarrativeData.get(server);var r=d.actors.get(NpcEvents.STORY);
   var file=server.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");var disk=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getCompound("data");
   if(!disk.getCompound("returnNetwork").equals(d.returnNetwork().save())||e.checkpoint!=4)throw new IllegalStateException("signal cut disk/state mismatch");
   Files.copy(file,E.resolve(MODE+".dat"));
   String pass="PASS "+MODE+" boundary="+target+" cp=4 uuid="+r.entityId+" instance="+r.instanceId+" generation="+r.generation+" worldFlushBeforeCut=false";
   Files.writeString(E.resolve(MODE+".txt"),pass+"\n",StandardOpenOption.APPEND);note(pass);Runtime.getRuntime().halt(0);
  }catch(Exception x){note("FAIL "+x);Runtime.getRuntime().halt(8);}
 }
 public static boolean step(ServerLevel l,ReturnNetworkState s){
  if(!ENABLED)return false;var p=ReturnNetworkEvents.signal(l);
  boolean eligible=s.stage.ordinal()==2&&!oldHeld||s.stage.ordinal()==4&&!responseHeld;
  if(eligible&&p!=null&&p.complete()){
   hold+=10;if(hold<40)return true;
   note("PASS emitted_without_observation cp="+s.stage.ordinal()+" heldTicks="+hold+" session="+p.id());hold=0;
   if(s.stage.ordinal()==2)oldHeld=true;else responseHeld=true;
  }
  return false;
 }
 @SubscribeEvent public static void capture(ClientTickEvent.Post e){
  if(!ENABLED||!MODE.equals("Normal")||System.nanoTime()>captureUntil||++clientTicks%5!=0)return;
  var mc=Minecraft.getInstance();if(mc.level==null)return;
  try {Files.createDirectories(E);Screenshot.grab(E.toFile(),String.format("signal-%03d.png",frames++),mc.getMainRenderTarget(),m->{});}catch(Exception x){note("CAPTURE_FAIL "+x);}
 }
}
