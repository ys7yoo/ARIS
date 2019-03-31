package verbAnalyze;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.jawjaw.util.WordNetUtil;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Resnik;

public class LeskAnalyse {
	static RelatednessCalculator res;
	static{
		ILexicalDatabase db = new NictWordNet();
		Lesk lesk = new Lesk(db);
		res  = new Resnik(db);
	}
	
	public static void main(String[] args) {
		
		String w1 = "crack";
		String w2 = "find";

		
		List<Concept> allConcepts = getAllConcepts(w1, "v");
		List<Concept> allConcepts2 = getAllConcepts(w1, "v");
		
		System.out.println(res.calcRelatednessOfSynset(allConcepts.get(0),
				allConcepts2.get(1)));
		//
		// Synset
		System.out.println(res.calcRelatednessOfWords(w1, w2));
		System.out.println("sim: "+similarity(w1, w2));
		
	}

	public static List<Concept> getAllConcepts(String word, String posText) {
		POS pos = POS.valueOf(posText);
		List<edu.cmu.lti.jawjaw.pobj.Synset> synsets = WordNetUtil
				.wordToSynsets(word, pos);
		List<Concept> synsetStrings = new ArrayList<Concept>(synsets.size());
		for (edu.cmu.lti.jawjaw.pobj.Synset synset : synsets) {
			synsetStrings.add(new Concept(synset.getSynset(), POS.valueOf(pos
					.toString())));
		}
		return synsetStrings;
	}
	
	public static double similarity(String w1, String w2){
		double msim = -1;
		int i=1;
		List<Concept> l1 = getAllConcepts(w1, "v");
		List<Concept> l2 = getAllConcepts(w1, "v");
		for (Concept v1:l1){
			
			int j = 1;
			for (Concept v2:l2){
				double sim = res.calcRelatednessOfSynset(v1, v2).getScore()/(Math.log(i+Math.pow(j, 2)));
				if (sim>msim){
					msim = sim;
				}
				j = j+1;
			}
			i = i+1;
		}
		
		return msim;
	}

}
