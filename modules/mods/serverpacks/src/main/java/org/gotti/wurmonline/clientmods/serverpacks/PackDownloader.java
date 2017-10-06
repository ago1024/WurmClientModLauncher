package org.gotti.wurmonline.clientmods.serverpacks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PackDownloader implements Runnable {

	private Logger logger = Logger.getLogger(ServerPacksMod.class.getName());

	private URL packUrl;

	private String packId;

	public PackDownloader(URL packUrl, String packId) {
		this.packUrl = packUrl;
		this.packId = packId;
	}

	protected abstract void done(String packId);

	@Override
	public void run() {
		try {
			Path tmpName = Paths.get("packs", packId);
			Path packName = Paths.get("packs", packId + ".jar");

			try (InputStream is = packUrl.openStream()) {
				Files.copy(is, tmpName, StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(tmpName, packName);

			done(packId);

		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
