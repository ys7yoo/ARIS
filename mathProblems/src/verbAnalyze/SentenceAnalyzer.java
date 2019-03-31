package verbAnalyze;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import jsonGenerator.CompactQuantitativeEntity;
import jsonGenerator.MathJsonGenerator;
import net.didion.jwnl.JWNLException;
import edu.cmu.lti.ws4j.impl.Lin;
import equationExtraction.World;
import problemAnalyser.AF;
import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import sun.reflect.generics.visitor.Reifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.Normalize;

public class SentenceAnalyzer {

	ArrayList<String> featuresList = new ArrayList<String>();
	HashMap<String, Integer> featuresToVerb = new HashMap<String, Integer>();
	public HashMap<String, String> verbToFeatures = new HashMap<String, String>();
	public int numFeatures;
	boolean testBase = false;
	ArrayList<CompactQuantitativeEntity> cents;
	static int SEED = 15;
	static boolean carefulSplit = true;
	static boolean randSplit = false;

	public static boolean debug = false;

	public static boolean refined = true;
	static int[] numCentsInDS;

	public static HashSet<String> zeroVerbs;// used for removing verb from
											// training

	static {
		if (refined) {
			numCentsInDS = new int[] { 810, 1282, 1825 };
		} else {
			numCentsInDS = new int[] { 833, 1305, 1915 };
		}
	}

	public static int SIM = 0, STRUCTURE = 1, SIMandSTRUCTURE = 3, LEX = 2,
			SIMandLEX = 4, STRUCTUREandLEX = 5, ALL = 6;

	public static void main(String[] args) throws Exception {
		// MathCoreNLP.verbsAddress = "verbs.txt";
		// MathCoreNLP.setVerbMean();

		// MathJsonGenerator.main(new String[] { "input_refined.txt" });
//		zeroVerbs = new HashSet<String>();
//		String[] l = new String[] { "have", "be", "begin", "own", "contain",
//				"include", "remain" };
//		for (String s : l) {
			// zeroVerbs.add(s);//THIS WAS NOT USED IN GENERATING THE MATH-ACC
			// NUMS
			// I REMOVED ZERO VERBS FROM TRAIN AND TEST AT THE LAST MOMENTS
			// USED JUST FOR VERB CLASSIFICATION, NOT ANYMORE...!
//		}
		// ZERO VERBS ARE USED FOR TRAINING, BUT NOT REPORTED WHILE TESTING!!!
		//
		
//		test1();

		SentenceAnalyzer arffGenerator = new SentenceAnalyzer();
		ArrayList<CompactQuantitativeEntity> cents = null;
		try {
			cents = MathJsonGenerator.readJsons();
		} catch (Exception e) {
			e.printStackTrace();
		}
		arffGenerator.cents = cents;
		// arffGenerator.generateArffSim();

		ArrayList<String> verbsList = arffGenerator
				.generateArffWithFeatures(SIMandSTRUCTURE);
		arffGenerator.analyzerArff(verbsList);
	}

	static void test1() throws Exception {
		MathCoreNLP.PREDICT = true;
		MathCoreNLP.debug = false;

		PrintStream op = new PrintStream(new File("results" + "_care"
				+ carefulSplit + "_arnd" + randSplit + ".xls"));
		int numRun = 5;
		int startSeed = 41;

		String[] DSnames = new String[] { "prev_a", "ixl_a", "a2" };
		String[] ansNames = new String[] { "prev_aa", "ixl_aa", "aa2" };

		if (refined) {
			for (int i = 0; i < DSnames.length; i++) {
				DSnames[i] += "_refined";
				ansNames[i] += "_refined";
			}
		}

		double[] cumAcc = new double[numRun];

		for (int type = 6; type >= 2; type--) {
			double[] paccs = new double[numRun];// sen
			double[] paccs2 = new double[numRun];// verb
			double[] paccs3 = new double[numRun];// pr
			double[] paccs4 = new double[numRun];// recall
			double[][] maccs = new double[numRun][DSnames.length];
			for (int j = 0; j < +numRun; j++) {
				SEED = j + startSeed;
				QuantitativeEntity.lastUniqueIdx = 0;
				SentenceAnalyzer arffGenerator = new SentenceAnalyzer();
				ArrayList<CompactQuantitativeEntity> cents = null;
				try {
					cents = MathJsonGenerator.readJsons();
				} catch (Exception e) {
					e.printStackTrace();
				}
				arffGenerator.cents = cents;
				// arffGenerator.generateArffSim();

				ArrayList<String> verbsList = arffGenerator
						.generateArffWithFeatures(type);
				// This will output the prediction of verbs in a pverbs
				double[] pacc = arffGenerator.analyzerArff(verbsList);
				paccs[j] = pacc[0];
				paccs2[j] = pacc[1];
				paccs3[j] = pacc[2];
				paccs4[j] = pacc[3];
				MathCoreNLP.setVerbMeans();
				int allN = 0;
				int allCor = 0;
				for (int k = 0; k < DSnames.length; k++) {
					int offset = k == 0 ? 0 : numCentsInDS[k - 1];
					// QuantitativeEntity.lastUniqueIdx = 0;
					MathCoreNLP.analyzeQuestions(new String[] {
							DSnames[k] + ".txt", ansNames[k] + ".txt", "0" });
					int cor = MathCoreNLP.cor2;
					int numPM = MathCoreNLP.NQ;
					allCor += cor;
					if (k == 0) {
						numPM -= 109;
					}
					allN += numPM;
					double macc = (double) (cor) / numPM;
					maccs[j][k] = macc;
				}
				cumAcc[j] = (double) (allCor) / allN;
			}
			op.print("sentence\t");
			for (int j = 0; j < numRun; j++) {
				op.print(paccs[j] + "\t");
			}
			op.println();
			op.print("Verb\t");
			for (int j = 0; j < numRun; j++) {
				op.print(paccs2[j] + "\t");
			}
			op.println();

			op.print("precision\t");
			for (int j = 0; j < numRun; j++) {
				op.print(paccs3[j] + "\t");
			}
			op.println();

			op.print("recall\t");
			for (int j = 0; j < numRun; j++) {
				op.print(paccs4[j] + "\t");
			}
			op.println();

			for (int k = 0; k < DSnames.length; k++) {
				op.print("DS" + k + "\t");
				for (int j = 0; j < numRun; j++) {
					op.print(maccs[j][k] + "\t");
				}
				op.println();
			}
			op.print("cumAcc\t");
			for (int j = 0; j < numRun; j++) {
				op.print(cumAcc[j] + "\t");
			}

			op.println();
			op.println();
		}
		op.close();
	}

