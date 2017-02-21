package simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import LocalThickness.LocalThicknessWrapper;
import cycleFinder.CycleFinder;
//import basicProcessing.POVRayExecution;
import data.DataRetrieval;
import data.WriteToFile;
import equations.EquationMatrixBuilder;
import equations.EquationSolver;
import graph.Edge;
import graph.Graph;
import graph.Pruner;
import graph.Vertex;
import idyno.Idynomics;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import protocolModfr.AgentStateBuilder;
import protocolModfr.IncFileSecondPhaseModifier;
//import ij.IJ;
//import ij.WindowManager;
import protocolModfr.OptimizedProtocolModifier;
import protocolModfr.ProtocolModifier;
import result.Test;
//import skeletonize.DisplayGraph;
import utils.ImgProcLog;
import utils.XMLParser;

public class Controller {
	private final String CONSOLIDATE_SOLUTE_CONCENTRATION_PATH = "\\SoluteConcentration\\Consolidated.txt";
	private final String SOLUTE_CONCENTRATION_PATH = "\\SoluteConcentration\\xy-1\\";
	private final String POVRAY_PATH = "\\povray\\";
	private static String protocol_xml = "Vascularperc30-quartSize.xml";
	private final String RESULT_PATH = "E:\\Bio research\\2D Cell Factory\\results\\test case 3\\my result\\";
	private final String PROTOCOL_PATH = "E:\\Bio research\\2D Cell Factory\\protocols\\";
	private String AGENT_LOC_PATH; // = "E:\\Bio research\\2D Cell Factory\\results\\test case 3\\my result\\2nd(20161203_1019)\\agent_State\\";

	public static String name = "30-quartSize(20161203_1434)";
	// public static String name;

	private static int numCycles = -10;
	private String totalProduct = "-100";
	// static Map<String, Double> secretionMap = null ;
	// private ImageProcessingUnit imageProcessingUnit;

	public Controller(String n) {
		name = n;
		ImgProcLog.write("Name of folder in Controller: " + name);
		System.out.println("Name of folder in Controller: " + name);
	}

	public static void main(String[] args) throws Exception {
		Controller controller = new Controller(name);
		ImgProcLog.write("Inside main method.");
		controller.runFirstPhase();
		ImgProcLog.write("Back at main method.");
		controller.resetParams();
	}

	public void start() throws Exception {
		Controller controller = new Controller(name);
		ImgProcLog.write("Inside start method.");
		controller.runFirstPhase();
		ImgProcLog.write("Back at start method.");
		controller.resetParams();
	}

	/**
	 * Verifies if this file can be fully processed to output product amount
	 * 
	 * @throws Exception
	 */
	public void verifyConditions() throws Exception {
	}

