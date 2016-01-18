package edu.mayo.bmi.medtagger.ml.ae;
/**
package edu.mayo.bmi.nlp.tlink;


import java.io.IOException;



/**
 * SimpleSegment
 * Sentence
 * @author m093788
 *
q */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;

import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.typesystem.type.refsem.Lab;
import org.apache.ctakes.typesystem.type.refsem.LabValue;
import org.apache.ctakes.typesystem.type.refsem.Procedure;
import org.apache.ctakes.typesystem.type.structured.Demographics;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabInterpretationModifier;
import org.apache.ctakes.typesystem.type.textsem.LabReferenceRangeModifier;
import org.apache.ctakes.typesystem.type.textsem.LabValueModifier;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
//
//import org.apache.commons.lang.StringEscapeUtils;
//import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * the xml file format of bioc cid:
 * collection
 * 	source
 * 	date
 * 	key
 * 	document
 * 		id
 * 		passage<key="type">title/abstract	
 * 		infon<key="type">
 * 		offset
 * 		text
 * 		annotation<id='0'>
 * 			infon<key="type">
 * 			location<offset, length>
 * 			text
 * 			infon<key="MESH">
 * 	annotation<id='R0'>
 * 		infon<key="relation">CID
 * 		infon<key="Chmical">
 *  	infon<key="Disease">
 *  
 * 
 *  
 * @author m048100
 *
 */
public class InputReader extends JCasAnnotator_ImplBase {

	//	labtest | The name of lab test
	//	labtestABB | The abbreviate of lab test
	//	resultString | The lab test result represent in text narrate, such as increase, decrease, elevate, positive, etc.
	//	symbol | sign before a value in a math notation representation, such as >,<, etc.
	//	resultValue | lab test result value is a number
	//	unit | the unit used for the value in a unit symbol, it usually after a value
	//	symbolinString |sign before or around a value in a text notation representation, such as above, no less than ...
	//	unitinString | sometimes unit represent in a text representation, such as mmol per liter
	//	method | the method of lab test
	//	body&organ | the body or organ at which this lab test do, such as serum, liver
	//	Demographic | just annotate sex Demographic in our annotation(women, men, female, male)
	//	TempCondition | some condition about time, such as at 2 hours during a standard OGTT
	//	boolean | and, or , used for connect different single QDM
	//	exclusion | occasionally there is a exclusion criteria, but it is only one criteria here mentioned exclusion.

	public static final String PARAM_SEGMENT_ID = "SegmentID";
	@ConfigurationParameter(
			name = PARAM_SEGMENT_ID,
			mandatory = false,
			defaultValue = "SIMPLE_SEGMENT",
			description = "Name to give to all segments"
			)
	private String docId ;
	private String segmentId;

