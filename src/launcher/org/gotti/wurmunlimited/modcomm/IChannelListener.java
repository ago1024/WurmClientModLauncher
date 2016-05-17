package org.gotti.wurmunlimited.modcomm;

import java.nio.ByteBuffer;

/**
 * Listener for mod channels, implement in a class and register with {@link ModComm#registerChannel}
 */
public interface IChannelListener {
    /**
     * Handle a message from a player
     *
     * @param message message contents
     */
    default void handleMessage(ByteBuffer message) {
    }

    /**
     * Called when a server connection is started and this channel is activated
     */
    default void onServerConnected() {
    }

}
