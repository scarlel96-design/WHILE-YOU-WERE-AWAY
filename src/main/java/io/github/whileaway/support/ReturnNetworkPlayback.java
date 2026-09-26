package io.github.whileaway.support;

import java.util.UUID;

/** Transient every-tick driver. Packet delivery is NOT proof of human perception. */
public final class ReturnNetworkPlayback {
    public enum Purpose { OLD_PATTERN, RESPONSE }
    public static final ReturnNetworkSignal.Timing TIMING=new ReturnNetworkSignal.Timing(12,36,16,16);
    public record Frame(int pulse,boolean onset,boolean offset,boolean complete) {}
    private final ReturnNetworkSignal.TransientSession session;
    private final Purpose purpose;
    private long lastTick=-1;
    private int active=-1;
    private boolean interrupted;
    public ReturnNetworkPlayback(Purpose purpose,long now) {
        this.purpose=purpose;
        session=ReturnNetworkSignal.TransientSession.start(UUID.randomUUID().toString(),ReturnNetworkSignal.SignalUse.CORE,
            Math.addExact(now,10),TIMING,ReturnNetworkSignal.CompletedReplayPolicy.NO_REPLAY);
    }
    public Frame tick(long now,boolean ready) {
        if(interrupted)return new Frame(-1,false,false,false);
        if(!ready||(lastTick>=0&&now!=lastTick+1)){interrupted=true;return new Frame(-1,false,active>=0,false);}
        lastTick=now;
        var emitted=session.poll(now);
        if(session.state()==ReturnNetworkSignal.SessionState.RESTART_REQUIRED){interrupted=true;return new Frame(-1,false,active>=0,false);}
        int current=-1;
        for(var p:session.timeline().pulses())if(now>=p.startTick()&&now<p.endTickExclusive())current=p.identityIndex();
        boolean end=active>=0&&current!=active;active=current;
        return new Frame(current,!emitted.isEmpty(),end,session.completed());
    }
    public void carrierFailed(){interrupted=true;}
    public boolean interrupted(){return interrupted;}
    public boolean complete(){return !interrupted&&session.completed();}
    public Purpose purpose(){return purpose;}
    public String id(){return session.sessionIdentity();}
    public long origin(){return session.timeline().originTick();}
    public int emitted(){return session.emissions().size();}
}
