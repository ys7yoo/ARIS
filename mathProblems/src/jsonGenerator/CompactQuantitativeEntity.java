package jsonGenerator;

import java.util.ArrayList;

import problemAnalyser.Entity;
import problemAnalyser.QuantitativeEntity;
import edu.stanford.nlp.ling.IndexedWord;

public class CompactQuantitativeEntity {
	boolean isQuestion = false;
	boolean foundRealEntity;// it is mainly used for questions to
	// find out whether the qent is matched with previous ents
	boolean unknownEntity = false;
	String num;
	String pathToVerb;
	CompactEntity entity;
	
	CompactEntity subject;
	CompactEntity iobj;// used for transfer, not just iobj
	
	CompactEntity time;
	CompactEntity place;
	String verb;
	ArrayList<String> verbRels;
	int uniqueIdx;
	
	public CompactQuantitativeEntity(QuantitativeEntity qent) {
		isQuestion = qent.isQuestion();
		foundRealEntity = qent.isFoundRealEntity();
		unknownEntity = qent.isFakeEntity();
		num = qent.getNum();
		pathToVerb = qent.getPathToVerb();
		entity = new CompactEntity(qent.getEntity());
		subject = new CompactEntity(qent.getAf().getSubject());
		iobj = new CompactEntity(qent.getAf().getIobj());
		time = new CompactEntity(qent.getAf().getTime());
		place = new CompactEntity(qent.getAf().getPlace());
		if (qent.getAf().getVerbid()!=null){
			verb = qent.getAf().getVerbid().lemma();
		}
		this.verbRels = qent.getAf().getVerbRels();
		this.uniqueIdx = qent.getUniqeIdx();
	}
	public int getUniqueIdx() {
		return uniqueIdx;
	}
	public CompactQuantitativeEntity(boolean isQuestion, boolean foundRealEntity,
			boolean unknownEntity, String num, String pathToVerb, CompactEntity entity,
			CompactEntity subject, CompactEntity iobj, CompactEntity time, CompactEntity place, ArrayList<String> verbRels,
			int uniqueIdx){
		this.isQuestion = isQuestion;
		this.foundRealEntity = foundRealEntity;
		this.unknownEntity = unknownEntity;
		this.num = num;
		this.pathToVerb = pathToVerb;
		this.entity = entity;
		this.subject = subject;
		this.iobj = iobj;
		this.time = time;
		this.place = place;
		this.verbRels = verbRels;
		this.uniqueIdx = uniqueIdx;
	}
	
	public String toString() {
		String ret = "";
		ret += entity.toString() + "\n";
		ret += "quantity: " + num + "\n";
		ret += "relatoin to verb: " + pathToVerb;
		ret += "vrels: "+verbRels;
		return ret;
	}
	public boolean isQuestion() {
		return isQuestion;
	}
	public boolean isFoundRealEntity() {
		return foundRealEntity;
	}
	public boolean isUnknownEntity() {
		return unknownEntity;
	}
	public String getNum() {
		return num;
	}
	public String getPathToVerb() {
		return pathToVerb;
	}
	public CompactEntity getEntity() {
		return entity;
	}
	public CompactEntity getSubject() {
		return subject;
	}
	public CompactEntity getIobj() {
		return iobj;
	}
	public CompactEntity getTime() {
		return time;
	}
	public CompactEntity getPlace() {
		return place;
	}
	public String getVerb() {
		return verb;
	}
	public ArrayList<String> getVerbRels() {
		return verbRels;
	}
}
