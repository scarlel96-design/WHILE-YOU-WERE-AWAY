package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in integrated-client harness. Uses real NPC pathing and real block-use dispatch.
 * Teleport/setup are fixtures, not natural-survival evidence. Cuts do NOT flush world storage. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class ReturnNetworkSmoke {
    private static final String MODE=System.getProperty("whileaway.networkSmoke","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/return-network-integration/client"));
    private static boolean launched,setup,stopped;private static volatile boolean queued;
    private static int ticks,stable,entryCheckpoint;private static UUID entity,instance;private static int generation;
    private static Set<String> initialFacts;
    private static int exceptionPhase,held;private static CompoundTag frozen;private static net.minecraft.core.BlockPos oldNpcPos;
    private static net.minecraft.world.level.block.state.BlockState obstacleBefore;private static byte[] corruptOriginal;
    private static String world(){return System.getProperty("whileaway.networkWorld","network");}
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");}
    private static void fail(Exception e){note("FAIL "+e);e.printStackTrace();Runtime.getRuntime().halt(8);}
    private static void snapshot(MinecraftServer server)throws Exception {
        Files.copy(file(server),E.resolve(MODE+".dat"),StandardCopyOption.REPLACE_EXISTING);
        note("DISK sha256="+StoryStorage.digest(Files.readAllBytes(file(server))));
    }
    private static void finish(MinecraftServer s)throws Exception {stopped=true;s.saveEverything(true,true,true);snapshot(s);s.halt(false);Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());}
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent e) {
        if(!MODE.startsWith("Cut")||!e.eventId.equals(ReturnNetworkState.ID))return;
        String target=switch(MODE){case "CutA"->"RN_A_DISCOVERED";case "CutB"->"RN_B_NPC_JOINED";case "CutC"->"RN_C_PATTERN";case "CutD"->"RN_D_INTERVENTION";case "CutE"->"RN_E_RESPONSE";case "CutF"->"RN_F_SHARED";case "CutGPre"->"RN_G_BEFORE_COMPLETE";case "CutG"->"RN_G_COMPLETED";default->"";};
        if(!target.equals(e.boundary))return;
        try {
            var s=e.player.getServer();var d=NarrativeData.get(s);var r=d.actors.get(NpcEvents.STORY);var state=d.returnNetwork();
            check(ReturnNetworkIntegrity.issues(state,d.actors).isEmpty(),"cross-system integrity at cut");
            var disk=NbtIo.readCompressed(file(s),NbtAccounter.unlimitedHeap()).getCompound("data");
            check(disk.getCompound("returnNetwork").equals(state.save()),"commit not on disk");
            snapshot(s);note("PASS "+MODE+" boundary="+e.boundary+" cp="+state.stage.ordinal()+" uuid="+r.entityId+" instance="+r.instanceId+" generation="+r.generation+" facts="+state.facts+" worldFlushBeforeCut=false");
            Runtime.getRuntime().halt(0);
        }catch(Exception x){fail(x);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen) {
                launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                if(MODE.startsWith("Corrupt")) {
                    var f=mc.gameDirectory.toPath().resolve("saves").resolve(world()).resolve("data/whileaway_story.dat");
                    check(world().equals("world-"+MODE),"corruption restricted to named copied fixture");
                    var root=NbtIo.readCompressed(f,NbtAccounter.unlimitedHeap());var event=root.getCompound("data").getCompound("returnNetwork");
                    check(event.getInt("checkpoint")==6,"corruption requires completed copy");
                    if(MODE.equals("CorruptBinding"))event.getCompound("participant").remove("entity");
                    else root.getCompound("data").getList("actors",10).getCompound(0).getList("facts",8).removeIf(t->t.getAsString().equals(ReturnNetworkIntegrity.SHARED));
                    NbtIo.writeCompressed(root,f);corruptOriginal=Files.readAllBytes(f);Files.write(E.resolve(MODE+"-input.dat"),corruptOriginal);
                }
                if(MODE.equals("Seed")) {
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel("network",new LevelSettings("Return network fixture",GameType.SPECTATOR,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(74192361L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());
                }else mc.createWorldOpenFlows().openWorld(System.getProperty("whileaway.networkWorld","network"),mc::stop);
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(mc.screen instanceof DeathScreen&&mc.player!=null){mc.player.respawn();return;}
            if(++ticks>18000)throw new IllegalStateException("network timeout "+MODE);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued||ticks%10!=0)return;queued=true;
            mc.getSingleplayerServer().execute(()->{try {
                var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());if(p==null)return;
                var city=server.getLevel(CityDistrict.KEY);check(city!=null,"city absent");
                if(MODE.startsWith("Corrupt")) {
                    check(!StoryStorage.available(server),"corruption was accepted");
                    var status=StoryStorage.status(server.overworld().getDataStorage());
                    check(status.outcome()==StoryStorage.Outcome.CORRUPT&&!status.writable(),"wrong corruption classification");
                    if((held+=10)<100)return;
                    for(int n=0;n<3;n++)server.saveEverything(true,true,true);
                    check(Arrays.equals(corruptOriginal,Files.readAllBytes(file(server))),"corrupt autosave overwrite");
                    var quarantine=file(server).getParent().resolve("whileaway-quarantine/"+StoryStorage.digest(corruptOriginal)+".dat");
                    check(Arrays.equals(corruptOriginal,Files.readAllBytes(quarantine)),"corrupt original not preserved");
                    note("PASS "+MODE+" saveDispatches=3 writeBlocked=true emptyCampaign=false originalHash="+StoryStorage.digest(corruptOriginal)+" diagnostic="+status.diagnostic());finish(server);return;
                }
                if(!setup){setup=true;if(MODE.equals("Seed"))CityDistrict.get(city).start();else check(CityDistrict.get(city).ready(),"seed not ready");}
                if(!CityDistrict.get(city).ready())return;
                var d=NarrativeData.get(server);var r=d.actors.get(NpcEvents.STORY);
                if(MODE.equals("Seed")) {
                    if(p.serverLevel()!=city){p.setGameMode(GameType.CREATIVE);p.teleportTo(city,40.5,65,68.5,Set.of(),0,0);}
                    if(r==null)return;
                    var actor=StoryActors.find(server,r.entityId);if(!(actor instanceof StoryNpc npc))return;
                    if(r.checkpoint.checkpoint==1)npc.mobInteract(p,InteractionHand.MAIN_HAND);
                    if(r.checkpoint.checkpoint!=4)return;
                    check(ReturnNetworkIntegrity.prerequisite(r),"actual prerequisite absent");
                    check(ReturnNetworkEvents.originalEquipment(city),"revision2 equipment mismatch");
                    check(ReturnNetworkEvents.environmentReady(city,d.returnNetwork()),"revision2 access mismatch");
                    note("PASS Seed actual Yeoul event completed via interaction and path; uuid="+r.entityId+" instance="+r.instanceId+" generation="+r.generation);finish(server);return;
                }
                check(r!=null,"resident lost");
                if(entity==null) {
                    entity=r.entityId;instance=r.instanceId;generation=r.generation;initialFacts=Set.copyOf(r.facts);entryCheckpoint=d.returnNetwork().stage.ordinal();
                    note("INPUT cp="+entryCheckpoint+" uuid="+entity+" instance="+instance+" generation="+generation+" load="+StoryStorage.status(server.overworld().getDataStorage()));
                    p.setGameMode(GameType.CREATIVE);p.teleportTo(city,45.5,65,47.5,Set.of(),90,0);
                }
                var s=d.returnNetwork();
                check(s.stage.ordinal()>=entryCheckpoint,"checkpoint rollback");
                check(entity.equals(r.entityId)&&instance.equals(r.instanceId)&&generation==r.generation,"resident identity changed");
                check(r.facts.containsAll(initialFacts),"resident facts lost");
                check(ReturnNetworkIntegrity.issues(s,d.actors).isEmpty(),"persistent integrity");
                if(MODE.equals("CopyHold")||MODE.equals("CopyReopen")) {
                    check(entryCheckpoint==3&&s.stage.ordinal()==3,"independent copy progressed");
                    if((held+=10)<100)return;
                    note("PASS "+MODE+" cp=3 unchanged while original completed; generation="+generation+" uuid="+entity+" instance="+instance);finish(server);return;
                }
                if(exception(server,p,city,s,r))return;
                var actual=StoryActors.find(server,r.entityId);if(!(actual instanceof StoryNpc npc))return;
                if(ticks%200==0)note("PROGRESS cp="+s.stage.ordinal()+" npc="+npc.position()+" player="+p.position()+" wait="+ReturnNetworkEvents.waitState(city)+" path="+npc.storyNavigation().state()+" attempts="+npc.storyNavigation().attempts()+" followRange="+npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
                check(StoryActors.canonical(npc,r),"live participant not canonical");
                if(s.stage.ordinal()>=2&&s.stage.ordinal()<=5) {
                    p.setShiftKeyDown(s.stage.ordinal()>=3);
                    p.gameMode.useItemOn(p,city,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ReturnNetworkEvents.RELAY),Direction.EAST,ReturnNetworkEvents.RELAY,false));
                    p.setShiftKeyDown(false);
                }
                if(s.stage==ReturnNetworkState.Stage.RESPONSE_OBSERVED)npc.mobInteract(p,InteractionHand.MAIN_HAND);
                if(s.stage!=ReturnNetworkState.Stage.COMPLETED)return;
                check(!MODE.startsWith("Cut"),"expected cut was not reached");
                if(!ReturnNetworkEvents.activatedEquipment(city)) {
                    p.setShiftKeyDown(true);p.gameMode.useItemOn(p,city,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ReturnNetworkEvents.RELAY),Direction.EAST,ReturnNetworkEvents.RELAY,false));p.setShiftKeyDown(false);return;
                }
                check(ActorCandidates.inspect(server,r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical count not one");
                check(ReturnNetworkEvents.integrity(server).isEmpty(),"world integrity");
                check(r.facts.contains(ReturnNetworkIntegrity.SHARED)&&r.facts.contains(ReturnNetworkIntegrity.AFTERMATH),"NPC aftermath lost");
                stable+=10;if(stable<160)return;
                note("PASS "+MODE+" cp=6 canonical=1 generation="+generation+" uuid="+entity+" instance="+instance+" stableTicks="+stable+" aftermath=true facts="+s.facts);finish(server);
            }catch(Exception x){fail(x);}finally{queued=false;}});
        }catch(Exception x){fail(x);}
    }
    private static boolean exception(MinecraftServer server,net.minecraft.server.level.ServerPlayer p,net.minecraft.server.level.ServerLevel city,ReturnNetworkState s,StoryActorRecord r)throws Exception {
        boolean travel=Set.of("Nether","End","Chunk").contains(MODE),death=MODE.startsWith("Death"),env=MODE.equals("Environment");
        if(!travel&&!death&&!env)return false;
        int target=death?Character.digit(MODE.charAt(5),10):env?3:4;
        if(exceptionPhase==0&&s.stage.ordinal()==target) {
            frozen=s.save();oldNpcPos=r.position;held=0;exceptionPhase=1;
            if(env) {
                obstacleBefore=city.getBlockState(ReturnNetworkEvents.STAND);
                city.setBlock(ReturnNetworkEvents.STAND,net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(),3);
            }else if(death) {
                p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
                server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(MODE.endsWith("On"),server);
                p.setRespawnPosition(Level.OVERWORLD,server.overworld().getSharedSpawnPos(),0,true,false);
                p.kill();check(!p.isAlive(),"death did not occur");note("DEATH cp="+target+" keep="+MODE.endsWith("On"));
            }else {
                var away=MODE.equals("Nether")?server.getLevel(Level.NETHER):MODE.equals("End")?server.getLevel(Level.END):city;
                p.setGameMode(GameType.SPECTATOR);p.teleportTo(away,4096.5,160,4096.5,Set.of(),0,0);
            }
            return true;
        }
        if(exceptionPhase==1) {
            check(frozen.equals(s.save()),"exception changed durable event");
            if(death&&!p.isAlive())return true;
            if(MODE.equals("Chunk")) {
                boolean unloaded=!city.hasChunkAt(oldNpcPos)&&!city.areEntitiesLoaded(new ChunkPos(oldNpcPos).toLong())
                    &&!city.hasChunkAt(ReturnNetworkEvents.RELAY)&&!city.areEntitiesLoaded(new ChunkPos(ReturnNetworkEvents.RELAY).toLong())
                    &&!city.hasChunkAt(r.position)&&!city.areEntitiesLoaded(new ChunkPos(r.position).toLong())&&StoryActors.find(server,r.entityId)==null;
                if(!unloaded){held=0;return true;}
            }
            if((held+=10)<120)return true;
            if(death)check(p.serverLevel()==server.overworld()&&p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)==(MODE.endsWith("On")?7:0),"respawn/keepInventory result");
            if(env) {
                check(city.getBlockState(ReturnNetworkEvents.STAND).is(net.minecraft.world.level.block.Blocks.BARRIER),"production deleted player obstruction");
                check(ReturnNetworkEvents.waitState(city)==ReturnNetworkState.Wait.WAITING_ENVIRONMENT,"environment not suspended");
                city.setBlock(ReturnNetworkEvents.STAND,obstacleBefore,3);
                obstacleBefore=city.getBlockState(ReturnNetworkEvents.RELAY);city.setBlock(ReturnNetworkEvents.RELAY,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                exceptionPhase=2;held=0;return true;
            }
            note("PASS exception "+MODE+" heldTicks="+held+" cp="+s.stage.ordinal()+" unchanged=true actualUnloaded="+MODE.equals("Chunk"));
            p.setGameMode(GameType.CREATIVE);p.teleportTo(city,45.5,65,47.5,Set.of(),90,0);exceptionPhase=3;return true;
        }
        if(env&&exceptionPhase==2) {
            check(frozen.equals(s.save())&&city.getBlockState(ReturnNetworkEvents.RELAY).isAir(),"missing equipment advanced or was overwritten");
            if((held+=10)<120)return true;
            city.setBlock(ReturnNetworkEvents.RELAY,obstacleBefore,3);
            // The obstruction can push the scripted player out of interaction reach. Reposition only the test player.
            p.teleportTo(city,45.5,65,47.5,Set.of(),90,0);exceptionPhase=3;
            note("PASS exception Environment obstruction and missing equipment each held120 ticks; fixture restored; identity unchanged");return true;
        }
        return false;
    }
}

