package equationExtraction;

import problemAnalyser.Entity;
import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import problemAnalyser.QuestionAnalyzer;
import verbAnalyze.SentenceAnalyzer;
import edu.stanford.nlp.util.Pair;

public class ActionSpecifier implements Comparable {
	boolean subjIsPerson;
	boolean centIobjisPerson;
	String verb;
	int vmean;
	QuantitativeEntity cent;

	String centNum;
	String centHolder;
	String centIobj;
	String centPlace;

	int matchScore = 0;
	boolean transferMatch = false;
	World w;

	public ActionSpecifier(QuantitativeEntity cent, World w) {
		this.w = w;
		Entity subj = cent.getAf().getSubject();
		subjIsPerson = subj != null && subj.isPerson();
		this.cent = cent;
		verb = "null";
		if (cent.getVerbid() != null) {
			verb = cent.getVerbid().lemma();
			vmean = QuestionAnalyzer.getVerbMean(cent);
		}
		subj = cent.getAf().getSubject();

		centNum = cent.getNum();
		centHolder = cent.getSubject();
		// MathCoreNLP.println("1st "+centSubj);

		centPlace = "PLACE";
		if (cent.getAf().getPlace() != null) {
			centPlace = cent.getAf().getPlace().getName();
		}
		SentenceAnalyzer.println(centHolder+ " is meaningful: "+cent.getAf().isSubjMeaningFul()
				+" "+cent.getAf().getSubject());
		if (!cent.getAf().isSubjMeaningFul()) {
			centHolder = centPlace;
			// closed world solution
			if (centHolder.equals("PLACE") && w.states.size() > 0) {
				State st = w.states.get(w.states.size() - 1);
				for (Holder h : st.holders) {
					if (!h.isPerson && !h.holderName.equals("PLACE")) {
						centHolder = h.holderName;
						SentenceAnalyzer.println(h.isPerson);
						SentenceAnalyzer.println("setting holder name to "+centHolder);
					}
				}
			}
		}
		
		//some minimal coref
		int pr;
		if ((pr = checkPronoun(centHolder))!=-1){
			boolean isPerson = pr==1;
			if (w.states.size() > 0) {
				State st = w.states.get(w.states.size() - 1);
				for (Holder h : st.holders) {
					if (h.isPerson == isPerson && !h.holderName.equals("PLACE")) {
						centHolder = h.holderName;
					}
				}
			}
		}

		// SentenceAnalyzer.println("is passive: "+cent.getAf().isPassive());
		if (cent.isQuestion() && cent.getAf().isPassive()) {
			if (!cent.getAf().isSubjMeaningFul()
					|| !cent.getAf().getSubject().isPerson()) {
				centHolder = "they";
				// SentenceAnalyzer.println("set to they");
			}
		}

		centIobj = null;
		if (MathCoreNLP.hasTwoHolders(vmean)) {
			cent.getAf().setIobj();
			Entity iobj = cent.getAf().getIobj();
			subjIsPerson = subj != null && subj.isPerson();
			centIobjisPerson = iobj!=null && iobj.isPerson();
			Entity ent = cent.getAf().getIobj();
			centIobj = ent != null ? ent.getName() : null;
			
			if (centIobj == null
					|| (!cent.getAf().getIobj().isPerson() && !centHolder
							.equals(centPlace)) && !cent.getAf().isPassive()) {
//				SentenceAnalyzer.println("centholder: "+centHolder+" p "+centPlace);
				if (!centPlace.equals(centHolder)){
					centIobj = centPlace;
				}
				else{
					centIobj = "PLACE";
				}
			}
			// closed world assumption
			if (centIobj.equals("PLACE") && w.states.size() > 0) {
				State st = w.states.get(w.states.size() - 1);
				// search for a location!
				for (Holder h : st.holders) {
					if (!h.isPerson && !h.holderName.equals("PLACE")
							&& !h.holderName.equals(centHolder)) {
						// SentenceAnalyzer.println("set iobj from "+
						// centIobj+" to "+h.holderName);

						centIobj = h.holderName;
						
					}
				}
				
//			  	// if not such a thing found, search for a person!
				if (centIobj.equals("PLACE")) {
					for (Holder h : st.holders) {
						if (h.isPerson && !h.holderName.equals("PLACE")
								&& !h.holderName.equals(centHolder)) {
							// SentenceAnalyzer.println("set iobj from "+
							// centIobj+" to "+h.holderName);

							centIobj = h.holderName;

						}
					}
				}
			}
		}
		SentenceAnalyzer.println("in aspec iobj: "+centIobj+" "+this.vmean);
		if (cent.isFakeEntity()) {
			centNum = Holder.getNewVariable(centHolder);
		}

	}
	
