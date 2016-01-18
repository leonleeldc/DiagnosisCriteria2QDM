package edu.mayo.bmi.medtagger.ml.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class String2KLFeatureConverter {
	
	// training set or testing set
	static private String dataSetType;
	
	private static String inputDir;
	
	// feature set: bigram+uniNorm+eventStr+context+document+postTagging+orthography+affix+lda_docfeatures
	private String featureSet;
	

	// vocabulary path
	//private static final String VOCABULARY_PATH = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/ldaModels/bigram_uniNorm_eventStr.wordmap";
	
	static String VOCABULARY_PATH;
	// vocabulary
	private static ArrayList<String> vocabulary;
	
	// training set's "p(word|topic)" distribution file
	//private static final String TRAINING_PHI = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/ldaModels/i2b22010_txt.bigram_uniNorm_eventStr-final_6.phi";
	String TRAINING_PHI;
	
	String TESTING_PHI;
	// testing set's "p(word|topic)" distribution file
	//private static final String TESTING_PHI = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/ldaModels/aspects.json.i2b22010_txt.bigram_uniNorm_eventStr-final_6.phi.global";
	
//	private static final String TRAINING_WORD_TOPIC_ASSIGN = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/i2b2_lda/i2b22010-final.tassign";
//	private static final String TESTING_WORD_TOPIC_ASSIGN = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/i2b2_lda/aspects.json.i2b22010-final.tassign";
	
	private static ArrayList<ArrayList<Double>> wordTopicDistribution;
	
	private static List<String> labelArray;
	//private static boolean iv_klAttached;
	//I found that we may need more approaches rather than binary. So, string is a better way
	private static String iv_klAttached;
	
	/**
	 * Constructor
	 * @throws IOException 
	 */
	public String2KLFeatureConverter(String[] paths,String featureSet, String dataSetType,String klAttached) throws IOException {
		this.dataSetType = dataSetType;
		VOCABULARY_PATH = paths[0];
		this.TRAINING_PHI = paths[1];
		this.TESTING_PHI = paths[2];
		this.iv_klAttached = klAttached; //it means whetehr kl values are attached to original features or independent
		labelArray = Arrays.asList("absent",
								   "associated_with_someone_else",
								   "conditional",
								   "hypothetical",
								   "possible",
								   "present");
//		getFeatureType();
		this.featureSet = featureSet;
		processVocabulary();
		if(dataSetType.contains("train")) {
			processWordTopicDistribution(TRAINING_PHI);
			//inputDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/context_features/";
			inputDir = paths[3];
		}
		else {
			processWordTopicDistribution(TESTING_PHI);
			//inputDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/Test/reports/context_features/";
			inputDir = paths[3];
		}
	}
	
	private static BufferedReader getReader(String name) throws FileNotFoundException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(name))));
		return br;
	}
	
	private static BufferedWriter getWriter(String name) throws FileNotFoundException {
		//in write many files, due to the for loop, dataSetType would add "attached" many, many times until the program dies
		//therefore, we need to guarantee only one time added.
		if(iv_klAttached.contains("attached") && !dataSetType.contains("attached")){
			dataSetType="attached."+dataSetType;
		}else if(iv_klAttached.contains("addValue") && !dataSetType.contains("addValue")){
			dataSetType="addValue."+dataSetType;
		}
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(name+"."+dataSetType))));
		System.out.println("output data is saved to: "+name+"."+dataSetType);
		return bw;
	}
	
	private static void processVocabulary() throws IOException {
		vocabulary = new ArrayList<String>();
		BufferedReader br = getReader(VOCABULARY_PATH);
		String line = "";
		// skip the first line
		br.readLine();
		while((line = br.readLine()) != null) {
			vocabulary.add(line.substring(0, line.indexOf(" ")));
		}
		br.close();
	}
	
	private static void processWordTopicDistribution(String name) throws IOException {
		wordTopicDistribution = new ArrayList<ArrayList<Double>>();
		BufferedReader br = getReader(name);
		System.out.println(name);
		String line = "";
		String[] tokens = null;
		ArrayList<Double> al = null;

		while((line = br.readLine()) != null) {
			al = new ArrayList<Double>();
			tokens = line.split("\\s");
			int count = 0;
			for(String token: tokens) {
				al.add(Double.parseDouble(token));
				count++;
			}
			//System.out.println("phi size: "+al.size());
			wordTopicDistribution.add(al);
		}
		br.close();
	}
	
	public void writeTrain() throws IOException {
		String inputName = inputDir + "features.svmsuite." + featureSet;
		System.out.println(inputName);
		this.write2file(inputName);
	}
	
	public void writeTest() throws IOException {
		File fInput = new File(inputDir);
		String inputName;
		for(File f: fInput.listFiles()) {
			if(!f.getName().endsWith(featureSet)) continue;
			System.out.println(f.getName());
			inputName = f.getCanonicalPath();
			this.write2file(inputName);
		}
	}
	
	private void write2file(String inputName) throws NumberFormatException, IOException{
		String line = "";
		BufferedReader br = getReader(inputName);
		BufferedWriter bw = getWriter(inputName + ".numericalFV.svm");
		Pattern pattern = Pattern.compile("\"(.*)\\s+(\\d+:\\d+)\\s+(\\d+:\\d+)\\s+(.*)");
		Matcher matcher;
		while((line = br.readLine()) != null) {
			matcher = pattern.matcher(line.trim());
			if(matcher.matches()) {
				line = matcher.group(4);
			}
			StringTokenizer st = new StringTokenizer(line, "\t");
			//the following method is not efficient and flexible. For training data and testing data,
			//we have different format. After we use RE, we can handle both.
//			// ignore: concept
//			st.nextToken();
//			// ignore: begin
//			st.nextToken();
//			// ignore: end
//			st.nextToken();
			String label = st.nextToken();
			ArrayList<Double> docTopicProbability = new ArrayList<Double>();
			ArrayList<String> tokens = new ArrayList<String>();
			// skip LDA0=present
			st.nextToken();
			String token = "";
			while(st.hasMoreTokens()) {
				token = st.nextToken();
				if(token.startsWith("LDA")) {
					docTopicProbability.add(Double.parseDouble(token.substring(token.lastIndexOf("=") + 1)));
				}
				else {
					tokens.add(token.toLowerCase());
				}
			}
			
			SortedMap<Integer, Double> map = new TreeMap<Integer, Double>();
			int wordIndex = 0;
			for(int i = 0; i < tokens.size(); i ++) {
				token = tokens.get(i);
				wordIndex = vocabulary.indexOf(token);
				// not all the tokens in testing set are in the training set
				if(wordIndex == -1) continue;
				double value = getLDAKL(wordIndex, docTopicProbability);
				if(value == 0.0) continue;
				map.put(wordIndex + 1, value);
			}

			//the following condition would decide whether original BOW features are still needed.
			if(iv_klAttached.contains("attached")){
				line = labelArray.indexOf(label) + " "+line+" ";
			}else{
				line = labelArray.indexOf(label) + " ";
			}
			
			for(Integer i: map.keySet()) {
				if(iv_klAttached.contains("addValue")){
					line+= i+ ":" + (1+map.get(i)) +" ";
				}else{
					line += i + ":" + map.get(i) + " ";
				}
				//line += i + ":" + 1 + " ";
			}
			
			//System.out.println(".");
			bw.write(line.trim());
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	public static double getWordDocKL(int wordIndex, ArrayList<Double> docTopicProbability) {
		double kl = 0.0;
		double pWordTopic = 0.0;
		double pDocTopic = 0.0;
		for(int i = 0; i < docTopicProbability.size(); i ++) {
			//System.out.println(wordTopicDistribution.get(i).size());
			//System.out.println(wordIndex);
			pDocTopic = docTopicProbability.get(i);
			pWordTopic = wordTopicDistribution.get(i).get(wordIndex);
			if(pDocTopic == 0.0 || pWordTopic == 0.0) continue;
			kl += (pWordTopic) * Math.log(pWordTopic/ pDocTopic);
		}
		return kl;
	}
	
	public static double getDocWordKL(int wordIndex, ArrayList<Double> docTopicProbability) {
		double kl = 0.0;
		double pWordTopic = 0.0;
		double pDocTopic = 0.0;
		for(int i = 0; i < docTopicProbability.size(); i ++) {
			pDocTopic = docTopicProbability.get(i);
			pWordTopic = wordTopicDistribution.get(i).get(wordIndex);
			if(pDocTopic == 0.0 || pWordTopic == 0.0) continue;
			kl += pDocTopic * Math.log(pDocTopic / pWordTopic);
		}
		return kl;
	}
	
	public static double getLDAKL(int wordIndex, ArrayList<Double> docTopicProbability) {
		return (getWordDocKL(wordIndex, docTopicProbability) + getDocWordKL(wordIndex, docTopicProbability));
	}
	
	//"C:\Users\m048100\Documents\i2b2\i2b2Challenge2010\Data\release3_merged\ldaModels\aspects.json.i2b22010_txt.uniNorm_eventStr-final_6.phi.global"  
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String[] paths = new String[4];
		///Users/m048100/Documents/i2b2/i2b2Challenge2010/Data/release3_merged/ldaModels/uniNorm_eventStr.word2idmap 
		///Users/m048100/Documents/i2b2/i2b2Challenge2010/Data/release3_merged/ldaModels/i2b22010_txt.uniNorm_eventStr-final_alpha=0.25_beta=0.1_k=100.phi
		///Users/m048100/Documents/i2b2/i2b2Challenge2010/Data/release3_merged/ldaModels/aspects.json.i2b22010_txt.uniNorm_eventStr-final_6.phi.global /Users/m048100/Documents/i2b2/i2b2Challenge2010/Data/release3_merged/GoldStdsvm_features uniNorm+eventStr train
		//"C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\uniNorm_eventStr.wordmap"
		//"C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\i2b22010_txt.uniNorm_eventStr-final_6.phi"
		//"C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\aspects.json.i2b22010_txt.uniNorm_eventStr-final_6.phi.global"
		//"C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\Test\\reports\Goldstdsvm_features"
		//"uniNorm+eventStr+lda_docfeature" "train"
		//System.out.println(args[0]+"\n"+args[1]+"\n"+args[2]+"\n"+args[3]);
		System.out.println(args.length);
		paths[0] = "C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\uniNorm_eventStr.wordmap";
		paths[1] = "C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\i2b22010_txt.uniNorm_eventStr-final_6.phi";
		paths[2] = "C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\ldaModels\\aspects.json.i2b22010_txt.uniNorm_eventStr-final_6.phi.global";
		paths[3] = "C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\release3_merged\\GoldStdsvm_features\\";
		String featureSet = "uniNorm+eventStr+lda_docfeatures_k=50";
		//String datasetType = args[0];
		String datasetType = "train";
		String klAttached = "addValue";
		if(datasetType.contains("test")){
			paths[3] = "C:\\Users\\m048100\\Documents\\i2b2\\i2b2Challenge2010\\Data\\Test\\reports\\GoldStdsvm_features\\";
		}

		if(args.length>0){
			paths[0]=args[0];
			paths[1]=args[1];
			paths[2]=args[2];
			paths[3]=args[3];
		}
		featureSet = args[4];
		datasetType = args[5];
		//klAttached = Boolean.parseBoolean(args[6]);
		klAttached = args[6];

		String2KLFeatureConverter s2KL = new String2KLFeatureConverter(paths,featureSet, datasetType,klAttached);

		if(dataSetType.contains("train"))
			s2KL.writeTrain();
		else
			s2KL.writeTest();
		
	}

}
