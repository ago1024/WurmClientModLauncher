package org.gotti.wurmunlimited.modcomm;

import com.wurmonline.client.comm.SimpleServerConnectionClass;
import com.wurmonline.communication.SocketConnection;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modsupport.ModClient;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModComm {
    static final HashMap<String, Channel> channels = new HashMap<>();
    static final HashMap<Integer, Channel> idMap = new HashMap<>();

    static byte serverVersion = -1;

    private static final Logger logger = Logger.getLogger("ModComm");

    private static Field fConnection;

    /**
     * Register mod channel
     *
     * @param name     Unique identifier of the channel
     * @param listener Listener that will handle communication
     * @return new channel object
     */
    public static Channel registerChannel(String name, IChannelListener listener) {
        if (channels.containsKey(name))
            throw new RuntimeException(String.format("Channel %s already registered", name));
        Channel ch = new Channel(name, listener);
        channels.put(name, ch);
        return ch;
    }

    // === internal stuff ===

    /**
     * Internal initialization, called from {@link org.gotti.wurmunlimited.modloader.ModLoader#loadModsFromModDir}
     */
    public static void init() {
        final ClassPool classPool = HookManager.getInstance().getClassPool();
        try {
            CtClass ctServerConnection = classPool.getCtClass("com.wurmonline.client.comm.SimpleServerConnectionClass");

            ctServerConnection.getMethod("reallyHandle", "(ILjava/nio/ByteBuffer;)V").instrument(new ExprEditor() {
                private boolean first = true;

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("get") && first) {
                        m.replace("$_ = $proceed($$); " +
                                "if ($_ == " + ModCommConstants.CMD_MODCOMM + ") {" +
                                "   org.gotti.wurmunlimited.modcomm.ModCommHandler.handlePacket(bb);" +
                                "   return;" +
                                "}");
                        first = false;
                    }
                }
            });

            ctServerConnection.getMethod("reallyHandleCmdMessage", "(Ljava/nio/ByteBuffer;)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("textMessage")) {
                        m.replace("if (title.equals(\":Event\") && message.startsWith(org.gotti.wurmunlimited.modcomm.ModCommConstants.MARKER)) {" +
                                "org.gotti.wurmunlimited.modcomm.ModCommHandler.startHandshake();" +
                                "} else $proceed($$);");
                    }
                }
            });

        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException("Error initializing ModComm", e);
        }
    }

    static SocketConnection getServerConnection() {
        try {
            SimpleServerConnectionClass serverConnection = ModClient.getWorld().getServerConnection();
            if (fConnection == null) {
                fConnection = SimpleServerConnectionClass.class.getDeclaredField("connection");
                fConnection.setAccessible(true);
            }
            return (SocketConnection) fConnection.get(serverConnection);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // === Logging ===

    static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

}
