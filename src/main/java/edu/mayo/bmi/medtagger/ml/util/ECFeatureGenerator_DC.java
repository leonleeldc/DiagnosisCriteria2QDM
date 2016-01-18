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
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import edu.mayo.bmi.medtagger.ml.crfsuite.CRFSuiteWrapper;
import edu.mayo.bmi.medtagger.ml.type.i2b2Token;
import edu.mayo.bmi.medtagger.ml.type.shareToken;
import edu.mayo.bmi.medtagger.type.ConceptMention;
import edu.mayo.bmi.medtagger.util.CompIntPair;
import edu.mayo.bmi.medtagger.util.FileProcessor;
import edu.mayo.bmi.medtagger.util.OverlapType;
import edu.mayo.bmi.medtagger.util.Pair;
import edu.mayo.bmi.medtagger.util.Triple;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.structured.Demographics;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabInterpretationModifier;
import org.apache.ctakes.typesystem.type.textsem.LabReferenceRangeModifier;
import org.apache.ctakes.typesystem.type.textsem.LabValueModifier;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;


public class ECFeatureGenerator_DC {

	CRFSuiteWrapper csw=null; 
	File modelFile=null;

	SortedMap<String,Integer> wordStrHm;
	SortedMap<String,Integer> normStrHm;
	SortedMap<String,Integer> cmStrHm;
	SortedMap<String,Integer> contextStrHm;
	HashMap<String,String> contextRuleHm;
	List<String> stopList;
	String featureType;
	private Logger logger = Logger.getLogger(getClass().getName());

	public ECFeatureGenerator_DC(SortedMap<String,Integer> wordStrHm,
			SortedMap<String,Integer> normStrHm,SortedMap<String,Integer> cmStrHm,
			SortedMap<String,Integer> contextStrHm, String featureType) throws Exception{
		this.wordStrHm = wordStrHm;
		this.normStrHm = normStrHm;
		this.cmStrHm = cmStrHm;
		this.contextStrHm = contextStrHm;
		this.featureType = featureType;
		contextRuleHm = new HashMap<String,String>();
		stopList = new ArrayList<String>();
	}
	public ECFeatureGenerator_DC(File modelfile, String csh) throws Exception{	
		csw=new CRFSuiteWrapper(csh);
		modelFile=modelfile;
		contextRuleHm = new HashMap<String,String>();
	}

	@SuppressWarnings("rawtypes")
	public int generateTrainFeatureLabel(JCas aJCas, String type, String inFile, String modelFileName, File name,  int leftWindow, int rightWindow,int globalCounter, HashMap<String, String> mapLDA) throws Exception {

		
		int localCounter = 0;
		String delimiter = "\\|";
		File contextFile = new File("resources/medtaggerresources/context/contextRule.txt");
		contextRuleHm = FileProcessor.readFile(contextFile, delimiter);
		File stopListFile = new File("resources/medtaggerresources/stoplist_less.txt");//the stoplist is empty for now.
		stopList = FileProcessor.readSimpleFile(stopListFile);
		FileOutputStream out = null;
		//the following aim at generating individual files for featureType so that we can convert the final outputs back to i2b2format easily.

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
			}else return 0;
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
				while (senIter.hasNext()) {
					senCounter++;
					//should we consider featureType inter-sentential
					Sentence sen=(Sentence) senIter.next();
					String coveredText = sen.getCoveredText();
					List<String> tokenList = Arrays.asList(coveredText.split(" "));
					List<Triple<Integer,Integer,String>> contextTripleList = this.findContextRule(coveredText);
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
					
					SortedMap<Pair<Integer,Integer>, IdentifiedAnnotation> eventHm = new TreeMap<Pair<Integer,Integer>, IdentifiedAnnotation>();
					SortedMap<Pair<Integer,Integer>,IdentifiedAnnotation> sortedEventMap = new TreeMap<Pair<Integer,Integer>,IdentifiedAnnotation>();
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
						sortedEventMap.put(offsetPair, event);
					}
					
