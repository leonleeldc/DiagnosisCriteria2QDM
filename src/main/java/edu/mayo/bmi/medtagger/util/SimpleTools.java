package edu.mayo.bmi.medtagger.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.nj.action.Convert2QdmXml;

import java.util.List;
import java.util.Map;

public class SimpleTools {

	
	public static <T> void updateMap(HashMap<T, List<T>> firstLevelNode2pmidHm,  T aKey, T item){
		if(firstLevelNode2pmidHm.containsKey(aKey)){
			List<T> aList = firstLevelNode2pmidHm.get(aKey);
			aList.add(item);
			firstLevelNode2pmidHm.put(aKey, aList);
		}else{
			List<T> aList = new ArrayList<T>();
			aList.add(item);
			firstLevelNode2pmidHm.put(aKey, aList);
		}
	}
	
	public static <T> void updateMap(SortedMap<T,List<T>> aMap, T aKey, T item){
		if(aMap.containsKey(aKey)){
			List<T> aList = aMap.get(aKey);
			aList.add(item);
			aMap.put(aKey, aList);
		}else{
			List<T> aList = new ArrayList<T>();
			aList.add(item);
			aMap.put(aKey, aList);
		}
	}
	
	public static <T> void updateMap(HashMap<T,Integer> aMap, T aKey){
		if(!aMap.containsKey(aKey)){
			aMap.put(aKey, aMap.size());
		}
	}
	
	public static <T> void updateMap(SortedMap<T,Integer> aMap, T aKey){
		if(!aMap.containsKey(aKey)){
			aMap.put(aKey, aMap.size());
		}
	}
	
	public static <T> void updateMap(SortedMap<T,Integer> aMap, SortedMap<Integer,T> contraMap,  T aKey){
		if(!aMap.containsKey(aKey)){
			aMap.put(aKey, aMap.size());
			contraMap.put(aMap.size(),aKey);
		}
	}

	
	public static int countLines(String filename) throws IOException {
		long time = System.currentTimeMillis();
	    LineNumberReader reader  = new LineNumberReader(new FileReader(filename));
	    int cnt = 0;
	    String lineRead = "";
	    while ((lineRead = reader.readLine()) != null) {}
	    cnt = reader.getLineNumber(); 
	    reader.close();
		System.out.println("time taken in ms in SimpleTools.java countLines = "
				+ (System.currentTimeMillis() - time));
	    return cnt;
	}
	
	public static void genEle(List<Element> eleList, String typeNodeVal,Document doc,String textNodeVal){
		//labtest or labtestABB is actually the disease. // #####how many types in HN's input#########
		if(typeNodeVal.equals("labtest")||typeNodeVal.equals("labtestABB")||typeNodeVal.equals("anatomical")){
			Element labTestEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(labTestEle);
		
		}else if(typeNodeVal.equals("resultValue")||
				typeNodeVal.equals("unit") || typeNodeVal.equals("unitInString")){
			Element resultEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(resultEle);

		}else if(typeNodeVal.equals("symbolInString") ||typeNodeVal.equals("symbol") ){
			Element symbolEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(symbolEle);

		}else if(typeNodeVal.equals("resultString")||typeNodeVal.equals("entity")||
			typeNodeVal.equals("exclusion") || typeNodeVal.equals("boolean")) {
			Element boolEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(boolEle);
			
		}else if(typeNodeVal.equals("method")){
			Element measureEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(measureEle);

		}else if(typeNodeVal.equals("body&Organ")){
			Element anatomicalEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(anatomicalEle);

		}else if(typeNodeVal.equals("Demographic")){
			Element demoEle=Convert2QdmXml.genType1Ele(doc, textNodeVal);
			eleList.add(demoEle);
		}
	}
	
	public static void genEle(List<Element> eleList, HashMap<String,String> entityHm, 
			HashMap<String,String> entity2cuiHm,Document doc){
		//labtest or labtestABB is actually the disease. // #####how many types in HN's input#########
		Element labTestEle = Convert2QdmXml.genType1Ele(doc, entityHm, entity2cuiHm);
		eleList.add(labTestEle);
	}

}
