/**
 * 
 */
package utils;

import java.io.IOException;

/**
 * @author Sima
 * @author Delin Davis
 *
 */
public class PovRayExecutor {
	
	private final static String POVRAY_ENGINE_PATH = "C:\\Program Files\\POV-Ray\\v3.7\\bin\\pvengine.exe";
	private final static String DOUBLE_QUOTE = "\"";

	public static void execute(String file){
		String filename = DOUBLE_QUOTE + file + DOUBLE_QUOTE;

		String[] command1 = {POVRAY_ENGINE_PATH, "/EXIT", "/RENDER", filename, };
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command1);
		} catch (IOException e) {
			ImgProcLog.write("Error in PovRay execution.");
			e.printStackTrace();
		}
		while(process.isAlive());
	}

}
