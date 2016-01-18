package org.apache.ctakes.clinicalpipeline.main;

import static org.apache.uima.fit.util.JCasUtil.iterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.JCasPool;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;


/**
 * A UIMA pipeline is composed of a CAS initialize, TAE, and CAS Consumer. Note:
 * copied from UimaPipelineImpl that Patrick had originally written for one of
 * the J2EE projects
 */
public class UimaPipeline {
	public Logger iv_logger = Logger.getLogger(UimaPipeline.class);
	public static String PROCESS_COMPLETE = "0";

	// -- constructors ----
	public UimaPipeline() {

	}

	public UimaPipeline(String ciDesc, String taeDesc, String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {

		initComponents(ciDesc, taeDesc, ccDesc);
	}

	public UimaPipeline(String ciDesc, String taeDesc, String ccDesc,
			boolean useNewCas) throws ResourceInitializationException,
			IOException, InvalidXMLException {

		iv_useNewCas = useNewCas;
		initComponents(ciDesc, taeDesc, ccDesc);
	}


	/**
	 * This helper returns a new instance of UimaPipeline. 
	 * If you are using this method to create an instance, 
	 * you <b>must</b> use the following methods to completely 
	 * initialize the instance before it can be used.
	 * {@link #setCasInitializer(String)}, {@link #setTae(String)}, {@link #setCasConsumer(String)} and
	 * {@link #prepForCasInit(Object)}
	 * @return
	 */
	public static UimaPipeline getInstance()
	{ return new UimaPipeline(); }


	

	public void setCollectionReader(String crDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initCollectionReader(crDesc);
	}

	
	public void setCasInitializer(String ciDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initCasInitializer(ciDesc);
	}

	public void setCasConsumer(String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initCasConsumer(ccDesc);
	}

	public void setTae(String taeDesc) throws ResourceInitializationException,
	IOException, InvalidXMLException {
		initTae(taeDesc);
	}

	public void setTaeUsingStream(String taeDesc)
	// Alternate tae when running stand alone
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initTaeUsingStream(taeDesc);
	}

	public void setTaeConfigParam(String paramName, String paramVal)
			throws ResourceConfigurationException {
		if (iv_ae == null) {
			iv_logger
			.error("TAE has not been loaded yet. Invoke setTaeConfigurationParam only after TAE instance has been created.");
			return;
		}

		iv_ae.setConfigParameterValue(paramName, paramVal);
		iv_ae.reconfigure();
	}


	
	/**
	 * Executes the pipepline to annotate the document.
	 * 
	 * @param initObj
	 *            DocumentMetaData
	 * @return
	 * @throws CollectionException
	 * @throws AnalysisEngineProcessException
	 * @throws IOException
	 * @throws ResourceProcessException
	 */
	public synchronized String annotate(String initObj,
			List<String> returnMessages)
					throws ResourceInitializationException,
					AnalysisEngineProcessException, IOException, CollectionException,
					ResourceProcessException {
		if (iv_casConsumer == null)
			throw new RuntimeException("Cas consumer not initialized.");

		returnMessages.add("Cas consumer not null");
		if (iv_ae == null)
			throw new RuntimeException("TAE not initialized.");
		returnMessages.add("TAE not null");

		if (iv_useNewCas) {
			returnMessages.add("Using new CAS");
			iv_tcas = iv_ae.newJCas();
			iv_useNewCas = false;
		} else {
			returnMessages.add("resetting old CAS");
			iv_tcas.reset();
		}

		if(iv_collectionReader != null){
			iv_collectionReader.getNext(iv_tcas.getCas());
		}
		
		if(iv_collectionReader == null && initObj != null)
			iv_tcas.getCas().setDocumentText(initObj);
		
		returnMessages.add("CAS Doc: " + initObj);
		System.out.println("CAS Doc: " + initObj);
		if (iv_casInitializer != null) {
			returnMessages.add("Going to process in CAS Init");
			iv_casInitializer.process(iv_tcas.getCas());
			returnMessages.add("CI process completed");
		}
		returnMessages.add("Going to TAE");
		System.out.println("Going to TAE");
//		returnMessages.add("Plaintext sofa now has:"
//				+ iv_tcas.getCas().getView("plaintext").getDocumentText());
		iv_ae.process(iv_tcas);
		returnMessages.add("completed TAE processing");
		System.out.println("completed TAE processing");
		iv_casConsumer.processCas(iv_tcas.getCas());

		// if (iv_casConsumer instanceof NonTerminalConsumer)
		// {
		// return ((NonTerminalConsumer) iv_casConsumer).getOutputXml();
		// }
		// else
		// {
		// return PROCESS_COMPLETE; //indicate success
		return "you need to read the output file";
		// }
	}

	/**
	 * Intended for Junit testing only
	 * 
	 * @return
	 */
	public JCas getJCas() throws CASException {
		return iv_tcas;
	}

	public void destroy() {
		if (iv_casInitializer != null)
			iv_casInitializer.destroy();

		if (iv_ae != null)
			iv_ae.destroy();

		if (iv_casConsumer != null)
			iv_casConsumer.destroy();
	}

	private void initComponents(String ciDesc, String taeDesc, String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initCasInitializer(ciDesc);
		// load TAE
		initTae(taeDesc);

		// load CAS Consumer
		initCasConsumer(ccDesc);
	}

	public UimaPipeline(String taeDesc, String ccDesc, boolean useNewCas)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {

		iv_useNewCas = useNewCas;
		initComponents(taeDesc, ccDesc);
	}

	private void initComponents(String taeDesc, String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		initTaeUsingStream(taeDesc);

		if (ccDesc != null) {
			// load CAS Consumer
			initCasConsumerUsingStream(ccDesc);
		} else {
			iv_tcas = iv_AEPool.getJCas();
		}
	}

