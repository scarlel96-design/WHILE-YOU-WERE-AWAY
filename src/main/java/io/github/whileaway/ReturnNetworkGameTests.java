package io.github.whileaway;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.gametest.*;
import static io.github.whileaway.ReturnNetworkState.*;

/** Contract tests only: no claim of player interaction, world mutation or real-client proof. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkGameTests {
    private static ReturnNetworkState discovered(){return notStarted().discover(new Prerequisite(4,true,true,true),UUID.randomUUID(),new Location("rail_relay","whileaway:quiet_city",42));}
    private static Participant participant(){return new Participant("npc:yeoul",UUID.randomUUID(),UUID.randomUUID(),0);}
    private static ReturnNetworkState response(){return discovered().join(participant()).observePattern().intervene().beginPresentation().observeResponse();}
    private static void rejected(GameTestHelper h,Runnable r,String message){boolean rejected=false;try{r.run();}catch(IllegalArgumentException ex){rejected=true;}h.assertTrue(rejected,message);}
    @GameTest(template="empty") public static void networkOrderedFactsAndIdempotence(GameTestHelper h){
        var s=response();h.assertTrue(s.stage==Stage.RESPONSE_OBSERVED&&!s.facts.contains(Fact.SHARED_EXPERIENCE),"response is not shared/complete");
        var shared=s.shareExperience();h.assertTrue(shared.stage==Stage.RESPONSE_OBSERVED&&shared.facts.contains(Fact.SHARED_EXPERIENCE),"F durable substep separate from G");
        var end=shared.complete();h.assertTrue(end.complete()==end&&end.observePattern()==end&&end.shareExperience()==end,"repeat does not mutate terminal facts");
        h.assertTrue(end.facts.containsAll(Set.of(Fact.NEXT_PREREQUISITE,Fact.AFTERMATH,Fact.COMPLETE))&&end.presentation==Presentation.AFTERMATH,"atomic completion group");h.succeed();
    }
    @GameTest(template="empty") public static void networkRoundTripsEverySemanticBoundary(GameTestHelper h){
        var s=notStarted();var list=new ArrayList<ReturnNetworkState>();list.add(s);s=discovered();list.add(s);s=s.join(participant());list.add(s);s=s.observePattern();list.add(s);s=s.intervene();list.add(s);s=s.beginPresentation();list.add(s);s=s.observeResponse();list.add(s);s=s.shareExperience();list.add(s);s=s.complete();list.add(s);
        for(var state:list){var copy=load(state.save());h.assertTrue(copy.save().equals(state.save()),"exact NBT roundtrip "+state.stage+" "+state.presentation);}h.succeed();
    }
    @GameTest(template="empty") public static void networkCannotSkipPrerequisiteOrIntervention(GameTestHelper h){
        rejected(h,()->notStarted().discover(new Prerequisite(3,true,true,true),UUID.randomUUID(),new Location("relay","minecraft:overworld",0)),"incomplete prerequisite");
        var s=discovered();rejected(h,s::observePattern,"must join");rejected(h,s::intervene,"must observe");rejected(h,s::observeResponse,"must present");rejected(h,s::complete,"must share");h.succeed();
    }
    @GameTest(template="empty") public static void networkBindingAndCopyIsolation(GameTestHelper h){
        var s=discovered().join(participant());var b=load(s.save());var a=s.observePattern().intervene();h.assertTrue(b.stage==Stage.NPC_JOINED&&a.stage==Stage.INTERVENTION_COMMITTED,"independent immutable drafts");
        rejected(h,()->s.join(participant()),"no UUID/instance reassignment");h.assertTrue(a.participant.equals(b.participant)&&a.participant.generation()==0,"identity stable");h.succeed();
    }
    @GameTest(template="empty") public static void networkWaitsNeverRollbackFacts(GameTestHelper h){
        var s=response();var before=s.save();h.assertTrue(s.waiting(false,true,true,true)==Wait.PLAYER_ABSENT,"death wait");h.assertTrue(s.waiting(true,false,true,true)==Wait.WRONG_DIMENSION,"dimension wait");h.assertTrue(s.waiting(true,true,false,true)==Wait.WAITING_NPC,"unconfirmed NPC wait");h.assertTrue(s.waiting(true,true,true,false)==Wait.WAITING_ENVIRONMENT,"damaged relay wait");h.assertTrue(before.equals(s.save()),"all waits read only");h.succeed();
    }
    @GameTest(template="empty") public static void networkRejectsSemanticCorruption(GameTestHelper h){
        var end=response().shareExperience().complete().save();
        for(String fact:List.of("RESPONSE","SHARED_EXPERIENCE","AFTERMATH","PATTERN_MATCH_EVIDENCE")){var t=end.copy();t.getList("facts",8).removeIf(v->v.getAsString().equals(fact));rejected(h,()->load(t),"missing permanent fact "+fact);}
        for(String field:List.of("participant","instance","position")){var t=end.copy();t.remove(field);rejected(h,()->load(t),"missing binding "+field);}
        var wrong=end.copy();wrong.putString("structure","INTACT");rejected(h,()->load(wrong),"structure contradiction");h.succeed();
    }
    @GameTest(template="empty") public static void networkRejectsMalformedAndUnsupportedRecords(GameTestHelper h){
        var original=response().save();
        for(String field:List.of("schema","checkpoint")){var t=original.copy();t.putInt(field,99);rejected(h,()->load(t),"invalid "+field);}
        var t=original.copy();t.getList("facts",8).add(StringTag.valueOf("RESPONSE"));rejected(h,()->load(t),"duplicate not silently deduplicated");
        var wrong=original.copy();wrong.putString("stage","UNKNOWN");rejected(h,()->load(wrong),"invalid enum");
        var extra=original.copy();extra.putString("future_optional","ignored");h.assertTrue(load(extra).save().equals(original),"unknown optional key safe");h.assertTrue(original.equals(load(original).save()),"input unmodified");h.succeed();
    }
}
