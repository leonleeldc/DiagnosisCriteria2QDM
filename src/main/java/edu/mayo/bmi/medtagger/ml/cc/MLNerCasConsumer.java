package edu.mayo.bmi.medtagger.ml.cc;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;

import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;


import edu.mayo.bmi.medtagger.ie.type.Match;
import edu.mayo.bmi.medtagger.ml.type.Event;
import edu.mayo.bmi.medtagger.ml.type.i2b2Token;
import edu.mayo.bmi.medtagger.ml.type.shareSlot;
import edu.mayo.bmi.medtagger.ml.type.shareToken;
import edu.mayo.bmi.medtagger.ml.util.FeatureGenerator;
import edu.mayo.bmi.medtagger.ml.util.FeatureCats;
import edu.mayo.bmi.medtagger.ml.util.String2NumFeatureConverter;
import edu.mayo.bmi.medtagger.type.ConceptMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;

/**
 * A simple CAS consumer that writes the CAS to XMI format.
 * <p>
 * This CAS Consumer takes one parameter:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * </ul>
 */
public class MLNerCasConsumer extends CasConsumer_ImplBase {
	/**
	 * Name of configuration parameter that must be set to the path of a directory into which the
	 * output files will be written.
	 */
	public static final String PARAM_OUTPUTDIR = "OutputDirectory";
	public static final String PARAM_RW = "RightWindow";
	public static final String PARAM_LW = "LeftWindow";
	public static final String PARAM_TYPE = "TaskType";
	public static final String PRARAM_FEATURE_TYPE = "FeatureType";
	public static final String TOPIC_FEATURE_DIR = "TopicFeautreDir";
	// public static final String PARAM_MIMICVECTOR = "MIMICVector";
	// public static final String PARAM_EDTVECTOR = "EDTVector";


	private File mOutputDir;

	private int mDocNum;

	private int leftWindow=2;
	private int rightWindow=2;
	//private String mimicVector="";
	//private String EDTVector="";
	private String taskType="share";
	private FeatureGenerator fg=null;
	private File outFeatureFile = null;
	private PrintWriter pwContextStrFile;
	private PrintWriter pwWordStrFile;
	private PrintWriter pwNormStrFile;
	private PrintWriter pwCmStrFile;
	SortedMap<String,Integer> wordStrHm;
	SortedMap<String,Integer> normStrHm;
	SortedMap<String,Integer> cmStrHm;
	SortedMap<String,Integer> contextStrHm;
	int globalCounter  = 0;
	
	private HashMap<String, String> mapLDA;
	
	String featureType;
	String topicFeatureDir;
	
