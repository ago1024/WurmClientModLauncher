package org.gotti.wurmunlimited.modsupport.packs;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.StatusEffect.StatusEffectXmlParser;
import com.wurmonline.client.renderer.gui.StatusEffectComponent;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modsupport.ModClient;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CellRenderer;
import com.wurmonline.client.renderer.cell.PlayerCellRenderable;
import com.wurmonline.client.resources.ResourceUrl;
import com.wurmonline.client.resources.Resources;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.shared.constants.IconConstants;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Helper for adding additional packs on demand.
 */
public class ModPacks {

	public enum Options {
		/**
		 * Insert the pack as the first pack, overwriting entries from the
		 * default packs.
		 */
		PREPEND,

		/**
		 * Do not reload packs.
		 */
		NORELOAD,

		/**
		 * Don't read mapsunlimited.xml.
		 */
		NOMAPS,

		/**
		 * Don't read armor.xml.
		 */
		NOARMOR
	}

	private static Logger logger = Logger.getLogger(ModPacks.class.getName());

	private static Class<?> jarPackClass;

	private static Constructor<?> jarPackConstructor;

	private static Method jarPackInit;

	private static Method jarPackGetResource;

	private static Field jarPackJarFile;

	private static Field resourceResolvedResources;

	private static Field resourcesUnresolvedResources;

	private static Field resourcesPacks;

	private static Field cellRendererTickRenderables;

	private static Field playerCellRenderableTextureDirty;

	private static Field playerBodyRenderableTextureDirty;

	private static Class<?> jarResourceUrl;

	private static Field jarResourceUrlPack;

	// Track JarPacks created for a absolute pack file locations
	private static HashMap<String, Object> addedPacks = new HashMap<>();

	public static void preInit() {
		try {
			// com.wurmonline.client.resources.textures.PlayerTextureBuilder.loadImage(ResourceUrl, String)
			
			ClassPool classPool = HookManager.getInstance().getClassPool();
			
			ExprEditor modArmorDeriveEditor = new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if ("com.wurmonline.client.resources.ResourceUrl".equals(m.getClassName()) && "derive".equals(m.getMethodName())) {
						StringBuilder code = new StringBuilder();
						code.append("{\n");
						code.append("	com.wurmonline.client.resources.ResourceUrl res = org.gotti.wurmunlimited.modsupport.packs.ModArmor.getArmorTexture($$);\n");
						code.append("	$_ = res != null ? res : $proceed($$);\n");
						code.append("}\n");
						m.replace(code.toString());
					}
				}
			};
			
