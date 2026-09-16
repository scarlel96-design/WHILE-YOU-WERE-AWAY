package io.github.whileaway;

import io.github.whileaway.core.EventCheckpoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Checkpoints for the existing three city books, not a new puzzle or automatic deduction system.
 * Evidence is permanent; presentation of the already existing summary is an at-most-once notice. */
public final class CityInvestigation {
    public static final String EVENT_ID="city_records";
    public final EventCheckpoint progress=new EventCheckpoint();
    private int evidence,pendingEvidence;
    private boolean noticeClaimed;
    public int evidence(){return evidence;}
    public int pendingEvidence(){return pendingEvidence;}
    public boolean reserve(int bit) {
        if(bit!=1&&bit!=2&&bit!=4)throw new IllegalArgumentException("Unknown city evidence bit="+bit);
        if(((evidence|pendingEvidence)&bit)!=0)return false;
        pendingEvidence|=bit;return true;
    }
    public boolean noticeClaimed(){return noticeClaimed;}
    public boolean accept(int bit) {
        if(bit!=1&&bit!=2&&bit!=4)throw new IllegalArgumentException("Unknown city evidence bit="+bit);
        if((evidence&bit)!=0)return false;
        evidence|=bit;pendingEvidence&=~bit;
        while(progress.checkpoint<Integer.bitCount(evidence))progress.advance(progress.checkpoint+1);
        return true;
    }
    public boolean confirm() {
        if(evidence!=7||progress.checkpoint==4)return false;
        if(!progress.advance(4))throw new IllegalStateException("Investigation checkpoint disagrees with evidence");
        noticeClaimed=true;return true;
    }
    public CompoundTag save() {
        var t=new CompoundTag();t.putString("eventId",EVENT_ID);t.putInt("evidence",evidence);t.putInt("pendingEvidence",pendingEvidence);
        t.putString("state",progress.state.name());t.putInt("checkpoint",progress.checkpoint);t.putBoolean("noticeClaimed",noticeClaimed);return t;
    }
    public static CityInvestigation load(CompoundTag t,int legacyBits,boolean exists) {
        var r=new CityInvestigation();
        // Both are persisted facts from supported versions. Never undo an already read book.
        r.evidence=(legacyBits|(exists?t.getInt("evidence"):0))&7;
        r.pendingEvidence=exists?(t.getInt("pendingEvidence")&7&~r.evidence):0;
        int cp=Integer.bitCount(r.evidence);
        boolean completed=r.evidence==7&&(!exists || ("COMPLETED".equals(t.getString("state"))&&t.getInt("checkpoint")==4));
        for(int i=1;i<=cp;i++)r.progress.advance(i);
        if(completed)r.confirm();
        r.noticeClaimed=r.noticeClaimed||(exists&&t.getBoolean("noticeClaimed"));
        return r;
    }
    public static void read(ServerPlayer p,int bit) {
        var d=NarrativeData.get(p.getServer());var e=d.entry(p.getUUID());
        if(!e.cityVisited||!p.level().dimension().equals(CityDistrict.KEY))return;
        if(e.investigation.reserve(bit)) {
            SceneRecovery.persist(p.getServer(),d);
            RecoveryBoundaryEvent.emit(p,EVENT_ID,"PREPARED",e.investigation.progress.checkpoint);
        }
        finish(p);
    }
    public static void finish(ServerPlayer p) {
        var d=NarrativeData.get(p.getServer());var e=d.entry(p.getUUID());var r=e.investigation;
        for(int bit:new int[]{1,2,4})if((r.pendingEvidence()&bit)!=0) {
            r.accept(bit);e.cityClues=r.evidence();SceneRecovery.persist(p.getServer(),d);
            RecoveryBoundaryEvent.emit(p,EVENT_ID,"FACT_COMMITTED",r.progress.checkpoint);
        }
        if(r.evidence()!=7||r.progress.checkpoint==4)return;
        RecoveryBoundaryEvent.emit(p,EVENT_ID,"BEFORE_COMPLETE",r.progress.checkpoint);
        boolean notify=!r.noticeClaimed();r.confirm();SceneRecovery.persist(p.getServer(),d);
        RecoveryBoundaryEvent.emit(p,EVENT_ID,"AFTER_COMPLETE",r.progress.checkpoint);
        if(notify)p.displayClientMessage(Component.translatable("city.whileaway.records_complete"),false);
    }
}