	private String mode;
	private boolean training;

	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
		return AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(String segmentID) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class,
				SimpleSegmentAnnotator.PARAM_SEGMENT_ID,
				segmentID);
	}

	static Hashtable<String,String> html_specialchars_table = new Hashtable<String,String>();
	static {
		html_specialchars_table.put("&lt;","<");
		html_specialchars_table.put("&gt;",">");
		html_specialchars_table.put("&amp;","&");
		html_specialchars_table.put("&quot;","\"");
		html_specialchars_table.put("&apos;","'");       
	}
	static String htmlspecialchars_decode_ENT_NOQUOTES(String s){
		Enumeration<String> en = html_specialchars_table.keys();
		while(en.hasMoreElements()){
			String key = (String)en.nextElement();
			String val = (String)html_specialchars_table.get(key);
			s = s.replaceAll(key, val);
		}
		return s;
	}

	private String getDocId(JCas jcas)
	{
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();

		FSIterator<TOP> annotItr = indexes.getAllIndexedFS(DocumentID.type);

		while(annotItr.hasNext())
		{
			DocumentID docIDAnn = (DocumentID)annotItr.next();
			return docIDAnn.getDocumentID();
		}

		return null;
	}

	static Logger logger = Logger.getLogger(InputReader.class);


	public static String LeftPad(String str, Integer length, char car) {
		//logger.info("String:" + str + " : Length:" + length+ " : Char:"+String.valueOf(car)) ;
		//logger.info(String.format("%" + (length - str.length()) + "s", "").replace(" ", String.valueOf(car)) + str) ;
		return str+ String.format("%" + (length - str.length()) + "s", "").replace(" ", String.valueOf(car));
	}

	public static String RightPad(String str, Integer length, char car) {

		//logger.info("String:" + str + " : Length:" + length+ " : Char:"+String.valueOf(car)) ;
		//logger.info(String.format("%" + (length - str.length()) + "s", "").replace(" ", String.valueOf(car)) + str) ;
		return String.format("%" + (length - str.length()) + "s", "").replace(" ", String.valueOf(car)) + str;
	}

	/**
	 * @see AnalysisComponent#initialize(UimaContext)
	 */
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		// Create the pipeline that will take as input {data = File, target = String for classname}
		// and turn them into {data = FeatureVector, target = Label}
		super.initialize(aContext);
		mode = (String) aContext.getConfigParameterValue("mode");
		if(mode.equalsIgnoreCase("training")){
			training = true;
		}
		training = true;
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		docId = getDocId(aJCas);
		logger.info("processing document:\t"+docId);
		if(docId!=null && (docId.contains(".svn")||docId.contains(".dtd"))){
			return;
		}
		String xmlText=aJCas.getDocumentText();
		//InputStream inputStream = new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8));
		this.xmlReader(aJCas, xmlText);
	}	

	private void xmlReader(JCas aJCas, String xmlText ){
		xmlText = xmlText.replaceAll("&", "l"); //I have been tortured by these special characters. Now, I decide to use the simplest approach. Just replacing & as l.
		StringReader strReader = new StringReader(xmlText);
		int textStart = xmlText.indexOf("<TEXT><![CDATA[")+"<TEXT><![CDATA[".length()+1;
		String utf8XmlText = null;
		utf8XmlText = new String(xmlText.getBytes(),StandardCharsets.UTF_8);
		utf8XmlText  = htmlspecialchars_decode_ENT_NOQUOTES(xmlText);
		InputSource inputSource = new InputSource(strReader);
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			document = dBuilder.parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
			if(document==null){
				document = dBuilder.parse(inputSource);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		NodeList textNodes = document.getElementsByTagName("TEXT");
		SourceData sourceData = new SourceData(aJCas);
		String text = textNodes.item(0).getTextContent();

		Segment segment = new Segment(aJCas);
		segment.setBegin(textStart);
		segment.setEnd(textStart+text.length());
		if(docId==null || docId.trim().length()==0){
			docId = "testDoc";
		}
		if(segmentId==null){
			segmentId = "testSeg";
		}
		segment.setId(docId+"-"+segmentId);
		segment.addToIndexes();

		NodeList docNodes = document.getElementsByTagName("NOUN");
		System.out.println("the length of docNodes: "+docNodes.getLength());
		NodeList kidNodes = document.getChildNodes(); //the kidNodes should include only one node, namely, collection
		System.out.println("the length of kidNodes: "+kidNodes.getLength());
		for(int i=0;i<docNodes.getLength();i++){
			Node ithChild = docNodes.item(i);
			Lab lab = new Lab(aJCas);
			FSArray fsArray = new FSArray(aJCas,2);
			NamedNodeMap nnm = ithChild.getAttributes();

			Node idNode = nnm.getNamedItem("id");
			String idNodeVal = idNode.getNodeValue();
			Node textNode = nnm.getNamedItem("text");
			String textNodeVal = textNode.getNodeValue();
			logger.info("textNodeVal="+textNodeVal);
			Node startNode = nnm.getNamedItem("start");
			int start = Integer.valueOf(startNode.getNodeValue())+textStart;
			Node endNode = nnm.getNamedItem("end");
			int end = Integer.valueOf(endNode.getNodeValue())+textStart;
			Node typeNode = nnm.getNamedItem("type");
			String typeNodeVal = typeNode.getNodeValue();
			//labtest or labtestABB is actually the disease. 
			if(typeNodeVal.equalsIgnoreCase("labtest")||typeNodeVal.equalsIgnoreCase("labtestABB")){
				EntityMention mention = new EntityMention(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
//				fsArray.set(0, mention);
//				lab.setMentions(fsArray);
			}else if(typeNodeVal.equalsIgnoreCase("resultValue")||
					typeNodeVal.equalsIgnoreCase("unit") || typeNodeVal.equalsIgnoreCase("unitInString")){
				LabValueModifier mention = new LabValueModifier(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
				//lab.setIntValue(feat, val);
			}else if(typeNodeVal.equalsIgnoreCase("symbolInString") ||typeNodeVal.equalsIgnoreCase("symbol") ){
				LabReferenceRangeModifier mention = new LabReferenceRangeModifier(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
			}else if(typeNodeVal.equalsIgnoreCase("resultString")||
				typeNodeVal.equalsIgnoreCase("exclusion") || typeNodeVal.equalsIgnoreCase("boolean")) {
				LabInterpretationModifier mention = new LabInterpretationModifier(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
			}else if(typeNodeVal.equalsIgnoreCase("method")){
				MeasurementAnnotation mention = new MeasurementAnnotation(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
			}else if(typeNodeVal.equalsIgnoreCase("body&Organ")||
					typeNodeVal.equalsIgnoreCase("bodylamp;organ")){
				AnatomicalSiteMention mention = new AnatomicalSiteMention(aJCas);
				mention.setBegin(start);
				mention.setEnd(end);
				mention.setSubject(textNodeVal);
				mention.setSegmentID("Hong Na Annotated");
				mention.addToIndexes();
			}else if(typeNodeVal.equalsIgnoreCase("Demographic")){
				Demographics demographics = new Demographics(aJCas);
				demographics.setGender(textNodeVal);
				
				demographics.setMiddleName("Hong Na Annotated");
				demographics.addToIndexes();
			}else if(typeNodeVal.equalsIgnoreCase("TempCondition")){
				TimeMention time = new TimeMention(aJCas);
				time.setBegin(start);
				time.setEnd(end);
				time.setSegmentID("Hong Na Annotated");
				time.addToIndexes();
			}
		}
	}
}
