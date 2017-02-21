package data;

import java.util.List;

import org.jdom.Element;

import utils.ImgProcLog;
import utils.XMLParser;

/**
 * 
 * @author Sima Mehri
 * Gets cell data from an agentState.xml file and creates a matrix of cells with their x,y,z coordinates
 */
public class DataRetrieval {

	static double[][] data;
	static int numPipeCells;
	public static String agentStateFileName;
	

	public static void extractAgentDetails(XMLParser agentFileParser, String filePath) {
		ImgProcLog.write("Agent state file path for graph creation: "+ filePath);
		Element agentRoot = agentFileParser.get_localRoot();
		@SuppressWarnings("unchecked")
		List<Element> speciesList = agentRoot.getChild("simulation").getChildren("species");
		Element movingCells = null;
		Element pipeCellsLeft = null;
		Element pipeCellsRight = null;
		for (Element s : speciesList) {
			if (s.getAttributeValue("name").equals("MovingCells")) {
				movingCells = s;
			}else if(s.getAttributeValue("name").equals("PipeCellsLeft") ){
				pipeCellsLeft = s;
			}else if(s.getAttributeValue("name").equals("PipeCellsRight"))
				pipeCellsRight = s;	
		}
		String text = movingCells.getText();
		String[] agentArray = text.split(";\n");
		String plText = pipeCellsLeft.getText();
		String[] lpipeCellArray = plText.split(";\n");
		String prText = pipeCellsRight.getText();
		String[] rpipeCellArray = prText.split(";\n");
		int numLeftPCells = lpipeCellArray.length;
		int numRightPCells = rpipeCellArray.length;
		int numVCells = agentArray.length;
		numPipeCells = numLeftPCells + numRightPCells;		
		int numberOfCells = numVCells + numPipeCells; 
		System.out.println(numberOfCells);
		data = new double[numberOfCells][];
		
		for (int i = 0; i < numVCells; i++) {
			String[] elements = agentArray[i].split(",");
			data[i] = new double[elements.length];
			int x = (int) Math.round(256 - Double.parseDouble(elements[10]));
			int y = (int) Math.round(512 - Double.parseDouble(elements[11]));
//			int z = (int) Math.round(32 - Double.parseDouble(elements[12]));
			int z = 0;
			data[i][0] = x;
			data[i][1] = y;
			data[i][2] = z;
		}
		int pipeCounter =0;
		for (int i = numVCells; i < numVCells + numLeftPCells; i++) {
			
			String[] elements = lpipeCellArray[pipeCounter++].split(",");
			data[i] = new double[elements.length];
			int x = (int) Math.round(256 - Double.parseDouble(elements[10]));
			int y = (int) Math.round(512 - Double.parseDouble(elements[11]));
//			int z = (int) Math.round(32 - Double.parseDouble(elements[12]));
			int z = 0;
			data[i][0] = x;
			data[i][1] = y;
			data[i][2] = z;
		}
		pipeCounter =0;
		for (int i = numVCells + numLeftPCells; i < numberOfCells; i++) {
			
			String[] elements = rpipeCellArray[pipeCounter++].split(",");
			data[i] = new double[elements.length];
			int x = (int) Math.round(256 - Double.parseDouble(elements[10]));
			int y = (int) Math.round(512 - Double.parseDouble(elements[11]));
//			int z = (int) Math.round(32 - Double.parseDouble(elements[12]));
			int z = 0;
			data[i][0] = x;
			data[i][1] = y;
			data[i][2] = z;
		}
		
	}
	
	/**
	 * Provides the location data of cells
	 * @return A matrix of cell x,y,z coordinates
	 */
	 public static double[][] getData(){
	        return data;
	 }
	 
	 /**
	  * Provides the number of pipe cells in this state file
	  * @return The number of pipe cells
	  */
	 public static int getNumPipeCells(){
		 return numPipeCells;
	 }
}
