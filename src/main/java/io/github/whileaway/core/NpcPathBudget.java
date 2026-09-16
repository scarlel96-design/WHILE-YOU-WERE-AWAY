package io.github.whileaway.core;

/** Entity-local, game-tick retry budget. Never serialized or shared across worlds. */
public final class NpcPathBudget {
    public static final int RETRY_TICKS=60, BACKOFF_TICKS=200, FAILURE_LIMIT=3, STALL_TICKS=160;
    private long nextAttempt;private int failures,attempts;
    public boolean ready(long tick){return tick>=nextAttempt;}
    public void attempted(long tick){attempts++;nextAttempt=tick+RETRY_TICKS;}
    public void failed(long tick){failures=Math.min(FAILURE_LIMIT,failures+1);nextAttempt=tick+(unreachable()?BACKOFF_TICKS:RETRY_TICKS);}
    public void progress(){failures=0;}
    public boolean unreachable(){return failures>=FAILURE_LIMIT;}
    public int attempts(){return attempts;}
    public int failures(){return failures;}
    public void reset(){nextAttempt=0;failures=0;}
}
