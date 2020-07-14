package org.gotti.wurmonline.clientmods.serverpacks;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.gotti.wurmunlimited.modcomm.Channel;
import org.gotti.wurmunlimited.modcomm.IChannelListener;
import org.gotti.wurmunlimited.modcomm.ModComm;
import org.gotti.wurmunlimited.modcomm.PacketReader;
import org.gotti.wurmunlimited.modcomm.PacketWriter;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.gotti.wurmunlimited.modsupport.ModClient;
import org.gotti.wurmunlimited.modsupport.console.ConsoleListener;
import org.gotti.wurmunlimited.modsupport.console.ModConsole;
import org.gotti.wurmunlimited.modsupport.packs.ModPacks;
import org.gotti.wurmunlimited.modsupport.packs.ModPacks.Options;

import com.wurmonline.client.renderer.ItemColorsXml;
import com.wurmonline.client.renderer.effects.CustomParticleEffectXml;
import com.wurmonline.client.renderer.terrain.TilePropertiesXml;
import com.wurmonline.client.renderer.terrain.TerrainTexture;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.CtMethod;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ServerPacksMod implements WurmClientMod, Initable, PreInitable, ConsoleListener {
	private static final Options[] OPTIONS_DEFAULT = new Options[] {};

	private static final Options[] OPTIONS_PREPEND = new Options[] { Options.PREPEND };

	private static final String CONSOLE_PREFIX = "mod serverpacks";

	private static final byte CMD_REFRESH = 0x01;

	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());
	private Channel channel = null;

	@Override
	public void preInit() {
		try {
			ClassPool classPool = HookManager.getInstance().getClassPool();

			CtClass ctResources = classPool.get("com.wurmonline.client.resources.Resources");
			CtClass ctPack = classPool.get("com.wurmonline.client.resources.Pack");
			CtClass ctPackResourceUrl = classPool.get("com.wurmonline.client.resources.PackResourceUrl");

			CtMethod findPackMethod = new CtMethod(ctPack, "findPack", new CtClass[]{classPool.get("java.lang.String")}, ctResources);
			findPackMethod.setBody("{" +
					"        for (java.util.Iterator iterator = this.packs.iterator(); iterator.hasNext(); ) {" +
					"            com.wurmonline.client.resources.Pack pack = (com.wurmonline.client.resources.Pack) iterator.next();" +
					"            if (pack.getName().equals($1)) return pack;" +
					"        }" +
					"        return null;" +
					"}"
			);
			ctResources.addMethod(findPackMethod);

			ctPack.getMethod("init", "(Lcom/wurmonline/client/resources/Resources;)V")
					.instrument(new ExprEditor() {
						@Override
						public void edit(MethodCall m) throws CannotCompileException {
							if (m.getMethodName().equals("exists")) {
								m.replace("$_ = $0.getFilePath().startsWith(\"~\") || $proceed();");
							}
						}
					});

			ctPack.getMethod("getResource", "(Ljava/lang/String;)Lcom/wurmonline/client/resources/ResourceUrl;")
					.insertAfter("if ($_ != null && $_.getFilePath().startsWith(\"~\")) {" +
							"    	int sep = $_.getFilePath().indexOf('/');" +
							"       com.wurmonline.client.resources.Pack pack = com.wurmonline.client.WurmClientBase.getResourceManager()" +
							"			.findPack($_.getFilePath().substring(1,sep));" +
							"       if (pack!=null)" +
							"       	$_ = new com.wurmonline.client.resources.PackResourceUrl(pack, $_.getFilePath().substring(sep+1));" +
							"     };");

			ctPackResourceUrl.getMethod("derive", "(Ljava/lang/String;)Lcom/wurmonline/client/resources/PackResourceUrl;")
					.insertBefore("if ($1.startsWith(\"~\")) {\n" +
							"			int sep = $1.indexOf('/');\n" +
							"           com.wurmonline.client.resources.Pack pack = com.wurmonline.client.WurmClientBase.getResourceManager()" +
							"				.findPack(newFilename.substring(1,sep));" +
							"           if (pack!=null) {" +
							"				com.wurmonline.client.resources.PackResourceUrl nurl = new com.wurmonline.client.resources.PackResourceUrl(pack, $1.substring(sep+1));" +
							"				if (!nurl.exists()) throw com.wurmonline.client.GameCrashedException.forFailure(\"Derived cross-pack resource \" + nurl + \" does not exist (source \" + this + \")\");" +
							"           	return nurl;" +
							"			}" +
							"		}");

		} catch (NotFoundException | CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init() {
	
		try {
			channel = ModComm.registerChannel("ago.serverpacks", new IChannelListener() {
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
						refreshModels();
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
			
			
			ModConsole.addConsoleListener(this);
			
		} catch (NotFoundException e) {
			throw new HookException(e);
		}
	}
	
	private void installServerPack(String packId, String packUri) {
		try {
			URL packUrl = new URL(packUri);
			boolean force = Boolean.parseBoolean(splitQuery(packUrl).getOrDefault("force", emptyList()).stream().map(v -> v == null ? "true" : v).reduce((a, b) -> b).orElse("false"));
			if (force || !checkForExistingPack(packId)) {
				downloadPack(packUrl, packId);
			} else {
				enableDownloadedPack(packId, packUrl);
			}
		} catch (MalformedURLException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private Map<String, List<String>> splitQuery(URL url) {
		if (url == null || url.getQuery() == null || url.getQuery().isEmpty()) {
			return Collections.emptyMap();
		}
		return Arrays.stream(url.getQuery().split("&"))
				.map(this::splitQueryParameter)
				.collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
	}

	private SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
		try {
			final int idx = it.indexOf("=");
			final String key = idx > 0 ? it.substring(0, idx) : it;
			final String value = idx > 0 && it.length() > idx + 1 ? URLDecoder.decode(it.substring(idx + 1), "UTF-8") : null;
			return new SimpleImmutableEntry<>(key, value);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void enableDownloadedPack(String packId, URL packUrl) {
		File file = Paths.get("packs", getPackName(packId)).toFile();
		
		boolean prepend = Boolean.parseBoolean(splitQuery(packUrl).getOrDefault("prepend", emptyList()).stream().map(v -> v == null ? "true" : v).reduce((a, b) -> b).orElse("false"));
		if (ModPacks.addPack(file, prepend ? OPTIONS_PREPEND : OPTIONS_DEFAULT)) {
			logger.log(Level.INFO, "Added server pack " + packId);
			CustomParticleEffectXml.reloadParticlesFile();
			ItemColorsXml.reloadItemColors(ModClient.getWorld());
			TilePropertiesXml.reloadTiles();
			TerrainTexture.reloadNormalMaps();
		}
	}

	private void downloadPack(URL packUrl, String packId) {
		PackDownloader downloader = new PackDownloader(packUrl, packId) {
			@Override
			protected void done(String packId) {
				enableDownloadedPack(packId, packUrl);
				refreshModels();
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

	/**
	 * Send CMD_REFRESH to the server for a complete refresh of all creatures and models
	 */
	private void refreshModels() {
		if (channel != null) {
			try (PacketWriter writer = new PacketWriter()) {
				writer.writeByte(CMD_REFRESH);
				channel.sendMessage(writer.getBytes());
			} catch (IOException e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}
	
	private void handleConsoleInput(String string) {
		StringTokenizer tokenizer = new StringTokenizer(string, " ");
		if (tokenizer.hasMoreTokens()) {
			String cmd = tokenizer.nextToken();
			switch (cmd.toLowerCase()) {
			case "installpack":
				String id = null;
				String url = null;
				if (tokenizer.hasMoreTokens())
					id = tokenizer.nextToken();
				if (tokenizer.hasMoreTokens())
					url = tokenizer.nextToken();
				if (id != null && url != null)
					installServerPack(id, url);
				else
					printConsoleHelp();
				break;
			case "refresh":
				refreshModels();
				break;
			default:
				printConsoleHelp();
				break;
			}
		} else {
			printConsoleHelp();
		}
		String[] s = string.split(" ", 5);
		if (s.length == 5) {
			installServerPack(s[3], s[4]);
			refreshModels();
		}
		
	}
	
	private void printConsoleHelp() {
		System.out.println("Mod serverpacks console usage:");
		System.out.println("mod serverpacks installpack <packid> <url>");
		System.out.println("	Load a serverpack");
		System.out.println("mod serverpacks refresh");
		System.out.println("	Refresh the models");
	}


	@Override
	public boolean handleInput(String string, Boolean silent) {
		if (string != null && string.startsWith(CONSOLE_PREFIX)) {
			handleConsoleInput(string.substring(CONSOLE_PREFIX.length()).trim());
			return true;
		}
		return false;
	}
}
