package io.github.whileaway;

import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Missing binding recovery; deliberately never creates an entity or advances an epoch. */
public final class CanonicalRecovery {
    private static final Map<MinecraftServer,Pending> pending=new WeakHashMap<>();
    private static final class Pending {
        NarrativeData draft;Map<String,UUID> tickets;String hash,last="",diagnostic="RECOVERY_PENDING";
        int lastTick=-1;boolean manual;
    }
    public static String diagnostic(MinecraftServer server){var p=pending.get(server);return p==null?"RECOVERY_NONE":p.diagnostic;}
    public static String attempt(MinecraftServer server) {
        if(StoryStorage.available(server))return "LOAD_OK";
        var state=StoryStorage.status(server.overworld().getDataStorage());
        var p=pending.computeIfAbsent(server,s->new Pending());
        if(p.manual)return p.diagnostic;
        try {
            if(state.outcome()!=StoryStorage.Outcome.CORRUPT||!state.detail().contains("field=entityUUID missing_or_malformed"))
                return manual(p,"RECOVERY_NOT_ELIGIBLE "+state.outcome());
            Path file=server.getWorldPath(LevelResource.ROOT).resolve("data/whileaway_story.dat");
            if(p.draft==null) {
                byte[] bytes=Files.readAllBytes(file);
                if(!StoryStorage.digest(bytes).equals(state.originalHash()))return manual(p,"RECOVERY_SOURCE_CHANGED");
                // Loader has already bounded and parsed this exact file. Only a missing field, not malformed data, is eligible.
                CompoundTag root;
                try(var input=new java.io.ByteArrayInputStream(bytes)) {
                    if(bytes.length>1&&(bytes[0]&255)==31&&(bytes[1]&255)==139)root=NbtIo.readCompressed(input,NbtAccounter.create(64*1024*1024)).getCompound("data");
                    else root=NbtIo.read(new java.io.DataInputStream(input),NbtAccounter.create(64*1024*1024)).getCompound("data");
                }
                var tickets=new TreeMap<String,UUID>();
                for(var raw:root.getList("actors",Tag.TAG_COMPOUND)) {
                    var actor=(CompoundTag)raw;if(actor.hasUUID("entityUUID"))continue;
                    if(actor.contains("entityUUID")||!actor.hasUUID("owner")||!actor.getString("ownerEventId").equals(StoryActors.CHASE))
                        return manual(p,"UUID_MALFORMED_OR_NO_SUPPORTED_EVENT_JOURNAL");
                    UUID ticket=null;
                    for(var item:root.getList("players",Tag.TAG_COMPOUND)) {
                        var player=(CompoundTag)item;
                        if(player.hasUUID("id")&&player.getUUID("id").equals(actor.getUUID("owner"))) {
                            if(!player.hasUUID("encounter")||player.getBoolean("complete"))return manual(p,"EVENT_JOURNAL_CONFLICT");
                            ticket=player.getUUID("encounter");
                        }
                    }
                    if(ticket==null||tickets.putIfAbsent(actor.getString("storyId"),ticket)!=null)return manual(p,"EVENT_JOURNAL_MISSING_OR_DUPLICATE");
                }
                if(tickets.isEmpty())return manual(p,"UUID_MISSING_NOT_FOUND");
                p.draft=StoryStorage.recoveryRead(server,tickets,true);p.tickets=tickets;p.hash=state.originalHash();
            }
            StringBuilder identity=new StringBuilder();
            for(var entry:p.tickets.entrySet()) {
                var record=p.draft.actors.get(entry.getKey());
                if(record==null||record.checkpoint.terminal())return manual(p,"EVENT_CHECKPOINT_CONFLICT");
                var candidates=ActorCandidates.inspect(server,record);
                p.diagnostic=candidates.outcome()+" "+candidates.detail()+" writeBlocked=true";
                if(candidates.outcome()==ActorCandidates.Outcome.AMBIGUOUS_MULTIPLE_CANDIDATES
                    ||candidates.outcome()==ActorCandidates.Outcome.IDENTITY_CONFLICT)return manual(p,p.diagnostic);
                if(candidates.outcome()!=ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE){p.last="";p.lastTick=-1;return p.diagnostic;}
                if(!entry.getValue().equals(candidates.exact().getUUID()))return manual(p,"IDENTITY_CONFLICT event_ticket_uuid");
                identity.append(entry.getKey()).append(':').append(candidates.exact().getUUID()).append(';');
            }
            String proof=identity.toString();int tick=server.getTickCount();
            if(!proof.equals(p.last)||tick<=p.lastTick){p.last=proof;p.lastTick=tick;return p.diagnostic="UNLOADED_OR_UNCONFIRMED awaiting_distinct_loaded_pass writeBlocked=true";}
            // Re-read original hash, run the same strict loader validator, quarantine, and commit on this server thread.
            if(!StoryStorage.digest(Files.readAllBytes(file)).equals(p.hash))return manual(p,"RECOVERY_SOURCE_CHANGED");
            StoryStorage.recoveryRead(server,p.tickets,false);
            p.diagnostic="UUID_RELINK_COMMITTED RECOVER_WITH_WARNING generation_unchanged=true "+proof;
            com.mojang.logging.LogUtils.getLogger().warn(p.diagnostic);
            return p.diagnostic;
        } catch(Exception ex){return manual(p,"RECOVERY_REJECTED "+ex.getMessage());}
    }
    private static String manual(Pending p,String reason){p.manual=true;p.diagnostic="MANUAL_DIAGNOSTIC "+reason+" writeBlocked=true";
        com.mojang.logging.LogUtils.getLogger().error(p.diagnostic);return p.diagnostic;}
    @SubscribeEvent public void tick(ServerTickEvent.Post event){var s=event.getServer();if(s.getTickCount()%20==0&&!StoryStorage.available(s))attempt(s);}
    @SubscribeEvent public void stopped(ServerStoppedEvent event){pending.remove(event.getServer());}
}
