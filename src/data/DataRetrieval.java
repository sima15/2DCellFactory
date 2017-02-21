package data;

import java.util.LinkedHashMap;
import java.util.List;

//import org.jdom2.Element;
import org.jdom.Element;

import utils.XMLParser;

public class DataRetrieval {

	static double[][] data;
	static int numPipeCells;
	private static String protocol_xml;
//	private static final String PROTOCOL_PATH = "E:\\Courses\\cs6600\\Project\\program\\ImageProcessing Results\\"
//			+ "Vascularperc30-quartSize(20161206_0158)\\agent_State\\";
//	public static String name; // = "agent_State(120).xml";
	public static String agentStateFileName;
	
//	private static final String PROTOCOL_PATH = "C:\\Delin\\Updated Workspace\\iDynoMiCS\\protocols\\experiments\\";
//	public static String name = "Vascularperc30(20160807_1246)";

	
	public static void extractAgentDetails(XMLParser agentFileParser, String filePath) {
		
		System.out.println(filePath);
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
//			else break;
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
		
//		edgeIDMap = new LinkedHashMap<String, String>();
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
	
	 public static double[][] getData(){
	        return data;
	 }
	 
	 public static int getNumPipeCells(){
		 return numPipeCells;
	 }
}
