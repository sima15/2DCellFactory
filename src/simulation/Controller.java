package simulation;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import cycleFinder.CycleFinder;
import data.DataRetrieval;
import data.WriteToFile;
import equations.EquationBuilder;
import graph.Edge;
import graph.Graph;
import graph.Pruner;
import graph.Vertex;
import idyno.Idynomics;
import protocolModfr.AgentStateBuilder;
import protocolModfr.IncFileSecondPhaseModifier;
import protocolModfr.ProtocolModifier;
import result.Test;
import utils.ImgProcLog;
import utils.XMLParser;

/**
 * 
 * @author Sima
 * Starts this project. Runs the two phases and records the results.
 */
public class Controller {
	private static String protocol_xml; // = "Vascularperc30-quartSize.xml";
	private static String RESULT_PATH; // = "E:\\Bio research\\GA\\resultss\\experiments\\";
	private String AGENT_LOC_PATH; 
	

	public static String name;
	public static int numIteration;

	private int numCycles = -10;
	private double product = -100;
	private static int pathFromLeftToRight = 0;

	/**
	 * Creates a new controller object which finds cell-factory running results
	 * @param n The name of the folder where the results will be saved.
	 */

	public Controller(String n, String protocol_xml, String RESULT_PATH, int number) {
		name = n;
		Controller.protocol_xml = protocol_xml;
		Controller.RESULT_PATH = RESULT_PATH;
		ImgProcLog.write("Name of folder in Controller: " + name);
		numIteration = number;
	}

	/**
	 * Starts this project by creating a new Controller object and running different phases in it
	 * @param args
	 */
	public static void main(String[] args) {
		ImgProcLog.write("******************************************************************************");
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date start = new Date();
		ImgProcLog.write("Start Date/Time: "+ dateFormat.format(start));
		Controller controller = new Controller("Vascularperc30-quartSize-short(20170302_0305)", 
				"Vascularperc30-quartSize-short.xml", "E:\\Bio research\\2D Cell Factory\\results\\test case 8\\my result\\", 16);
		controller.runFirstPhase();
		Date end = new Date();
		ImgProcLog.write("End Date/Time: "+ dateFormat.format(end));
		ImgProcLog.write("******************************************************************************");
	}

	
	public void start() {
		ImgProcLog.write("******************************************************************************");
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date start = new Date();
		ImgProcLog.write("Start Date/Time: "+ dateFormat.format(start));
		pathFromLeftToRight = 0;
		runFirstPhase();
		Date end = new Date();
		ImgProcLog.write("End Date/Time: "+ dateFormat.format(end));
		ImgProcLog.write("******************************************************************************");
	}



	/**
	 * Does the first set of procedures needed to simulate cell factory with an active vascular network
	 * 
	 */
	public void runFirstPhase(){
		Graph graph = createGraph();
		if(graph.equals(null)){
			ImgProcLog.write("Unsuccessful in running the first phase. Aborting...");
			product = 0;
			return;
		}
		
		CycleFinder cycleFinder = new CycleFinder(graph);
		ArrayList<List<Edge>> cycles;

		try{
			graph = cycleFinder.simplifyGraph();
			cycles = cycleFinder.getCycles();
			numCycles = (cycleFinder.getCycleSize());
			ImgProcLog.write("Number of cycles found: " + numCycles);
			ImgProcLog.write("cycles are: " + cycles);
		}catch(Exception e){
			ImgProcLog.write("Error in finding cycles. Aborting...");
			product = 0;
			ImgProcLog.write("Exception: " + e.getStackTrace());
			ImgProcLog.write(e.getMessage());
			e.printStackTrace();
			return;
		}
		EquationBuilder equationBuilder;
		try {
			equationBuilder = new EquationBuilder(graph, cycles);
			equationBuilder.buildPressureEquations();
		} catch (Exception e) {
			ImgProcLog.write("Equation solver not resolved! ");
			ImgProcLog.write("Exception: " + e.getStackTrace());
			ImgProcLog.write(e.getMessage());
			e.printStackTrace();
			product = 0;
			return;
		}
		
		AgentStateBuilder agentStateBuilder = new AgentStateBuilder(graph, equationBuilder);
		agentStateBuilder.modifyAgentStateFile(RESULT_PATH + name);
		HashMap<Integer, Double> secretionMap = agentStateBuilder.getSecretionMap();
		ProtocolModifier protocolModifier = 
				new ProtocolModifier(protocol_xml, agentStateBuilder.getEdgeCellLength(), secretionMap);
		protocolModifier.modifyXML(RESULT_PATH + name);
		protocolModifier = null;
		if(runSecondPhase(name)){
			IncFileSecondPhaseModifier incFileModifier = new IncFileSecondPhaseModifier(RESULT_PATH + name, secretionMap);
			try {
				incFileModifier.modify();
			} catch (IOException | InterruptedException e) {
				ImgProcLog.write("Exception in inc file modification process. Aborting ... ");
				ImgProcLog.write(e.getMessage());
				e.printStackTrace();
				return;
			}
		}else return;
	}

