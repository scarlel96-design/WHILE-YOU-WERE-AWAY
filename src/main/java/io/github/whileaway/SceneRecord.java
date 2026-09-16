package io.github.whileaway;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import io.github.whileaway.core.EventCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** These scenes own cosmetic actors only. Permanent inventory/relay/ledger facts remain in their authoritative stores. */
public final class SceneRecord {
    public final int kind;
    public final UUID id;
    public final BlockPos origin;
    public final EventCheckpoint progress=new EventCheckpoint();
    public SceneRecord(int kind,BlockPos origin){this(kind,UUID.randomUUID(),origin);}
    private SceneRecord(int kind,UUID id,BlockPos origin){this.kind=kind;this.id=id;this.origin=origin.immutable();}
    /** Migration/repair identity is content-derived, never clock, path, world name or RNG derived.
     * Live event creation still gets a fresh instance ID. World copies intentionally keep their IDs:
     * the containing SavedData, not a process-global UUID index, owns their state. */
    public static SceneRecord migrated(int kind,BlockPos origin,UUID player) {
        String key="whileaway:scene:v1:"+player+":"+kind+":"+origin.asLong();
        return new SceneRecord(kind,UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),origin);
    }
    public String eventId(){return switch(kind){case 1->"signal_wake";case 2->"city_arrival";default->"seven_departures";};}
    public String dimension(){return kind==1?"minecraft:overworld":"whileaway:quiet_city";}
    public CompoundTag save() {
        var t=new CompoundTag();t.putInt("kind",kind);t.putString("eventId",eventId());t.putUUID("instanceId",id);
        t.putLong("origin",origin.asLong());t.putString("dimension",dimension());
        t.putString("structureId",kind==1?"station:"+origin.asLong():"quiet_city:station_district");
        t.putString("state",progress.state.name());t.putInt("checkpoint",progress.checkpoint);
        t.putInt("shown",progress.shown);t.putInt("interruptions",progress.interruptions);t.putString("reason",progress.reason);
        // No NPCs, rewards, consumptions or block edits are performed by these presentation events.
        t.putString("temporaryActorRoles",kind==3?"apparition_0..6":"none");
        return t;
    }
    public static SceneRecord load(CompoundTag t) {return load(t,new UUID(0,0));}
    public static SceneRecord load(CompoundTag t,UUID player) {
        int kind=t.getInt("kind");if(kind<1||kind>3)throw new IllegalStateException("Unsupported scene kind="+kind);
        if(!t.contains("origin"))throw new IllegalStateException("Scene origin missing for kind="+kind);
        var origin=BlockPos.of(t.getLong("origin"));
        var r=t.hasUUID("instanceId")?new SceneRecord(kind,t.getUUID("instanceId"),origin):migrated(kind,origin,player);
        r.progress.restore(t.getString("state"),t.getInt("checkpoint"),t.getInt("shown"),t.getInt("interruptions"));
        if(r.progress.reason.isEmpty())r.progress.reason=t.getString("reason");
        return r;
    }
}
