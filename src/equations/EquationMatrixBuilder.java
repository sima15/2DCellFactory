/**
 * 
 */
package equations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import graph.Edge;
import graph.Graph;
import graph.Vertex;
import utils.ImgProcLog;

/**
 * @author Sima Mehri
 * @author Delin Davis
 * This class creates the matrices needed to solve flow calculation equations
 */
public class EquationMatrixBuilder {

	Graph graph;
	ArrayList<Vertex> nodes;
	ArrayList<Edge> edges;
	private double[][] equationLeftSide;
	private double[][] equationRightSide;
	private double[][] flowRateMatrix;
	private List<List<Edge>> cycles;
	private double bloodViscosity = 0.00089;
	private static final int _PRESSURE = 1000; // kPa
	
	public EquationMatrixBuilder(Graph graph, ArrayList<List<Edge>> cycles){
		this.graph = graph;
		nodes = graph.getVertices();
		edges = graph.getEdges();
		this.cycles = cycles;
	}
	

	public void generateEquationMatrix() {

		equationLeftSide = new double[nodes.size() + cycles.size()][edges.size() + 2];
		equationRightSide = new double[nodes.size() + cycles.size()][1];
		ImgProcLog.write("Nodes + cycles size: " + ((int) nodes.size() + cycles.size()));
		ImgProcLog.write("Edges size: " + ((int) edges.size() + 2));
		int equationCounter = 0;
		List<Edge> longestCycle = getLongestCycle();
		ImgProcLog.write("Longest cycle: "+ longestCycle);
		cycles.remove(longestCycle);
		for (Vertex v : nodes) {
			populateVertexEquation(v, equationLeftSide, equationRightSide, equationCounter++);
		}

		for (List<Edge> cycle : cycles) {
			populateCycleEquations(cycle, equationLeftSide, equationRightSide, equationCounter++);
		}

		populatePowerCycleEquation(longestCycle, equationLeftSide, equationRightSide, equationCounter++);
		ImgProcLog.write("Adjacency matrix:");
		ImgProcLog.write(Arrays.deepToString(equationLeftSide));
		ImgProcLog.write("Equation right side:");
		ImgProcLog.write(Arrays.deepToString(equationRightSide));
		ImgProcLog.write("Counter = "+ equationCounter);
	}
	
	private void populatePowerCycleEquation(List<Edge> longestCycle, double[][] adjMatrix,
			double[][] constants, int equationCounter) {
		System.out.println(longestCycle);
		List<Edge> pathFromStartToEnd = new ArrayList<Edge>();
		longestCycle.addAll(longestCycle);
		boolean startNodeFlag = false;
		boolean endNodeFlag = false;
		Vertex currentNode = null;
		Edge currentEdge = null;
		for (int i = 0; i < longestCycle.size() - 1; i++) {
			currentEdge = longestCycle.get(i);
			Edge nextEdge = longestCycle.get(i + 1);
			if (currentEdge.getStartV().equals(nextEdge.getStartV())
					|| currentEdge.getStartV().equals(nextEdge.getDestV())) {
				currentNode = currentEdge.getDestV();
			} else {
				currentNode = currentEdge.getStartV();
			}
			if (currentNode.isPCellLeft()) {
				startNodeFlag = true;
			}
			if (currentNode.isPCellRight()) {
				endNodeFlag = true;
			}
			if (startNodeFlag && endNodeFlag) {
				break;
			}
			if (startNodeFlag || endNodeFlag) {
				pathFromStartToEnd.add(currentEdge);
			}
		}
		populateCycleEquations(pathFromStartToEnd, adjMatrix, constants, equationCounter);
		ImgProcLog.write("pathFromStartToEnd : " + pathFromStartToEnd);
//		System.out.println("pathFromStartToEnd : " + pathFromStartToEnd);
//		if (pathFromStartToEnd.get(0).equals(endNode)) {
		if (pathFromStartToEnd.get(0).getStartV().isPCellRight() || pathFromStartToEnd.get(0).getDestV().isPCellRight()) {
			constants[equationCounter][0] = 0;
		} else {
			constants[equationCounter][0] = _PRESSURE;
		}
	}

