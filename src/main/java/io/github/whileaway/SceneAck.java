package io.github.whileaway;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SceneAck(UUID eventId,UUID lease,int checkpoint,int shown,boolean suspended) implements CustomPacketPayload {
    public static final Type<SceneAck> TYPE=new Type<>(WhileAway.id("scene_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SceneAck> CODEC=StreamCodec.ofMember(SceneAck::write,SceneAck::read);
    private void write(RegistryFriendlyByteBuf b){b.writeUUID(eventId);b.writeUUID(lease);b.writeVarInt(checkpoint);b.writeVarInt(shown);b.writeBoolean(suspended);}
    private static SceneAck read(RegistryFriendlyByteBuf b){return new SceneAck(b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readBoolean());}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
