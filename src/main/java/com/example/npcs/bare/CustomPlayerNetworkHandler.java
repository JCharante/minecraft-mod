package com.example.npcs.bare;


import com.example.util.LogManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class CustomPlayerNetworkHandler extends ServerPlayNetworkHandler {
    private static final ClientConnection FAKE_CONNECTION = new ControlledClientConnection();

    public CustomPlayerNetworkHandler(ServerPlayerEntity player) {
        super(player.getServer(), FAKE_CONNECTION, player, ConnectedClientData.createDefault(player.getGameProfile()));
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        super.send(packet, callbacks);
        LogManager.info("CustomPlayerNetworkHandler-send", packet.toString());
    }

    public void tickClientConnection() {
        FAKE_CONNECTION.tick();
    }

    private static final class ControlledClientConnection extends ClientConnection {
        private ControlledClientConnection() {
            super(NetworkSide.CLIENTBOUND);
        }

        @Override
        public void setPacketListener(PacketListener packetListener) {
            // super.setPacketListener(packetListener);
        }
    }
}
