package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.communication.SocketConnection;

import java.nio.ByteBuffer;

/**
 * Channel object, created by calling {@link ModComm#registerChannel}
 */
public class Channel {
    int id;
    final IChannelListener listener;
    final String name;

    Channel(String name, IChannelListener listener) {
        this.id = -1;
        this.name = name;
        this.listener = listener;
    }

    /**
     * Send message to the server on this channel. Channel must be active.
     *
     * @param message contents of the message
     */
    public void sendMessage(ByteBuffer message) {
        if (!isActive())
            throw new RuntimeException(String.format("Channel %s is not active", name));
        try {
            SocketConnection conn = ModComm.getServerConnection();
            ByteBuffer buff = conn.getBuffer();
            buff.put(ModCommConstants.CMD_MODCOMM);
            buff.put(ModCommConstants.PACKET_MESSAGE);
            buff.putInt(id);
            buff.put(message);
            buff.put(message);
            conn.flush();
        } catch (Exception e) {
            ModComm.logException(String.format("Error sending packet on channel %s", name), e);
        }
    }

    /**
     * Check if a channel is active for current server connection
     *
     * @return true if the channel is active
     */
    public boolean isActive() {
        return id > 0;
    }
}
