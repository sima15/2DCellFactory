package graph;

import java.util.ArrayList;
import java.util.ListIterator;

import data.DataRetrieval;
import sun.security.util.Length;
import utils.ImgProcLog;

/**
 * 
 * @author Sima Mehri
 * A class to create spatial graphs
 */
public class Graph {
	private double[][] data ;
	private ArrayList<Vertex> vertices;
	private ArrayList<Vertex> pipeCells;
	private ArrayList<Edge> edges;
	private int numPipeCells;
	private int GRAPHLENGTH;

	/**
	 * Gets the coordinates of vertices from XML file read
	 * @return Vertex coordinates for creating graphs
	 */
	public double[][] importVData(){
		numPipeCells = DataRetrieval.getNumPipeCells();
		return DataRetrieval.getData();
	}
	
	/**
	 * Constructs an empty graph 
	 */
	public Graph(){
		vertices = new ArrayList<Vertex>();
		edges = new ArrayList<Edge>();
		pipeCells = new ArrayList<Vertex>();
	}
	
	/**
	 * Constructs a graph and loads it with the given vertices and edges
	 */
	public Graph(ArrayList<Vertex> vertices, ArrayList<Edge> edges){
		setVertices(vertices);
		setEdges(edges);
	}
	/**
	 * Constructs a graph and loads it with the given vertices 
	 */
	public Graph(ArrayList<Vertex> vertices){
		setVertices(vertices);
	}
	
	/**
	 * Creates vertices of the graph
	 * @return an ArrayList of vertices to be placed in the graph
	 */
    public ArrayList<Vertex> createVertices(){
    	data = importVData();
        vertices = new ArrayList<>();
        double[] coordinates; 
        int id =0;
        int numVCells = data.length-numPipeCells;
        //Add consumer cells to the vertices
        for (int i=0; i<numVCells; i++){
            coordinates = data[i];
            Vertex v = new Vertex(id, coordinates[0], coordinates[1], coordinates[2]);
            id++;
            vertices.add(v);
        }
        //Add left pipe cells to the vertices
        for (int i=numVCells; i< data.length-getNumPipeCellsOnSide(); i++){
            coordinates = data[i];
            Vertex v = new Vertex(id, coordinates[0], coordinates[1], coordinates[2]);
            id++;
            v.setPipeCell();
            v.setPCellLeft();
            vertices.add(v);
            pipeCells.add(v);
        }
        //Add right pipe cells to the vertices
        for (int i = data.length-getNumPipeCellsOnSide(); i< data.length; i++){
            coordinates = data[i];
            Vertex v = new Vertex(id, coordinates[0], coordinates[1], coordinates[2]);
            id++;
            v.setPipeCell();
            v.setPCellRight();
            vertices.add(v);
            pipeCells.add(v);
        }
        return vertices;
    }

    /**
     * Calculates the distance between two given vertices
     * @param v1 First Vertex
     * @param v2 Second Vertex
     * @return distance in units of distance
     */
    public double calDistance(Vertex v1, Vertex v2 ){
        double[] v1Coords = v1.getcoord();
        double[] v2Coords = v2.getcoord();
        double dist = Math.sqrt(Math.pow(v1Coords[0] - v2Coords[0], 2) + Math.pow(v1Coords[1] -v2Coords[1], 2)
        	+ Math.pow(v1Coords[2] -v2Coords[2], 2));
        return  dist;
    }

    /**
     * Creates the edges for a graph which only has vertices assigned so far
     * @param vertices The already assigned vertices to the graph
     * @return edges created by this method for the current graph
     */
    public ArrayList<Edge> createEdges(ArrayList<Vertex> vertices){
    	edges = new ArrayList<Edge>();
    	int edgeID = 0;
    	ListIterator<Vertex> iterator2;
        for(int i=0; i<vertices.size(); i++) {
            iterator2 = vertices.listIterator(i+1);
            while (iterator2.hasNext()){
                Vertex next = iterator2.next();
                Vertex curr = vertices.get(i);
                double dist = calDistance(curr, (next)); 
                if (dist < 10){
                    Edge edge = new Edge(edgeID++, curr, next );
                    edges.add(edge);
                }
            }
        }
        GRAPHLENGTH = (int) calDistance(pipeCells.get(0), pipeCells.get(pipeCells.size()-1));
        ImgProcLog.write("Graph length = "+ GRAPHLENGTH);
        //Creates edges among pipe cells
        for(int i=0; i<pipeCells.size(); i++){
        	for(int j=i+1; j<pipeCells.size(); j++){
        		if(calDistance(pipeCells.get(i), pipeCells.get(j))<100){
//        		if(calDistance(pipeCells.get(i), pipeCells.get(j))<GRAPHLENGTH/3){
        		Edge edge = new Edge(edgeID++, pipeCells.get(i), pipeCells.get(j));
                	edges.add(edge);
        		}
        	}
        }
        return edges;
    }

    
    
