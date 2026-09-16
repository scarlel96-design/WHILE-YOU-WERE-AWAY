package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;

/** NPC facts are preserved by the existing actor-schema-1 fact ledger, including old readers.
 * Identity is still exclusively StoryActorRecord; these values never select a canonical entity. */
public final class NpcState {
    private static final String HOME="npc.home=", GOAL="npc.goal=", WORK="npc.work=";
    public static final String MET="npc.met=", DIALOGUE="npc.dialogue=introduction", ARRIVED="npc.arrived",
        OBSERVED="npc.observed=return_light", SETTLED="npc.settled", LINK="npc.link=return_light_shared";
    private NpcState() {}
    public static void initialize(StoryActorRecord r,BlockPos home) {
        r.facts.add("npc.schema=1");r.facts.add(HOME+home.asLong());
        r.facts.add(GOAL+"choose_evening_routine");r.facts.add(WORK+"check_return_light");
    }
    private static String one(StoryActorRecord r,String prefix) {
        var values=r.facts.stream().filter(f->f.startsWith(prefix)).toList();
        if(values.size()!=1)throw new IllegalStateException("NPC_FACT_CARDINALITY "+r.storyId+" field="+prefix);
        return values.getFirst().substring(prefix.length());
    }
    public static BlockPos home(StoryActorRecord r){return BlockPos.of(Long.parseLong(one(r,HOME)));}
    public static String goal(StoryActorRecord r){return one(r,GOAL);}
    public static String work(StoryActorRecord r){return one(r,WORK);}
    public static void settle(StoryActorRecord r) {
        r.facts.removeIf(f->f.startsWith(WORK));r.facts.add(WORK+"maintain_return_light");r.facts.add(SETTLED);
    }
    /** Counterparts are not limited to players: npc:doyun and player:<uuid> use the same relation ledger. */
    public static String relationship(String counterpart,String sharedEvent) {
        if(!counterpart.matches("(?:npc|player):[a-zA-Z0-9_-]+")||!sharedEvent.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("NPC_RELATION_KEY");
        return "npc.relationship="+counterpart+"/"+sharedEvent;
    }
    public static List<String> integrity(StoryActorRecord r) {
        var issues=new ArrayList<String>();
        try {
            if(!r.facts.contains("npc.schema=1"))issues.add("NPC_SCHEMA_MISSING");
            home(r);goal(r);work(r);
            int cp=r.checkpoint.checkpoint;
            if(cp>=2&&(!r.facts.contains(DIALOGUE)||r.facts.stream().noneMatch(f->f.startsWith(MET))))issues.add("NPC_DIALOGUE_FACT_MISSING");
            if(cp>=3&&(!r.facts.contains(ARRIVED)||!r.facts.contains(OBSERVED)))issues.add("NPC_ARRIVAL_FACT_MISSING");
            if(cp==4&&(!r.facts.contains(SETTLED)||!r.facts.contains(LINK)||!work(r).equals("maintain_return_light")))issues.add("NPC_LIFE_COMMIT_MISSING");
            if(cp<4&&(r.facts.contains(SETTLED)||r.facts.contains(LINK)))issues.add("NPC_PREMATURE_LIFE_COMMIT");
            if(r.temporary||r.owner!=null)issues.add("NPC_INDEPENDENT_OWNERSHIP_REQUIRED");
        }catch(RuntimeException ex){issues.add(ex.getMessage());}
        return issues.stream().map(s->s+" storyId="+r.storyId+" instance="+r.instanceId+" checkpoint="+r.checkpoint.checkpoint).toList();
    }
}
