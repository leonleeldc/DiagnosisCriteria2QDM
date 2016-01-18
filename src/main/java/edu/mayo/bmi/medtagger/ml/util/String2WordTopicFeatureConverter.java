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

public class String2WordTopicFeatureConverter {

	// training set or testing set
		private String dataSetType;
		
		private static String inputDir;
		
		// feature set: bigram+uniNorm+eventStr+context+document+postTagging+orthography+affix+lda_docfeatures
		private String featureSet;
		

		// vocabulary path
		private static final String VOCABULARY_PATH = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/ldaModels/bigram_uniNorm_eventStr.wordmap";
		
		// vocabulary
		private static ArrayList<String> vocabulary;
		
		// training set's "p(word|topic)" distribution file
		private static final String TRAINING_PHI = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/ldaModels/i2b22010_txt.bigram_uniNorm_eventStr-final_6.phi";
		
		// testing set's "p(word|topic)" distribution file
		private static final String TESTING_PHI = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/ldaModels/aspects.json.i2b22010_txt.bigram_uniNorm_eventStr-final_6.phi";
		
		private static final String TRAINING_WORD_TOPIC_ASSIGN = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/ldaModels/i2b22010_txt.bigram_uniNorm_eventStr-final_6.tassign";
		private static final String TESTING_WORD_TOPIC_ASSIGN = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/ldaModels/aspects.json.i2b22010_txt.bigram_uniNorm_eventStr-final_6.tassign";
		
		private static ArrayList<ArrayList<Double>> wordTopicDistribution;
		
		private static List<String> labelArray;
		
		
		/**
		 * Constructor
		 * @throws IOException 
		 */
		public String2WordTopicFeatureConverter(String featureSet, String dataSetType) throws IOException {
			this.dataSetType = dataSetType;
			labelArray = Arrays.asList("absent",
									   "associated_with_someone_else",
									   "conditional",
									   "hypothetical",
									   "possible",
									   "present");
//			getFeatureType();
			this.featureSet = featureSet;
			processVocabulary();
			if(dataSetType.contains("train")) {
				processWordTopicDistribution(TRAINING_PHI);
				inputDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/release3_merged/context_features/";
			}
			else {
				processWordTopicDistribution(TESTING_PHI);
				inputDir = "/Users/ningxia/Works/workspace/Tools/i2b2Challenge2010/Data/Test/reports/context_features/";
			}
		}
		
		private static BufferedReader getReader(String name) throws FileNotFoundException {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(name))));
			return br;
		}
		
		private static BufferedWriter getWriter(String name) throws FileNotFoundException {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(name))));
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
				for(String token: tokens) {
					al.add(Double.parseDouble(token));
				}
				wordTopicDistribution.add(al);
			}
			br.close();
		}
		
		private static void processTopicAssign() {
			
		}
		
		public void writeTrain() throws IOException {
			String inputName = inputDir + "features.svmsuite." + featureSet;
			System.out.println(inputName);
			String outputName = inputName + ".numericalFV.svm";
			BufferedReader br = getReader(inputName);
			BufferedWriter bw = getWriter(outputName);
			String line = "";
			String label = "";
			StringTokenizer st = null;
			String token = "";
			ArrayList<Double> docTopicProbability = null;
			ArrayList<String> tokens = null;
			SortedMap<Integer, Double> map = null;
			int wordIndex = 0;
			double value;
			while((line = br.readLine()) != null) {
				st = new StringTokenizer(line, "\t");
				label = st.nextToken();
				docTopicProbability = new ArrayList<Double>();
				tokens = new ArrayList<String>();
				while(st.hasMoreTokens()) {
					token = st.nextToken();
					if(token.startsWith("LDA")) {
						docTopicProbability.add(Double.parseDouble(token.substring(token.lastIndexOf("=") + 1)));
					}
					else {
						tokens.add(token.toLowerCase());
					}
				}
				
				map = new TreeMap<Integer, Double>();
				for(int i = 0; i < tokens.size(); i ++) {
					token = tokens.get(i);
					wordIndex = vocabulary.indexOf(token);
					value = getLDAKL(wordIndex, docTopicProbability);
					if(value == 0.0) continue;
					map.put(wordIndex + 1, value);
				}

				line = labelArray.indexOf(label) + " ";
				for(Integer i: map.keySet()) {
					line += i + ":" + map.get(i) + " ";
				}
				
				System.out.println(".");
				bw.write(line.trim());
				bw.newLine();
			}
			br.close();
			bw.close();
		}
		
		public void writeTest() throws IOException {
			File fInput = new File(inputDir);
			String inputName;
			BufferedReader br = null;
			BufferedWriter bw = null;
			String line = "";
			String label = "";
			StringTokenizer st = null;
			ArrayList<Double> docTopicProbability = null;
			ArrayList<String> tokens = null;
			String token = "";
			SortedMap<Integer, Double> map = null;
			int wordIndex = 0;
			double value;
			for(File f: fInput.listFiles()) {
				if(!f.getName().endsWith(featureSet)) continue;
				System.out.println(f.getName());
				inputName = f.getCanonicalPath();
				br = getReader(inputName);
				bw = getWriter(inputName + ".numericalFV.svm");
				while((line = br.readLine()) != null) {
					st = new StringTokenizer(line, "\t");
					// ignore: concept
					st.nextToken();
					// ignore: begin
					st.nextToken();
					// ignore: end
					st.nextToken();
					label = st.nextToken();
					docTopicProbability = new ArrayList<Double>();
					tokens = new ArrayList<String>();
					while(st.hasMoreTokens()) {
						token = st.nextToken();
						if(token.startsWith("LDA")) {
							docTopicProbability.add(Double.parseDouble(token.substring(token.lastIndexOf("=") + 1)));
						}
						else {
							tokens.add(token.toLowerCase());
						}
					}
					
					map = new TreeMap<Integer, Double>();
					for(int i = 0; i < tokens.size(); i ++) {
						token = tokens.get(i);
						wordIndex = vocabulary.indexOf(token);
						// not all the tokens in testing set are in the training set
						if(wordIndex == -1) continue;
						value = getLDAKL(wordIndex, docTopicProbability);
						if(value == 0.0) continue;
						map.put(wordIndex + 1, value);
					}

					line = labelArray.indexOf(label) + " ";
					for(Integer i: map.keySet()) {
						line += i + ":" + map.get(i) + " ";
					}
					
					System.out.println(".");
					bw.write(line.trim());
					bw.newLine();
				}
				br.close();
				bw.close();
			}
		}
		
		public static double getWordDocKL(int wordIndex, ArrayList<Double> docTopicProbability) {
			double kl = 0.0;
			double pWordTopic = 0.0;
			double pDocTopic = 0.0;
			for(int i = 0; i < docTopicProbability.size(); i ++) {
				pDocTopic = docTopicProbability.get(i);
				pWordTopic = wordTopicDistribution.get(i).get(wordIndex);
				if(pDocTopic == 0.0 || pWordTopic == 0.0) continue;
				kl += pWordTopic * Math.log(pWordTopic / pDocTopic);
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
			return 1.0 / (getWordDocKL(wordIndex, docTopicProbability) + getDocWordKL(wordIndex, docTopicProbability));
		}
		
		/**
		 * @param args
		 * @throws IOException 
		 */
		public static void main(String[] args) throws IOException {
			
			String2WordTopicFeatureConverter s2KL = new String2WordTopicFeatureConverter(args[0], args[1]);

			if(s2KL.dataSetType.equals("train"))
				s2KL.writeTrain();
			else
				s2KL.writeTest();
			
		}

}
