package edu.mayo.bmi.medtagger.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


import edu.mayo.bmi.medtagger.type.ConceptMention;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;


/**
 * 
 * Abstract class for all Resource Managers to inherit from. Contains basic
 * functionality such as file system access and some private members.
 *
 */
public class ResourceUtilManager {

	public static String RESOURCEDIR;
	private static ResourceUtilManager INSTANCE = null;
	
	private Logger iv_logger = Logger.getLogger(getClass().getName());
	
	private Pattern regexpPattern = Pattern.compile("(.*)");
	private Pattern normPattern = Pattern.compile("^(.*?)\t(.*?)$");
	private Pattern rulePattern = Pattern.compile("RULENAME=\"(.*?)\",REGEXP=\"(.*?)\",LOCATION=\"(.*?)\",NORM=\"(.*?)\"(.*)");
	
	private TreeMap <String, String>hmRegExpEntries = new TreeMap<String, String>(); //regular expression
	private HashMap<String, HashMap<String,String>> hmNormEntries = new HashMap<String, HashMap<String,String>>(); //normalization

	HashMap<Pattern, String> hmRulePattern     = new HashMap<Pattern, String>(); // patterns in rules
	HashMap<String, String> hmRuleNormalization = new HashMap<String, String>(); // normalization target in rules
	HashMap<String, String> hmRuleLocation     = new HashMap<String, String>(); // location of the patterns


	public static ResourceUtilManager getInstance() {
		if(ResourceUtilManager.INSTANCE == null)
			ResourceUtilManager.INSTANCE = new ResourceUtilManager(RESOURCEDIR);	
		return ResourceUtilManager.INSTANCE;
	}
	
	public ResourceUtilManager(String resourcedir) {
		RESOURCEDIR=resourcedir;
		readResources(readResourcesFiles("norm"), normPattern, "norm");
		readResources(readResourcesFiles("regexp"), regexpPattern, "regexp");
		reformatRegExp();
		readResources(readResourcesFiles("rules"), rulePattern, "rules");	
	}

