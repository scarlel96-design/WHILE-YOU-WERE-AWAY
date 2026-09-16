package io.github.whileaway.client;

import io.github.whileaway.*;
import io.github.whileaway.entity.StoryNpc;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated opt-in fault injector. Never moves the NPC or modifies its event facts. */
@EventBusSubscriber(modid=WhileAway.ID,value=Dist.CLIENT)
public final class NpcExceptionSmoke {
    private static final String MODE=System.getProperty("whileaway.npcException","");
    private static final Path E=Path.of(System.getProperty("whileaway.evidence","evidence/npc-exceptions/client"));
    private static final BlockPos REMOTE=new BlockPos(8192,150,8192);
    private static boolean launched,setup,dead,stopped;private static volatile boolean queued;
    private static int ticks,phase,wait,stable,beforeCheckpoint;
    private static UUID actorId,instance;private static int generation;
    private static net.minecraft.world.phys.Vec3 movementStart;
    private static Set<String> facts;private static String work,dimension;private static BlockPos residence;
    private static boolean keep(){return MODE.endsWith("On");}
    private static StoryActorRecord record(MinecraftServer s){var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);check(r!=null,"NPC ledger lost");return r;}
    private static void check(boolean ok,String s){if(!ok)throw new IllegalStateException(s);}
    private static void note(String s){try{Files.createDirectories(E);Files.writeString(E.resolve(MODE+".txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static void fail(Exception e){note("FAIL "+e);Runtime.getRuntime().halt(8);}
    private static void remember(StoryActorRecord r){actorId=r.entityId;instance=r.instanceId;generation=r.generation;facts=Set.copyOf(r.facts);beforeCheckpoint=r.checkpoint.checkpoint;work=NpcState.work(r);residence=NpcState.home(r);dimension=r.dimension;}
    private static void identity(StoryActorRecord r){check(r.entityId.equals(actorId)&&r.instanceId.equals(instance)&&r.generation==generation&&r.dimension.equals(dimension)&&NpcState.home(r).equals(residence),"identity/residence changed");}
    private static void saved(MinecraftServer s,String suffix) throws Exception {
        s.saveEverything(true,true,true);Files.copy(s.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat"),E.resolve(MODE+suffix+".dat"),StandardCopyOption.REPLACE_EXISTING);
    }
    private static void back(ServerPlayer p){var l=p.getServer().getLevel(CityDistrict.KEY);p.setGameMode(GameType.CREATIVE);p.teleportTo(l,CityDistrict.ARRIVAL.getX()+.5,CityDistrict.ARRIVAL.getY(),CityDistrict.ARRIVAL.getZ()+.5,Set.of(),0,0);}
    @SubscribeEvent public static void boundary(RecoveryBoundaryEvent e){
        if(!MODE.startsWith("Death")||dead||!e.eventId.equals(NpcEvents.ID))return;
        int target=Character.digit(MODE.charAt(5),10)-1;
        if(e.checkpoint!=target||!e.boundary.startsWith("NPC_"))return;
        try {
            var p=e.player;var r=record(p.getServer());
            if(target==2){var npc=StoryActors.find(p.getServer(),r.entityId);check(npc!=null,"moving NPC absent");movementStart=npc.position();return;}
            injectDeath(p,e.boundary);
        }catch(Exception x){fail(x);}
    }
    private static void injectDeath(ServerPlayer p,String boundary) throws Exception {
            var r=record(p.getServer());remember(r);saved(p.getServer(),"-before");
            p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().add(new ItemStack(Items.DIAMOND,7));
            p.getServer().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(keep(),p.getServer());
            dead=true;p.kill();check(!p.isAlive(),"actual player death not observed");
            note("DEATH boundary="+boundary+" checkpoint="+beforeCheckpoint+" keepInventory="+keep()+" uuid="+actorId+" generation="+generation);phase=1;wait=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(MODE.isEmpty()||stopped)return;var mc=Minecraft.getInstance();
        try {
            if(!launched&&mc.screen instanceof TitleScreen){
                launched=true;Files.createDirectories(E);note(ClientTestEnvironment.verify(E,MODE));
                String source=MODE.startsWith("Reload")?"Death5"+(keep()?"On":"Off"):MODE;
                mc.createWorldOpenFlows().openWorld("exception-"+source,mc::stop);
            }
            if(mc.screen instanceof BackupConfirmScreen screen)for(var c:screen.children())if(c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString())){b.onPress();break;}
            if(++ticks>16000)throw new IllegalStateException("NPC_EXCEPTION_TIMEOUT phase="+phase);
            if(mc.player==null||mc.getSingleplayerServer()==null||queued)return;
            if(mc.screen instanceof DeathScreen){mc.player.respawn();return;}
            if(ticks%5!=0)return;queued=true;var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{try {
                var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayer(id);if(p==null)return;
                if(!setup){
                    setup=true;check(CityDistrict.get(s.getLevel(CityDistrict.KEY)).ready(),"fixture city missing");
                    if(MODE.startsWith("Reload")){remember(record(s));check(beforeCheckpoint==4,"completed record not restored");back(p);phase=2;}
                    else {
                        check(!NarrativeData.get(s).actors.containsKey(NpcEvents.STORY),"seed already has NPC");
                        var l=s.overworld();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)l.setBlock(REMOTE.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                        p.setRespawnPosition(Level.OVERWORLD,REMOTE,0,true,false);back(p);
                    }
                }
                var r=NarrativeData.get(s).actors.get(NpcEvents.STORY);if(r==null)return;
                if(phase==1){
                    if(!p.isAlive())return;
                    check(p.level().dimension().equals(Level.OVERWORLD)&&p.blockPosition().distSqr(REMOTE)<100,"not a remote Overworld respawn");
                    identity(r);check(r.checkpoint.checkpoint==beforeCheckpoint,"death advanced checkpoint "+beforeCheckpoint+" -> "+r.checkpoint.checkpoint);
                    check(r.facts.equals(facts)&&NpcState.work(r).equals(work),"absence changed permanent facts/current work");
                    if((wait+=5)<120)return;
                    check(p.getInventory().countItem(Items.DIAMOND)==(keep()?7:0),"actual keepInventory result wrong");
                    check(r.lifecycle==(beforeCheckpoint==0?StoryActorRecord.Lifecycle.PREPARED:beforeCheckpoint==4?StoryActorRecord.Lifecycle.ACTIVE:StoryActorRecord.Lifecycle.SUSPENDED),"wrong absent lifecycle "+r.lifecycle);
                    saved(s,"-away");note("PASS remote_respawn checkpoint="+beforeCheckpoint+" heldTicks="+wait+" same identity/facts; keepInventory="+keep()+" diamonds="+p.getInventory().countItem(Items.DIAMOND));
                    back(p);phase=2;return;
                }
                var entity=StoryActors.find(s,r.entityId);if(!(entity instanceof StoryNpc npc))return;
                if(r.checkpoint.checkpoint==1)npc.mobInteract(p,InteractionHand.MAIN_HAND);
                if(!dead&&movementStart!=null&&r.checkpoint.checkpoint==2&&npc.position().distanceToSqr(movementStart)>.25){
                    note("PASS moving_boundary actualMovement=true distanceSquared="+npc.position().distanceToSqr(movementStart));
                    injectDeath(p,"NPC_C_ACTUAL_MOVEMENT");return;
                }
                if(phase!=2||r.checkpoint.checkpoint<4)return;
                identity(r);check(facts.stream().filter(f->!f.startsWith("npc.work=")).allMatch(r.facts::contains),"lost permanent receipt");
                check(NpcState.integrity(r).isEmpty()&&StoryActors.integrity(s).isEmpty(),"post-return integrity");
                check(ActorCandidates.inspect(s,r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"canonical not unique");
                check(r.lifecycle==StoryActorRecord.Lifecycle.ACTIVE&&NpcState.work(r).equals("maintain_return_light"),"resident life missing");
                check(npc.distanceToSqr(residence.getX()+.5,residence.getY(),residence.getZ()+.5)<=1.4,"resident not at actual home");
                var before=Set.copyOf(r.facts);check(!NpcEvents.interact(npc,p)&&before.equals(r.facts),"repeat interaction changed history");
                if((stable+=5)<100)return;
                saved(s,"");note("PASS "+MODE+" canonical=1 checkpoint=4 generation="+generation+" uuid="+actorId+" instance="+instance+" dimension="+dimension+" work="+NpcState.work(r)+" stableTicks="+stable);
                stopped=true;s.halt(false);mc.execute(mc::stop);
            }catch(Exception x){fail(x);}finally{queued=false;}});
        }catch(Exception x){fail(x);}
    }
}
