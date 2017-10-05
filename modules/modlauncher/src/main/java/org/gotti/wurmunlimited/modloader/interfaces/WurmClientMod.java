package org.gotti.wurmunlimited.modloader.interfaces;

public interface WurmClientMod extends Versioned {
	
	public default void init() {
	}
	
	public default void preInit() {
	}

}