	double[] analyzerArff(ArrayList<String> verbsList) throws Exception {
		if (verbsList.size() == 0) {

		}
		// System.out.println("analyzing");
		ArrayList<ArrayList<String>> verbsTF = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> verbsTF2 = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < 4; i++) {
			verbsTF.add(new ArrayList<String>());
		}

		for (int i = 0; i < 4; i++) {
			verbsTF2.add(new ArrayList<String>());
		}

		int numFolds = 3;
		int TP = 0;
		int TN = 0;
		int FP = 0;
		int FN = 0;
		int TP2 = 0;
		int TN2 = 0;
		int FP2 = 0;
		int FN2 = 0;
		int vindex = 0;

		String[] sentenceVerbs = new String[verbsList.size()];
		for (int i = 0; i < sentenceVerbs.length; i++) {
			String str = verbsList.get(i); // find:120
			int beginIdx = str.indexOf(':') + 1;
			String intStr = str.substring(beginIdx);
			int t = Integer.parseInt(intStr);
			// System.out.println(verbsList.get(i));
			// System.out.println("here"+uniqeToCent.get(t));
			sentenceVerbs[i] = cents.get(uniqeToCent.get(t)).getVerb();
		}
		ArrayList<String> allVerbs = new ArrayList<String>();
		HashSet<String> verbsSet = new HashSet<String>();
		for (CompactQuantitativeEntity cent : cents) {
			if (!verbsSet.contains(cent.getVerb())) {
				verbsSet.add(cent.getVerb());
				allVerbs.add(cent.getVerb());
			}
		}

		// int[] foldIndices = getFoldIndices(sentenceVerbs, allVerbs);
		ArrayList<ArrayList<Integer>> foldIndices = null;
		if (carefulSplit) {
			foldIndices = getFoldIndices(sentenceVerbs, allVerbs, numFolds);
		} else {
			if (!randSplit) {

				foldIndices = getFoldIndicesCareless(numCentsInDS);
			} else {
				foldIndices = getFoldIndicesCarelessRand(numFolds,
						sentenceVerbs.length);
			}
		}

		// System.out.println("here");
		ArrayList<Integer> testIndices = new ArrayList<Integer>();
		ArrayList<Integer> testIndices2 = new ArrayList<Integer>();

		// inss.randomize(new Random());
		PrintStream op = new PrintStream(new File("pverbs_h1.txt"));
		PrintStream op2 = new PrintStream(new File("pverbs_h2.txt"));
		Instances inss = null;
		Instances inss2 = null;

