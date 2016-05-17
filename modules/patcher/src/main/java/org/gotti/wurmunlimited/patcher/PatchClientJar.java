package org.gotti.wurmunlimited.patcher;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public class PatchClientJar {

	private static Logger logger = Logger.getLogger(PatchClientJar.class.getName());
	
	private void run() throws NotFoundException, CannotCompileException, IOException {
		
		
		if (!Files.exists(Paths.get("modlauncher.jar"))) {
			logger.info("modlauncher.jar does not exist");
			return;
		}
		
		Path clientJar = Paths.get("client.jar");
		try (FileSystem clientFS = FileSystems.newFileSystem(URI.create("jar:" + clientJar.toUri()), new HashMap<>())) {
			Path path = clientFS.getPath("com/wurmonline/client/WurmClientBase.class");
			if (Files.exists(path)) {
				logger.info("client.jar contains Wurm Unlimited class files. Moving to client-patched.jar");
				Files.copy(Paths.get("client.jar"), Paths.get("client-patched.jar"), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		Files.copy(Paths.get("modlauncher.jar"), Paths.get("client.jar"), StandardCopyOption.REPLACE_EXISTING);
		logger.info("Client is now patched");
	}
	
	public static void main(String[] args) {

		try {
			new PatchClientJar().run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
