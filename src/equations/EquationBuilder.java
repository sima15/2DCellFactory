/**
 * 
 */
package equations;

import graph.*;
import jdk.nashorn.internal.runtime.arrays.NumericElements;
import utils.ImgProcLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
/**
 * @author Sima
 * 
 */
public class EquationBuilder {

	private Graph graph;
	private ArrayList<Edge> edges;
	private ArrayList<Vertex> nodes;
	private ArrayList<List<Edge>> cycles;
	private HashMap<List<Edge>, Integer> cyclesNames;
	
	private double[][] pressureMatrix;
	private double[][] constants; 
	private double[] resistances;
	private int[][] edgesCycles;
	private double[][] edgePressureArray;
	private double[][] edgeFlowArray;

	private final double BLOODVISCOSITY = 0.00089;
	private static final int _PRESSURE = 1000; // kPa
	private static final int REACTIONPRECISION = 100;
	double muMax = 1.1;
	double kS = .015;
	
	private final int NONEXISTENT = -5;
	private int numNeededEquations;
	private int maxSecretionRate;
	private int minSecretionRate;
	
	double[][] meshFlowMatrix;
	private double[][] meshCurrents;
	
	/**
	 * Creates flow equations for the given graph
	 * @param graph A given graph
	 * @param cycles The minimal cycles included in this graph
	 */
	public EquationBuilder(Graph graph, ArrayList<List<Edge>> cycles){
		this.graph = graph;
		edges = graph.getEdges();
		nodes = graph.getVertices();
		this.cycles = cycles;
		pressureMatrix = new double[edges.size()][edges.size()];
		constants = new double[edges.size()][1];
	}
	
	/**
	 * Builds equations based on the pressure drop over edges of the graph
	 */
	public void buildPressureEquations() throws Exception{
		if(edges.size()>cycles.size()+ nodes.size()+1){
			ImgProcLog.write("More unknowns than equations.");
			throw new Exception("Insufficient nodes and cycles.");
		}
		createResistanceArray();
		labelCycles();
		numNeededEquations = edges.size() - cycles.size()-1;
		int index;
		index = buildMeshPressureEquations();
		index = buildKCLs(index);
		ArrayList<Vertex> pathFromStartToEnd = getPathFromStartToEnd();
		if(pathFromStartToEnd.isEmpty()){
			ImgProcLog.write("There is no path from the left pipe cells to the right.");
			throw new Exception("Path builder exception.");
		}
		constants[index][0] = _PRESSURE;
		buildLongPathEquation( index, pathFromStartToEnd);
		ImgProcLog.write("Pressure drop coefficients matrix: ");
		ImgProcLog.write(Arrays.deepToString(pressureMatrix));
		ImgProcLog.write("Constants matrix: ");
		ImgProcLog.write(Arrays.deepToString(constants));
		ImgProcLog.write("                                 --------------------------");
		EquationSolver solver = new EquationSolver(edges.size(), pressureMatrix, constants);
		edgePressureArray = solver.solve();
		ImgProcLog.write("Pressure drop results matrix: ");
		ImgProcLog.write(Arrays.deepToString(edgePressureArray));
		edgeFlowArray = new double[edges.size()][1];
		edgeFlowArray = calculateEdgeFlows();
		ImgProcLog.write("Edge flow Array: ");
		ImgProcLog.write(Arrays.deepToString(edgeFlowArray));
		setSecretionRates();
	}
	
	/**
	 * Creates an array of the resistance of each edge. The resistance will be used in calculating the flows.
	 */
	public void createResistanceArray(){
		resistances = new double[edges.size()];
		for(int i=0; i<edges.size(); i++){
			Edge edge = edges.get(i);
			resistances[edge.getId()] = 8 * BLOODVISCOSITY * edge.getWeight() / Math.pow(edge.getEdgeThickness() / 2, 4)
					* Math.PI;
		}
	}
	
	/**
	 * Labels cycles for identification
	 */
	public void labelCycles(){
		//A hashMap of the cycles with cycles as the key and their index as the value
		cyclesNames = new HashMap<List<Edge>, Integer>();
		/**
		 * A hashMap of the cycles with index as the key and cycle as the value
		 */
		int index =0;
		for(List<Edge> cycle: cycles){
			cyclesNames.put(cycle, index);
			ImgProcLog.write(cycle+" "+ cyclesNames.get(cycle)+ " ");
			index++;
		}
	}
	
	/**
	 * Determines which cycles each edge belongs to.
	 */
	public void assignCyclesToEdges(){
		edgesCycles = new int[edges.size()][2];
		for(int i=0; i<edges.size(); i++){
			for(int j=0; j<2; j++)
				edgesCycles[i][j] = NONEXISTENT; 
		}
		int index;
		//Find the cycles an edge belongs to
		for(int i=0; i<edges.size(); i++){
			index = 0;
			for(List<Edge> cycle: cyclesNames.keySet()){
				if(cycle.contains(edges.get(i))) {
						edgesCycles[i][index++] = cyclesNames.get(cycle);
				}
			}
		}
	}
	
	
	
