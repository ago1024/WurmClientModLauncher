package com.wurmonline.client.launcherfx;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WurmMain {

	public static void main(String[] args) {
		
		try {
			initLogger();
			
			URL[] urls = new URL[] {
					//Paths.get("modlauncher.jar").toUri().toURL(),
					Paths.get("javassist.jar").toUri().toURL()
			};
			try (URLClassLoader urlClassLoader = new URLClassLoader(urls)) {
				Class<?> launcher = urlClassLoader.loadClass("org.gotti.wurmunlimited.clientlauncher.ClientLauncher");
				Method method = launcher.getDeclaredMethod("main", new Class[] { String[].class });
				method.invoke(launcher, new Object[] { args });
			}
		} catch (Exception e) {
			Logger.getLogger("org.gotti.wurmunlimited.Loader").log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
			}
			System.exit(-1);
		}
	}

	private static void initLogger() throws SecurityException, IOException {
		//  Use externally configured loggers
		if (System.getProperty("java.util.logging.config.file") != null) {
			return;
		}
		if (System.getProperty("java.util.logging.config.class") != null) {
			return;
		}
		
		// Use a provider logging.properties file
		Path loggingPropertiesFile = Paths.get("logging.properties");
		if (Files.isRegularFile(loggingPropertiesFile)) {
			System.setProperty("java.util.logging.config.file", loggingPropertiesFile.toString());
			return;
		}
	}

}
