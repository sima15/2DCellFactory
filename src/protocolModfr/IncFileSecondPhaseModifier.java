package protocolModfr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import graph.Graph;
import graph.Edge;
import ij.ImagePlus;
import utils.ImgProcLog;

public class IncFileSecondPhaseModifier {
	private String resultPath;
	private String lastFilePath;
	private String headerFilePath;
	private HashMap<Integer, Integer> secretionMap;

	public IncFileSecondPhaseModifier(String resultPath, HashMap<Integer, Integer> secretionMap) {
		this.resultPath = resultPath;
		this.secretionMap = secretionMap;
	}
	
	public void modify() throws IOException, InterruptedException {
		lastFilePath = copyFileFromZip();
		ImgProcLog.write("Header .inc file path: "+ headerFilePath);
		System.out.println(lastFilePath);
		System.out.println(headerFilePath);
		rewriteHeaderFile();
		// POVRayExecution.executer(lastFilePath);
	}
	
	public void modifySimple() throws IOException, InterruptedException {
		lastFilePath = copyFileFromZip();
		System.out.println("lastFilePath" + lastFilePath);
		System.out.println("headerFilePath" + headerFilePath);
		//rewriteHeaderFile();
		// POVRayExecution.executer(lastFilePath);
	}

	/**
	 * Assigns shades of red to the moving cells according to their secretion rates.
	 * The higher the secretion, the more close to red the moving cells get depicted. Darker colors show less secretion rates.
	 * @throws IOException
	 */
	private void rewriteHeaderFile() throws IOException {
		double maxSecretionRate = 0;
		for (Integer edgeId:secretionMap.keySet()) {
			ImgProcLog.write("Secretion for edge "+ edgeId + " = "+ secretionMap.get(edgeId));
			if (maxSecretionRate < secretionMap.get(edgeId)) {
					maxSecretionRate = secretionMap.get(edgeId);
			}
		}
		ImgProcLog.write("Secretion max  = "+ maxSecretionRate);
		List<String> lines = Files.readAllLines(Paths.get(headerFilePath));
		Object[] linesArray = lines.toArray();
		List<String> outputLines = new ArrayList<String>();
		for (int i = 0; i < lines.size(); i++) {
			if (((String)linesArray[i]).contains("color rgb < 1.0 , 0.0 , 0.0 >")) {
				String[] leftHalf = ((String)linesArray[i]).split(" = ");
				String[] numString = leftHalf[0].split("Cells");
				String edgeId = numString[1];
				Integer edgeID = Integer.parseInt(edgeId);
				if(secretionMap.containsKey(edgeID)){
					linesArray[i] = ((String)linesArray[i]).replace("< 1.0", "< " + (double) secretionMap.get(edgeID) / maxSecretionRate);
				} else {
					linesArray[i] = ((String)linesArray[i]).replace("< 1.0", "< " + "0.0");
				}
			}
			outputLines.add(((String)linesArray[i]));
		}
		FileUtils.writeLines(new File(headerFilePath), outputLines);
	}

	
	public String copyFileFromZip() throws IOException, InterruptedException {
		String zipPath = resultPath + "\\povray.zip";
		String extractedPath = resultPath + "\\povray";

		/* Get the newest file for a specific extension */
		ZipFile zipFile = new ZipFile(zipPath);

		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		List<ZipEntry> povZipEntryList = new ArrayList<ZipEntry>();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			povZipEntryList.add(entry);
			if (entry.getName().equals("sceneheader.inc") || entry.getName().equals("scenefooter.inc")) {
				InputStream initialStream = zipFile.getInputStream(entry);
				String finalPath = extractedPath + "\\" + entry.getName();
				final Path destination = Paths.get(finalPath);
				Files.copy(initialStream, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				if (entry.getName().equals("sceneheader.inc"))
					headerFilePath = destination.toString();
			}
		}

		String finalPath = "";
		if (povZipEntryList.size() > 0) {
			/** The newest file comes first **/
			povZipEntryList.sort(new Comparator<ZipEntry>() {

				@Override
				public int compare(ZipEntry o1, ZipEntry o2) {
					if (o1.getTime() > o2.getTime())
						return 1;
					else
						return 0;
				}

			});
			String lastFileName = povZipEntryList.get(0).getName();

			InputStream initialStream = zipFile.getInputStream(povZipEntryList.get(0));
			finalPath = extractedPath + "\\" + lastFileName;
			final Path destination = Paths.get(finalPath);
			Files.copy(initialStream, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		zipFile.close();
		return finalPath;

	}
}
