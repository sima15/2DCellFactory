/**
 * 
 */
package protocolModfr;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import org.jdom2.Element;

import graph.Edge;
import graph.Graph;
import graph.Vertex;
import utils.ImgProcLog;
import utils.XMLParserFromiDynomics;

/**
 * @author Sima Mehri
 * Recreates an agent state file based on the processed graph. The graph is already built based on the vascular network
 * which was formed in the 2D cell factory.
 *
 */
public class AgentStateBuilder {

	private String agentFilePath = "\\lastIter\\agent_State(last).xml";
	XMLParserFromiDynomics agentFileParser;
	Graph graph;
	ArrayList<Edge> edges;
	/**
	 * Contains coefficients of the mathematical equation for every edge
	 */
	private double[][] edgeEquations ;
	/**
	 * A map of only the edges which will remain in the simulation and their flow rates
	 */
	private HashMap<Integer, Double> reducedEdgeMap;
	/**
	 * A map of edge numbers and cells that belong to them. The cells information is taken from state files
	 * and is very comprehensive
	 */
	private LinkedHashMap<Integer, String> edgeMap;
	/**
	 * A map of how many cells belong to an edge
	 */
	private HashMap<Integer, Integer> edgeCellLength;
	
	
	public AgentStateBuilder(Graph g) {
		graph = g;
		edges = graph.getEdges();
	}
	
	/**
	 * Modifies the agent state file of the last iteration for use in restarting iDynomics from a previous run
	 * @param path The path to the current simulation directory
	 */
	public void modifyAgentStateFile(String path){
		ImgProcLog.write("Agent file path: " + path + agentFilePath);
		agentFileParser = new XMLParserFromiDynomics(path + agentFilePath);
		
		Element agentRoot = agentFileParser.get_localRoot();
		List<Element> speciesList = agentRoot.getChild("simulation").getChildren("species");
		Element movingCells = null;
		for (Element s : speciesList) {
			if (s.getAttributeValue("name").equals("MovingCells")) {
				movingCells = s;
			}
		}
		String text = movingCells.getText();
		String[] agentArray = text.split(";\n");
		ImgProcLog.write("Number of moving cells in agent state file: "+ agentArray.length);
		edgeMap = new LinkedHashMap<Integer, String>();
		edgeCellLength = new HashMap<Integer, Integer>();
		calEdgeEquations();
//		ImgProcLog.write("Assigning cells to edges. The distances are: ");
		for (int i = 0; i < agentArray.length; i++) {
			String[] elements = agentArray[i].split(",");
			double x = (256 - Double.parseDouble(elements[10]));
			double y = (512 - Double.parseDouble(elements[11]));
			int edgeId = assignCellToEdge(x, y);
			if(edgeId == -1) continue;
			if (edgeMap.containsKey(edgeId)) {
				String newText = edgeMap.get(edgeId);
				newText += agentArray[i] + ";\n";
				edgeMap.put(edgeId, newText);
				edgeCellLength.put(edgeId, edgeCellLength.get(edgeId)+1);
			} else {
				edgeMap.put(edgeId, "\n" + agentArray[i] + ";\n");
				edgeCellLength.put(edgeId, 1);
			}
		}

		for (int key : edgeMap.keySet()) {
			Element newMovingCells = movingCells.clone();
			newMovingCells.setAttribute("name", "MovingCells" + key);
			newMovingCells.setText(edgeMap.get(key));
			agentRoot.getChild("simulation").addContent(newMovingCells);
		}
		agentRoot.getChild("simulation").removeContent(movingCells);

		try {
			agentFileParser.replaceXMLFile(path + agentFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ImgProcLog.write("Agent state (lastIter) file modified. ");
		createReducedEdgeMap();
	}
	
	/**
	 * Assigns the given cell to its nearest edge
	 * @param x0 X value of the cell coordinates
	 * @param y0 Y value of the cell coordinates
	 * @return Id of the edge to which the cell is assigned, if it is not assigned to any edges, returns -1.
	 */
	public int assignCellToEdge(double x0, double y0){
		final int MINDISTANCE = 20;
		final int THRESHOLD = 10;
		double distance = Integer.MAX_VALUE;
		int edgeId = 0;
		Edge curr;
		double x1 =0;
		double x2= 0;
		for(int i=0; i<edgeEquations.length; i++){
			curr = edges.get(i);
			x1 = curr.getStartV().getcoord()[0];
			x2 = curr.getDestV().getcoord()[0];
			if(x2<x1){
				double temp = x1;
				x1 = x2;
				x2 = temp;
			}
//			ImgProcLog.write("(x1, x2) = ("+ x1 + ", "+ x2+ ")");
			//Check to see if the cell is within x-range of this edge
			if(x0>= x1-THRESHOLD && x0<=x2+THRESHOLD){
				double y1 = curr.getStartV().getcoord()[1];
				double y2 = curr.getDestV().getcoord()[1];
				if(y2<y1){
					double temp = y1;
					y1 = y2;
					y2 = temp;
				}
//				ImgProcLog.write("(y1, y2) = ("+ y1 + ", "+ y2+ ")");
				//Check to see if the cell is within y-range of this edge
				if(y0>= y1-THRESHOLD && y0<=y2+THRESHOLD){
					double distanceFromI = getPointToEdgeDistance(x0, y0, edgeEquations[i]);
					if(distanceFromI < distance) {
						distance = distanceFromI;
						edgeId = i;
					}
				}
			}
		}
		if(distance > MINDISTANCE) 
			edgeId = -1;
		return edgeId;
	}
	
	/**
	 * Finds the line equations for each edge
	 * @return Edge line equation coefficients
	 */
	public double[][] calEdgeEquations(){
		int numEdges = edges.size();
		edgeEquations = new double[numEdges][3]; // ax + by + c = 0
		double m; //Line slope 
		double x1;
		double y1; 
		for(int i=0 ; i<numEdges; i++){
			Vertex node1 = edges.get(i).getStartV();
			Vertex node2 = edges.get(i).getDestV();
			x1 = node1.getcoord()[0];
			y1 = node1.getcoord()[1];
			m = (node2.getcoord()[1]-y1)/(node2.getcoord()[0]-x1);
			edgeEquations[i][0] = m; //a
			edgeEquations[i][1] = -1; //b
			edgeEquations[i][2] = y1 - m*x1; //c
		}
		return edgeEquations;
	}
	
	/**
	 * Calculates the vertical distance of a point from a line
	 * @param x0 X-value of the point
	 * @param y0 Y-value of the point
	 * @param lineEquation Coefficients of the line in the form of ax + by + c = 0
	 * @return Distance from the line 
	 */
	public double getPointToEdgeDistance(double x0, double y0, double[] lineEquation){
		double numerator = Math.abs(lineEquation[0]*x0 + lineEquation[1]*y0 + lineEquation[2]);
		double denuminator = Math.sqrt((lineEquation[0]*lineEquation[0] + lineEquation[1]*lineEquation[1]));
		return numerator/denuminator;
	}
	
	/**
	 * Creates a HashMap of edges which are going to remain in the simulation and
	 * their secretion rates
	 * @return
	 */
	public HashMap<Integer, Double> createReducedEdgeMap(){
		reducedEdgeMap = new HashMap<Integer, Double>();
		for(Integer i: edgeMap.keySet()){
			reducedEdgeMap.put(i, graph.getEdges().get(i).getSecretionRate());
		}
		return reducedEdgeMap;
	}
	
	public HashMap<Integer, Double> getReducedMap(){
		return reducedEdgeMap;
	}
	
	public HashMap<Integer, Integer> getEdgeCellLength(){
		return edgeCellLength;
	}

}
