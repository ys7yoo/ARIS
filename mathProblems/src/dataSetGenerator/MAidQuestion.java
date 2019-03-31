package dataSetGenerator;

import java.util.Scanner;

public class MAidQuestion {
	String question;
	String answerObj;
	double answerNum;
	boolean goodQ;//is the question format acceptable
	
	boolean isGoodQuestion(){
		return goodQ;
	}
	
	public MAidQuestion(String s) {
		Scanner sc = new Scanner(s);
		System.err.println("s: "+s);
		try {
			sc.nextInt();
			sc.next();
		} catch (Exception e) {
			sc.next();
		}
		
		s = "";
		while (sc.hasNextLine()){
			s = s+sc.nextLine().trim()+" ";
		}
		s = s.trim();
		int lq = s.lastIndexOf('?');
		int fq = s.indexOf('?');
		if (fq != lq){
			System.out.println("bad question:\n"+s);
			return;
		}
		question = s.substring(0,lq+1);
		s = s.substring(lq+1).trim();
		System.out.println("ans s: "+s);
		int f_ = s.indexOf('_');
		s = s.substring(0,f_);
		sc = new Scanner(s);
		String numS = Util.dollarProcess(sc.next());
		try {
			answerNum = Double.parseDouble(numS);
		} catch (Exception e) {
			e.printStackTrace();
			goodQ = false;
			return;
		}
		
		try {
			answerObj = sc.nextLine();
		} catch (Exception e) {
			
		}
		
		if (sc.hasNext()){
			System.out.println("bad end "+question+" "+answerNum);
			goodQ = false;
		}
		goodQ = true;
	}
	
	public String toString(){
		String ret = "q: "+question+"\n"+"anso: "+answerObj+" ansn: "+answerNum;
		return ret;
	}
	
	public String getExcelString(){
		String ret = question+"\t"+answerNum+"\t"+answerObj;
		return ret;
	}
	
}
