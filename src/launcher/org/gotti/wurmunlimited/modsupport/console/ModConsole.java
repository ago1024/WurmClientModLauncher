package org.gotti.wurmunlimited.modsupport.console;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

public class ModConsole {

	private static List<ConsoleListener> listeners = new ArrayList<>();

	public static void preInit() {
	}

	public static void init() {

		try {
			ClassPool classPool = HookManager.getInstance().getClassPool();

			// com.wurmonline.client.console.WurmConsole.handleInput2(String, boolean)
			String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] { classPool.get("java.lang.String"), classPool.get("boolean") });

			HookManager.getInstance().registerHook("com.wurmonline.client.console.WurmConsole", "handleInput2", descriptor, new InvocationHandlerFactory() {

				@Override
				public InvocationHandler createInvocationHandler() {
					return new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							synchronized (listeners) {
								if (args != null && args.length == 2 && args[0] instanceof String && args[1] instanceof Boolean) {
									String input = (String) args[0];
									Boolean silent = (Boolean) args[1];

									for (ConsoleListener listener : listeners) {
										if (listener.handleInput(input, silent)) {
											return null;
										}
									}
								}

								return method.invoke(proxy, args);
							}
						}
					};
				}
			});
		} catch (NotFoundException e) {
			throw new HookException(e);
		}
	}

	public static void addConsoleListener(ConsoleListener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	public static void removeConsoleListener(ConsoleListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

}
