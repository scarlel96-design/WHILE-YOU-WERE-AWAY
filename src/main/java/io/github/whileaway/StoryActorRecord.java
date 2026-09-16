package io.github.whileaway;

import io.github.whileaway.core.EventCheckpoint;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** World-local identity and safe checkpoint; entity NBT is reconstructible, facts are not. */
public final class StoryActorRecord {
    public enum Lifecycle { PREPARED, ACTIVE, SUSPENDED, RETIRED }
    public final String storyId, eventId, role, entityType, dimension;
    public final UUID instanceId;
    public final UUID owner;
    public final boolean temporary;
    public UUID entityId;
    public int generation;
    public BlockPos position;
    public Lifecycle lifecycle=Lifecycle.PREPARED;
    public final EventCheckpoint checkpoint=new EventCheckpoint();
    public CompoundTag snapshot=new CompoundTag();
    public final Set<String> facts=new TreeSet<>();
    public boolean noticeClaimed;
    public StoryActorRecord(String storyId,String eventId,String role,String entityType,String dimension,
            UUID instanceId,UUID owner,UUID entityId,BlockPos position,boolean temporary) {
        this.storyId=storyId;this.eventId=eventId;this.role=role;this.entityType=entityType;this.dimension=dimension;
        this.instanceId=instanceId;this.owner=owner;this.entityId=entityId;this.position=position.immutable();this.temporary=temporary;
    }
    public CompoundTag save() {
        var t=new CompoundTag();t.putString("storyId",storyId);t.putString("ownerEventId",eventId);t.putString("spawnRole",role);
        t.putString("entityType",entityType);t.putString("dimension",dimension);t.putUUID("instanceId",instanceId);
        if(owner!=null)t.putUUID("owner",owner);t.putUUID("entityUUID",entityId);t.putInt("generation",generation);
        t.putLong("position",position.asLong());t.putBoolean("temporary",temporary);t.putString("lifecycleState",lifecycle.name());
        t.putString("state",checkpoint.state.name());t.putInt("checkpoint",checkpoint.checkpoint);
        t.putInt("interruptions",checkpoint.interruptions);t.putString("reason",checkpoint.reason);
        t.put("snapshot",snapshot.copy());t.putBoolean("noticeClaimed",noticeClaimed);
        var list=new ListTag();facts.forEach(f->list.add(StringTag.valueOf(f)));t.put("facts",list);return t;
    }
    public static StoryActorRecord load(CompoundTag t) {
        for(var key:List.of("storyId","ownerEventId","spawnRole","entityType","dimension","lifecycleState"))
            if(t.getString(key).isBlank())throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor missing "+key);
        for(var key:List.of("instanceId","entityUUID"))if(!t.hasUUID(key))throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor missing "+key);
        if(!t.contains("position",Tag.TAG_LONG)||t.getInt("generation")<0)throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor position/generation invalid");
        var r=new StoryActorRecord(t.getString("storyId"),t.getString("ownerEventId"),t.getString("spawnRole"),t.getString("entityType"),t.getString("dimension"),
            t.getUUID("instanceId"),t.hasUUID("owner")?t.getUUID("owner"):null,t.getUUID("entityUUID"),BlockPos.of(t.getLong("position")),t.getBoolean("temporary"));
        try{r.lifecycle=Lifecycle.valueOf(t.getString("lifecycleState"));}catch(IllegalArgumentException ex){throw new IllegalStateException("MANUAL_DIAGNOSTIC Unknown actor lifecycle",ex);}
        r.generation=t.getInt("generation");r.snapshot=t.getCompound("snapshot").copy();r.noticeClaimed=t.getBoolean("noticeClaimed");
        r.checkpoint.restore(t.getString("state"),t.getInt("checkpoint"),0,t.getInt("interruptions"));
        if(r.checkpoint.reason.isEmpty())r.checkpoint.reason=t.getString("reason");
        for(var fact:t.getList("facts",Tag.TAG_STRING))r.facts.add(fact.getAsString());
        return r;
    }
}