					for(Map.Entry<Pair<Integer,Integer>, IdentifiedAnnotation> entry : sortedEventMap.entrySet()){
						eventCounter++;
						
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
								if(event instanceof EntityMention){
									printStream.print("B-entity"+"\t");
								}else if(event instanceof LabInterpretationModifier){
									printStream.print("B-labintermod"+"\t");
								}else if(event instanceof LabValueModifier){
									printStream.print("B-labvalmod"+"\t");
								}else if(event instanceof LabReferenceRangeModifier){
									printStream.print("B-labrefrangemod"+"\t");
								}else if(event instanceof MeasurementAnnotation){
									printStream.print("B-measure"+"\t");
								}else if(event instanceof AnatomicalSiteMention){
									printStream.print("B-anatomical"+"\t");
								}
								//we need to take demographics into considerations specially. 
//								else if(event instanceof Demographics){
//									printStream.print("B-labrefrangemod"+"\t");
//								}
							}else{
								//let's just consider BIO now. May add more as BIEO or others
								if(event instanceof EntityMention){
									printStream.print("I-entity"+"\t");
								}else if(event instanceof LabInterpretationModifier){
									printStream.print("I-labintermod"+"\t");
								}else if(event instanceof LabValueModifier){
									printStream.print("I-labvalmod"+"\t");
								}else if(event instanceof LabReferenceRangeModifier){
									printStream.print("I-labrefrangemod"+"\t");
								}else if(event instanceof MeasurementAnnotation){
									printStream.print("I-measure"+"\t");
								}else if(event instanceof AnatomicalSiteMention){
									printStream.print("I-anatomical"+"\t");
								}
							}
							printStream.print(label+"\t");
							pwIndFile.print(label+"\t");
//							System.out.println(inputNameNoExt + " " + endLine + ":" + begin + " " + endLine + ":" + end);
							//lda features are not used yet and the file name is therefore not existent yet.
							if(featureType.contains(FeatureCats.lda_docfeatures.toString())){
								//featureLDA = mapLDA.get(inputNameNoExt + ":" + begin + ":" + end);
								StringTokenizer stLDA = new StringTokenizer(featureLDA);
								int countLDA = 0;
								sbLDA.setLength(0);
								stLDA.nextToken(); //skip the label since the label is not part of the probability. There is no reason that we put them into LDA features, confusing.
								while(stLDA.hasMoreTokens()) {
									sbLDA.append("LDA").append(countLDA).append("=").append(stLDA.nextToken()).append("\t");
									countLDA ++;
								}
								featureLDA = sbLDA.toString();
							}
							
							//Pair<Integer,Integer> beginOffsetPair = new Pair<Integer,Integer>(beginLine,begin);
							//Pair<Integer,Integer> endOffsetPair = new Pair<Integer,Integer>(endLine,end);
							String phrase = event.getCoveredText();
							
							// Print the Label.
							//printStream.print("\""+inputNameNoExt+ " " + event.getCoveredText() + " ("+beginOffsetPair+") ("+endOffsetPair+")\" " +event.getAssertion()+"\t");
							//printStream.print(event.getAssertion()+"\t");
							
							// Print LDA features.
							if(featureType.contains(FeatureCats.lda_docfeatures.toString())){
								printStream.print(featureLDA + "\t");
							}
							
							//pwIndFile.print("\""+phrase+"\"\t"+beginLine+":"+begin+"\t"+endLine+":"+end+"\t"+event.getAssertion() + "\t" + featureLDA + "\t");
							//pwIndFile.print(event.getAssertion()+" ");
							//the following aims at using BOW of the sentence directly. But it seems that this is not a good choice since
							//within one sentence, there are may be more than one assertion which may be opposite to each other.
							//therefore, unigram surrounding the concept should be a better choice.
							//						for(int j=0;j<tokenList.size();j++){
							//							printStream.print(tokenList.get(j)+" ");
							//							pwIndFile.print(tokenList.get(j)+" ");
							//						}
							int eventBegin = coveredText.indexOf(phrase);
							int eventEnd = eventBegin+phrase.length();
							Pair<Integer,Integer> eventSpan = new Pair<Integer,Integer>(eventBegin,eventEnd);
							
