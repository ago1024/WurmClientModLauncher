package org.gotti.wurmunlimited.clientlauncher;

import java.nio.file.Paths;
import java.util.List;

import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

public class DelegatedLauncher {
	
	public static void main(String[] args) {
		
		try {
			List<WurmMod> wurmMods = new ModLoader().loadModsFromModDir(Paths.get("mods"));
			
			
			String[] classes = {
					"com.wurmonline.client.launcherfx.WurmMain"
			};
			
			for (String classname : classes) {
				
				try {
					HookManager.getInstance().getLoader().run(classname, args);
					return;
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
			
			throw new ClassNotFoundException("com.wurmonline.client.launcherfx.WurmMain");
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
