package com.example.kadma.network;

import com.example.kadma.KadmaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * The single mod channel.
 *
 * <p>Only two packet types exist: the HUD payload and the cosmetic effect trigger.
 * Both are server to client. There is no client packet that can change state.</p>
 */
public final class KadmaNetwork {

    /** Channel name {@code kadma:main}. */
    public static final ResourceLocation CHANNEL = new ResourceLocation(KadmaMod.MODID, "main");

    private static final String PROTOCOL = "1";

    private static SimpleChannel channel;

    private KadmaNetwork() {
    }

    public static void init() {
        if (channel != null) {
            return;
        }
        channel = NetworkRegistry.newSimpleChannel(CHANNEL, () -> PROTOCOL,
                version -> version.equals(PROTOCOL), version -> version.equals(PROTOCOL));

        channel.registerMessage(0, HudSyncPacket.class,
                HudSyncPacket::encode,
                HudSyncPacket::decode,
                HudSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        channel.registerMessage(1, EffectPacket.class,
                EffectPacket::encode,
                EffectPacket::decode,
                EffectPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static SimpleChannel channel() {
        if (channel == null) {
            init();
        }
        return channel;
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        if (player == null) {
            return;
        }
        channel().send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToAll(Object message) {
        channel().send(PacketDistributor.ALL.noArg(), message);
    }

    /** Server to client HUD payload. */
    public static final class HudSyncPacket {

        public final String json;
        public final byte toggle;

        public HudSyncPacket(String json, int toggle) {
            this.json = json == null ? "" : json;
            this.toggle = (byte) Math.max(0, Math.min(2, toggle));
        }

        public static HudSyncPacket of(String json, boolean enabled) {
            return new HudSyncPacket(json, enabled ? 1 : 0);
        }

        public static void encode(HudSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.json, 32767);
            buffer.writeByte(packet.toggle);
        }

        public static HudSyncPacket decode(FriendlyByteBuf buffer) {
            return new HudSyncPacket(buffer.readUtf(32767), buffer.readByte());
        }

        public static void handle(HudSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                com.example.kadma.KadmaClientState.receive(packet.json, packet.toggle == 1);
            });
            context.setPacketHandled(true);
        }
    }

    /** Server to client cosmetic effect request. The client only plays local effects. */
    public static final class EffectPacket {

        public final String kind;
        public final double x;
        public final double y;
        public final double z;

        public EffectPacket(String kind, double x, double y, double z) {
            this.kind = kind == null ? "" : kind;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static void encode(EffectPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.kind, 64);
            buffer.writeDouble(packet.x);
            buffer.writeDouble(packet.y);
            buffer.writeDouble(packet.z);
        }

        public static EffectPacket decode(FriendlyByteBuf buffer) {
            return new EffectPacket(buffer.readUtf(64), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        public static void handle(EffectPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                com.example.kadma.KadmaClientState.playEffect(packet.kind, packet.x, packet.y, packet.z);
            });
            context.setPacketHandled(true);
        }
    }
}
