package org.gotti.wurmonline.clientmods.serverpacks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
import org.gotti.wurmunlimited.modsupport.packs.ModPacks;
import org.gotti.wurmunlimited.modcomm.IChannelListener;
import org.gotti.wurmunlimited.modcomm.ModComm;
import org.gotti.wurmunlimited.modcomm.PacketReader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;


public class ServerPacksMod implements WurmMod, Initable {
	
	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());

	@Override
	public void init() {
	
		try {
			ModComm.registerChannel("ago.serverpacks", new IChannelListener() {
				@Override
				public void handleMessage(ByteBuffer message) {
					try (PacketReader reader = new PacketReader(message)) {
						int n = reader.readInt();
						while (n-- > 0) {
							String packId = reader.readUTF();
							String uri = reader.readUTF();
							logger.log(Level.INFO, String.format("Got server pack %s (%s)", packId, uri));
							installServerPack(packId, uri);
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
			
			ClassPool classPool = HookManager.getInstance().getClassPool();
			
			String descriptor = Descriptor.ofMethod(classPool.get("com.wurmonline.client.resources.ResourceUrl"), new CtClass[] {
					classPool.get("java.lang.String")
			});
			
			
			HookManager.getInstance().registerHook("com.wurmonline.client.resources.Resources", "findResource", descriptor, new InvocationHandlerFactory() {
				
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							synchronized (proxy) {
								method.setAccessible(true);
								return method.invoke(proxy, args);
							}
						}
					};
				}
			});
			
			// com.wurmonline.client.console.WurmConsole.handleInput2(String, boolean)
			descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					classPool.get("java.lang.String"),
					classPool.get("boolean")
			});
			
			
			HookManager.getInstance().registerHook("com.wurmonline.client.console.WurmConsole", "handleInput2", descriptor, new InvocationHandlerFactory() {
				
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							synchronized (proxy) {
								String string = String.valueOf(args[0]);
								if (string.startsWith("mod serverpacks installpack")) {
									String[] s = string.split(" ", 5);
									if (s.length == 5) {
										installServerPack(s[3], s[4]);
									}
									return null;
								}
								
								return method.invoke(proxy, args);
							}
						}
					};
				}
			});
			
			
		} catch (NotFoundException e) {
			throw new HookException(e);
		}
	}
	

	private void installServerPack(String packId, String packUrl) {
		if (!checkForExistingPack(packId)) {
			downloadPack(packUrl, packId);
		} else {
			enableDownloadedPack(packId);
		}
	}

	private void enableDownloadedPack(String packId) {
		File file = Paths.get("packs", getPackName(packId)).toFile();
		
		if (ModPacks.addPack(file)) {
			logger.log(Level.INFO, "Added server pack " + packId);
		}
	}

	private void downloadPack(String packUrl, String packId) {
		PackDownloader downloader = new PackDownloader(packUrl, packId) {
			@Override
			protected void done(String packId) {
				enableDownloadedPack(packId);
			}
		};
		new Thread(downloader).start();
	}

	private boolean checkForExistingPack(String packId) {
		Path path = Paths.get("packs", getPackName(packId));
		if (Files.isRegularFile(path)) {
			return true;
		}
		return false;
	}

	private String getPackName(String packId) {
		return packId + ".jar";
	}
}
