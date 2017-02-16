package simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import protocolModfr.IncFileSecondPhaseModifier;
//import ij.IJ;
//import ij.WindowManager;
import protocolModfr.OptimizedProtocolModifier;
import protocolModfr.ProtocolModifier;
import result.Test;
//import skeletonize.DisplayGraph;
import utils.ImgProcLog;
import utils.XMLParser;
//import visualizations.EdgeIDImageCreator;

public class Controller {
	private final String CONSOLIDATE_SOLUTE_CONCENTRATION_PATH = "\\SoluteConcentration\\Consolidated.txt";
	private final String SOLUTE_CONCENTRATION_PATH = "\\SoluteConcentration\\xy-1\\";
	private final String POVRAY_PATH = "\\povray\\";
	private static String protocol_xml = "Vascularperc30-quartSize.xml";
	private final String RESULT_PATH = "E:\\Courses\\cs6600\\Project\\program\\resultss\\experiments\\";
	private final String PROTOCOL_PATH = "E:\\Courses\\cs6600\\Project\\program\\protocols\\experiments\\";
//	private final String AGENT_LOC_PATH = "E:\\Courses\\cs6600\\Project\\program\\ImageProcessing Results\\Vascularperc30-quartSize(20161206_0158)\\agent_State\\";
	private final String AGENT_LOC_PATH = "E:\\Courses\\cs6600\\Project\\program\\iDynomics Results\\Img Proc practice\\2nd(20161203_1019)\\agent_State\\";
	
	private final boolean speciesOptimizer = true;
	private boolean nodeMergerOptimizer = false;
	private int NodeMergerTileSize = 1;
	private boolean ratioLessThanFifty = true;
	public static String name = "2nd(20161203_1019)";
//	public static String name;
	
	private static int numCycles = -10;
	private String totalProduct = "-100";
	static int[][] edgeIdMatrix = null ;
	static Map<String, Double> secretionMap = null ;
//	private ImageProcessingUnit imageProcessingUnit;
	
