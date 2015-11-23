package org.gotti.wurmunlimited.clientlauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.Descriptor;

public class DelegatedLauncher {
	
	private static final String WURM_MAIN_CLASS = "com.wurmonline.client.launcherfx.WurmMain";
	
	public static void initLogger() {
		Formatter f = new Formatter() {

			@Override
			public String format(LogRecord record) {
		        String source;
		        if (record.getSourceClassName() != null) {
		            source = record.getSourceClassName();
		            if (record.getSourceMethodName() != null) {
		               source += " " + record.getSourceMethodName();
		            }
		        } else {
		            source = record.getLoggerName();
		        }
		        String message = formatMessage(record);
		        String throwable = "";
		        if (record.getThrown() != null) {
		            StringWriter sw = new StringWriter();
		            PrintWriter pw = new PrintWriter(sw);
		            pw.println();
		            record.getThrown().printStackTrace(pw);
		            pw.close();
		            throwable = sw.toString();
		        }
		        return String.format("%4$s: %5$s%6$s",
		                             new Date(record.getMillis()),
		                             source,
		                             record.getLoggerName(),
		                             record.getLevel().getLocalizedName(),
		                             message,
		                             throwable);
			}
			
		};
		
		Handler handler = new StreamHandler(System.out, new SimpleFormatter()) {
			@Override
			public synchronized void publish(LogRecord record) {
				
				System.out.println(f.format(record));
			}
		};
		Logger.getLogger("").addHandler(handler);
	}

	public static void main(String[] args) {
		
		try {
			List<WurmMod> wurmMods = new ModLoader().loadModsFromModDir(Paths.get("mods"));
			
			String code = "org.gotti.wurmunlimited.clientlauncher.DelegatedLauncher.initLogger();";
			ClassPool cp = HookManager.getInstance().getClassPool();
			cp.get(WURM_MAIN_CLASS).getMethod("main", Descriptor.ofMethod(cp.get("void"), new CtClass[] { cp.get("java.lang.String[]") })).insertBefore(code);
			HookManager.getInstance().getLoader().run(WURM_MAIN_CLASS, args);
			
		} catch (Throwable e) {
			e.printStackTrace();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e2) {
			}
			System.exit(-1);
		}
	}

}
