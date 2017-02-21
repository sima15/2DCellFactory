package cycleFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.WriteToFile;
import graph.Edge;
import graph.Graph;
import graph.Vertex;
import utils.ImgProcLog;
//import ij.ImagePlus;
//import skeletonize.DisplayGraph;

public class CycleFinder {

		List<Edge> edges;
		List<Vertex> nodes;
		boolean[][] adjMatrix;
		private ArrayList<List<Edge>> cycles;
//		private ImagePlus localThicknessImage;
		Graph graph;
		List<Edge> traversedList;

		public CycleFinder(Graph graph) {
			this.graph = graph;
//			this.localThicknessImage = graph.getLocalThicknessImage().duplicate();
		}

		public int getCycleSize(){
//			if(getCycles()== null) return 0;
//			return getCycles().size();
			return cycles.size();
		}
		
		/**
		 * Simplifies the graph by re-indexing vertices and edges to start from zero and increment by one.
		 * Also, merges left pipe cells together and right pipe cells.
		 */
		public Graph simplifyGraph(){
			graph.reindexVertices();
			graph.reindexEdges();
			nodes = graph.getVertices();
			edges = graph.getEdges();
			ImgProcLog.write("Number of edges: "+ edges.size());
			ImgProcLog.write("Number of nodes: "+ nodes.size());
//			ImgProcLog.write("Pipe cells from CycleFinder");
			ArrayList<Vertex> leftPipeCells = new ArrayList<Vertex>();
			ArrayList<Vertex> rightPipeCells = new ArrayList<Vertex>();
			ArrayList<Edge> removable = new ArrayList<Edge>();
			for(Vertex v: nodes){
				if(v.isPipeCell()){
//					ImgProcLog.write(v.toString() +" is left pipe cell: "+ v.isPCellLeft());
					if(v.isPCellLeft()) leftPipeCells.add(v);
					else rightPipeCells.add(v);
					for(Vertex v2: v.getadjList()){
//						ImgProcLog.write(v2.toString() + " edge: "+ v2.getEdge(v));
						if(v2.isPipeCell()){
//							ImgProcLog.write("Removing the edge between "+ v + " and "+ v2);
							removable.add(v2.getEdge(v));
						}
					}
				}
			}
			graph.delEdges(removable);
			ImgProcLog.write("Number of edges after side deletion: "+ graph.getEdges().size());
			int newRightPCellIndex = mergeVertices(leftPipeCells);
			mergeVertices(rightPipeCells);
			graph.getVertices().get(graph.getVertices().size()-1).setId(++newRightPCellIndex);
			ImgProcLog.write(" ---------------------- ");
			edges = graph.getEdges();
			ImgProcLog.write("Number of edges: "+ edges.size());
			nodes = graph.getVertices();
			ImgProcLog.write("Number of nodes after merging: "+ nodes.size());
//			ImgProcLog.write("Vertices:");
//			graph.dispVertDegree();
//			ImgProcLog.write(" ---------------------- ");
			graph.reindexEdges();
//			ImgProcLog.write("Edges:");
//			for(Edge e: edges)
//				ImgProcLog.write(e.toString());
			new WriteToFile( graph, "Output\\GraphPrunedLevel5.wrl"); 
			return graph;
		}
		
		/**
		 * Builds an adjacency matrix with nodes. For any node connected through an edge, it marks the matrix position as true.
		 */
		private void generateAdjacencyMatrix() {
			int matrSize = nodes.size();
			adjMatrix = new boolean[matrSize][matrSize];
			for (Edge edge : edges) {
					adjMatrix[(edge.getStartV().getId())][(edge.getDestV().getId())] = true;
					adjMatrix[(edge.getDestV().getId())][(edge.getStartV().getId())] = true;
			}
		}

		public List<List<Edge>> getRemainingCycles() {
			for (int i = 0; i < nodes.size(); i++) {

				for (int j = i + 1; j < nodes.size(); j++) {
					if (adjMatrix[i][j]) {
						List<Edge> cycle = findCycle(i, j);
						if (cycle != null) {
							cycles.add(cycle);
							traversedList.addAll(cycle);

						}
					}
					if (adjMatrix[j][i]) {
						List<Edge> cycle = findCycle(j, i);
						if (cycle != null) {
							cycles.add(cycle);
							traversedList.addAll(cycle);
						}
					}
				}
			}
			return cycles;
		}