			classPool.get("com.wurmonline.client.resources.textures.PlayerTextureBuilderGL").getMethod("generateTexture", Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[0])).instrument(modArmorDeriveEditor);
			classPool.get("com.wurmonline.client.renderer.cell.PlayerTexture$PlayerBodyTextureLoader").getMethod("run", Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[0])).instrument(modArmorDeriveEditor);
			
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
	}

	public static void init() {
		try {
			jarPackClass = Class.forName("com.wurmonline.client.resources.JarPack");
			jarPackConstructor = jarPackClass.getDeclaredConstructor(new Class<?>[] { File.class });
			jarPackInit = jarPackClass.getSuperclass().getDeclaredMethod("init", new Class<?>[] { Resources.class });
			jarPackGetResource = jarPackClass.getSuperclass().getDeclaredMethod("getResource", new Class<?>[] { String.class });
			jarPackJarFile = ReflectionUtil.getField(jarPackClass, "jarFile");
			resourceResolvedResources = ReflectionUtil.getField(Resources.class, "resolvedResources");
			resourcesUnresolvedResources = ReflectionUtil.getField(Resources.class, "unresolvedResources");
			resourcesPacks = ReflectionUtil.getField(Resources.class, "packs");
			cellRendererTickRenderables = ReflectionUtil.getField(CellRenderer.class, "tickRenderables");
			playerBodyRenderableTextureDirty = ReflectionUtil.getField(PlayerBodyRenderable.class, "textureDirty");
			playerCellRenderableTextureDirty = ReflectionUtil.getField(PlayerCellRenderable.class, "textureDirty");
			jarResourceUrl = Class.forName("com.wurmonline.client.resources.PackResourceUrl");
			jarResourceUrlPack = ReflectionUtil.getField(jarResourceUrl, "pack");
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

	public static Set<Options> getOptionSet(Options[] options) {
		if (options == null || options.length == 0) {
			return Collections.<Options> emptySet();
		} else {
			return EnumSet.copyOf(Arrays.asList(options));
		}
	}

	/**
	 * Add a new pack file
	 * 
	 * @param jarFile Pack file
	 * @param options options
	 * @return true if the pack was added successfully
	 */
	public static boolean addPack(File jarFile, Options... options) {

		Set<Options> o = getOptionSet(options);

		try {
			Object jarPack = initPack(jarFile);

			addPack(jarPack, o);

			addedPacks.put(jarFile.getAbsoluteFile().toString(), jarPack);

			updateServerData(jarPack, o);

			return true;
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
			return false;
		}
	}
	
	/**
	 * Close a pack. Closes a pack if the file has been registered with addPack before.
	 * 
	 * @param file
	 */
	public static void closePack(File file) {
		try {
			Resources resources = WurmClientBase.getResourceManager();
			
			Object addedPack = addedPacks.remove(file.getAbsoluteFile().toString());
			if (addedPack == null) {
				// nothing to do
				return;
			}
			
			// close the JarFile
			JarFile jarFile = ReflectionUtil.<JarFile>getPrivateField(addedPack, jarPackJarFile);
			jarFile.close();
			
			// remove the pack from the ResourceManager
			List<Object> packs = ReflectionUtil.<List<Object>> getPrivateField(resources, resourcesPacks);
			packs.remove(addedPack);
			
			// Cleanup resolved resources
			Map<String, ResourceUrl> resolved = ReflectionUtil.<Map<String, ResourceUrl>> getPrivateField(resources, resourceResolvedResources);
			Map<String, ResourceUrl> oldResolved = new HashMap<>(resolved);

			for (java.util.Map.Entry<String, ResourceUrl> entry : oldResolved.entrySet()) {
				ResourceUrl oldUrl = entry.getValue();
				if (jarResourceUrl.isInstance(oldUrl)) {
					Object urlPack = ReflectionUtil.getPrivateField(oldUrl, jarResourceUrlPack);
					// old resource is from the pack to close
					if (urlPack == addedPack) {
						// remove the resolved entry
						resolved.remove(entry.getKey());
						// resolve the entry from the remaining packs
						resources.getResource(entry.getKey());
					}
				}
			}
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
	}

	private static Object initPack(File file) throws SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		boolean contructorWasAccessible = jarPackConstructor.isAccessible();
		boolean initWasAccessible = jarPackInit.isAccessible();

		try {
			jarPackConstructor.setAccessible(true);
			jarPackInit.setAccessible(true);

			Resources resources = WurmClientBase.getResourceManager();

			Object jarPack = jarPackConstructor.newInstance(file);
			jarPackInit.invoke(jarPack, resources);

			return jarPack;

		} finally {
			jarPackInit.setAccessible(initWasAccessible);
			jarPackConstructor.setAccessible(contructorWasAccessible);
		}
	}

	private static void addPack(Object jarPack, Set<Options> options) throws IllegalArgumentException, IllegalAccessException, ClassCastException {

		Resources resources = WurmClientBase.getResourceManager();

		List<Object> packs = ReflectionUtil.<List<Object>> getPrivateField(resources, resourcesPacks);

		synchronized (resources) {

			if (options.contains(Options.PREPEND)) {
				packs.add(0, jarPack);
			} else {
				packs.add(jarPack);
			}
			
			if (!options.contains(Options.NORELOAD)) {
				reloadPacks();
			}
		}
	}

	private static void reloadPacks() throws IllegalArgumentException, IllegalAccessException, ClassCastException {
		Resources resources = WurmClientBase.getResourceManager();

		Set<String> unresolved = ReflectionUtil.<Set<String>> getPrivateField(resources, resourcesUnresolvedResources);
		Map<String, ResourceUrl> resolved = ReflectionUtil.<Map<String, ResourceUrl>> getPrivateField(resources, resourceResolvedResources);

		Map<String, ResourceUrl> oldResolved = new HashMap<>(resolved);
		unresolved.clear();
		resolved.clear();

		boolean reloadIcons = false;
		boolean reloadStatusEffects = false;
		for (java.util.Map.Entry<String, ResourceUrl> entry : oldResolved.entrySet()) {

			ResourceUrl oldUrl = entry.getValue();
			ResourceUrl newUrl = resources.getResource(entry.getKey());

			if (newUrl != null && !newUrl.equals(oldUrl)) {

				if (Arrays.asList(IconConstants.ICON_SHEET_FILE_NAMES).contains(entry.getKey())) {
					reloadIcons = true;
				} else if ("buff.xml".equals(entry.getKey())) {
					reloadStatusEffects = true;
				} else {
					ResourceTextureLoader.reload(oldUrl, newUrl);
				}
			}
		}

		if (reloadIcons) {
			IconLoader.initIcons();
			IconLoader.clear();
		}

		if (reloadStatusEffects) {
			try {
				final WurmClientBase clientBase = ReflectionUtil.getPrivateField(WurmClientBase.class, ReflectionUtil.getField(WurmClientBase.class, "clientObject"));
				final HeadsUpDisplay hud = ReflectionUtil.getPrivateField(clientBase, ReflectionUtil.getField(WurmClientBase.class, "hud"));
				Object statusEffectLibrary = StatusEffectXmlParser.loadXml();
				ReflectionUtil.setPrivateField(hud.getStatusEffectComponent(), ReflectionUtil.getField(StatusEffectComponent.class, "statusEffectLibrary"), statusEffectLibrary);
			} catch (NoSuchFieldException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				throw new HookException(e);
			}
		}
	}

	private static void updateServerData(Object jarPack, Set<Options> options) {

		if (!options.contains(Options.NOMAPS)) {
			updateMaps(jarPack);
		}

		if (!options.contains(Options.NOARMOR)) {
			updateArmor(jarPack);
		}

	}

	public static ResourceUrl getResource(Object jarPack, String resourceName) {
		try {
			return ReflectionUtil.<ResourceUrl> callPrivateMethod(jarPack, jarPackGetResource, resourceName);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	private static void updateMaps(Object jarPack) {
		ResourceUrl mapsUnlimited = getResource(jarPack, "mapunlimited");
		if (mapsUnlimited != null) {
			new MapsLoader().load(mapsUnlimited);

		}
	}

	private static void updateArmor(Object jarPack) {
		ResourceUrl armor = getResource(jarPack, "armor.xml");
		if (armor != null) {
			new ArmorLoader().load(armor);
			refreshPlayerModels();
		}
	}

	public static void refreshPlayerModels() {
		try {
			CellRenderer cellRenderer = ModClient.getWorld().getCellRenderer();
			List<CellRenderable> renderables = ReflectionUtil.getPrivateField(cellRenderer, cellRendererTickRenderables);
			if (renderables != null) {
				for (CellRenderable renderable : renderables) {
					if (renderable instanceof PlayerCellRenderable) {
						ReflectionUtil.setPrivateField(renderable, playerCellRenderableTextureDirty, true);
					}
					if (renderable instanceof PlayerBodyRenderable) {
						ReflectionUtil.setPrivateField(renderable, playerBodyRenderableTextureDirty, true);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

}
