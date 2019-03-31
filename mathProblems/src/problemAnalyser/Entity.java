package problemAnalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;

public class Entity {
	String name;
	int index;
	static String[] processedRelNames = new 
			String[]{"num", "amod", "nn"};
	List<String> nns;
	List<String> amods;
	AFSentenceAnalyzer afs;
	List<String> otherRels;
	List<String> otherRelsNames;
	WordInfo wi;
	boolean isPerson;
	//identifies whether the entity is a person or not, mainly used for subj
	
	
	public Entity() {//for makeCopy
		
	}
	
	public boolean isPerson() {
		return wi.NE.equals("PERSON");
	}

	public Entity(TreeGraphNode tdnode, AFSentenceAnalyzer afs) {
		this(tdnode.index(),afs);
		index = tdnode.index();
		this.name = afs.getLemma(index);
	}
	// when there is no name for the counted entity! e.g., I have 3.
	public Entity(int numIndex, AFSentenceAnalyzer afs) {
		this.index = numIndex;
		this.afs = afs;
		nns = new ArrayList<String>();
		amods = new ArrayList<String>();
		otherRels = new ArrayList<String>();
		otherRelsNames = new ArrayList<String>();
		addRelateds(afs.dependencies, "amod", amods);
		amods.remove("more");
		addRelateds(afs.dependencies, "nn", nns);
		addRelateds(afs.dependencies, "", otherRels);
		wi = afs.getWordInfo(index);
		this.isPerson = wi.NE.equals("PERSON");
	}
	
	void addRelateds(Collection<TypedDependency> dependencies, String relName, List<String> relateds){
		for (TypedDependency tde:dependencies){
			String tdeRelName = tde.reln().toString();
			boolean shouldIgnore = false;
			if (relName.equals("")){
				for (String s:processedRelNames){
					if (s.equals(tdeRelName)){
						shouldIgnore = true;
						break;
					}
				}
				if (shouldIgnore){
					continue;
				}
			}
			if ((tdeRelName.equals(relName) || relName.equals("") ) && Util.getNumber(tde.gov()) == index){
				String o = afs.getLemma(tde.dep().index());
				String oRelName = tde.reln().getShortName();
				if (o.equals("the") || oRelName.equals("poss")){
					continue;
				}
				relateds.add(o);
				if (relName.equals("")){
					otherRelsNames.add(oRelName);
				}
			}
		}
	}
	
	public String getName() {
		return name;
	}

	public int getIndex() {
		return index;
	}

	public static String[] getProcessedRelNames() {
		return processedRelNames;
	}

	public List<String> getNns() {
		return nns;
	}

	public List<String> getAmods() {
		return amods;
	}

	public AFSentenceAnalyzer getAfs() {
		return afs;
	}

	public List<String> getOtherRels() {
		return otherRels;
	}

	public List<String> getOtherRelsNames() {
		return otherRelsNames;
	}

	public WordInfo getWi() {
		return wi;
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
//		if (otherRels.size()>0){
//			str += "others: ";
//			for (int i=0; i<otherRels.size(); i++){
//				str += otherRels.get(i)+" ("+otherRelsNames.get(i)+") ";
//			}
//		}
		str += "name: "+ name+" idx_"+index;
		return str;
	}
	
	public void absorbFeatures(Entity en, boolean isQuestion){
		this.name = en.name;
		if ((this.amods.size()==0 && this.nns.size()==0)
				|| isQuestion){
			Util.getCopy(this.amods, en.amods);
			Util.getCopy(this.nns, en.nns);
		}
		this.wi = en.wi;
	}
	
	public boolean match(Entity ent1){
		if (!Util.SEqual(ent1.name, name)){
			return false;
		}
		if (!Util.listEqual(amods, ent1.amods) ||
				!Util.listEqual(amods, ent1.amods)){
			return false;
		}
		return true;
	}
	
	public Entity getCopy(){
		Entity ret = new Entity();
		ret.name = name;
		ret.index = index;
		ret.nns = Util.getCopy(nns);
		ret.amods = Util.getCopy(amods);
		ret.otherRels = Util.getCopy(otherRels);
		ret.otherRelsNames = Util.getCopy(otherRelsNames);
		return ret;
	}
}
