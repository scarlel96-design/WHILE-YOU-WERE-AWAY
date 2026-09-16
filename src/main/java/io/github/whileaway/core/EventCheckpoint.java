package io.github.whileaway.core;

/** Durable scene boundaries, independent of render frames and permanent story facts. */
public final class EventCheckpoint {
    public enum State { NOT_STARTED, PREPARING, ACTIVE, PHASE_1, PHASE_2, PHASE_3, RESOLVING, COMPLETED, FAILED_RECOVERABLE, SUSPENDED, ABORTED }
    public static final int[] TICKS={0,65,130,195,240};
    public State state=State.PREPARING;
    public int checkpoint, shown, interruptions;
    public String reason="";
    public boolean terminal(){return state==State.COMPLETED||state==State.ABORTED;}
    public boolean advance(int next) {
        if(terminal()||next!=checkpoint+1||next>4)return false;
        checkpoint=next;
        state=switch(next){case 1->State.PHASE_1;case 2->State.PHASE_2;case 3->State.RESOLVING;default->State.COMPLETED;};
        return true;
    }
    public void suspend(String why) {
        if(terminal()||state==State.SUSPENDED)return;
        state=State.SUSPENDED;interruptions++;reason=why;
    }
    public void restore(String saved,int cp,int cues,int failures) {
        reason="";checkpoint=Math.clamp(cp,0,4);shown=cues&127;interruptions=Math.max(0,failures);
        if(cp<0||cp>4){checkpoint=0;state=State.FAILED_RECOVERABLE;reason="invalid_checkpoint:"+cp;return;}
        try{state=State.valueOf(saved);}catch(IllegalArgumentException ex){checkpoint=Math.min(checkpoint,3);state=State.FAILED_RECOVERABLE;reason="invalid_state:"+saved;return;}
        // Terminality requires both matching fields. Never infer a reward-bearing completion
        // from just a counter, and never turn an explicitly aborted event into a completed one.
        if(state==State.ABORTED)return;
        if(state==State.COMPLETED&&checkpoint!=4) {
            state=State.FAILED_RECOVERABLE;reason="completed_checkpoint_mismatch:"+cp;return;
        }
        if(state!=State.COMPLETED&&checkpoint==4) {
            checkpoint=3;state=State.FAILED_RECOVERABLE;reason="unconfirmed_completion:"+saved;return;
        }
        if(!terminal()&&state!=State.FAILED_RECOVERABLE)state=State.SUSPENDED;
    }
}
