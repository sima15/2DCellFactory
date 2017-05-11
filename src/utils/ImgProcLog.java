/**
 * Project iDynoMiCS (copyright -> see Idynomics.java)
 */


package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * 
 * @author Sima Mehri
 * Logs important events 
 */
public class ImgProcLog {

	public static void write(String filePath, String message) {
		File file; 
		BufferedWriter output = null;
		String fileName = "Log.txt";
		try{
			file = new File(filePath+ fileName);
			output = new BufferedWriter(new FileWriter(file, true));
			output.write(message);
			output.write("\r\n");
			output.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}
}