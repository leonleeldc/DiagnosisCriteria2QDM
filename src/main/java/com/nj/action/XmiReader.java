package com.nj.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
//import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XmlCasDeserializer;
import org.xml.sax.SAXException;

public class XmiReader {

	static public CAS deserializeXcas_NOT_USED(String xmlDescriptor, String xmlXcas) {
		// File descriptorFile = new File(xmlDescriptor);

		// Parse descriptor and create TCas and StyleMap
		Object descriptor;
		try {
			// Create new CAS with the type system from input descriptor.
			// Also build style map file if there is none.
			descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(xmlDescriptor));
			CAS tcas;
			// File styleMapFile;
			if (descriptor instanceof AnalysisEngineDescription) {
				tcas = CasCreationUtils.createCas((AnalysisEngineDescription) descriptor);
				// styleMapFile =
				// getStyleMapFile((AnalysisEngineDescription)descriptor,
				// descriptorFile.getPath());

			} else if (descriptor instanceof TypeSystemDescription) {
				TypeSystemDescription typeSystemDescription = (TypeSystemDescription) descriptor;
				typeSystemDescription.resolveImports();
				tcas = CasCreationUtils.createCas(typeSystemDescription, null, new FsIndexDescription[0]);
				// styleMapFile =
				// getStyleMapFile((TypeSystemDescription)descriptor,
				// descriptorFile.getPath());
			} else {
				// displayError("Invalid Descriptor File \"" +
				// descriptorFile.getPath() + "\"" +
				// "Must be either an AnalysisEngine or TypeSystem
				// descriptor.");
				return null;
			}
			CAS cas = CasCreationUtils.createCas(tcas.getTypeSystem(), null, new FsIndexDescription[0],
					UIMAFramework.getDefaultPerformanceTuningProperties());

			// Deserialize XCAS or XMI into CAS
			File xcasFile = new File(xmlXcas);

			// For Apache-UIMA
			FileInputStream xcasInStream = null;
			try {
				xcasInStream = new FileInputStream(xcasFile);
				XmlCasDeserializer.deserialize(xcasInStream, cas, true);
			} finally {
				if (xcasInStream != null)
					xcasInStream.close();
			}
			FSIterator it = cas.getSofaIterator();
			SofaFS sf = null;
			int i = 0;
			while (it.hasNext()) {
				SofaFS sofa = (SofaFS) it.next();
				if (++i == 2)
					sf = sofa;
			}
			return cas.getCurrentView();
		} catch (InvalidXMLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
			// } catch (ParserConfigurationException e) {
			// e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return null;
	} // deserializeXcas

	public static HashMap<String,String> extractXmiValues(String[] args){
		String typeSystem = args[0];
		String xmiFileName = args[1];
		CAS cas = deserializeXcas_NOT_USED(typeSystem,xmiFileName);
		HashMap<String,String> umlskeyValueHm = new HashMap<String,String>(); 
		try {
			JCas jcas = cas.getJCas();
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();

			FSIterator<TOP> annotItr = indexes.getAllIndexedFS(IdentifiedAnnotation.type);
			while(annotItr.hasNext()){
				IdentifiedAnnotation idAnnot = (IdentifiedAnnotation) annotItr.next();
				FSArray umlsArr = idAnnot.getOntologyConceptArr();
				for(int j=0;j<umlsArr.size();j++){
					UmlsConcept umlsCon = (UmlsConcept) umlsArr.get(j);
					String cui = umlsCon.getCui();
					
					String tui = umlsCon.getTui();
					umlskeyValueHm.put("CUI", cui);
					umlskeyValueHm.put("TUI", tui);
					System.out.println(" cui="+ cui+" tui="+ tui);
				}
			}
			
			annotItr = indexes.getAllIndexedFS(WordToken.type);
			while(annotItr.hasNext()){
				WordToken token = (WordToken) annotItr.next();
				System.out.println(token.getCoveredText());
				//System.out.println(" cui="+ umlsConcept.getCui()+" tui="+ umlsConcept.getTui());
				
			}
		} catch (CASException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return umlskeyValueHm;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String typeSystem = "/Users/m048100/Documents/workspace/xmlView/desc/typesystem/typeSystemDescriptor.xml";
		String xmiFileName = "/Users/m048100/Documents/workspace/xmlView/data/test/testoutput/111.txt.xml";
		args = new String[2];
		args[1] = typeSystem;
		args[1] = xmiFileName;
		extractXmiValues(args);
	}

}
