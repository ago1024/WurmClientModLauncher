package org.gotti.wurmunlimited.modsupport;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public class ModClient {

	private static Logger logger = Logger.getLogger(ModClient.class.getName());

	private static Deque<Runnable> taskList = new LinkedBlockingDeque<>();

	private static Field wurmClientBaseClientObject;

	private static Field wurmClientBaseWorld;

	private static Field wurmClientBaseHud;
	
	public static void preInit() {
		try {
			StringBuilder code = new StringBuilder();
			code.append(String.format("%s#runTasks();", ModClient.class.getName()));
			HookManager.getInstance().getClassPool().get("com.wurmonline.client.WurmClientBase").getMethod("runGameLoop", "()V").insertBefore(code.toString());
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
	}

	public static void init() {
		try {
			wurmClientBaseClientObject = ReflectionUtil.getField(WurmClientBase.class, "clientObject");
			wurmClientBaseWorld = ReflectionUtil.getField(WurmClientBase.class, "world");
			wurmClientBaseHud = ReflectionUtil.getField(WurmClientBase.class, "hud");
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

	public static WurmClientBase getClientInstance() {
		try {

			return ReflectionUtil.getPrivateField(WurmClientBase.class, wurmClientBaseClientObject);
		}
		catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

	public static World getWorld() {
		try {
			WurmClientBase clientInstance = getClientInstance();
			return ReflectionUtil.getPrivateField(clientInstance, wurmClientBaseWorld);
		}
		catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}

	public static HeadsUpDisplay getHeadsUpDisplay() {
		try {
			WurmClientBase clientInstance = getClientInstance();
			return ReflectionUtil.getPrivateField(clientInstance, wurmClientBaseHud);
		}
		catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new HookException(e);
		}
	}
	
	public static void runTasks() {
		while (!taskList.isEmpty()) {
			Runnable task = taskList.poll();
			if (task != null) {
				try {
					task.run();
				} catch (Exception e) {
					logger.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	public static void runTask(Runnable task) {
		taskList.offer(task);
	}
}