		public ArrayList<List<Edge>> getCycles() {

			int cycleSize = 0;
			cycles = new ArrayList<List<Edge>>();
			List<Edge> missingEdges1 = new ArrayList<Edge>();
			List<Edge> missingEdges2 = new ArrayList<Edge>();
			while (true) {
				generateAdjacencyMatrix();
				generateCycles();
				getRemainingCycles();
				eliminateDuplicateCycles();
				if (cycleSize == cycles.size()) {
					break;
				}
				missingEdges1 = new ArrayList<Edge>();
				missingEdges2 = new ArrayList<Edge>();
				for (Edge e : edges) {
					Vertex n1 = e.getStartV();
					Vertex n2 = e.getDestV();
					int n1Label = n1.getId();
					int n2Label = n2.getId();
					if (adjMatrix[n1Label][n2Label] || adjMatrix[n2Label][n1Label]) {
						missingEdges1.add(e);
						//DisplayGraph.wantedList.add(e.getLabel());
					}
					if (adjMatrix[n1Label][n2Label] && adjMatrix[n2Label][n1Label]) {
						/*
						 * dataCopy[n1Label][n2Label] = false;
						 * dataCopy[n2Label][n1Label] = false;
						 */// getRemainingCycles();
						missingEdges2.add(e);
						//DisplayGraph.wantedList.add(e.getLabel());
					}
				}
				for (Edge e : missingEdges2) {
					graph.removeEdge(e);
				}
				// adjMatrix = dataCopy;
				//DisplayGraph.draw(missingEdges1, localThicknessImage);
				//DisplayGraph.draw(missingEdges2, localThicknessImage);
				//DisplayGraph.draw(traversedList, localThicknessImage);
				cycleSize = cycles.size();
			}

			generateAdjacencyMatrix();
			generateCycles();
			getRemainingCycles();
			eliminateDuplicateCycles();
			return cycles;
		}

		private void generateCycles() {
			cycles = new ArrayList<List<Edge>>();
			traversedList = new ArrayList<Edge>();
			for (Edge e : edges) {
				if (!traversedList.contains(e)) {
					Vertex n1 = e.getStartV();
					Vertex n2 = e.getDestV();
					int n1Label = n1.getId();
					int n2Label = n2.getId();
					List<Edge> cycle1 = null;
					List<Edge> cycle2 = null;
					if (adjMatrix[n1Label][n2Label]) {
						cycle1 = findCycle(n1Label, n2Label);
						if (cycle1 != null) {
							cycles.add(cycle1);
							traversedList.addAll(cycle1);
						}
					}
					if (adjMatrix[n2Label][n1Label]) {
						cycle2 = findCycle(n2Label, n1Label);
						if (cycle2 != null)
							if (cycle1 == null || !(cycle1.get(0).equals(cycle2.get(cycle2.size() - 2))
									&& cycle2.get(0).equals(cycle1.get(cycle1.size() - 2)))) {
								cycles.add(cycle2);
								traversedList.addAll(cycle2);
							}
					}
				}
			}
		}

		private void eliminateDuplicateCycles() {
			List<List<Integer>> cyclesSorted = new ArrayList<List<Integer>>();
			List<Integer> removeList = new ArrayList<Integer>();
			for (List<Edge> cycle : cycles) {
				List<Integer> cycleLabels = new ArrayList<Integer>();
				for (Edge e : cycle) {
					cycleLabels.add(e.getId());
				}
				Collections.sort(cycleLabels);
				cyclesSorted.add(cycleLabels);
			}

			for (int i = 0; i < cyclesSorted.size() - 1; i++) {
				List<Integer> cycle1 = cyclesSorted.get(i);
				for (int j = i + 1; j < cyclesSorted.size(); j++) {
					List<Integer> cycle2 = cyclesSorted.get(j);
					if (cycle1.size() == cycle2.size()) {
						boolean removeFlag = true;
						for (int k = 0; k < cycle1.size(); k++) {
							if (cycle1.get(k).intValue() != cycle2.get(k).intValue()) {
								removeFlag = false;
								break;
							}
						}
						if (removeFlag) {
							removeList.add(j);
						}
					}
				}
			}
		}

		static int maxCount = 0;

		private List<Edge> findCycle(int source, int current) {
			List<Edge> cycle = new ArrayList<Edge>();
			if (findCycle(source, current, cycle, source)) {
				cycle.add(graph.getEdge(nodes.get(source), nodes.get(current)));
				if (maxCount < cycle.size())
					maxCount = cycle.size();
				return cycle;
			}
			return null;
		}