	public void initialize() throws ResourceInitializationException {
		mDocNum = 0;
		mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
		rightWindow = ((Integer) getConfigParameterValue(PARAM_RW)).intValue();
		leftWindow=  ((Integer) getConfigParameterValue(PARAM_LW)).intValue();
		taskType = (String) getConfigParameterValue(PARAM_TYPE);
		featureType = (String) getConfigParameterValue(PRARAM_FEATURE_TYPE);
		wordStrHm = new TreeMap<String,Integer>();
		normStrHm = new TreeMap<String,Integer>();
		cmStrHm = new TreeMap<String,Integer>();
		contextStrHm = new TreeMap<String,Integer>();
		try {
			fg=new FeatureGenerator(mOutputDir,"resources/medtaggerresources/machinelearning/crfsuite");
			pwWordStrFile = new PrintWriter(new FileWriter(new File(mOutputDir, "word.txt")));
			pwNormStrFile = new PrintWriter(new FileWriter(new File(mOutputDir, "norm.txt")));
			pwCmStrFile = new PrintWriter(new FileWriter(new File(mOutputDir, "conceptMention.txt")));
			pwContextStrFile = new PrintWriter(new FileWriter(new File(mOutputDir, "contextPhrase.txt")));;
			//outFeatureFile = new File(mOutputDir, "features.svmsuite."+featureType);
			outFeatureFile = new File(mOutputDir, "features.crfsuite."+featureType);
			if(outFeatureFile.exists()){
				outFeatureFile.delete();
				outFeatureFile.createNewFile();
			}
			System.out.println("outFeatureFile name: "+outFeatureFile.getAbsolutePath());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!mOutputDir.exists()) {
			mOutputDir.mkdirs();
		}
		
		if(featureType.contains(FeatureCats.lda_docfeatures.toString())){
			topicFeatureDir = (String) getConfigParameterValue(TOPIC_FEATURE_DIR);
			
			/** Add LDA feature. **/
			BufferedReader brLDA;
			mapLDA = new HashMap<String, String>();
			String line = "";
			Pattern pattern = Pattern.compile("\"(.*)\\s+[\\(]{2}(.*)[\\)]{2}\\s+[\\(]{2}(.*)[\\)]{2}\"\\s+(.*)");
			Matcher matcher;
			
			try {
				brLDA = new BufferedReader(new InputStreamReader(new FileInputStream(new File(topicFeatureDir))));
				while((line = brLDA.readLine()) != null) {
					matcher = pattern.matcher(line.trim());
					if(matcher.matches()) {
//						System.out.println(matcher.group(1) + "\tstart|" + matcher.group(2) + "\tend|" + matcher.group(3) + "\t" + matcher.group(4));
						mapLDA.put(matcher.group(1).substring(0, matcher.group(1).indexOf(" ")) + " " + matcher.group(2) + " " + matcher.group(3), matcher.group(4));
					}
				}
				brLDA.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			/** Add LDA feature. **/
		}
	}


	/**
	 * Processes the CAS which was populated by the TextAnalysisEngines. <br>
	 * In this case, the CAS is converted to XMI and written into the output file .
	 * 
	 * @param aCAS
	 *          a CAS which has been populated by the TAEs
	 * 
	 * @throws ResourceProcessException
	 *           if there is an error in processing the Resource
	 * 
	 * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
	 */
	@SuppressWarnings("rawtypes")
	public void processCas(CAS aCAS) throws ResourceProcessException {
		String modelFileName = null;

		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		// retreive the filename of the input file from the CAS
		FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
		File outFile = null;
		
		if (it.hasNext()) {
			SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
			File inFile;
			try {
				inFile = new File(new URL(fileLoc.getUri()).getPath());
				String outFileName = inFile.getName();
				if (fileLoc.getOffsetInSource() > 0) {
					outFileName += ("_" + fileLoc.getOffsetInSource());
				}
				outFileName += ".xmi";
				outFile = new File(mOutputDir, outFileName);
				//outFeatureFile = new File(mOutputDir, "features.crfsuite");

				modelFileName = mOutputDir.getAbsolutePath() + "/" + inFile.getName() + "."+this.featureType;
			} catch (MalformedURLException e1) {
				// invalid URL, use default processing below
			}
		}
		if (outFile == null) {
			outFile = new File(mOutputDir, "doc" + mDocNum++ + ".xmi");     
		}
		// serialize XCAS and write to output file
		try {
			writeXmi(jcas.getCas(), outFile, modelFileName);
			fg.generateTrainFeatureLabel(jcas, taskType,  outFile.getName(), outFeatureFile, leftWindow, rightWindow);
			//System.out.println("globalCounter: "+globalCounter+" modelFileName: "+modelFileName);
		} catch (IOException e) {
			throw new ResourceProcessException(e);
		} catch (SAXException e) {
			throw new ResourceProcessException(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeXmi(CAS aCas, File name, String modelFileName) throws IOException, SAXException {
		FileOutputStream out = null;

		try {
			// write XMI
			out = new FileOutputStream(name);
			XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);

			ser.serialize(aCas, xmlSer.getContentHandler());

		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	/**
	 * The following is for training after we have created candidates and extracted corresponding features
	 * 
	 * basically, we would print all global data here including words, norms and concept mentions 
	 * 
	 */
	public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException, IOException
	{
		super.collectionProcessComplete(arg0);
		//String2NumFeatureConverter.string2NumFV(outFeatureFile);
		for(Map.Entry<String, Integer> entry : wordStrHm.entrySet()){
			String word = entry.getKey();
			pwWordStrFile.println(word);
		}
		pwWordStrFile.flush();
		pwWordStrFile.close();
		
		for(Map.Entry<String, Integer> entry : normStrHm.entrySet()){
			String norm = entry.getKey();
			pwNormStrFile.println(norm);
		}
		pwNormStrFile.flush();
		pwNormStrFile.close();
		
		for(Map.Entry<String, Integer> entry : cmStrHm.entrySet()){
			String cm = entry.getKey();
			pwCmStrFile.println(cm);
		}
		pwCmStrFile.flush();
		pwCmStrFile.close();
		
		for(Map.Entry<String, Integer> entry : contextStrHm.entrySet()){
			String contextPhrase = entry.getKey();
			pwContextStrFile.println(contextPhrase);
		}
		pwContextStrFile.flush();
		pwContextStrFile.close();
	}
}

