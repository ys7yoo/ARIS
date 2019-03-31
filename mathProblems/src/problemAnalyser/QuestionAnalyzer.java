package problemAnalyser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import verbAnalyze.SentenceAnalyzer;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import equationExtraction.World;

public class QuestionAnalyzer {

	static PrintStream output_op;

	ArrayList<AFSentenceAnalyzer> afslist;
	ArrayList<AF> allAFS;
	ArrayList<QuantitativeEntity> allCents;
	ArrayList<QuantitativeEntity> resolvedCents;
	QuantitativeEntity questionedEntity;
	boolean startsWith = false;

	List<CoreMap> sentences;
	ArrayList<EachRelation> eachRelations;

	HashMap<String, Integer> centToNums = new HashMap<String, Integer>();
	int length;

	public QuestionAnalyzer(String line, StanfordCoreNLP pipeline) {
		if (output_op == null) {
			try {
				output_op = new PrintStream(new File("output_op.txt"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		eachRelations = new ArrayList<EachRelation>();
		afslist = new ArrayList<AFSentenceAnalyzer>();
		allCents = new ArrayList<QuantitativeEntity>();
		allAFS = new ArrayList<AF>();
		resolvedCents = new ArrayList<QuantitativeEntity>();
		MathCoreNLP.println("############################################");
		MathCoreNLP.println(line);

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(line);
		pipeline.annotate(document);
		// MathCoreNLP.println("0: "+(System.currentTimeMillis()-time0));

		// It is used for the sake of not having error when first replacing
		// corefs and then parsing. But then it was removed!!! because there
		// was no difference. However, if you want use it you should not that
		// subject NE (PERSON) may have problem.
		ArrayList<ArrayList<String>> tokenNames = new ArrayList<ArrayList<String>>();
		ArrayList<String> fractions = new ArrayList<String>();
		line = MathCoreNLP.numberPreProcess(document, fractions);
		// line = line.replace("," ,".");

		output_op.println(line);

		document = new Annotation(line);
		MathCoreNLP.println("coref processed: " + line);
		// MathCoreNLP.println("1: "+(System.currentTimeMillis()-time0));
		pipeline.annotate(document);
		String corefStr = MathCoreNLP.corefPreProcess(document, tokenNames);
		// corefStr = corefStr.replace("," ,".");
		// ArrayList<String>
		document = new Annotation(corefStr);
		pipeline.annotate(document);
		SentenceAnalyzer.println("it is: " + corefStr);
		// SentenceAnalyzer.println("line is: "+line);

		// run all Annotators on this text

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys
		// and
		// has values with custom types
		sentences = document.get(SentencesAnnotation.class);
		// this.length = sentences.size();
		this.length = 0;
		// MathCoreNLP.println("2: "+(System.currentTimeMillis()-time0));

		// MathCoreNLP.println("chains:");
		// for (CorefChain ch: graph.values()){
		// MathCoreNLP.println(ch);
		// }
		String lastpart = sentences.get(sentences.size() - 1).toString()
				.toLowerCase();
		if (lastpart.contains("start") || lastpart.contains("begin")) {
			startsWith = true;
		}
		MathCoreNLP.println("last part: " + lastpart);
		if (lastpart.contains("how many") || lastpart.contains("how much")) {
			MathCoreNLP.howmmnum++;
		} else {
			MathCoreNLP.println("without howmm: " + line);
		}
		if (lastpart.contains("?")) {
			MathCoreNLP.numQ++;
		}
		int sentenceNum = 0;
		HashSet<String> allSeenCentNames = new HashSet<String>();
		int sidx = 0;
		// SentenceAnalyzer.println("sen size: "+sentences.size());
		for (CoreMap sentence : sentences) {
			length += sentence.size();

			AFSentenceAnalyzer afs = new AFSentenceAnalyzer(sentence,
					tokenNames.get(sidx), allSeenCentNames,
					sentences.size() > 1 && sidx == sentences.size() - 1);
			afslist.add(afs);

			// Tree root = null;
			// for (TypedDependency td: dependencies){
			//
			// // MathCoreNLP.println(td);
			// // MathCoreNLP.println(td.dep());
			// // MathCoreNLP.println("children: "+td.dep().children());
			// //// MathCoreNLP.println("parent: "+td.dep().pa );
			// // MathCoreNLP.println(td.gov());
			// // MathCoreNLP.println(td.reln());
			//
			//
			//
			// // MathCoreNLP.println("dependencies: "+td.gov().dep);
			//
			// }
			sidx++;
		}
		setAllAFandCents(fractions);
		postProcessQuestion(afslist.get(afslist.size() - 1), questionedEntity);
		// setQuestionedEntity(sentences.get(sentences.size() - 1));
	}
	
	// if the question asks about something, and it has occurred just once,
	// and it has num "one" or "two", try to set the qname to sth else
	void postProcessQuestion(AFSentenceAnalyzer afs, QuantitativeEntity cent) {

		String qname = cent.getName();
		SentenceAnalyzer.println("nnnn " + qname + " " + centToNums.get(qname));

		String num = null;
		for (QuantitativeEntity c : allCents) {
			SentenceAnalyzer.println("name: " + c.getName());
			if (c.getName().equals(qname)) {
				num = c.getNum();
				if (num != null) {
					break;
				}
			}
		}

		SentenceAnalyzer.println("the num is: " + num);

		if (num != null) {
			if (num.equals("1") || num.equals("2")) {

				for (String s : centToNums.keySet()) {
					if (centToNums.get(s) > 1) {
						if (!s.equals(qname)) {
							cent.entity.name = s;
							return;
						}
					}
				}
			}
		}

	}

	public QuestionAnalyzer(String line, StanfordCoreNLP pipeline,
			boolean hasQuestion) {
		eachRelations = new ArrayList<EachRelation>();
		afslist = new ArrayList<AFSentenceAnalyzer>();
		allCents = new ArrayList<QuantitativeEntity>();
		allAFS = new ArrayList<AF>();
		resolvedCents = new ArrayList<QuantitativeEntity>();
		MathCoreNLP.println("############################################");
		MathCoreNLP.println(line);

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(line);
		pipeline.annotate(document);
		// MathCoreNLP.println("0: "+(System.currentTimeMillis()-time0));

		// It is used for the sake of not having error when first replacing
		// corefs and then parsing. But then it was removed!!! because there
		// was no difference. However, if you want use it you should not that
		// subject NE (PERSON) may have problem.
		ArrayList<ArrayList<String>> tokenNames = new ArrayList<ArrayList<String>>();
		String corefStr = MathCoreNLP.corefPreProcess(document, tokenNames);
		// ArrayList<String>
		document = new Annotation(corefStr);
		pipeline.annotate(document);
		ArrayList<String> fractions = new ArrayList<String>();
		line = MathCoreNLP.numberPreProcess(document, fractions);
		document = new Annotation(line);
		MathCoreNLP.println("coref processed: " + line);
		pipeline.annotate(document);

		sentences = document.get(SentencesAnnotation.class);

		HashSet<String> allSeenCentNames = new HashSet<String>();
		int sidx = 0;
		;
		for (CoreMap sentence : sentences) {

			// AFSentenceAnalyzer afs = new AFSentenceAnalyzer(sentence,
			// tokenNames.get(sidx), allSeenCentNames,
			// sidx == sentences.size() - 1 && hasQuestion &&
			// sentences.size()>1);

			AFSentenceAnalyzer afs = new AFSentenceAnalyzer(sentence,
					tokenNames.get(sidx), allSeenCentNames,
					sentences.size() > 1 && sidx == sentences.size() - 1);

			afslist.add(afs);

			sidx++;
		}
		setAllAFandCents(fractions);
	}

	void setAllAFandCents(ArrayList<String> fractions) {
		int usedFractions = 0;
		allAFS = new ArrayList<AF>();
		for (AFSentenceAnalyzer afs : afslist) {
			for (AF af : afs.aflist.values()) {
				allAFS.add(af);
				for (QuantitativeEntity cent : af.getCents()) {
					allCents.add(cent);

					String num = cent.getNum();
					if (num != null && num.equals("3003")) {
						num = fractions.get(usedFractions++);
						cent.num = num;
					}
				}
			}
		}

		for (QuantitativeEntity cent : allCents) {
			if (cent.fakeEntity || cent.getName() == null) {
				continue;
			}

			if (!centToNums.containsKey(cent.getName())) {
				centToNums.put(cent.getName(), 0);
			}
			centToNums.put(cent.getName(), centToNums.get(cent.getName()) + 1);
		}

		setEntityforNonEntities();
		ImproveAmodDependenciesFromDeps();

		HashSet<String> allCentNames = new HashSet<String>();
		int qNum = allCents.size() - 1;
		for (int i = allCents.size() - 1; i >= 0; i--) {
			if (allCents.get(i).isQuestion) {
				qNum = i;
				break;
			}
			allCentNames.add(allCents.get(i).entity.name);
		}
		questionedEntity = allCents.remove(qNum);
		for (AFSentenceAnalyzer afs : afslist) {
			for (AF af : afs.aflist.values()) {
				af.filterEachRelations(allCentNames);
				for (EachRelation er : af.getEachRelations()) {
					eachRelations.add(er);
				}
			}
		}

	}

	// If previously you have seen sth as amod and now u see it as dep, it
	// should be amod
	// Same for nns
	void ImproveAmodDependenciesFromDeps() {
		HashSet<String> allAmods = new HashSet<String>();
		HashSet<String> allNNs = new HashSet<String>();
		for (QuantitativeEntity cent : allCents) {
			for (int i = 0; i < cent.entity.otherRels.size(); i++) {
				String s = cent.entity.otherRels.get(i);
				if (allAmods.contains(s)
						&& cent.entity.otherRelsNames.get(i).equals("dep")) {
					cent.entity.otherRels.remove(i);
					cent.entity.otherRelsNames.remove(i);
					cent.entity.amods.add(s);
				}
				if (allNNs.contains(s)) {
					cent.entity.otherRels.remove(i);
					cent.entity.otherRelsNames.remove(i);
					cent.entity.nns.add(s);
				}
			}
			for (String s : cent.entity.amods) {
				allAmods.add(s);
			}
			for (String s : cent.entity.nns) {
				allNNs.add(s);
			}
			for (int i = 0; i < cent.entity.otherRels.size(); i++) {
				String s = cent.entity.otherRels.get(i);
				if (allAmods.contains(s)
						&& cent.entity.otherRelsNames.get(i).equals("dep")) {
					cent.entity.otherRels.remove(i);
					cent.entity.otherRelsNames.remove(i);
					cent.entity.amods.add(s);
				}
				if (allNNs.contains(s)) {
					cent.entity.otherRels.remove(i);
					cent.entity.otherRelsNames.remove(i);
					cent.entity.nns.add(s);
				}
			}
			for (String s : cent.entity.amods) {
				allAmods.add(s);
			}
			for (String s : cent.entity.nns) {
				allNNs.add(s);
			}
		}

		QuantitativeEntity cent = allCents.get(allCents.size() - 1);
		int manyMuchIdx = -1;
		for (TypedDependency td : cent.af.afs.dependencies) {
			String depName = td.dep().nodeString().toLowerCase();
			if ((depName.equals("many") || depName.equals("much"))) {
				manyMuchIdx = td.dep().index();
				break;
			}
		}

		SentenceAnalyzer.println("manymuchIdx: " + manyMuchIdx);

		HashSet<String> manyMuchAmods = new HashSet<String>();
		String relName = "amod";
		for (TypedDependency td : cent.af.afs.dependencies) {
			String tdeRelName = td.reln().toString();
			if ((tdeRelName.equals(relName) || relName.equals(""))
					&& Util.getNumber(td.dep()) == manyMuchIdx) {
				String o = cent.af.afs.getLemma(td.gov().index());
				String oRelName = td.reln().getShortName();
				if (o.equals("the") || oRelName.equals("poss")) {
					continue;
				}
				manyMuchAmods.add(o);

			}
		}

		for (String s : manyMuchAmods) {
			if (allAmods.contains(s) && !cent.entity.amods.contains(s)) {
				cent.entity.amods.add(s);
			}
		}

	}

	// handle examples like "I have three."
	void setEntityforNonEntities() {
		for (int i = 0; i < allCents.size(); i++) {
			QuantitativeEntity cent = allCents.get(i);
			if (cent.entity.name == null
					|| (cent.isQuestion && !cent.foundRealEntity)) {
				int nearestCentIdx = -1;

				ArrayList<String> lemmas = cent.getAf().afs.lemmas;
				for (String centName : centToNums.keySet()) {
					SentenceAnalyzer.println("centName: " + centName);
					for (int j = 0; j < lemmas.size(); j++) {
						String s = lemmas.get(j);
						// SentenceAnalyzer.println("s: " + s);
						if (s.equals(centName)) {
							if (nearestCentIdx == -1
									|| Math.abs(nearestCentIdx - cent.numIndex) > Math
											.abs(j - cent.numIndex)) {
								nearestCentIdx = j;
							}
						}
					}
				}

				if (nearestCentIdx != -1) {
					cent.entity.name = lemmas.get(nearestCentIdx);
					if (cent.entity.amods.size() > 0) {
						continue;
					}
					for (TypedDependency td : cent.af.afs.dependencies) {
						if (td.gov().index() == nearestCentIdx + 1) {
							cent.entity = new Entity(td.gov(), cent.af.afs);
						} else if (td.dep().index() == nearestCentIdx + 1) {
							cent.entity = new Entity(td.dep(), cent.af.afs);
						}
					}
					continue;
				}
				nearestCentIdx = -1;
				for (int j = 1; j < allCents.size() && nearestCentIdx == -1; j++) {
					if (i - j >= 0) {
						String name = allCents.get(i - j).entity.name;
						if (name != null) {
							nearestCentIdx = i - j;
							continue;
						}
					}
					if (i + j < allCents.size()) {
						String name = allCents.get(i + j).entity.name;
						if (name != null) {
							nearestCentIdx = i + j;
							continue;
						}
					}
				}
				if (nearestCentIdx != -1) {
					cent.entity.absorbFeatures(
							allCents.get(nearestCentIdx).entity,
							cent.isQuestion);
				}

			}
		}

	}

	public String toString() {
		String ret = "";
		for (AFSentenceAnalyzer afs : afslist) {
			ret += "\n";
			ret += (afs.sentence + "\n\n");
			ret += (afs.dependencies + "\n");
			ret += afs.toString() + "\n";
		}
		return ret;
	}

	double worldGetAnswer() {
		// Let's see what happens in holder solution
		for (QuantitativeEntity cent : allCents) {
			simplemakeAction(cent);
		}
		boolean featuresImportant = !shouldIgnoreFeatures();
		SentenceAnalyzer.println("feat imp: " + featuresImportant);
		World w = new World(allCents, questionedEntity, startsWith,
				featuresImportant);
		MathCoreNLP.println(w);
		double ans = w.solveQuestion();
		;
		SentenceAnalyzer.println("eq ans: " + ans);
		return Math.abs(ans);
	}

	double simpleguessAnswer() {
		for (QuantitativeEntity cent : allCents) {
			simplemakeAction(cent);
		}
		boolean featuresImportant = !shouldIgnoreFeatures();

		// int equalVerbsCount = 0;
		// for (QuantitativeEntity cent : allCents) {
		// try {
		// if (cent.questionMatch(questionedEntity, featuresImportant)
		// && cent.verbid.lemma().equals(
		// questionedEntity.verbid.lemma())) {
		// equalVerbsCount++;
		// }
		// } catch (Exception e) {
		//
		// }
		// }

		// boolean eqVerbProb = false;// equalVerbsCount>=2; //==
		// allCents.size() -
		// // 1 && equalVerbsCount >= 2 &&
		// // CoreNLP.verbMean.get(questionedEntity.verbid.lemma())!=0;
		// if (eqVerbProb) {
		// MathCoreNLP.println("vow equal verb count!");
		// }

		// sameSetofFeatures = false;//TODO: handle not same features

		MathCoreNLP.println("resolved cents1: ");
		for (QuantitativeEntity cent : resolvedCents) {
			MathCoreNLP.println(cent);
		}
		BigDecimal X = new BigDecimal(0);
		String subj = null;
		if (questionedEntity.af.subject != null) {
			subj = questionedEntity.af.subject.name;
		}

		// startsWith = false;

		boolean signContradict = isSignContradict();
		int signContradictidx = -1;
		if (signContradict) {
			signContradictidx = 0;
			double maxval = -1;
			if (resolvedCents.size() > 1) {
				maxval = Double.parseDouble(resolvedCents.get(0).num);
			}

			for (int i = 0; i < resolvedCents.size(); i++) {
				QuantitativeEntity cent = resolvedCents.get(i);
				if (cent.fakeEntity) {
					continue;
				}
				double val = Double.parseDouble(cent.num);
				if (val > maxval) {
					signContradictidx = i;
					maxval = val;
				}
			}
		}
		MathCoreNLP.println("sci: " + signContradictidx);

		if (!questionedEntity.foundRealEntity) {
			MathCoreNLP.println("REAL NOT FOUND");
		}

		for (int i = 0; i < resolvedCents.size(); i++) {
			QuantitativeEntity cent = resolvedCents.get(i);
			// TODO: !questionedEntity.foundRealEntity ||
			// cent.questionMatch(questionedEntity, sameSetofFeatures)
			if (cent.questionMatch(questionedEntity, featuresImportant)
			/* || !questionedEntity.foundRealEntity */) {

				// handles vmean including buy, ...
				int verbMean = getVerbMean(cent);

				BigDecimal y = new BigDecimal(cent.num);
				if (i == signContradictidx) {
					y = y.multiply(new BigDecimal(-1));
				}
				if (verbMean == 2 || verbMean == -2) {
					// TODO: check others than subj if null
					String s2 = null;
					if (cent.af.subject != null) {
						s2 = cent.af.subject.name;
					}
					if ((subj != null && !Util.SEqual(s2, subj))) {
						y = y.multiply(new BigDecimal(-1));
					} else if (subj == null) {
						String lastpart = sentences.get(sentences.size() - 1)
								.toString().toLowerCase();
						if (s2 != null && !lastpart.contains(s2)) {
							y = y.multiply(new BigDecimal(-1));
						}
					}
				}

				if (verbMean >= 0) {

					if (i != resolvedCents.size() - 1) {
						X = X.add(y);
					} else {
						if (verbMean == 0 && hasSeenBefore(cent)) {
							X = X.subtract(y);
						} else {
							X = X.add(y);
						}
					}
				} else {

					X = X.subtract(y);
				}
			} else {
				System.out
						.println("ambigious: the counted entity was not matched with the question");
			}
		}

		// else {
		// X = new BigDecimal(resolvedCents.get(resolvedCents.size() - 1).num);
		// resolvedCents.remove(resolvedCents.size() - 1);
		// for (CountedEntity cent : resolvedCents) {
		// BigDecimal y = new BigDecimal(cent.num);
		// if (cent.questionMatch(questionedEntity, sameSetofFeatures,
		// eqVerbProb)) {
		// String verb = cent.af.verb;
		// if (CoreNLP.verbMean.get(verb) >= 0) {
		// X = X.subtract(y);
		// } else {
		// X = X.add(y);
		// }
		// } else {
		// MathCoreNLP.println("ambigious");
		// }
		//
		// }
		// }
		X = X.abs();
		MathCoreNLP.println("answer is: " + X);
		return X.doubleValue();

	}

	void simplemakeAction(QuantitativeEntity cent) {
		boolean merged = false;
		// for (int i = resolvedCents.size() - 1; i >= 0; i--) {
		// if (resolvedCents.get(i).match(cent)) {
		// merge(resolvedCents.get(i), cent);
		// merged = true;
		// break;
		// }
		// }
		// if (!merged) {
		if (!cent.fakeEntity) {
			resolvedCents.add(cent);
		}
		// }
	}

	boolean merge(QuantitativeEntity cent1, QuantitativeEntity cent2) {
		if (!cent1.match(cent2)) {
			return false;
		}
		if (cent1.af.verbid.lemma().equals(cent2.af.verbid.lemma())) {
			double num1 = Double.parseDouble(cent1.num);
			double num2 = Double.parseDouble(cent2.num);
			cent1.num = Util.getNumStr(num1 + num2);
		}
		return true;
	}

	// void setQuestionedEntity(CoreMap sentence) {
	// // TODO: complete for the case of non-how many/much
	// for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
	// String word = token.get(LemmaAnnotation.class);
	//
	// }
	// }
	// TODO: what about transfers?!!! should go to equation
	private boolean isSignContradict() {// TODO: add match here
		String lastpart = sentences.get(sentences.size() - 1).toString()
				.toLowerCase();
		if (lastpart.contains("how much") && lastpart.contains("change")) {
			return true;
		}
		int mean = 1;
		if (allCents.size() > 0) {
			mean = getVerbMean(allCents.get(0));
		}
		int plus = mean >= 0 ? 1 : -1;
		boolean allZero = mean == 0;

		for (QuantitativeEntity cent : allCents) {
			mean = getVerbMean(cent);
			int a = mean >= 0 ? 1 : -1;
			allZero = allZero && mean == 0;
			if (plus * a < 0) {
				return false;
			}
		}
		if (allZero) {
			return false;
		}
		try {
			int qmean = getVerbMean(questionedEntity);
			if (qmean * plus < 0) {
				return true;
			}
			return false;
		} catch (Exception e) {
			// e.printStackTrace();
			// System.err.println("in catch for "+questionedEntity.verbid.lemma());
			return false;
		}

	}

	public static int getVerbMean(QuantitativeEntity cent) {
		// if (1==1){
		// return 3;
		// }
		HashSet<String> zeroVerbs = new HashSet<String>();
		zeroVerbs.add("have");
		zeroVerbs.add("be");
		if (cent.verbid != null && zeroVerbs.contains(cent.verbid.lemma())) {
			return 0;
		}

		// just used in label extraction
		if (cent.preferredVmean != -1) {
			return cent.preferredVmean;
		}

		String verb = null;
		try {
			verb = cent.af.verbid.lemma();

		} catch (Exception e) {
			return 1;
		}
		Integer verbMean;
		if (!MathCoreNLP.PREDICT) {
			verbMean = MathCoreNLP.verbMean.get(verb);
			if (verb.equals("buy") || verb.equals("purchase")
					|| verb.equals("rent")) {// especial verb
				String centName = cent.getName();
				if (centName.contains("$") || centName.contains("dollar")) {
					verbMean *= -1;
				}
			}
		} else {
			String pverb = verb;
			SentenceAnalyzer.println(verb);
			int idx = cent.uniqueIdx + MathCoreNLP.pverbOffset;
			if (!MathCoreNLP.pverbMean.containsKey(verb)) {
				pverb = verb + ":" + idx;
			}
			SentenceAnalyzer.println(pverb);
			verbMean = MathCoreNLP.pverbMean.get(pverb);
			// if (SentenceAnalyzer.zeroVerbs.contains(verb)){
			// verbMean = 0;
			// }
			SentenceAnalyzer.println("vmean1: " + verbMean);
			// Integer verbMean2 = MathCoreNLP.pverbMean2.get(pverb);
			// if (verbMean*verbMean2>0){
			// if (verbMean == 2){
			// verbMean = 3;
			// }
			// else if(verbMean==-2){
			// verbMean = -3;
			// }
			// }
			// if (verbMean == null) {
			// SentenceAnalyzer.println("it is nulll: " + cent.uniqueIdx);
			// }
			SentenceAnalyzer.println("pverb: " + pverb + " " + verbMean);
		}

		MathCoreNLP.newVerbs.add(verb);
		if (verbMean == null) {
			verbMean = 1;
		}

		if (cent.getAf().getAfs().getSentence().toString().contains("total of")) {
			if (verbMean >= 0) {
				verbMean = -1;
			} else {
				verbMean = 1;
			}
		}
		return verbMean;
	}

	private boolean hasSeenBefore(QuantitativeEntity cent) {
		// CountedEntity cent = resolvedCents.get(i);
		// String subj = cent.getSubject();
		//
		String entName = cent.getName();
		for (int j = 0; j < allCents.size(); j++) {
			QuantitativeEntity ccent = allCents.get(j);
			if (cent.equals(ccent)) {
				continue;
			}
			if (Util.SEqual(ccent.getName(), entName)) {
				// checking occurance
				// if (Util.SEqual(subj, ccent.getSubject())) {
				// checking action
				if (getVerbMean(ccent) != 0) {
					return true;
				}
				// }

			}
		}

		return false;
	}

	private boolean shouldIgnoreFeatures() {
		int idx = 0;
		if (questionedEntity.entity.amods.size() == 0
				&& questionedEntity.entity.nns.size() == 0) {
			return true;
		}
		List<String> amods = null;
		List<String> nns = null;

		while (idx < resolvedCents.size()) {
			Entity entity = resolvedCents.get(idx).entity;
			if (entity.amods != null || entity.nns != null) {
				amods = entity.amods;
				nns = entity.nns;
				break;
			}
			idx++;
		}
		for (; idx < resolvedCents.size(); idx++) {
			Entity entity = resolvedCents.get(idx).entity;
			if (!Util.listSubset2(entity.amods, amods)
					|| !Util.listSubset2(entity.nns, nns)) {
				break;
			}
		}
		if (idx == resolvedCents.size()) {
			return true;
		}
		boolean qmatch = false;
		boolean qnmatch = false;
		for (QuantitativeEntity cent : resolvedCents) {
			if (cent.entity.amods.size() == 0 && cent.entity.nns.size() == 0) {
				continue;
			}
			if (Util.listSubset2(questionedEntity.entity.amods,
					cent.entity.amods)
					&& Util.listSubset2(questionedEntity.entity.nns,
							cent.entity.nns)) {
				qmatch = true;
			} else {
				qnmatch = true;
			}

		}
		MathCoreNLP.println("shouldIgnore returning: " + !(qmatch && qnmatch));
		if (!(qmatch && qnmatch)) {
			return true;
		}
		return false;
	}
}