    public void dispVertDegree(){
    	for (int i=0; i<vertices.size(); i++){
    		ImgProcLog.write("Vertex: "+ vertices.get(i));
//    		System.out.println(" "+vertices.get(i).getDegree()+ "  ");
//    		System.out.println("Vertex: "+ vertices.get(i)); //+ " , Degree: "+ vertices.get(i).getDegree() );
    	}
    }
    
    
    public ArrayList<Vertex> getVertices(){
    	return vertices;
    }
    
    public void setVertices(ArrayList<Vertex> v){
    	vertices = v;
    }
    
    
    public ArrayList<Edge> getEdges(){
    	return edges;
    }
    
    public void setEdges(ArrayList<Edge> e){
    	edges = e;
    	setEdgeWeights(edges);
    }
    
    public void delEdges(ArrayList<Edge> e){
    	getEdges().removeAll(e);
    	for(Edge e1: e){
    		e1.getStartV().delEdge(e1);
    	}
    }
 
    public void addEdge(Edge e){
    	edges.add(e);
    	e.getStartV().addEdge(e);
    }
    
    public void addVertex(Vertex v){
    	vertices.add(v);
    }
    
    public void removeEdge(Edge e){
    	if (e == null)return;
    	if( e.getStartV() != null)
    		e.getStartV().delEdge(e);
    	else if(e.getDestV() != null)
    		e.getDestV().delEdge(e);
    	if(!edges.isEmpty()) edges.remove(e);
    }
    
    public void removeVertex(Vertex v){
    	vertices.remove(v);
    	v = null;
    }
    
    public Edge getEdge(Vertex st, Vertex end){
    	if(null != st.getEdges())
    	{for( Edge e: st.getEdges()){
    		if(e.getDestV().equals(end) || e.getStartV().equals(end))
    			return e;
    	}}
//    	System.out.println("No such edge found. ");
    	return null;
    }
    
    public void setEdgeWeights(ArrayList<Edge> edges){
    	for(Edge e: edges){
    		e.calWeight(this);
    	}
    }
    
    public double getEdgeWeight(Vertex st, Vertex end){
    	if( null != getEdge( st,  end)) return getEdge( st,  end).getWeight();
//    	System.out.println("No such edge exists between nodes "+ st + " and " + end);
     return 30000;
    	
    }
    
    /**
	 * Re-labels vertices with continuous numbers from 0 to number of vertices minus one. 
	 */
	public void reindexVertices(){
		int index = 0;
		for(Vertex v : vertices){
			v.setId(index--);
		}
		for(Vertex v: vertices){
			v.setId(v.getId()*-1);
//			ImgProcLog.write("Vertex: "+ v + " , Adjacency list: "+ v.getadjList() );
		}
	}
	/**
	 * Re-labels edges with continuous numbers from 0 to number of edges minus one. 
	 */
	public void reindexEdges(){
		int index = 0;
		for(Edge e:edges){
			e.setId(index--);
		}
		for(Edge e: edges){
			e.setId(e.getId()*-1);
//			ImgProcLog.write("Edge: "+ e + " , Nodes list: "+ e.getStartV()+ " , "+ e.getDestV());
		}
	}
	
	public int getNumPipeCellsOnSide(){
		return numPipeCells/2;
	}
	
	public ArrayList<Vertex> getPipeCells(){
		return pipeCells;
	}
	
	public double getGraphLength(){
		return GRAPHLENGTH;
	}
}
