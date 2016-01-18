package com.nj.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.ctakes.typesystem.type.structured.Demographics;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.LabInterpretationModifier;
import org.apache.ctakes.typesystem.type.textsem.LabReferenceRangeModifier;
import org.apache.ctakes.typesystem.type.textsem.LabValueModifier;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;

public class CRFFormat2HongNaAnnot {
	
	public static LinkedHashMap<String,String> convertCRF2HongnaAnnot(String crfFileName, String hongnaFileName){
		LinkedHashMap<String,String> entityHm = new LinkedHashMap<String,String>();
		HashMap<Integer,String> indWordHm = new HashMap<Integer,String>();
		try {
			BufferedReader bfSentFileName = new BufferedReader(new FileReader(new File(hongnaFileName)));
			String line  = "";
			int count = 0;
			while((line=bfSentFileName.readLine())!=null){
				String[] lineArr = line.split("\\s+");
				for(int i =0;i<lineArr.length;i++){
					indWordHm.put(i, lineArr[i]);
				}
				count++;
			}
			bfSentFileName.close();
			BufferedReader bfCrfFileName = new BufferedReader(new FileReader(new File(crfFileName)));
			line = "";
			String entity = "",label="";
			count = 0;
			while((line=bfCrfFileName.readLine())!=null){
				String[] lineArr = line.split("\t");
				String predVal = lineArr[1];
				String[] entArr = predVal.split("-");
				//the first case suppose that before the beginning of the entity is non-entity 
				//
				if(entArr[0].equals("B")&&entity.trim().length()==0){
					label = entArr[1];
					entity=indWordHm.get(count)+" ";
				}
				//the first case suppose that before the beginning of the entity is another entity
				else if(entArr[0].equals("B") && entity.trim().length()>0){
					String hongNaLabel = CRFFormat2HongNaAnnot.convertCRFLabel2HongnaLabel(label, entity);
					entityHm.put(hongNaLabel, entity);
					label = entArr[1];
					entity=indWordHm.get(count)+" ";
				}
				else if(entArr[0].equals("I")){
					entity+=indWordHm.get(count)+" ";
				}else if(entArr[0].equals("O") && entity.trim().length()>0){
					String hongNaLabel = CRFFormat2HongNaAnnot.convertCRFLabel2HongnaLabel(label, entity);
					entityHm.put(hongNaLabel, entity);
					entity="";
					label="";
				}
				count++;
			}
			if(entity.trim().length()>0){
				String hongNaLabel = CRFFormat2HongNaAnnot.convertCRFLabel2HongnaLabel(label, entity);
				entityHm.put(hongNaLabel, entity);
			}
			bfCrfFileName.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return entityHm;
	}

	/**
	 * see generateTrainFeatureLabel and genAnnotatedFV from ECFeatureGenerator.java
	 * 
	 * 		if(typeNodeVal.equalsIgnoreCase("labtest")||typeNodeVal.equalsIgnoreCase("labtestABB")){
				EntityMention mention = new EntityMention(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("resultValue")||
					typeNodeVal.equalsIgnoreCase("unit") || typeNodeVal.equalsIgnoreCase("unitInString")){
				LabValueModifier mention = new LabValueModifier(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("symbolInString") ||typeNodeVal.equalsIgnoreCase("symbol") ){
				LabReferenceRangeModifier mention = new LabReferenceRangeModifier(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("resultString")||
				typeNodeVal.equalsIgnoreCase("exclusion") || typeNodeVal.equalsIgnoreCase("boolean")) {
				LabInterpretationModifier mention = new LabInterpretationModifier(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("method")){
				MeasurementAnnotation mention = new MeasurementAnnotation(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("body&Organ")||
					typeNodeVal.equalsIgnoreCase("bodylamp;organ")){
				AnatomicalSiteMention mention = new AnatomicalSiteMention(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("Demographic")){
				Demographics demographics = new Demographics(aJCas);
			}else if(typeNodeVal.equalsIgnoreCase("TempCondition")){
				TimeMention time = new TimeMention(aJCas);
			}
	 * @param crfLabel
	 * @return
	 */
	public static String convertCRFLabel2HongnaLabel(String crfLabel,String crfValue){
		String hongnaLabel = "";
		if(crfLabel.equals("time")){
			hongnaLabel = "TempCondition";
		}else if(crfLabel.equals("entity")){
			hongnaLabel = "labtest";
		}else if(crfLabel.equals("labintermod") && (crfValue.equalsIgnoreCase("true")
				||crfValue.equalsIgnoreCase("false"))){
			hongnaLabel = "boolean";
		}else if(crfLabel.equals("labintermod") && (!crfValue.equalsIgnoreCase("false")
				&& !crfValue.equalsIgnoreCase("false"))){
			hongnaLabel = "resultString";
		}else if(crfLabel.equals("labvalmod") && (isNumeric(crfValue))){
			hongnaLabel = "resultValue";
		}
		else if(crfLabel.equals("labvalmod") && (!isNumeric(crfValue))){
			hongnaLabel = "unitInString";
		}
//		else if(crfLabel.equals("labvalmod") && (isString(crfValue))){
//			hongnaLabel = "unitInString";
//		}
//		else if(crfLabel.equals("labvalmod") && (isUnit(crfValue))){
//			hongnaLabel = "unit";
//		}
		else if(crfLabel.equals("anatomical")){
			hongnaLabel = "body&organ";
		}
		else if(crfLabel.equals("labrefrangemod")){
			hongnaLabel = "symbolInString";
		}
//		else if(crfLabel.equals("labrefrangemod") && isString(crfValue)){
//			hongnaLabel = "symbolInString";
//		}
//		else if(crfLabel.equals("labrefrangemod") && isSymbol(crfValue)){
//			hongnaLabel = "symbol";
//		}
		else if(crfLabel.equals("measure")){
			hongnaLabel = "method";
		}
		return hongnaLabel;
	}
	
	
	//******************************************************************************************
	//*************** 4 boolean functions to match DC output and HN's annotation****************
	
	public static boolean isNumeric(String str)
	{
	    for (char c : str.toCharArray())
	    {
	        if (!Character.isDigit(c)) 
	        	return false;
	    }
	    return true;
	}
	
	
	public static boolean isString(String str){
		if (str instanceof String)
			return true;
		else
			return false;
	}
	
	
	public static boolean isUnit(HashMap<String,Integer> unitHm, String crfValue){
		if(unitHm.containsKey(crfValue)){
			return true;
		}
		return false;
	}
	
//	public static boolean isSymbol(String crfValue){
//		return false;
//		
//	}
	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CRFFormat2HongNaAnnot.convertCRF2HongnaAnnot(args[0], args[1]);
	}

}
