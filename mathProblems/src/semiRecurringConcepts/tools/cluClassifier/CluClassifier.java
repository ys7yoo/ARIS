package semiRecurringConcepts.tools.cluClassifier;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instance;
import weka.core.Instances;

public class CluClassifier extends NaiveBayesUpdateable {
	public static double minAcceptableRatio=.7;// used to check whether an instance
	// can be assigned to a cluster or not due to the labels...
	public static int maxIter = 20;
	int K;
	EMCluster[] clusters;
	int numIns;
	static boolean debugMode = false;

	public void addBatch(Instance[] batch, boolean[] hasLabels) {
		myprintln("adding batch");
		int[] assignedNums = new int[batch.length];
		for (int i = 0; i < assignedNums.length; i++) {
			assignedNums[i] = -1;
		}
		int iterCount = 0;
		boolean first = true;
		while (update(batch, hasLabels, assignedNums, first)
				&& iterCount < maxIter) {
			for (int k = 0; k < K; k++) {
				System.out.println("cluster " + k+" "+clusters[k].numIns);
				
			}
			myprintln("next iter");
			first = false;
			for (int i = 0; i < K; i++) {
				myprintln(clusters[i].toString());
			}
			iterCount++;
		}
		numIns += batch.length;
		myprintln("num ins: " + numIns);
	}

	// returns the continue signal!
	boolean update(Instance[] batch, boolean[] hasLabels, int[] assignedNums,
			boolean first) {
		System.out.println("updating");
		int[] newAssignedNums = new int[batch.length];
		boolean shouldContinue = false;
		for (int i = 0; i < batch.length; i++) {
			if (numIns == 0 && first) {
				// if (hasLabels[i] && batch[i].classValue()==0){
				// newAssignedNums[i] = (i%K)/2;
				// }
				// else if (hasLabels[i]){
				// newAssignedNums[i] = K-1- (i%K)/2;
				// }
				// else{
				System.out.println("assign to k");
				newAssignedNums[i] = i % K;
				// }
			} else {
				newAssignedNums[i] = getAssignedCluster(batch[i], hasLabels[i]);
				// myprintln("new assi: "+newAssignedNums[i]);
				// myprintln("prev assi: "+assignedNums[i]);
			}
		}
		for (int i = 0; i < batch.length; i++) {
			if (assignedNums[i] == -1) {
				shouldContinue = true;
				assignedNums[i] = newAssignedNums[i];
				clusters[assignedNums[i]].addInstance(batch[i], hasLabels[i]);
			} else if (assignedNums[i] != newAssignedNums[i]) {
				shouldContinue = true;
				clusters[assignedNums[i]]
						.removeInstance(batch[i], hasLabels[i]);
				clusters[newAssignedNums[i]]
						.addInstance(batch[i], hasLabels[i]);
				assignedNums[i] = newAssignedNums[i];
			}
		}
		myprintln("sh " + shouldContinue);
		return shouldContinue;
	}

	//expectation
	public int getAssignedCluster(Instance ins, boolean hasLabel) {
		if (hasLabel){
			System.out.println("HAS LABEL");
		}
		if (!hasLabel) {
			return getNearestNeighbor(ins);
		}
		int bestIdx = -1;
		double minDist = 1000000;
		for (int i = 0; i < K; i++) {
			System.out.println(((double) clusters[i].cnums[(int) ins.classValue()])
					+" "+ clusters[i].numLabeledIns);
			if (((double) clusters[i].cnums[(int) ins.classValue()])
					/ clusters[i].numLabeledIns < minAcceptableRatio) {
				 myprintln("using an assumption"+((double)clusters[i].cnums[(int)ins.classValue()])/clusters[i].numLabeledIns);
				continue;
			} else {
				// myprintln("no assumption"+((double)clusters[i].cnums[(int)ins.classValue()])/clusters[i].numLabeledIns);
			}
			System.out.println("CONSTRAINT");
			double temp;
			if ((temp = clusters[i].getDis(ins)) < minDist) {
				bestIdx = i;
				minDist = temp;
			}
		}
		if (bestIdx == -1) {
			return getNearestNeighbor(ins);
		}
		return bestIdx;
	}

	public CluClassifier(Instances structure, int K) {
		this.K = K;
		numIns = 0;
		clusters = new EMCluster[K];
		for (int i = 0; i < K; i++) {
			clusters[i] = new EMCluster(structure);
		}
	}

	public double classifyInstance(Instance ins) {
		// myprintln("classifying");
		int bestIdx = getNearestNeighbor(ins);
		myprintln("b idx: " + bestIdx);
		for (int i = 0; i < K; i++) {
			myprintln(clusters[i].toString());
		}
		double ret = clusters[bestIdx].classify(ins);
		// try{
		// throw new RuntimeException("hey");
		//
		// }catch (Exception e) {
		// e.printStackTrace();
		// }
		if (ins.classValue() != ret) {
			myprintln("not correct");
		}
		myprintln("label: " + ret + " real label: " + ins.classValue());
		return ret;
	}

	int getNearestNeighbor(Instance ins) {
		int bestIdx = 0;
		double minDist = clusters[0].getDis(ins);

		myprintln("dist: " + minDist);
		for (int i = 1; i < K; i++) {

			double temp = clusters[i].getDis(ins);
			myprintln("dist: " + "" + temp);
			if (temp < minDist) {
				bestIdx = i;
				minDist = temp;
			}
		}
		return bestIdx;
	}

	// notAcceptable is sorted

	int getNearestNeighbor(double[] miu) {
		int bestIdx = 0;
		double minDist = clusters[0].getDis(miu);
		for (int i = 1; i < K; i++) {
			double temp;
			if ((temp = clusters[i].getDis(miu)) < minDist) {
				bestIdx = i;
				minDist = temp;
			}
		}
		return bestIdx;
	}

	public double[] distributionForInstance(Instance ins) {
		int bestIdx = getNearestNeighbor(ins);

		return clusters[bestIdx].distributionsForInstance(ins);
	}

	public void mergeWithNaive(NaiveBayes nb) {
		CluClassifier cla = (CluClassifier) nb;
		for (int i = 0; i < cla.K; i++) {
			int bestIdx = getNearestNeighbor(cla.clusters[i].miu);
			clusters[bestIdx].mergeWithEMCluster(cla.clusters[i]);
		}
		numIns += cla.numIns;
	}

	void myprintln(String s) {
		if (debugMode) {
			System.out.println(s);
		}
	}
}