	/**
	 * Builds equations based on pressure drops over edges for each mesh in the graph
	 * e.g. Va + Vb - Vc = 0;
	 */
	public int buildMeshPressureEquations(){
		int index = 0;
		for( List<Edge> cycle: cycles){
			constants[index][0] = 0;
			//Index of the current mesh
			Edge currentEdge = cycle.get(0);
			Edge nextEdge = null;
			Vertex srcVertex  = currentEdge.getStartV();
			Vertex startVertex  = currentEdge.getStartV();
			Vertex endVertex  = currentEdge.getDestV();
			Vertex nextVertex = null;
//			ImgProcLog.write("Current edge: "+ currentEdge+ " Start node: "+ startVertex + ", End node: "+ endVertex);
			for(Edge edge: endVertex.getEdges()){
//				ImgProcLog.write("Edges of "+ endVertex+ ": "+ endVertex.getEdges());
				if(!edge.equals(currentEdge) && cycle.contains(edge)){
					nextEdge = edge;
					nextVertex = endVertex.getOpposite(endVertex, nextEdge);
//					ImgProcLog.write("Next Edge: "+ nextEdge + " Next node: "+ nextVertex);
					break;
				}
			}
			while(!endVertex.equals(srcVertex)){
//				ImgProcLog.write("Current edge: "+ currentEdge+ " Start node: "+ startVertex + ", End node: "+ endVertex);
//				ImgProcLog.write("Next Edge: "+ nextEdge + " Next node: "+ nextVertex);
				if(startVertex.equals(currentEdge.getStartV()))
					pressureMatrix[index][currentEdge.getId()] += resistances[currentEdge.getId()];
				else pressureMatrix[index][currentEdge.getId()] -= resistances[currentEdge.getId()];
//				ImgProcLog.write("Edges of next node: "+ nextVertex.getEdges());
				for(Edge edge: nextVertex.getEdges()){
					if(!edge.equals(nextEdge) && cycle.contains(edge)){
						currentEdge = nextEdge;
						startVertex = endVertex ;
						endVertex = nextVertex;
						nextEdge = edge; 
						nextVertex = endVertex.getOpposite(endVertex, nextEdge);
//						ImgProcLog.write("Next Edge: "+ nextEdge + " next node: "+ nextVertex);
						break;
					}
				}
			}
			if(startVertex.equals(currentEdge.getStartV()))
				pressureMatrix[index][currentEdge.getId()] += resistances[currentEdge.getId()];
			else pressureMatrix[index][currentEdge.getId()] -= resistances[currentEdge.getId()];
			index++;
		}
		ImgProcLog.write("Number of mesh equations = "+ index);
		return index;
	}
	
	/**
	 * Builds equations based on pressure drop over edges of the graph.
	 * The equations are based on KCL equations over (n-2) nodes
	 */
	public int buildKCLs(int index){
		constants[index][0] = 0;
		nodes = graph.getVertices();
		for(int i=0; i<numNeededEquations; i++){
			Vertex node = nodes.get(i);
			for(Edge e: node.getEdges()){
				if(node.equals(e.getStartV())) pressureMatrix[index][e.getId()] = 1/(resistances[e.getId()]);
				else pressureMatrix[index][e.getId()] = -1/(resistances[e.getId()]);
			}
			index++;
		}
		ImgProcLog.write("Number of equations so far: "+ index);
		return index;
	}
	
	
	/**
	 * Finds a path of edges from the left pipe cell to the one on the right
	 * @param longestCycle The longest cycle in this graph
	 * @return A list of edges showing a path from the left pipe cell to the one on the right
	 */
	public ArrayList<Vertex> getPathFromStartToEnd(){
		ArrayList<Vertex> pathFromStartToEnd = new ArrayList<Vertex>();
		ArrayList<Vertex> traversed = new ArrayList<Vertex>();
		HashMap<Vertex, ArrayList<Vertex>> nodePaths = new HashMap<Vertex, ArrayList<Vertex>>();
		LinkedList<Vertex> queue = new LinkedList<Vertex>();
		for(int i=0; i<nodes.size(); i++){
			nodePaths.put(nodes.get(i), new ArrayList<Vertex>());
		}
		Vertex startNode = null;
		Vertex endNode = null;
		for(Vertex v:nodes){
			if(v.isPCellLeft()){
				startNode = v;
				break;
			}
		}
		Vertex currentN = null;
		traversed.add(startNode);
		queue.add(startNode);
		ArrayList<Vertex> temp = new ArrayList<Vertex>();
		temp.add(startNode);
		nodePaths.put(startNode, temp);
//		ImgProcLog.write("Current node: "+ startNode);
		boolean end = false;
		while(queue.size() != 0){
			currentN = queue.poll();
//			ImgProcLog.write("Path to node "+ currentN+ ": "+ nodePaths.get(currentN));
			for(Vertex v: currentN.getadjList()){
				if(!traversed.contains(v)){
					traversed.add(v);
					queue.add(v);
					ArrayList<Vertex> currentPath = new ArrayList<Vertex>(nodePaths.get(currentN));
					currentPath.add(v);
					nodePaths.put(v, currentPath);
				}
				if(v.isPCellRight()){
					endNode = v;
					pathFromStartToEnd = nodePaths.get(endNode);
					ImgProcLog.write("End node: "+ endNode);
					end = true;
					break;
				}
			}
			if(end) break;
		}
		ImgProcLog.write("pathFromStartToEnd : "+ pathFromStartToEnd);
		return pathFromStartToEnd;
	}
	
