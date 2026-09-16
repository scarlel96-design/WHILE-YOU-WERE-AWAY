package io.github.whileaway.core;

/** Deterministic 12-second reveal: smooth envelopes, no forced camera or flashing. */
public final class SceneTimeline {
    public static final int LENGTH=240;
    public record Frame(float pressure,float bars,int caption,boolean finished) {}
    private SceneTimeline() {}
    private static float smooth(float v) { v=Math.clamp(v,0f,1f);return v*v*(3-2*v); }
    public static Frame at(int tick) {
        if(tick<0||tick>=LENGTH)return new Frame(0,0,0,true);
        float rise=smooth(tick/70f),fall=1-smooth((tick-160)/80f);
        int caption=tick>=65&&tick<120?1:tick>=160&&tick<225?2:0;
        return new Frame(rise*fall*.6f,smooth(tick/40f)*fall*.035f,caption,false);
    }
}
