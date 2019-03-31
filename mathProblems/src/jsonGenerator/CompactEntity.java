package jsonGenerator;

import java.util.List;

import problemAnalyser.AFSentenceAnalyzer;
import problemAnalyser.Entity;

public class CompactEntity {
	String name;
	int index;
	
	List<String> nns;
	List<String> amods;
	List<String> otherRels;
	List<String> otherRelsNames;
	boolean isPerson;
	public CompactEntity(Entity ent) {
		if (ent==null){
			return;
		}
		name = ent.getName();
		index = ent.getIndex();
		nns = ent.getNns();
		amods = ent.getAmods();
		otherRels = ent.getOtherRels();
		otherRelsNames = ent.getOtherRelsNames();
		isPerson = ent.isPerson();
	}
	public CompactEntity(String name, int index, List<String> nns, List<String> amods, List<String> otherRels, List<String> otherRelsNames, boolean isPerson) {
		this.name = name;
		this.index = index;
		this.nns = nns;
		this.amods = amods;
		this.otherRels = otherRels;
		this.otherRelsNames = otherRelsNames;
		this.isPerson = isPerson;
	}
	
	public String toString(){
		String str = "";
		if (amods.size()>0){
			str += "amods: ";
			for (String s:amods){
				str += s+" ";
			}
		}
		if (nns.size()>0){
			str += "nns: ";
			for (String s:nns){
				str += s+" ";
			}
		}
		str += "name: "+ name+" idx_"+index;
		return str;
	}
}