		/**
		 * Continues with inner edges in the cycle until the last node equals the starting (source) node
		 * @param previous
		 * @param current
		 * @param cycle List of edges in the cycle so far
		 * @param source The starting node in the cycle to be found
		 * @return if this can be a cycle or not
		 */
		private boolean findCycle(int previous, int current, List<Edge> cycle, int source) {
			boolean history1 = adjMatrix[previous][current];
			boolean history2 = adjMatrix[current][previous];
			adjMatrix[previous][current] = false;
			adjMatrix[current][previous] = false;

			if (current == source) {
				adjMatrix[current][previous] = history2;
				return true;
			}
			Vertex previousNode = nodes.get(previous);
			Vertex currentNode = nodes.get(current);
			Vertex nextNode = null;
			Map<String, Vertex> candidateNodes = new HashMap<String, Vertex>();
			for (Edge e : currentNode.getEdges()) {
				Vertex candidateNode = null;
				if (e.getStartV().equals(currentNode)) {
					candidateNode = e.getDestV();
				} else {
					candidateNode = e.getStartV();
				}
				if (!candidateNode.equals(previousNode)) {
					int angle = angleBetweenTwoPointsWithFixedPoint(previousNode.getcoord()[0], previousNode.getcoord()[1],
							candidateNode.getcoord()[0], candidateNode.getcoord()[1], currentNode.getcoord()[0], currentNode.getcoord()[1]);
					candidateNodes.put(Integer.toString(angle), candidateNode);
				}
			}
			int minAngle = Integer.MAX_VALUE;
			for (String key : candidateNodes.keySet()) {
				int temp = Integer.parseInt(key);
				if (temp < minAngle) {
					minAngle = temp;
				}
			}

			if (minAngle < Integer.MAX_VALUE)
				nextNode = candidateNodes.get(Integer.toString(minAngle));

			if (nextNode != null) {
				int next = nextNode.getId();
				if (adjMatrix[current][next] && findCycle(current, next, cycle, source)) {
					Edge edge = graph.getEdge(currentNode, nextNode);
					if (edge != null)
						cycle.add(edge);
					else
						System.out.println("Error: " + currentNode + "  " + nextNode);
					adjMatrix[current][previous] = history2;
					return true;
				}
			}
			adjMatrix[previous][current] = history1;
			adjMatrix[current][previous] = history2;
			return false;
		}

		/**
		 * Calculates the smaller angle between two lines, first the line crossing previous and candidate node and the second 
		 * crossing candidate and current node 
		 * @param point1X x value of previous node
		 * @param point1Y y value of previous node
		 * @param point2X x value of candidate node
		 * @param point2Y y value of candidate node
		 * @param fixedX x value of current node
		 * @param fixedY y value of current node
		 * @return The angle between these two lines
		 */
		public static int angleBetweenTwoPointsWithFixedPoint(double point1X, double point1Y, double point2X,
				double point2Y, double fixedX, double fixedY) {

			double angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
			double angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);
			int degreeAngle = (int) Math.toDegrees(angle1 - angle2);
			if (degreeAngle < 0)
				return 360 + degreeAngle;
			else
				return degreeAngle;
		}

		public void displayCycles() {
			System.out.println("Number of Cycles: " + cycles.size());
			for (List<Edge> cycle : cycles) {
				System.out.println("Number of edges in cycle: " + cycle.size());
				System.out.println(cycle);
			}
		}

		/**
		 * Merges two pipe cells into one
		 * @param v1 First pipe cell
		 * @param v2 Second pipe cell
		 * @param g The graph containing the pipe cells
		 */
		public int mergeVertices(ArrayList<Vertex> sidePipeCells){
			Vertex toRemain = sidePipeCells.get(0);
			for(Vertex v: sidePipeCells){
				if(v.getId()<toRemain.getId()) toRemain = v;
			}
			ArrayList<Vertex> removableV = sidePipeCells;
			removableV.remove(toRemain);
			ArrayList<Edge> removableE = new ArrayList<Edge>();
			ArrayList<Edge> insertableE = new ArrayList<Edge>();
			
			ImgProcLog.write("Removing nodes "+ removableV);
			ImgProcLog.write("Keeping node "+ toRemain);
			
			for(int i=0; i<removableV.size(); i++){
				Vertex tempV = removableV.get(i);
				for(int j=0; j<tempV.getEdges().size(); j++){
					Edge newEdge  = new Edge(tempV.getEdges().get(j).getId(), toRemain, tempV.getOpposite(tempV, tempV.getEdges().get(j)));
					insertableE.add(newEdge);
					removableE.add(tempV.getEdges().get(j));
				}
			}
			ArrayList<Edge> edges = graph.getEdges();
			edges.addAll(insertableE);
			graph.setEdges(edges);
			graph.delEdges(removableE);
			for(int i=0; i<removableV.size(); i++){
				graph.removeVertex(removableV.get(i));
			}
			return toRemain.getId();
		}
		
		
		public Graph getSimplifiedGraph(){
			return graph;
		}
}