		for (int fold = 0; fold < numFolds; fold++) {
			DataSource source = new DataSource("arff/verb" + fold + "_h1.arff");
			DataSource source2 = new DataSource("arff/verb" + fold + "_h2.arff");
			inss = source.getDataSet();
			inss.setClassIndex(inss.numAttributes() - 1);

			inss2 = source2.getDataSet();
			inss2.setClassIndex(inss.numAttributes() - 1);
			// Instances train = getTrainTest(foldIndices, fold, inss, true,
			// testIndices);
			// Instances test = getTrainTest(foldIndices, fold, inss, false,
			// testIndices);
			Instances train;
			Instances test;
			Instances train2;
			Instances test2;
			// if (carefulSplit) {
			train = getTrainTest(foldIndices, fold, numFolds, inss, true,
					testIndices);
			test = getTrainTest(foldIndices, fold, numFolds, inss, false,
					testIndices);

			train2 = getTrainTest(foldIndices, fold, numFolds, inss2, true,
					testIndices2);
			test2 = getTrainTest(foldIndices, fold, numFolds, inss2, false,
					testIndices2);
			// } else {
			// train = inss.trainCV(numFolds, fold);
			// test = inss.testCV(numFolds, fold);
			//
			// for (int i = 0; i < test.numInstances(); i++) {
			// testIndices.add(i);
			// }
			//
			// train2 = inss.trainCV(numFolds, fold);
			// test2 = inss.testCV(numFolds, fold);
			// }

			// System.out.println("inss size: " + inss.numInstances());
			// System.out.println("test size: " + test.numInstances());
			// for (int i=0; i<inss.numInstances(); i++){
			// System.out.println(inss.instance(i).value(inss.numAttributes()-1));
			// }

			Classifier cl = getNewClassifier(-1);
			cl.buildClassifier(train);

			Classifier cl2 = getNewClassifier(-1);
			cl2.buildClassifier(train2);

			String[] l = new String[] { "have", "be", "begin", "own",
					"contain", "include", "remain" };
			HashSet<String> zz = new HashSet<String>();
			for (String s : l) {
				zz.add(s);
			}

			// System.out.println("predictions");
			for (int i = 0; i < test.numInstances(); i++) {
				// System.out.println(test.instance(i));
				int plabel = (int) cl.classifyInstance(test.instance(i));
				int plabel2 = (int) cl2.classifyInstance(test2.instance(i));
				// System.out.println("plabel is: " + plabel);
				// System.out.println("class val: "+test.instance(i).classValue()+" "+test.classIndex());
				int label = (int) test.instance(i).classValue();
				int label2 = (int) test2.instance(i).classValue();
				// double p = cl.distributionForInstance(test.instance(i))[0];
				// if (p>=.5){
				// plabel = 0;
				// }
				// else{
				// plabel = 1;
				// }
				String verb = verbsList.get(testIndices.get(vindex));
				// System.out.println(verb);
				// if (vindex != i) {
				// System.out.println("not equal");
				// }
				// System.out.println("vidx: " + vindex);
				// System.out.println("whole instance: " + test.instance(i));
				// System.out.println("plabel: " + plabel + " real l: " +
				// label);
				op.print(verb + "\t");
				op2.print(verb + "\t");

				int endStr = verb.indexOf(':');
				String realVerb = verb.substring(0, endStr);
				boolean zero = false;
				if (zz.contains(realVerb)) {
					// zero = true;
					op.println("0");
					op2.println("0");
				} else {
					op.println((plabel == 1 ? "t+" : "t-"));
					op2.println((plabel2 == 1 ? "t+" : "t-"));
				}
				// System.out.println("hi "+vindex);
				if (!zero) {
					if (plabel == label) {
						if (plabel == 1) {
							if (!zero) {
								verbsTF.get(0).add(verb);
								TP++;
							}
						} else {

							verbsTF.get(1).add(verb);
							TN++;
						}
					} else {
						if (plabel == 1) {
							verbsTF.get(2).add(verb);
							FP++;
						} else {
							verbsTF.get(3).add(verb);
							FN++;
						}
					}

					if (plabel2 == label2) {
						if (plabel2 == 1) {
							verbsTF2.get(0).add(verb);
							TP2++;
						} else {
							verbsTF2.get(1).add(verb);
							TN2++;
						}
					} else {
						if (plabel2 == 1) {
							verbsTF2.get(2).add(verb);
							FP2++;
						} else {
							verbsTF2.get(3).add(verb);
							FN2++;
						}
					}

				}

				// System.out.println(plabel2 + " vsp " + label2 + " "
				// + (plabel2 == label2));

				// int label =
				// System.out.println(test.instance(i));
				// vindex = featuresToVerb.get(test.instance(i).toString());
				// System.out.println(verbsList.get(vindex) + ": real label: "
				// + label + " "
				// + (label == plabel ? "true" : "false"));
				// System.out.println(featuresList.get(vindex));

				// System.out.println();
				vindex++;
			}
		}
		op.close();
		op2.close();

		System.out.println("acc2: " + (double) (TP2 + TN2)
				/ inss2.numInstances());

		int numCor = TP + TN;
		double acc = (double) numCor / inss.numInstances();
		System.out.println("acc: " + acc);

		double pr = ((double) TP + TP2) / (TP + TP2 + FP + FP2);
		double recall = ((double) TP + TP2) / (TP + TP2 + FN + FN2);
		double f1 = 2 * (pr * recall) / (pr + recall);

		System.out.println("TP: " + TP + " FN: " + FN);
		System.out.println("FP: " + FP + " TN: " + TN);
		System.out.println("TP2: " + TP2 + " FN: " + FN2);
		System.out.println("FP: " + FP2 + " TN: " + TN2);
		System.out.println("f1: " + f1);
		double allAcc = (double) (TP + TN + TP2 + TN2)
				/ (TP + TN + TP2 + TN2 + FP + FP2 + FN + FN2);
		System.out.println("allacc: " + allAcc);
		String[] charac = new String[] { "TP: ", "TN: ", "FP: ", "FN: " };

		for (int i = 0; i < verbsTF.size(); i++) {
			ArrayList<String> b = verbsTF.get(i);
			System.out.println(charac[i] + b);
		}
		double accVerb = checkDifferentVerbRes(verbsTF);
		System.out.println("f1verb: " + accVerb);
		double[] ret = new double[4];
		ret[0] = allAcc;