							for(int i=begin+1; i<=end;i++){
								Pair<Integer,Integer> endOffsetPair = new Pair<Integer,Integer>(i-1,i);
								BaseToken bt = locationMap.get(endOffsetPair);
								int phraseInd = i-begin;
								if(bt!=null){
									if(phraseInd==0){
//										if(event.getAssertion()==null){
//											System.out.println(modelFileName);
//										}
										//globalCounter aims at counting how many assertions. So, only when i==0, 
										//does it add.
										globalCounter++;
									}
									int phraseLen = end-begin+1;
									String eventType = "tmp";
									this.processBaseToken(bt, printStream,pwIndFile, type, inFile, cmList, eventType, 
											seg, baseTokenList, name, leftWindow, rightWindow, phraseInd, phraseLen,globalCounter);


									if(i==end){
										localCounter++;
//										printStream.print("\n");
//										pwIndFile.println();
									}
								}
							}

							if(featureType.contains(FeatureCats.context.toString())){
								this.printContextRule(coveredText, eventSpan, contextTripleList, pwIndFile, printStream);
							}
							
							// Add windowed word unigram feature.
							if(featureType.contains(FeatureCats.unigram.toString()))
								this.windowWordNgram(printStream, pwIndFile, locationMap, 1, leftWindow, rightWindow, begin, end);
							
							// Add windowed word bigram feature.
							if(featureType.contains(FeatureCats.bigram.toString()))
								this.windowWordNgram(printStream, pwIndFile, locationMap, 2, leftWindow, rightWindow, begin, end);
							
