package equationExtraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import verbAnalyze.SentenceAnalyzer;

public class World {
	// change of state is either done by an observation or an action
	boolean featImportant;
	ArrayList<State> states;
	ArrayList<ActionSpecifier> actionSpecs;
	public ArrayList<QuantitativeEntity> quantitativeEntities;
	static int stateNum;
	boolean startsWith;
	ActionSpecifier qas;
	boolean allObsDone = false;//checks if this is not the original copy of the world.
	//useful for testAllObs 
	
	public QuantitativeEntity qCEntity;

	public World(ArrayList<QuantitativeEntity> countedEntities,
			QuantitativeEntity qCEntity, boolean startsWith,
			boolean featImportant) {
		this(countedEntities, qCEntity, featImportant);

		// SentenceAnalyzer.println("s with: " + startsWith);
		this.startsWith = startsWith;
	}

	public World(ArrayList<QuantitativeEntity> countedEntities,
			QuantitativeEntity qCEntity, boolean featImportant) {
		this.featImportant = featImportant;
		this.qCEntity = qCEntity;
		this.quantitativeEntities = countedEntities;

		for (QuantitativeEntity cent : countedEntities) {
			cent.setUniqeIdx();
		}
		qCEntity.setUniqeIdx();

		formStates();

		// if (!MathCoreNLP.isWebAnalyzing) {
		// SentenceAnalyzer.println("eq ans: " + solveQuestion());
		// }
	}

	void formStates() {
		stateNum = 1;
		states = new ArrayList<State>();
		actionSpecs = new ArrayList<ActionSpecifier>();
		State lastState = null;
		boolean matched = false;
		for (int i = 0; i < quantitativeEntities.size(); i++) {
			QuantitativeEntity cent = quantitativeEntities.get(i);
			if (cent.questionMatch(qCEntity, featImportant)) {
				SentenceAnalyzer.println("matched "+cent.getName()+" "+qCEntity.getName());
				matched = true;
				State st = new State(cent, lastState, this);
				actionSpecs.add(st.aspec);
				lastState = st;
				states.add(st);
				stateNum++;
			}
			else{
				SentenceAnalyzer.println("not matched cent"+quantitativeEntities.get(i));
			}
		}

		// another closed world assumption
		// sth should match! or a mistake has been there??? But it will cause
		// some problems
		if (!matched) {
			for (int i = 0; i < quantitativeEntities.size(); i++) {
				QuantitativeEntity cent = quantitativeEntities.get(i);
				State st = new State(cent, lastState, this);
				actionSpecs.add(st.aspec);
				lastState = st;
				states.add(st);
				stateNum++;
			}
		}

	}
	
