package io.github.whileaway;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CINEMATIC_OVERLAY;
    static {
        var b=new ModConfigSpec.Builder();
        CINEMATIC_OVERLAY=b.comment("Subtle scene edge shading/captions. No forced camera, strobe or control lock.").define("cinematicOverlay",true);
        SPEC=b.build();
    }
    private ClientConfig() {}
}