	private void initCollectionReader(String ciDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		// load CAS Initializer
		ResourceSpecifier crSpecifier = null;
		System.out.println("UimaPipelineImpl Collection Reader:[" + ciDesc + "]");
		if (ciDesc != null) {
			File file = new File(ciDesc);
			XMLInputSource xmlIS = new XMLInputSource(file);
			XMLParser parser = UIMAFramework.getXMLParser();

			// ciSpecifier = parser.parseCasInitializerDescription(xmlIS);
			// iv_casInitializer =
			// UIMAFramework.produceCasInitializer(ciSpecifier);

			crSpecifier = parser.parseCollectionReaderDescription(xmlIS);
			iv_collectionReader = UIMAFramework.produceCollectionReader(crSpecifier);
		}

		iv_logger.info("Loaded collectionReader: " + ciDesc);

	}
	
	private void initCasInitializer(String ciDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		// load CAS Initializer
		ResourceSpecifier ciSpecifier = null;
		System.out.println("UimaPipelineImpl ciDesc:[" + ciDesc + "]");
		if (ciDesc != null) {
			File file = new File(ciDesc);
			XMLInputSource xmlIS = new XMLInputSource(file);
			XMLParser parser = UIMAFramework.getXMLParser();

			// ciSpecifier = parser.parseCasInitializerDescription(xmlIS);
			// iv_casInitializer =
			// UIMAFramework.produceCasInitializer(ciSpecifier);

			ciSpecifier = parser.parseAnalysisEngineDescription(xmlIS);
			iv_casInitializer = UIMAFramework
					.produceAnalysisEngine(ciSpecifier);
		}

		iv_logger.info("Loaded CAS initializer: " + ciDesc);

	}

	private void initTae(String taeDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		ResourceSpecifier taeSpecifier = UIMAFramework.getXMLParser()
				.parseResourceSpecifier(new XMLInputSource(new File(taeDesc)));
		// iv_tae = UIMAFramework.produceTAE(taeSpecifier);
		iv_ae = UIMAFramework.produceAnalysisEngine(taeSpecifier);
		iv_AEPool = new JCasPool(2, iv_ae);// Using as a precaution if there is
		// parallel execution e.g. smoking
		// status pipeline
		iv_logger.info("Loaded TAE: " + taeDesc);
	}

	private void initTaeUsingStream(String taeDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		InputStream fileAsStream = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream(taeDesc);
		ResourceSpecifier taeSpecifier = UIMAFramework.getXMLParser()
				.parseResourceSpecifier(new XMLInputSource(fileAsStream, null));
		// iv_tae = UIMAFramework.produceTAE(taeSpecifier);
		iv_ae = UIMAFramework.produceAnalysisEngine(taeSpecifier);
		iv_AEPool = new JCasPool(2, iv_ae);// Using as a precaution if there is
		// parallel execution e.g. smoking
		// status pipeline
		iv_logger.info("Loaded TAE: " + taeDesc);
	}

	private void initCasConsumer(String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		iv_logger.info("Loading CAS consumer: " + ccDesc);
		ResourceSpecifier ccSpecifier = UIMAFramework.getXMLParser()
				.parseCasConsumerDescription(
						new XMLInputSource(new File(ccDesc)));

		iv_casConsumer = UIMAFramework.produceCasConsumer(ccSpecifier);

		iv_tcas = iv_AEPool.getJCas();

		// -- iv_casInitializer.typeSystemInit(iv_tcas.getTypeSystem());
		iv_casConsumer.typeSystemInit(iv_tcas.getTypeSystem());
		iv_logger
		.info("Initialized type system for CAS initializer and consumer(s).");
	}

	private void initCasConsumerUsingStream(String ccDesc)
			throws ResourceInitializationException, IOException,
			InvalidXMLException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		InputStream inputCCDescUsingStream = classLoader
				.getResourceAsStream(ccDesc);

		iv_logger.info("Loading CAS consumer: " + ccDesc);
		ResourceSpecifier ccSpecifier = UIMAFramework.getXMLParser()
				.parseCasConsumerDescription(
						new XMLInputSource(inputCCDescUsingStream, null));

		iv_casConsumer = UIMAFramework.produceCasConsumer(ccSpecifier);

		// keep reference to one TCAS object that can be reused
		// recreating TCAS objects is very expensive
		// iv_tcas = iv_tae.newJCas();
		iv_tcas = iv_AEPool.getJCas();

		// -- iv_casInitializer.typeSystemInit(iv_tcas.getTypeSystem());
		iv_casConsumer.typeSystemInit(iv_tcas.getTypeSystem());
		iv_logger
		.info("Initialized type system for CAS initializer and consumer(s).");
	}
	
//	public static void main(String[] args){
//		
//		List<String> returnMessages = new ArrayList();
//		
//		//add from here 
//		UimaPipeline uimaPipeCda = new UimaPipeline();
//		uimaPipeCda.setCollectionReader(crDesc);
//		uimaPipeCda.setCasInitializer(ciDescCdt);
//		uimaPipeCda.setTae(taeDescCda);
//		uimaPipeCda.setCasConsumer(ccDescCda);
//		//add up to here to the servelet's initialize() method
//		
//		
//		//
//		String output = uimaPipeCda.annotate(null, returnMessages);
//		
//	}

	// UIMA pipeline objs
	protected boolean iv_useNewCas = true;
	protected AnalysisEngine iv_casInitializer;
	protected CollectionReader iv_collectionReader;

	protected AnalysisEngine iv_ae;
	protected JCasPool iv_AEPool;
	protected CasConsumer iv_casConsumer;

	// re-usable TCAS
	protected JCas iv_tcas;

}