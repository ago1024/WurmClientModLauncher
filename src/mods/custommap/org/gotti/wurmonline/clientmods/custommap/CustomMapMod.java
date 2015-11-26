package org.gotti.wurmonline.clientmods.custommap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.WorldMap;
import com.wurmonline.client.renderer.gui.maps.ClusterMap;
import com.wurmonline.client.renderer.gui.maps.Map;

public class CustomMapMod implements WurmMod, Initable {

	private static Logger logger = Logger.getLogger(CustomMapMod.class.getName());

	@Override
	public void init() {
		
		//com.wurmonline.client.game.World.setServerInformation(int, boolean, String)
		HookManager.getInstance().registerHook("com.wurmonline.client.game.World", "setServerInformation", null, new InvocationHandlerFactory() {
			
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						
						Object result = method.invoke(proxy, args);

						try {
							
							String mapName = String.valueOf(args[2]);
							final String textureName = "map." + mapName.toLowerCase(Locale.ROOT);
						
							WorldMap worldMap = ((World)proxy).getHud().getWorldMap();
							ClusterMap currentCluster = ReflectionUtil.<ClusterMap>getPrivateField(worldMap, ReflectionUtil.getField(WorldMap.class, "currentCluster"));
							
							List<Map> maps = ReflectionUtil.<List<Map>>getPrivateField(currentCluster, ReflectionUtil.getField(ClusterMap.class, "serverMaps"));
							
							for (Map map : maps) {
								if (map.getTextureName().equalsIgnoreCase(textureName)) {
									return result;
								}
							}
							
							Map map = new Map(mapName, textureName, false, 620, 620);
							maps.add(map);
							
							return result;
						} catch (Exception e) {
							logger.log(Level.WARNING, e.getMessage(), e);
							return result;
						}
						
					}
				};
				
			}
		});
		
	}

}
