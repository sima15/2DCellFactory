package data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import graph.Edge;
import graph.Graph;
import graph.Vertex;

public class WriteToFile {
	/**
	 * A graph object to be visualized
	 */
	Graph graph;
	PrintWriter pr;
	PrintWriter pw;
	String logPath;
	final float SCALERATIO = 5.0f;
	final double RADIUS = 0.07;
	DecimalFormat df = new DecimalFormat("#.##");
	
	
	/**
	 * Passes a graph to be visualized to the class
	 * @param g The graph to be visualized
	 */
	public WriteToFile(Graph g, String fileName){
		graph = g;
		write(fileName);
	}
	
	/**
	 * Default constructor of the class
	 */
	public WriteToFile(){
	}
	
	
	/**
	 * Writes graph specifications to a VRML file
	 */
	public void write(String fileName) {
		
		try {
			pr = new PrintWriter (new FileWriter(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		pr.println("#VRML V2.0 utf8");
		pr.println("Background{ skyColor [0.704 0.896 0.92]}");
		pr.println("DEF vertex Shape{ appearance Appearance{");
		pr.println("material Material {diffuseColor 1 0 0 }} geometry Sphere { radius 0.07 }}");
		
		// Prints vertex nodes to the VRML file
		for(Vertex v: graph.getVertices()){
			pr.println("Transform {");
			pr.println("	translation "+df.format(v.getcoord()[0]/SCALERATIO) + 
					" "+ df.format(v.getcoord()[1]/SCALERATIO)+ " "+ df.format(v.getcoord()[2]/SCALERATIO));
			pr.println("		children");
			pr.println("			USE vertex");
			pr.println("}");	
		}
		pr.println("DEF TransA Transform{ children DEF edge Shape{ appearance");
		pr.println("	DEF edgeAppear Appearance {material Material {diffuseColor 0 0 0}}");
		pr.println("		geometry Cylinder {radius 0.01 height 1.0 }}}");
		
		// Prints vertex nodes to the VRML file
		for(Edge e: graph.getEdges()){
			pr.println("DEF Trans"+ e.getId() +" Transform { children USE edge }");
		}
		
		// Writes vrml scripts to connect edges to vertices
		for(Edge e: graph.getEdges()){
				double[] v1 = e.getStartV().getcoord();
				double[] v2 = e.getDestV().getcoord();
				pr.println("\nDEF Scr Script {");
				pr.println("	field SFVec3f A "+ v1[0]/SCALERATIO+" "+ v1[1]/SCALERATIO+
						" "+ v1[2]/SCALERATIO);
				pr.println("	field SFVec3f B "+ v2[0]/SCALERATIO+" "+ v2[1]/SCALERATIO+
						" "+ v2[2]/SCALERATIO);
				pr.println("	field SFNode Trans USE Trans"+ e.getId());
				pr.println("	url \"vrmlscript: function initialize(){Link(Trans, A, B);}");
				pr.println("	function Link(Tr, PosA, PosB){");
				pr.println("		Tr.scale=       new SFVec3f(1, PosB.subtract(PosA).length(), 1);");
				pr.println("		Tr.translation= PosA.add(PosB).multiply(.5);");
				pr.println("		Tr.rotation=    new SFRotation(new SFVec3f(0, 1, 0), PosB.subtract(PosA));}\"}");
				
		}
		pr.close();
	}

	public void log(){
		logPath = "data\\nodeEdgeLog.log";
		try {
			pw = new PrintWriter (new FileWriter(logPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLog(String mesg){
		pw.println(mesg);
	}
	
	public void finishLog(){
		pw.close();
	}
}
