package org.gotti.wurmunlimited.modsupport.console;

/**
 * Interface to access console input in mods.
 */
public interface ConsoleListener {
	/**
	 * Handle console input. 
	 * 
	 * @param input Console input
	 * @param silent Silent flag
	 * @return true if the input has been handled and should not be processed further
	 */
	boolean handleInput(String input, Boolean silent);

}
