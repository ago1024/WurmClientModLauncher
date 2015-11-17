package org.gotti.wurmunlimited.clientlauncher;

import javassist.ClassPool;
import javassist.Loader;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

public class ClientLauncher {

	public static void main(String[] args) {
		try {

			ClassPool classPool = HookManager.getInstance().getClassPool();
			classPool.insertClassPath("client-patched.jar");
			
			Loader loader = HookManager.getInstance().getLoader();
			loader.delegateLoadingOf("javafx.");
			loader.delegateLoadingOf("com.sun.");
			loader.delegateLoadingOf("org.controlsfx.");
			loader.delegateLoadingOf("impl.org.controlsfx");
			loader.delegateLoadingOf("com.mysql.");
			loader.delegateLoadingOf("org.sqlite.");
			loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.classhooks.");
			//loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.");
			loader.delegateLoadingOf("javassist.");

			Thread.currentThread().setContextClassLoader(loader);

			loader.run("org.gotti.wurmunlimited.clientlauncher.DelegatedLauncher", args);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
