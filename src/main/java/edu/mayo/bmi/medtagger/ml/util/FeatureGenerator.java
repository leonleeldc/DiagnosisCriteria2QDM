package edu.mayo.bmi.medtagger.ml.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import edu.mayo.bmi.medtagger.util.Pair;
//import edu.mayo.bmi.uima.core.type.syntax.ConllDependencyNode;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;

public class FeatureGenerator {

	CRFSuiteWrapper csw=null; 
	File modelFile=null;

	public FeatureGenerator() throws Exception{	
	}
	
	public FeatureGenerator(File modelfile, String csh) throws Exception{	
		csw=new CRFSuiteWrapper(csh);
		modelFile=modelfile;
	}

	@SuppressWarnings("rawtypes")
	public void generateTrainFeatureLabel(JCas aJCas, String type, String inFile, File name,  int leftWindow, int rightWindow) throws Exception {
		FileOutputStream out = null;
		System.out.println("input file name in generateTrainingFeatureLabel: "+inFile);
		try {
			FSIterator it = aJCas.getAnnotationIndex(DocumentAnnotation.type).iterator();
			AnnotationIndex segIndex = aJCas.getAnnotationIndex(Segment.type);
			AnnotationIndex senIndex = aJCas.getAnnotationIndex(Sentence.type);
			AnnotationIndex tokenIndex = aJCas.getAnnotationIndex(BaseToken.type);
			//AnnotationIndex cdnAnnotIndex = aJCas.getAnnotationIndex(ConllDependencyNode.type);

			AnnotationIndex annotIndex = null;
			if(type.equals("i2b2"))
				annotIndex = aJCas.getAnnotationIndex(Event.type);
			if(type.equals("share")) 
				annotIndex = aJCas.getAnnotationIndex(shareSlot.type);
			AnnotationIndex cmIndex = aJCas.getAnnotationIndex(ConceptMention.type);
			DocumentAnnotation docInfo;
			if(it.hasNext()){
				docInfo = (DocumentAnnotation) it.next();
			}else return;
			// write XMI

			out = new FileOutputStream(name, true);
			final PrintStream printStream = new PrintStream(out); 
			FSIterator segIter= segIndex.iterator();
			FSIterator annotIter = annotIndex.iterator();
			List annotList=new ArrayList();
			while (annotIter.hasNext())    
				annotList.add(annotIter.next());             

			while(segIter.hasNext()){
				Segment seg=(Segment) segIter.next();
				FSIterator senIter= senIndex.subiterator(seg);

				while (senIter.hasNext()) {
					Sentence sen=(Sentence) senIter.next();
					FSIterator baseTokenIter = tokenIndex.subiterator(sen);
					FSIterator cmIter = cmIndex.subiterator(sen);
					//FSIterator cdnIter = cdnAnnotIndex.subiterator(sen);
//					while(cdnIter.hasNext()){
//						ConllDependencyNode cdn = (ConllDependencyNode) cdnIter.next();
//						Pair<Integer,Integer> cdnOffset = new Pair<Integer,Integer>(cdn.getBegin(),cdn.getEnd());
//						//the following while loop would find the head for the cdn which is within the range of some tdt
//						ConllDependencyNode cdnHead=cdn.getHead();
//					}
					List<BaseToken> baseTokenList = new ArrayList<BaseToken>();
					List<ConceptMention> cmList = new ArrayList<ConceptMention>();
					while (baseTokenIter.hasNext()) 
						baseTokenList.add((BaseToken) baseTokenIter.next());
					while (cmIter.hasNext()) 
						cmList.add((ConceptMention) cmIter.next());


					for(int li=0; li < baseTokenList.size(); li++){
						BaseToken bt= (BaseToken) baseTokenList.get(li);
						String ctext=bt.getCoveredText();
						//printStream.print();
						if(bt instanceof shareToken){
							shareToken st=(shareToken) bt;
							//i2b2Begin=1     i2b2End=10      i2b2Line=1      i2b2tokNumb=0   begin=0 end=9   
									printStream.print("shareBegin="+st.getShareBegin()+"\tshareEnd="+st.getShareEnd()+"\tshareLine="
											+st.getLineNumber()+"\tsharetokNumb="+st.getTokenNumber()+"\tbegin="
											+st.getBegin()+"\tend="+st.getEnd()+"\t");
						}
						if(bt instanceof i2b2Token){
							i2b2Token st=(i2b2Token) bt;
							//i2b2Begin=1     i2b2End=10      i2b2Line=1      i2b2tokNumb=0   begin=0 end=9   
							//printStream.print("i2b2Begin="+st.getI2b2Begin() +"\ti2b2End="+st.getI2b2End()+"\ti2b2Line="
								//	+st.getLineNumber()+"\ti2b2tokNumb="+st.getTokenNumber()+"\tbegin="
									//+st.getBegin()+"\tend="+st.getEnd()+"\t");
						}
						if(ctext.equals(":")) ctext="PUNC_COLON";
						if(ctext.equals("\\")) ctext="PUNC_SLASH";
						printStream.print(getAnnotString(bt,type, annotList));
						printStream.print("\t"+ctext);
						printStream.print("\tsectionHeader="+seg.getId());
						printStream.print(getCMString(bt, cmList));

						for(int j=li-leftWindow; j<=li+rightWindow; j++ ){
							int ws=j-li;
							if(j>=0 && j< baseTokenList.size()){
								printStream.print(getTokenString((BaseToken) baseTokenList.get(j), ws));	      
							}
							else if(j<0) printStream.print("\ttok["+ws+"]SSSSS");
							else printStream.print("\ttok["+ws+"]EEEEE");
						}
						//this aims at record the name of the file name. Namely, we cannot where the features are from.
						printStream.print("#"+inFile);
						printStream.print("\n");
					}
				}
				//printStream.print("\n");
			}

			printStream.close();
		}

		finally {  
			if (out != null) {
				out.close();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public List<Event> applyCRF(JCas aJCas, int leftWindow, int rightWindow) throws Exception {
		FileOutputStream out = null;
		File featureFile = File.createTempFile("features", ".crfsuite");
		System.out.println("feature file for testing data generated in applyCRF of FeatureGenerator.java: "+featureFile.getAbsolutePath());
		featureFile.deleteOnExit();
		HashMap<Integer,BaseToken> tokenIndexes=new HashMap<Integer,BaseToken>();
		int featureIndex=0;	
		try {
			FSIterator it = aJCas.getAnnotationIndex(DocumentAnnotation.type).iterator();
			AnnotationIndex senIndex = aJCas.getAnnotationIndex(Sentence.type);
			AnnotationIndex tokenIndex = aJCas.getAnnotationIndex(BaseToken.type);
			AnnotationIndex cmIndex = aJCas.getAnnotationIndex(ConceptMention.type);

			DocumentAnnotation docInfo;
			if(it.hasNext()){
				docInfo = (DocumentAnnotation) it.next();
			}else {
				return null;
			}
			out = new FileOutputStream(featureFile);
			final PrintStream printStream = new PrintStream(out); 
			FSIterator senIter= senIndex.subiterator(docInfo);
			while (senIter.hasNext()) {
				Sentence sen=(Sentence) senIter.next();
				FSIterator baseTokenIter = tokenIndex.subiterator(sen);
				FSIterator cmIter = cmIndex.subiterator(sen);
				List<BaseToken> baseTokenList = new ArrayList<BaseToken>();
				List<ConceptMention> cmList = new ArrayList<ConceptMention>();

				while (baseTokenIter.hasNext()) 
					baseTokenList.add((BaseToken) baseTokenIter.next());
				while (cmIter.hasNext()) 
					cmList.add((ConceptMention) cmIter.next());


				for(int li=0; li < baseTokenList.size(); li++){
					BaseToken bt= (BaseToken) baseTokenList.get(li);
					tokenIndexes.put(featureIndex, bt);
					featureIndex++;
					printStream.print("U");
					String ctext=bt.getCoveredText();
					if(ctext.equals(":")) ctext="PUNC_COLON";
					if(ctext.equals("\\")) ctext="PUNC_SLASH";
					printStream.print("\t"+ctext);
					printStream.print(getCMString(bt, cmList));

					for(int j=li-leftWindow; j<=li+rightWindow; j++ ){
						int ws=j-li;
						if(j>=0 && j< baseTokenList.size()){
							printStream.print(getTokenString((BaseToken) baseTokenList.get(j), ws));	      
						}
						else if(j<0) printStream.print("\ttok["+ws+"]SSSSS");
						else printStream.print("\ttok["+ws+"]EEEEE");
					}
					printStream.print("\n");
				}
				//printStream.print("\n");
				tokenIndexes.put(featureIndex, null);
				featureIndex++;
			}

			printStream.close();
		}
		finally {  
			if (out != null) {
				out.close();
			}
		}     
		List<Event> pEvents=new ArrayList<Event>();
		List<String> result=csw.classifyFeatures(featureFile, modelFile, tokenIndexes.size()); 
		boolean inE=false;
		Event e=null;
		int lastEnd=-1;
		for(int li=0; li<result.size(); li++){
			BaseToken bt=tokenIndexes.get(li);
			String rstr=result.get(li).trim();
			if(bt==null || rstr.equals("")) {inE=false;}
			if(rstr.equals("O")) {
				if(inE) {
					e.setEnd(lastEnd);
					pEvents.add(e);
				}
				e=null;
				inE=false;
				lastEnd=bt.getEnd();
				continue;
			}
			if(rstr.indexOf("_") >=0){
				String[] pre=rstr.split("_");	  
				if(pre[1].equals("B")){
					inE=true;
					e=new Event(aJCas);
					e.setEventType(pre[0]);
					e.setBegin(bt.getBegin());
					lastEnd=bt.getEnd();
					continue;
				}
				if(pre[1].equals("I")){
					inE=true;
					lastEnd=bt.getEnd();
					continue;
				}
			}
		} 
		if(inE) {
			e.setEnd(lastEnd);
			pEvents.add(e);
		}
		return pEvents;
	}


	private String getTokenString(BaseToken bt, int ws){
		String featStr="";
		if(bt.getClass().equals(shareToken.class) || bt.getClass().equals(i2b2Token.class) || bt.getClass().equals(WordToken.class)){ 
			WordToken wordToken=(WordToken) bt;
			String norm=wordToken.getCanonicalForm();
			if(norm==null) norm=wordToken.getCoveredText().toLowerCase(); 
			String ctext=bt.getCoveredText();
			if(ctext.equals(":")) {ctext="PUNC_COLON"; norm=ctext;}
			if(ctext.equals("\\")) {ctext="PUNC_SLASH"; norm=ctext;}

			featStr="\ttok["+ws+"]="+ctext+"\tnorm"+"["+ws+"]="+norm
					+"\tcapital"+"["+ws+"]="+wordToken.getCapitalization()
					+"\tpos"+"["+ws+"]="+wordToken.getPartOfSpeech()
					+getOtherFeatures(wordToken.getCoveredText(), ws) 
					+getPrefixAndSuffix(wordToken.getCoveredText(), ws);   
		}
		return featStr.replaceAll("  "," ");
	}

	private String getAnnotString(BaseToken bt, String type, List annotl){
		int size=annotl.size();
		//System.out.println("base token in getAnnotString of FeatureGenerator: "+bt.getCoveredText());
		if(type.equals("share")){
			if(bt instanceof shareToken) {
				shareToken wt = (shareToken) bt;	 
				for(int j=0; j<size; j++){
					shareSlot ss=(shareSlot) annotl.get(j);
					if(ss.getSlotClass().indexOf("Disease")>=0){
						//if(ss.getSlotClass().indexOf("Abbreviation")>=0){
						if(ss.getBegin()==wt.getShareBegin()) {
							return ss.getSlotClass()+"_B";
						}
						else if (ss.getBegin() < wt.getShareBegin() && ss.getEnd()>= wt.getShareEnd()) 
						{
							return ss.getSlotClass()+"_I"; 
						}
					}
				}
				return "O";
			}
		}

		if(type.equals("i2b2")){
			i2b2Token wt = (i2b2Token) bt;	 
			for(int j=0; j<size; j++) {
				Event e=(Event) annotl.get(j);
				//System.out.println("event begin:"+e.getBegin()+" event end: "+e.getEnd()+" line: "+e.getBeginLine());
				if(e.getBegin()>=0){
					if (wt.getBegin() - wt.getLineNumber() +1 ==e.getBegin()+0)
						return e.getEventType()+"_B";
					else if (wt.getBegin() - wt.getLineNumber() +1 > e.getBegin()+0 && wt.getEnd() - wt.getLineNumber() +1<= e.getEnd()+0 )
						return e.getEventType()+"_I";
				}
				else{
					if(e.getBeginLine()==wt.getLineNumber()){
						if(e.getBeginLineToken()==wt.getLineTokenNumber()) return e.getEventType()+"_B";
						else if(e.getBeginLine()==e.getEndLine() && wt.getLineTokenNumber()>e.getBeginLineToken() && wt.getLineTokenNumber()<=e.getEndLineToken()) return e.getEventType()+"_I";
						else if(e.getBeginLine()+1==e.getEndLine()&& wt.getLineTokenNumber()>e.getBeginLineToken()) return e.getEventType()+"_I";
					}
					else if(e.getBeginLine()+1==wt.getLineNumber() && e.getBeginLine()+1==e.getEndLine() && wt.getLineTokenNumber()<=e.getEndLineToken()) return e.getEventType()+"_I";
				}
			}
			return "O";
		}
		return "O";
	}

	private String getCMString(BaseToken bt, List<ConceptMention> cl){
		String msg="";
		for(int j=0; j<cl.size(); j++) {
			ConceptMention cm=(ConceptMention) cl.get(j);
			if (bt.getBegin()==cm.getBegin()){
				msg+="\tcer="+cm.getCer() +"\tsemGrp="+cm.getSemG()+"\tsemText="+cm.getNorm().replaceAll(" ","_");
				msg+= "\tsemGrpBI="+cm.getSemG()+"_B";
			}
			else if (bt.getBegin()>cm.getBegin() && bt.getEnd()<=cm.getEnd() ){
				msg+="\tcer="+cm.getCer() +"\tsemGrp="+cm.getSemG()+"\tsemText="+cm.getNorm().replaceAll(" ","_");
				msg+="\tsemGrpBI="+cm.getSemG()+"_I";
			}
		}
		return msg;
	}



	private String getOtherFeatures(String text, int ws){
		String str="";

		str+=getRegexMatch(text, ws, "ALPHA", Pattern.compile("^[A-Za-z]+$"));
		str+=getRegexMatch(text, ws, "INITCAPS", Pattern.compile("^[A-Z].*$"));
		str+=getRegexMatch(text, ws, "UPPER-LOWER", Pattern.compile("^[A-Z][a-z].*$"));
		str+=getRegexMatch(text, ws, "LOWER-UPPER", Pattern.compile("^[a-z]+[A-Z]+.*$"));
		str+=getRegexMatch(text, ws, "ALLCAPS", Pattern.compile("^[A-Z]+$"));
		str+=getRegexMatch(text, ws, "MIXEDCAPS", Pattern.compile("^[A-Z][a-z]+[A-Z][A-Za-z]*$"));
		str+=getRegexMatch(text, ws, "SINGLECHAR", Pattern.compile("^[A-Za-z]$"));
		str+=getRegexMatch(text, ws, "SINGLEDIGIT", Pattern.compile("^[0-9]$"));
		str+=getRegexMatch(text, ws, "NUMBER", Pattern.compile("^[0-9,]+$"));
		str+=getRegexMatch(text, ws, "HASDIGIT", Pattern.compile("^.*[0-9].*$"));
		str+=getRegexMatch(text, ws, "ALPHANUMERIC", Pattern.compile("^.*[0-9].*[A-Za-z].*$"));
		str+=getRegexMatch(text, ws, "ALPHANUMERIC", Pattern.compile("^.*[A-Za-z].*[0-9].*$"));
		str+=getRegexMatch(text, ws, "NUMBERS_LETTERS", Pattern.compile("^[0-9]+[A-Za-z]+$"));
		str+=getRegexMatch(text, ws, "LETTERS_NUMBERS", Pattern.compile("^[A-Za-z]+[0-9]+$"));
		str+=getRegexMatch(text, ws, "ROMAN", Pattern.compile("^[IVXDLCM]+$",Pattern.CASE_INSENSITIVE));
		str+=getRegexMatch(text, ws, "GREEK", Pattern.compile("^(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)$"));
		str+=getRegexMatch(text, ws, "ISPUNCT", Pattern.compile("^[`~!@#$%^&*()-=_+\\[\\]\\\\{}|;\':\\\",./<>?]+$"));


		return str;
	}

	private String getRegexMatch(String str, int ws, String strClass , Pattern p){
		Matcher m = p.matcher(str);
		if (m.matches()) {return "\tSTCLS["+ws+"]="+strClass;}
		else { return "";}
	}

	private String getPrefixAndSuffix(String txt, int ws){
		if( txt.length()>=3){
			return "\tPRE["+ws+"]="+txt.substring(0,3)+"\tSUF["+ws+"]="+txt.substring(txt.length()-3,txt.length());
		}
		return "";
	}
	@SuppressWarnings("rawtypes")
	public List<shareSlot> applyCRFShare(JCas aJCas, int leftWindow, int rightWindow) throws Exception {
		FileOutputStream out = null;
		File featureFile = File.createTempFile("features", ".crfsuite");
		featureFile.deleteOnExit();
		HashMap<Integer,BaseToken> tokenIndexes=new HashMap<Integer,BaseToken>();
		int featureIndex=0;	
		try {
			FSIterator it = aJCas.getAnnotationIndex(DocumentAnnotation.type).iterator();
			AnnotationIndex senIndex = aJCas.getAnnotationIndex(Sentence.type);
			AnnotationIndex tokenIndex = aJCas.getAnnotationIndex(BaseToken.type);
			AnnotationIndex cmIndex = aJCas.getAnnotationIndex(ConceptMention.type);

			DocumentAnnotation docInfo;
			if(it.hasNext()){
				docInfo = (DocumentAnnotation) it.next();
			}else {
				return null;
			}
			out = new FileOutputStream(featureFile);
			final PrintStream printStream = new PrintStream(out); 
			FSIterator senIter= senIndex.subiterator(docInfo);
			while (senIter.hasNext()) {
				Sentence sen=(Sentence) senIter.next();
				FSIterator baseTokenIter = tokenIndex.subiterator(sen);
				FSIterator cmIter = cmIndex.subiterator(sen);
				List<BaseToken> baseTokenList = new ArrayList<BaseToken>();
				List<ConceptMention> cmList = new ArrayList<ConceptMention>();

				while (baseTokenIter.hasNext()) 
					baseTokenList.add((BaseToken) baseTokenIter.next());
				while (cmIter.hasNext()) 
					cmList.add((ConceptMention) cmIter.next());


				for(int li=0; li < baseTokenList.size(); li++){
					BaseToken bt= (BaseToken) baseTokenList.get(li);
					tokenIndexes.put(featureIndex, bt);
					featureIndex++;
					printStream.print("U");
					String ctext=bt.getCoveredText();
					if(ctext.equals(":")) ctext="PUNC_COLON";
					if(ctext.equals("\\")) ctext="PUNC_SLASH";
					printStream.print("\t"+ctext);
					printStream.print(getCMString(bt, cmList));

					for(int j=li-leftWindow; j<=li+rightWindow; j++ ){
						int ws=j-li;
						if(j>=0 && j< baseTokenList.size()){
							printStream.print(getTokenString((BaseToken) baseTokenList.get(j), ws));	      
						}
						else if(j<0) printStream.print("\ttok["+ws+"]SSSSS");
						else printStream.print("\ttok["+ws+"]EEEEE");
					}
					printStream.print("\n");
				}
				//printStream.print("\n");
				tokenIndexes.put(featureIndex, null);
				featureIndex++;
			}

			printStream.close();
		}
		finally {  
			if (out != null) {
				out.close();
			}
		}     
		List<shareSlot> pss=new ArrayList<shareSlot>();
		List<String> result=csw.classifyFeatures(featureFile, modelFile, tokenIndexes.size()); 
		boolean inE=false;
		shareSlot ss=null;
		int lastEnd=-1;
		for(int li=0; li<result.size(); li++){
			BaseToken bt=tokenIndexes.get(li);
			String rstr=result.get(li).trim();
			if(bt==null || rstr.equals("")) {inE=false;}
			if(rstr.equals("O")) {
				if(inE) {
					ss.setEnd(lastEnd);
					pss.add(ss);
				}
				ss=null;
				inE=false;
				lastEnd=bt.getEnd();
				continue;
			}
			if(rstr.indexOf("_") >=0){
				String[] pre=rstr.split("_");	  
				if(pre[1].equals("B")){
					inE=true;
					ss=new shareSlot(aJCas);
					ss.setSlotClass(pre[0]);
					ss.setBegin(bt.getBegin());
					lastEnd=bt.getEnd();
					continue;
				}
				if(pre[1].equals("I")){
					inE=true;
					lastEnd=bt.getEnd();
					continue;
				}
			}
		}  
		if(inE) {
			ss.setEnd(lastEnd);
			pss.add(ss);
		}
		return pss;
	}
}
