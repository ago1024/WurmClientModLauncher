package org.gotti.wurmonline.clientmods.serverpacks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PackDownloader implements Runnable {

	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());

	private String packUrl;

	private String packId;

	public PackDownloader(String packUrl, String packId) {
		this.packUrl = packUrl;
		this.packId = packId;
	}

	protected abstract void done(String packId);

	@Override
	public void run() {
		try {
			URL url = new URL(packUrl);
			Path tmpName = Paths.get("packs", packId);
			Path packName = Paths.get("packs", packId + ".jar");

			try (InputStream is = url.openStream()) {
				Files.copy(is, tmpName);
			}
			Files.move(tmpName, packName);

			done(packId);

		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
