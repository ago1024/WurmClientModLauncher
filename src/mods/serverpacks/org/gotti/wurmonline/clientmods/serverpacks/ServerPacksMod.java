package org.gotti.wurmonline.clientmods.serverpacks;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.resources.Resources;


public class ServerPacksMod implements WurmMod, Initable {
	
	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());

	@Override
	public void init() {
	
		try {
			//com.wurmonline.client.renderer.gui.ChatManagerManager.textMessage(String, float, float, float, String, boolean)
			
			String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
					HookManager.getInstance().getClassPool().get("java.lang.String"),
					CtClass.floatType,
					CtClass.floatType,
					CtClass.floatType,
					HookManager.getInstance().getClassPool().get("java.lang.String"),
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
			
			descriptor = Descriptor.ofMethod(HookManager.getInstance().getClassPool().get("com.wurmonline.client.resources.ResourceUrl"), new CtClass[] {
					HookManager.getInstance().getClassPool().get("java.lang.String")
			});
			
			
			HookManager.getInstance().registerHook("com.wurmonline.client.resources.Resources", "findResource", descriptor, new InvocationHandlerFactory() {
				
				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							synchronized (proxy) {
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
		try {
			File file = Paths.get("packs", getPackName(packId)).toFile();
			
			Class<?> jarPackClass = Class.forName("com.wurmonline.client.resources.JarPack");
			Constructor<?> constructor = jarPackClass.getDeclaredConstructor(new Class<?>[] { File.class });
			Method init = jarPackClass.getSuperclass().getDeclaredMethod("init", new Class<?>[] { Resources.class });
					
			
			boolean contructorWasAccessible = constructor.isAccessible();
			boolean initWasAccessible = init.isAccessible();
			
			try {
				constructor.setAccessible(true);
				init.setAccessible(true);
				
				Resources resources = WurmClientBase.getResourceManager(); 
				
				Object jarPack = constructor.newInstance(file);
				init.invoke(jarPack, resources);
				
				List<Object> packs = ReflectionUtil.<List<Object>>getPrivateField(resources, ReflectionUtil.getField(Resources.class, "packs"));
				Set<String> unresolved = ReflectionUtil.<Set<String>>getPrivateField(resources, ReflectionUtil.getField(Resources.class, "unresolvedResources"));
				
				synchronized (resources) {
					packs.add(jarPack);
					unresolved.clear();
				}
				
			} finally {
				init.setAccessible(initWasAccessible);
				constructor.setAccessible(contructorWasAccessible);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
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
