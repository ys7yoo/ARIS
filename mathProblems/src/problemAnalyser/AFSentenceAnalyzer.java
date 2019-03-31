package problemAnalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import weka.filters.unsupervised.attribute.Center;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class AFSentenceAnalyzer {

	HashMap<IndexedWord, AF> aflist;// verb to af
	Collection<TypedDependency> dependencies;
	SemanticGraph dependenciesTree;
	List<IndexedWord> allIndexedWords;
	List<IndexedWord> verbIndexedWords;
	HashSet<String> allSeenCentNames;

	static LexicalizedParser lp;
	static TreebankLanguagePack tlp;
	static GrammaticalStructureFactory gsf;
	private ArrayList<WordInfo> wordInfos;
	ArrayList<String> lemmas;
	ArrayList<Integer> numIndices;
	ArrayList<Integer> seenNumIndices;
	CoreMap sentence;
	boolean isQuestion;

	ArrayList<String> tokenNameswithCoref;
	HashSet<Integer> usedPersonIds;//this is to see which persons 
	HashSet<Integer> usedEachIds;
//	ArrayList<EachRelation> eachRelations;
	
	
	static {
		lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
				"-maxLength", "80", "-retainTmpSubcategories");
		tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
	}
	
	public AFSentenceAnalyzer(CoreMap sentence,
			ArrayList<String> tokenNameswithCoref,
			HashSet<String> allSeenCentNames, boolean isQuestion) {
		usedEachIds = new HashSet<Integer>();
		
//		eachRelations = new ArrayList<EachRelation>();
		this.usedPersonIds = new HashSet<Integer>();
		this.isQuestion = isQuestion;
		this.allSeenCentNames = allSeenCentNames;
		this.sentence = sentence;
		this.tokenNameswithCoref = tokenNameswithCoref;
		wordInfos = new ArrayList<WordInfo>();
		lemmas = new ArrayList<String>();
		numIndices = new ArrayList<Integer>();
		seenNumIndices = new ArrayList<Integer>();
		dependenciesTree = sentence
				.get(CollapsedCCProcessedDependenciesAnnotation.class);
		// MathCoreNLP.println("3: "+(System.currentTimeMillis()-CoreNLP.time0));
		dependencies = dependenciesTree.typedDependencies();
		setDependencies();
		// MathCoreNLP.println("4: "+(System.currentTimeMillis()-CoreNLP.time0));

		allIndexedWords = dependenciesTree.getAllNodesByWordPattern(".*");
		// MathCoreNLP.println("ids: "+ allIndexedWords);
		setAllVerbsIndexedWord();
		extractAFs(isQuestion);
	}

	// void analyzeNumber(CoreMap sentence) {
	// MathCoreNLP.println("number process: ");
	// List<CoreMap> findAndMergeNumbers =
	// NumberNormalizer.findAndMergeNumbers(sentence);
	// for (CoreMap m:findAndMergeNumbers){
	// MathCoreNLP.println(m);
	// }
	// MathCoreNLP.println();
	//
	// }

	public Collection<TypedDependency> getDependencies(String[] textWords,
			LexicalizedParser lp, GrammaticalStructureFactory gsf) {
		// String[] sent = Util.getArray(text);
		Tree parse = lp.apply(Sentence.toWordList(textWords));
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		// MathCoreNLP.println(tdl);
		return tdl;
	}

	void setAllVerbsIndexedWord() {
		verbIndexedWords = new ArrayList<IndexedWord>();
		for (IndexedWord id : allIndexedWords) {
			if (id.tag().startsWith("VB")) {
				verbIndexedWords.add(id);
			}
		}
		removePassVerbs();
		// removeDidDo();
		// for (IndexedWord iw: verbIndexedWords){
		// CoreNLP.addVerb(iw);
		// }
		// MathCoreNLP.println("verbs are:");
		// for (IndexedWord id:verbIndexedWords){
		// MathCoreNLP.println(id);
		// }
	}

	// how many did Mike pick...
	void removeDidDo() {
		ArrayList<IndexedWord> shouldRemove = new ArrayList<IndexedWord>();
		for (IndexedWord id : verbIndexedWords) {
			if (!id.lemma().equals("do")) {
				continue;
			}
			int idx = id.index();
			for (int j = 0; j < 5; j++) {
				try {
					if (getLemma(idx - 2 - j).equals("how")
							&& getLemma(idx - 1 - j).equals("many")) {
						shouldRemove.add(id);
					}
				} catch (Exception e) {
					continue;
				}
			}
			if (shouldRemove.size() > 0) {// how many verbs are you
				// going to remove?! :-)
				break;
			}
		}

		for (IndexedWord id : shouldRemove) {
			verbIndexedWords.remove(id);
		}
	}

	void removePassVerbs() {// it was picked: remove was
		for (TypedDependency td : dependencies) {

			if (td.reln().toString().equals("auxpass")) {
				int idx = td.dep().index();
				for (int i = 0; i < verbIndexedWords.size(); i++) {
					IndexedWord id = verbIndexedWords.get(i);
					if (id.index() == idx) {
						// MathCoreNLP.println("removing passive verb: " + id);
						verbIndexedWords.remove(i);
						break;
					}
				}
			}
		}
	}

	IndexedWord getIndexedWord(int idx) {
		for (IndexedWord id : allIndexedWords) {
			if (id.index() == idx) {
				return id;
			}
		}
		return null;
	}

	void setRelationToVerb(QuantitativeEntity cent) {
		int idx = cent.entity.index;
		IndexedWord wordid = getIndexedWord(idx);
		String pathStr = null;
		int distance = 100000;
		IndexedWord selectedverbid = null;
		// MathCoreNLP.println("verbs: " + verbIndexedWords);
		// MathCoreNLP.println("computing Nearest Neigh");
		if (!cent.isQuestion) {// if how many..., undirected may work better!
			for (IndexedWord verbid : verbIndexedWords) {
				List<SemanticGraphEdge> path = dependenciesTree
						.getShortestDirectedPathEdges(verbid, wordid);
				// MathCoreNLP.println(verbid+" "+path);
				if (path != null && path.size() < distance) {
					distance = path.size();
					pathStr = path.toString();
					selectedverbid = verbid;
				}
				// MathCoreNLP.println("path: "+path);
			}
			// boolean reverse path
			if (selectedverbid == null) {
				for (IndexedWord verbid : verbIndexedWords) {
					List<SemanticGraphEdge> path = dependenciesTree
							.getShortestDirectedPathEdges(wordid, verbid);
					if (path != null && path.size() < distance) {
						distance = path.size();
						pathStr = path.toString();
						selectedverbid = verbid;
						cent.reversePath = true;
					}
					// MathCoreNLP.println("path: "+path);
				}
			}
		}

		boolean isPassive = false;
		for (TypedDependency td : dependencies) {
			String reln = td.reln().toString();
			if (reln.equals("nsubj")) {
				isPassive = false;
				break;
			}
			if (reln.equals("nsubjpass") || reln.equals("auxpass")
					|| reln.equals("agent")) {
				isPassive = true;
			}
		}
//		System.out.println("in anal is passive: " + isPassive + " "
//				+ isQuestion + " " + verbIndexedWords);
		if (selectedverbid == null) {
			for (IndexedWord verbid : verbIndexedWords) {
				List<SemanticGraphEdge> path = dependenciesTree
						.getShortestUndirectedPathEdges(verbid, wordid);
//				System.out.println("in af: " + verbid + " " + path);
				if (path != null
						&& (path.size() < distance
								|| (selectedverbid.lemma().equals("do") && isQuestion) || (selectedverbid
								.lemma().equals("be") && isPassive))) {
					if (isQuestion && selectedverbid != null
							&& verbid.lemma().equals("do")
							&& !selectedverbid.lemma().equals("do")) {
						continue;
					}
					if (isPassive && verbid.lemma().equals("be")
							&& (selectedverbid==null || !selectedverbid.lemma().equals("be"))) {
						continue;
					}
					distance = path.size();
					pathStr = path.toString();
					selectedverbid = verbid;
				}
				// MathCoreNLP.println("path: "+path);
			}
		}
		
		if (isQuestion && !cent.foundRealEntity) {
			int manyIdx = -1;
			for (TypedDependency td : dependencies) {
				String depName = td.dep().nodeString().toLowerCase();
				if ((depName.equals("many") || depName.equals("much"))
						&& getLemma(td.dep().index() - 1).equals("how")) {
					manyIdx = td.dep().index();
				}
			}
			if (selectedverbid == null || selectedverbid.index() <= manyIdx) {

				for (int i = manyIdx + 1; i <= wordInfos.size(); i++) {
					WordInfo wi = getWordInfo(i);
					if (wi.pos.startsWith("V")) {
						if (!wi.lemma.equals("do")) {
							selectedverbid = getIndexedWord(i);
							break;
						}
					}
				}

			}

		}

		cent.pathToVerb = pathStr;
		cent.verbid = selectedverbid;

	}

	void setDependencies() {// just used for lemmatization
		int tokenidx = 0;
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			String lemma = token.get(LemmaAnnotation.class);

			String name = token.get(TextAnnotation.class);

			String pos = token.get(PartOfSpeechAnnotation.class);
			if (pos.equals("CD")) {
				numIndices.add(token.index());
			}

			if (pos.equals("PRP")) {
				lemma = tokenNameswithCoref.get(tokenidx);
			} else if (pos.equals("PRP$")) {// removing 's if needed
				lemma = tokenNameswithCoref.get(tokenidx);
				// removing 's if needed
				// ArrayList<String> non
				for (TypedDependency td : dependencies) {

					if (td.reln().toString().equals("iboj")
							|| td.reln().toString().equals("prep_to")) {
						if (td.dep().index() == token.index()) {
							lemma = lemma.substring(0, lemma.length() - 2);
							break;
						}
					}
				}
			}
			lemmas.add(lemma.toLowerCase());

			String NE = token.get(NamedEntityTagAnnotation.class);
			// MathCoreNLP.println(name);
			// MathCoreNLP.println(pos);
			// MathCoreNLP.println(NE);
			WordInfo wi = new WordInfo(name, lemma, pos, NE);
			wordInfos.add(wi);
			tokenidx++;
		}
		// dependencies = getDependencies(Util.listToArr(strs), lp, gsf);
		// MathCoreNLP.println(dependencies);
	}

	void extractAFs(boolean isQuestion) {
		
		int manyMuchidx = -1;
		aflist = new HashMap<IndexedWord, AF>();
		boolean qentFound = false;
		boolean entityInGovForQuestion = true;
		QuantitativeEntity qent = null;
		HashSet<Integer> seenTypedDependencies = new HashSet<Integer>();
		HashMap<Integer, String> numberamod = new HashMap<Integer, String>();
		for (TypedDependency td : dependencies) {
			
			// MathCoreNLP.println(td.dep());
			//
			// MathCoreNLP.println(td.dep());
			// MathCoreNLP.println(td.gov().nodeString());
			// MathCoreNLP.println(td.reln());

			if (td.reln().toString().equals("num")) {
				try {
					int idx = td.dep().index();// handling e.g: 35 of the
												// seashells
					if (getLemma(idx + 1).equals("of")) {
						continue;
					}
				} catch (Exception e) {

				}

				seenNumIndices.add(td.dep().index());
				seenTypedDependencies.add(td.gov().index());
				
				
				QuantitativeEntity cent = new QuantitativeEntity(td, this,
						true, false);
				// CoreNLP.addVerb(cent.verbid);
				addAFforCent(cent);
				allSeenCentNames.add(getLemma(td.gov().index()));
			} else if (td.reln().toString().equals("number")) {
				// a component of the num... he has 43 blue and 5 red marbles
				numberamod.put(td.dep().index(), getLemma(td.gov().index()));

			} else {
				String depName = td.dep().nodeString().toLowerCase();
				if ((depName.equals("many") || depName.equals("much"))
						&& getLemma(td.dep().index() - 1).equals("how")) {
					// how many was seen
					// int qentIdx = td.dep().index() + 1;
					
					manyMuchidx = td.dep().index();
					
					
					int qentIdx = -1;
					int startIdx = td.dep().index();
					// TypedDependency qtd = td;
					TypedDependency qtd = null;
					// boolean foundRealEntity = allSeenCentNames
					// .contains(getLemma(qentIdx));
					boolean foundRealEntity = false;
					// MathCoreNLP.println(allSeenCentNames
					// .contains(getLemma(qentIdx)) + " " + qentIdx);
					// search for the first real entity. If not found, go
					// to else!
					for (TypedDependency td2 : dependencies) {

						int[] indices = new int[] { td2.gov().index(),
								td2.dep().index() };
						for (int idx : indices) {

							if (idx <= startIdx) {
								continue;
							}
							// MathCoreNLP.println("ii "+idx+getLemma(idx)+" "+
							// allSeenCentNames.contains(getLemma(idx))+" "+
							// foundRealEntity);
							if (idx < qentIdx || !foundRealEntity) {
								String lemma = getLemma(idx);
								if (foundRealEntity
										&& !allSeenCentNames.contains(lemma)) {
									continue;
								}
								if (allSeenCentNames.contains(lemma)) {
									foundRealEntity = true;
									qentIdx = idx;
									qtd = td2;
									if (idx == indices[1]) {
										entityInGovForQuestion = false;
									}
								}
							}
						}
					}
					if (qtd != null) {
						// lastTypeDependency = qtd;
						qentFound = true;
						// MathCoreNLP.println("in how many "+qtd.gov().index()+" "+
						// qtd.dep().index());
						seenTypedDependencies.add(entityInGovForQuestion ? qtd
								.gov().index() : qtd.dep().index());
						QuantitativeEntity cent = new QuantitativeEntity(qtd,
								this, entityInGovForQuestion, true);
						qent = cent;
						cent.foundRealEntity = foundRealEntity;
						addAFforCent(cent);
						
					}
				}
			}
		}
		// what if we do not have how many, how much?
		if (isQuestion && !qentFound) {
			TypedDependency qtd = null;
			boolean foundRealEntity = false;
			for (TypedDependency td2 : dependencies) {
				int idx = td2.gov().index();
				String lemma = getLemma(idx);
				if (allSeenCentNames.contains(lemma)) {
					if (seenTypedDependencies.contains(td2.gov().index())) {
						continue;
					}
					foundRealEntity = true;
					qtd = td2;
					break;
					// we prefer not to have seen exactly this thing before
					// in the entities
				}
			}

			// There was no previously seen cent in the question.
			if (qtd == null) {
				// search for the subject of the verb or its object
				TypedDependency cand = null;
				for (TypedDependency td2 : dependencies) {
					if (td2.reln().toString().equals("nsubj")) {
						// foundRealEntity = true;
						qtd = td2;
						entityInGovForQuestion = false;

					} else if (td2.reln().toString().equals("dobj")) {
						// foundRealEntity = true;
						cand = td2;
						entityInGovForQuestion = false;
					}
				}
				if (qtd == null) {// it should be the dobj
					qtd = cand;
				}
			}

			// randomly set :-)
			if (qtd == null) {
				qtd = dependencies.iterator().next();
			}
			seenTypedDependencies.add(entityInGovForQuestion ? qtd.gov()
					.index() : qtd.dep().index());
			QuantitativeEntity cent = new QuantitativeEntity(qtd, this,
					entityInGovForQuestion, true);
			qent = cent;
			MathCoreNLP.allEntNames.remove(cent.entity.name);
			cent.foundRealEntity = foundRealEntity;
			addAFforCent(cent);

		}
		// handling nums without object
		if (seenNumIndices.size() != numIndices.size()) {
			ArrayList<Integer> nonseen = new ArrayList<Integer>();
			for (int i = 0; i < numIndices.size(); i++) {
				int idx = numIndices.get(i);
				if (!seenNumIndices.contains(idx)) {
					nonseen.add(i);
				}
			}
			// for (Integer i : seenNumIndices) {
			// numIndices.remove(i);
			// }
			// MathCoreNLP.println("nonseen: "+nonseen);
			for (Integer i : nonseen) {
				int numIdx = numIndices.get(i);
				QuantitativeEntity cent = new QuantitativeEntity(
						numIndices.get(i), this);
				// if it was of, then mark the entity, so that it won't be
				// used for fake
				if (lemmas.size() > numIdx + 1
						&& lemmas.get(numIdx + 1).equals("of")) {
					for (int j = numIdx + 2; j < lemmas.size(); j++) {
						if (allSeenCentNames.contains(lemmas.get(j))) {
							seenTypedDependencies.add(j);
							break;
						}
					}
				}

				// CoreNLP.addVerb(cent.verbid);
				addAFforCent(cent);

				if (numberamod.containsKey(cent.entity.index)) {
					cent.entity.amods.add(numberamod.get(cent.entity.index));
				}

			}
		}

		// adding fake cents: The ones that determine actions:
		// e.g: "He bought some more books."
		// cannot be in the question. Since it should be before...
		if (!isQuestion) {
			for (TypedDependency td : dependencies) {
				int[] indices = new int[] { td.gov().index(), td.dep().index() };
				for (int idx : indices) {

					// MathCoreNLP.println("ii "+idx+getLemma(idx)+" "+
					// allSeenCentNames.contains(getLemma(idx))+" "+
					// foundRealEntity);

					String lemma = getLemma(idx);

					if (!seenTypedDependencies.contains(idx)
							&& allSeenCentNames.contains(lemma)) {
						boolean goodFakeEntity = isMorethanamodnn(idx);
						if (!goodFakeEntity){
							continue;
						}
						if (idx == indices[1]) {
							entityInGovForQuestion = false;
						}
						
						QuantitativeEntity cent = new QuantitativeEntity(td,
								this, entityInGovForQuestion, true);
						cent.foundRealEntity = true;
						cent.fakeEntity = true;

						addAFforCent(cent);
						seenTypedDependencies.add(idx);
					}
				}
			}

		}

		for (AF af : aflist.values()) {
			af.resolveAF();
			// MathCoreNLP.println(af);
		}
		
		//check superlative
		if (manyMuchidx!=-1){
			String pos = getWordInfo(manyMuchidx+1).pos.toLowerCase();
//			System.out.println("the pos: "+pos+" "+manyMuchidx);
			if (pos.equals("jjr")||pos.equals("jjs")
					||pos.equals("rbr") || pos.equals("rbs")){
				if (qent!=null){
					qent.setSuperLative(true);
				}
			}
		}
		
		

	}
	
	private boolean isMorethanamodnn(int idx){
		for (TypedDependency td : dependencies) {
			if (td.gov().index() == idx ){
				return true;
			}
			else if(td.dep().index() == idx){
				if (!td.reln().toString().equals("amod")
						&&!td.reln().toString().equals("nn")){
					return true;
				}
			}
		}
		return false;
	}

	private void addAFforCent(QuantitativeEntity cent) {
		
		if (!canbeCountedEntity(cent)){
			return;
		}
		
		if (aflist.containsKey(cent.verbid)) {
			aflist.get(cent.verbid).addCent(cent);
			// MathCoreNLP.println("adding "+cent);
			// try {
			// throw new RuntimeException();
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
		} else {
			AF af = new AF(this, this.dependenciesTree, cent.verbid);
			aflist.put(cent.verbid, af);
			af.addCent(cent);
			// MathCoreNLP.println("adding "+cent);
			// try {
			// throw new RuntimeException();
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
		}
	}
	
	boolean canbeCountedEntity(QuantitativeEntity cent){
		try {
			int idx = cent.getEntity().index;
			for (TypedDependency td : dependencies) {
				if (td.reln().toString().equals("tmod") && td.dep().index() == idx){
					if (cent.num.equals("1") || cent.num.equals("ones")){
						return false;
					}
				}
			}
			return true;
		} catch (Exception e) {
			return true;
		}
		
	}

	public String getLemma(int index) {
		return lemmas.get(index - 1);
	}

	public WordInfo getWordInfo(int index) {
		return wordInfos.get(index - 1);
	}

	public String toString() {
		String ret = "";
		for (AF af : aflist.values()) {
			ret = ret + af.toString() + "\n";
		}
		return ret;
	}

	public HashMap<IndexedWord, AF> getAflist() {
		return aflist;
	}

	public Collection<TypedDependency> getDependencies() {
		return dependencies;
	}

	public SemanticGraph getDependenciesTree() {
		return dependenciesTree;
	}

	public List<IndexedWord> getAllIndexedWords() {
		return allIndexedWords;
	}

	public List<IndexedWord> getVerbIndexedWords() {
		return verbIndexedWords;
	}

	public CoreMap getSentence() {
		return sentence;
	}
	
//	public void addEachRelation(EachRelation er){
//		eachRelations.add(er);
//	}

}
