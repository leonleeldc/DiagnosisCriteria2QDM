package com.nj.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.mayo.bmi.medtagger.util.SimpleTools;

/**
 * 
 * @author Rachel Xiu
 * The class has the function to read xml file (HN's), then produce new xml based on the type of NOUN. 
 * will be called in main() by converter.
 *
 */

public class XmlFromCem2Qdm {
	
	public static List<Element> xmlIO(String xmlText,Document doc){
		List<Element> eleList = new ArrayList<Element>();
		xmlText = xmlText.replaceAll("&", "l"); //I have been tortured by these special characters. Now, I decide to use the simplest approach. Just replacing & as l.
		StringReader strReader = new StringReader(xmlText);
		int textStart = xmlText.indexOf("<TEXT><![CDATA[")+"<TEXT><![CDATA[".length()+1;
		String utf8XmlText = null;
		utf8XmlText = new String(xmlText.getBytes(),StandardCharsets.UTF_8);
		//utf8XmlText  = htmlspecialchars_decode_ENT_NOQUOTES(xmlText);
		InputSource inputSource = new InputSource(strReader);
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			document = dBuilder.parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
			if(document==null){
				document = dBuilder.parse(inputSource);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		NodeList textNodes = document.getElementsByTagName("TEXT");
		String text = textNodes.item(0).getTextContent();


		NodeList docNodes = document.getElementsByTagName("NOUN");
		System.out.println("the length of docNodes: "+docNodes.getLength());
		NodeList kidNodes = document.getChildNodes(); //the kidNodes should include only one node, namely, collection
		System.out.println("the length of kidNodes: "+kidNodes.getLength());
		for(int i=0;i<docNodes.getLength();i++){
			Node ithChild = docNodes.item(i);
			NamedNodeMap nnm = ithChild.getAttributes();// children be treated as nodes and their attributes be put in a map

			Node idNode = nnm.getNamedItem("id");
			String idNodeVal = idNode.getNodeValue();
			Node textNode = nnm.getNamedItem("text");
			String textNodeVal = textNode.getNodeValue();
			Node startNode = nnm.getNamedItem("start");
			int start = Integer.valueOf(startNode.getNodeValue())+textStart;
			Node endNode = nnm.getNamedItem("end");
			int end = Integer.valueOf(endNode.getNodeValue())+textStart;
			Node typeNode = nnm.getNamedItem("type");
			String typeNodeVal = typeNode.getNodeValue();
			SimpleTools.genEle(eleList, typeNodeVal, document, textNodeVal);
		}
		return eleList;
	}


}
