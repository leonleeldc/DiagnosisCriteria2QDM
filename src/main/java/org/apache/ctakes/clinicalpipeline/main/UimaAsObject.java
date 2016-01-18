/*
 * Copyright: (c) 2004-2012 Mayo Foundation for Medical Education and 
 * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 * triple-shield Mayo logo are trademarks and service marks of MFMER.
 *
 * Except as contained in the copyright notice above, or as used to identify 
 * MFMER as the author of this software, the trade names, trademarks, service
 * marks, or product names of the copyright holder shall not be used in
 * advertising, promotion or otherwise in connection with this software without
 * prior written authorization of the copyright holder.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ctakes.clinicalpipeline.main;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

//import edu.mayo.bmi.phenotyping.translator.cc.NonTerminalConsumer;

//import edu.mayo.bmi.uima.common.cc.NonTerminalConsumer; 
//import edu.mayo.mir.nlp.preprocessor.CDTDocumentType; 
//import edu.mayo.mir.nlp.preprocessor.DocumentMetaData; 

/** 
 * A UIMA pipeline is composed of a CAS initialize, TAE, and CAS Consumer. 
 * Note: copied from UimaPipelineImpl that Patrick had originally written 
 * for one of the J2EE projects 
 */ 
public class UimaAsObject 
{ 
	public Logger iv_logger; 

	public static String PROCESS_COMPLETE = "0"; 
	HashMap<String,HashMap<String,String>> parameterMap;

	public UimaAsObject(CPEFactory cpeFactory) throws ResourceConfigurationException 
	{ 
		iv_logger = Logger.getLogger(UimaAsObject.class);
		//CollectionReader cr = (CollectionReader) cpeFactory.getCollectionReader();
		this.iv_colReader = (CollectionReader) cpeFactory.getCollectionReader();
		//CasProcessor[] casProcessorArr = cpeFactory.getCasProcessors();
		casProcessorArr = cpeFactory.getCasProcessors();
		System.out.println("why lost");
	} 
	
	private UimaAsObject() 
	{ 
		iv_logger = Logger.getLogger(UimaAsObject.class);    
	} 




	/** 
	 * Executes the pipepline to annotate the document. 
	 * @param initObj DocumentMetaData 
	 * @return 
	 * @throws CollectionException 
	 * @throws AnalysisEngineProcessException 
	 * @throws IOException 
	 * @throws ResourceProcessException 
	 * @throws ResourceConfigurationException 
	 */ 
	public synchronized String annotate(Object initObj) 
			throws ResourceInitializationException, AnalysisEngineProcessException, 
			IOException, CollectionException, ResourceProcessException, ResourceConfigurationException { 

		if (iv_colReader == null ) 
			throw new RuntimeException("CasInitializer not initialized."); 
		PrimitiveAnalysisEngine_impl iv_tae = null;
		while(iv_colReader.hasNext()){
			
			for(int i=0;i<casProcessorArr.length;i++){
				if(casProcessorArr[i] instanceof PrimitiveAnalysisEngine_impl){
					iv_tae = (PrimitiveAnalysisEngine_impl) casProcessorArr[i];
//					boolean iv_useNewCas = false;
//					if (iv_useNewCas) 
//					{ 
//						iv_tcas = iv_tae.newCAS();
//						iv_colReader.getNext(iv_tcas);
//					} 
//					else 
//					{ 
//						iv_tcas.reset(); 
//					} 
					if (iv_tae == null) 
						throw new RuntimeException("TAE not initialized."); 

					iv_tae.process(iv_tcas);

				}else if(casProcessorArr[i] instanceof AggregateAnalysisEngine_impl){ 
					AggregateAnalysisEngine_impl iv_atae =  (AggregateAnalysisEngine_impl) casProcessorArr[i];
					boolean iv_useNewCas = true;
					if (iv_useNewCas) 
					{ 
						iv_tcas = iv_atae.newCAS(); 
						iv_colReader.getNext(iv_tcas);
					} 
					else 
					{ 
						iv_tcas.reset(); 
					} 
					if (iv_atae == null) 
						throw new RuntimeException("TAE not initialized.");
					iv_atae.process(iv_tcas);
					//iv_atae.collectionProcessComplete();
				}
			}
			//iv_atae.process(iv_tcas);
			//iv_tae.process(iv_tcas); 
			//flushNonStandardLogs(); 
		}
		if(iv_tae!=null){
			iv_tae.collectionProcessComplete();
		}
		
		return PROCESS_COMPLETE;  //indicate success 
	} 

	/** 
	 * Intended for Junit testing only 
	 * @return 
	 */ 
	public JCas getJCas() 
			throws CASException 
			{ return iv_tcas.getJCas(); } 

	public void destroy() 
	{  
		iv_colReader.destroy();
		for(int i=0;i<casProcessorArr.length;i++){
			if(casProcessorArr[i] instanceof PrimitiveAnalysisEngine_impl){
				PrimitiveAnalysisEngine_impl iv_tae=(PrimitiveAnalysisEngine_impl) casProcessorArr[i];
				iv_tae.destroy();
			}else if(casProcessorArr[i] instanceof AggregateAnalysisEngine_impl){
				AggregateAnalysisEngine_impl iv_atae =  (AggregateAnalysisEngine_impl) casProcessorArr[i];
				iv_atae.destroy();
			}
		}
	} 


	/*
	private void flushNonStandardLogs() 
			throws IOException { 
		los_err.flush(); 
		los_info.flush(); 
	} 
	 */
	// UIMA pipeline objs 
	//protected boolean iv_useNewCas = true; 
	protected CollectionReader iv_colReader;
	//protected PrimitiveAnalysisEngine_impl iv_tae; 
	//protected AggregateAnalysisEngine_impl iv_atae;
	protected CasProcessor[] casProcessorArr;

	// re-usable TCAS 
	protected CAS iv_tcas; 

	// preserve old stdout/stderr streams in case they might be useful      
	protected PrintStream stdout = System.out;                                       
	protected PrintStream stderr = System.err;                                       

} 
