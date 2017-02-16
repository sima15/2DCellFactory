package protocolModfr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import utils.XMLParserFromiDynomics;

public class SimpleProtocolModifier {

	private static final String NUTRIENT_SECRETION_REACTION = "NutrientSecretion";
	private static final String PRODUCT_UPTAKE_REACTION = "ProductUptake";
	private static final int SECOND_PHASE_EXTRA_HOURS = 2;
	private String protocolXML;

	public SimpleProtocolModifier(String protocolXml) {
		this.protocolXML = protocolXml;
	}

	public void modifyXML(String path) throws IOException {
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
				if(reaction.getAttributeValue("name").equals("AttractSecretion")||reaction.getAttributeValue("name").equals("GradientSecretion")){
					removeList.add(reaction);
				}
			}
			for(Element reaction:removeList){
				s.removeContent(reaction);
			}
		}
		movingCells.removeChild("chemotaxis");
		movingCells.removeChildren("reaction");
	}

	private void modifyReactionsTag(Element protocolRoot) {
		List<org.jdom2.Element> reactions = (List<Element>) protocolRoot.getChildren("reaction");
		for (Element e : reactions) {
			if (e.getAttributeValue("name").equals(NUTRIENT_SECRETION_REACTION)) {
				e.getChild("param").setText(Double.toString(1.1));
			}
			if (e.getAttributeValue("name").equals(PRODUCT_UPTAKE_REACTION)) {
				e.getChild("param").setText(Double.toString(1.5));
			}
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
				e.setText(Integer.toString(Integer.parseInt(e.getText())+SECOND_PHASE_EXTRA_HOURS));
			}
		}
	}
}
