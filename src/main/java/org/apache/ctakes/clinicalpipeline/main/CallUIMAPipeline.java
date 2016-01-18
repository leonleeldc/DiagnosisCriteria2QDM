package org.apache.ctakes.clinicalpipeline.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.InvalidXMLException;

public class CallUIMAPipeline {

	private UimaPipeline uimaPipeCda;
	private List<String> returnMessages = new ArrayList();

	private static final String crDesc = "/Users/m048100/Documents/workspace-web/xmlView/desc/collection_reader/FilesInDirectoryCollectionReader.xml";
	//private static final String ciDesc = "Add your stuff here";
	private static final String taeDesc = "/Users/m048100/Documents/workspace-web/xmlView/desc/analysis_engine/AggregateUMLSProcessor.xml";
	//private static final String taeDesc = "desc/analysis_engine/AggregatePlaintextUMLSProcessorTester.xml";
	//private static final String taeDesc = "desc/analysis_engine/AggregatePlaintextFastUMLSProcessor.xml";
	//private static final String ccDesc = "/Users/m048100/Documents/workspace/xmlView/desc/cas_consumer/FileWriterCasConsumer.xml";
	private static final String ccDesc = "/Users/m048100/Documents/workspace-web/xmlView/desc/cas_consumer/ec_cc_training.xml";
	
	public void init() {

		//add from here 
		uimaPipeCda = new UimaPipeline();

		try {
			uimaPipeCda.setCollectionReader(crDesc);
			//uimaPipeCda.setCasInitializer(ciDesc);
			//System.out.println("what happened to the dictionary look up?");
			uimaPipeCda.setTae(taeDesc);

			//uimaPipeCda.setTae(ccDesc);
			uimaPipeCda.setCasConsumer(ccDesc);
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//add up to here to the servelet's initialize() method
	}
	
	public String process() {
		//TODO: implement read from file and resolve java.uitl.* issue
		String output = null;
		try {
			String xmifilename = "/Users/m048100/Documents/workspace-web/xmlView/data/test/testoutput/111.txt.xml";
			String featureFileName = "/Users/m048100/Documents/workspace-web/xmlView/test/output_ind/111.xml.crfsuite.unigam.bigram.context.document.concept.conToken.uniNorm.posTagging.orthography.affix.umls.unit.numToken.crffv";
			File xmiFile = new File(xmifilename);
			File featureFile = new File(featureFileName);
			if(xmiFile.exists() && featureFile.exists()){
			}else{
				uimaPipeCda.annotate(null, returnMessages);
			}
			output = readOutputFromFile();
		} catch (AnalysisEngineProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CollectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResourceProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String string : returnMessages){
			System.out.println(string);
		}
		return output;
	}
	
	private String readOutputFromFile() throws IOException{
		//String outputDir = "data/test/testoutput/sample_note_plaintext.xml";
		//this is a temporal solution for the outputDir, better way should be get the dir from command line.
		//String outputFileName = "111.txt.crfsuite.unigam.bigram.context.document.concept.conToken.uniNorm.posTagging.orthography.affix.umls.unit.numToken.crffv";
		String outputFileName = "111.xml.crfsuite.unigam.bigram.context.document.concept.conToken.uniNorm.posTagging.orthography.affix.umls.unit.numToken.crffv";
		String outputStr = "";
		String outputDir = "/Users/m048100/Documents/workspace-web/xmlView/test/output_ind/";
		File outputFile = new File(outputDir,outputFileName);
		BufferedReader bfOut = new BufferedReader(new FileReader(
				outputFile));
		String line = "";
		while ((line = bfOut.readLine()) != null) {
			outputStr += line;
		}
		bfOut.close();
		return outputStr;
		//return outputDir;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CallUIMAPipeline callUIMAPipeline = new CallUIMAPipeline();
		callUIMAPipeline.init();
		callUIMAPipeline.process();
	}

}
