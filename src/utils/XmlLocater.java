/**
 * 
 */
package utils;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

/**
 * @author Sima
 * @version 1
 */
public class XmlLocater {

	/**
	 * @param filePath
	 * @return Last XML file name
	 */
	public static String locateXml(String filePath) {
		/* Get the newest file for a specific extension */
	    File dir = new File(filePath);
	    File[] files = dir.listFiles();
	    if (files.length > 0) {
	        /** The newest file comes first **/
	        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
	    }
	    return files[0].getName();

	}

}
