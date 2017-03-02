package protocolModfr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom2.Element;

import graph.Graph;
import utils.ImgProcLog;
import utils.XMLParserFromiDynomics;

public class ProtocolModifier {

	private static final int REACTIONPRECISION = 100;
	private static final String NUTRIENT_SECRETION_REACTION = "NutrientSecretion";
	private static final String PRODUCT_UPTAKE_REACTION = "ProductUptake";
	private static final double EXTRA_HOURS = 2;
	private String protocolXML;
//	private Graph graph;
	private HashMap<Integer, Integer> edgeCellLengthMap;
	private HashMap<Integer, Double> secretionMap;

	
	public ProtocolModifier(String protocolXml, HashMap<Integer, Integer> edgeCellLengthMap, HashMap<Integer, Double> secretionMap) {
		this.protocolXML = protocolXml;
//		this.graph = graph;
		this.edgeCellLengthMap = edgeCellLengthMap;
		this.secretionMap = secretionMap;
	}

	/**
	 * Builds a new protocol file for a new set of simulations. It divides vascular cells into different groups based on
	 * their belonging to different edges in the graph
	 * @param path The path to the old protocol file
	 */
	public void modifyXML(String path) {
		XMLParserFromiDynomics protocolFileParser = new XMLParserFromiDynomics(path + "\\" +protocolXML);
		Element protocolRoot = protocolFileParser.get_localRoot();
		modifyInputTag(protocolRoot);
		ImgProcLog.write("Input tag modified");
		modifySimulatorTag(protocolRoot);
		ImgProcLog.write("simulator tag modified");
		modifyReactionsTag(protocolRoot);
		ImgProcLog.write("reactions modified");
		modifySpecies(protocolRoot);
		ImgProcLog.write("species modified");
		modifyAgentGrid(protocolRoot);
		ImgProcLog.write("Agent grid modified");
		ImgProcLog.write("Protocol modification path: "+ path + "\\" + protocolXML);
		try {
			protocolFileParser.replaceXMLFile(path + "\\" + protocolXML);
		} catch (IOException e) {
			ImgProcLog.write("Exception in creating a new protocol file");
			e.printStackTrace();
		}

	}

	private void modifyInputTag(Element protocolRoot) {
		Element agentGrid = protocolRoot.getChild("input");
		for (Element param : agentGrid.getChildren("param")) {
			if (param.getAttributeValue("name").equals("useAgentFile")) {
				param.setText("true");
			}
		}
	}

	private void modifyAgentGrid(Element protocolRoot) {
		Element agentGrid = protocolRoot.getChild("agentGrid");
		for (Element param : agentGrid.getChildren("param")) {
			if (param.getAttributeValue("name").equals("shovingMaxIter")) {
				param.setText("10");
			}
		}

	}

	/**
	 * Removes MovingCell elements replacing it with new moving cells with edgeID index
	 * @param protocolRoot The root element of this XML file
	 */
	private void modifySpecies(Element protocolRoot) {
		List<Element> species = protocolRoot.getChildren("species");
		Element movingCells = null;
		for (Element s : species) {
			if (s.getAttributeValue("name").equals("MovingCells")) {
				movingCells = s;
			}
			List<Element> reactions = s.getChildren("reaction");
			List<Element> removeList = new ArrayList<Element>();
			for (Element reaction : reactions) {
				if (reaction.getAttributeValue("name").equals("AttractSecretion")
						|| reaction.getAttributeValue("name").equals("GradientSecretion")) {
					removeList.add(reaction);
				}
			}
			for (Element reaction : removeList) {
				s.removeContent(reaction);
			}
		}
		movingCells.removeChild("chemotaxis");
		movingCells.removeChildren("reaction");
		for (Integer key : edgeCellLengthMap.keySet()) {
			Element newMovingCells = movingCells.clone();
			newMovingCells.setAttribute("name", "MovingCells" + key);
			if (edgeCellLengthMap.get(key) != null) {
				Element reaction = new Element("reaction");
				reaction.setAttribute("name", NUTRIENT_SECRETION_REACTION + key);
				reaction.setAttribute("status", "active");
				newMovingCells.addContent(reaction);

				Element reaction1 = new Element("reaction");
				reaction1.setAttribute("name", PRODUCT_UPTAKE_REACTION + key);
				reaction1.setAttribute("status", "active");
				newMovingCells.addContent(reaction1);
			}

			Element initArea = newMovingCells.getChild("initArea");
			initArea.setAttribute("number", String.valueOf(edgeCellLengthMap.get(key)));
			newMovingCells.getChild("tightJunctions").getChild("tightJunction").setAttribute("withSpecies",
					"MovingCells" + key);
			protocolRoot.addContent(newMovingCells);
		}
		protocolRoot.removeContent(movingCells);
	}

	
	private void modifyReactionsTag(Element protocolRoot) {
		List<Element> reactions = (List<Element>) protocolRoot.getChildren("reaction");
		Element nutrientSecretion = null;
		Element productUptake = null;
		for (Element e : reactions) {
			if (e.getAttributeValue("name").equals(NUTRIENT_SECRETION_REACTION)) {
				nutrientSecretion = e.clone();
				e.getChild("param").setText("1.1");
			}
			if (e.getAttributeValue("name").equals(PRODUCT_UPTAKE_REACTION)) {
				e.getChild("param").setText("1.5");
				productUptake = e.clone();
			}
		}
		for (Integer reactionId : secretionMap.keySet()) {
			Element reaction = nutrientSecretion.clone();
			reaction.setAttribute("name", NUTRIENT_SECRETION_REACTION + reactionId);
			reaction.getChild("param").setText(Double.toString((((secretionMap.get(reactionId)) / REACTIONPRECISION))));
			protocolRoot.addContent(reaction);
			Element newReaction = new Element("reaction");
			newReaction.setAttribute("name", NUTRIENT_SECRETION_REACTION + reactionId);
			protocolRoot.getChild("solver").addContent(newReaction);

			Element consumption = productUptake.clone();
			consumption.setAttribute("name", PRODUCT_UPTAKE_REACTION + reactionId);
			consumption.getChild("param")
					.setText(Double.toString((1.5 / 1.1) * (secretionMap.get(reactionId) / REACTIONPRECISION)));
			protocolRoot.addContent(consumption);
			Element newReaction1 = new Element("reaction");
			newReaction1.setAttribute("name", PRODUCT_UPTAKE_REACTION + reactionId);
			protocolRoot.getChild("solver").addContent(newReaction1);
		}
	}

	
	private void modifySimulatorTag(Element protocolRoot) {
		Element simulator = protocolRoot.getChild("simulator");
		for (Element e : (List<Element>) simulator.getChildren("param")) {
			if (e.getAttribute("name") != null && e.getAttributeValue("name").equals("restartPreviousRun")) {
				e.setText("true");
			}
			if (e.getAttribute("name") != null && e.getAttributeValue("name").equals("outputPeriod")) {
				e.setText("0.05");
			}
		}

		Element timestep = simulator.getChild("timeStep");
		for (Element e : (List<Element>) timestep.getChildren()) {
			if (e.getAttribute("name") != null && e.getAttributeValue("name").equals("endOfSimulation")) {
				e.setText(Double.toString(Double.parseDouble(e.getText() + ".0") + EXTRA_HOURS));
			}
		}
	}
		
}
