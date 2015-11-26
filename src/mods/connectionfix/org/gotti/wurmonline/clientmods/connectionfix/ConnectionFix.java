package org.gotti.wurmonline.clientmods.connectionfix;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ConnectionFix implements WurmMod, PreInitable {

	@Override
	public void preInit() {
		try {
			
			ClassPool classpool = HookManager.getInstance().getClassPool();
			
			classpool.get("com.wurmonline.client.WurmClientBase").getMethod("performConnection", "()V").instrument(new ExprEditor() {
				
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("com.wurmonline.client.comm.SimpleServerConnectionClass") && m.getMethodName().equals("isConnecting")) {
						if (m.getLineNumber() == 737) {
							m.replace("$_ = $proceed($$) && !serverConnection.GetSteamAuthenticateSucces();"); 
						}
					} 
				}
			});
		} catch (NotFoundException | CannotCompileException e) {
			throw new HookException(e);
		}
	}

}
