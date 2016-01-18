package edu.mayo.bmi.medtagger.ml.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.mayo.bmi.medtagger.util.Pair;
import edu.mayo.bmi.medtagger.util.SimpleTools;

/**
 * for training data, we do not need to care about where the data is from while for testing data, we do. This can be handled 
 * similarly as TLink SVM.
 * @author m048100
 *
 */
public class String2NumFeatureConverter {
	static boolean debug = false;
	// if train == true generating training data, if false generating testing data.
	static boolean isTrain;
	
	// Use boolean value for LDA feature.
	static boolean booleanLDA = false;
	static boolean ldaOnly = false;
	static String classifier;

	File trainingFile;
	List<String> labelList;
	NavigableMap<String,Double> featValueHm;
	List<String> featList;	

	public String2NumFeatureConverter(File trainingFile,String classifier,boolean isTrain) throws IOException{
		this.trainingFile = trainingFile;
		labelList = new ArrayList<String>();
		featValueHm = new TreeMap<String,Double>();
		featList = this.collectFV(trainingFile, labelList);
		//featValueHm = this.collectFV(trainingFile,labelList);
		this.classifier = classifier;
		this.isTrain = isTrain;
	}

	/**
	 * 
	 * @param inputDir
	 * @return featValueListHm is used for storing features and their valueList. 
	 * @throws IOException
	 */
	public List<String> collectFV(File inputFile,List<String> labelList) throws IOException{
		BufferedReader brFeature = new BufferedReader(new FileReader(inputFile));
		//the reason that we use NavigableMap is that it can provide sorted keysets as well.
		List<String> featList = new ArrayList<String>();
		//in the wordList, I would add all independent features, including unigram, bigrams. Then, I will 
		//convert preToken1, preToken2, sufToken1, sufToken2 as values. 
		String line = "";
		int count = 0;
		while((line=brFeature.readLine())!=null){
			StringTokenizer st = new StringTokenizer(line,"\t ");
			String label = st.nextToken().trim();
			if(label.trim().equals("eventType-1=Disease_Disorder_I")){
				//System.out.println("count: "+count+" line: "+line);
			}
			if(!labelList.contains(label)){
				labelList.add(label);
			}
			st.nextToken();
			while(st.hasMoreTokens()){
				String nextFeature = st.nextToken();
				if(!booleanLDA && nextFeature.startsWith("LDA")) {
					String[] ldaFeatureArr = nextFeature.split("=");
					//nextFeature = nextFeature.substring(0, nextFeature.indexOf("="));
					nextFeature = ldaFeatureArr[0];
					featValueHm.put(nextFeature, Double.parseDouble(ldaFeatureArr[1]));
				}
				if(!featList.contains(nextFeature)){
					featList.add(nextFeature);
				}
				//System.out.println(count);
				count++;
			}
		}
		
		brFeature.close();
		
		Collections.sort(labelList);
		for(int i=0;i<labelList.size();i++){
			System.out.println(labelList.get(i));
		}
		Collections.sort(featList);
		
		System.out.println(" feat value: $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

		return featList;
	}

	/**
	 * 
	 * @param inputDir the directory can be a big directory which include all individual files or it can be
	 * a 
	 * @return
	 * @throws IOException 
	 */
	public void orderedStr2NumFV(File testingFile ) throws IOException{
		//PrintWriter pwFeatureV = new PrintWriter(new FileWriter(new File(inputFile.getParent(),"numericalFV.svm")));
		//System.out.println(testingFile.getAbsolutePath()+".labels.txt");
		PrintWriter pwLabels = null;
		if(this.classifier.equals("slda") || this.classifier.equals("sldashan") || this.classifier.equals("lda_r")){
			pwLabels = new PrintWriter(new FileWriter(new File(testingFile.getAbsolutePath()+".labels.txt")));
		}
		
		
//		for(int i=0;i<labelList.size();i++){
//			pwLabels.println(labelList.get(i));
//		}

		//System.out.println("the length of keySet: "+keySet.length);
		//tokList refers to unigram tokens, which may have values, like tok-0,tok[0-0], tok[0-1] (means right 1), tok[0--1] (means left 1)
//		System.out.println("features: ");

		//		System.out.println("tokValList size: "+ tokValList.size() + " posValList size: "+posValList.size()+" semValList size: "+semValList.size());
		PrintWriter pwFeatureV = new PrintWriter(new FileWriter(new File(testingFile.getAbsolutePath()+".numericalFV."+classifier)));
		System.out.println("feature files saved to: "+testingFile.getAbsolutePath()+".numericalFV."+classifier);
		BufferedReader brFeature = new BufferedReader(new FileReader(testingFile));
		if(classifier.equals("medlda_gr") || classifier.equals("lda")){
			int docNumber = SimpleTools.countLines(testingFile.getAbsolutePath());
			pwFeatureV.println(docNumber);
		}
		
		String line = "";
		int countLine=0;
		while((line=brFeature.readLine())!=null){
			//the data format look like: 
			//"abdominal pain"        20:13   20:14   present  eventType-0=Disease_Disorder_B ctext-0=The     sectionHeader-0=date_of_admission_5.16.35.44    tok[0--2]=SSSSS tok[0--1]=SSSSS tok[0-0]=The    norm[0-0]=the   capital[0-0]=1  pos[0-0]=DT     STCLS[0-0-0]=ALPHA      STCLS[0-1-0]=INITCAPS   STCLS[0-2-0]=UPPER-LOWER        PRE[0-0]=The    SUF[0-0]=The            tok[0-1]=patient        norm[0-1]=patient       capital[0-1]=0  pos[0-1]=NN STCLS[0-0-1]=ALPHA      PRE[0-1]=pat    SUF[0-1]=ent            tok[0-2]=is     norm[0-2]=is    capital[0-2]=0  pos[0-2]=VBZ    STCLS[0-0-2]=ALPHA
			Pattern pattern = Pattern.compile("\"(.*)\"\\s([\\d]+:[\\d]+)\\s([\\d]+:[\\d]+)\\s(.*)");
			Matcher matcher = pattern.matcher(line.trim());
			if(matcher.matches()){
				String phrase = matcher.group(1);
				String[] offsetBegin = matcher.group(2).split(":");
				String[] offsetEnd = matcher.group(3).split(":");
				Pair<String,String> offsetBeginPair = new Pair<String,String>(offsetBegin[0],offsetBegin[1]);
				Pair<String,String> offsetEndPair = new Pair<String,String>(offsetEnd[0],offsetEnd[1]);
				line = matcher.group(4);
			}
			
			StringTokenizer st = new StringTokenizer(line,"\t ");
			String label = st.nextToken().trim();
			
			int labelInd = 0;
			if(labelList.contains(label)){
				SortedMap<Integer, Object> featMap = new TreeMap<Integer, Object>();
				labelInd = labelList.indexOf(label);

				String nextFeature = "";
				int featInd = 0;
				//ignore the first token since it is the label
				st.nextToken();
				while(st.hasMoreTokens()) {
					nextFeature = st.nextToken();
					if(booleanLDA) {
						if(!featList.contains(nextFeature)) continue;
						featInd = featList.indexOf(nextFeature);
						featMap.put(featInd, 1);
					}
					else {
						if(nextFeature.startsWith("LDA")) {
							String[] arrayLDA = nextFeature.split("=");
							featInd = featList.indexOf(arrayLDA[0]);
							if(ldaOnly){
								featMap.put(featInd, Double.parseDouble(arrayLDA[1]));
								continue;
							}
							if(Double.parseDouble(arrayLDA[1]) < 0.2) continue;
							featMap.put(featInd, 1);
							//featMap.put(featInd, Double.parseDouble(arrayLDA[1]));
						}
						else {
							if(ldaOnly){
								continue;
							}
							if(!featList.contains(nextFeature)) continue;
							featInd = featList.indexOf(nextFeature);
							featMap.put(featInd, 1);
						}
					}
				}

				if(classifier.equals("slda")){
					pwLabels.println(labelInd);
					pwFeatureV.print(featMap.size()+" ");
				}else if(classifier.contains("svm")){
					pwFeatureV.print(labelInd+" ");
				}else if(classifier.equals("medlda")){
					pwFeatureV.print(featMap.size()+" "+labelInd+" ");
				}else if(classifier.equals("medlda_gr")){
					pwFeatureV.print(featMap.size()+" "+labelInd+" ");
					for(Entry<Integer, Object> keyValueEntry : featMap.entrySet()){
						int keyInd = keyValueEntry.getKey()+1;
						Object valueInd = keyValueEntry.getValue();
						pwFeatureV.print(keyInd+" ");
					}
					pwFeatureV.println();
					continue;
				}else if(classifier.equals("sldashan")||classifier.equals("lda_r")){
					pwFeatureV.print(labelInd+",");
				}
				
				if(classifier.contains("lda_r")){
					for(int i=0;i<featList.size();i++){
						if(featMap.containsKey(i)){
							//System.out.println();
							pwFeatureV.print(1+",");
							//System.out.print(1+",");
						}else{
							pwFeatureV.print(0+",");
							//System.out.print(0+",");
						}
					}
					pwFeatureV.println();
				}else if(classifier.contains("slda")){
					for(int i=0;i<labelList.size()-1;i++){
					if(labelInd==i){
						//System.out.println(labelInd+" ");
						//for Hanhuai's code, she list all labels, then under each label, use 1 or 0
						//to represent whether that line belongs to that label or not.
						pwLabels.print(1+"\t");
						//for lda topic models in R, we need to know the labels for stats
					}else{
						pwLabels.print(0+"\t");
					}
				}
				pwLabels.println();
				}else{
					for(Map.Entry<Integer, Object> keyValueEntry : featMap.entrySet()){
					int keyInd = keyValueEntry.getKey()+1;
					Object valueInd = keyValueEntry.getValue();
					pwFeatureV.print(keyInd+":"+valueInd+" ");
				}
				pwFeatureV.println();

				}
				
			}
			countLine++;	
		}

		System.out.println("countLine: "+countLine);
		pwFeatureV.flush();
		pwFeatureV.close();
		if(this.classifier.equals("slda") || this.classifier.equals("sldashan")|| this.classifier.equals("lda_r")){
			pwLabels.flush();
			pwLabels.close();
		}
		brFeature.close();
	}

	public static void main(String[] args) throws IOException{
		String featureType = "bigram_uniNorm_eventStr_lda_docfeatures";
		String trainingFeatureDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/context_features/features.svmsuite."+featureType;
		String testingFeatureDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/context_features";
//		String classifier = "svm";
//		String classifier = "slda";
//		String classifier = "medlda";
//		String classifier = "lldashan";
//		String classifier = "lda_r";
		String classifier = "medlda_gr";
		boolean train = true;
		
		if(!train){
			testingFeatureDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/Test/reports/context_features";
		}
		if(args.length>0){
			featureType = args[0];
			trainingFeatureDir = args[1]+"."+featureType;
			testingFeatureDir = args[2];
			classifier = args[3];
			isTrain = Boolean.parseBoolean(args[4]);
			System.out.println("classifier: "+classifier+" feataureType: "+featureType + " isTrain: " + isTrain);
		}

		File trainingFeatureFile = new File(trainingFeatureDir);
		String2NumFeatureConverter s2nConverter = new String2NumFeatureConverter(trainingFeatureFile, classifier,isTrain);

		
		if(isTrain){
			//String trainFileName = "features."+classifier+".suite";
			//File trainFile = new File(testingFeatureFile,trainFileName);
			s2nConverter.orderedStr2NumFV(trainingFeatureFile);
			return;
		}else{
			File testingFeatureFile = new File(testingFeatureDir);
			File[] testFeatureFileArr = testingFeatureFile.listFiles();
			if(debug){
				String fileName = "0135.txt.ecore"; 
				File debugFile = new File(testingFeatureFile,fileName);
				s2nConverter.orderedStr2NumFV(debugFile);
				return;
			}

			
			for(int i=0;i<testFeatureFileArr.length;i++){
				File oneFile = testFeatureFileArr[i];
				if(oneFile.getName().endsWith(featureType)){
					//System.out.println(oneFile);
					s2nConverter.orderedStr2NumFV(oneFile);
				}
			}
		}
	}
}
