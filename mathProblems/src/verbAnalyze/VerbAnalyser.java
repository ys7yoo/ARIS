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

import net.didion.jwnl.JWNLException;
import edu.cmu.lti.ws4j.impl.Lin;
import equationExtraction.World;
import problemAnalyser.AF;
import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
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

public class VerbAnalyser {

	ArrayList<String> featuresList = new ArrayList<String>();
	HashMap<String, Integer> featuresToVerb = new HashMap<String, Integer>();
	public HashMap<String, String> verbToFeatures = new HashMap<String, String>();
	public int numFeatures;
	boolean testBase = false;
	int numFolds = 3;

	public static int SIM = 0, STRUCTURE = 1, SIMandSTRUCTURE = 2, LEX = 3,
			SIMandLEX = 4, STRUCTUREandLEX = 5, ALL = 6;

	public static void main(String[] args) throws Exception {
		// MathCoreNLP.verbsAddress = "verbs.txt";
		// MathCoreNLP.setVerbMean();
		VerbAnalyser arffGenerator = new VerbAnalyser();
		// arffGenerator.generateArffSim();

		ArrayList<String> verbsList = arffGenerator
				.generateArffWithFeatures(ALL);
		arffGenerator.analyzerArff(verbsList);
	}
	
	void analyzerArff(ArrayList<String> verbsList) throws Exception {
		if (verbsList.size() == 0) {

		}
		System.out.println("analyzing");
		ArrayList<ArrayList<String>> verbsTF = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < 4; i++) {
			verbsTF.add(new ArrayList<String>());
		}

		
		int TP = 0;
		int TN = 0;
		int FP = 0;
		int FN = 0;
		int vindex = 0;
		// inss.randomize(new Random());
		PrintStream op = new PrintStream(new File("pverbs.txt"));
		Instances inss = null;
		for (int fold = 0; fold < numFolds; fold++) {
			System.out.println("f: " + fold);
			DataSource source = new DataSource("arff/verb" + fold + ".arff");
			inss = source.getDataSet();
			inss.setClassIndex(inss.numAttributes() - 1);
			Instances train = inss.trainCV(numFolds, fold);
			// for (int i=0; i<inss.numInstances(); i++){
			// System.out.println(inss.instance(i).value(inss.numAttributes()-1));
			// }
			Instances test = inss.testCV(numFolds, fold);
			Classifier cl = getNewClassifier(-1);
			cl.buildClassifier(train);

			HashSet<String> zeroVerbs = new HashSet<String>();
			zeroVerbs.add("have");
			zeroVerbs.add("be");

			for (int i = 0; i < test.numInstances(); i++) {
				// System.out.println(test.instance(i));
				int plabel = (int) cl.classifyInstance(test.instance(i));
				System.out.println("plabel is: " + plabel);

				int label = (int) test.instance(i).value(
						inss.numAttributes() - 1);
				// double p = cl.distributionForInstance(test.instance(i))[0];
				// if (p>=.5){
				// plabel = 0;
				// }
				// else{
				// plabel = 1;
				// }
				String verb = verbsList.get(vindex);
				// System.out.println(verb);
				// System.out.println("p: " + plabel+" l: "+label+" "+p);
				op.print(verbsList.get(vindex) + "\t");

				if (zeroVerbs.contains(verb)) {
					op.println("0");
				} else {
					op.println((plabel == 0 ? "t+" : "t-"));
				}
				// System.out.println("hi "+vindex);

				if (plabel == label) {
					if (plabel == 0) {
						verbsTF.get(0).add(verb);
						TP++;
					} else {
						verbsTF.get(1).add(verb);
						TN++;
					}
				} else {
					if (plabel == 0) {
						verbsTF.get(2).add(verb);
						FP++;
					} else {
						verbsTF.get(3).add(verb);
						FN++;
					}
				}
				// int label =
				// System.out.println(test.instance(i));
				// vindex = featuresToVerb.get(test.instance(i).toString());
				System.out.println(verbsList.get(vindex) + ": real label: "
						+ (label == 0 ? "+1" : "-1") + " "
						+ (label == plabel ? "true" : "false"));
				// System.out.println(featuresList.get(vindex));
				System.out.println("whole instance: " + test.instance(i));
				System.out.println();
				vindex++;
			}
		}
		op.close();
		int numCor = TP + TN;
		System.out.println("acc: " + (double) numCor / inss.numInstances());

