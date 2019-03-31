//TODO: time, 8:00 am in numberPreProcess
package problemAnalyser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

import verbAnalyze.SentenceAnalyzer;
import labelExtraction.Assignments;
import labelExtraction.QuestionLabel;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ie.NumberNormalizer;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import equationExtraction.World;

public class MathCoreNLP {
	public static boolean isWebAnalyzing = false;
	static int howmmnum = 0;
	static int numQ = 0;
	static int corCount = 0;
	static HashSet<String> newVerbs = new HashSet<String>();
	static HashSet<String> allEntNames = new HashSet<String>();
	static ArrayList<Double> answers;
	static HashMap<String, Integer> verbCounts = new HashMap<String, Integer>();
	public static HashMap<String, Integer> verbMean;
	public static HashMap<String, Integer> pverbMean;
	public static HashMap<String, Integer> pverbMean2;//for the second holder
	public static boolean debug = true;
	public static HashMap<String, Integer> verbRelsCounts = new HashMap<String, Integer>();
	public static boolean PREDICT = false;// use the sentence verb predictions
	public static int cor2;// number of correctly answered questions
	public static int NQ;
	public static int pverbOffset = 0;//this is an offset in the pverbs.txt to map
	//a verbIdx in processing a DS correctly to its prediction.
	
	
	public static void main(String[] args) throws FileNotFoundException {
		// analyzeQuestionsWeb(null, null, true);
		analyzeQuestions(null);
//		SentenceAnalyzer.println(verbRelsCounts);
//		for (String s : verbRelsCounts.keySet()) {
//			SentenceAnalyzer.println(s);
//		}
		// for (String s:allEntNames){
		// MathCoreNLP.println(s);
		// }
		// for (String s:verbMean.keySet()){
		// MathCoreNLP.println(s);
		// }
		MathCoreNLP.println(newVerbs.size());
		MathCoreNLP.println(newVerbs);
		// parseText();
	}

	public static boolean innerRun = true;
	public static String verbsAddress = "verbs3.txt";
	public static String pverbsAddress = "pverbs_h1.txt";
	public static String pverbsAddress2 = "pverbs_h2.txt";
	public static HashMap<Pair<String, String>, Double> vsims;
	static {
		setVerbMeans();
	}
	
