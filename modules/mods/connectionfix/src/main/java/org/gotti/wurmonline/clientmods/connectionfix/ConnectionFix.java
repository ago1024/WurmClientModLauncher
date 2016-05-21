package org.gotti.wurmonline.clientmods.connectionfix;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ConnectionFix implements WurmClientMod, PreInitable {

	@Override
	public void preInit() {
		try {
			
			ClassPool classpool = HookManager.getInstance().getClassPool();
			
			classpool.get("com.wurmonline.communication.SocketConnection").getMethod("tick", "()V").instrument(new ExprEditor() {
				
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("java.nio.channels.SocketChannel") && m.getMethodName().equals("read")) {
						m.replace("{ int r = $proceed($$); if (r < 0) throw new java.io.IOException(\"Disconnected.\"); $_ = r; }");
					}
				}
				
			});
			
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
	}

}
