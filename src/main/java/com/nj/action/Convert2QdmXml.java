package com.nj.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Rachel Xiu
 * The calss contains the method to produce type 1 xml,
 */

public class Convert2QdmXml {

//	public static Element genType2Ele(Document doc){
//		
//	}
//	
//	public static Element genType3Ele(Document doc){
//		
//	}
	
	public static Element genType1Ele(Document doc, HashMap<String,String> entityHm, HashMap<String,String> entityCuiHm){
		
		String typeNode = "", valueNode = "", cuiString;
		if(entityHm.containsKey("labtestABB")){
			typeNode = entityHm.get("labtestABB");
			
		}else if(entityHm.containsKey("labtest")){
			typeNode = entityHm.get("labtestABB");
			
		}else if(entityHm.containsKey("unit") && entityHm.containsKey("unitString")){
			typeNode = entityHm.get("unit");
			typeNode+= " "+ entityHm.get("unitString");
			
		}else if(entityHm.containsKey("unitInString")){
			typeNode = entityHm.get("unitInString");
			
		}else if(entityHm.containsKey("symbolInString")){
			typeNode = entityHm.get("symbolInString");
			
		}else if(entityHm.containsKey("symbol")){
			typeNode = entityHm.get("symbol");
			
		}else if(entityHm.containsKey("resultString")){
			typeNode = entityHm.get("resultString");
			
		}else if(entityHm.containsKey("boolean")){
			typeNode = entityHm.get("boolean");
			
		}else if(entityHm.containsKey("method")){
			typeNode = entityHm.get("method");
			
		}else if(entityHm.containsKey("body&Organ")){
			typeNode = entityHm.get("body&Organ");
			
		}else if(entityHm.containsKey("Demographic")){
			typeNode = entityHm.get("Demographic");
			
		}

		cuiString = entityCuiHm.get(typeNode);
		
		if(entityHm.containsKey("resultValue")){
			valueNode = entityHm.get("resultValue");
		}
		
		List<Element> seventhLowestEleList = new ArrayList<Element>();
		
		Element conjunctEle = doc.createElement("conjunctionCode");
		conjunctEle.setAttribute("code", "AND");
		seventhLowestEleList.add(conjunctEle);
		
		SortedMap<String,Object> actAttrHm = new TreeMap<String,Object>();
		actAttrHm.put("classCode", "ACT");
		actAttrHm.put("moodCode", "EVN");
		actAttrHm.put("isCriterionInd", "true");
		
		
		List<Element> sixthLowestEleList = new ArrayList<Element>();
		Element templateIdEle = doc.createElement("templateId");
		templateIdEle.setAttribute("root", "2.16.840.1.113883.3.560.1.12");
		Element idEle = doc.createElement("id");
		templateIdEle.setAttribute("root", "8a4d92b2-397a-48d2-0139-c61d57a55046");
		Element codeEle = doc.createElement("code");
		codeEle.setAttribute("code", "30954-2");
		codeEle.setAttribute("displayName", "Results");
		codeEle.setAttribute("codeSystem", "2.16.840.1.113883.6.1");
		
		sixthLowestEleList.add(templateIdEle);
		sixthLowestEleList.add(idEle);
		sixthLowestEleList.add(codeEle);
		
		List<Element> fourthLowestEleList = new ArrayList<Element>();
		codeEle = doc.createElement("code");
		
		//codeEle.setAttribute("code", cuiString);
		codeEle.setAttribute("code", "2.16.840.1.113883.3.464.1003.198.12.1013");
		//codeEle.setAttribute("displayName", "HbA1c Laboratory Test Grouping Value Set");

		if(typeNode!=""){
			codeEle.setAttribute("displayName", typeNode + " Laboratory Test Grouping Value Set");
			codeEle.setAttribute("codeSystem", "2.16.840.1.113883.3.560.101.1");
			fourthLowestEleList.add(codeEle);
		}
		
		//Element titleEle = Convert2QdmXml.setTerminalElement(doc, "title", "Laboratory Test, Result: HbA1c Laboratory Test (result)");
		if(typeNode!=""){
			Element titleEle = Convert2QdmXml.setTerminalElement(doc, "title", "Laboratory Test, Result:"+ typeNode+" Laboratory Test (result)");
			fourthLowestEleList.add(titleEle);
		}
		
		
		
		Element statusCodeEle = doc.createElement("statusCode");
		statusCodeEle.setAttribute("code", "completed");
		fourthLowestEleList.add(statusCodeEle);
		
		//the lowest elements
		Element tempIdEle = doc.createElement("templateId");
		tempIdEle.setAttribute("root", "2.16.840.1.113883.3.560.1.1019.3");

		SortedMap<String,Object> attrValueHm = new TreeMap<String,Object>();
		attrValueHm.put("code", "385676005");
		attrValueHm.put("codeSystem", "2.16.840.1.113883.6.96");
		attrValueHm.put("displayName", "result");
		attrValueHm.put("codeSystemName", "SNOMED-CT");
		List<Element> lowestEleList = new ArrayList<Element>();
		codeEle = Convert2QdmXml.setIntermediateElement(doc, "code", lowestEleList, attrValueHm);

		Element valueEle = doc.createElement("value");
		tempIdEle.setAttribute("xsitype", "ANYNonNull");


		List<Element> secLowestEleList = new ArrayList<Element>();
		secLowestEleList.add(tempIdEle);
		secLowestEleList.add(codeEle);
		secLowestEleList.add(valueEle);
		//the observation element
		SortedMap<String,Object> obsAttrHm = new TreeMap<String,Object>();
		obsAttrHm.put("classCode", "OBS");
		obsAttrHm.put("moodCode", "EVN");
		obsAttrHm.put("isCriterionInd", "true");
		Element obsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", secLowestEleList, obsAttrHm);

		SortedMap<String,Object> sourceAttrHm = new TreeMap<String,Object>();
		sourceAttrHm.put("typeCode", "REFR");
		List<Element> thirdLowestEleList = new ArrayList<Element>();
		thirdLowestEleList.add(obsEle);
		Element sourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", thirdLowestEleList, sourceAttrHm);

		idEle = doc.createElement("id");
		idEle.setAttribute("root", "8a4d92b2-397a-48d2-0139-c61d57a5503f");
		
		Element titleNode = Convert2QdmXml.setTerminalElement(doc, "title", "Measurement Period");

		lowestEleList = new ArrayList<Element>();
		lowestEleList.add(idEle);
		lowestEleList.add(titleNode);
		lowestEleList.add(valueEle);
		//the observation element
		obsAttrHm = new TreeMap<String,Object>();
		obsAttrHm.put("classCode", "OBS");
		obsAttrHm.put("moodCode", "EVN");
		obsAttrHm.put("isCriterionInd", "true");
		obsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", lowestEleList, obsAttrHm);

		sourceAttrHm = new TreeMap<String,Object>();
		sourceAttrHm.put("typeCode", "DURING");
		thirdLowestEleList = new ArrayList<Element>();
		thirdLowestEleList.add(obsEle);
		Element sourceEle2 = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", thirdLowestEleList, sourceAttrHm);
	
		fourthLowestEleList.add(sourceEle);
		fourthLowestEleList.add(sourceEle2);
		
		sourceAttrHm.put("typeCode", "REFR");
		
		
		SortedMap<String,Object> highObsAttrHm = new TreeMap<String,Object>();
		//<observation classCode="OBS" moodCode="EVN" isCriterionInd="true">
		highObsAttrHm.put("classCode", "OBS");
		highObsAttrHm.put("moodCode", "EVN");
		highObsAttrHm.put("isCriterionInd", "true");
		Element highObsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", fourthLowestEleList, highObsAttrHm);
		
		SortedMap<String,Object> highSourceAttrHm = new TreeMap<String,Object>();
		//<observation classCode="OBS" moodCode="EVN" isCriterionInd="true">
		highSourceAttrHm.put("typeCode", "COMP");
		
		List<Element> fifthLowestEleList = new ArrayList<Element>();
		fifthLowestEleList.add(highObsEle);
		
		Element highSourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", fifthLowestEleList, highSourceAttrHm);

		sixthLowestEleList.add(highSourceEle);
		
		Element actEle = Convert2QdmXml.setIntermediateElement(doc, "act", sixthLowestEleList, actAttrHm);
		seventhLowestEleList.add(actEle);
		//typeCode="PRCN"
		SortedMap<String,Object> highestSourceAttrHm = new TreeMap<String,Object>();
		highestSourceAttrHm.put("typeCode", "PRCN");
		Element highestSourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", seventhLowestEleList, highestSourceAttrHm);

		return highestSourceEle;
	}
	