		System.out.println("TP: " + TP + " FN: " + FN);
		System.out.println("FP: " + FP + " TN: " + TN);
		String[] charac = new String[] { "TP: ", "TN: ", "FP: ", "FN: " };

		for (int i = 0; i < verbsTF.size(); i++) {
			ArrayList<String> a = verbsTF.get(i);
			System.out.println(charac[i] + a);
		}

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
		
		ArrayList<ArrayList<String>> simFeatures = new ArrayList<ArrayList<String>>();
		ArrayList<HashMap<String, String>> simVerbToFeatures = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<String>> arffFeatures = new ArrayList<ArrayList<String>>();// These

		for (int k = 0; k < numFolds; k++) {
			verbList = new ArrayList<String>();
			simVerbToFeatures.add(new HashMap<String, String>());
			features = generateFeaturesSim(simVerbToFeatures.get(k), verbList,
					0);
			simFeatures.add(features);
		}
		if (type == SIM) {
			arffFeatures = simFeatures;
		} else if (type == STRUCTURE) {
			verbList = new ArrayList<String>();
			features = generateFeaturesStructure(new HashMap<String, String>(),
					verbList);
			for (int k = 0; k < numFolds; k++) {
				arffFeatures.add(features);
			}
		} else if (type == SIMandSTRUCTURE) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesStructure(verbToFeatures2, verbList);
			for (int k = 0; k < numFolds; k++) {
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
			for (int k = 0; k < numFolds; k++) {
				features = mergeFeatures(verbToFeatures1, verbToFeatures2,
						verbList, new HashMap<String, String>());
				arffFeatures.add(features);
			}

		} else if (type == LEX) {
			verbList = new ArrayList<String>();
			features = generateFeaturesLexFileNames(
					new HashMap<String, String>(), verbList);
			for (int k = 0; k < numFolds; k++) {
				arffFeatures.add(features);
			}
		} else if (type == SIMandLEX) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesLexFileNames(verbToFeatures2, verbList);
			for (int k = 0; k < numFolds; k++) {
				features = mergeFeatures(simVerbToFeatures.get(k),
						verbToFeatures2, verbList,
						new HashMap<String, String>());
				System.out.println("the size: " + features.size());
				arffFeatures.add(features);
			}
		} else if (type == ALL) {
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures2 = new HashMap<String, String>();
			generateFeaturesLexFileNames(verbToFeatures2, verbList);
			verbList = new ArrayList<String>();
			HashMap<String, String> verbToFeatures3 = new HashMap<String, String>();
			generateFeaturesStructure(verbToFeatures3, verbList);
			for (int k = 0; k < numFolds; k++) {
				HashMap<String, String> m1 = new HashMap<String, String>();
				mergeFeatures(simVerbToFeatures.get(k), verbToFeatures2,
						verbList, m1);
				features = mergeFeatures(m1, verbToFeatures3, verbList,
						new HashMap<String, String>());
				arffFeatures.add(features);
			}

		}
		for (int k = 0; k < numFolds; k++) {
			PrintStream op = new PrintStream(
					new File("arff/verb" + k + ".arff"));
			op.println("@RELATION dis");
			op.println();
			features = arffFeatures.get(k);
			int numAttrs = features.get(0).split(",").length - 1;
			for (int i = 0; i < numAttrs; i++) {
				op.println("@ATTRIBUTE i" + i + " NUMERIC");
			}
			op.println("@ATTRIBUTE class        {1,-1}");
			op.println("@DATA");

			for (String s : features) {
				System.out.println("that was: " + s);
				op.println(s);
			}
			op.close();
		}

		return verbList;
	}

	ArrayList<String> mergeFeatures(HashMap<String, String> verbToFeatures1,
			HashMap<String, String> verbToFeatures2,
			ArrayList<String> verbsList, HashMap<String, String> vfout) {
		ArrayList<String> features = new ArrayList<String>();
		int numFeatrures2 = verbToFeatures2.get("be").split(",").length;
		String zeroFeature2 = "";
		for (int i = 0; i < numFeatrures2 - 1; i++) {
			zeroFeature2 += "0.0,";
		}

		features = new ArrayList<String>();

		for (String verb : verbsList) {
			System.out.println(verb);
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
//		Scanner sc = new Scanner(new File("sverbs/"+numFolds+"/dis" + 0 + ".txt"));
		Scanner sc = new Scanner(new File("sverbs2/"+"dis" + 0 + ".txt"));
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
			System.out.println("it is: " + features);
			featuresList1.add(features);
			features = features.replace(" ", "");
			featuresToVerb.put(features, numVerb);
			// System.out.println(features );
			numVerb++;
		}
		// generateArffWithFeatures(featuresList1);
		return featuresList1;
		// System.out.println("NN TP: "+TP+" TN: "+TN);
	}

	static int getLabel(String verb) {
		int label = -1;
		try {
			int l = MathCoreNLP.verbMean.get(verb);
			label = l >= 0 ? 1 : -1;
			// label = Math.abs(l) == 2 ? 1 : -1;
		} catch (Exception e) {
			
		}
		return label;
	}

	ArrayList<String> generateFeaturesStructure(
			HashMap<String, String> verbToFeatures, ArrayList<String> verbsList)
			throws FileNotFoundException {
		World[] worlds = null;
		try {
			worlds = MathCoreNLP.analyzeQuestionsWeb(null, "input.txt", true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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

		for (World w : worlds) {
			if (w == null) {
				continue;
			}
			// System.out.println(w);
			for (QuantitativeEntity cent : w.quantitativeEntities) {
				AF af = null;
				String verb = null;
				try {
					af = cent.getAf();
					verb = af.getVerbid().lemma();
				} catch (Exception e) {
					// TODO: handle exception
					continue;
				}

				double[] features = new double[numRels + 2];
				for (String s : af.getVerbRels()) {
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
					int label = getLabel(verb);
					features[features.length - 1] = label;
					verbCount.put(verb, 1);
					verbToDFeatures.put(verb, features);
				} else {
					int prevn = verbCount.get(verb);
					verbCount.put(verb, prevn + 1);
					double[] prevFeatures = verbToDFeatures.get(verb);

					for (int j = 0; j < prevFeatures.length - 1; j++) {
						prevFeatures[j] = (prevn * prevFeatures[j] + features[j])
								/ (prevn + 1);
					}
				}
			}

		}
		for (String verb : verbsList) {
			System.out.println(verb);
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
//		String vaddress = "verbs2.txt";
		String vaddress = MathCoreNLP.verbsAddress;
		Scanner sc = new Scanner(new File(vaddress));
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
			System.out.println("it is: " + features);
			featuresList1.add(features);
			features = features.replace(" ", "");
			// System.out.println(features );
			numVerb++;
		}
		System.out.println("max min: " + maxIdx + " " + minIdx);
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

}

class Distance implements Comparable {
	int index;
	double dis;

	public Distance(int index, double dis) {
		this.index = index;
		this.dis = dis;
	}

	public int compareTo(Object o) {
		Distance d = (Distance) o;
		if (this.dis > d.dis) {
			return 1;
		} else if (this.dis == d.dis) {
			return 0;
		}
		return -1;
	}

	public String toString() {
		return "idx: " + index + " dis: " + dis;
	}
}