	/**
	 * Builds an equation of pressures over a path from the left pipe cell to the right
	 */
	public void buildLongPathEquation( int index, ArrayList<Vertex> nodesPath){
		ImgProcLog.write("index = "+ index);
		for(int i= 0; i<nodesPath.size()-1; i++){
			Vertex curr = nodesPath.get(i);
			Vertex next = nodesPath.get(i+1);
			Edge edge = graph.getEdge(curr, next);
			if(curr.equals(edge.getStartV()))
				pressureMatrix[index][edge.getId()] += resistances[edge.getId()];
			else pressureMatrix[index][edge.getId()] -= resistances[edge.getId()];
		}
	}
	
	/**
	 * Calculates the amount of flow in every edge based on the pressure drops over each edge
	 * @return An array of edge flow amounts for edges
	 */
	public double[][] calculateEdgeFlows(){
		for(int i=0; i<edgePressureArray.length; i++)
			edgeFlowArray[i][0] = edgePressureArray[i][0]/resistances[i];
		return edgeFlowArray;
	}
	
	/**
	 * Finds the maximum and minimum flow among the edges of the graph
	 * @return An array of doubles containing max and min edge flow rates respectively.
	 */
	public double findMinMaxFlowRate(){
		double maxFlowRate = 0;
    	for(int i=0; i<edges.size(); i++){
            if(edgeFlowArray[i][0] > maxFlowRate) maxFlowRate = edgeFlowArray[i][0];
        }
    	return maxFlowRate;
    }
	
	/**
	 * Normalizes the flow rates of the edges. It also finds and sets rounded secretion rates for each edge. 
	 */
	public void setSecretionRates(){
		ArrayList<Integer> secretions = new ArrayList<Integer>();
		ImgProcLog.write("Edge secretion results: ");
		double maxFlow = findMinMaxFlowRate();
		int roundedSecretionRate =0;
		for (Edge e : edges) {
			e.setFlowRate(Math.abs(edgeFlowArray[e.getId()][0]) / maxFlow);
			roundedSecretionRate = (int) Math.floor((muMax * e.getFlowRate() / (e.getFlowRate() + kS) * REACTIONPRECISION));
			e.setSecretionRate(roundedSecretionRate);
			secretions.add(roundedSecretionRate);
			ImgProcLog.write(e.getSecretionRate()+ "");
		}
		secretions.sort(null);
		minSecretionRate = secretions.get(0);
		maxSecretionRate = secretions.get(edges.size()-1);
		ImgProcLog.write("Max secretion rate = "+ maxSecretionRate );
		ImgProcLog.write("Min secretion rate = "+ minSecretionRate );
	}
	
	public int getMaxSecretion(){
		return maxSecretionRate;
	}
	
	public int getMinSecretion(){
		return minSecretionRate;
	}
	
	/**
	 * Builds equations obtained from mesh current method
	 * This method is not currently used since mesh equations yield homogeneous linear equations with 
	 * unlimited number of solutions.
	 */
	public void buildMeshEquations(){
		//A matrix of mesh current coefficients in the equations
		meshCurrents = new double[cycles.size()][cycles.size()];
		//The right side matrix
		constants = new double[cycles.size()][1];
		ImgProcLog.write("Mesh currents matrix: ");
		//Indicator of current line number in the equation matrix
		int index =0;
		for( List<Edge> cycle: cyclesNames.keySet()){
			constants[index][0] = 0;
			//Index of the current mesh
			int currentMesh = cyclesNames.get(cycle);
			double sum =0;
			for(Edge e: cycle){
				sum += resistances[e.getId()];
			}
			meshCurrents[index][currentMesh] = sum;
			for(Edge e: cycle){
				if(edgesCycles[e.getId()][1]!= NONEXISTENT){
					int cycleNum;
					if(edgesCycles[e.getId()][1] == currentMesh) cycleNum = edgesCycles[e.getId()][0];
					else cycleNum = edgesCycles[e.getId()][1];
					meshCurrents[index][cycleNum] -= resistances[e.getId()]; 
				}
			}
			ImgProcLog.write("Mesh #"+currentMesh+ ": " +Arrays.toString(meshCurrents[index]));
			index++;
		}
	}
	
//	public void createEquations(){
//	createResistanceArray();
//	labelCycles();
//	assignCyclesToEdges();
//	buildMeshEquations();
//	EquationSolver solver = new EquationSolver(cycles.size(), meshCurrents, constants);
//	meshFlowMatrix = solver.solve();
//	ImgProcLog.write("Mesh current results matrix: ");
//	ImgProcLog.write(Arrays.deepToString(meshFlowMatrix));
//}
}
