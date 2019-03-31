package labelExtraction;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import problemAnalyser.QuestionAnalyzer;

public class QuestionLabel {
	ArrayList<QuantitativeEntity> cents;
	QuantitativeEntity qent;
	public Assignments assignments;
	double correctAns;
	
	public static void main(String[] args) throws FileNotFoundException {
		MathCoreNLP.PREDICT = false;
		MathCoreNLP.analyzeQuestionsforGuessLabel(null, null, true);
	}
	
	public QuestionLabel(ArrayList<QuantitativeEntity> cents, QuantitativeEntity qent, double ans) {
		this.cents = cents;
		this.qent = qent;
		this.correctAns = ans;
		setAssignments();
	}
	
	void setAssignments(){
		this.assignments = new Assignments(this);
	}
	
}
