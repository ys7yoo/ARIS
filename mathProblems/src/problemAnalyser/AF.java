package problemAnalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import verbAnalyze.SentenceAnalyzer;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;

public class AF {
	Entity subject;
	private Entity iobj;// used for transfer, not just iobj

	Entity time;
	Entity place;
	IndexedWord verbid;
	ArrayList<String> verbRels;
	boolean passive = false;
	static HashSet<String> acceptablePlaceRels;
	static HashSet<String> acceptableEachStrs;
	boolean ibojSet = false;

	private ArrayList<QuantitativeEntity> cents;
	Collection<TypedDependency> dependencies;
	SemanticGraph dependenciesTree;
	AFSentenceAnalyzer afs;
	ArrayList<EachRelation> eachrelations;

	static {
		acceptablePlaceRels = new HashSet<String>();
		acceptablePlaceRels.add("prep_in");
		acceptablePlaceRels.add("prep_at");
		acceptablePlaceRels.add("prep_on");
		acceptablePlaceRels.add("prep_into");
		acceptablePlaceRels.add("prep_out_of");
		
		acceptableEachStrs = new HashSet<String>();
		acceptableEachStrs.add("each");
		acceptableEachStrs.add("an");
		acceptableEachStrs.add("a");
	}

	public AF(AFSentenceAnalyzer afs, SemanticGraph dependenciesTree,
			IndexedWord verbid) {
		this.afs = afs;
		this.dependencies = afs.dependencies;
		this.dependenciesTree = dependenciesTree;
		cents = new ArrayList<QuantitativeEntity>();
		this.verbid = verbid;
		eachrelations = new ArrayList<EachRelation>();
		// MathCoreNLP.println(verbid.index());
		// if (verbid != null) {
		// this.verb = afs.getLemma(verbid.index());
		// } else {
		// MathCoreNLP.println("null verb");
		// }
	}

	public void resolveAF() {
		setTime();
		setSubject();
		setPlace();
		setIobj();
		resolveDoDid();
		setVerbRels();
		
		setEachRelations();
	}

	void setEachRelations() {
		for (TypedDependency tde : dependencies) {
			if (tde.reln().toString().equals("det")
					&& acceptableEachStrs.contains(tde.dep().nodeString()
							.toLowerCase())) {
				// SentenceAnalyzer.println("each seen for "+tde.gov().nodeString());
				Entity ent1 = new Entity(tde.gov(), afs);
				if (afs.usedEachIds.contains(ent1.index)) {
					continue;
				}
				String n1 = 1 + "";
				int dis = 100000;
				Entity ent2 = null;
				String n2 = "";
				for (QuantitativeEntity cent : cents) {
					if (Math.abs(cent.entity.index - ent1.index) < dis) {
						dis = Math.abs(cent.entity.index - ent1.index);
						ent2 = cent.entity;
						n2 = cent.num;
					}
				}
				if (ent2 != null) {
					// SentenceAnalyzer.println("n1: "+n1);
					// SentenceAnalyzer.println("n2: "+n2);
					// SentenceAnalyzer.println("ent1: "+ent1);
					// SentenceAnalyzer.println("ent2: "+ent2);
					EachRelation er = new EachRelation(ent1, ent2, n1, n2);
					eachrelations.add(er);
					afs.usedEachIds.add(ent1.index);
				}
			}
		}
	}

	void setVerbRels() {
		verbRels = new ArrayList<String>();
		if (verbid == null) {
			return;
		}
		for (TypedDependency tde : dependencies) {
			if (tde.gov().index() == verbid.index()) {
				String reln = tde.reln().toString();
				verbRels.add(reln);
				if (!MathCoreNLP.verbRelsCounts.containsKey(reln)) {
					MathCoreNLP.verbRelsCounts.put(reln, 0);
				}
				int c = MathCoreNLP.verbRelsCounts.get(reln);
				MathCoreNLP.verbRelsCounts.put(reln, c + 1);
			}
		}
		// SentenceAnalyzer.println("vRels: " + verbRels);
	}

	public ArrayList<String> getVerbRels() {
		return verbRels;
	}

	void resolveDoDid() {
		if (cents.get(0).isQuestion && verbid != null) {
			if (verbid.lemma().equals("did") || verbid.lemma().equals("do")) {
				for (TypedDependency td : dependencies) {
					if (td.reln().toString().equals("dobj")) {
						if (td.gov().index() == verbid.index()) {

							verbid = afs.getIndexedWord(td.dep().index());
							MathCoreNLP.println("herell " + verbid.lemma());
							for (QuantitativeEntity cent : cents) {
								cent.verbid = verbid;
							}
							subject = getNPersontoIdxedWord(verbid);
							break;
						}
					}

				}
			}
		}
	}

