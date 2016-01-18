package com.nj.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.jcas.JCas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.mayo.bmi.medtagger.util.SimpleTools;

public class Converter {

	public static void entityHm2qdmXml(LinkedHashMap<String,String> entityHm, HashMap<String,String> umlskeyValueHm, String qdmDir, String qdmXmlOut){
		// TODO Auto-generated method stub
		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder = null;
		//I need to call casReader to read the UIMA cas file to find CUI or TUI
		try {
			if(!new File(qdmXmlOut).exists()){
				new File(qdmXmlOut).mkdir();
			}
			PrintWriter pwQdm = new PrintWriter(new FileWriter(new File(qdmDir,qdmXmlOut)));
			try {
				icBuilder = icFactory.newDocumentBuilder();
				Document doc = icBuilder.newDocument();
				Element mainRootElement = doc.createElementNS("http://www.w3.org/2001/XMLSchema-instance", "QualityMeasureDocument");

				doc.appendChild(mainRootElement);

				icBuilder = icFactory.newDocumentBuilder();
				//the following needs to change:
				List<Element> eleList = new ArrayList<Element>();
				SimpleTools.genEle(eleList, entityHm,umlskeyValueHm, doc);
//				for(Map.Entry<String, String> entityEntry : entityHm.entrySet()){
//					String entName = entityEntry.getKey();
//					String entityString = entityEntry.getValue();
//					SimpleTools.genEle(eleList, entName, doc, entityString);
//				}


				for(int i=0;i<eleList.size();i++){
					Element highestEle = eleList.get(i);
					mainRootElement.appendChild(highestEle);
				}

				// output DOM XML to console 
				Transformer transformer;
				try {
					transformer = TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
					DOMSource source = new DOMSource(doc);
					//StreamResult console = new StreamResult(System.out);
					//StringWriter outWriter = new StringWriter();
					//StreamResult console = new StreamResult(outWriter);
					StreamResult console = new StreamResult(pwQdm);
					//							if(doc.getDoctype()!=null){
					//								 String systemValue = (new File (doc.getDoctype().getSystemId())).getName();
					//								 transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);
					//							}
					//							 String systemValue = (new File (qdmXmlOut,xmlFile.getName())).getName();
					//							 transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);

					//StreamResult console = new StreamResult(pwQdm);
					try {
						transformer.transform(source, console);
						//StringBuffer sb = outWriter.getBuffer(); 
						//String finalstring = sb.toString();
						//pwQdm.println(finalstring);
						pwQdm.flush();
						pwQdm.close();
					} catch (TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (TransformerConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformerFactoryConfigurationError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("\nXML DOM Created Successfully..");
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}


	public static void file2xml(String[] args){
		// TODO Auto-generated method stub
		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder = null;
		File dir = new File(args[0]);
		String qdmXmlOut = args[1];
		File[] xmlFiles = dir.listFiles();
		for(File xmlFile : xmlFiles){	
			try {
				if(!xmlFile.getName().endsWith("xml")){
					continue;
				}
				if(!new File(qdmXmlOut).exists()){
					new File(qdmXmlOut).mkdir();
				}
				PrintWriter pwQdm = new PrintWriter(new FileWriter(new File(qdmXmlOut,xmlFile.getName())));
				BufferedReader brXml = new BufferedReader(new FileReader(xmlFile));
				String xmlText = "";
				String line = "";
				while((line=brXml.readLine())!=null){
					xmlText+=line;
				}
				try {
					icBuilder = icFactory.newDocumentBuilder();
					Document doc = icBuilder.newDocument();
					Element mainRootElement = doc.createElementNS("http://www.w3.org/2001/XMLSchema-instance", "QualityMeasureDocument");

					doc.appendChild(mainRootElement);
					icBuilder = icFactory.newDocumentBuilder();
					List<Element> eleList = XmlFromCem2Qdm.xmlIO(xmlText,doc);
					for(int i=0;i<eleList.size();i++){
						Element highestEle = eleList.get(i);
						mainRootElement.appendChild(highestEle);
					}

					// output DOM XML to console 
					Transformer transformer;
					try {
						transformer = TransformerFactory.newInstance().newTransformer();
						transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
						DOMSource source = new DOMSource(doc);
						//StreamResult console = new StreamResult(System.out);
						//StringWriter outWriter = new StringWriter();
						//StreamResult console = new StreamResult(outWriter);
						StreamResult console = new StreamResult(pwQdm);
						//								if(doc.getDoctype()!=null){
						//									 String systemValue = (new File (doc.getDoctype().getSystemId())).getName();
						//									 transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);
						//								}
						//								 String systemValue = (new File (qdmXmlOut,xmlFile.getName())).getName();
						//								 transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemValue);

						//StreamResult console = new StreamResult(pwQdm);
						try {
							transformer.transform(source, console);
							//StringBuffer sb = outWriter.getBuffer(); 
							//String finalstring = sb.toString();
							//pwQdm.println(finalstring);
							brXml.close();
							pwQdm.flush();
							pwQdm.close();
						} catch (TransformerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (TransformerConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TransformerFactoryConfigurationError e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("\nXML DOM Created Successfully..");
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}

	public static void main(String[] args) {

	}

}
