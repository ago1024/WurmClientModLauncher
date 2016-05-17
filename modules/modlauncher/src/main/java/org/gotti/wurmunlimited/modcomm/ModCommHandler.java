package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.communication.SocketConnection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

public class ModCommHandler {
    public static void handlePacket(ByteBuffer msg) {
        try {
            byte type = msg.get();
            switch (type) {
                case ModCommConstants.PACKET_MESSAGE:
                    handlePacketMessage(msg);
                    break;
                case ModCommConstants.PACKET_CHANNELS:
                    handlePacketChannels(msg);
                    break;
                default:
                    ModComm.logWarning(String.format("Unknown packet from server (%d)", type));
            }
        } catch (Exception e) {
            ModComm.logException("Error handling packet from server", e);
        }
    }

    public static void startHandshake() {
        ModComm.logInfo(String.format("Starting handshake, reporting %d registered channels", ModComm.channels.size()));
        try (PacketWriter writer = new PacketWriter()) {
            writer.writeByte(ModCommConstants.CMD_MODCOMM);
            writer.writeByte(ModCommConstants.PACKET_CHANNELS);
            writer.writeByte(ModCommConstants.PROTO_VERSION);
            writer.writeInt(ModComm.channels.size());
            for (Channel channel : ModComm.channels.values()) {
                writer.writeUTF(channel.name);
            }
            SocketConnection conn = ModComm.getServerConnection();
            ByteBuffer buff = conn.getBuffer();
            buff.put(writer.getBytes());
            conn.flush();
        } catch (IOException e) {
            ModComm.logException("Error in handshake", e);
        }


    }

    private static void handlePacketChannels(ByteBuffer msg) throws IOException {
        PacketReader reader = new PacketReader(msg);
        HashSet<Channel> toActivate = new HashSet<>();

        ModComm.serverVersion = reader.readByte();

        int n = reader.readInt();

        while (n-- > 0) {
            int id = reader.readInt();
            String name = reader.readUTF();
            if (ModComm.channels.containsKey(name)) {
                Channel channel = ModComm.channels.get(name);
                channel.id = id;
                ModComm.idMap.put(id, channel);
                toActivate.add(channel);
            }
        }

        ModComm.logInfo(String.format("Handshake response received, server protocol version is %d, %d channels activated", ModComm.serverVersion, toActivate.size()));

        for (Channel channel : toActivate) {
            try {
                channel.listener.onServerConnected();
            } catch (Exception e) {
                ModComm.logException(String.format("Error in channel %s onServerConnected", channel.name), e);
            }
        }
    }

    private static void handlePacketMessage(ByteBuffer msg) {
        int id = msg.getInt();
        if (!ModComm.idMap.containsKey(id)) {
            ModComm.logWarning(String.format("Message on unregistered channel %d", id));
            return;
        }
        Channel ch = ModComm.idMap.get(id);
        try {
            ch.listener.handleMessage(msg.slice());
        } catch (Exception e) {
            ModComm.logException(String.format("Error in channel handler %s", ch.name), e);
        }
    }
}