	void setTime() {
		if (verbid == null) {
			return;
		}
		for (TypedDependency tde : dependencies) {
			if (tde.reln().toString().equals("tmod")) {
				if (tde.gov().index() == verbid.index()) {
					time = new Entity(tde.dep(), afs);
				}
			}
		}
	}

	void setPlace() {
		if (verbid == null) {
			return;
		}
		SentenceAnalyzer.println("setting vid for "+verbid.index());
		int distanceToVerb = -1;
		for (TypedDependency tde : dependencies) {
			String reln = tde.reln().toString();
			if (acceptablePlaceRels.contains(reln)) {
//				if (tde.gov().index() == cents.get(0).entity.index){
//					place = new Entity(tde.dep(), afs);
//					return;
//				}
				List<SemanticGraphEdge> path = dependenciesTree
						.getShortestDirectedPathEdges(afs.getIndexedWord(cents.get(0).entity.index),
								afs.getIndexedWord(tde.dep().index()));
				SentenceAnalyzer.println("path: "+path+" for "+tde.dep().index());
				if (distanceToVerb == -1
						|| (path != null && path.size() < distanceToVerb)) {
//					if (path.size()>1){
//						continue;
//					}
					place = new Entity(tde.dep(), afs);
					if (path != null) {
						distanceToVerb = path.size();
					}
				}
			}
		}
	}

	void setSubject() {
		if (verbid == null) {
			return;
		}
		String selectedReln = "";
		for (TypedDependency tde : dependencies) {
			String reln = tde.reln().toString();
			if (reln.equals("nsubj") || reln.equals("agent")
					|| reln.equals("auxpass")) {
				// agent is better than auxpass!!!
				if (selectedReln.equals("agent") && reln.equals("auxpass")) {
					continue;
				}
				selectedReln = reln;

				if (tde.gov().index() == verbid.index()) {
					subject = new Entity(tde.dep(), afs);
					int subjIdx = tde.dep().index();
					// MathCoreNLP.println("it is "+subjIdx+" "+afs.getWordInfo(subjIdx));
					if (afs.getWordInfo(subjIdx).NE.equals("PERSON")) {
						subject.isPerson = true;
					}

					if (reln.equals("auxpass") || reln.equals("agent")) {
						passive = true;
					}

					if (selectedReln.equals("nsubj")) {
						boolean shouldBreak = true;
						// if the subj is meaningful, break. (i.e. subj!=ent)
						for (int i = 0; i < cents.size(); i++) {
							if (subject.name.equals(cents.get(i).getName())) {
								shouldBreak = false;
							}
						}
						if (shouldBreak) {
							break;
						}
					}
				}

			}
		}
		if (subject != null && subject.isPerson) {
			afs.usedPersonIds.add(subject.index);
		}
		if (subject == null){
			for (TypedDependency tde : dependencies) {
				if (tde.reln().toString().equals("nsubj")){
					IndexedWord prevVerbid = verbid;
					verbid = afs.getIndexedWord(tde.gov().index());
					setSubject();
					verbid = prevVerbid;
					return;
				}
			}
		}
	}

	public void setIobj() {
		int distanceToVerb = -1;
		boolean isPersonFound = false;
		for (TypedDependency tde : dependencies) {
			String reln = tde.reln().toString();
			if (reln.equals("iobj") || reln.equals("dobj")) {
				if (tde.gov().index() == verbid.index()) {
					List<SemanticGraphEdge> path = dependenciesTree
							.getShortestDirectedPathEdges(verbid,
									afs.getIndexedWord(tde.dep().index()));
					Entity ent = new Entity(tde.dep(), afs);
					if (distanceToVerb == -1
							|| (path != null && path.size() < distanceToVerb)
							|| (ent.isPerson && !isPersonFound)) {

						if (isPersonFound && !ent.isPerson) {
							continue;
						}
						if (ent.isPerson) {
							isPersonFound = true;
						}
						iobj = ent;
						int iobjIdx = tde.dep().index();
						// MathCoreNLP.println("it is "+subjIdx+" "+afs.getWordInfo(subjIdx));

					}
				}
			}
		}

		// search for nsubjpass to be the iobj! e.g. A ship is filled with...
		if (iobj == null) {
			SentenceAnalyzer.println("setting iobj with passive");
			for (TypedDependency tde : dependencies) {
				String reln = tde.reln().toString();
				if (reln.equals("nsubjpass")) {
					if (verbid != null && tde.gov().index() != verbid.index()) {
						continue;
					}
					iobj = new Entity(tde.dep(), afs);
					SentenceAnalyzer.println("iobj set to: " + iobj.name);
					return;
				}

			}
		}

		// SentenceAnalyzer.println("it is: "+iobj);
		// find the nearest person to verb
		if (iobj == null) {
			Entity ent = getNPersontoIdxedWord(verbid);
			if (ent != null
					&& (subject == null || !ent.name.equals(subject.name))
					&& !afs.usedPersonIds.contains(ent.index)) {
				iobj = ent;
			}
		} else if (!iobj.isPerson) {
			Entity ent = getNPersontoIdxedWord(afs.getIndexedWord(iobj.index));
			if (ent != null
					&& (subject == null || !ent.name.equals(subject.name))
					&& !afs.usedPersonIds.contains(ent.index)) {
				iobj = ent;
			}
		}

	}

