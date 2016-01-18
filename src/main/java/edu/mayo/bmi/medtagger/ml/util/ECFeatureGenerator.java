package edu.mayo.bmi.medtagger.ml.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import edu.mayo.bmi.medtagger.ml.crfsuite.CRFSuiteWrapper;
import edu.mayo.bmi.medtagger.ml.type.Event;
import edu.mayo.bmi.medtagger.ml.type.i2b2Token;
import edu.mayo.bmi.medtagger.ml.type.shareAnnotation;
import edu.mayo.bmi.medtagger.ml.type.shareSlot;
import edu.mayo.bmi.medtagger.ml.type.shareToken;
import edu.mayo.bmi.medtagger.type.ConceptMention;
import edu.mayo.bmi.medtagger.util.CompIntPair;
import edu.mayo.bmi.medtagger.util.FileProcessor;
import edu.mayo.bmi.medtagger.util.OverlapType;
import edu.mayo.bmi.medtagger.util.Pair;
import edu.mayo.bmi.medtagger.util.Triple;







import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
//import edu.mayo.bmi.uima.core.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.structured.Demographics;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabInterpretationModifier;
import org.apache.ctakes.typesystem.type.textsem.LabReferenceRangeModifier;
import org.apache.ctakes.typesystem.type.textsem.LabValueModifier;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;


public class ECFeatureGenerator {

	CRFSuiteWrapper csw=null; 
	File modelFile=null;

	SortedMap<String,Integer> wordStrHm;
	SortedMap<String,Integer> normStrHm;
	SortedMap<String,Integer> cmStrHm;
	SortedMap<String,Integer> contextStrHm;
	HashMap<String,String> contextRuleHm;
	SortedMap<String,Integer> featureIndHm;
	List<String> stopList;
	String featureType;
	private Logger logger = Logger.getLogger(getClass().getName());

	public ECFeatureGenerator(SortedMap<String,Integer> wordStrHm,
			SortedMap<String,Integer> normStrHm,SortedMap<String,Integer> cmStrHm,
			SortedMap<String,Integer> contextStrHm, SortedMap<String,Integer> featureIndHm,
			String featureType) throws Exception{
		this.wordStrHm = wordStrHm;
		this.normStrHm = normStrHm;
		this.cmStrHm = cmStrHm;
		this.contextStrHm = contextStrHm;
		this.featureIndHm = featureIndHm;
		this.featureType = featureType;
		contextRuleHm = new HashMap<String,String>();

		stopList = new ArrayList<String>();
	}
	public ECFeatureGenerator(File modelfile, String csh) throws Exception{	
		csw=new CRFSuiteWrapper(csh);
		modelFile=modelfile;
		contextRuleHm = new HashMap<String,String>();
	}

