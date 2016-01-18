package edu.mayo.bmi.medtagger.ml.crfsuite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import edu.mayo.bmi.medtagger.util.FileProcessor;

public class CRFTrainMain {

	protected static boolean testonefile = false;
	protected static int[] test_numbers = {41,63,78,33,46,16,128,105,48, 130,54,65,36, 8, 111, 9,96, 113, 142, 127,50, 
		135, 122,73, 138,88,32, 123,15,64,40,81,39, 141,17,45,91,75, 124,79,22,43};
	//	static int[] test_numbers = {63,33,16,105, 130,65, 8, 9, 113, 127, 122, 138,32, 15,40,39,17,91, 124,22};
	protected static List<Integer> test_numbersList;

	public static void main(String[] args) throws Exception {
		// parse CPE descriptor in file specified on command line
		//resources/medtaggerresources/machinelearning/crfsuite/osx_x86_64/bin/crfsuite
		String usage = "Usage: To train a CRF model:\n"
				+ "java -cp <this jar file> [-csh crfsuitehome [-cpexml CPEFile] [-train trainData] [-model modelFileStr]\n";
		if (args.length == 0 || args.length > 0
				&& ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}
		String csh = null;
		String cpexml = null;
		String trainFile = null;
		String modelFileStr = null;
		String featureFile = null;
		String testDirName = null;
		String predOutput = null;
		String evaOutput = null;
		
		PrintWriter pwOutput = null;


		for (int i = 0; i < args.length; i++) {
			// System.out.println(i+" "+args[i]);
			if("-csh".equals(args[i])){
				csh = args[i + 1];
				i++;
			}
			if ("-cpexml".equals(args[i])) {

				cpexml = args[i + 1];
				i++;
			} else if ("-train".equals(args[i])) {
				trainFile = args[i + 1];
				i++;
			} else if ("-model".equals(args[i])) {
				modelFileStr = args[i + 1];
				i++;
			} else if("-features".equals(args[i])){
				featureFile = args[i + 1];
				i++;
			}else if("-test".equals(args[i])){
				testDirName = args[i + 1];
				i++;
			}else if("-testonefile".equals(args[i])){
				testDirName = args[i + 1];	
				testonefile = true;
				i++;
			}else if("-predOutput".equals(args[i])){
				predOutput = args[i+1];
				i++;
			}else if("-evaOutput".equals(args[i])){
				evaOutput = args[i+1];
				i++;
			}		}


		try {
			if(cpexml!=null){
				CpeDescription cpeDesc;
				cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(
						new XMLInputSource(cpexml));
				CollectionProcessingEngine mCPE;
				mCPE = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);
				mCPE.process();
				// code to make sure CPE has finished
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				System.out.println(dateFormat.format(date)+":"+"waiting for CPE to finish .... ");
				Thread.sleep(60000);
				//Thread.sleep(600);
				File tmpFile=new File(trainFile);
				System.out.println("tmpFile in CRFTrainMain: "+tmpFile.getAbsolutePath());
				//tmpFile.delete();
				//tmpFile=new File(trainFile);
				long size=tmpFile.length();
				System.out.println("size of tmpFile:"+size +" trainFile="+trainFile);
				boolean change=true;
				while(change){
					Thread.sleep(300000);
					date = new Date();
					System.out.println(dateFormat.format(date)+":"+"still waiting for CPE to finish .... ");
					tmpFile=new File(trainFile);
					long size1=tmpFile.length();
					System.out.println("size of tmpFile in change while loop:"+size1 );
					if(size1==size){
						change=false;
					}
					else{
						change=true;
					}
					size=size1;
				}	
			}
			CRFSuiteWrapper csw=new CRFSuiteWrapper(csh);
			if(trainFile!=null){
				System.out.println("Start building models");
				csw.trainClassifier(modelFileStr, trainFile, new String[]{});
			}

			/**
			 * the first if is used for web input. Namely, only one file or one passage is input by the user
			 */
			if(testDirName!=null && testonefile){
				File testDirFile = new File(testDirName); //for now, I will always assign a number to the file generated.
				//int[] oneFileTest  = {Integer.valueOf(testDirFile.getName().split("\\.")[0])};
				int[] oneFileTest = {111};
				CRFTrainMain.crfTest(pwOutput, modelFileStr, testDirName, csw, oneFileTest,predOutput,evaOutput);
			}else if(testDirName!=null && !testonefile){
//				String predOutput = "test/prediction_output/";
//				String evaOutput = "test/prediction_eva/";
				CRFTrainMain.crfTest(pwOutput, modelFileStr, testDirName, csw, test_numbers,predOutput,evaOutput);
			}
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * 
	 * @param pwOutput
	 * @param modelFileStr
	 * @param testDirName
	 * @param csw
	 * @throws IOException
	 */
	private static void crfTest(PrintWriter pwOutput,String modelFileStr,String testDirName,
			CRFSuiteWrapper csw,int[] test_numbers,String predOutput,String evaOutput) throws IOException{
		File modelFile = new File(modelFileStr);
		System.out.println(modelFile.getName());
		//HashMap<String,Integer> featIndHm = FileProcessor.readFile(featureFile, "\t");
		test_numbersList=Arrays.asList(ArrayUtils.toObject(test_numbers));
		File testFileDir = new File(testDirName);
		System.out.println("testDirName="+testDirName);
		File[] testFiles = testFileDir.listFiles();
		for(int i=0;i<testFiles.length;i++){
			File ithFile = testFiles[i];
			//System.out.println(ithFile.getName());
			//System.out.println(ithFile.getName().contains(modelFile.getName().substring(0, modelFile.getName().indexOf(".models"))));
			//					System.out.println(test_numbersList.contains(Integer.valueOf(ithFile.getName().split("\\.")[0]))+
			//							" "+Integer.valueOf(ithFile.getName().split("\\.")[0]));
			if(ithFile.getName().contains(modelFile.getName().substring(0, modelFile.getName().indexOf(".models")))
					&& ithFile.getName().endsWith(".crffv") //&& 
					//test_numbersList.contains(Integer.valueOf(ithFile.getName().split("\\.")[0]))
					){
				System.out.println(ithFile.getName().contains(modelFile.getName().substring(0, modelFile.getName().indexOf(".models")))
						+" "+ithFile.getName());

				if(test_numbersList.contains(Integer.valueOf(ithFile.getName().split("\\.")[0]))){
					String outputFile = predOutput+ithFile.getName();
					pwOutput = new PrintWriter(new FileWriter(new File(outputFile)));
					//List<String> result=csw.classifyFeatures(ithFile, new File(modelFileStr), featIndHm.size());
					List<String> result=csw.classifyFeatures(ithFile, new File(modelFileStr), 20);
					for(int j=0;j<result.size();j++){
						pwOutput.println(result.get(j));
					}
					pwOutput.flush();
					pwOutput.close();
					String outputFile2 = evaOutput+ithFile.getName();
					pwOutput = new PrintWriter(new FileWriter(new File(outputFile2)));
					//List<String> result=csw.classifyFeatures(ithFile, new File(modelFileStr), featIndHm.size());
					List<String> evaResult=csw.classifyFeatures2(ithFile, new File(modelFileStr));
					for(int j=0;j<evaResult.size();j++){
						pwOutput.println(evaResult.get(j));
					}
					pwOutput.flush();
					pwOutput.close();
				}
			}
		}
	}

}
