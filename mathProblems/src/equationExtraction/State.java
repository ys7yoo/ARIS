package equationExtraction;

import java.util.ArrayList;
import java.util.HashSet;

import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import verbAnalyze.SentenceAnalyzer;

public class State {
	ArrayList<Holder> holders;
	QuantitativeEntity cent;
	// String associatedParamter;//the parameter created here
	// ArrayList<String> associatedStartParameters;//the start parameters
	ActionSpecifier aspec;
	World w;

	public State(QuantitativeEntity cent, State lastState, World w) {
		this.w = w;
		this.cent = cent;
		holders = new ArrayList<Holder>();
		if (lastState != null) {
			for (Holder h : lastState.holders) {
				holders.add(h.getCopy());
			}
		}
		aspec = new ActionSpecifier(cent, w);

		boolean firstEqSeen = false;
		boolean secEqSeen = !(MathCoreNLP.hasTwoHolders(aspec.vmean));
		
		boolean subjIsPerson = aspec.subjIsPerson;

		String centNum = aspec.centNum;
		String centHolder = aspec.centHolder;
		String centIobj = aspec.centIobj;
		SentenceAnalyzer.println("in state iobj: " + centIobj);

		
		for (int i = 0; i < holders.size(); i++) {
			Holder h = holders.get(i);
			if (h.cent.questionMatch(cent, w.featImportant)) {
				if (aspec.vmean == 0) {
					if (centHolder.equals(h.holderName)) {
						firstEqSeen = true;
						//Consecutive Obs Assumption
						// check two consecutive observations for one holder
						// it should be a sum! e.g. "I have 2 pens on the desk,
						// I have three on the table..."
						boolean shouldAdd = false;
						if (w.actionSpecs.size() > 0) {
							ActionSpecifier paspec = w.actionSpecs
									.get(w.actionSpecs.size() - 1);
							if ((paspec.cent.getUniqeIdx() == cent.getUniqeIdx()-1) && paspec.vmean == 0
									&& paspec.centHolder
											.equals(aspec.centHolder)) {
								shouldAdd = true;
							}
						}
						if (!shouldAdd){
							holders.get(i).equation = centNum;
						}
						else{
							String eq = holders.get(i).equation;
							eq += " + " + centNum;
							holders.get(i).equation = eq;
						}
						break;
					}

				} else if (aspec.vmean == 1 || aspec.vmean == -1) {
					if (centHolder.equals(h.holderName)) {
						firstEqSeen = true;
						String eq = holders.get(i).equation;
						eq += (aspec.vmean == 1 ? " + " : " - ") + centNum;
						holders.get(i).equation = eq;
						break;
					}

				} else if (MathCoreNLP.hasTwoHolders(aspec.vmean)) {

					if (centHolder.equals(h.holderName)) {
						firstEqSeen = true;
						String eq = holders.get(i).equation;
						eq += (aspec.vmean < 0 ? " - " : " + ") + centNum;
						holders.get(i).equation = eq;
						break;
					}
					// TODO: assumption of contains needed?
					// TODO: still have an assumption about place:
					// we have a PLACE as pholder and now in transfer, we again
					// find PLACE as the sec side
					else if ((centIobj.equals(h.holderName))) {
						secEqSeen = true;
						String eq = holders.get(i).equation;
						int secVmean = MathCoreNLP.getSecVmean(aspec.vmean);
						eq += (secVmean>=0 ? " + " : " - ") + centNum;
						holders.get(i).equation = eq;
						break;
					}
					// else if (h.holderName.equals("PLACE")) {// It may be the
					// // place...
					// firstEqSeen = true;
					// String eq = holders.get(i).equation;
					// eq += (af.vmean == 1 ? " + " : " - ") + centNum;
					// holders.get(i).equation = eq;
					// break;
					// }
				}
			}

		}

		String preEq = "";
		Holder firstHolder = null;
		if (!firstEqSeen) {
			if (aspec.vmean != 0) {
				preEq = centHolder + "_start ";
			}
			String eq = preEq
					+ (aspec.vmean >= 0 ? (preEq.equals("") ? "" : "+ ") : "- ")
					+ centNum;
			HashSet<String> compSet = new HashSet<String>();
			Holder h = new Holder(centHolder, eq, cent, subjIsPerson, w.featImportant, compSet);
			firstHolder = h;
			holders.add(h);
		}
		if (!secEqSeen) {
			if (aspec.vmean != 0) {
				preEq = centIobj + "_start ";
			}
			
			int secVmean = MathCoreNLP.getSecVmean(aspec.vmean);
			
			String eq = preEq
					+ (secVmean >= 0 ? "+ " : "- ")
					+ centNum;
			try {
				MathCoreNLP.println("iobjjj: " + centIobj);
			} catch (Exception e) {
				// TODO: handle exception
			}
			HashSet<String> compSet = new HashSet<String>();
			Holder h = new Holder(centIobj, eq, cent, subjIsPerson, w.featImportant, compSet);
			if (firstHolder != null){
				h.compHolderNames.add(firstHolder.holderName);
				firstHolder.compHolderNames.add(h.holderName);
			}
			holders.add(h);
		}
	}

	public String toString() {
		String ret = "";
		// ret += "for cent: " + cent.getAf()+"\n";
		for (Holder h : holders) {
			ret += h.toString() + "\n";
		}
		return ret;
	}
}