	@SuppressWarnings("rawtypes")
	public void generateTrainFeatureLabel(JCas aJCas, String type, String inFile, File mOutputDir, String docId, File name,  
			int leftWindow, int rightWindow,HashMap<String, String> mapLDA,
			List<Integer> test_numbersList) throws Exception {
		int localCounter = 0;
		if(docId.contains("1.xml")){
			System.out.println("debug");
		}
		String delimiter = "\\|";
		File contextFile = new File("resources/medtaggerresources/context/contextRule.txt");
		contextRuleHm = FileProcessor.readFile(contextFile, delimiter);
		File stopListFile = new File("resources/medtaggerresources/stoplist_less.txt");//the stoplist is empty for now.
		stopList = FileProcessor.readSimpleFile(stopListFile);
		FileOutputStream out = null;
		//the following aim at generating individual files for featureType so that we can convert the final outputs back to i2b2format easily.
		String modelFileName = mOutputDir.getAbsolutePath()+"_ind" + "/" + docId + "."+this.featureType+".crffv";
		File individualFeatFile = new File(modelFileName);
		logger.info("inFile name="+inFile);
		logger.info("individualFeatFile name="+modelFileName);
		//logger.info("name="+name);
		PrintWriter pwIndFile = new PrintWriter(new FileWriter(individualFeatFile));
		out = new FileOutputStream(name, true);
		logger.info("printStream name="+name);
		final PrintStream printStream = new PrintStream(out); 
		String featureLDA = "";
		StringBuffer sbLDA = new StringBuffer();
		try {
			FSIterator it = aJCas.getAnnotationIndex(DocumentAnnotation.type).iterator();
			AnnotationIndex segIndex = aJCas.getAnnotationIndex(Segment.type);
			AnnotationIndex senIndex = aJCas.getAnnotationIndex(Sentence.type);
			AnnotationIndex tokenIndex = aJCas.getAnnotationIndex(BaseToken.type);
			AnnotationIndex annotIndex = aJCas.getAnnotationIndex(IdentifiedAnnotation.type);
			DocumentAnnotation docInfo;
			if(it.hasNext()){
				docInfo = (DocumentAnnotation) it.next();
			}else return;
			// write XMI


			FSIterator segIter= segIndex.iterator();
			int senCounter = 0;
			while(segIter.hasNext()){
				Segment seg=(Segment) segIter.next();
				String segText = seg.getCoveredText();
				int segBegin = seg.getBegin();
				int segEnd = seg.getEnd();
				FSIterator senIter= senIndex.subiterator(seg);
				//HashMap<Pair<Integer,Integer>, BaseToken> locationMap = null;
				SortedMap<Pair<Integer,Integer>, BaseToken> locationMap = null;
				HashMap<Pair<Integer,Integer>, String> unitPairHm = new HashMap<Pair<Integer,Integer>,String>();
				while (senIter.hasNext()) {
					SortedMap<Pair<Integer,Integer>, StringBuilder> labelOffsetFVHm = 
							new TreeMap<Pair<Integer,Integer>, StringBuilder> ();
					senCounter++;
					//should we consider featureType inter-sentential
					Sentence sen=(Sentence) senIter.next();
					String senText = sen.getCoveredText();
					List<String> tokenList = Arrays.asList(senText.split(" "));
					Pattern pattern1 = Pattern.compile(">/=|</=|>=|<=|<|>");
					Matcher matcher1 = pattern1.matcher(senText);
					while(matcher1.find()){
						String found = matcher1.group();
						int start = sen.getBegin()+matcher1.start();
						int end = sen.getBegin()+matcher1.end();
						Pair<Integer,Integer> unitPair = new Pair<Integer,Integer>(start,end);
						unitPairHm.put(unitPair, found);
						System.out.println("found symbols: "+found);
					}
					//Pattern pattern2 = Pattern.compile("\\[0-9\\]+?\\.?[0-9]+[a-zA-Z]+/[a-zA-Z0-9]+|[0-9]+?\\.?[0-9]+ [a-zA-Z0-9]+ per[a-zA-Z0-9]+|[0-9]+?\\.?[0-9]+?\\.?\/[a-zA-Z0-9]+");
					Pattern pattern2 = Pattern.compile("\\d+?\\.?\\d+\\[a-zA-Z\\]+/\\w+|\\d+?\\.?\\d+ \\w+ per \\w+|\\d+?\\.?\\d+?\\.?/\\w+");
					Matcher matcher2 = pattern2.matcher(senText);
					while(matcher2.find()){
						String found = matcher2.group();
						int start = sen.getBegin()+matcher2.start();
						int end = sen.getBegin()+matcher2.end();
						Pair<Integer,Integer> unitPair = new Pair<Integer,Integer>(start,end);
						unitPairHm.put(unitPair, found);
						System.out.println("found numbers: "+found);
					}

					//					if($freetext[$a] =~ s/(>\/=|<\/=|>=|<=|<|>)/(print WRITE "$1,$-[0]\nunit\n")/eg) {		
					//						print "$1,$-[0]\nunit\n";
					//						#print WRITE "$1,$-[0]\nunit\n";
					//					}
					//
					//		if($freetext[$a] =~ s/([0-9]+?.?[0-9]+
					//		[a-zA-Z]+\/[a-zA-Z0-9]+|[0-9]+?.?[0-9]+ [a-zA-Z0-9]+ per
					//		[a-zA-Z0-9]+|[0-9]+?.?[0-9]+?.?\/[a-zA-Z0-9]+)/(print WRITE
					//		"$1,$-[0]\nsymbol\n")/eg) {		
					//						print "$1,$-[0]\nsymbol\n";
					//						#print WRITE "$1,$-[0]\nsymbol\n";
					//					}

					List<Triple<Integer,Integer,String>> contextTripleList = this.findContextRule(senText);
					//System.out.println("sentence number: "+sen.getSentenceNumber()+" begin: ");
					//locationMap = new HashMap<Pair<Integer,Integer>, BaseToken>();
					locationMap = new TreeMap<Pair<Integer,Integer>, BaseToken>();

					FSIterator baseTokenIter = tokenIndex.subiterator(sen);
					List<BaseToken> baseTokenList = new ArrayList<BaseToken>();
					List<ConceptMention> cmList = new ArrayList<ConceptMention>();
					//it is found the sentence may be detected wrongly. Then, the trouble comes
					//for example, 1. Metastatic renal cell carcinoma with metastatic disease to the brain and lungs . 
					//in discharge101.txt is detected as two sentences. namely, 1. is one sentence. 
					while (baseTokenIter.hasNext()){
						BaseToken bt = (BaseToken) baseTokenIter.next();						
						Pair<Integer,Integer> offsetPair = new Pair<Integer,Integer>(bt.getBegin(),bt.getEnd());
						//usually, offsetBegin should be equal to offsetEnd since token usually involves one string
						//but this may not be true for i2b2Token or shareToken. Then, we still need baseTokenList
						//since we need it for find the tokens from both sides of the window
						baseTokenList.add(bt);
						locationMap.put(offsetPair, bt);
					}

					FSIterator annotLabelIter = annotIndex.subiterator(sen);		
					int eventCounter = 0;

					//not sure if there are duplicated EventMention. I want to extract UMLS concepts as features
					SortedMap<Pair<Integer,Integer>, List<IdentifiedAnnotation>> ctakesEventHm = new TreeMap<Pair<Integer,Integer>, List<IdentifiedAnnotation>>();
					SortedMap<Pair<Integer,Integer>,IdentifiedAnnotation> hongnaEventMap = new TreeMap<Pair<Integer,Integer>,IdentifiedAnnotation>();
					while(annotLabelIter.hasNext()){
						IdentifiedAnnotation event = (IdentifiedAnnotation) annotLabelIter.next();
						//printStream.print(event.getAssertion()+" "+event.getCoveredText());
						//String phrase = event.getCoveredText(); phrase is the same as ctext
						//in fact, lineNum1 and lineNum2 should be identical
						//we should loop all annotLabelIter if we find that their line number is not identical
						//but note: it seems that we should add one to sentence number since it starts from 0
						//while beginLine starts from 1.
						int begin = event.getBegin();
						int end = event.getEnd();
						Pair<Integer,Integer> offsetPair = new Pair<Integer,Integer>(begin,end);
						if(event.getSegmentID()==null || !event.getSegmentID().contains("Hong Na Annotated")){
							List<IdentifiedAnnotation> listAnnot = new ArrayList<IdentifiedAnnotation>();
							if(ctakesEventHm.containsKey(offsetPair)){
								listAnnot=ctakesEventHm.get(offsetPair);
							}
							listAnnot.add(event);
							ctakesEventHm.put(offsetPair, listAnnot);
							continue;
						}
						hongnaEventMap.put(offsetPair, event);
					}
					String eventType = "tmp";
					this.genAnnotatedFV(labelOffsetFVHm, hongnaEventMap, ctakesEventHm, locationMap, senText, leftWindow, rightWindow, segText, segBegin, contextTripleList, type, inFile, cmList, seg, baseTokenList, name, eventType, unitPairHm);
					printStream.print("\n");
					pwIndFile.println("");
					//printStream.print("SS\n");
					//pwIndFile.println("SS");

					for(Map.Entry<Pair<Integer,Integer>, StringBuilder> fvEntry : labelOffsetFVHm.entrySet()){
						Pair<Integer,Integer> offsetPair = fvEntry.getKey();
						StringBuilder sb = fvEntry.getValue();
						if(!test_numbersList.contains(Integer.valueOf(docId.split("\\.")[0]))){
							printStream.print(sb);
						}
						pwIndFile.print(sb);
					}
					printStream.print("\n");
					pwIndFile.println("");
					//printStream.print("ES\n");
					//pwIndFile.println("ES");
				}
			}
		}

		finally {  

			if (out != null) {
				out.close();
				printStream.close();
				pwIndFile.flush();
				pwIndFile.close();
			}
		}
	}

