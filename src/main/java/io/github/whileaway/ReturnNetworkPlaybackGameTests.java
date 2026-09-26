package io.github.whileaway;
import io.github.whileaway.support.ReturnNetworkPlayback;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(WhileAway.ID) @PrefixGameTestTemplate(false)
public final class ReturnNetworkPlaybackGameTests {
 @GameTest(template="empty") public static void playbackExactDurationsAndGaps(GameTestHelper h){
  var p=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,0);int[] seen=new int[3];int on=0,off=0;
  for(int t=1;t<=103;t++){var f=p.tick(t,true);if(f.pulse()>=0)seen[f.pulse()]++;if(f.onset())on++;if(f.offset())off++;}
  h.assertTrue(java.util.Arrays.equals(seen,new int[]{12,12,36})&&on==3&&off==3&&p.complete(),"exact visible durations, three on/off edges");h.succeed();
 }
 @GameTest(template="empty") public static void playbackSkippedTickRestartsWithoutBurst(GameTestHelper h){
  var p=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,0);p.tick(1,true);var f=p.tick(40,true);
  h.assertTrue(p.interrupted()&&p.emitted()==0&&!f.onset()&&!p.complete(),"no catch-up on missed tick");h.succeed();
 }
 @GameTest(template="empty") public static void playbackReadinessLossAtEveryBoundary(GameTestHelper h){
  for(int cut:new int[]{1,10,22,38,50,66,80,102}){var p=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,0);for(int t=1;t<cut;t++)p.tick(t,true);p.tick(cut,false);p.tick(cut+1,true);h.assertTrue(p.interrupted()&&!p.complete(),"availability loss cannot resume partial sequence");}h.succeed();
 }
 @GameTest(template="empty") public static void playbackFreshSessionAfterInterruption(GameTestHelper h){
  var old=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,0);for(int t=1;t<50;t++)old.tick(t,true);old.tick(50,false);
  var fresh=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,50);for(int t=51;t<=152;t++)fresh.tick(t,true);
  h.assertTrue(!old.id().equals(fresh.id())&&old.interrupted()&&fresh.complete()&&fresh.emitted()==3,"no cross-session progress");h.succeed();
 }
 @GameTest(template="empty") public static void playbackCarrierFailureNeverConfirms(GameTestHelper h){
  var p=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.OLD_PATTERN,0);for(int t=1;t<=102;t++)p.tick(t,true);p.carrierFailed();
  h.assertTrue(!p.complete()&&p.interrupted(),"failed carrier revokes transient confirmation only");h.succeed();
 }
 @GameTest(template="empty") public static void playbackCompletedNoAutomaticReplay(GameTestHelper h){
  var p=new ReturnNetworkPlayback(ReturnNetworkPlayback.Purpose.RESPONSE,0);for(int t=1;t<=102;t++)p.tick(t,true);
  for(int t=103;t<=302;t++){var f=p.tick(t,true);h.assertTrue(!f.onset()&&f.pulse()==-1&&p.complete(),"completed frame remains quiet");}
  h.assertTrue(p.emitted()==3,"no duplicate emit");h.succeed();
 }
}
