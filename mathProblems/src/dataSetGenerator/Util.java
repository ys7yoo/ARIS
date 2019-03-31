package dataSetGenerator;

import java.util.ArrayList;

import edu.stanford.nlp.ie.NumberNormalizer;

public class Util {
	public static String dollarProcess(String s){
		if (s.startsWith("$")){
			return s.substring(1);
		}
		return s;
	}
	
}