	private void genAnnotatedFV(SortedMap<Pair<Integer,Integer>, StringBuilder> labelOffsetFVHm,
			SortedMap<Pair<Integer,Integer>,IdentifiedAnnotation> hongnaEventMap,
			SortedMap<Pair<Integer,Integer>, List<IdentifiedAnnotation>> ctakesEventHm,
			SortedMap<Pair<Integer,Integer>, BaseToken> locationMap,String senText,
			int leftWindow,int rightWindow,String segText,int segBegin,
			List<Triple<Integer,Integer,String>> contextTripleList, 
			String type, String inFile,List<ConceptMention> cmList,Segment seg,
			List<BaseToken> baseTokenList,File name,String eventType,
			HashMap<Pair<Integer,Integer>, String> unitPairHm){
		int eventCounter = 0;

		for(Map.Entry<Pair<Integer,Integer>, IdentifiedAnnotation> entry : hongnaEventMap.entrySet()){
			eventCounter++;
			StringBuilder sb = new StringBuilder();	
			Pair<Integer,Integer> offsetPair = entry.getKey();
			IdentifiedAnnotation event = (IdentifiedAnnotation) entry.getValue();
			int begin = event.getBegin();
			int end = event.getEnd();
			//for now, we just ignore those IdentifiedAnnotation without segment Id
			//since we only care about Hong Na's annotations for now. 
			//But we may consider other IdentifiedAnnotation later when we use others 
			//as features.
			if(event.getSegmentID()==null || !event.getSegmentID().contains("Hong Na Annotated")){
				continue;
			}
			String label = segText.substring(event.getBegin()-segBegin, event.getEnd()-segBegin).trim();
			String[] labelArr = label.split("\\s+");
			for(int ii=0;ii<labelArr.length;ii++){
				String ithLabel = labelArr[ii];
				if(ii==0){
					if(event instanceof AnatomicalSiteMention){
						sb.append("B-anatomical"+"\t");
					}else if(event instanceof LabInterpretationModifier){
						sb.append("B-labintermod"+"\t");
					}else if(event instanceof LabValueModifier){
						sb.append("B-labvalmod"+"\t");
					}else if(event instanceof LabReferenceRangeModifier){
						sb.append("B-labrefrangemod"+"\t");
					}else if(event instanceof MeasurementAnnotation){
						sb.append("B-measure"+"\t");
					}else if(event instanceof TimeMention){
						sb.append("B-time"+"\t");
					}
					else if(event instanceof EntityMention){
						sb.append("B-entity"+"\t");
					} 
					//we need to take demographics into considerations specially. 
					//					else if(event instanceof Demographics){
					//						printStream.print("B-labrefrangemod"+"\t");
					//					}
				}else{
					//let's just consider BIO now. May add more as BIEO or others
					if(event instanceof AnatomicalSiteMention){
						sb.append("I-anatomical"+"\t");
					}else if(event instanceof LabInterpretationModifier){
						sb.append("I-labintermod"+"\t");
					}else if(event instanceof LabValueModifier){
						sb.append("I-labvalmod"+"\t");
					}else if(event instanceof LabReferenceRangeModifier){
						sb.append("I-labrefrangemod"+"\t");
					}else if(event instanceof MeasurementAnnotation){
						sb.append("I-measure"+"\t");
					}else if(event instanceof TimeMention){
						sb.append("I-time"+"\t");
					}else if(event instanceof EntityMention){
						sb.append("I-entity"+"\t");
					}
				}

				for(int i=0;i<labelArr.length;i++){
					if(!featureIndHm.containsKey(labelArr[i])){
						featureIndHm.put(labelArr[i], featureIndHm.size());
					}
				}


				sb.append("\t"+ithLabel);
				//				System.out.println(inputNameNoExt + " " + endLine + ":" + begin + " " + endLine + ":" + end);
				//lda features are not used yet and the file name is therefore not existent yet.
				//				if(featureType.contains(FeatureCats.lda_docfeatures.toString())){
				//					//featureLDA = mapLDA.get(inputNameNoExt + ":" + begin + ":" + end);
				//					StringTokenizer stLDA = new StringTokenizer(featureLDA);
				//					int countLDA = 0;
				//					sbLDA.setLength(0);
				//					stLDA.nextToken(); //skip the label since the label is not part of the probability. There is no reason that we put them into LDA features, confusing.
				//					while(stLDA.hasMoreTokens()) {
				//						sbLDA.append("LDA").append(countLDA).append("=").append(stLDA.nextToken()).append("\t");
				//						countLDA ++;
				//					}
				//					featureLDA = sbLDA.toString();
				//				}

				//Pair<Integer,Integer> beginOffsetPair = new Pair<Integer,Integer>(beginLine,begin);
				//Pair<Integer,Integer> endOffsetPair = new Pair<Integer,Integer>(endLine,end);
				String phrase = event.getCoveredText();

				// Print the Label.
				//printStream.print("\""+inputNameNoExt+ " " + event.getCoveredText() + " ("+beginOffsetPair+") ("+endOffsetPair+")\" " +event.getAssertion()+"\t");
				//printStream.print(event.getAssertion()+"\t");

				// Print LDA features.
				//				if(featureType.contains(FeatureCats.lda_docfeatures.toString())){
				//					sb.append(featureLDA + "\t");
				//				}

				//pwIndFile.print("\""+phrase+"\"\t"+beginLine+":"+begin+"\t"+endLine+":"+end+"\t"+event.getAssertion() + "\t" + featureLDA + "\t");
				//pwIndFile.print(event.getAssertion()+" ");
				//the following aims at using BOW of the sentence directly. But it seems that this is not a good choice since
				//within one sentence, there are may be more than one assertion which may be opposite to each other.
				//therefore, unigram surrounding the concept should be a better choice.
				//						for(int j=0;j<tokenList.size();j++){
				//							printStream.print(tokenList.get(j)+" ");
				//							pwIndFile.print(tokenList.get(j)+" ");
				//						}

				if(featureType.contains("umls")){
					//this.genUmlsFeature(sb, ctakesEventHm, offsetPair);
					//if offsetPair is not included in ctakesEventHm, it does not mean the annotated label does not contain concept
					//it may mean that sub-phrases of the label
					List<Pair<Integer,Integer>> listOffset = new ArrayList<Pair<Integer,Integer>>();
					int singleBegin = offsetPair.getFirst() , singleEnd = offsetPair.getFirst();
					for(int i=0;i<label.length();i++){
						if((label.charAt(i)==' '||i==label.length()-1) && i>0){
							if(i==label.length()-1){
								singleEnd +=i+1;
							}else{
								singleEnd += i;	
							}
							Pair<Integer,Integer> sinOffsetPair = new Pair<Integer,Integer>(singleBegin,singleEnd);
							listOffset.add(sinOffsetPair);
							singleBegin = singleEnd+1;
							singleEnd = offsetPair.getFirst();
							System.out.println("i="+i+" "+sinOffsetPair);
							//this.genUmlsFeature(sb, ctakesEventHm, sinOffsetPair);
						}
					}
					for(int i=0;i<listOffset.size();i++){  
						Pair<Integer,Integer> ithSinOffsetPair = listOffset.get(i);
						for(int j=i;j<listOffset.size();j++){
							Pair<Integer,Integer> jthSinOffsetPair = listOffset.get(j);
							Pair<Integer,Integer> comOffsetPair = new Pair<Integer,Integer>(ithSinOffsetPair.getFirst(),jthSinOffsetPair.getSecond());
							this.genUmlsFeature(sb, ctakesEventHm, comOffsetPair);
						}
					}
				}

				if(featureType.contains("unit")){
					for(Map.Entry<Pair<Integer,Integer>, String> unitEntry : unitPairHm.entrySet()){
						Pair<Integer,Integer> unitPair = unitEntry.getKey();
						if(CompIntPair.spanCompare(offsetPair, unitPair)==OverlapType.exact 
								|| CompIntPair.spanCompare(offsetPair, unitPair)==OverlapType.overlap
								||CompIntPair.spanCompare(offsetPair, unitPair)==OverlapType.subset
								||CompIntPair.spanCompare(offsetPair, unitPair)==OverlapType.subsumes){
							String unitStr = unitEntry.getValue();
							//sb.append("\tunit"+unitStr);
							sb.append("\tunit");
						}
					}
				}

				int eventBegin = senText.indexOf(phrase);
				int eventEnd = eventBegin+phrase.length();
				Pair<Integer,Integer> eventSpan = new Pair<Integer,Integer>(eventBegin,eventEnd);
				int globalCounter = 0;
				for(int i=begin; i<end;i++){
					for(int j=i+1;j<=end;j++){
						Pair<Integer,Integer> endOffsetPair = new Pair<Integer,Integer>(i,j);
						logger.info("endOffsetPair="+endOffsetPair);
						if(endOffsetPair.getFirst()==127 && endOffsetPair.getSecond()==131){
							System.out.println("debug: ");
						}
						BaseToken bt = locationMap.get(endOffsetPair);
						int phraseInd = i-begin;
						if(bt!=null){
							if(phraseInd==0){
								//							if(event.getAssertion()==null){
								//								System.out.println(modelFileName);
								//							}
								//globalCounter aims at counting how many assertions. So, only when i==0, 
								//does it add.
								globalCounter++;
							}
							int phraseLen = end-begin+1;
							this.processBaseToken(sb, bt, type, inFile, cmList, eventType, seg, baseTokenList, name, leftWindow, rightWindow, phraseInd, phraseLen, globalCounter);
						}
					}
				}

				if(featureType.contains(FeatureCats.context.toString())){
					this.extractConextRule(sb,senText, eventSpan, contextTripleList,locationMap);
				}

				// Add windowed word unigram feature.
				if(featureType.contains(FeatureCats.unigram.toString()))
					this.windowWordNgram(sb,locationMap, 1, leftWindow, rightWindow, begin, end);

				// Add windowed word bigram feature.
				if(featureType.contains(FeatureCats.bigram.toString()))
					this.windowWordNgram(sb,locationMap, 2, leftWindow, rightWindow, begin, end);

				// Add windowed (token number) character-4-gram feature.
				if(featureType.contains(FeatureCats.char4gram.toString()))
					this.windowCharNgram(sb,locationMap,4, leftWindow, rightWindow, begin, end);
				// Add LDA feature.
				sb.append("\n");
			}

			labelOffsetFVHm.put(offsetPair, sb);
		}

		for(Map.Entry<Pair<Integer,Integer>, BaseToken> btEntry: locationMap.entrySet()){
			Pair<Integer,Integer> offsetPair = btEntry.getKey();
			//if not contained, it means that they are outside tokens.
			if(!labelOffsetFVHm.containsKey(offsetPair)){
				StringBuilder sb = new StringBuilder();	
				BaseToken bt = btEntry.getValue();
				sb.append("O\t");
				// Add windowed word unigram feature.
				sb.append(bt.getCoveredText());
				if(featureType.contains(FeatureCats.bigram.toString()))
					this.windowWordNgram(sb,locationMap, 2, leftWindow, rightWindow, bt.getBegin(), bt.getEnd());
				sb.append("\n");
				labelOffsetFVHm.put(offsetPair, sb);
			}
		}
	}