	public double solveQuestion() {
		qCEntity.setVerbid(qCEntity.getAf().getVerbid());
		qas = new ActionSpecifier(qCEntity, this);
		for (ActionSpecifier as : actionSpecs) {
			as.match(qas, false);
		}
		
		if (qCEntity.isSuperLative()) {
			double ans = handleSuperLatives();
			return ans;
		}
		
		if (qas.vmean != 0) {// action
			String var = null;
			ArrayList<ActionSpecifier> sortAS = findVariable(qas);
			for (ActionSpecifier as : sortAS) {
				// SentenceAnalyzer.println("sorted: "+as.centNum+" "+as.cent.isFakeEntity());
				if (as.matchScore > 0 && as.cent.isFakeEntity()) {
					var = as.centNum;
					break;
				}
			}

			MathCoreNLP.println("var is: " + var);

			String equation = extractEquation(var);
			MathCoreNLP.println("eq: " + equation);
			if (equation != null) {
				return solveEquation(equation, var);
			}
			
//			if (!allObsDone){
//				double ans = testAllObs();
//				if (ans!=0){
//					return ans;
//				}
//			}
			
			// var = null or eq was not found (maybe var was mistaken)
			
			double sum = 0;
			boolean matched = false;
			
			if (1==1){
				for (ActionSpecifier as : sortAS) {
					SentenceAnalyzer.println(as.centHolder + " : " + as.matchScore);
					if (as.matchScore != 0) {
						matched = true;
						
						
						int sign = (int)Math.signum(as.vmean);
						if (as.transferMatch){
							sign = (int)Math.signum(MathCoreNLP.getSecVmean(as.vmean));
						}
						
						if (sign == 0){
							sign = 1;
						}
						
						SentenceAnalyzer.println(as.verb+ " sign: "+sign);
						
						try {
							sum += Double.parseDouble(as.cent.getNum()) * sign;
						} catch (Exception e) {
//							e.printStackTrace();
							// maybe it was a mistaken var
						}
					}
				}
			}
			else{
				State lastState = states.get(states.size() - 1);
				equation = "X = ";

				matched = false;

				for (Holder h : lastState.holders) {
					if (h.qmatch(qas, true)) {
						matched = true;
						if (!h.equation.startsWith("-")) {
							equation += " +";
						}
						equation += " " + h.equation;
					}
				}
				SentenceAnalyzer.println("eq: "+equation);
				if (matched){
					return solveEquation(equation, "X");
				}
			}
			
			
			
			
			if (!matched) {
				SentenceAnalyzer.println("not matched...");
				for (ActionSpecifier aspec : actionSpecs) {
					aspec.match(qas, true);
					if (aspec.matchScore!=0){
						matched = true;
					}
					SentenceAnalyzer.println("score: " + aspec.matchScore);
				}
				sum = 0;

				for (ActionSpecifier as : actionSpecs) {
					if (matched && as.matchScore == 0) {
						continue;
					}
					// SentenceAnalyzer.println(as.centHolder + " : " +
					// as.matchScore);
						
						int sign = (int)Math.signum(as.vmean);
						
						if (as.transferMatch){
							sign = (int)Math.signum(MathCoreNLP.getSecVmean(as.vmean));
						}
						
						if (sign == 0){
							sign = 1;
						}
						
						try {
							sum += Double.parseDouble(as.cent.getNum()) * sign;
						} catch (Exception e) {
							// maybe it was a mistaken var
					}
				}

			}
			
			if (sum==0){
				State lastState = states.get(states.size() - 1);
				equation = "X = ";

				matched = false;

				for (Holder h : lastState.holders) {
					if (h.qmatch(qas, true)) {
						matched = true;
						if (!h.equation.startsWith("-")) {
							equation += " +";
						}
						equation += " " + h.equation;
					}
				}
				SentenceAnalyzer.println("eq: "+equation);
				if (matched){
					return solveEquation(equation, "X");
				}
			}
			
			return sum;

		} else {// observation
				// first lets forget about start! so, just the last
			if (startsWith) {
				// SentenceAnalyzer.println("starts with");
				String var = qas.centHolder + "_start";
				String equation = extractEquation(var);
				MathCoreNLP.println("eq: " + equation);
				return solveEquation(equation, var);
			} else {
				State lastState = states.get(states.size() - 1);
				String equation = "X = ";

				boolean matched = false;
				for (Holder h : lastState.holders) {
					if (h.qmatch(qas, true)) {
						matched = true;
						if (!h.equation.startsWith("-")) {
							equation += " +";
						}
						equation += " " + h.equation;
					}
				}

				// closed world assumption for the final holderName
				ArrayList<Holder> usedHolders = new ArrayList<Holder>();
				if (!matched) {
					SentenceAnalyzer.println("not matched");
					for (ActionSpecifier aspec : actionSpecs) {
						aspec.match(qas, true);
						if (aspec.matchScore!=0){
							matched = true;
						}
						SentenceAnalyzer.println("score: " + aspec.matchScore);
					}
					double sum = 0;
					
					for (ActionSpecifier as : actionSpecs) {
						if (matched && as.matchScore == 0) {
							continue;
						}
						 SentenceAnalyzer.println(as.centHolder + " : " +
						 as.matchScore);
							
							int sign = (int)Math.signum(as.vmean);
							SentenceAnalyzer.println("sign "+sign+" "+as.transferMatch);
							if (as.transferMatch){
								sign = (int)Math.signum(MathCoreNLP.getSecVmean(as.vmean));
							}
							
							if (sign == 0){
								sign = 1;
							}
							
							try {
								sum += Double.parseDouble(as.cent.getNum()) * sign;
							} catch (Exception e) {
								// maybe it was a mistaken var
						}
					}
					
					if (1==1){
						return sum;
					}
					
					for (Holder h : lastState.holders) {
						if (h.qmatch(qas, false)) {
							matched = true;
							Holder comHolder = null;
							for (Holder hh:usedHolders){
								if (hh.compHolderNames.contains(h.holderName)){
									comHolder= hh;
									break;
								}
							}
							if (comHolder!=null){//TODO: it has problem...
								SentenceAnalyzer.println("eq1: "+comHolder.equation);
								SentenceAnalyzer.println("eq2: "+h.equation);
								if (comHolder.equation.length()<h.equation.length()){
									equation = "X = ";
								}
								else{
									continue;
								}
							}
							usedHolders.add(h);
							if (!h.equation.startsWith("-")) {
								equation += " +";
							}
							equation += " " + h.equation;
						}
					}
				}
				MathCoreNLP.println("eq: " + equation);

				return solveEquation(equation, "X");
			}

		}

	}
	
	//if an equation could not be found for an action verb, set all verbs to obs and try again
	//may solve questions like : There were 12 game, win some. Lost 4. How many win?
	//Not used for now
	double testAllObs(){
		for (QuantitativeEntity cent:quantitativeEntities){
			if (!cent.isFakeEntity()){
				cent.getAf().getVerbid().setLemma("have");
				cent.getVerbid().setLemma("have");
			}
			
		}
		World w = new World(quantitativeEntities, qCEntity, featImportant);
		w.allObsDone = true;
		SentenceAnalyzer.println("new world:"+w);
		return w.solveQuestion();
	}