	public Entity getIobj() {
		if (!ibojSet) {
			setIobj();
			ibojSet = true;
		}
		return iobj;
	}

	Entity getNPersontoIdxedWord(IndexedWord first) {
		int distanceToVerb = -1;
		Entity ret = null;
		for (TypedDependency tde : dependencies) {
			List<TreeGraphNode> personNodes = new ArrayList<TreeGraphNode>();
			if (afs.getWordInfo(tde.gov().index()).NE.equals("PERSON")) {
				personNodes.add(tde.gov());
			}
			if (afs.getWordInfo(tde.dep().index()).NE.equals("PERSON")) {
				personNodes.add(tde.dep());
			}
			for (TreeGraphNode tgn : personNodes) {
				List<SemanticGraphEdge> path = dependenciesTree
						.getShortestDirectedPathEdges(first,
								afs.getIndexedWord(tgn.index()));
				if (distanceToVerb == -1
						|| (path != null && path.size() < distanceToVerb)) {
					ret = new Entity(tgn, afs);
					if (path != null) {

						distanceToVerb = path.size();
					}
				}
			}
		}
		return ret;
	}

	public String toString() {
		String str = "";
		str += ("verb: " + ((verbid != null) ? verbid.lemma() : "null")) + "\n";
		str += ("subject: " + ((subject != null) ? subject.toString() : "null"))
				+ "\n";
		str += ("time: " + (time != null ? time.toString() : "null")) + "\n";
		str += ("place: " + (place != null ? place.toString() : "null")) + "\n";
		str += ("counted entities: ") + "\n";
		for (QuantitativeEntity cent : cents) {
			str += "\n" + (cent.toString()) + "\n";
		}
		if (eachrelations.size() > 0) {
			str += "erels: \n";
			for (EachRelation er : eachrelations) {
				str += er;
			}
		}

		return str;
	}

	public void addCent(QuantitativeEntity cent) {
		cents.add(cent);
		cent.af = this;
	}

	public ArrayList<QuantitativeEntity> getCents() {
		return cents;
	}

	public Entity getSubject() {
		return subject;
	}

	public Entity getTime() {
		return time;
	}

	public Entity getPlace() {
		return place;
	}

	public IndexedWord getVerbid() {
		return verbid;
	}

	public Collection<TypedDependency> getDependencies() {
		return dependencies;
	}

	public SemanticGraph getDependenciesTree() {
		return dependenciesTree;
	}

	public AFSentenceAnalyzer getAfs() {
		return afs;
	}

	public boolean isPassive() {
		return passive;
	}

	public boolean isSubjMeaningFul() {
		if (subject == null) {
			return false;
		}
		if (subject.getName().equals("be")){
			return false;
		}
		if (subject.isPerson) {
			return true;
		}
		if (afs.dependencies.toString().contains("expl")) {
			return false;
		}
		if (subject.getName()!=null && 
				subject.getName().equals(cents.get(0).getNum())){
			SentenceAnalyzer.println("our case");
			return false;
		}
//		if (subject.name.equals(cents.get(0).entity.name)){
//			return false;
//		}
		for (int i = 0; i < cents.size(); i++) {
			if (subject.name.equals(cents.get(i).getName())) {
				String num = cents.get(i).getNum();
				if (num!=null && !num.equals("1") && !num.equals("2")){
					return false;
				}
			}
		}
		return true;
	}

	// public boolean isIobjMeaningFul() {
	// if (Math.abs(QuestionAnalyzer.getVerbMean(this.cents.get(0))) != 2) {
	// return true;
	// }
	// if (iobj == null) {
	// return false;
	// }
	// if (iobj.isPerson) {
	// return true;
	// }
	//
	// for (int i = 0; i < cents.size(); i++) {
	// if (iobj.name.equals(cents.get(i).getName())) {
	// return false;
	// }
	// }
	// return true;
	// }

	public List<EachRelation> getEachRelations() {
		return eachrelations;
	}

	public void filterEachRelations(HashSet<String> entNames) {
		// ArrayList<EachRelation> newEachRels = new ArrayList<EachRelation>();
		// for (EachRelation er:eachrelations){
		// if (entNames.contains(er.ent1.name)){
		// newEachRels.add(er);
		// }
		// }
		// eachrelations = newEachRels;
	}
}