	private void genUmlsFeature(StringBuilder sb,SortedMap<Pair<Integer,Integer>, 
			List<IdentifiedAnnotation>> ctakesEventHm,
			Pair<Integer,Integer> offsetPair){
		List<IdentifiedAnnotation> idAnnotList = ctakesEventHm.get(offsetPair);
		if(idAnnotList!=null){
			boolean cuiAdded = false, tuiAdded=false;
			for(int i=0;i<idAnnotList.size();i++){
				IdentifiedAnnotation idAnnot = idAnnotList.get(i);
				FSArray umlsArr = idAnnot.getOntologyConceptArr();
				if(umlsArr!=null){
					for(int j=0;j<umlsArr.size();j++){
						UmlsConcept umlsCon = (UmlsConcept) umlsArr.get(j);
						String cui = umlsCon.getCui();
						String tui = umlsCon.getTui();
//						if(!cuiAdded){
//							sb.append("\t"+cui);
//							cuiAdded = true;
//						}

						if(!tuiAdded){
							sb.append("\t"+tui);
							tuiAdded = true;
						}
					}
				}
			}
		}
	}

	/**
	 * contextFuleHm is obtained by reading Sunhwan's context dictionary, which mainly involves the check
	 * of negation phrases, history phrases and so on. Thus, this is different from window features, which 
	 * includes unigram, bigram of neiboring words.
	 * @param sentence
	 * @return
	 */
	private List<Triple<Integer,Integer,String>> findContextRule(String sentence){
		List<Triple<Integer,Integer,String>> contextTripleList = new ArrayList<Triple<Integer,Integer,String>>();
		for(Map.Entry<String, String> entry : contextRuleHm.entrySet()){
			String context = entry.getKey();
			if(sentence.contains(context)){
				int contextBegin = sentence.indexOf(context);
				int contextEnd = contextBegin+context.length();
				Triple<Integer,Integer,String> contextTriple = new Triple<Integer,Integer,String>(contextBegin,contextEnd,context);
				contextTripleList.add(contextTriple);
			}
		}
		return contextTripleList;
	}

