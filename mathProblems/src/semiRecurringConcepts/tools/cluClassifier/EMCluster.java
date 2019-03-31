package semiRecurringConcepts.tools.cluClassifier;

import weka.core.Instance;
import weka.core.Instances;

public class EMCluster {
	int numClass;
	int numAttr;//without class attr
	double[] miu;
	int numIns;
	int numLabeledIns;
	int[] cnums;// num of instances in each class
	int maxIdx;
	
	int[] allCnums;//TODO: completely remove
	
	public EMCluster(Instances structure){
		this.numClass = structure.numClasses();
		this.numAttr = structure.numAttributes()-1;
		this.miu = new double[numAttr];
		cnums = new int[numClass];
		allCnums = new int[numClass];
	}
	
	double classify(Instance ins){
		return maxIdx;
	}
	
	double[] distributionsForInstance(Instance ins){
		double[] ret = new double[numClass];
		if (numLabeledIns==0){
			for (int i=0; i<numClass; i++){
				ret[i] = ((double) 1)/numClass;
			}
		}
		else{
			
			for (int i=0; i<numClass; i++){
				ret[i] = cnums[i]/numLabeledIns;
			}
		}
		return ret;
		
	}
	
	double getDis(Instance ins){
		double ret = 0;
		for (int i=0; i<ins.numAttributes()-1; i++){
			ret += Math.pow(miu[i]-ins.value(i), 2);
		}
		return ret;
	}
	
	double getDis(double[] values){
		double ret = 0;
		for (int i=0; i<values.length; i++){
			ret += Math.pow(miu[i]-values[i], 2);
		}
		return ret;
	}
	
	void mergeWithEMCluster(EMCluster clu){
		for (int i=0; i<numAttr; i++){
			miu[i] = ((clu.miu[i]*clu.numIns)+(miu[i]*numIns))/(numIns+clu.numIns); 
		}
		numIns += clu.numIns;
		numLabeledIns += clu.numLabeledIns;
		for (int i=0; i<numClass; i++){
			cnums[i] += clu.cnums[i];
			allCnums[i] += clu.allCnums[i];
		}
		setMaxIdx();
	}
	
	void setMaxIdx(){
		int mi = 0;
//		System.out.println("cnums");
//		System.out.println(cnums[0]);
		for (int i=1; i<cnums.length; i++){
//			System.out.println(cnums[i]);
			if (cnums[i]>cnums[mi]){
				mi = i;
			}
		}
		maxIdx = mi;
	}
	
	public String toString(){
		String ret = "";
		for (int i=0; i<numClass; i++){
			ret+= cnums[i]+(" ");
		}
		ret += ("num ins: ")+" ";
		for (int i=0; i<numClass; i++){
			ret+= allCnums[i]+(" ");
		}
		return ret;
	}
	
	void addInstance(Instance ins, boolean hasLabel){
		for (int i=0; i<numAttr; i++){
			miu[i] = ((miu[i]*numIns)+ins.value(i))/(numIns + 1);
		}
		allCnums[(int)ins.classValue()]++;
		numIns++;
		if (hasLabel){
			cnums[(int)ins.classValue()]++;
			numLabeledIns++;
		}
		setMaxIdx();
	}
	
	void removeInstance(Instance ins, boolean hasLabel){
		for (int i=0; i<numAttr; i++){
			if (numIns==1){
				miu[i] = 0;
			}
			else{
				miu[i] = ((miu[i]*numIns)-ins.value(i))/(numIns - 1);
			}
			
		}
		numIns--;
		allCnums[(int)ins.classValue()]--;
		if (hasLabel){
			cnums[(int)ins.classValue()]--;
			numLabeledIns--;
		}
		setMaxIdx();
	} 
}
