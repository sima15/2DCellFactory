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

import equations.EquationBuilder;
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
	EquationBuilder equationBuilder;
	/**
	 * Contains coefficients of the mathematical equation for every edge
	 */
	private double[][] edgeEquations ;
	/**
	 * A map of only the edges which will remain in the simulation and their flow rates
	 */
	private HashMap<Integer, Double> edgeSecretionMap;
	/**
	 * A hashMap of a limited number of groups of cells and their corresponding secretion rates
	 */
	private HashMap<Integer, Double> secretionMap = new HashMap<Integer, Double>();;
	/**
	 * A map of edge numbers and cells that belong to them. The cells information is taken from state files
	 * and is very comprehensive
	 */
	private LinkedHashMap<Integer, String> edgeCellMap;
	/**
	 * A map of how many cells belong to an edge
	 */
	private HashMap<Integer, Integer> edgeCellLength;
	
	
	public AgentStateBuilder(Graph g, EquationBuilder equationBuilder) {
		graph = g;
		edges = graph.getEdges();
		this.equationBuilder = equationBuilder;
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
		edgeCellMap = new LinkedHashMap<Integer, String>();
		edgeCellLength = new HashMap<Integer, Integer>();
		calEdgeEquations();
		ImgProcLog.write("Assigning cells to groups of edges:");
		for (int i = 0; i < agentArray.length; i++) {
			String[] elements = agentArray[i].split(",");
			double x = (256 - Double.parseDouble(elements[10]));
			double y = (512 - Double.parseDouble(elements[11]));
			int edgeId = assignCellToEdge(x, y);
			int groupId = assignToGroup(edgeId);
			if (edgeCellMap.containsKey(groupId)) {
				String newText = edgeCellMap.get(groupId);
				newText += agentArray[i] + ";\n";
				edgeCellMap.put(groupId, newText);
				edgeCellLength.put(groupId, edgeCellLength.get(groupId)+1);
			} else {
				edgeCellMap.put(groupId, "\n" + agentArray[i] + ";\n");
				edgeCellLength.put(groupId, 1);
			}
		}
		//Ordering MovingCells in ascending order for correct insertion into agentState file
		ArrayList<Integer> orderedCellArray = new ArrayList<Integer>();
		orderedCellArray.addAll(edgeCellMap.keySet());
		orderedCellArray.sort(null);
		for (int key : orderedCellArray) {
			Element newMovingCells = movingCells.clone();
			newMovingCells.setAttribute("name", "MovingCells" + key);
			newMovingCells.setText(edgeCellMap.get(key));
			agentRoot.getChild("simulation").addContent(newMovingCells);
		}
		agentRoot.getChild("simulation").removeContent(movingCells);

		try {
			agentFileParser.replaceXMLFile(path + agentFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ImgProcLog.write("Agent state (lastIter) file modified. ");
		ImgProcLog.write("Secretion map: ");
		printSecMap();
	}
	
	
	
	/**
	 * Assigns the given cell to its nearest edge
	 * @param x0 X value of the cell coordinates
	 * @param y0 Y value of the cell coordinates
	 * @return Id of the edge to which the cell is assigned, if it is not assigned to any edges, returns 1 plus the highest index.
	 */
	public int assignCellToEdge(double x0, double y0){
		final int MINDISTANCE = 20;
		final int THRESHOLD = 10;
//		final int PIPEDISTANCE = 40;
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
		if(y0 <= 512/7 || y0 >= 6*512/7)
			return edgeId;
		else if(distance > MINDISTANCE)
			//Arbitrary edgeId of the cells that don't belong to any edges
			edgeId = edges.size();
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
	 * Assigns each edge to a limited number of moving cell groups
	 * @param edgeId Id of the edge which needs to be assigned to a group of edges
	 * @return groupId Id of the group the edge gets assigned to.
	 */
	private int assignToGroup(int edgeId){
		
		final int CELLTYPES = 9;
		int maxSecretion = equationBuilder.getMaxSecretion();
		int minSecretion = equationBuilder.getMinSecretion();
		int diff = (maxSecretion - minSecretion)/CELLTYPES;
		int groupId = 0;
		double start = minSecretion;
		double end = minSecretion + diff;
		secretionMap.put(CELLTYPES, 0.0);
		if(edgeId == edges.size()) {
			groupId = CELLTYPES;
			if(!secretionMap.containsKey(groupId))
				secretionMap.put(groupId, 0.0);
			return groupId;
		}
		for(int i=0; i<CELLTYPES; i++){
			if(edges.get(edgeId).getSecretionRate()>=start && edges.get(edgeId).getSecretionRate() < end){
				groupId = i;
				if(!secretionMap.containsKey(i))
					secretionMap.put(i, start + diff/2);
//				ImgProcLog.write("Edge: " + edges.get(edgeId).getId()+ ", Start = "+ start+ ", End = "+ end+", secretion = "+ secretionMap.get(i));
				return groupId;
			}
			else {
				start = end;
				end = end + diff;
			}
		}
		return groupId;
	}
	
	/**
	 * Is not used any more
	 * Creates a HashMap of edges which are going to remain in the simulation and
	 * their secretion rates
	 * @return
	 */
	public HashMap<Integer, Double> createEdgeSecretionMap(){
		edgeSecretionMap = new HashMap<Integer, Double>();
		for(Integer i: edgeCellMap.keySet()){
			if(i== edges.size())edgeSecretionMap.put(i, 0.0); 
			else edgeSecretionMap.put(i, graph.getEdges().get(i).getSecretionRate());
		}
		return edgeSecretionMap;
	}
	
	public void printSecMap(){
		for(int i:secretionMap.keySet()){
			ImgProcLog.write(i+", "+ secretionMap.get(i));
		}
	}
	
	public HashMap<Integer, Double> getSecretionMap(){
		return secretionMap;
	}
	
	public HashMap<Integer, Integer> getEdgeCellLength(){
		return edgeCellLength;
	}

}
