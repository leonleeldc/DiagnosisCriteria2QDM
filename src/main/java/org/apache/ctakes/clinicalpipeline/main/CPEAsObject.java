/*** Eclipse Class Decompiler plugin, copyright (c) 2012 Chao Chen (cnfree2000@hotmail.com) ***/
package org.apache.ctakes.clinicalpipeline.main;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

public class CPEAsObject {
	public static void runCpe(String descriptorDir) throws IOException, InvalidXMLException, ResourceInitializationException, ResourceConfigurationException, AnalysisEngineProcessException, CollectionException, ResourceProcessException{
		ResourceManager aResourceManager = null;
		File descFile = new File(descriptorDir);
//		try {
//			descFile = new File(CPEAsObject.class.getClassLoader().getResource(descriptorDir).toURI());		
//			XMLInputSource cpeXml = new XMLInputSource(descFile);
//			CpeDescription aDescriptor = UIMAFramework.getXMLParser().parseCpeDescription(cpeXml);
//			CPEFactory cpeFactory = new CPEFactory(aDescriptor,aResourceManager);
//			UimaAsObject uao = new UimaAsObject(cpeFactory);
//			uao.annotate(null);
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		XMLInputSource cpeXml = new XMLInputSource(descFile);
		CpeDescription aDescriptor = UIMAFramework.getXMLParser().parseCpeDescription(cpeXml);
		CPEFactory cpeFactory = new CPEFactory(aDescriptor,aResourceManager);
		UimaAsObject uao = new UimaAsObject(cpeFactory);
		uao.annotate(null);

	}

	public static void main(String[] args)
			throws AnalysisEngineProcessException, InvalidXMLException,
			ResourceInitializationException, ResourceConfigurationException,
			CollectionException, ResourceProcessException, IOException, XMLStreamException, FactoryConfigurationError {
		String[] arguments = {
				"desc/collection_processing_engine/edtCPE.xml", //0
				};
		runCpe(arguments[0]);
	}
}