	private void extractConextRule(StringBuilder sb,String sentence,Pair<Integer,Integer> eventSpan, List<Triple<Integer,Integer,String>> contextTripleList,
			SortedMap<Pair<Integer,Integer>, BaseToken> locationMap){

		for(int i=0;i<contextTripleList.size();i++){
			//it seems that we may only need one context rule, not sure though. Just test it.
			Triple<Integer,Integer,String> contextTriple = contextTripleList.get(i);
			Pair<Integer,Integer> contextSpan = new Pair<Integer,Integer>(contextTriple.getFirst(),contextTriple.getSecond());
			//how to assign pseudo is a problem. I would think more. It seems that this is quite discrimative.
			String contextPhrase = contextTriple.getThird();
			String contextValue = contextRuleHm.get(contextPhrase);
			String[] contextPhraseArr = contextPhrase.split(" ");
			for(int j=0;j<contextPhraseArr.length;j++){
				if(j==0){
					//it means that only one token in this phrase
					if(contextPhraseArr.length==1){
						contextPhrase = "context-"+contextPhraseArr[j];
					}else{
						contextPhrase = "context-"+contextPhraseArr[j]+"-";
					}

				}else{
					if(j==contextPhraseArr.length-1){
						contextPhrase += contextPhraseArr[j];
					}else{
						contextPhrase += contextPhraseArr[j]+"-";
					}

				}

			}
			if(contextValue.contains("pseudo")){
				//writer.print(contextPhrase+"=pseudo ");
				//printStream.print(contextPhrase+"=pseudo ");
				sb.append("\tpseudo-"+i+"="+contextPhrase);
			}
			if(CompIntPair.spanCompare(eventSpan, contextSpan)==OverlapType.leftseparate){
				if(contextTriple.getSecond()==sentence.length()-1){
					//					writer.print(contextPhrase+"=righttermin ");
					//					printStream.print(contextPhrase+"=righttermin ");
					sb.append("\trighttermin-"+i+"="+contextPhrase);
				}else{
					sb.append("\tpostcontext-"+i+"="+contextPhrase);
				}

			}else if(CompIntPair.spanCompare(eventSpan, contextSpan)==OverlapType.rightseparate){
				if(contextTriple.getSecond()==0){
					sb.append("\tlefttermin-"+i+"="+contextPhrase);
				}else{
					sb.append("\tbeforecontext-"+i+"="+contextPhrase);
				}
			}
			contextStrHm.put(contextPhrase, contextStrHm.size());
		}
	}

	public void processBaseToken(StringBuilder sb, BaseToken bt, String type, String inFile,
			List<ConceptMention> cmList,String eventType,Segment seg,
			List<BaseToken> baseTokenList, File name,  int leftWindow, 
			int rightWindow, int phraseInd, int phraseLen, int globalCounter){
		if(bt!=null){
			//this is usual case since concept should be within one sentence instead of cross two or more sentences
			//however, we'd better check.
			//tokenNumber like 29:4 29:4, if subtracted, would be 0. But in fact, there should be one.
			//int tokenNumber = begin-end+1;
			int gramType = 1;
			String ctext=bt.getCoveredText().trim();
			int ii = baseTokenList.indexOf(bt);
			if(ctext.equals(":")) ctext="PUNC_COLON";
			if(ctext.equals("\\")) ctext="PUNC_SLASH";
			if(ctext.equals("=")) ctext="equal_symbol";
			//it seems that getAnnotString is not so helpful for now since its type I set for the event is always the same
			//comment it now.
			//printStream.print(getAnnotString(bt,type, eventType, phraseInd, gramType));
			//pwIndFile.print(getAnnotString(bt,type, eventType,phraseInd, gramType));
			//ctext should not be so useful for assertion detection though it may be useful
			if(this.featureType.contains(FeatureCats.conToken.toString())){
				sb.append("\tctext-"+phraseInd+"="+ctext);
			}

			if(phraseInd==0 && this.featureType.contains(FeatureCats.document.toString())){
				sb.append("\tsectonHeader-"+phraseInd+"="+seg.getId());
			}

			if(this.featureType.contains(FeatureCats.concept.toString())){
				sb.append(getCMString(bt, cmList,phraseInd));
			}

			if(this.featureType.contains("numToken") && (bt instanceof NumToken)){
				sb.append("\tnum");
			}

			wordStrHm.put(ctext.trim(),wordStrHm.size());
			//when ii==0, window = 4, j starts from -4, ws = -4-0=-4, 
			//next j = -3
			for(int j=ii-leftWindow; j<=ii+rightWindow; j++ ){
				int ws=j-ii;
				if(j>=0 && j< baseTokenList.size()){
					if(phraseInd==0||phraseInd==phraseLen-1){
						//System.out.println(phraseInd+" phrase length: "+phraseLen);
						sb.append(getWindowTokenString((BaseToken) baseTokenList.get(j), ws, gramType));	 
						if(globalCounter==3001 || globalCounter==3000){
							System.out.print(getWindowTokenString((BaseToken) baseTokenList.get(j), ws, gramType));
						}
					}
				}
				else if(j<0 && phraseInd==0){
					// ws < 0
					if(featureType.contains(FeatureCats.uniNorm.toString())){
						sb.append("\tuniTok|"+gramType+ws+"|=SSSSS");
						if(globalCounter==3001 || globalCounter==3000){
							System.out.print("\tuniTok|"+gramType+ws+"|=SSSSS");
						}
					}

				}
				else{
					// ws >= 0
					if(featureType.contains(FeatureCats.uniNorm.toString())){
						sb.append("\tuniTok|"+gramType+"+"+ws+"|=EEEEE");
						if(globalCounter==3001 || globalCounter==3000){
							System.out.print("\tuniTok|"+gramType+"+"+ws+"|=EEEE");
						}
					}
				}
			}

			//			if(featureType.contains("bigram")){
			//				this.processNgramWindow(bt, printStream, pwIndFile, baseTokenList, 2, leftWindow, rightWindow, phraseInd, phraseLen);
			//			}
		}
	}

