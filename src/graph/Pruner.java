package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

import data.WriteToFile;
import graph.Pruner.Comp;
import jdk.internal.org.objectweb.asm.commons.RemappingMethodAdapter;
import simulation.Controller;
import utils.ImgProcLog;

public class Pruner {
	/**
	 * The minimum path length required for pruning edges 
	 */
	final int MINPATHLENGTH = 50;
	Comp comparator = new Comp();
	PriorityQueue<Vertex> heap = new PriorityQueue<>(3000, comparator);
	
	/**
	 * Constructs a pruner based on the given graph
	 * and its edges
	 * @param graph A graph which needs pruning
	 * @param e The edges of the graph
	 */
//	public Pruner(Graph g, ArrayList<Edge> e){
//		graph = g;
//		edges = e;
//		lastEdgeID = edges.get(e.size()-1).getID();
//	}
	
	/**
	 * Constructs a pruner based on the given graph
	 * @param graph A graph which needs pruning
	 */
//	public Pruner(Graph graph){
////		startPruning(graph);
//	}
	
	/**
	 * Applies several levels of pruning to the current graph object
	 * @param A graph which needs to be pruned and simplified
	 * @return The final pruned graph
	 */
	public Graph startPruning(Graph graph){
		System.out.println("Before pruning:");
		ImgProcLog.write("Before pruning:");
        System.out.println("Number of edges = "+ graph.getEdges().size()
        + " vertices: "+ graph.getVertices().size());
        ImgProcLog.write("Number of edges = "+ graph.getEdges().size()
        + " vertices: "+ graph.getVertices().size());
        double startTime = System.currentTimeMillis();
//        System.out.println("start time: "+ startTime/1000);
        
        Graph oneLevelPruned = pruneLevel1(graph);
//		System.out.println("After pruning:");
		ImgProcLog.write("After pruning:");
//		System.out.println("Number of edges in pruned level 1 = edges: "+ oneLevelPruned.getEdges().size()
//				+ " vertices: "+ oneLevelPruned.getVertices().size());
		ImgProcLog.write("Number of edges in pruned level 1 = edges: "+ oneLevelPruned.getEdges().size()
				+ " vertices: "+ oneLevelPruned.getVertices().size());
		new WriteToFile( oneLevelPruned, "Output\\" +Controller.getName() + "_Pruned1.wrl");
		Graph twoLevelPruned = pruneLevel2(oneLevelPruned);
//		System.out.println("Number of edges in pruned level 2 = edges: "+ twoLevelPruned.getEdges().size()
//        		+ " vertices: "+ twoLevelPruned.getVertices().size());
		ImgProcLog.write("Number of edges in pruned level 2 = edges: "+ twoLevelPruned.getEdges().size()
        		+ " vertices: "+ twoLevelPruned.getVertices().size());
		new WriteToFile( twoLevelPruned, "Output\\" +Controller.getName() + "_Pruned2.wrl");
		Graph threeLevelPruned = pruneLevel3(twoLevelPruned);
//        System.out.println("Number of edges in pruned level 3 = edges: "+ threeLevelPruned.getEdges().size()
//        		+ " vertices: "+ threeLevelPruned.getVertices().size());
        ImgProcLog.write("Number of edges in pruned level 3 = edges: "+ threeLevelPruned.getEdges().size()
        		+ " vertices: "+ threeLevelPruned.getVertices().size());
        new WriteToFile( threeLevelPruned, "Output\\" +Controller.getName() + "_Pruned3.wrl");  
        Graph fourLevelPruned = prunePipeCells(threeLevelPruned);
//        System.out.println("Number of edges in pruned level 4 = edges: "+ fourLevelPruned.getEdges().size()
//        		+ " vertices: "+ fourLevelPruned.getVertices().size());
        ImgProcLog.write("Number of edges in pruned level 4 = edges: "+ fourLevelPruned.getEdges().size()
        		+ " vertices: "+ fourLevelPruned.getVertices().size());
        new WriteToFile( fourLevelPruned, "Output\\" +Controller.getName() + "_Pruned4.wrl");  
        
        System.out.println("Pruning duration = "+ (System.currentTimeMillis()-startTime)/1000);
        ImgProcLog.write("Pruning duration = "+ (System.currentTimeMillis()-startTime)/1000);
		return fourLevelPruned;
	}
	
// *********************************************	
	/**
	 * Prunes the graph one level. It removes extra edges while maintaining connectivity in the graph.
	 * @param The graph to be pruned
	 * @return A pruned graph
	 */
	public Graph pruneLevel1(Graph graph){
		System.out.println("Started level 1 pruning");
		ArrayList<Edge> origEdges = graph.getEdges();
		ArrayList<Edge> edges = new ArrayList<Edge>(origEdges);
		ArrayList<Vertex> origVer = graph.getVertices();
		ArrayList<Vertex> ver = new ArrayList<Vertex>(origVer);
		//Copies of the arrayLists have to be made since altering the original 
		//list will cause concurrent modification exception
		Collections.copy(edges, origEdges );
		Collections.copy(ver, origVer );
		Graph pruned1 = new Graph( ver, edges);
		Iterator<Edge> it = origEdges.iterator();
		//Tests if each edge can be removed while maintaining connectivity
		while(it.hasNext()){
			Edge e = it.next();
			double weight = e.getWeight();
			e.setWeight(10000);
			if(isMinPath(e, pruned1)) {
				pruned1.removeEdge(e);
				it.remove();
			}
			else e.setWeight(weight);
		}
		return pruned1;
	}
	
	
	/**
	 * Determines if there exists a path between two ends of the given edge 
	 * which is short enough
	 * @param A candidate edge to be removed
	 * @param A graph which contains the given edge
	 * @return True if there is a short enough path after deletion of this given edge
	 */
	public boolean isMinPath(Edge e, Graph graph){
		Vertex source  = e.getStartV();
		Vertex destination = e.getDestV();
		if (null == source || null == destination  ) {
			System.out.println("Src/dest null");
			return false;
		}
		Vertex curr;
		/**
		 * A set including vertices with the shortest path to the source
		 */
		Set<Vertex> settledV = new HashSet<Vertex>();
		/**
		 * A hashmap including vertices visited so far and their distance from the source
		 */
		HashMap<Vertex, Double> srcDists = new HashMap<Vertex, Double>();
		settledV.add(source);
		curr = source;
		srcDists.put(source, 0.0);
		int loopLength = graph.getVertices().size(); 
		for(int i=1;  i<= loopLength; i++){
			//Adding adjacent vertices of the current vertex to visited vertices map
			for(Vertex v: curr.getadjList()){
				srcDists.put(v, srcDists.get(curr)+ graph.getEdgeWeight(v, curr) );
			}
			double min = 10000;
//			for(Vertex v: settledV){
//				 if (!v.equals(source) && srcDists.get(v) == 0) System.out.println("Distance for "+ srcDists.get(v) 
//				 		+ " = 0" );
//			}
			//Finding a visited vertex with the shortest path from the source
			// and putting it into settled vertices (settledV) set
			for( Vertex v: srcDists.keySet()){
				if(!settledV.contains(v))
					if(srcDists.get(v)<= min){
						min = srcDists.get(v);
						curr = v;
					}
			}
			settledV.add(curr);
			//Updating path lengths in the visited vertices to shorter ones
			for( Vertex v: srcDists.keySet()){
				if(!settledV.contains(v))
					if(graph.getEdge(v, curr) != null)
						if( srcDists.get(v) > srcDists.get(curr)+ graph.getEdgeWeight(v, curr))
							srcDists.put(v, srcDists.get(curr)+ graph.getEdgeWeight(v, curr));
			}
			//Checks if the destination vertex is among the visited nodes
			if(srcDists.containsKey(destination)){
				//If the distance to destination is higher the 500 continue the program
				if(srcDists.get(destination) < 500.0){
					if (srcDists.get(destination) < MINPATHLENGTH) return true;
					else return false;}
			}
		}
//		System.out.println("Destination vertex: "+ destination + " never found! :( ");
		return false;
	}
	
// **************************************************************************************************
	