	/**
	 * Starts cDynomics with the latest agent state file and outputs the results including total product
	 * of the cell factory
	 * @param name Name of the folder in which the agent state and protocol files exist
	 * @return True if the method was successfully run.
	 */
	private boolean runSecondPhase(String name) {
		System.out.println(Runtime.getRuntime().totalMemory());
		System.gc();
		System.out.println(Runtime.getRuntime().totalMemory());

		String[] restartProtocolPath = { RESULT_PATH + name + "\\"+protocol_xml };
		try {
			Idynomics.main(restartProtocolPath);
		} catch (Exception e) {
			ImgProcLog.write("Error running cDynomics.");
			e.printStackTrace();
			product = 0;
			return false;
		}
		try {
			product = Test.consolidateSoluteConcentrations(RESULT_PATH, name, numIteration);
		} catch (IOException e) {
			ImgProcLog.write(e.getMessage());
			e.printStackTrace();
		}
		ImgProcLog.write("Product amount = " + product);
		return true;
	}

	/**
	 * Creates a graph based on the position data of vascular cells provided in
	 * State.xml file
	 * 
	 * @return A simplified graph
	 */
	public Graph createGraph() {
		AGENT_LOC_PATH = RESULT_PATH + name + "\\agent_State\\";
		String agentLocFileName = findLastStateXml(AGENT_LOC_PATH);
		String fullPath = AGENT_LOC_PATH + agentLocFileName;
		XMLParser agentFileParser = new XMLParser(fullPath);
		DataRetrieval.extractAgentDetails(agentFileParser, AGENT_LOC_PATH + agentLocFileName);

		Graph primGraph = new Graph();
		Graph pruned = null;
		ArrayList<Vertex> vertices = primGraph.createVertices();
		primGraph.createEdges(vertices);
//		new WriteToFile(primGraph, "Output\\2DGraph.wrl");
		try{
			pruned = new Pruner().startPruning(primGraph);
//			new WriteToFile(pruned, "Output\\2DPruned.wrl");
			ImgProcLog.write("Graph created");
		}catch(Exception e){
			ImgProcLog.write("Error in pruning the graph.");
			ImgProcLog.write(e.getMessage());
			e.printStackTrace();
		}
		return pruned;
	}

	/**
	 * Sets the name field of the current run
	 * @param name Name to be assigned to this run
	 */
	public static void setName(String name) {
		Controller.name = name;
	}

	/**
	 * Finds and returns an agent state xml file for graph creation
	 * 
	 * @param filePath The folder containing agent state files
	 * @return agent state file to be used for graph creation
	 */
	public String findLastStateXml(String filePath) {
		File dir = new File(filePath);
		File[] xmlFiles = dir.listFiles();
		Arrays.sort(xmlFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return xmlFiles[0].getName();
	}

	public void resetParams() {
	}

	public double getProduct() {
		if (product == -100)
			return 0;
		return product;
	}

	public int getNumCycles() {
		if (numCycles == -10)
			return 0;
		return numCycles;
	}

	public void setNumCycles(int n) {
		numCycles = n;
	}
	
	/**
	 * Returns the name of the main folder where the program is being run
	 * @return Name of the current folder
	 */
	public static String getName(){
		return name;
	}
	
	public static void setPathFromLeftToRightExistence(){
		pathFromLeftToRight  = 1;
	}
	
	public static int getPathFromLeftToRightExistence(){
		return pathFromLeftToRight;
	}
}
