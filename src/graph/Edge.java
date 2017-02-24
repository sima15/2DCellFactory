package graph;

import java.util.ArrayList;
import java.util.Random;

//import skeleton_analysis.Edge;
//import skeleton_analysis.Point;

public class Edge {
	int id;
	Vertex startV;
    Vertex destV;
    double weight;
    double flowRate;
    double secretionRate;

    /**
     * Builds an Edge for a graph
     * @param id Unique ID of an edge in the graph
     * @param s The starting vertex
     * @param d The ending vertex
     */
    public Edge(int id, Vertex s, Vertex d){
        this.id = id;
    	startV = s;
        destV = d;
        calWeight();
        startV.addEdge(this);
    }
    
    
    @Override
    public String toString(){
//    	return id + " v1: "+startV + " v2: "+ destV +", " ;
    	return String.valueOf(id);
    }
    
    public int getId(){
    	return id;
    }
    
    public Vertex getStartV(){
    	return startV;
    }
    
    public Vertex getDestV(){
    	return destV;
    }
    
    public void calWeight(){
    	weight = new Graph().calDistance(this.getStartV(), this.getDestV());
    }
    
    public void calWeight(Graph g){
    	weight = g.calDistance(this.getStartV(), this.getDestV());
    }
    
    public double getWeight(){
    	return weight;
    }
    
    public void setWeight(double w){
    	weight = w;
    }
    
    public int getEdgeThickness(){
		int thickness = (int) Math.random()+ 8;
//		for(Edge e:skeletonEdges){
//			thickness = Math.max(getEdgeThickness(e),thickness);
//		}
//		if(thickness == 0){
//			System.out.println("0 thicknss for " + label);
//		}
		return Math.max(thickness, 8);
	}
	
	private int getEdgeThickness(Edge skeletonEdge) {
		int avgThickness = 0;
		int count = 0;
//		for (Point point : skeletonEdge.getSlabs()) {
//			avgThickness += (int) thicknessImage.getChannelProcessor().getf(point.x, point.y);
//			count++;
//		}
//		return avgThickness/count;
		return avgThickness;
	}


	/**
	 * Changes the id of the edge
	 * @param i New edge id
	 */
	public void setId(int i) {
		id = i;
	}
	
	public double getFlowRate() {
		return flowRate;
	}

	public void setFlowRate(double flowRate) {
		this.flowRate = flowRate;
	}
	
	public double getSecretionRate() {
		return flowRate;
	}

	public void setSecretionRate(double flowRate) {
		this.flowRate = flowRate;
	}
}
