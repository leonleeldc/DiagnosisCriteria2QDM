package edu.mayo.bmi.medtagger.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class XMLreader {

	public static Document getDocument(String path) {
		Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(path);
			document.getDocumentElement().normalize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}
	
	public static Document getDocument_2(String path) {
		StringReader sr = null;
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader(new File(path)));
			String input = "";
			String line = "";
			while((line=bf.readLine())!=null){
				input+=line;
			}
			if(input.contains("&")){
				input = input.replace("&", "A");
			}
			sr = new StringReader(input);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		InputSource inputSource = new InputSource(sr);
		Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(inputSource);
			document.getDocumentElement().normalize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}

	public static String getOneNode(Element el, String name) {
		String text = "";
		try {
			NodeList nodeCon = el.getElementsByTagName(name);
			if (nodeCon.getLength() > 0) {
				Element elmCon = (Element) nodeCon.item(0);
				NodeList childElemNameCon = elmCon.getChildNodes();
				if (childElemNameCon.getLength() > 0) {
					text = (childElemNameCon.item(0)).getNodeValue();
					if (!text.isEmpty())
						text = text.trim();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (text.length() > 0)
			text = text.trim();
		return text;
	}

	public static List<String> getLstOfOneNode(Element el, String name) {
		List<String> lstNodes = new ArrayList<String>();
		String text;
		try {
			NodeList nodeCon = el.getElementsByTagName(name);
			for (int i = 0; i < nodeCon.getLength(); i++) {
				Element elmCon = (Element) nodeCon.item(i);
				NodeList childElemNameCon = elmCon.getChildNodes();
				if (childElemNameCon.getLength() > 0) {
					text = (childElemNameCon.item(0)).getNodeValue();
					if (!text.isEmpty()) {
						text = text.trim();
						lstNodes.add(text);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstNodes;
	}

	public static List<String> getLstOfOneNode(Element el, String grandParent, String parent, String name) {
		List<String> lstNodes = new ArrayList<String>();
		try {
			NodeList lstNodeInter = el.getElementsByTagName(parent);
			for (int k = 0; k < lstNodeInter.getLength(); k++) {
				Node ndInter = lstNodeInter.item(k);
				if (ndInter.getParentNode().getNodeName().equalsIgnoreCase(grandParent)) {
					if (ndInter.getNodeType() == Node.ELEMENT_NODE) {
						Element elem = (Element) ndInter;
						lstNodes.addAll(XMLreader.getLstOfOneNode(elem, name));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstNodes;
	}

	public static String getOneNode(Element el, String parent, String name) {
		String text = "";
		try {
			NodeList lstBody = el.getElementsByTagName(parent);
			if (lstBody.getLength() > 0) {
				Node ndBody = lstBody.item(0);
				if (ndBody.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element) ndBody;
					text = getOneNode(elem, name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}

	public static void main(String[] args) {
		try {
			String path = "N:\\Projects\\Repurposing\\data\\DrugBank\\drugbank.xml";
			Document document = XMLreader.getDocument(path);
			NodeList lstNode = document.getElementsByTagName("drug");
			for (int i = 0; i < lstNode.getLength(); i++) {
				Node ndDrug = lstNode.item(i);
				if (ndDrug.getNodeType() == Node.ELEMENT_NODE) {
					Element elemDrug = (Element) ndDrug;
					String name = XMLreader.getOneNode(elemDrug, "name");
					// NodeList lstNdIssn =
					// elemJournal.getElementsByTagName("groups");
					// if (lstNdIssn.getLength() > 0) {
					// Element elmIssn = (Element) lstNdIssn.item(0);
					// if
					// (elmIssn.getAttribute("IssnType").equalsIgnoreCase("print"))
					// issn_print = true;
					// NodeList ndIssn = elmIssn.getChildNodes();
					// String issn = ((Node)
					// (ndIssn.item(0))).getNodeValue().trim();
					// }
					NodeList lstNodeTarget = elemDrug.getElementsByTagName("targets");
					if (lstNodeTarget.getLength() > 0) {
						Node ndJournalIssue = lstNodeTarget.item(0);
						if (ndJournalIssue.getNodeType() == Node.ELEMENT_NODE) {
							Element elemJournalIssue = (Element) ndJournalIssue;
							String volume = XMLreader.getOneNode(elemJournalIssue, "target");
							System.out.println(volume);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> getLstPathOfFiles(String path) {
		List<String> lstPaths = new ArrayList<String>();
		try {
			File folder = new File(path);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					if (!listOfFiles[i].getName().equalsIgnoreCase(".DS_Store")) {
						if (listOfFiles[i].getName().indexOf("~") < 0) {
							lstPaths.add(path + listOfFiles[i].getName());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstPaths;
	}
	
	
	public static List<String> getLstNameOfFiles(String path) {
		List<String> lstNames = new ArrayList<String>();
		try {
			File folder = new File(path);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					if (!listOfFiles[i].getName().equalsIgnoreCase(".DS_Store")) {
						if (listOfFiles[i].getName().indexOf("~") < 0) {
							lstNames.add(listOfFiles[i].getName());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstNames;
	}
	
}