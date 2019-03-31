package labelExtraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Assignment {
	HashMap<String, Integer> vlabels;
	public Assignment(HashMap<String, Integer> vlabels) {
		this.vlabels = vlabels;
	}
	int getLabel(String s){
		System.err.println("vlabels: "+vlabels);
		System.err.println("s: "+s);

		if (!vlabels.containsKey(s)){
			return 0;
		}
		return vlabels.get(s);
	}
	
	public String toString(){
		String ret = "";
		for (String s:vlabels.keySet()){
			ret += "|"+s+" = "+vlabels.get(s);
		}
		
		return ret;
	}
	
	Assignment merge(Assignment as){
		HashMap<String, Integer> vls = new HashMap<String, Integer>();
		for (String s:vlabels.keySet()){
			if (as.vlabels.containsKey(s)){
				if (as.vlabels.get(s)!=vlabels.get(s)){
					return null;
				}
			}
			vls.put(s, vlabels.get(s));
		}
		for (String s:as.vlabels.keySet()){
			if (!vls.containsKey(s)){
				vls.put(s, as.vlabels.get(s));
			}
		}
		return new Assignment(vls);
	}
	
	public boolean equals(Assignment as){
		if (vlabels.size()!=as.vlabels.size()){
			return false;
		}
		for (String s:vlabels.keySet()){
			if (!as.vlabels.containsKey(s)){
				return false;
			}
			if (as.vlabels.get(s)!=vlabels.get(s)){
				return false;
			}
		}
		return true;
	}
}