	/**
	 * getWindowTokenString
	 * @param bt
	 * @param ws
	 * @param gramType
	 * @return
	 */
	private String getWindowTokenString(BaseToken bt, int ws, int gramType){
		String featStr="";
		if(bt.getClass().equals(shareToken.class) || bt.getClass().equals(i2b2Token.class) || bt.getClass().equals(WordToken.class)){ 
			WordToken wordToken=(WordToken) bt;
			String norm=wordToken.getCanonicalForm();
			if(norm==null) 
				norm=wordToken.getCoveredText().toLowerCase().trim();
			else if(norm.trim().equals("="))
				norm="equal_symbol";
			else
				norm=norm.trim();
			normStrHm.put(norm, normStrHm.size());

			String ctext=wordToken.getCoveredText().trim();
			if(ctext.equals("=")){
				ctext = "equal_symbol";
			}
			if(ctext.equals(":")) {ctext="PUNC_COLON"; norm=ctext;}
			if(ctext.equals("\\")) {ctext="PUNC_SLASH"; norm=ctext;}
			String index = gramType + ((ws < 0) ? "-" : "+") + Math.abs(ws);
			//windows refers to tokens surrounding the concepts
			if(this.featureType.contains(FeatureCats.uniTok.toString())){
				featStr+="\tuniTok|"+index+"|="+ctext;
			}

			if(this.featureType.contains(FeatureCats.uniNorm.toString())){
				//featStr+="\tuniNorm["+index+"]="+norm;
				if(!stopList.contains(norm)){
					if(norm.contains("[") || norm.contains("]")) {
						norm = norm.replaceAll("\\[", "");
						norm = norm.replaceAll("\\]", "");
					}
					featStr+="\t"+norm;
				}
			}

			if(this.featureType.contains(FeatureCats.posTagging.toString())){
				featStr+="\tpos|"+index+"|="+wordToken.getPartOfSpeech().trim();
			}

			if(this.featureType.contains(FeatureCats.orthography.toString())){
				featStr+="\tcapital|"+index+"|="+wordToken.getCapitalization()
						+getOrthographyFeatures(ctext, ws, 1);
			}

			if(this.featureType.contains(FeatureCats.affix.toString())){
				featStr+= getPrefixAndSuffix(ctext, ws, 1);
			}
			wordStrHm.put(ctext.trim(),wordStrHm.size());
		}
		return featStr.replaceAll("  "," ");
	}

	/**
	 * concept mention features
	 * @param bt
	 * @param cmList
	 * @param gramType
	 * @return
	 */
	private String getCMString(BaseToken bt, List<ConceptMention> cmList, int gramType){
		String msg="";
		for(int j=0; j<cmList.size(); j++) {
			ConceptMention cm=(ConceptMention) cmList.get(j);
			if (bt.getBegin()==cm.getBegin()){
				msg+="\tcer-"+gramType+"="+cm.getCer() +"\tsemGrp-"+gramType+"="+cm.getSemG()+"\tsemText-"+gramType+"="+cm.getNorm().replaceAll(" ","_");
				msg+= "\tsemGrpBI-"+gramType+"="+cm.getSemG()+"_B";
				cmStrHm.put(cm.getNorm().replaceAll(" ","_"), cmStrHm.size());
			}
			else if (bt.getBegin()>cm.getBegin() && bt.getEnd()<=cm.getEnd() ){
				msg+="\tcer-"+gramType+"="+cm.getCer() +"\tsemGrp-"+gramType+"="+cm.getSemG()+"\tsemText-"+gramType+"="+cm.getNorm().replaceAll(" ","_");
				msg+="\tsemGrpBI-"+gramType+"="+cm.getSemG()+"_I";
				cmStrHm.put(cm.getNorm().replaceAll(" ","_"), cmStrHm.size());
			}
		}

		return msg;
	}