	public Controller(String n){
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
	
	public void start() throws Exception{
		Controller controller = new Controller(name);
		ImgProcLog.write("Inside start method."); 
		controller.runFirstPhase();
		ImgProcLog.write("Back at start method.");
		controller.resetParams();
	}
	
	/**
	 * Verifies if this file can be fully processed to output product amount
	 * @throws Exception
	 */
	public void verifyConditions() throws Exception {
	}
	
	/**
	 * Does the first image processing actions needed
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void runFirstPhase() throws IOException, InterruptedException{
//		System.out.println("Current thread: " + Thread.currentThread().getName());
		Graph graph =  createGraph();
		double[][] equationLeftSide;
		double[][] equationRightSide;
		double[][] flowRateMatrix; 
	    //  Might have to add binarizing and cropping procedures later to get thickness image to work.
//	    processImage();
	    CycleFinder cycleFinder = new CycleFinder(graph);
	    graph = cycleFinder.simplifyGraph();
	    ArrayList<Edge> edges = graph.getEdges();
	    ArrayList<List<Edge>> cycles = cycleFinder.getCycles(); 
	    ImgProcLog.write("Number of cycles found: "+ cycleFinder.getCycleSize());
	    ImgProcLog.write("cycles are: "+ cycles);
	    try {
			EquationMatrixBuilder matrixBuilder = new  EquationMatrixBuilder(graph, cycles); 
			matrixBuilder.generateEquationMatrix();
			equationLeftSide = matrixBuilder.getEquationLeftSide();
			equationRightSide = matrixBuilder.getEquationRightSide();
			EquationSolver solver = new EquationSolver(edges.size() + 2, equationLeftSide,
					equationRightSide);
			flowRateMatrix = solver.solve();
			ImgProcLog.write("Flow rate matrix: "+ Arrays.deepToString(flowRateMatrix));
			for (Edge e : edges) {
				e.setFlowRate(flowRateMatrix[e.getId()][0]);
			}
//			for (Edge e : getRemovedEdges()) {
//				if ((e.getVertex1().equals(getStartNode()) && e.getVertex2().equals(getStartNode()))
//						|| (e.getVertex1().equals(getEndNode()) && e.getVertex2().equals(getEndNode()))) {
//					e.setFlowRate(flowRateMatrix[flowRateMatrix.length - 1][0]);
//				} else {
//					e.setFlowRate(0);
//				}
//			}
		} catch (Exception e) {
			System.out.println("Equation solver not resolved! ");
			e.printStackTrace();
		}
//	    EdgeIDImageCreator edgeIDImageCreator = new EdgeIDImageCreator(custGraph,
//				binarizedImage.duplicate());
//		edgeIDImageCreator.generateImages();
//		edgeIdMatrix = edgeIDImageCreator.getEdgeIdMatrix();
//		secretionMap = edgeIDImageCreator.getSecretionMap();
//		maskImage = edgeIDImageCreator.getMaskImage();
//		flowVisualizationImage = edgeIDImageCreator.getFlowImage();
//		flowRateMatrix = custGraph.getFlowRateMatrix();
//
//		flowVisualizationImage.show();
//		IJ.run("Fire");
		// local thi close();
		ImgProcLog.write("Graph created");
		System.out.println("Processing ended");
		
//		ProtocolModifier protocolModifier = new OptimizedProtocolModifier(edgeIdMatrix, 
//				secretionMap, protocol_xml);
//		if (speciesOptimizer) {
//			protocolModifier = new OptimizedProtocolModifier(edgeIdMatrix, secretionMap, protocol_xml);
//		} else {
//			protocolModifier = new ProtocolModifier(edgeIdMatrix, secretionMap, protocol_xml);
//		}
//		protocolModifier.modifyXML(RESULT_PATH + name);
//		
//		protocolModifier = null;
//		runSecondPhase(name);
//		IncFileSecondPhaseModifier incFileModifier = new IncFileSecondPhaseModifier(RESULT_PATH + name,
//				secretionMap);
//		incFileModifier.modify();
	}


	private void runSecondPhase(String name) {
		System.out.println(Runtime.getRuntime().totalMemory());
		System.gc();
		System.out.println(Runtime.getRuntime().totalMemory());

		String[] restartProtocolPath = { RESULT_PATH + name + protocol_xml };
		try {
			Idynomics.main(restartProtocolPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		try {
			totalProduct =  Test.consolidateSoluteConcentrations(RESULT_PATH, name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ImgProcLog.write("Product amount = " + totalProduct);
	}

	/**
	 * Creates a graph based on the position data of vascular cells provided in State.xml file
	 * @return A simplified graph 
	 */
	public Graph createGraph(){
//		String agentLocFileName = XmlLocater.locateXml(AGENT_LOC_PATH);
		String agentLocFileName = findLastStateXml(AGENT_LOC_PATH);
		String fullPath = AGENT_LOC_PATH + agentLocFileName;
		XMLParser agentFileParser = new XMLParser(fullPath);
		DataRetrieval.extractAgentDetails(agentFileParser, AGENT_LOC_PATH + agentLocFileName);
	 
	    Graph primGraph = new Graph();
	    ArrayList<Vertex> vertices = primGraph.createVertices();
	    ArrayList<Edge> edges = primGraph.createEdges(vertices);
	    new WriteToFile( primGraph, "Output\\2DGraph.wrl");
	
	    Graph pruned = new Pruner().startPruning(primGraph);
	    new WriteToFile( pruned, "Output\\2DPruned.wrl");
	    return pruned;
	}
	
	/**
	 * Calculates average thickness of every edge for measuring flow rate
	 */
	public void processImage(){
//		LocalThicknessWrapper localThicknessWrapper = new LocalThicknessWrapper();
//		localThicknessWrapper.setShowOptions(false);
//		localThicknessWrapper.run("");
//		ImagePlus localThicknessImage = WindowManager.getCurrentImage().duplicate();
//		localThicknessImage.show();
	}
	
	
	public static void setName(String name){
		Controller.name = name;
	}
	
	/**
	 * Finds and returns an agent state xml file for graph creation
	 * @param filePath The folder containing agent state files
	 * @return agent state file to be used for graph creation
	 */
	public String findLastStateXml(String filePath){
		File dir = new File(filePath);
		File[] xmlFiles = dir.listFiles();
		Arrays.sort(xmlFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return xmlFiles[0].getName();
//		String[] fileNames = new String[xmlFiles.length];
//		int index =0;
//		for(File file: xmlFiles){
//			fileNames[index++] = file.getName();
//		}
//		Arrays.sort(fileNames);
//		return fileNames[fileNames.length-1];
	}
	
	public void resetParams(){
//		fullRun = false;
//		setNodeMergerOptimizer(false);
//		setNodeMergerTileSize(1);
//		finished = false;
	}
	
	public double getProduct() {
		if(totalProduct == "-100")
			return 0;
		return Double.parseDouble(totalProduct);
	}
	
	public static int getNumCycles() {
		if(numCycles == -10)
			return 0;
		return numCycles;
	}
	
	public static void setNumCycles(int n){
		numCycles = n;
	}

	/**
	 * @return the nodeMergerTileSize
	 */
	public int getNodeMergerTileSize() {
		return NodeMergerTileSize;
	}

	/**
	 * @param nodeMergerTileSize the nodeMergerTileSize to set
	 */
	public void setNodeMergerTileSize(int nodeMergerTileSize) {
		NodeMergerTileSize = nodeMergerTileSize;
	}

	/**
	 * @return the nodeMergerOptimizer
	 */
	public boolean isNodeMergerOptimizer() {
		return nodeMergerOptimizer;
	}

	/**
	 * @param nodeMergerOptimizer the nodeMergerOptimizer to set
	 */
	public void setNodeMergerOptimizer(boolean nodeMergerOptimizer) {
		this.nodeMergerOptimizer = nodeMergerOptimizer;
	}
}