		ret[1] = accVerb;
		ret[2] = pr;
		ret[3] = recall;
		return ret;
	}

	double checkDifferentVerbRes(ArrayList<ArrayList<String>> verbsTF) {
		HashMap<String, int[]> verbToClassificationRes = new HashMap<String, int[]>();
		HashMap<String, Integer> verbToLabel = new HashMap<String, Integer>();
		for (int i = 0; i < verbsTF.size(); i++) {
			ArrayList<String> ss = verbsTF.get(i);
			int plabel = 0;
			if (i % 2 == 0) {
				plabel = 1;
			}
			int realLabel = 0;
			if (i == 0 || i == 3) {
				realLabel = 1;
			}
			for (String verb : ss) {
				int endStr = verb.indexOf(':');
				String realVerb = verb.substring(0, endStr);
				if (!verbToClassificationRes.containsKey(realVerb)) {
					verbToClassificationRes.put(realVerb, new int[2]);
				}
				verbToClassificationRes.get(realVerb)[plabel]++;
				verbToLabel.put(realVerb, realLabel);
			}
		}
		// System.out.println("contradict labels:");
		verbsTF = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < 4; i++) {
			verbsTF.add(new ArrayList<String>());
		}

		int TP = 0;
		int TN = 0;
		int FP = 0;
		int FN = 0;

		for (String verb : verbToClassificationRes.keySet()) {
			int[] l = verbToClassificationRes.get(verb);
			// if (l[0] > 0 && l[1] > 0) {
			// System.out.println("BAD: " + verb + ": " + l[0] + " " + l[1]);
			// } else {
			// System.out.println(verb + ": " + l[0] + " " + l[1]);
			// }
			int label = verbToLabel.get(verb);
			int plabel = l[0] > l[1] ? 0 : 1;
			if (plabel == label) {
				if (plabel == 1) {
					verbsTF.get(0).add(verb);
					TP++;
				} else {
					verbsTF.get(1).add(verb);
					TN++;
				}
			} else {
				if (plabel == 1) {
					verbsTF.get(2).add(verb);
					FP++;
				} else {
					verbsTF.get(3).add(verb);
					FN++;
				}
			}
		}
		int numCor = TP + TN;
		double acc = (double) numCor / (TP + TN + FP + FN);
		System.out.println("acc: " + acc);

		System.out.println("TP: " + TP + " FN: " + FN);
		System.out.println("FP: " + FP + " TN: " + TN);
		String[] charac = new String[] { "TP: ", "TN: ", "FP: ", "FN: " };

		for (int i = 0; i < verbsTF.size(); i++) {
			ArrayList<String> b = verbsTF.get(i);
			System.out.println(charac[i] + b);
		}

		double pr = ((double) TP) / (TP + FP);
		double recall = ((double) TP) / (TP + FN);
		double f1 = 2 * (pr * recall) / (pr + recall);

		return acc;
		// return f1;
	}

	// public ArrayList<String> getSimFeatures() {
	//
	// }

	// It produces numFold = 10 arff files. If the type does not have sim, all
	// of them will be the same!
	// The goal of producing numFold files is to reduce the bias of feature
	// selection
	public ArrayList<String> generateArffWithFeatures(int type)
			throws FileNotFoundException {
		ArrayList<String> verbList = new ArrayList<String>();
		ArrayList<String> features = null;
		int numFold = 10;
		ArrayList<ArrayList<String>> simFeatures = new ArrayList<ArrayList<String>>();
		ArrayList<HashMap<String, String>> simVerbToFeatures = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<String>> arffFeatures = new ArrayList<ArrayList<String>>();// These
																						// used
		for (int k = 0; k < numFold; k++) {
			verbList = new ArrayList<String>();
			simVerbToFeatures.add(new HashMap<String, String>());
			features = generateFeaturesSim(simVerbToFeatures.get(k), verbList,
					0);
			simFeatures.add(features);
		}
		System.out.println(uniqeToCent);
		if (type == SIM) {
			arffFeatures = simFeatures;
		} else if (type == STRUCTURE) {
			verbList = new ArrayList<String>();
			features = generateFeaturesStructure(new HashMap<String, String>(),
					verbList);
			for (int k = 0; k < numFold; k++) {
				arffFeatures.add(features);
			}
		} else if (type == SIMandSTRUCTURE) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesStructure(verbToFeatures2, verbList);
			for (int k = 0; k < numFold; k++) {
				features = mergeFeatures(simVerbToFeatures.get(k),
						verbToFeatures2, verbList,
						new HashMap<String, String>());
				arffFeatures.add(features);
			}

		} else if (type == STRUCTUREandLEX) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures1 = new HashMap<String, String>();
			generateFeaturesLexFileNames(verbToFeatures1, verbList);

			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesStructure(verbToFeatures2, verbList);
			for (int k = 0; k < numFold; k++) {
				features = mergeFeatures(verbToFeatures1, verbToFeatures2,
						verbList, new HashMap<String, String>());
				arffFeatures.add(features);
			}

		} else if (type == LEX) {
			verbList = new ArrayList<String>();
			features = generateFeaturesLexFileNames(
					new HashMap<String, String>(), verbList);
			for (int k = 0; k < numFold; k++) {
				arffFeatures.add(features);
			}
		} else if (type == SIMandLEX) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesLexFileNames(verbToFeatures2, verbList);
			for (int k = 0; k < numFold; k++) {
				features = mergeFeatures(simVerbToFeatures.get(k),
						verbToFeatures2, verbList,
						new HashMap<String, String>());
				// System.out.println("the size: " + features.size());
				arffFeatures.add(features);
			}
		} else if (type == ALL) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesLexFileNames(verbToFeatures2, verbList);
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures3 = new HashMap<String, String>();
			generateFeaturesStructure(verbToFeatures3, verbList);
			for (int k = 0; k < numFold; k++) {
				HashMap<String, String> m1 = new HashMap<String, String>();
				mergeFeatures(simVerbToFeatures.get(k), verbToFeatures2,
						verbList, m1);

				features = mergeFeatures(m1, verbToFeatures3, verbList,
						new HashMap<String, String>());
				arffFeatures.add(features);
			}

		}

		makeArffs(numFold, arffFeatures, verbList, 1);
		// TODO: maybe remove 0, 1, -1
		makeArffs(numFold, arffFeatures, verbList, 2);

		return verbList;
	}

	void makeArffs(int numFold, ArrayList<ArrayList<String>> arffFeatures,
			ArrayList<String> verbList, int hnum) throws FileNotFoundException {
		for (int k = 0; k < numFold; k++) {
			PrintStream op = new PrintStream(new File("arff/verb" + k + "_h"
					+ hnum + ".arff"));
			op.println("@RELATION dis");
			op.println();
			ArrayList<String> features = arffFeatures.get(k);
			ArrayList<String> features2 = new ArrayList<String>();
			if (hnum == 2) {
				for (int i = 0; i < features.size(); i++) {
					String verb = verbList.get(i);
					int endStr = verb.indexOf(':');
					verb = verb.substring(0, endStr);
					// System.out.println(verb);
					int label = 0;
					try {
						label = MathCoreNLP.getSecVmean(MathCoreNLP.verbMean
								.get(verb));
					} catch (Exception e) {

					}
					label = getLabel(label);
					String feature = features.get(i);
					int lastcomma = feature.lastIndexOf(',');
					feature = feature.substring(0, lastcomma + 1);
					feature += label;
					// features.add(i,feature);
					features2.add(feature);
				}
				features = features2;
			}
			int numAttrs = features.get(0).split(",").length - 1;
			for (int i = 0; i < numAttrs; i++) {
				op.println("@ATTRIBUTE i" + i + " NUMERIC");
			}
			op.println("@ATTRIBUTE class        {0,1}");
			op.println("@DATA");

			for (String s : features) {
				// System.out.println("that was: " + s);
				op.println(s);
			}
			op.close();
		}
	}

	ArrayList<String> mergeFeatures(HashMap<String, String> verbToFeatures1,
			HashMap<String, String> verbToFeatures2,
			ArrayList<String> verbsList, HashMap<String, String> vfout) {
		ArrayList<String> features = new ArrayList<String>();
		int numFeatrures2 = verbToFeatures2.values().iterator().next()
				.split(",").length;
		String zeroFeature2 = "";
		for (int i = 0; i < numFeatrures2 - 1; i++) {
			zeroFeature2 += "0.0,";
		}

		features = new ArrayList<String>();

		for (String verb : verbsList) {
			// System.out.println(verb);
			String s = verbToFeatures1.get(verb);
			if (s == null) {// no info about the verb!
				continue;
			}
			int lidx = s.lastIndexOf(',');
			s = s.substring(0, lidx);
			String s2 = verbToFeatures2.get(verb);
			if (s2 == null) {
				s2 = zeroFeature2 + getLabel(verb);
			}
			s += "," + s2;
			vfout.put(verb, s);
			features.add(s);

		}
		return features;
	}

	public ArrayList<String> generateFeaturesSim(
			HashMap<String, String> verbToFeatures,
			ArrayList<String> verbsList, int k) throws FileNotFoundException {

		ArrayList<String> featuresList1 = new ArrayList<String>();
		// Scanner sc = new Scanner(new File("sverbs2/dis" + k + ".txt"));
		Scanner sc = new Scanner(new File("sverbs/" + "dis" + 0 + ".txt"));
		// System.out.println(CoreNLP.verbMean);
		int numVerb = 0;

		while (sc.hasNext()) {
			String line = sc.nextLine();
			if (line.trim().equals("")) {
				continue;
			}
			String verb = line;

			line = sc.nextLine();
			String ll = line;
			line = line.replace("[", "").replace("]", "");
			sc.nextLine();
			String line2 = sc.nextLine();
			ll = ll + "\n" + line2;
			line2 = line2.replace("[", "").replace("]", "");

			String features = line + "," + line2;
			String[] vals = features.split(",");
			double[] values = new double[vals.length];
			numFeatures = vals.length;
			for (int i = 0; i < vals.length; i++) {
				values[i] = Double.parseDouble(vals[i].trim());
			}
			featuresList.add(ll);
			verbsList.add(verb);

			// accomodating knowledge about verb
			if (testBase) {
				int numPlus = 3;
				double vplus = 0;
				double vminus = 0;
				for (int i = 0; i < numPlus; i++) {
					// vplus = Math.max(vplus,values[i]);
					vplus = vplus + values[i];
				}
				vplus /= numPlus;
				for (int i = numPlus; i < vals.length; i++) {
					// vminus = Math.max(vminus,values[i]);
					vminus += values[i];
				}
				vminus /= (vals.length - numPlus);
				// System.out.println("mp "+ vminus+" "+vplus);

				features = features + ", " + vplus + ", " + vminus;
			}
			int label = getLabel(verb);
			// System.out.println(verb);
			// System.out.println(verb);
			// System.out.println("p: " + plabel+" l: "+label);

			features = features + label;
			verbToFeatures.put(verb, features);
			// System.out.println("it is: " + features);
			featuresList1.add(features);
			features = features.replace(" ", "");
			featuresToVerb.put(features, numVerb);
			// System.out.println(features );
			numVerb++;
		}

		makeSentenceFeatures(featuresList1, verbToFeatures, verbsList);

		// generateArffWithFeatures(featuresList1);
		return featuresList1;
		// System.out.println("NN TP: "+TP+" TN: "+TN);
	}

	HashMap<Integer, Integer> uniqeToCent = new HashMap<Integer, Integer>();

	void makeSentenceFeatures(ArrayList<String> featuresList1,
			HashMap<String, String> verbToFeatures, ArrayList<String> verbsList) {
		featuresList1.clear();
		HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
		verbsList.clear();

		int numFeatrures2 = verbToFeatures.values().iterator().next()
				.split(",").length;
		String zeroFeature2 = "";
		for (int i = 0; i < numFeatrures2 - 1; i++) {
			zeroFeature2 += "0.0,";
		}

		int i = 0;
		for (CompactQuantitativeEntity cent : cents) {
			String verb = cent.getVerb();
			String features = verbToFeatures.get(verb);
			if (features == null) {
				// System.out.println("was null "+verb);
				features = zeroFeature2 + getLabel(verb);
			}
			String v = verb + ":" + cent.getUniqueIdx();
			if (!zeroVerbs.contains(verb)) {
				featuresList1.add(features);
				uniqeToCent.put(cent.getUniqueIdx(), i);
				verbToFeatures2.put(v, features);
				verbsList.add(v);
				i++;
			}
		}
		verbToFeatures.clear();
		for (String s : verbToFeatures2.keySet()) {
			verbToFeatures.put(s, verbToFeatures2.get(s));
			// System.out.println(s + ": " + verbToFeatures2.get(s));
		}
	}

	static int getLabel(String verb) {
		// 0:minus, 1:plus
		int label = 0;
		try {
			int l = MathCoreNLP.verbMean.get(verb);
			label = l >= 0 ? 1 : 0;
			// label = Math.abs(l) == 2 ? 1 : -1;
		} catch (Exception e) {

		}
		return label;
	}

	static int getLabel(int l) {
		int label = l >= 0 ? 1 : 0;
		return label;
	}

	ArrayList<String> generateFeaturesStructure(
			HashMap<String, String> verbToFeatures, ArrayList<String> verbsList)
			throws FileNotFoundException {
		HashMap<String, Integer> relTon = new HashMap<String, Integer>();
		HashMap<String, double[]> verbToDFeatures = new HashMap<String, double[]>();
		HashMap<String, Integer> verbCount = new HashMap<String, Integer>();
		Scanner sc = new Scanner(new File("vrels.txt"));
		int i = 0;
		while (sc.hasNext()) {
			relTon.put(sc.nextLine(), i++);
		}
		int numRels = relTon.size();
		ArrayList<String> featuresList1 = new ArrayList<String>();

		for (CompactQuantitativeEntity cent : cents) {
			// System.out.println(w);
			// for (QuantitativeEntity cent : w.quantitativeEntities) {
			String realVerb = null;
			try {
				realVerb = cent.getVerb();
			} catch (Exception e) {
				// TODO: handle exception
				continue;
			}
			if (zeroVerbs.contains(realVerb)) {
				continue;
			}
			String verb = realVerb + ":" + cent.getUniqueIdx();

			double[] features = new double[numRels + 2];
			for (String s : cent.getVerbRels()) {
				if (!relTon.containsKey(s)) {
					continue;
				}
				features[relTon.get(s)] = 1;
			}
			try {
				features[numRels] = cent.getPathToVerb().split(",").length;
			} catch (Exception e) {
			}
			// features[numRels + 1] = af.isSubjMeaningFul() ? 1 : 0;

			if (!verbToDFeatures.containsKey(verb)) {

				verbsList.add(verb);
				int label = getLabel(realVerb);
				features[features.length - 1] = label;
				verbCount.put(verb, 1);
				verbToDFeatures.put(verb, features);
			} else {// we never come here for sentence based analysis
				int prevn = verbCount.get(verb);
				verbCount.put(verb, prevn + 1);
				double[] prevFeatures = verbToDFeatures.get(verb);

				for (int j = 0; j < prevFeatures.length - 1; j++) {
					prevFeatures[j] = (prevn * prevFeatures[j] + features[j])
							/ (prevn + 1);
				}
			}
			// }

		}

		// System.out.println("in structure");
		for (String verb : verbsList) {
			// System.out.println(verb);
			String featureStr = "";
			double[] features = verbToDFeatures.get(verb);
			for (int j = 0; j < features.length; j++) {
				if (j != features.length - 1) {
					featureStr += features[j] + ",";
				} else {
					featureStr += ((int) (features[j]) + "");
				}
			}
			featuresList1.add(featureStr);
			verbToFeatures.put(verb, featureStr);
			// System.out.println(verb + ": " + featureStr);
		}
		// if (verbsList.size() == 0) {
		// }
		return featuresList1;
		// generateArffWithFeatures(featuresList1);
	}

	ArrayList<String> generateFeaturesLexFileNames(
			HashMap<String, String> verbToFeatures, ArrayList<String> verbsList)
			throws FileNotFoundException {
		ArrayList<String> featuresList1 = new ArrayList<String>();
		Scanner sc = new Scanner(new File("verbs3.txt"));
		// System.out.println(CoreNLP.verbMean);

		int numVerb = 0;
		int maxIdx = 0;
		int minIdx = 60;
		// StanfordLemmatizer lemmatizer = new StanfordLemmatizer();
		while (sc.hasNext()) {
			String line = sc.nextLine();
			if (line.trim().equals("")) {
				continue;
			}
			StringTokenizer st = new StringTokenizer(line);
			String verb = st.nextToken();

			double[] values = null;
			try {
				values = WordNetHelper.getLexFileIdsFraction(verb);
			} catch (JWNLException e) {
				e.printStackTrace();
			}

			String features = "";
			for (int i = 0; i < values.length; i++) {
				features += values[i] + ",";
			}

			verbsList.add(verb);

			// accomodating knowledge about verb

			int label = getLabel(verb);
			// System.out.println(verb);

			// System.out.println(verb);
			// System.out.println("p: " + plabel+" l: "+label);

			features = features + label;
			verbToFeatures.put(verb, features);
			featuresToVerb.put(features, numVerb);
			// System.out.println("it is: " + features);
			featuresList1.add(features);
			features = features.replace(" ", "");
			// System.out.println(features );
			numVerb++;
		}
		// System.out.println("max min: " + maxIdx + " " + minIdx);
		makeSentenceFeatures(featuresList1, verbToFeatures, verbsList);
		// generateArffWithFeatures(featuresList1);
		return featuresList1;
		// System.out.println("NN TP: "+TP+" TN: "+TN);
	}

	double[] getDistances(String arffLine) {
		String[] splits = arffLine.split(",");
		int numAttrs = splits.length;
		double[] ret = new double[numAttrs];
		for (int i = 0; i < numAttrs; i++) {
			ret[i] = Double.parseDouble(splits[i].trim());
			// System.out.print(ret[i]+" ");
		}
		// System.out.println();
		return ret;
	}

	int NNpredict(String arffLine, int numPos) {
		double[] distances = getDistances(arffLine);
		ArrayList<Distance> dobjs = new ArrayList<Distance>();
		for (int i = 0; i < distances.length; i++) {
			dobjs.add(new Distance(i, distances[i]));
		}
		Collections.sort(dobjs);
		int fIndex = 0;
		// System.out.println(dobjs);
		// while (dobjs.get(fIndex+1).dis == dobjs.get(fIndex).dis &&
		// dobjs.get(fIndex).index>=numPos){
		// fIndex++;
		// }
		if (dobjs.get(fIndex).index < numPos) {
			return 1;
		} else {
			return -1;
		}
	}

	public static Classifier getNewClassifier(double gamma) throws Exception {
		// if (1 == 1) {
		// return new J48();
		// return new NaiveBayes();
		// }
		Classifier classifier1 = new SMO();
		if (gamma == -1) {
			SMO smo = ((SMO) classifier1);

			// ls.setKernelType(KERNELTYPE_RBF);

			smo.setBuildLogisticModels(true);
			return classifier1;
		}
		SMO smo = ((SMO) classifier1);
		smo.setBuildLogisticModels(true);
		smo.setOptions(new String[] { "-C", "1", "-L", ".001", "-P", "1.0E-12",
				"-N", "0", "-V", "-1", "-W", "1", "-K",
				"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0" });

		// smo.setOptions(weka.core.Utils
		// .splitOptions("-C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));

		// smo.setBuildLogisticModels(false);
		smo.setEpsilon(Math.pow(10, -12));
		smo.setToleranceParameter(.001);
		PolyKernel polyKernel = new PolyKernel();
		polyKernel.setOptions(new String[] { "-C", "25007", "-E", "1" });
		smo.setKernel(polyKernel);
		smo.setNumFolds(-1);

		// ls.setKernelType(KERNELTYPE_RBF);
		// RBFKernel rbf = new RBFKernel();
		// rbf.setGamma(gamma);
		// try {
		// rbf.setOptions(new String[] { "-C", "10" });
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// smo.setKernel(rbf);
		// smo.setBuildLogisticModels(true);

		LibSVM lb = new LibSVM();

		// lb.setGamma(gamma);
		// lb.setCost(10);
		try {
			lb.setOptions(new String[] { "-K", "2", "-G", gamma + "", "-C",
					"10" });
			// lb.setOptions(new String[] { "-K", "2", "-C", "1" });
			// lb.setOptions(new String[] {"-G", gamma + "", "-w1", "3", "-w0",
			// "1" });
			// lb.setOptions(new String[]{"-K","0"});
		} catch (Exception e) {
			e.printStackTrace();
		}
		lb.setProbabilityEstimates(true);

		// return lb;
		return smo;
	}

	int[] getFoldIndices(String[] sentenceVerbs, ArrayList<String> allVerbs) {
		ArrayList<Integer> retArr = new ArrayList<Integer>();
		int halfSize = sentenceVerbs.length / 2;
		int verbsSize = allVerbs.size() / 2;
		Random r = new Random();
		r.setSeed(SEED);
		int numUsedVerb = 0;
		// System.out.println("all verbs in two folds: " + allVerbs.size());
		while (numUsedVerb < verbsSize / 2) {
			int idx = (int) (r.nextDouble() * allVerbs.size());
			String verb = allVerbs.remove(idx);
			numUsedVerb++;
			for (int i = 0; i < sentenceVerbs.length; i++) {
				if (sentenceVerbs[i].equals(verb)) {
					retArr.add(i);
				}
			}
		}
		// System.out.println("num verbs in fold 1: " + numUsedVerb);
		int[] ret = new int[retArr.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = retArr.get(i);
		}
		return ret;
	}

	// each arr contains the indices of cents that are in that fold for test.
	// for careful, The sentences of 1/numFold of verbs are selected for each
	// fold.
	//
	ArrayList<ArrayList<Integer>> getFoldIndices(String[] sentenceVerbs,
			ArrayList<String> allVerbs, int numFold) {
		ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < numFold; i++) {
			ret.add(new ArrayList<Integer>());
		}

		int numInFold = allVerbs.size() / numFold;

		Random r = new Random();
		r.setSeed(SEED);
		int verbsSize = allVerbs.size();

		for (int k = 0; k < numFold; k++) {
			int count = k < numFold - 1 ? numInFold : verbsSize
					- ((numInFold) * (numFold - 1));
			// System.out.println("here");
			ArrayList<Integer> subjectArr = ret.get(k);
			for (int i = 0; i < count; i++) {
				int idx = (int) (r.nextDouble() * allVerbs.size());
				String verb = allVerbs.remove(idx);
				// System.out.println(verb);
				for (int j = 0; j < sentenceVerbs.length; j++) {
					if (sentenceVerbs[j].equals(verb)) {
						subjectArr.add(j);
					}
				}
			}
		}

		// System.out.println("num in folds from " + sentenceVerbs.length);
		// for (int i = 0; i < ret.size(); i++) {
		// System.out.println(ret.get(i).size());
		// }

		return ret;

	}

	ArrayList<ArrayList<Integer>> getFoldIndicesCareless(int[] numCentsInDS) {
		int numFold = numCentsInDS.length;
		int numInstances = numCentsInDS[numCentsInDS.length - 1];
		ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < numFold; i++) {
			ret.add(new ArrayList<Integer>());
		}
		int[] foldNumOfInstance = new int[numInstances];// what is the fold
														// number for each
														// instance
		int idx = 0;
		for (int i = 0; i < numInstances; i++) {

			if (i >= numCentsInDS[idx]) {
				idx++;
			}
			foldNumOfInstance[i] = idx;
		}

		for (int i = 0; i < numInstances; i++) {
			if (uniqeToCent.containsKey(i)) {
				ret.get(foldNumOfInstance[i]).add(i);
			}

		}

		// System.out.println("num in folds from ");
		// for (int i = 0; i < ret.size(); i++) {
		// System.out.println(ret.get(i).size());
		// }

		return ret;
	}

	ArrayList<ArrayList<Integer>> getFoldIndicesCarelessRand(int numFold,
			int numInss) {
		ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < numFold; i++) {
			ret.add(new ArrayList<Integer>());
		}
		ArrayList<Integer> allIdx = new ArrayList<Integer>();
		for (int i = 0; i < numInss; i++) {
			allIdx.add(i);
		}

		Random r = new Random(SEED);
		for (int i = 0; i < numFold; i++) {
			int numInsofFold = i < numFold - 1 ? numInss / numFold : allIdx
					.size();
			for (int j = 0; j < numInsofFold; j++) {
				int ri = r.nextInt(allIdx.size());
				int idx = allIdx.remove(ri);
				ret.get(i).add(idx);
			}
		}
		return ret;

	}

	Instances getTrainTest(int[] foldIndices, int k, Instances inss,
			boolean train, ArrayList<Integer> testIndices) {
		Instances ret = new Instances(inss);
		ret.delete();
		boolean useArr = false;
		if ((k == 0 && train) || (k == 1 && !train)) {
			useArr = true;
		}
		HashSet<Integer> foldSet = new HashSet<Integer>();
		for (int i = 0; i < foldIndices.length; i++) {
			foldSet.add(foldIndices[i]);
		}
		// System.out.println("Rets: ");
		for (int i = 0; i < cents.size(); i++) {
			if (useArr) {
				if (foldSet.contains(i)) {
					// System.out.println(i);
					ret.add(inss.instance(i));
					if (!train) {
						testIndices.add(i);
					}
				}
			} else {
				if (!foldSet.contains(i)) {
					// System.out.println(i);
					ret.add(inss.instance(i));
					if (!train) {
						testIndices.add(i);
					}
				}
			}
		}
		// System.out.println("ret size: " + ret.numInstances());
		return ret;
	}

	Instances getTrainTest(ArrayList<ArrayList<Integer>> foldIndices, int k,
			int numFolds, Instances inss, boolean train,
			ArrayList<Integer> testIndices) {
		Instances ret = new Instances(inss);
		ret.delete();

		HashSet<Integer> foldSet = new HashSet<Integer>();// It is a set
															// containing our
															// goal instances

		for (int i = 0; i < foldIndices.size(); i++) {
			if ((train && k != i) || (!train && k == i)) {
				for (int j : foldIndices.get(i)) {
					if (uniqeToCent.containsKey(j)) {
						foldSet.add(j);
					}
				}
			}

		}
		// System.out.println("fold set size: " + foldSet.size());
		// System.out.println("Rets: ");
		for (int i = 0; i < cents.size(); i++) {
			if (foldSet.contains(i)) {
				System.out.println(i);
				ret.add(inss.instance(uniqeToCent.get(i)));
				if (!train) {
					testIndices.add(uniqeToCent.get(i));
				}
			}
		}
		// System.out.println("ret size: " + ret.numInstances());
		return ret;
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

}

class ClassificationRes {
	double acc;
	double fm1;
	double fm2;

	public ClassificationRes(int TP, int TN, int FP, int FN) {
		int pos = TP + FN;
		int neg = TN + FP;
		int all = pos + neg;
		acc = (double) (TP + TN) / all;

	}
}
