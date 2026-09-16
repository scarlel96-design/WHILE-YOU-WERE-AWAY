package io.github.whileaway;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** Non-cancellable observation seam. Boundary values are immutable; this is not authority to modify story facts.
 * Emitted on the server thread; normal gameplay has no fault-injection listener enabled. */
public final class RecoveryBoundaryEvent extends Event {
    public final ServerPlayer player;
    public final String eventId,boundary;
    public final int checkpoint;
    private RecoveryBoundaryEvent(ServerPlayer p,String id,String boundary,int checkpoint) {
        this.player=p;this.eventId=id;this.boundary=boundary;this.checkpoint=checkpoint;
    }
    public static void emit(ServerPlayer p,String id,String boundary,int cp) {
        NeoForge.EVENT_BUS.post(new RecoveryBoundaryEvent(p,id,boundary,cp));
    }
}