	private String getOrthographyFeatures(String text, int ws, int gramType){
		String str="";
		int count = 0;
		str+=getRegexMatch(text, ws, "ALPHA", Pattern.compile("^[A-Za-z]+$"),gramType,count++);
		str+=getRegexMatch(text, ws, "INITCAPS", Pattern.compile("^[A-Z].*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "UPPER-LOWER", Pattern.compile("^[A-Z][a-z].*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "LOWER-UPPER", Pattern.compile("^[a-z]+[A-Z]+.*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "ALLCAPS", Pattern.compile("^[A-Z]+$"),gramType,count++);
		str+=getRegexMatch(text, ws, "MIXEDCAPS", Pattern.compile("^[A-Z][a-z]+[A-Z][A-Za-z]*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "SINGLECHAR", Pattern.compile("^[A-Za-z]$"),gramType,count++);
		str+=getRegexMatch(text, ws, "SINGLEDIGIT", Pattern.compile("^[0-9]$"),gramType,count++);
		str+=getRegexMatch(text, ws, "NUMBER", Pattern.compile("^[0-9,]+$"),gramType,count++);
		str+=getRegexMatch(text, ws, "HASDIGIT", Pattern.compile("^.*[0-9].*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "ALPHANUMERIC", Pattern.compile("^.*[0-9].*[A-Za-z].*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "ALPHANUMERIC", Pattern.compile("^.*[A-Za-z].*[0-9].*$"),gramType,count++);
		str+=getRegexMatch(text, ws, "NUMBERS_LETTERS", Pattern.compile("^[0-9]+[A-Za-z]+$"),gramType,count++);
		str+=getRegexMatch(text, ws, "LETTERS_NUMBERS", Pattern.compile("^[A-Za-z]+[0-9]+$"),gramType,count++);
		str+=getRegexMatch(text, ws, "ROMAN", Pattern.compile("^[IVXDLCM]+$",Pattern.CASE_INSENSITIVE),gramType,count++);
		str+=getRegexMatch(text, ws, "GREEK", Pattern.compile("^(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)$"),gramType,count++);
		str+=getRegexMatch(text, ws, "ISPUNCT", Pattern.compile("^[`~!@#$%^&*()-=_+\\[\\]\\\\{}|;\':\\\",./<>?]+$"),gramType,count++);
		return str;
	}

	private String getRegexMatch(String str, int ws, String strClass , Pattern p, int gramType,int count){
		Matcher m = p.matcher(str);
		String index = gramType + ((ws < 0) ? "-" : "+") + Math.abs(ws);
		if (m.matches()) {return "\tSTCLS|"+index+"-"+count+"|="+strClass;}
		else { return "";}
	}

	private String getPrefixAndSuffix(String txt, int ws, int gramType){
		if( txt.length()>=3){
			String index = gramType + ((ws < 0) ? "-" : "+") + Math.abs(ws);
			txt = txt.replace("\n", "").replace("\r", "");
			return "\tPRE|"+index+"|="+txt.substring(0,3).trim()+"\tSUF|"+index+"|="+txt.substring(txt.length()-3,txt.length()).trim()+"\t";
		}
		return "";
	}

	/**
	 * Process n-gram within windows.
	 * @param bt
	 * @param printStream
	 * @param pwIndFile
	 * @param baseTokenList
	 * @param nGram
	 * @param leftWindow
	 * @param rightWindow
	 * @param phraseInd, the relative index for concept mention.
	 * @param phraseLen
	 */
	public void processNgramWindow(BaseToken bt, PrintStream printStream, PrintWriter pwIndFile, List<BaseToken> baseTokenList, int nGram, int leftWindow, int rightWindow, int phraseInd, int phraseLen) {
		int ii = baseTokenList.indexOf(bt);
		ArrayList<BaseToken> al = null;
		// ii now is the index of the first token of concept mention.
		if(phraseInd == 0) {
			al = new ArrayList<BaseToken>();
			for(int j = ii - leftWindow; j < ii; j ++) {
				if(j < 0) continue;
				al.add(baseTokenList.get(j));
			}
			int length = al.size();
			// no n-gram
			if(length < nGram) {
				for(int k = 0; k < al.size(); k ++) {
					String temp = "\t"+nGram+"gramTok|"+ (k - length) + "|=SSSSS";
					printStream.print(temp);
					pwIndFile.print(temp);
				}
			}
			else {
				//for example, in the sentence
				//The patient developed the syndrome after her thoracic aortic aneurysm repair .
				//where "the syndrome" is the concept, the bigrams on the left include "the patient, patient developed"
				//then, al adds three words: "the, patient, developed". Loop k, k=0, nGram-k = 2, 
				//when k=1, al.get(l)
				//al.size=3
				for(int k = 0; k < al.size() - nGram + 1; k ++) {
					//String temp = "\t"+nGram+"gramTok["+ (k - length+1) + "]=";
					String temp = "\t";
					for(int l = k; l < k+nGram; l ++) {
						temp += al.get(l).getCoveredText().trim() + "_";
					}
					temp.replaceAll("[", "");
					temp.replaceAll("]", "");
					printStream.print(temp.substring(0, temp.length() - 1));
					pwIndFile.print(temp.substring(0, temp.length() - 1));
				}
			}

		}
		// ii now is the index of the last token of a concept mention, hence the right window should start from ii + 1.
		else if(phraseInd == phraseLen - 1) {
			al = new ArrayList<BaseToken>();
			//the same example, j=5 since ii=4 which is the index of syndrome. Then, al has {after, her, thoracic, aortic, aneurysm}
			for(int j = ii + 1; j <= ii + rightWindow; j ++) {
				if(j > baseTokenList.size() - 1) continue;
				al.add(baseTokenList.get(j));
			}
			int length = al.size();
			if(length < nGram) {
				for(int k = 0; k < al.size(); k ++) {
					String temp = "\t"+nGram+"gramTok|"+ (length - k) +"|=EEEEE";
					temp.replaceAll("[", "");
					temp.replaceAll("]", "");
					printStream.print(temp);
					pwIndFile.print(temp);
				}
			}
			else {
				//k starts from 0 and would be end at 3 if nGram = 2 since al usually has size 4.
				for(int k = 0; k < al.size() - nGram + 1; k ++) {
					//String temp = "\t"+nGram+"gramTok["+  (k+1) + "]=";
					String temp = "\t";
					//when k ==2, l=2, nGram+1 = 2
					for(int l = k; l < k+nGram; l ++) {
						temp += al.get(l).getCoveredText().trim() + "_";
					}
					temp.replaceAll("[", "");
					temp.replaceAll("]", "");
					printStream.print(temp.substring(0, temp.length() - 1));
					pwIndFile.print(temp.substring(0, temp.length() - 1));
				}
			}
		}
	}

	/**
	 * In assertion data, begin and end are the token number of each line. But now for eligibility criteria, begin and end are bytes.
	 * @param printStream
	 * @param pwIndFile
	 * @param baseTokenList
	 * @param nGram
	 * @param leftWindow
	 * @param rightWindow
	 * @param begin
	 * @param end
	 */
	public void windowWordNgram(StringBuilder sb, SortedMap<Pair<Integer,Integer>, BaseToken> locationMap, int nGram, int leftWindow, int rightWindow, int begin, int end) {
		ArrayList<String> al;
		ArrayList<String> alWord = new ArrayList<String>();
		String temp = "";
		al = new ArrayList<String>();
		List<Pair<Integer,Integer>> offsetList = new ArrayList<Pair<Integer,Integer>>(locationMap.keySet());
		Pair<Integer,Integer> tokenNumPair = this.findTokenNumPair(offsetList, begin, end);
		logger.info("tokenNumPair in windowWordNgram="+tokenNumPair+" begin="+begin+" end="+end);
		if(tokenNumPair.getFirst()>=0){
			Pair<Integer,Integer> beginOffsetPair = offsetList.get(tokenNumPair.getFirst());
			//			int curStartInd = beginOffsetPair.getFirst();
			//			int curEndInd = beginOffsetPair.getSecond();
			int curStartInd = tokenNumPair.getFirst();

			for(int i = curStartInd - leftWindow; i < curStartInd; i ++) {
				if(i < 0) continue;
				Pair<Integer,Integer> includedOffset = offsetList.get(i);
				temp = locationMap.get(includedOffset).getCoveredText().trim();
				temp = temp.replaceAll("\\[", "");
				temp = temp.replaceAll("\\]", "");
				al.add(temp);
			}
			alWord = this.wordNgram(al, nGram);
		}

		if(tokenNumPair.getSecond()>=0){
			int curEndInd = tokenNumPair.getSecond();
			Pair<Integer,Integer> endOffsetPair = offsetList.get(tokenNumPair.getSecond());
			al = new ArrayList<String>();
			for(int i = curEndInd + 1; i < curEndInd + rightWindow + 1; i ++) {
				if(i > offsetList.size() - 1) continue;
				Pair<Integer,Integer> includedOffset = offsetList.get(i);
				temp = locationMap.get(includedOffset).getCoveredText().trim();
				temp = temp.replaceAll("\\[", "");
				temp = temp.replaceAll("\\]", "");
				al.add(temp);
			}
			alWord.addAll(this.wordNgram(al, nGram));
		}
		for(int i = 0; i < alWord.size(); i ++) {
			sb.append(" ").append(alWord.get(i));
			if(!featureIndHm.containsKey(alWord.get(i))){
				featureIndHm.put(alWord.get(i), featureIndHm.size());
			}
		}
	}


	public Pair<Integer,Integer> findTokenNumPair(List<Pair<Integer,Integer>> offsetList,int begin,int end){
		int phraseBeginInd = -1, phraseEndInd = -1;;

		int counter = 0;
		for(Pair<Integer,Integer> offsetPair : offsetList){
			if(offsetPair.getFirst()==begin){
				phraseBeginInd = counter; 
			}
			if(offsetPair.getSecond()==end){
				phraseEndInd = counter;
			}
			counter++;
		}
		return new Pair<Integer,Integer>(phraseBeginInd,phraseEndInd);
	}

	public void windowCharNgram(StringBuilder sb, SortedMap<Pair<Integer,Integer>, BaseToken> locationMap, int nGram, int leftWindow, int rightWindow, int begin, int end) {
		ArrayList<String> al;
		ArrayList<String> alChar = new ArrayList<String>();
		String temp = "";
		al = new ArrayList<String>();
		List<Pair<Integer,Integer>> offsetList = new ArrayList<Pair<Integer,Integer>>(locationMap.keySet());
		Pair<Integer,Integer> tokenNumPair = this.findTokenNumPair(offsetList, begin, end);
		if(tokenNumPair.getFirst()>=0){
			int curStartInd = tokenNumPair.getFirst();

			for(int i = curStartInd - leftWindow; i < curStartInd; i ++) {
				if(i < 0) continue;
				Pair<Integer,Integer> includedOffset = offsetList.get(i);
				temp = locationMap.get(includedOffset).getCoveredText().trim();
				temp = temp.replaceAll("\\[", "");
				temp = temp.replaceAll("\\]", "");
				al.add(temp);
			}
			alChar = this.charNgram(al, nGram);
		}

		al = new ArrayList<String>();

		if(tokenNumPair.getSecond()>=0){
			int curEndInd = tokenNumPair.getSecond();
			Pair<Integer,Integer> endOffsetPair = offsetList.get(tokenNumPair.getSecond());
			al = new ArrayList<String>();
			for(int i = curEndInd + 1; i < curEndInd + rightWindow + 1; i ++) {
				if(i > offsetList.size() - 1) continue;
				Pair<Integer,Integer> includedOffset = offsetList.get(i);
				temp = locationMap.get(includedOffset).getCoveredText().trim();
				temp = temp.replaceAll("\\[", "");
				temp = temp.replaceAll("\\]", "");
				al.add(temp);
			}
			alChar.addAll(this.charNgram(al, nGram));
		}

		for(int i = 0; i < alChar.size(); i ++) {
			sb.append("\t").append(alChar.get(i));
		}
	}

	/******************
	 * @author Ning Xia
	 ******************
	 * 
	 * Word n-gram feature.
	 * 
	 * @param printStream
	 * @param pwIndFile
	 * @param baseTokenList
	 * @param nGram
	 */
	public ArrayList<String> wordNgram(ArrayList<String> windowList, int nGram) {
		int sentenceLength = windowList.size();
		StringBuffer sb = new StringBuffer();
		ArrayList<String> al = new ArrayList<String>();
		if(sentenceLength >= nGram) {
			for(int i = 0; i < sentenceLength - nGram + 1; i ++) {
				// clear previous string buffer
				sb.setLength(0);
				for(int j = i; j < i + nGram; j ++) {
					sb.append(windowList.get(j)).append((j == i + nGram - 1) ? "" : "_");
				}
				al.add(sb.toString());
			}
		}
		return al;
	}

	/******************
	 * @author Ning Xia
	 * ****************
	 * 
	 * Character n-gram feature.
	 * 
	 * @param printStream
	 * @param pwIndFile
	 * @param baseTokenList
	 * @param nGram
	 */
	public ArrayList<String> charNgram(ArrayList<String> windowList, int nGram) {
		ArrayList<String> al = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < windowList.size(); i ++) {
			sb.append(windowList.get(i)).append((i == windowList.size() - 1) ? "" : "_");
		}
		String window = sb.toString();
		if(window.length() >= nGram) {
			for(int i = 0; i < window.length() - nGram + 1; i ++) {
				sb.setLength(0);
				for(int j = i; j < i + nGram; j ++) {
					sb.append(window.charAt(j));
				}
				al.add(sb.toString());
			}
		}
		return al;
	}

	/******************
	 * @author Ning Xia
	 * ****************
	 * 
	 * Sentence length feature.
	 * 
	 * @param printStream
	 * @param pwIndFile
	 * @param baseTokenList
	 * @param averageSentenceLength
	 */
	public void sentenceLength(PrintStream printStream, PrintWriter pwIndFile, List<BaseToken> baseTokenList, int averageSentenceLength) {
		if(baseTokenList.size() > averageSentenceLength) {
			printStream.print("\tsentence=long");
			pwIndFile.print("\tsentence=long");
		}
		else if(baseTokenList.size() == averageSentenceLength) {
			printStream.print("\tsentence=average");
			pwIndFile.print("\tsentence=average");
		}
		else {
			printStream.print("\tsentence=short");
			pwIndFile.print("\tsentence=short");
		}
	}
}