	/**
	 * Does the first image processing actions needed
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runFirstPhase() throws IOException, InterruptedException {
		Graph graph = createGraph();
		double[][] equationLeftSide;
		double[][] equationRightSide;
		double[][] flowRateMatrix;
		double muMax = 1.1;
		double kS = .015;
		// Might have to add binarizing and cropping procedures later to get
		// thickness image to work.
		// processImage();
		CycleFinder cycleFinder = new CycleFinder(graph);
		graph = cycleFinder.simplifyGraph();
		ArrayList<Edge> edges = graph.getEdges();
		ArrayList<List<Edge>> cycles = cycleFinder.getCycles();
		ImgProcLog.write("Number of cycles found: " + cycleFinder.getCycleSize());
		ImgProcLog.write("cycles are: " + cycles);
		try {
			EquationMatrixBuilder matrixBuilder = new EquationMatrixBuilder(graph, cycles);
			matrixBuilder.generateEquationMatrix();
			equationLeftSide = matrixBuilder.getEquationLeftSide();
			equationRightSide = matrixBuilder.getEquationRightSide();
			EquationSolver solver = new EquationSolver(edges.size() + 2, equationLeftSide, equationRightSide);
			flowRateMatrix = solver.solve();
			ImgProcLog.write("Flow rate matrix: " + Arrays.deepToString(flowRateMatrix));
			ImgProcLog.write("Max flow rate = " + solver.getMaxFlowRate());
			for (Edge e : edges) {
				e.setFlowRate(flowRateMatrix[e.getId()][0] / solver.getMaxFlowRate());
				e.setSecretionRate(muMax * e.getFlowRate() / (e.getFlowRate() + kS));
			}
		} catch (Exception e) {
			System.out.println("Equation solver not resolved! ");
			e.printStackTrace();
		}
		// EdgeIDImageCreator edgeIDImageCreator = new
		// EdgeIDImageCreator(custGraph,
		// binarizedImage.duplicate());
		// edgeIDImageCreator.generateImages();
		// edgeIdMatrix = edgeIDImageCreator.getEdgeIdMatrix();
		// secretionMap = edgeIDImageCreator.getSecretionMap();
		// maskImage = edgeIDImageCreator.getMaskImage();
		// flowVisualizationImage = edgeIDImageCreator.getFlowImage();
		// flowRateMatrix = custGraph.getFlowRateMatrix();
		//
		// flowVisualizationImage.show();
		// IJ.run("Fire");
		// local thi close();
		ImgProcLog.write("Graph created");
		System.out.println("Processing ended");
		AgentStateBuilder agentStateBuilder = new AgentStateBuilder(graph);
		agentStateBuilder.modifyAgentStateFile(RESULT_PATH + name);
		HashMap<Integer, Double> edgeMap = agentStateBuilder.getReducedMap();
		OptimizedProtocolModifier protocolModifier = new OptimizedProtocolModifier(graph, protocol_xml, edgeMap);
		protocolModifier.modifyXML(RESULT_PATH + name);
		edgeMap = protocolModifier.getSecretionMap();
		protocolModifier = null;
		runSecondPhase(name);
		IncFileSecondPhaseModifier incFileModifier = new IncFileSecondPhaseModifier(RESULT_PATH + name, graph, edgeMap);
		incFileModifier.modify();
	}

	private void runSecondPhase(String name) {
		System.out.println(Runtime.getRuntime().totalMemory());
		System.gc();
		System.out.println(Runtime.getRuntime().totalMemory());

		String[] restartProtocolPath = { RESULT_PATH + name + "\\"+protocol_xml };
		try {
			Idynomics.main(restartProtocolPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		try {
			totalProduct = Test.consolidateSoluteConcentrations(RESULT_PATH, name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ImgProcLog.write("Product amount = " + totalProduct);
	}

	/**
	 * Creates a graph based on the position data of vascular cells provided in
	 * State.xml file
	 * 
	 * @return A simplified graph
	 */
	public Graph createGraph() {
		// String agentLocFileName = XmlLocater.locateXml(AGENT_LOC_PATH);
		AGENT_LOC_PATH = RESULT_PATH + name + "\\agent_State\\";
		String agentLocFileName = findLastStateXml(AGENT_LOC_PATH);
		String fullPath = AGENT_LOC_PATH + agentLocFileName;
		XMLParser agentFileParser = new XMLParser(fullPath);
		DataRetrieval.extractAgentDetails(agentFileParser, AGENT_LOC_PATH + agentLocFileName);

		Graph primGraph = new Graph();
		ArrayList<Vertex> vertices = primGraph.createVertices();
		ArrayList<Edge> edges = primGraph.createEdges(vertices);
		new WriteToFile(primGraph, "Output\\2DGraph.wrl");

		Graph pruned = new Pruner().startPruning(primGraph);
		new WriteToFile(pruned, "Output\\2DPruned.wrl");
		return pruned;
	}

	/**
	 * Calculates average thickness of every edge for measuring flow rate
	 */
	public void processImage() {
		// LocalThicknessWrapper localThicknessWrapper = new
		// LocalThicknessWrapper();
		// localThicknessWrapper.setShowOptions(false);
		// localThicknessWrapper.run("");
		// ImagePlus localThicknessImage =
		// WindowManager.getCurrentImage().duplicate();
		// localThicknessImage.show();
	}

	public static void setName(String name) {
		Controller.name = name;
	}

	/**
	 * Finds and returns an agent state xml file for graph creation
	 * 
	 * @param filePath
	 *            The folder containing agent state files
	 * @return agent state file to be used for graph creation
	 */
	public String findLastStateXml(String filePath) {
		File dir = new File(filePath);
		File[] xmlFiles = dir.listFiles();
		Arrays.sort(xmlFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return xmlFiles[0].getName();
		// String[] fileNames = new String[xmlFiles.length];
		// int index =0;
		// for(File file: xmlFiles){
		// fileNames[index++] = file.getName();
		// }
		// Arrays.sort(fileNames);
		// return fileNames[fileNames.length-1];
	}

	public void resetParams() {
	}

	public double getProduct() {
		if (totalProduct == "-100")
			return 0;
		return Double.parseDouble(totalProduct);
	}

	public static int getNumCycles() {
		if (numCycles == -10)
			return 0;
		return numCycles;
	}

	public static void setNumCycles(int n) {
		numCycles = n;
	}
}
