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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.resources.ResourceUrl;
import com.wurmonline.client.resources.Resources;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.shared.constants.IconConstants;

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

	private static Field resourceResolvedResources;

	private static Field resourcesUnresolvedResources;

	private static Field resourcesPacks;

	public static void init() {
		try {
			jarPackClass = Class.forName("com.wurmonline.client.resources.JarPack");
			jarPackConstructor = jarPackClass.getDeclaredConstructor(new Class<?>[] { File.class });
			jarPackInit = jarPackClass.getSuperclass().getDeclaredMethod("init", new Class<?>[] { Resources.class });
			jarPackGetResource = jarPackClass.getSuperclass().getDeclaredMethod("getResource", new Class<?>[] { String.class });
			resourceResolvedResources = ReflectionUtil.getField(Resources.class, "resolvedResources");
			resourcesUnresolvedResources = ReflectionUtil.getField(Resources.class, "unresolvedResources");
			resourcesPacks = ReflectionUtil.getField(Resources.class, "packs");
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

	public static boolean addPack(File jarFile, Options... options) {

		Set<Options> o = getOptionSet(options);

		try {
			Object jarPack = initPack(jarFile);

			addPack(jarPack, o);

			if (!o.contains(Options.NORELOAD)) {
				reloadPacks();
			}

			updateServerData(jarPack, o);

			return true;
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
			return false;
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
		for (java.util.Map.Entry<String, ResourceUrl> entry : oldResolved.entrySet()) {

			ResourceUrl oldUrl = entry.getValue();
			ResourceUrl newUrl = resources.getResource(entry.getKey());

			if (newUrl != null && !newUrl.equals(oldUrl)) {

				if (Arrays.asList(IconConstants.ICON_SHEET_FILE_NAMES).contains(entry.getKey())) {
					reloadIcons = true;
				} else {
					ResourceTextureLoader.reload(oldUrl, newUrl);
				}
			}
		}

		if (reloadIcons) {
			IconLoader.initIcons();
			IconLoader.clear();
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
		// TODO Auto-generated method stub

	}

}
