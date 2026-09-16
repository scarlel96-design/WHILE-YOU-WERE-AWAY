package io.github.whileaway;

import java.util.*;
import net.minecraft.nbt.*;

/** Immutable event contract, not a live adapter. Never infer permanent facts on load.
 * The future adapter must persist each returned draft BEFORE presenting its effects. */
public final class ReturnNetworkState {
    public static final String ID="return_network_first_response";
    public enum Stage { NOT_STARTED, DISCOVERED, NPC_JOINED, SIGNAL_OBSERVED, INTERVENTION_COMMITTED, RESPONSE_OBSERVED, COMPLETED }
    public enum Structure { INTACT, READY, ACTIVATED, RESPONDED }
    public enum Presentation { NONE, PRESENTING, FACT_COMMITTED, AFTERMATH }
    public enum Wait { NONE, PLAYER_ABSENT, WRONG_DIMENSION, WAITING_NPC, WAITING_ENVIRONMENT }
    public enum Fact { SIGNAL_DISCOVERED, PARTICIPANT_JOINED, OLD_PATTERN_EVIDENCE, INTERVENTION, RESPONSE, PATTERN_MATCH_EVIDENCE, SHARED_EXPERIENCE, COMPLETE, AFTERMATH, NEXT_PREREQUISITE }
    public record Location(String id,String dimension,long position) {
        public Location { require(id!=null&&!id.isBlank(),"location_id");require(dimension!=null&&dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"),"dimension"); }
    }
    public record Participant(String storyId,UUID instance,UUID entity,int generation) {
        public Participant { require("npc:yeoul".equals(storyId)&&instance!=null&&entity!=null&&generation>=0,"participant_identity"); }
    }
    public record Prerequisite(int checkpoint,boolean completed,boolean sharedExperience,boolean relationship) {
        public boolean ready(){return checkpoint==4&&completed&&sharedExperience&&relationship;}
    }
    public final Stage stage;
    public final UUID instance;
    public final Location location;
    public final Participant participant;
    public final Structure structure;
    public final Presentation presentation;
    public final Set<Fact> facts;
    private ReturnNetworkState(Stage stage,UUID instance,Location location,Participant participant,Structure structure,Presentation presentation,Set<Fact> facts) {
        this.stage=Objects.requireNonNull(stage);this.instance=instance;this.location=location;this.participant=participant;
        this.structure=Objects.requireNonNull(structure);this.presentation=Objects.requireNonNull(presentation);this.facts=Set.copyOf(facts);validate();
    }
    private static void require(boolean ok,String field){if(!ok)throw new IllegalArgumentException("RETURN_NETWORK_CORRUPT field="+field);}
    private static EnumSet<Fact> expected(int cp,boolean shared){
        var f=EnumSet.noneOf(Fact.class);
        if(cp>=1)f.add(Fact.SIGNAL_DISCOVERED);
        if(cp>=2)f.add(Fact.PARTICIPANT_JOINED);
        if(cp>=3)f.add(Fact.OLD_PATTERN_EVIDENCE);
        if(cp>=4)f.add(Fact.INTERVENTION);
        if(cp>=5)f.addAll(Set.of(Fact.RESPONSE,Fact.PATTERN_MATCH_EVIDENCE));
        if(shared)f.add(Fact.SHARED_EXPERIENCE);
        if(cp==6)f.addAll(Set.of(Fact.COMPLETE,Fact.AFTERMATH,Fact.NEXT_PREREQUISITE));
        return f;
    }
    private void validate(){
        int cp=stage.ordinal();boolean shared=facts.contains(Fact.SHARED_EXPERIENCE);
        require(cp==0?instance==null&&location==null:instance!=null&&location!=null,"event_identity");
        require(cp<2?participant==null:participant!=null,"participant_binding");
        require(!shared||cp>=5,"shared_before_response");require(cp!=6||shared,"completed_without_shared");
        require(facts.equals(expected(cp,shared)),"checkpoint_evidence_order");
        require(structure==(cp<3?Structure.INTACT:cp==3?Structure.READY:cp==4?Structure.ACTIVATED:Structure.RESPONDED),"structure_commit");
        require(cp<4?presentation==Presentation.NONE:cp==4?(presentation==Presentation.NONE||presentation==Presentation.PRESENTING):cp==5?presentation==Presentation.FACT_COMMITTED:presentation==Presentation.AFTERMATH,"presentation_phase");
    }
    public static ReturnNetworkState notStarted(){return new ReturnNetworkState(Stage.NOT_STARTED,null,null,null,Structure.INTACT,Presentation.NONE,Set.of());}
    public ReturnNetworkState discover(Prerequisite proof,UUID eventInstance,Location at){
        require(proof.ready(),"prerequisite");
        if(stage!=Stage.NOT_STARTED){require(instance.equals(eventInstance)&&location.equals(at),"discovery_rebind");return this;}
        return new ReturnNetworkState(Stage.DISCOVERED,eventInstance,at,null,Structure.INTACT,Presentation.NONE,expected(1,false));
    }
    public ReturnNetworkState join(Participant binding){
        if(stage.ordinal()>=2){require(participant.equals(binding),"participant_rebind");return this;}
        require(stage==Stage.DISCOVERED,"join_order");
        return new ReturnNetworkState(Stage.NPC_JOINED,instance,location,binding,structure,presentation,expected(2,false));
    }
    private ReturnNetworkState advance(Stage next,Structure physical,Presentation scene){
        if(stage.ordinal()>=next.ordinal())return this;
        require(stage.ordinal()+1==next.ordinal(),"advance_order");
        return new ReturnNetworkState(next,instance,location,participant,physical,scene,expected(next.ordinal(),facts.contains(Fact.SHARED_EXPERIENCE)));
    }
    public ReturnNetworkState observePattern(){return advance(Stage.SIGNAL_OBSERVED,Structure.READY,Presentation.NONE);}
    public ReturnNetworkState intervene(){return advance(Stage.INTERVENTION_COMMITTED,Structure.ACTIVATED,Presentation.NONE);}
    public ReturnNetworkState beginPresentation(){
        if(stage.ordinal()>4||presentation==Presentation.PRESENTING)return this;
        require(stage==Stage.INTERVENTION_COMMITTED,"presentation_before_intervention");
        return new ReturnNetworkState(stage,instance,location,participant,structure,Presentation.PRESENTING,facts);
    }
    public ReturnNetworkState observeResponse(){
        require(stage.ordinal()>4||presentation==Presentation.PRESENTING,"response_before_presentation");
        return advance(Stage.RESPONSE_OBSERVED,Structure.RESPONDED,Presentation.FACT_COMMITTED);
    }
    public ReturnNetworkState shareExperience(){
        require(stage.ordinal()>=5,"shared_before_response");if(facts.contains(Fact.SHARED_EXPERIENCE))return this;
        return new ReturnNetworkState(stage,instance,location,participant,structure,presentation,expected(5,true));
    }
    public ReturnNetworkState complete(){
        require(facts.contains(Fact.SHARED_EXPERIENCE),"complete_before_shared");
        return advance(Stage.COMPLETED,Structure.RESPONDED,Presentation.AFTERMATH);
    }
    /** Observations do not mutate durable progress or manufacture a replacement participant. */
    public Wait waiting(boolean playerAlive,boolean correctDimension,boolean npcConfirmed,boolean environmentReady){
        if(!playerAlive)return Wait.PLAYER_ABSENT;if(!correctDimension)return Wait.WRONG_DIMENSION;
        if(!npcConfirmed)return Wait.WAITING_NPC;if(!environmentReady)return Wait.WAITING_ENVIRONMENT;return Wait.NONE;
    }
    public CompoundTag save(){
        var t=new CompoundTag();t.putInt("schema",1);t.putString("eventId",ID);t.putString("stage",stage.name());t.putInt("checkpoint",stage.ordinal());
        t.putString("structure",structure.name());t.putString("presentation",presentation.name());
        var list=new ListTag();facts.stream().sorted().forEach(f->list.add(StringTag.valueOf(f.name())));t.put("facts",list);
        if(instance!=null){t.putUUID("instance",instance);t.putString("locationId",location.id());t.putString("dimension",location.dimension());t.putLong("position",location.position());}
        if(participant!=null){var p=new CompoundTag();p.putString("storyId",participant.storyId());p.putUUID("instance",participant.instance());p.putUUID("entity",participant.entity());p.putInt("generation",participant.generation());t.put("participant",p);}
        return t;
    }
    private static void typed(CompoundTag t,String key,int type){require(t.contains(key,type),key+"_type");}
    public static ReturnNetworkState load(CompoundTag t){
        typed(t,"schema",Tag.TAG_INT);require(t.getInt("schema")==1,"unsupported_schema");typed(t,"eventId",Tag.TAG_STRING);require(ID.equals(t.getString("eventId")),"event_id");
        for(String key:List.of("stage","structure","presentation"))typed(t,key,Tag.TAG_STRING);
        typed(t,"checkpoint",Tag.TAG_INT);Stage stage=Stage.valueOf(t.getString("stage"));require(stage.ordinal()==t.getInt("checkpoint"),"checkpoint_stage");
        typed(t,"facts",Tag.TAG_LIST);var list=(ListTag)t.get("facts");require(list.isEmpty()||list.getElementType()==Tag.TAG_STRING,"facts_element_type");
        var facts=EnumSet.noneOf(Fact.class);for(Tag v:list)require(facts.add(Fact.valueOf(v.getAsString())),"duplicate_fact");
        UUID instance=null;Location at=null;Participant participant=null;
        if(stage!=Stage.NOT_STARTED){require(t.hasUUID("instance"),"event_instance");instance=t.getUUID("instance");typed(t,"locationId",Tag.TAG_STRING);typed(t,"dimension",Tag.TAG_STRING);typed(t,"position",Tag.TAG_LONG);at=new Location(t.getString("locationId"),t.getString("dimension"),t.getLong("position"));}
        else require(!t.contains("instance")&&!t.contains("locationId")&&!t.contains("dimension")&&!t.contains("position"),"inactive_identity");
        if(t.contains("participant")){typed(t,"participant",Tag.TAG_COMPOUND);var p=t.getCompound("participant");typed(p,"storyId",Tag.TAG_STRING);typed(p,"generation",Tag.TAG_INT);require(p.hasUUID("instance")&&p.hasUUID("entity"),"participant_uuid");participant=new Participant(p.getString("storyId"),p.getUUID("instance"),p.getUUID("entity"),p.getInt("generation"));}
        return new ReturnNetworkState(stage,instance,at,participant,Structure.valueOf(t.getString("structure")),Presentation.valueOf(t.getString("presentation")),facts);
    }
}
