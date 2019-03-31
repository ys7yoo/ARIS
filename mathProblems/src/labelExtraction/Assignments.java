package labelExtraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.stanford.nlp.ling.Word;
import equationExtraction.World;
import problemAnalyser.QuantitativeEntity;
import problemAnalyser.QuestionAnalyzer;

public class Assignments {
	public ArrayList<Assignment> assignments;
	public static void main(String[] args) {
	}
	
	public Assignments() {
		assignments = new ArrayList<Assignment>();
	}

	public Assignments(QuestionLabel ql) {
		assignments = new ArrayList<Assignment>();
		ArrayList<QuantitativeEntity> cents = ql.cents;
		QuantitativeEntity qent = ql.qent;
		HashSet<String> verbs = new HashSet<String>();
		for (QuantitativeEntity cent : cents) {
//			System.out.println("verbii: "+cent.getVerbid().lemma());
			if (QuestionAnalyzer.getVerbMean(cent) != 0) {
				if (cent.getVerbid()!=null){
					System.out.println("added");
					verbs.add(cent.getVerbid().lemma());
				}
			}
		}
		if (QuestionAnalyzer.getVerbMean(qent) != 0) {
			if (qent.getVerbid()!=null){
				verbs.add(qent.getVerbid().lemma());
			}
		}
		
		ArrayList<String> verbsl = new ArrayList<String>();
		for (String s : verbs) {
			verbsl.add(s);
		}
		for (int i=0; i<Math.pow(2,verbsl.size()); i++){
			Assignment assignment = getAssignment(i, verbsl);
			if (checkFeasibility(assignment, ql)){
				System.out.println("feas: "+i);
				decToBin(i, verbsl.size());
				System.out.println();
				assignments.add(assignment);
			}
		}
		System.out.println("feasible assignments: ");
		for (Assignment ass:assignments){
			System.out.println(ass);
		}
	}

	Assignment getAssignment(int dec, ArrayList<String> verbsl) {
		int[] binary = decToBin(dec, verbsl.size());
		HashMap<String, Integer> ass = new HashMap<String, Integer>();
		for (int i=0; i<verbsl.size(); i++){
			int l = binary[i]==1?2:-2;
			ass.put(verbsl.get(i), l);
		}
		return new Assignment(ass);
	}

	public static int[] decToBin(int a, int size) {
		int b[] = new int[size];
		int i = size-1;
		while (a != 0) {
			b[i] = a%2;
			i--;
			a/=2;
		}
		for (int j=0; j<size; j++) {
			System.out.print(b[j]);
		}
		return b;
	}
	
	boolean checkFeasibility(Assignment ass, QuestionLabel ql){
		for (QuantitativeEntity cent:ql.cents){
			if (cent.getVerbid()!=null){
				cent.setPreferredVmean(ass.getLabel(cent.getVerbid().lemma()));
			}
		}
		if (ql.qent.getVerbid()!=null){
			ql.qent.setPreferredVmean(ass.getLabel(ql.qent.getVerbid().lemma()));
		}
		World w = new World(ql.cents,ql.qent, false);
		double wans = Math.abs(w.solveQuestion());
		return wans == ql.correctAns;
	}
	
	public void merge(Assignments ass){
		ArrayList<Assignment> merged = new ArrayList<Assignment>();
		if (assignments.size()==0){
			merged = ass.assignments;
			this.assignments = merged;
			return;
		}
		for (Assignment as:this.assignments){
			for (Assignment as2:ass.assignments){
				Assignment mas = as.merge(as2);
				boolean shouldAdd = true;
				if (mas!=null){
					for (Assignment s:merged){
						if (s.equals(mas)){
							shouldAdd = false;
							break;
						}
					}
					if (shouldAdd){
						merged.add(mas);
					}
				}
				
			}
		}
		this.assignments = merged;
	}
	
	public String toString(){
		String ret = "";
		for (int i=0; i<assignments.size(); i++){
			ret+= i+": \n";
			ret+= assignments.get(i).toString();
			ret+="\n";
		}
		return ret;
	}
	
}
