package org.gotti.wurmunlimited.modcomm;

public class ModCommConstants {
    /**
     * Packet ID for all packets used by this system, should not collide with any packets used by WU (see {@link com.wurmonline.shared.constants.ProtoConstants})
     */
    public static final byte CMD_MODCOMM = -100;

    /**
     * Marker that will be detected by the client to initiate the handshake process
     */
    public static final String MARKER = "[ModCommV1]";

    /**
     * Version of the internal protocol
     */
    public static final byte PROTO_VERSION = 1;

    /**
     * Packet types for the internal protocol
     */
    public static final byte PACKET_MESSAGE = 1;
    public static final byte PACKET_CHANNELS = 2;
}
