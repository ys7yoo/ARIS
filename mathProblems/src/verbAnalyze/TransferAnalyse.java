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

import problemAnalyser.MathCoreNLP;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class TransferAnalyse {

	ArrayList<String> verbsList = new ArrayList<String>();
	ArrayList<String> featuresList = new ArrayList<String>();
	HashMap<String, Integer> featuresToVerb = new HashMap<String, Integer>();

	public static void main(String[] args) throws Exception {
		TransferAnalyse arffGenerator = new TransferAnalyse();
		arffGenerator.generateArff();
		arffGenerator.analyzerArff();

	}

	void analyzerArff() throws Exception {
		DataSource source = new DataSource("verb2.arff");
		Instances inss = source.getDataSet();
		inss.setClassIndex(inss.numAttributes() - 1);
		ArrayList<ArrayList<String>> verbsTF = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < 4; i++) {
			verbsTF.add(new ArrayList<String>());
		}
		int numFolds = 10;
		int TP = 0;
		int TN = 0;
		int FP = 0;
		int FN = 0;
		int vindex = 0;
		inss.randomize(new Random());
		PrintStream op = new PrintStream(new File("pverbs2.txt"));

		for (int fold = 0; fold < numFolds; fold++) {
			Instances train = inss.trainCV(numFolds, fold);
			// for (int i=0; i<inss.numInstances(); i++){
			// System.out.println(inss.instance(i).value(inss.numAttributes()-1));
			// }
			Instances test = inss.testCV(numFolds, fold);
			Classifier cl = getNewClassifier(1);
			cl.buildClassifier(train);

			HashSet<String> zeroVerbs = new HashSet<String>();
			zeroVerbs.add("have");
			zeroVerbs.add("be");
			zeroVerbs.add("call");

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
				System.out.println(featuresList.get(vindex));
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
		
		for (ArrayList<String> a: verbsTF){
			System.out.println(a);
		}
		

	}

	void generateArff() throws FileNotFoundException {
		Scanner sc = new Scanner(new File("dis2.txt"));
		PrintStream op = new PrintStream(new File("verb2.arff"));
		// System.out.println(CoreNLP.verbMean);
		boolean firstTime = true;
		op.println("@RELATION dis");
		op.println();

		int numVerb = 0;
		int TP = 0;
		int TN = 0;

		while (sc.hasNext()) {
			String line = sc.nextLine();
			if (line.trim().equals("")) {
				continue;
			}
			String verb = line;

			line = sc.nextLine();
			String ll = line;
			line = line.replace("[", "").replace("]", "");
			String line2 = sc.nextLine();
			ll = ll + "\n" + line2;
			line2 = line2.replace("[", "").replace("]", "");
			String features = line + ", " + line2;
			String[] vals = features.split(",");
			double[] values = new double[vals.length];
			for (int i = 0; i < vals.length; i++) {
				values[i] = Double.parseDouble(vals[i].trim());
			}
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
			featuresList.add(ll);
			verbsList.add(verb);
			features = features + ", " + vplus + ", " + vminus;
			// System.out.println(verb);
			int label = -1;
			if (firstTime) {
				firstTime = false;
				int numAttrs = features.split(",").length;
				for (int i = 0; i < numAttrs; i++) {
					op.println("@ATTRIBUTE i" + i + " NUMERIC");
				}
				op.println("@ATTRIBUTE class        {1,-1}");
				op.println("@DATA");
			}

			try {
				label = Math.abs(MathCoreNLP.verbMean.get(verb)) >= 2 ? 1 : -1;
				System.out.println(verb);
				System.out.println(label);
			} catch (Exception e) {

			}
			int plabel = NNpredict(features, 3);
			// System.out.println(verb);
			// System.out.println("p: " + plabel+" l: "+label);
			if (plabel == label) {
				if (label == 1) {
					TP++;
				} else {
					TN++;
				}
			}
			features = features + ", " + label;
			op.println(features);
			features = features.replace(" ", "");
			featuresToVerb.put(features, numVerb);
			// System.out.println(features );
			numVerb++;
		}
		// System.out.println("NN TP: "+TP+" TN: "+TN);
		op.close();
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

	Classifier getNewClassifier(double gamma) {
		// return new J48();
		return new NaiveBayes();
		// Classifier classifier1 = new SMO();
		// if (gamma == -1) {
		// SMO smo = ((SMO) classifier1);
		//
		// // ls.setKernelType(KERNELTYPE_RBF);
		//
		// smo.setBuildLogisticModels(true);
		// return classifier1;
		// }
		// SMO smo = ((SMO) classifier1);
		//
		// // ls.setKernelType(KERNELTYPE_RBF);
		// RBFKernel rbf = new RBFKernel();
		// rbf.setGamma(gamma);
		// try {
		// rbf.setOptions(new String[] { "-C", "10" });
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// smo.setKernel(rbf);
		// smo.setBuildLogisticModels(true);
		//
		// LibSVM lb = new LibSVM();
		// lb.setProbabilityEstimates(true);
		//
		// // lb.setGamma(gamma);
		// // lb.setCost(10);
		// try {
		// // lb.setOptions(new String[] { "-K", "2", "-G", gamma + "", "-C",
		// // "10", "-w1", "10", "-w0", "1" });
		// lb.setOptions(new String[] {"-G", gamma + "", "-w1", "3", "-w0", "1"
		// });
		// // lb.setOptions(new String[]{"-K","0"});
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// lb.setProbabilityEstimates(true);
		// return lb;
		// return smo;
	}

}
