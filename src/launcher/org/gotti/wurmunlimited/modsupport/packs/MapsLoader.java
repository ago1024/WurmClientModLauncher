package org.gotti.wurmunlimited.modsupport.packs;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modsupport.ModClient;

import com.sun.webkit.graphics.Ref;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.WorldMap;
import com.wurmonline.client.renderer.gui.maps.ClusterMap;
import com.wurmonline.client.renderer.gui.maps.MapXml;
import com.wurmonline.client.resources.ResourceUrl;
import com.wurmonline.shared.xml.XmlNode;
import com.wurmonline.shared.xml.XmlParser;

class MapsLoader {

	private static Logger logger;

	private static Method mapXmlLoadFromRootNode;

	static {
		logger = Logger.getLogger(MapsLoader.class.getName());
		try {
			mapXmlLoadFromRootNode = ReflectionUtil.getMethod(MapXml.class, "loadFromRootNode");
		}
		catch (NoSuchMethodException | SecurityException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

	private WorldMap worldMap;

	public MapsLoader() {
		worldMap = ModClient.getHeadsUpDisplay().getWorldMap();
	}

	public void load(ResourceUrl mapsUnlimited) {

		XmlNode rootNode = null;

		try (InputStream input = mapsUnlimited.openStream()) {
			rootNode = XmlParser.parse(input);
		}

		catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		if (rootNode != null) {
			try {
				HashMap<Integer, ClusterMap> clusterMapList = ReflectionUtil.callPrivateMethod(MapXml.class, mapXmlLoadFromRootNode, rootNode, worldMap);

				Runnable task = new Runnable() {

					public void run() {
						try {
							if (clusterMapList.size() > 0) {
								for (ClusterMap map : clusterMapList.values()) {
									ReflectionUtil.setPrivateField(worldMap, ReflectionUtil.getField(WorldMap.class, "currentCluster"), map);
									map.load();
								}
								
								World world = ModClient.getWorld();
								int cluster = ReflectionUtil.getPrivateField(world, ReflectionUtil.getField(World.class, "cluster"));
								String serverName = ReflectionUtil.getPrivateField(world, ReflectionUtil.getField(World.class, "serverName"));
								
								worldMap.setStartingArea(cluster, serverName);
							}
						}
						catch (IllegalAccessException | IllegalArgumentException | ClassCastException | NoSuchFieldException e) {
							logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				};
				
				ModClient.runTask(task);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

}