	public static Element genType1Ele(Document doc, String textVal){
		List<Element> seventhLowestEleList = new ArrayList<Element>();
		
		Element conjunctEle = doc.createElement("conjunctionCode");
		conjunctEle.setAttribute("code", "AND");
		seventhLowestEleList.add(conjunctEle);
		
		SortedMap<String,Object> actAttrHm = new TreeMap<String,Object>();
		actAttrHm.put("classCode", "ACT");
		actAttrHm.put("moodCode", "EVN");
		actAttrHm.put("isCriterionInd", "true");
		
		
		List<Element> sixthLowestEleList = new ArrayList<Element>();
		Element templateIdEle = doc.createElement("templateId");
		templateIdEle.setAttribute("root", "2.16.840.1.113883.3.560.1.12");
		Element idEle = doc.createElement("id");
		templateIdEle.setAttribute("root", "8a4d92b2-397a-48d2-0139-c61d57a55046");
		Element codeEle = doc.createElement("code");
		codeEle.setAttribute("code", "30954-2");
		codeEle.setAttribute("displayName", "Results");
		codeEle.setAttribute("codeSystem", "2.16.840.1.113883.6.1");
		
		sixthLowestEleList.add(templateIdEle);
		sixthLowestEleList.add(idEle);
		sixthLowestEleList.add(codeEle);
		
		List<Element> fourthLowestEleList = new ArrayList<Element>();
		codeEle = doc.createElement("code");
		codeEle.setAttribute("code", "2.16.840.1.113883.3.464.1003.198.12.1013");
		//codeEle.setAttribute("displayName", "HbA1c Laboratory Test Grouping Value Set");
		codeEle.setAttribute("displayName", textVal+ " Laboratory Test Grouping Value Set");
		codeEle.setAttribute("codeSystem", "2.16.840.1.113883.3.560.101.1");
		fourthLowestEleList.add(codeEle);
		
		//Element titleEle = Convert2QdmXml.setTerminalElement(doc, "title", "Laboratory Test, Result: HbA1c Laboratory Test (result)");
		Element titleEle = Convert2QdmXml.setTerminalElement(doc, "title", "Laboratory Test, Result:"+ textVal+" Laboratory Test (result)");
		fourthLowestEleList.add(titleEle);
		
		Element statusCodeEle = doc.createElement("statusCode");
		statusCodeEle.setAttribute("code", "completed");
		fourthLowestEleList.add(statusCodeEle);
		
		//the lowest elements
		Element tempIdEle = doc.createElement("templateId");
		tempIdEle.setAttribute("root", "2.16.840.1.113883.3.560.1.1019.3");

		SortedMap<String,Object> attrValueHm = new TreeMap<String,Object>();
		attrValueHm.put("code", "385676005");
		attrValueHm.put("codeSystem", "2.16.840.1.113883.6.96");
		attrValueHm.put("displayName", "result");
		attrValueHm.put("codeSystemName", "SNOMED-CT");
		List<Element> lowestEleList = new ArrayList<Element>();
		codeEle = Convert2QdmXml.setIntermediateElement(doc, "code", lowestEleList, attrValueHm);

		Element valueEle = doc.createElement("value");
		tempIdEle.setAttribute("xsitype", "ANYNonNull");


		List<Element> secLowestEleList = new ArrayList<Element>();
		secLowestEleList.add(tempIdEle);
		secLowestEleList.add(codeEle);
		secLowestEleList.add(valueEle);
		//the observation element
		SortedMap<String,Object> obsAttrHm = new TreeMap<String,Object>();
		obsAttrHm.put("classCode", "OBS");
		obsAttrHm.put("moodCode", "EVN");
		obsAttrHm.put("isCriterionInd", "true");
		Element obsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", secLowestEleList, obsAttrHm);

		SortedMap<String,Object> sourceAttrHm = new TreeMap<String,Object>();
		sourceAttrHm.put("typeCode", "REFR");
		List<Element> thirdLowestEleList = new ArrayList<Element>();
		thirdLowestEleList.add(obsEle);
		Element sourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", thirdLowestEleList, sourceAttrHm);

		idEle = doc.createElement("id");
		idEle.setAttribute("root", "8a4d92b2-397a-48d2-0139-c61d57a5503f");
		
		Element titleNode = Convert2QdmXml.setTerminalElement(doc, "title", "Measurement Period");

		lowestEleList = new ArrayList<Element>();
		lowestEleList.add(idEle);
		lowestEleList.add(titleNode);
		lowestEleList.add(valueEle);
		//the observation element
		obsAttrHm = new TreeMap<String,Object>();
		obsAttrHm.put("classCode", "OBS");
		obsAttrHm.put("moodCode", "EVN");
		obsAttrHm.put("isCriterionInd", "true");
		obsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", lowestEleList, obsAttrHm);

		sourceAttrHm = new TreeMap<String,Object>();
		sourceAttrHm.put("typeCode", "DURING");
		thirdLowestEleList = new ArrayList<Element>();
		thirdLowestEleList.add(obsEle);
		Element sourceEle2 = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", thirdLowestEleList, sourceAttrHm);
	
		fourthLowestEleList.add(sourceEle);
		fourthLowestEleList.add(sourceEle2);
		
		sourceAttrHm.put("typeCode", "REFR");
		
		
		SortedMap<String,Object> highObsAttrHm = new TreeMap<String,Object>();
		//<observation classCode="OBS" moodCode="EVN" isCriterionInd="true">
		highObsAttrHm.put("classCode", "OBS");
		highObsAttrHm.put("moodCode", "EVN");
		highObsAttrHm.put("isCriterionInd", "true");
		Element highObsEle = Convert2QdmXml.setIntermediateElement(doc, "observation", fourthLowestEleList, highObsAttrHm);
		
		SortedMap<String,Object> highSourceAttrHm = new TreeMap<String,Object>();
		//<observation classCode="OBS" moodCode="EVN" isCriterionInd="true">
		highSourceAttrHm.put("typeCode", "COMP");
		
		List<Element> fifthLowestEleList = new ArrayList<Element>();
		fifthLowestEleList.add(highObsEle);
		
		Element highSourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", fifthLowestEleList, highSourceAttrHm);

		sixthLowestEleList.add(highSourceEle);
		
		Element actEle = Convert2QdmXml.setIntermediateElement(doc, "act", sixthLowestEleList, actAttrHm);
		seventhLowestEleList.add(actEle);
		//typeCode="PRCN"
		SortedMap<String,Object> highestSourceAttrHm = new TreeMap<String,Object>();
		highestSourceAttrHm.put("typeCode", "PRCN");
		Element highestSourceEle = Convert2QdmXml.setIntermediateElement(doc, "sourceOf", seventhLowestEleList, highestSourceAttrHm);
		return highestSourceEle;
	}

//	public static void main(String[] args) {
//		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder icBuilder;
//		try {
//			icBuilder = icFactory.newDocumentBuilder();
//			Document doc = icBuilder.newDocument();
//			Element mainRootElement = doc.createElementNS("http://www.w3.org/2001/XMLSchema-instance", "QualityMeasureDocument");
//			doc.appendChild(mainRootElement);
//			Element highestSourceEle = XmlFromCem2Qdm.xmlIO(xmlText, doc, mainRootElement);
//			
//			mainRootElement.appendChild(highestSourceEle);
//			// output DOM XML to console 
//			Transformer transformer = TransformerFactory.newInstance().newTransformer();
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
//			DOMSource source = new DOMSource(doc);
//			StreamResult console = new StreamResult(System.out);
//			transformer.transform(source, console);
//
//			System.out.println("\nXML DOM Created Successfully..");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private static Element setIntermediateElement(Document doc,String name, List<Element> lowerEleList,SortedMap<String,Object> attributeValueHm){
		Element node = doc.createElement(name);
		for(Map.Entry<String, Object> attrValEntry : attributeValueHm.entrySet()){
			String attr = attrValEntry.getKey();
			Object value = attrValEntry.getValue();
			node.setAttribute(attr, value.toString());
		}
		for(Element ele : lowerEleList){
			node.appendChild(ele);
		}
		return node;
	} 

	// utility method to create text node
	private static Element setTerminalElement(Document doc, String name, String value) {
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value));
		return node;
	}
}