	/**
	 * Reads resource files of the type resourceType from the "used_resources.txt" file and returns a HashMap
	 * containing information to access these resources.
	 * @return HashMap containing filename/path tuples
	 */
	protected HashMap<String, String> readResourcesFiles(String resourceType) {

		HashMap<String, String> hmResources = new HashMap<String, String>();
		
		try{
			String resourcefile=RESOURCEDIR+"/used_resources.txt";
			Scanner sc = new Scanner(new File(resourcefile));
	 		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			Pattern paResource = Pattern.compile("\\./"+resourceType+"/resources_"+resourceType+"_"+"(.*?)\\.txt");
			for (Object r : findMatches(paResource, line)){
					MatchResult ro=(MatchResult) r;
					String foundResource  = ro.group(1);
					String pathToResource = RESOURCEDIR+"/"+resourceType+"/resources_"+resourceType+"_"+foundResource+".txt";
					System.out.println(foundResource);
					System.out.println(pathToResource);
					hmResources.put(foundResource, pathToResource);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			iv_logger.warn("Failed to read a resource from used_resources.txt.");
			System.exit(-1);
		}
		return hmResources;
	}
	
	private void readResources(HashMap<String, String> hmResources, Pattern p, String resourceType) {
		
		try {
			
			for (String resource : hmResources.keySet()) {
				iv_logger.info("Adding "+resourceType+" from resource: "+resource);
				Scanner sc = new Scanner(new File(hmResources.get(resource)));
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (!line.startsWith("//") && ! line.equals("")) {
						boolean correctLine = false;
						iv_logger.info("Reading "+resource+" at line: "+ line);
						for (Object r : findMatches(p, line)) {
							MatchResult mr = (MatchResult) r;
							correctLine = true;
							if(resourceType.equals("norm")) {
								String resource_word   = mr.group(1);
								String normalized_word = mr.group(2);
								boolean flag=true;
								for (Object key: hmNormEntries.keySet()) {
									String entry=(String) key;						
									if (resource.equals(entry)) {
										hmNormEntries.get(key).put(resource_word,normalized_word);
										flag=false;
									}
								}
								if(flag) {
									  HashMap <String, String> lhm=new HashMap <String, String>();
									  lhm.put(resource_word, normalized_word);
									  hmNormEntries.put(resource,lhm);
								}
							}
						   if(resourceType.equals("regexp")) {
							   String regexp_entry=mr.group(1);
							   if(regexp_entry.equals("")) continue;
							   boolean flag=true;
							   for (Object lentry : hmRegExpEntries.keySet()) {
							    String entry=(String) lentry;
								if (resource.equals(entry)) {
									String oldentries = (String) hmRegExpEntries.get(entry);
									oldentries = oldentries + "|" + regexp_entry;
									flag=false;
									hmRegExpEntries.put(entry, oldentries);
								}
							   }
								if(flag) hmRegExpEntries.put(resource,regexp_entry);
						   }
						   if(resourceType.equals("rules")){
							 	String rule_name          = mr.group(1);
								String rule_extraction    = mr.group(2);
								String rule_location	  = mr.group(3);
								String rule_normalization = mr.group(4);
								Pattern paVariable = Pattern.compile("%(re[a-zA-Z0-9]*)");
								for (Object o1 : findMatches(paVariable,rule_extraction)) {
									MatchResult mr1 = (MatchResult) o1;
									iv_logger.info("Replacing patterns..."+ mr1.group());
									System.out.println(mr1.group(1));
									if (!(hmRegExpEntries.containsKey(mr1.group(1)))) {
										iv_logger.error("Error creating rule:"+rule_name);
										iv_logger.error("The pattern may not exist : "+mr1.group(1));
										System.exit(-1);
									}
									rule_extraction = rule_extraction.replaceAll("%"+mr1.group(1), getRegExp(mr1.group(1)));
								}
								rule_extraction = rule_extraction.replaceAll(" ", "[\\\\s]+");
								System.out.println(rule_extraction);
								Pattern pattern = null;
								try{
									pattern = Pattern.compile(rule_extraction);
								}
								catch (java.util.regex.PatternSyntaxException e) {
									iv_logger.error("Cannot compile pattern in "+rule_name+": "+rule_extraction);
										e.printStackTrace();
										System.exit(-1);
									}

								hmRulePattern.put(pattern, rule_name);// get pattern part
								hmRuleNormalization.put(rule_name, rule_normalization);	//get normalization part
								hmRuleLocation.put(rule_name,rule_location); // get location part 
								
						}
						if ((correctLine == false) && (!(line.matches("")))) {
							iv_logger.error("Cannot read one of the lines of "+resource+" at Line: "+line);
						}
					}
				}
			}						
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	

	public final Boolean containsRegExpKey(String key) {
		return hmRegExpEntries.containsKey(key);
	}

	public final String getRegExp(String key) {
		return (String) hmRegExpEntries.get(key);
	}
	
	public final HashMap<Pattern, String> getHmRulePattern() {
		return hmRulePattern;
	}

	
	public final HashMap<String, String> getHmRuleNormalization() {
		return hmRuleNormalization;
	}
	
	public final HashMap<String, String> getHmRuleLocation() {
		return hmRuleLocation;
	}
	
	public final HashMap<String, String> getHmNormEntry(String key) {
		return hmNormEntries.get(key);
	}
	
	 
	private void reformatRegExp() {
		 for (Object lkey : hmRegExpEntries.keySet()) {
			    String key=(String) lkey;
			    String regexp = (String) hmRegExpEntries.get(key);				
		regexp = regexp.replaceAll("\\|\\|", "\\|");
		regexp = regexp.replaceAll("\\(([^\\?])", "(?:$1");
		regexp = regexp.replace("\\|$", "");
		regexp = "(" + regexp + ")";
		regexp = regexp.replaceAll("\\\\", "\\\\\\\\");
		hmRegExpEntries.put(key, regexp);
	}
}
	
	public static Iterable<MatchResult> findMatches(Pattern pattern, CharSequence s) {
		List<MatchResult> results = new ArrayList<MatchResult>();

		for (Matcher m = pattern.matcher(s); m.find();)
			results.add(m.toMatchResult());

		return results;
	}
	
	public static List<Pattern> sortByValue(final HashMap<Pattern,String> m) {
        List<Pattern> keys = new ArrayList<Pattern>();
        keys.addAll(m.keySet());
        Collections.sort(keys, new Comparator<Object>() {
        	public int compare(Object o1, Object o2) {
                Object v1 = m.get(o1);
                Object v2 = m.get(o2);
                if (v1 == null) {
                    return (v2 == null) ? 0 : 1;
                } else if (v1 instanceof Comparable) {
                    return ((Comparable) v1).compareTo(v2);
                } else {
                    return 0;
                }
            }
        });
        return keys;
    }
	
	/**
	 * Get the last tense used in the sentence
	 * 
	 * @param timex timex construct to discover tense data for
	 * @return string that contains the tense
	 */
	public static String getTense(ConceptMention cm, JCas jcas) {
	
		Sentence sen=cm.getSentence();
		String lastTense="";
		// Get the tokens
		TreeMap<Integer, BaseToken> tmToken = new TreeMap<Integer, BaseToken>();
		FSIterator iterToken = jcas.getAnnotationIndex(BaseToken.type).subiterator(sen);
		while (iterToken.hasNext()) {
			BaseToken token= (BaseToken) iterToken.next();
			tmToken.put(token.getEnd(), token);
		}
	
		for (Integer tokEnd : tmToken.keySet()) {
			if (tokEnd < cm.getBegin() || lastTense.equals("") && tokEnd > cm.getEnd()) {
				BaseToken token= tmToken.get(tokEnd);
				String pos=token.getPartOfSpeech();
				if (token.getPartOfSpeech() == null) {
					
				}
				else if (pos.matches("VBP|VBZ|VHP|VHZ|VVP|VVZ|VV")) {
					lastTense = "PRESENTFUTURE";
				}
				else if (pos.matches("V[BHVN]D|V[BH]N")) {
					lastTense = "PAST";
				}
				else if (pos.matches("MD")) {
					if (token.getCoveredText().matches("will|would|shall|should")) {
						lastTense = "FUTURE";
					}
				}
				if (token.getCoveredText().equals("since")) {
					lastTense = "PAST";
				}
			}
		}
		String prevPos = "";
		String longTense = "";
		for (Integer tokEnd : tmToken.keySet()) { 
		
		if (lastTense.equals("PRESENTFUTURE") && tokEnd < cm.getBegin() || lastTense.equals("") && tokEnd > cm.getEnd()) {
				if (tokEnd < cm.getBegin()) {
					BaseToken token= tmToken.get(tokEnd);
					if ((prevPos.equals("VHZ")) || (prevPos.equals("VBZ")) || (prevPos.equals("VHP")) || (prevPos.equals("VBP"))) {
						if (token.getPartOfSpeech().equals("VVN")) {
							if ((!(token.getCoveredText().equals("expected"))) && (!(token.getCoveredText().equals("scheduled")))) {
								lastTense = "PAST";
								longTense = "PAST";
							}
						}
					}
					prevPos = token.getPartOfSpeech();
				}
		}
		}	
		return lastTense;
	}
}
