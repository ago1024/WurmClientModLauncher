package org.gotti.wurmunlimited.modsupport.packs;

import java.util.HashMap;
import java.util.Map;

import com.wurmonline.client.options.Options;
import com.wurmonline.client.resources.ResourceUrl;

public class ModArmor {
	
	private static Map<String, ResourceUrl> packTextures = new HashMap<>();
	
	public static void addArmorTexture(String textureName, ResourceUrl baseUrl) {
		packTextures.put(textureName, baseUrl);
	}
	
	public static ResourceUrl getArmorTexture(String textureName) {
		ResourceUrl pack = getArmorTexturePack(textureName);
		if (pack != null) {
			return pack.derive(textureName);
		} else {
			return null;
		}
	}
	
	public static ResourceUrl getArmorTexturePack(String textureName) {
		ResourceUrl url = packTextures.get(textureName);
		if (url == null) {
			int pos = textureName.indexOf('/');
			if (pos != -1 && textureName.substring(pos).startsWith("/" + Options.getTextureSize(Options.playerTextureSize) + "/")) {
				String nameWithoutResolution = textureName.substring(0, pos) + textureName.substring(textureName.indexOf('/', pos + 1));
				return getArmorTexturePack(nameWithoutResolution);
			} else {
				return null;
			}
		} else {
			return url;
		}
	}
}
