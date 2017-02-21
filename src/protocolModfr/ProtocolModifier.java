package protocolModfr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import utils.XMLParserFromiDynomics;

public class ProtocolModifier {

	private static final String NUTRIENT_SECRETION_REACTION = "NutrientSecretion";
	private static final String PRODUCT_UPTAKE_REACTION = "ProductUptake";
	private static final String SECOND_PHASE_HOURS = "24";
	int[][] edgeIdMatrix;
	private Map<String, String> edgeIDMap;
	private String protocolXML;
	private Map<String, Double> secretionMap;
	private static String agentFilePath = "\\lastIter\\agent_State(last).xml";

	public ProtocolModifier(int[][] edgeIdMatrix, Map<String, Double> secretionMap, String protocolXml) {
		this.edgeIdMatrix = edgeIdMatrix;
		this.secretionMap = secretionMap;
		this.protocolXML = protocolXml;
	}

	public void modifyXML(String path) throws IOException {
		System.out.println(path + agentFilePath);
		XMLParserFromiDynomics agentFileParser = new XMLParserFromiDynomics(path + agentFilePath);
		extractAgentDetails(agentFileParser);
		agentFileParser.replaceXMLFile(path + agentFilePath);

		XMLParserFromiDynomics protocolFileParser = new XMLParserFromiDynomics(path + protocolXML);
		Element protocolRoot = protocolFileParser.get_localRoot();
		modifyInputTag(protocolRoot);
		System.out.println("Input tag modified");
		modifySimulatorTag(protocolRoot);
		System.out.println("simulator tag modified");
		modifyReactionsTag(protocolRoot);
		System.out.println("reactions modified");
		modifySpecies(protocolRoot);
		System.out.println("species modified");
		modifyAgentGrid(protocolRoot);
		System.out.println("agentgrid modified modified");
		System.out.println(path + "\\" + protocolXML);
		protocolFileParser.replaceXMLFile(path + "\\" + protocolXML);
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

	private void extractAgentDetails(XMLParserFromiDynomics agentFileParser) {
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
		System.out.println(agentArray.length);
		edgeIDMap = new LinkedHashMap<String, String>();
		for (int i = 0; i < agentArray.length; i++) {
			String[] elements = agentArray[i].split(",");
			int x = (int) Math.round(512 - Double.parseDouble(elements[10]));
			int y = (int) Math.round(1024 - Double.parseDouble(elements[11]));
			int edgeId = edgeIdMatrix[y][x];
			if (edgeIDMap.containsKey(Integer.toString(edgeId))) {
				String newText = edgeIDMap.get(Integer.toString(edgeId));
				newText += agentArray[i] + ";\n";
				edgeIDMap.put(Integer.toString(edgeId), newText);
			} else {
				edgeIDMap.put(Integer.toString(edgeId), "\n" + agentArray[i] + ";\n");
			}
		}

		for (String key : edgeIDMap.keySet()) {
			Element newMovingCells = movingCells.clone();
			newMovingCells.setAttribute("name", "MovingCells" + key);
			newMovingCells.setText(edgeIDMap.get(key));
			agentRoot.getChild("simulation").addContent(newMovingCells);
		}
		agentRoot.getChild("simulation").removeContent(movingCells);
	}

	private void modifySpecies(Element protocolRoot) {
		List<Element> species = protocolRoot.getChildren("species");
		Element movingCells = null;
		for (Element s : species) {
			if (s.getAttributeValue("name").equals("MovingCells")) {
				movingCells = s;
			}
			List<Element> reactions = s.getChildren("reaction");
			List<Element> removeList = new ArrayList<Element>();
			for(Element reaction:reactions){
				if(reaction.getAttributeValue("name").equals("AttractSecretion")||
						reaction.getAttributeValue("name").equals("GradientSecretion")){
					removeList.add(reaction);
				}
			}
			for(Element reaction:removeList){
				s.removeContent(reaction);
			}
			/*if (!s.getAttributeValue("name").contains("Pipe"))
				s.removeChildren("reaction");
			*//*if (s.getAttributeValue("name").equals("Consumer")) {
				Element reaction = new Element("reaction");
				reaction.setAttribute("name", "ProductSecretion");
				reaction.setAttribute("status", "active");
				s.addContent(reaction);
			}*/
		}
		movingCells.removeChild("chemotaxis");
		movingCells.removeChildren("reaction");
		for (String key : edgeIDMap.keySet()) {
			Element newMovingCells = movingCells.clone();
			newMovingCells.setAttribute("name", "MovingCells" + key);
			if (secretionMap.get(key)!=null) {
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
			initArea.setAttribute("number", Integer.toString(edgeIDMap.get(key).split(";\n").length));
			newMovingCells.getChild("tightJunctions").getChild("tightJunction").setAttribute("withSpecies",
					"MovingCells" + key);
			protocolRoot.addContent(newMovingCells);
		}
		protocolRoot.removeContent(movingCells);
	}

	private void modifyReactionsTag(Element protocolRoot) {
		List<org.jdom2.Element> reactions = (List<Element>) protocolRoot.getChildren("reaction");
		Element nutrientSecretion = null;
		Element productUptake = null;
		for (Element e : reactions) {
			if (e.getAttributeValue("name").equals(NUTRIENT_SECRETION_REACTION)) {
				nutrientSecretion = e.clone();
			}
			if (e.getAttributeValue("name").equals(PRODUCT_UPTAKE_REACTION)) {
				productUptake = e.clone();
			}
		}
		for (String edgeId:secretionMap.keySet()) {
			Element reaction = nutrientSecretion.clone();
			reaction.setAttribute("name", NUTRIENT_SECRETION_REACTION + edgeId);
			reaction.getChild("param").setText(Double.toString(Math.abs(secretionMap.get(edgeId))));
			protocolRoot.addContent(reaction);
			Element newReaction = new Element("reaction");
			newReaction.setAttribute("name", NUTRIENT_SECRETION_REACTION + edgeId);
			protocolRoot.getChild("solver").addContent(newReaction);

			Element consumption = productUptake.clone();
			consumption.setAttribute("name", PRODUCT_UPTAKE_REACTION + edgeId);
			consumption.getChild("param").setText(Double.toString((1.5/1.1)*secretionMap.get(edgeId)));
			protocolRoot.addContent(consumption);
			Element newReaction1 = new Element("reaction");
			newReaction1.setAttribute("name", PRODUCT_UPTAKE_REACTION + edgeId);
			protocolRoot.getChild("solver").addContent(newReaction1);
		}
	}

	private void modifySimulatorTag(Element protocolRoot) {
		Element simulator = protocolRoot.getChild("simulator");
		for (Element e : (List<Element>) simulator.getChildren("param")) {
			if (e.getAttribute("name") != null && e.getAttributeValue("name").equals("restartPreviousRun")) {
				e.setText("true");
			}
		}

		Element timestep = simulator.getChild("timeStep");
		for (Element e : (List<Element>) timestep.getChildren()) {
			if (e.getAttribute("name") != null && e.getAttributeValue("name").equals("endOfSimulation")) {
				e.setText(Integer.toString(Integer.parseInt(e.getText())+2));
			}
		}
	}

	public Map<String, String> getAgentMap() {
		return edgeIDMap;
	}

	public void setAgentMap(Map<String, String> agentMap) {
		this.edgeIDMap = agentMap;
	}
}
