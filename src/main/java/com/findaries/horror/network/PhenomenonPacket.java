package com.findaries.horror.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * One packet for everything the client has to draw or do on its own:
 * the things in the sky, the walkers, the tower and the "thrown to the main menu" kick.
 */
public record PhenomenonPacket(int kind, double x, double y, double z, float angle, long seed) {
    public static final int KICK = 0;
    public static final int TRIANGLE = 1;
    public static final int SLAB = 2;
    public static final int UFO = 3;
    public static final int WALKERS = 4;
    public static final int TOWER = 5;
    public static final int TRIANGLE_LYING = 6;
    public static final int TRIANGLE_STAR = 7;
    public static final int TRIANGLE_EYES = 8;
    public static final int SHIP = 9;
    /** The 860 block tall Nevelim: rises, opens its mouth, fires an orange beam and explodes. */
    public static final int GIANT_NEVELIM = 10;

    public static boolean isTriangle(int kind) {
        return kind == TRIANGLE || kind == TRIANGLE_LYING || kind == TRIANGLE_STAR || kind == TRIANGLE_EYES;
    }

    public static void encode(PhenomenonPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.angle);
        buf.writeLong(p.seed);
    }

    public static PhenomenonPacket decode(FriendlyByteBuf buf) {
        return new PhenomenonPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readLong());
    }

    public static void handle(PhenomenonPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.findaries.horror.client.ClientPhenomena.receive(p)));
        ctx.get().setPacketHandled(true);
    }
}