	private List<Edge> getLongestCycle() {
		List<Edge> longestCycle = new ArrayList<Edge>();
		for (List<Edge> cycle : cycles) {
			if (cycle.size() > longestCycle.size())
				longestCycle = cycle;
		}
		return longestCycle;
	}

	private void populateCycleEquations(List<Edge> cycle, double[][] adjMatrix, double[][] constants,
			int equationCounter) {
		Vertex currentNode = null;
		Vertex nextNode = null;
		Edge currentEdge = null;
		if (cycle.size() == 1) {
			int edgeLabel = cycle.get(0).getId();
			adjMatrix[equationCounter][edgeLabel] = 1;
		} else {
			for (int i = 0; i < cycle.size(); i++) {
				currentEdge = cycle.get(i);
				int edgeLabel = currentEdge.getId();
				if (i < cycle.size() - 1) {
					Edge nextEdge = cycle.get(i + 1);
					if (currentEdge.getStartV().equals(nextEdge.getStartV())
							|| currentEdge.getStartV().equals(nextEdge.getDestV())) {
						currentNode = currentEdge.getDestV();
						nextNode = currentEdge.getStartV();
					} else {
						currentNode = currentEdge.getStartV();
						nextNode = currentEdge.getDestV();
					}
				} else {
					currentNode = nextNode;
					if (currentNode.equals(currentEdge.getStartV())) {
						nextNode = currentEdge.getDestV();
					} else {
						nextNode = currentEdge.getStartV();
					}
				}
				int nodeLabel = currentNode.getId();
				int v1label = nextNode.getId();
				double length = currentEdge.getWeight();
				double resistance = 8 * bloodViscosity * length / Math.pow(currentEdge.getEdgeThickness() / 2, 4)
						* Math.PI;
				if (v1label > nodeLabel) {
					adjMatrix[equationCounter][edgeLabel] = -resistance;
				} else {
					adjMatrix[equationCounter][edgeLabel] = resistance;
				}
				constants[equationCounter][0] = 0;
			}
		}
	}

//	private double getLength(Edge e) {
//		double maxLength = 0;
//		for (Edge se : e.getSkeletonEdges()) {
//			if (maxLength < se.getWeight()) maxLength = se.getWeight();
//		}
//		return maxLength;
//	}

	private void populateVertexEquation(Vertex node, double[][] adjMatrix, double[][] constants,
			int equationCounter) {
		int nodeLabel = (node.getId());
		for (Edge edge : node.getEdges()) {
			int v1label = edge.getStartV().getId();
			int v2label = edge.getDestV().getId();
			int edgeLabel = (edge.getId());

			if (v1label > nodeLabel || v2label > nodeLabel) {
				adjMatrix[equationCounter][edgeLabel] = 1;
			} else {
				adjMatrix[equationCounter][edgeLabel] = -1;
			}

			if (node.isPCellLeft()) {
				adjMatrix[equationCounter][edges.size()] = -1;
			}

			if (node.isPCellRight()) {
				adjMatrix[equationCounter][edges.size() + 1] = 1;
			}
			constants[equationCounter][0] = 0;
		}
	}

	public Vertex addOrGetNode(Vertex skeletonVertex) {
//		int x = (int) Math.floor(skeletonVertex.getPoints().get(0).x / 5) * 5;
//		int y = (int) Math.floor(skeletonVertex.getPoints().get(0).y / 5) * 5;
//		Vertex node = new Vertex(x, y);
//		if ((nodes.get(node.getKey())) == null) {
//			nodes.put(node.getKey(), node);
//		} else {
//			node = nodes.get(node.getKey());
//		}
//		return node;
		return null;
	}
	
	/**
	 * Returns a matrix for the left side of equations necessary to solve the flows
	 * @return equation left side matrix
	 */
	public double[][] getEquationLeftSide(){
		return equationLeftSide;
	}
	
	/**
	 * Returns a matrix for the right side of equations necessary to solve the flows
	 * @return equation right side matrix
	 */
	public double[][] getEquationRightSide(){
		return equationRightSide;
	}
}