							// Add windowed (token number) character-4-gram feature.
//							if(featureType.contains(FeatureCats.char4gram.toString()))
//								this.windowCharNgram(printStream, pwIndFile, baseTokenList, 4, leftWindow, rightWindow, begin, end);
							// Add LDA feature.
							//printStream.print("\n");
							//pwIndFile.println();
						}
					}
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
		//System.out.println("localCounter for ast: "+localCounter+" file name: "+modelFileName);
		return globalCounter;
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

	private void printContextRule(String sentence,Pair<Integer,Integer> eventSpan, List<Triple<Integer,Integer,String>> contextTripleList,PrintWriter writer,PrintStream printStream){

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
				writer.print("\tpseudo-"+i+"="+contextPhrase);
				printStream.print("\tpseudo-"+i+"="+contextPhrase);
			}
			if(CompIntPair.spanCompare(eventSpan, contextSpan)==OverlapType.leftseparate){
				if(contextTriple.getSecond()==sentence.length()-1){
					//					writer.print(contextPhrase+"=righttermin ");
					//					printStream.print(contextPhrase+"=righttermin ");
					writer.print("\trighttermin-"+i+"="+contextPhrase);
					printStream.print("\trighttermin-"+i+"="+contextPhrase);
				}else{
					writer.print("\tpostcontext-"+i+"="+contextPhrase);
					printStream.print("\tpostcontext-"+i+"="+contextPhrase);
				}

			}else if(CompIntPair.spanCompare(eventSpan, contextSpan)==OverlapType.rightseparate){
				if(contextTriple.getSecond()==0){
					writer.print("\tlefttermin-"+i+"="+contextPhrase);
					printStream.print("\tlefttermin-"+i+"="+contextPhrase);
				}else{
					writer.print("\tbeforecontext-"+i+"="+contextPhrase);
					printStream.print("\tbeforecontext-"+i+"="+contextPhrase);
				}
			}
			contextStrHm.put(contextPhrase, contextStrHm.size());
		}
	}

	public void processBaseToken(BaseToken bt,PrintStream printStream,PrintWriter pwIndFile, String type, String inFile,
			List<ConceptMention> cmList,String eventType,Segment seg,
			List<BaseToken> baseTokenList, File name,  int leftWindow, int rightWindow, int phraseInd, int phraseLen, int globalCounter){
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
				printStream.print("\tctext-"+phraseInd+"="+ctext);
				pwIndFile.print("\tctext-"+phraseInd+"="+ctext);
			}

			if(phraseInd==0 && this.featureType.contains(FeatureCats.document.toString())){
				printStream.print("\tsectionHeader-"+phraseInd+"="+seg.getId());
				pwIndFile.print("\tsectionHeader-"+phraseInd+"="+seg.getId());
			}

			if(this.featureType.contains(FeatureCats.concept.toString())){
				printStream.print(getCMString(bt, cmList,phraseInd));
				pwIndFile.print(getCMString(bt, cmList,phraseInd));
			}

			wordStrHm.put(ctext.trim(),wordStrHm.size());
			//when ii==0, window = 4, j starts from -4, ws = -4-0=-4, 
			//next j = -3
			for(int j=ii-leftWindow; j<=ii+rightWindow; j++ ){
				int ws=j-ii;
				if(j>=0 && j< baseTokenList.size()){
					if(phraseInd==0||phraseInd==phraseLen-1){
						//System.out.println(phraseInd+" phrase length: "+phraseLen);
						printStream.print(getWindowTokenString((BaseToken) baseTokenList.get(j), ws, gramType));	 
						pwIndFile.print(getWindowTokenString((BaseToken) baseTokenList.get(j), ws, gramType));
						if(globalCounter==3001 || globalCounter==3000){
							System.out.print(getWindowTokenString((BaseToken) baseTokenList.get(j), ws, gramType));
						}
					}
				}
				else if(j<0 && phraseInd==0){
					// ws < 0
					if(featureType.contains(FeatureCats.uniNorm.toString())){
						printStream.print("\tuniTok|"+gramType+ws+"|=SSSSS");
						pwIndFile.print("\tuniTok|"+gramType+ws+"|=SSSSS");
						if(globalCounter==3001 || globalCounter==3000){
							System.out.print("\tuniTok|"+gramType+ws+"|=SSSSS");
						}
					}
					
				}
				else{
					// ws >= 0
					if(featureType.contains(FeatureCats.uniNorm.toString())){
						printStream.print("\tuniTok|"+gramType+"+"+ws+"|=EEEEE");
						pwIndFile.print("\tuniTok|"+gramType+"+"+ws+"|=EEEEE");
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
	public void windowWordNgram(PrintStream printStream, PrintWriter pwIndFile, SortedMap<Pair<Integer,Integer>, BaseToken> locationMap, int nGram, int leftWindow, int rightWindow, int begin, int end) {
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

		

		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < alWord.size(); i ++) {
			sb.append("\t").append(alWord.get(i));
		}
		printStream.print(sb.toString());
		pwIndFile.print(sb.toString());
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
	
	public void windowCharNgram(PrintStream printStream, PrintWriter pwIndFile, List<BaseToken> baseTokenList, int nGram, int leftWindow, int rightWindow, int begin, int end) {
		ArrayList<String> al;
		ArrayList<String> alChar;
		String temp = "";
		al = new ArrayList<String>();
		for(int i = begin - leftWindow; i < begin; i ++) {
			if(i < 0) continue;
			temp = baseTokenList.get(i).getCoveredText().trim();
			temp = temp.replaceAll("\\[", "");
			temp = temp.replaceAll("\\]", "");
			al.add(temp);
		}
		alChar = this.charNgram(al, nGram);
		
		al = new ArrayList<String>();
		for(int i = end + 1; i < end + rightWindow + 1; i ++) {
			if(i > baseTokenList.size() - 1) continue;
			temp = baseTokenList.get(i).getCoveredText().trim();
			temp = temp.replaceAll("\\[", "");
			temp = temp.replaceAll("\\]", "");
			al.add(temp);
		}
		
		alChar.addAll(this.charNgram(al, nGram));
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < alChar.size(); i ++) {
			sb.append("\t").append(alChar.get(i));
		}
		printStream.print(sb.toString());
		pwIndFile.print(sb.toString());
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