	double handleSuperLatives() {
		ArrayList<ActionSpecifier> aspecs = new ArrayList<ActionSpecifier>();
		for (ActionSpecifier as : actionSpecs) {
			if (!as.cent.isFakeEntity()) {
				aspecs.add(as);
			}
		}
		if (aspecs.size() >= 2) {
			int size = aspecs.size();
			double d1 = Double.parseDouble(aspecs.get(size - 1).cent.getNum());
			double d2 = Double.parseDouble(aspecs.get(size - 2).cent.getNum());
			return Math.abs(d1 - d2);
		}
		return 0;
	}

	public static double solveEquation(String eq, String var) {
		if (eq == null) {
			return 0;
		}
		Scanner sc = new Scanner(eq);
		int side = 1;
		int sign = 1;
		int varCoef = 0;
		double sum = 0;
		while (sc.hasNext()) {
			String s = sc.next();
			double i = -1;
			boolean isInt = true;
			try {
				i = Double.parseDouble(s);
			} catch (Exception e) {
				isInt = false;
			}
			if (s.equals("=")) {
				side = -1;
				sign = 1;
				continue;
			}
			if (isInt) {
				sum = sum + side * sign * i;
			}

			else if (s.equals("-")) {
				sign = -1;
			} else if (s.equals("+")) {
				sign = +1;
			} else if (s.equals(var)) {

				varCoef += side * sign;
			}
		}
		// SentenceAnalyzer.println("var coef: " + varCoef);
		// SentenceAnalyzer.println("sum is: " + sum);
		sum *= -1;
		if (varCoef == 0) {
			return 0;
		} else {
			return sum / varCoef;
		}

	}

	String extractEquation(String var) {

		if (var != null) {
			String bestEq = null;
			for (int i = states.size() - 1; i > 0; i--) {
				// MathCoreNLP.println("v "+actionSpecs.get(i).verb);
				if (actionSpecs.get(i).vmean != 0) {
					// MathCoreNLP.println("not zero:"+actionSpecs.get(i).verb);
					continue;
				} else {
					// MathCoreNLP.println(actionSpecs.get(i).verb);
				}

				for (Holder h : states.get(i).holders) {
					
					Holder hm = searchForHolder(h, states.get(i - 1));
					// MathCoreNLP.println(hm==null);
					if (hm != null) {
						if (!hm.equation.contains(var)) {// TODO: check for
							continue;
						}
						if (h.equation.trim().equals(hm.equation.trim())) {
							bestEq = h.equation + " = " + hm.equation;
							continue;
						}
						return h.equation + " = " + hm.equation;
					}
				}
				// check for fill-in-the-blanks
				for (Holder h : states.get(i).holders) {

					Holder hm = searchForHolder(h, states.get(i - 1));
					// MathCoreNLP.println(hm==null);
					if (hm != null) {
						if (h.equation.trim().equals(hm.equation.trim())) {
							continue;
						}
						// the equations are not the same

						if (!hm.equation.contains(var)) {// TODO: check for
															// trivial sol

							return h.equation + " = " + hm.equation + " + "
									+ var;
						}

					}
				}
			}
			return bestEq;
			// } else {// another close world assumption
			// // if question is asking about an action, and all the verbs are
			// // about obs,
			// // it should be an implicit action
			// if (qas.vmean != 0) {
			// boolean cworld = true;
			// for (ActionSpecifier as : actionSpecs) {
			// if (as.vmean != 0) {
			// cworld = false;
			// break;
			// }
			// }
			// if (cworld) {
			// var = "X";
			// // check for fill-in-the-blanks
			// for (int i = states.size() - 1; i > 0; i--) {
			// for (Holder h : states.get(i).holders) {
			//
			// Holder hm = searchForHolder(h, states.get(i - 1));
			// // MathCoreNLP.println(hm==null);
			// if (hm != null) {
			// if (h.equation.trim()
			// .equals(hm.equation.trim())) {
			// continue;
			// }
			// // the equations are not the same
			//
			// if (!hm.equation.contains(var)) {// TODO: check
			// // for
			// // trivial
			// // sol
			//
			// return h.equation + " = " + hm.equation
			// + " + " + var;
			// }
			//
			// }
			// }
			//
			// }
			//
			// }
			// }
		}
		return null;
	}

	Holder searchForHolder(Holder h, State s) {
		for (Holder h2 : s.holders) {
			if (h.match(h2)) {
				return h2;
			}
		}
		return null;
	}

	ArrayList<ActionSpecifier> findVariable(ActionSpecifier qas) {
		for (ActionSpecifier as : actionSpecs) {
			as.match(qas, false);
		}
		ArrayList<ActionSpecifier> sorting = new ArrayList<ActionSpecifier>();
		for (ActionSpecifier as : actionSpecs) {
			sorting.add(as);
		}
		Collections.sort(sorting);
		Collections.reverse(sorting);
		return sorting;
	}
	
	public String toString() {
		String ret = "World:\n"+quantitativeEntities.size();
		for (State s : states) {
			ret += s.toString() + "\n\n";
		}
		ret += qCEntity.getAf();
		return ret;
	}
}
