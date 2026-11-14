package com.findaries.horror.network;

import com.findaries.horror.FinDaries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            FinDaries.rl("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, PhenomenonPacket.class, PhenomenonPacket::encode, PhenomenonPacket::decode, PhenomenonPacket::handle);
    }

    public static void toPlayer(ServerPlayer player, PhenomenonPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void toLevel(ServerLevel level, PhenomenonPacket packet) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    /** Throws the player out of the world straight to the title screen. */
    public static void kick(ServerPlayer player) {
        toPlayer(player, new PhenomenonPacket(PhenomenonPacket.KICK, 0, 0, 0, 0, 0L));
    }

    private Net() {
    }
}