	//check pronoun
	int checkPronoun(String s){//1: Person, 2: Object
		if (s.equals("it") || s.equals("that") || s.equals("this")){
			return 2;
		}
		else if (s.equals("he") || s.equals("she")){
			return 1;
		}
		return -1;
	}
	
	// exact match
	// subj not match
	// cases like verb not match, but sign match ...
	// built for actions, mainly
	// TODO: assumption here
	public void match(ActionSpecifier as, boolean matchPronounWithPersons) {
		// SentenceAnalyzer.println("matching qv:"+as.vmean+" thisv"+vmean+" qsu "+as.centHolder+
		// " this subj: "+centHolder);
		double vsim = 0;
		try {
			vsim = MathCoreNLP.vsims.get(new Pair<String, String>(as.verb, this.verb));
			if (as.vmean == 0){//Why should we ignore sth because of its verb for obs?!
				vsim = 1;
			}
		} catch (Exception e) {
		
		}
		
		SentenceAnalyzer.println("vsim of " + as.verb + " " + verb + " " + vsim);
		
//		if (vmean == 0) {
//			return;
//		}
		//TODO: this holder match should not be just for the 1st holder?
		
		
		if (verb.equals(as.verb)) {
			int hs = holderMatch(as, matchPronounWithPersons);
			if (hs == 1) {
				matchScore = 10;
			} else if (hs == 0) {
				matchScore = 2;
			}
		} else if (vmean * as.vmean >= 0 || vsim>.2 ) {
			SentenceAnalyzer.println("the 2nd if in match");
			int hscore = holderMatch(as, matchPronounWithPersons);
		 	if (hscore == 1) {
				matchScore = 7;
				if (hscore == 2){
					transferMatch = true;
				}
			} else if (holderMatch(as, matchPronounWithPersons) == 0) {
				matchScore = 1;
			}
		 	SentenceAnalyzer.println("match score is: "+matchScore);
		}
		
		//previously it was else if
		//TODO: check this matchscore == 0. OK with ixl and a. Accepted to remove
		
		if (/*isTransfer() && matchScore==0 &&*/ vmean * as.vmean<=0) {
			if (holderMatch(as, matchPronounWithPersons) == 2) {
				matchScore = 7;
				transferMatch = true;
			} else if (holderMatch(as, matchPronounWithPersons) == 0) {
				// 0: because a place has receive from someone unknown!
				// matchScore = 1;

			}
		}
	}
	
	int holderMatch(ActionSpecifier as, boolean matchPronounWithPersons) {
		// centHolder.equals(as.centHolder)
		if (holderNameMatch(centHolder, as) || ((matchPronounWithPersons && subjIsPerson == as.subjIsPerson))) {
			return 1;
		} else if (isTransfer()) {
			// centIobj.equals(as.centHolder) && verb.equals(as.verb)
			if (holderNameMatch(centIobj, as) || (matchPronounWithPersons && centIobjisPerson == as.subjIsPerson)) {
				// matchScore = 10;
				return 2;
			}
		}
		
//		if (/*!subjIsPerson || */) {
//			return 0;
//		}
		SentenceAnalyzer.println("subj isp: "+ subjIsPerson);
		return -1;
	}

	boolean holderNameMatch(String s, ActionSpecifier as) {
		if (s.equals(as.centHolder) || as.centHolder.equals("they")) {
			return true;
		}
		if (as.centHolder.equals("he") || as.centHolder.equals("she")){
			if (subjIsPerson){
				return true; 
			}
		}
//		if (matchPronounWithPersons){
//			if (centIobjisPerson == as.subjIsPerson){
//				SentenceAnalyzer.println("isperson is the same");
//				return true;
//			}
//		}
		return false;
	}
	
	boolean isTransfer() {
		return MathCoreNLP.hasTwoHolders(vmean);
	}

	@Override
	public int compareTo(Object o) {
		ActionSpecifier as = (ActionSpecifier) o;
		if (matchScore > as.matchScore) {
			return 1;
		} else if (matchScore < as.matchScore) {
			return -1;
		}
		return 0;
	}
}
