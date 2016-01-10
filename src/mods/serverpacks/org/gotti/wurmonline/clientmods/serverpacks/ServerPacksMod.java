package org.gotti.wurmonline.clientmods.serverpacks;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;


public class ServerPacksMod implements WurmMod, Initable {
	
	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());

	@Override
	public void init() {
	
		try {
			//com.wurmonline.client.renderer.gui.ChatManagerManager.textMessage(String, float, float, float, String, boolean)
			
			ClassPool classPool = HookManager.getInstance().getClassPool();
			String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					classPool.get("java.lang.String"),
					CtClass.floatType,
					CtClass.floatType,
					CtClass.floatType,
					classPool.get("java.lang.String"),
					CtClass.booleanType
			});
			
			HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.ChatManagerManager", "textMessage", descriptor, new InvocationHandlerFactory() {
				
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if (":mod:serverpacks".equals(args[0])) {
								logger.log(Level.INFO, "Got server pack " + args[4]);
								
								installServerPack(String.valueOf(args[4]));
								
								return null;
							} else {
								return method.invoke(proxy, args);
							}
						}
					};
				}
			});
			
			descriptor = Descriptor.ofMethod(classPool.get("com.wurmonline.client.resources.ResourceUrl"), new CtClass[] {
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
									String[] s = string.split(" ", 4);
									if (s.length == 4) {
										installServerPack(s[3]);
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
	

	private void installServerPack(String packEntry) {
		String[] parts = packEntry.split(":", 2);
		if (parts.length == 2) {
			String packId = parts[0];
			String packUrl = parts[1];
			if (!checkForExistingPack(packId)) {
				downloadPack(packUrl, packId);
			} else {
				enableDownloadedPack(packId);
			}
		} else {
			logger.log(Level.WARNING, "Failed to extract pack id from " + packEntry);
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
