package equationExtraction;

import java.util.HashMap;
import java.util.HashSet;

import problemAnalyser.QuantitativeEntity;

public class Holder {
	String holderName;
	String equation;
	QuantitativeEntity cent;
	boolean isPerson;
	boolean featImportant;
	static HashMap<String, Integer> holderToNum = new HashMap<String, Integer>();// used
	HashSet<String> compHolderNames = new HashSet<String>();
	
	public Holder(String holderName, String equation, QuantitativeEntity cent, boolean isPerson, boolean featImportant,
			HashSet<String> compHolderNames ) {
		this.compHolderNames = compHolderNames;
		this.featImportant = featImportant;
		this.isPerson = isPerson;
		this.holderName = holderName;
		this.equation = equation;
		this.cent = cent;
	}

	public Holder getCopy() {
		QuantitativeEntity centCopy = cent.getCopy();
		HashSet<String> comSet = new HashSet<String>();
		for (String s:compHolderNames){
			comSet.add(s);
		}
		Holder h = new Holder(holderName, equation, centCopy, isPerson, featImportant, comSet);
		return h;
	}
	
	public String toString(){
		String ret = "";
		ret = holderName+" has "+equation+" "+cent.getEntity().toString();
		return ret;
	}
	public static String getNewVariable(String centSubj){
		int variableNum;
		if (Holder.holderToNum.containsKey(centSubj)) {
			variableNum = Holder.holderToNum.get(centSubj);
		} else {
			variableNum = 1;
		}
		Holder.holderToNum.put(centSubj, variableNum + 1);
		return centSubj + "_" + variableNum;
	}
	
	public boolean match(Holder h){
		return h.holderName.equals(holderName) && cent.questionMatch(h.cent, featImportant);
	}
	
	public boolean qmatch(ActionSpecifier qas, boolean shouldCheckHolderName){
//		if (!cent.questionMatch(qas.cent, featImportant)){
//			System.out.println("cent not matched");
//			return false;
//		}
		if (!shouldCheckHolderName && isPerson == qas.subjIsPerson){
			System.out.println("isperson is the same");
			return true;
		}
		if (qas.centHolder.equals(holderName)){
			return true;
		}
		if (qas.centHolder.equals("they") && isPerson){
			return true;
		}
		return false;
	}
}
