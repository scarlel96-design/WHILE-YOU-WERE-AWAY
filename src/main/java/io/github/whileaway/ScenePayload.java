package io.github.whileaway;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record ScenePayload(int kind,BlockPos origin,java.util.UUID eventId,java.util.UUID lease,int checkpoint,int shown) implements CustomPacketPayload {
    public static final java.util.UUID NONE=new java.util.UUID(0,0);
    public ScenePayload(int kind,BlockPos origin){this(kind,origin,NONE,NONE,0,0);}
    public static final Type<ScenePayload> TYPE=new Type<>(WhileAway.id("scene"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ScenePayload> CODEC=StreamCodec.ofMember(ScenePayload::write,ScenePayload::read);
    private void write(RegistryFriendlyByteBuf b){b.writeVarInt(kind);b.writeBlockPos(origin);b.writeUUID(eventId);b.writeUUID(lease);b.writeVarInt(checkpoint);b.writeVarInt(shown);}
    private static ScenePayload read(RegistryFriendlyByteBuf b){return new ScenePayload(b.readVarInt(),b.readBlockPos(),b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt());}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