	/**
	 * Prunes the graph to level 2 
	 * It eliminates vertices of degree 1 and the edge connected to them
	 * @return 2 levels pruned graph 
	 */
	public Graph pruneLevel2(Graph graph){
		System.out.println("Started level 2 pruning");
		ArrayList<Vertex> ver = new ArrayList<Vertex>(graph.getVertices());
		ArrayList<Edge> edg = new ArrayList<Edge>( graph.getEdges());
		Graph pGraph = new Graph(ver, edg);
		heap.addAll(pGraph.getVertices());
		Vertex root;
		WriteToFile writer = new WriteToFile();
		writer.log();
		while(!heap.isEmpty()){
			root = heap.poll();
			
			if(root.getDegree() == 0){
				writer.writeLog("Degree 0 started");
				writer.writeLog("Heap root vertex: "+ root.toString());
				writer.writeLog("		edges: "+ root.getEdges());
				writer.writeLog("		adj nodes: "+ root.getadjList());
				pGraph.removeVertex(root);
			}
			else if(root.getDegree() == 1){
				writer.writeLog("Degree 1 started");
				writer.writeLog("Heap root vertex: "+ root.toString());
				writer.writeLog("		edges: "+ root.getEdges());
				writer.writeLog("		adj nodes: "+ root.getadjList());
				Edge tempE ;
				if(null != (tempE =  root.getEdges().get(0))) {
					pGraph.removeEdge(tempE);
				}//else System.out.println("No edges exist for this vertex");
				Vertex tempV = root.getOpposite(root, tempE);
				//Remove and later add this node's adjacent node to update the heap with its new degree
				heap.remove(tempV);
				pGraph.removeVertex(root);
				heap.add(tempV);
			}
			else{
				writer.writeLog("---------------------------------------");
				writer.writeLog("Heap root vertex: "+ root.toString());
				writer.writeLog("		edges: "+ root.getEdges());
				writer.writeLog("		adj nodes: "+ root.getadjList());
				heap.add(root);
				//Quit if the heap root has a degree of 2 or bigger
				if(heap.peek().getDegree() >= 2) break;
			};
		}
		return pGraph;
	}
	