	public static void setVerbMeans() {
		verbMean = new HashMap<String, Integer>();
		pverbMean = new HashMap<String, Integer>();
		pverbMean2 = new HashMap<String, Integer>();
		setVerbMean(verbMean, verbsAddress);
		setVerbMean(pverbMean, pverbsAddress);
		setVerbMean(pverbMean2, pverbsAddress2);
		vsims = new HashMap<Pair<String, String>, Double>();
		Scanner sc = null;
		try {
			sc = new Scanner(new File("vsim.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while (sc.hasNext()) {
			String v1 = sc.next();
			String v2 = sc.next();
			double d = sc.nextDouble();
			sc.nextLine();
			vsims.put(new Pair<String, String>(v1, v2), d);
		}
	}

	public static void setVerbMean(HashMap<String, Integer> verbMean,
			String verbsAddress) {
		verbMean.clear();
		Scanner sc3 = null;
		try {
			sc3 = new Scanner(new File(verbsAddress));
			while (sc3.hasNext()) {
				StringTokenizer st = new StringTokenizer(sc3.nextLine());
				String verb = st.nextToken();
				String mean = st.nextToken();
				int meani = 1;
				if (mean.equals("+")) {
					meani = 1;
				} else if (mean.equals("-")) {
					meani = -1;
				} else if (mean.equals("t+")) {
					meani = 2;
				} else if (mean.equals("t-")) {
					meani = -2;
				} else if (mean.equals("0")) {
					meani = 0;
				} else if (mean.equals("++")) {
					meani = 3;
				} else if (mean.equals("--")) {
					meani = -3;
				}

				verbMean.put(verb, meani);
			}
			sc3.close();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		}
	}

	static long time0;
	
	public static void analyzeQuestions(String[] args) throws FileNotFoundException {
		// Test.main(null);
		Scanner sc;
		Scanner sc2;
		SentenceAnalyzer.debug = true;
		int allLen = 0;
		if (args == null){
//			sc = new Scanner(new File("all_a_refined.txt"));
//			sc = new Scanner(new File("prev_a.txt"));
			sc = new Scanner(new File("DfN/DS2/q.txt"));
			// Scanner sc = new Scanner(new File("ixl_a.txt"));
//			sc2 = new Scanner(new File("all_aa_refined.txt"));
			sc2 = new Scanner(new File("DfN/DS2/ans.txt"));
//			sc2 = new Scanner(new File("prev_aa.txt"));
		}
		else{
			sc = new Scanner(new File(args[0]));
			// Scanner sc = new Scanner(new File("ixl_a.txt"));
			sc2 = new Scanner(new File(args[1]));
			pverbOffset = Integer.parseInt(args[2]);
		}

		answers = new ArrayList<Double>();
		// Scanner sc2 = new Scanner(new File("ixl_aa.txt"));
		while (sc2.hasNext()) {
			String line = sc2.nextLine().replace(",", "");
			ArrayList<String> l = new ArrayList<String>();
			l.add(line);
			line = Util.MatharrToString(l);
			answers.add(Double.parseDouble(line));
		}
		sc2.close();

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		time0 = System.currentTimeMillis();
		int i = 0;
		int corrCount = 0;
		int corrCount2 = 0;
		PrintStream op = new PrintStream(new File("corrects.txt"));
		PrintStream op2 = new PrintStream(new File("corrects3.txt"));
		PrintStream opIncorrect = new PrintStream(new File("incorrect_11.txt"));
		while (sc.hasNext()) {
			
			i++;
			String line = sc.nextLine();
			try {
				QuestionAnalyzer questionAnalyzer = new QuestionAnalyzer(line,
						pipeline);
				// guessBaseAnswer();
				allLen += questionAnalyzer.length;
//				allLen += questionAnalyzer.allCents.size()+1;
				
				MathCoreNLP.println(questionAnalyzer);
				// boolean start =
				// line.contains("start")||line.contains("begin");
				// double ans = -1;
				// try {
				// // ans = questionAnalyzer.simpleguessAnswer();
				//
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// if (ans == answers.get(i - 1)) {
				// corrCount++;
				// MathCoreNLP.println("correct guess");
				// op.println(line);
				// }
				
				double ans2 = questionAnalyzer.worldGetAnswer();
				if (Math.abs(ans2 - answers.get(i - 1)) < .000001) {
					corrCount2++;
					MathCoreNLP.println("correct guess2");
					op2.println(line);
				} else {
					opIncorrect.println(line);
				}

//				SentenceAnalyzer.println(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (i % 10 == 0) {
				MathCoreNLP.println("i: " + i + " num q: " + numQ + " numhmm: "
						+ howmmnum);
				MathCoreNLP.println("cor: " + corrCount);
				MathCoreNLP.println("msec: "
						+ (System.currentTimeMillis() - time0));
			}
		}
		double avgLen = ((double)allLen)/numQ;
		System.out.println("avg length: "+allLen);
		NQ = i;
		ArrayList<VerbCount> vbs = new ArrayList<MathCoreNLP.VerbCount>();
		for (String txt : verbCounts.keySet()) {
			vbs.add(new VerbCount(txt, verbCounts.get(txt)));
		}
		Collections.sort(vbs);
		Collections.reverse(vbs);
		for (VerbCount vb : vbs) {
			MathCoreNLP.println(vb.txt + " " + vb.num);
		}
		MathCoreNLP.println("cor2: " + corrCount2);
		sc.close();
		op.close();
		op2.close();
		cor2 = corrCount2;
	}

	public static World[] analyzeQuestionsWeb(String[] questions,
			String fileName, boolean hasQuestion) throws FileNotFoundException {
		// Test.main(null);
		isWebAnalyzing = true;
		ArrayList<String> questionsArr = new ArrayList<String>();
		if (questions == null) {
			if (fileName == null) {
				fileName = "a.txt";
			}
			Scanner sc = new Scanner(new File(fileName));
			questionsArr = new ArrayList<String>();
			while (sc.hasNext()) {
				questionsArr.add(sc.nextLine());
			}
			sc.close();
			questions = new String[questionsArr.size()];
			int i = 0;
			for (String s : questionsArr) {
				questions[i++] = s;
			}
		}
		World[] worlds = new World[questionsArr.size()];

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		time0 = System.currentTimeMillis();
		int i = 0;
		for (String line : questions) {
			i++;

			try {
				QuestionAnalyzer questionAnalyzer = new QuestionAnalyzer(line,
						pipeline);
				// guessBaseAnswer();

				World w = new World(questionAnalyzer.allCents,
						questionAnalyzer.questionedEntity, false);
				MathCoreNLP.println(questionAnalyzer);
				MathCoreNLP.println(w);
				worlds[i - 1] = w;
				// boolean start =
				// line.contains("start")||line.contains("begin");
				// double ans = questionAnalyzer.simpleguessAnswer();
				// if (ans == answers.get(i - 1)) {
				// corrCount++;
				// MathCoreNLP.println("correct guess");
				// op.println(line);
				// }
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (i % 10 == 0) {
				MathCoreNLP.println("i: " + i);
				// MathCoreNLP.println("cor: " + corrCount);
				MathCoreNLP.println("msec: "
						+ (System.currentTimeMillis() - time0));
			}
		}
		ArrayList<VerbCount> vbs = new ArrayList<MathCoreNLP.VerbCount>();
		for (String txt : verbCounts.keySet()) {
			vbs.add(new VerbCount(txt, verbCounts.get(txt)));
		}

		return worlds;
	}

	public static World[] analyzeQuestionsforGuessLabel(String[] questions,
			String fileName, boolean hasQuestion) throws FileNotFoundException {
		// Test.main(null);
		Assignments allAssignments = new Assignments();
		isWebAnalyzing = true;
		Scanner sc2 = new Scanner(new File("aa.txt"));
		ArrayList<String> questionsArr = new ArrayList<String>();
		if (questions == null) {
			if (fileName == null) {
				fileName = "a.txt";
			}
			Scanner sc = new Scanner(new File(fileName));
			questionsArr = new ArrayList<String>();
			while (sc.hasNext()) {
				questionsArr.add(sc.nextLine());
			}
			sc.close();
			questions = new String[questionsArr.size()];
			int i = 0;
			for (String s : questionsArr) {
				questions[i++] = s;
			}
		}
		World[] worlds = new World[questionsArr.size()];

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		time0 = System.currentTimeMillis();
		int i = 0;
		for (String line : questions) {
			i++;

			try {
				QuestionAnalyzer questionAnalyzer = new QuestionAnalyzer(line,
						pipeline);
				// guessBaseAnswer();

				World w = new World(questionAnalyzer.allCents,
						questionAnalyzer.questionedEntity, false);
				MathCoreNLP.println(questionAnalyzer);
				MathCoreNLP.println(w);
				worlds[i - 1] = w;
				QuestionLabel ql = new QuestionLabel(w.quantitativeEntities,
						w.qCEntity, sc2.nextDouble());
				if (ql.assignments.assignments.size() != 0) {
//					SentenceAnalyzer.println("all assignments: "
//							+ ql.assignments.assignments.size());
					allAssignments.merge(ql.assignments);
//					SentenceAnalyzer.println(allAssignments);
				}

				// boolean start =
				// line.contains("start")||line.contains("begin");
				// double ans = questionAnalyzer.simpleguessAnswer();
				// if (ans == answers.get(i - 1)) {
				// corrCount++;
				// MathCoreNLP.println("correct guess");
				// op.println(line);
				// }
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (i % 10 == 0) {
				MathCoreNLP.println("i: " + i);
				// MathCoreNLP.println("cor: " + corrCount);
				MathCoreNLP.println("msec: "
						+ (System.currentTimeMillis() - time0));
			}
		}
		ArrayList<VerbCount> vbs = new ArrayList<MathCoreNLP.VerbCount>();
		for (String txt : verbCounts.keySet()) {
			vbs.add(new VerbCount(txt, verbCounts.get(txt)));
		}

		return worlds;
	}

	public static void parseText() {
		String text = "Tim placed 3 more pencils in the drawer.";
		// String text =
		// "Bell, a company which is based in LA, makes and distributes red beautiful computer products.";
		// String text = "how many books does John have?"; // Add your text
		// here!
		getDependencies(text);

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// read some text in the text variable

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				MathCoreNLP.println(word);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				MathCoreNLP.println(pos);
				// this is the NER label of the token
				String ne = token.get(NamedEntityTagAnnotation.class);
				MathCoreNLP.println(ne);
			}

			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			MathCoreNLP.println("tree: " + tree);

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence
					.get(CollapsedCCProcessedDependenciesAnnotation.class);
			MathCoreNLP.println("dependencies: " + dependencies);

		}

		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document
				.get(CorefChainAnnotation.class);

		MathCoreNLP.println("chains: " + graph);
	}

	public static void getDependencies(String text) {
		LexicalizedParser lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
				"-maxLength", "80", "-retainTmpSubcategories");
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		String[] sent = Util.getArray(text);
		Tree parse = lp.apply(Sentence.toWordList(sent));
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		MathCoreNLP.println(tdl);
	}

	public static String corefPreProcess(Annotation document,
			ArrayList<ArrayList<String>> tokenNames) {
		Map<Integer, CorefChain> graph = document
				.get(CorefChainAnnotation.class);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		HashSet<String> personNames = getPersonNames(document);
		HashMap<Pair<Integer, Integer>, String> pronounPlaces = new HashMap<Pair<Integer, Integer>, String>();
		// from place to pos type
		// ArrayList<ArrayList<String>> tokenNames = new
		// ArrayList<ArrayList<String>>();
		int seni = 1;
		for (CoreMap sentence : sentences) {
			ArrayList<String> tokenL = new ArrayList<String>();
			tokenNames.add(tokenL);

			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				// if (!isWebAnalyzing) {
				// MathCoreNLP.println(word);
				// MathCoreNLP
				// .println(token.get(PartOfSpeechAnnotation.class));
				// MathCoreNLP.println(token
				// .get(NamedEntityTagAnnotation.class));
				// }

				String text = token.originalText();

				tokenL.add(text);

				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.startsWith("PRP")) {
					// MathCoreNLP.println("added: "+word);
					pronounPlaces.put(
							new Pair<Integer, Integer>(seni, token.index()),
							pos);
				}
				// MathCoreNLP.println(token.get(NamedEntityTagAnnotation.class));
			}
			seni++;
		}
		for (CorefChain ch : graph.values()) {
			if (!isWebAnalyzing) {
				MathCoreNLP.println(ch.getMentionsInTextualOrder());
			}

			List<CorefMention> mentions = ch.getMentionsInTextualOrder();
			String pname = hasPersonName(mentions, tokenNames, personNames);
			if (pname == null) {
				continue;
			}
			for (CorefMention men : mentions) {
				int sent = men.sentNum;
				int index = men.startIndex;
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(sent,
						index);
				if (!pronounPlaces.containsKey(pair)) {
					continue;
				}
				String pos = pronounPlaces.get(pair);
				tokenNames.get(sent - 1).remove(index - 1);
				tokenNames.get(sent - 1).add(index - 1,
						pos.equals("PRP") ? pname : (pname + "'s"));
			}
		}
		String ret = "";
		for (ArrayList<String> ss : tokenNames) {
			for (String s : ss) {
				ret = ret + s + " ";
			}
		}
		return ret;

	}

	public static HashSet<String> getPersonNames(Annotation document) {
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		HashSet<String> ret = new HashSet<String>();
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				if (token.get(NamedEntityTagAnnotation.class).equals("PERSON")) {
					ret.add(token.originalText());
					if (!isWebAnalyzing) {
						// MathCoreNLP.println("pname: " + token.originalText()
						// + "1");
					}
				}
				ret.add(token.get(PartOfSpeechAnnotation.class));
				// MathCoreNLP.println();
			}
		}
		return ret;
	}

	static String hasPersonName(List<CorefMention> mentions,
			ArrayList<ArrayList<String>> tokenNames, HashSet<String> personNames) {
		for (CorefMention men : mentions) {
			if (men.startIndex == men.endIndex - 1) {
				String str = tokenNames.get(men.sentNum - 1).get(
						men.startIndex - 1);
				if (personNames.contains(str)) {
					return str;
				}
			}
		}
		return null;
	}

	static String numberPreProcess(Annotation document,
			ArrayList<String> fractions) {

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ArrayList<String> numStrs = null;
		ArrayList<String> allTokens = new ArrayList<String>();
		int sidx = 0;
		for (CoreMap sentence : sentences) {
			boolean numberProcessing = false;
			boolean shouldDeleteLeft = false;
			ArrayList<CoreLabel> coreLabels = new ArrayList<CoreLabel>();
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				boolean isQ = sentences.size() > 1 && sidx == sentences.size() - 1;
				if (!isQ && token.originalText().equals("more")){
					continue;
				}
				coreLabels.add(token);
				String lemma = token.get(LemmaAnnotation.class);
//				SentenceAnalyzer.println("lemma: "+lemma);
				if (lemma.equals("have")
						||lemma.equals("be")){
					shouldDeleteLeft = true;
				}
			}
			sidx++;
			for (int i = 0; i < coreLabels.size(); i++) {
				CoreLabel token = coreLabels.get(i);
				// String word = token.get(TextAnnotation.class);
				// MathCoreNLP.println(word);
				// MathCoreNLP.println(token.get(PartOfSpeechAnnotation.class));
				// MathCoreNLP.println(token.get(NamedEntityTagAnnotation.class));
				String NE = token.get(NamedEntityTagAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				String text = token.originalText();

				if (numberProcessing) {
					if (text.equals("and")) {
						continue;
					}
					
					if (text.equals("left") && shouldDeleteLeft){
						continue;
					}
					
					boolean isFractionProcessing = numStrs.size() > 0
							&& numStrs.get(numStrs.size() - 1).contains("/");
					if (text.equals("of") && isFractionProcessing) {
						continue;
					}
					if ((text.equals("a") || text.equals("an"))
							&& i > 1
							&& coreLabels.get(i - 1).originalText()
									.equals("of")) {
						continue;
					}
					if (pos.equals("CD") && !NE.equals("TIME")) {
						numStrs.add(text);
					} else {

						double num;
						String numStr;
//						SentenceAnalyzer.println("numStr: " + numStrs);
						if (!isNumber(numStrs)) {
							numStr = Util.MatharrToString(numStrs);
							num = NumberNormalizer.wordToNumber(numStr)
									.doubleValue();
							numStr = Util.getNumStr(num);
							if (numStr.contains(".")) {
								fractions.add(numStr);
								numStr = "3003";

							}
						} else {
							numStr = numStrs.get(0);
						}

						allTokens.add(numStr);
						allTokens.add(text);
						numberProcessing = false;
					}
				} else {
					if (pos.equals("CD") && !NE.equals("TIME")) {
						numStrs = new ArrayList<String>();
						numberProcessing = true;
						numStrs.add(text);
					} else {
						
						if (text.equals("left") && shouldDeleteLeft){
							continue;
						}
						
						allTokens.add(text);
					}
				}
			}
			if (numberProcessing) {
				String numStr = Util.arrToString(numStrs);
				double num = NumberNormalizer.wordToNumber(numStr).floatValue();
				if (num - Math.floor(num) == 0) {
					numStr = ((int) num) + "";
				} else {
					numStr = num + "";
				}
				allTokens.add(numStr);
				numberProcessing = false;
			}
		}
		return Util.arrToString(allTokens);
	}

	private static boolean isNumber(ArrayList<String> numStrs) {
		if (numStrs.size() > 1) {
			return false;
		}
		for (char c : numStrs.get(0).toCharArray()) {
			if (c != '.' && (c < '0' || c > '9')) {
				return false;
			}
		}
		return true;
	}

	static class VerbCount implements Comparable {
		String txt;
		int num;

		public VerbCount(String txt, int num) {
			this.txt = txt;
			this.num = num;
		}

		public int compareTo(Object o) {
			VerbCount v = (VerbCount) o;
			if (num > v.num) {
				return 1;
			} else if (num < v.num) {
				return -1;
			}
			return 0;
		}
	}

	public static void addVerb(IndexedWord iw) {
		if (iw == null) {
			return;
		}
		String txt = iw.lemma();
		if (MathCoreNLP.verbCounts.containsKey(txt)) {
			int num = MathCoreNLP.verbCounts.get(txt);
			MathCoreNLP.verbCounts.put(txt, num + 1);
		} else {
			MathCoreNLP.verbCounts.put(txt, 1);
		}
	}

	public static void println(Object s) {
		if (debug) {
			System.out.println(s);
		}
	}

	public static void print(Object s) {
		if (debug) {
			System.out.print(s);
		}
	}

	public static boolean hasTwoHolders(int vmean) {
		return Math.abs(vmean) >= 2;
	}

	public static int getSecVmean(int vmean) {
		if (Math.abs(vmean) == 2) {
			return -vmean;
		}
		return vmean;
	}

}
