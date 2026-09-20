package io.github.whileaway;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

final class ReturnNetworkFixtures {
    static StoryActorRecord resident(NarrativeData d) {
        var r=new StoryActorRecord(NpcEvents.STORY,NpcEvents.ID,"resident","whileaway:story_npc","minecraft:overworld",
            UUID.fromString("70000000-0000-0000-0000-000000000001"),null,
            UUID.fromString("70000000-0000-0000-0000-000000000002"),new BlockPos(12,64,-8),false);
        NpcState.initialize(r,new BlockPos(12,64,-8));
        r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;
        for(int i=1;i<=4;i++)r.checkpoint.advance(i);
        r.facts.add(NpcState.MET+"70000000-0000-0000-0000-000000000003");r.facts.add(NpcState.DIALOGUE);
        r.facts.add(NpcState.ARRIVED);r.facts.add(NpcState.OBSERVED);NpcState.settle(r);r.facts.add(NpcState.LINK);
        r.facts.add(NpcState.relationship("player:70000000-0000-0000-0000-000000000003","shared_return_light"));
        var pos=new ListTag();pos.add(DoubleTag.valueOf(12.5));pos.add(DoubleTag.valueOf(64));pos.add(DoubleTag.valueOf(-7.5));r.snapshot.put("Pos",pos);
        var motion=new ListTag();for(int i=0;i<3;i++)motion.add(DoubleTag.valueOf(0));r.snapshot.put("Motion",motion);
        d.actors.put(r.storyId,r);return r;
    }
}
