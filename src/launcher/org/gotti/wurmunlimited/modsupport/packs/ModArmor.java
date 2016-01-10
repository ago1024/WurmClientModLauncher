package org.gotti.wurmunlimited.modsupport.packs;

import java.util.HashMap;
import java.util.Map;

import com.wurmonline.client.resources.ResourceUrl;

public class ModArmor {
	
	private static Map<String, ResourceUrl> packTextures = new HashMap<>();
	
	public static void addArmorTexture(String textureName, ResourceUrl baseUrl) {
		packTextures.put(textureName, baseUrl);
	}
	
	public static ResourceUrl getArmorTexturePack(String textureName) {
		return packTextures.get(textureName);
	}
}
