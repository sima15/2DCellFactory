package utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLReader {

	public static void updateParameter(Map map) throws TransformerException {

		try {
			String filepath = "D:\\iDynomics\\protocols\\Contact.xml";

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(new File(filepath));

			NodeList company = doc.getElementsByTagName("param");
			for (int i = 0; i < company.getLength(); i++) {

				Node node = company.item(i);
				Element eElement = (Element) node;
				if (eElement.getAttribute("name").equals("muMax")) {

					String muMaxstr = Double.toString((double) map.get("muMax"));

					eElement.setTextContent(muMaxstr);

					DOMSource source = new DOMSource(doc);
					transformer.transform(source, result);
				}

			}

			// chemotaticStrength

			NodeList company1 = doc.getElementsByTagName("chemotactic");
			String cstrength = Double.toString((double) map.get("chemotaticstrength"));
			for (int i = 0; i < company1.getLength(); i++) {

				Node node = company1.item(i);
				Element eElement = (Element) node;

				eElement.setAttribute("strength", cstrength);

				DOMSource source = new DOMSource(doc);
				transformer.transform(source, result);
				// }

			}

			NodeList company2 = doc.getElementsByTagName("tightJunction");
			String cstrength2 = Double.toString((double) map.get("tightJunctionstiffness"));
			for (int i = 0; i < company2.getLength(); i++) {
				Node node = company2.item(i);
				Element eElement = (Element) node;

				eElement.setAttribute("stiffness", cstrength2);
				// System.out.println("..");
				DOMSource source = new DOMSource(doc);
				transformer.transform(source, result);
				// }

			}

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}
}
