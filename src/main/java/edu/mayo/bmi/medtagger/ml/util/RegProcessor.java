package edu.mayo.bmi.medtagger.ml.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegProcessor {
	private static String symbols = "[=@`\\^\\]\\[\\$&*+><~#%\\(\\)/\\.,;:\\-!?'\"]+";
	
	public static String removePunc(String string){
		Pattern pattern = Pattern.compile(symbols);
		Matcher matcher = pattern.matcher(string);
		boolean found = matcher.find();
		while(found){	
			String foundMatch = matcher.group();
			string = string.replace(foundMatch, "");
			found = matcher.find();
		}
		return string;
	}
	
	/**
	 * 
	 * @param title
	 * @return
	 */
	public static String polishTitle(String title){
		title = RegProcessor.removePunc(title);
		title = title.replaceAll("\\s+"," ");
		if(title.contains("the")){
			title = title.replaceAll(" the", "");
		}
		return title;
	}
	
	/**
	 * 
	 * @param aString
	 * @param str4find
	 * @return
	 */
	public static int findAllStrOccur(String aString,String str4find){
		Pattern p = Pattern.compile("\\b"+str4find+"\\b", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(aString);
	    // indicate all matches on the line
		int count=0;
	    while (m.find()) {
	        System.out.println("Word was found at position " + 
	                       m.start() + " on a string "+aString);
	        count++;
	    }
	    return count;
	}
	
	public static boolean containsString(String original, String tobeChecked, boolean caseSensitive)
	{
	    if (caseSensitive)
	    {
	        return original.contains(tobeChecked);

	    }
	    else
	    {
	        return original.toLowerCase().contains(tobeChecked.toLowerCase());
	    }

	}
}