	/**
	 * Connects close-enough vertices eliminating the common adjacent vertex in between
	 * @param The graph to be further pruned
	 * @return three levels pruned graph
	 */
	public Graph pruneLevel3(Graph g){
		System.out.println("Started level 3 pruning");
		ArrayList<Vertex> ver = new ArrayList<Vertex>(g.getVertices());
		PriorityQueue<Vertex> heap2 = new PriorityQueue<Vertex>( comparator);
		heap2.addAll(ver);
		//An arbitrarily big number for numbering edges
		int id = 9500;
		while(!heap2.isEmpty()){
			Vertex v = heap2.poll();
			if(v.getDegree() == 2){
				Vertex v1 = v.getadjList().get(0);
				Vertex v2 = v.getadjList().get(1);
				double dist = g.calDistance( v1, v2 );
				if (dist>=5 && dist<50) {
					g.removeEdge(g.getEdge(v, v1));;
					g.removeEdge(g.getEdge(v, v2));;
					g.removeVertex(v);
					v.setEdges(null);
					Edge newE = new Edge(id++, v1, v2);
					g.getEdges().add(newE);
				}
			}
			else break;
		}
		return g;
	}
	
	/**
	 * Prunes the pipe cells which are connected to each other but not to vascular cells.
	 * @param g The graph to be pruned
	 * @return A graph without unnecessary vertices for pipe cells.
	 */
	public Graph prunePipeCells(Graph g){
		ArrayList<Vertex> pToNonpVert = new ArrayList<Vertex>(); 
		ArrayList<Vertex> removableV = new ArrayList<Vertex>();
		ArrayList<Edge> removableE = new ArrayList<Edge>();
		ArrayList<Edge> edges;
		//Marking nodes also connected to vascular nodes and adding them to pToNonpVert arraylist
		for(Vertex v1: g.getVertices()){
			if(v1.isPipeCell()){
				for(Vertex v2 : v1.getadjList()){
					if(!v2.isPipeCell())
						if(!pToNonpVert.contains(v1)) pToNonpVert.add(v1);
				}	
			}
		}
		//Removing extra edges which connect only nodes representing pipe cells
		for(Vertex v1: g.getVertices()){
			if(v1.isPipeCell()){
				if(pToNonpVert.contains(v1)){
					for(Edge e: v1.getEdges()){
						Vertex tempV = v1.getOpposite(v1, e);
						if(!pToNonpVert.contains(tempV) && tempV.isPipeCell()){
							if(removableV.contains(tempV)) removableV.add(tempV);
							if (!removableE.contains(e)) removableE.add(e);
						}
					}
				}else{
					for(Edge e: v1.getEdges()){
						if(!removableE.contains(e)) removableE.add(e);
					}
					if(!removableV.contains(v1)) removableV.add(v1);
				}
			}
		}
		//Removing extra nodes representing pipe cells
		for(int i= removableE.size()-1; i>=0; i--){
			g.removeEdge(removableE.get(i));
		}
		g.getVertices().removeAll(removableV);
		int numPCellsOnSide = g.getNumPipeCellsOnSide();
		int numPipeCells = g.getNumPipeCellsOnSide()*2;
		edges = g.getEdges();
//		ImgProcLog.write("Pipe cells: ");
//		for(Vertex v: g.getVertices()){
//			if(v.isPipeCell())ImgProcLog.write(v.toString()); 
//		}
		
		return g;
	}
	
	
	/**
	 * Prints a heap
	 */
	public void printHeap(){
		System.out.println("After pruning: "+heap.toString());
		System.out.println("No. of vertices: "+ heap.size());
	}
	
	/**
	 * A class created to provide a comparator object for the heap
	 * @author Sima
	 *
	 */
	static class Comp implements Comparator<Vertex>{

		@Override
		public int compare(Vertex v1, Vertex v2) {
			
			return v1.getDegree()-v2.getDegree();
		}
		
	}
	
	public double calAvg(double... value){
		double sum = 0;
		int n = value.length;
		for(double d: value){
			sum += d;
		}
		return sum/n;
	}
	
	// ****************************************************	
	/**
	 * An incomplete implementation of graph pruning based on spatial closeness
	 * @param graph The graph to be pruned
	 * @param extra An unneeded parameter to distinguish this method from the one above
	 * @return pruned graph
	 */
		public Graph pruneLevel1(Graph graph, int extra){
			Graph pGraph = new Graph();
			Iterator<Vertex> verIter = graph.getVertices().iterator();
			ArrayList<Vertex> adjList = new ArrayList<Vertex>(); 
			Vertex[] adjArray; // = new Vertex[20];
			ArrayList<Vertex> delVert = new ArrayList<Vertex>();
			Vertex curr;
			int verId =0;
			int edgeId =0;
			int length =0;
			int counter = 0;
			while(verIter.hasNext()){
				length =0;
//				if(counter++>10) break;
				curr = verIter.next();
				adjArray = new Vertex[160];
				if(curr.getadjList().isEmpty()) {
					pGraph.addVertex(curr);
					continue;
				}
				if(delVert.contains(curr)) {
					pGraph.addVertex(curr);
					continue;
				}
				for(Vertex v: curr.getadjList()){
					if(graph.calDistance(curr, v)<5){
						for(Vertex v2: v.getadjList()){
//							adjList.add(v2);
							adjArray[length] = v2;
							length++;
						}
					}else{
						pGraph.addVertex(v);
					}
				}
				length--;
				double sumX =0;
				double sumY =0;
				double sumZ =0;
				
				for(Vertex v: curr.getadjList()){
					sumX += v.getcoord()[0];
					sumY += v.getcoord()[1];
					sumZ += v.getcoord()[2];
				}
				if(length==0)
					continue;// length++;
				double midX =sumX/length;
				double midY =sumY/length;
				double midZ =sumZ/length;
				Vertex middleVert = new Vertex(verId++, midX, midY, midZ);
				pGraph.addVertex(middleVert);
//				verIter.remove();
				for(Vertex v:adjArray ){
					if(v == null) break;
					if(curr.getadjList().contains(v)){
						v.getEdges().clear();
						pGraph.delEdges(v.getEdges());
//						adjList.remove(v);
					}else{
						Edge tempEdge = new Edge(edgeId++, v, middleVert);
						pGraph.addEdge(tempEdge);
						middleVert.addEdge(tempEdge);
						middleVert.setDegree(middleVert.getDegree()-1);
					}
				}
				delVert.add(curr);
				delVert.addAll(curr.getadjList());
			}
			return pGraph;
		}
}
