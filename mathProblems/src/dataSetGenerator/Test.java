package dataSetGenerator;

import java.io.File;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.StringTokenizer;

import equationExtraction.World;

public class Test {
	static ArrayList<HashSet<String>> ops;
	
	public static void main(String[] args) throws FileNotFoundException {
//		readVerbsH();
		testEquations();
//		refineAns();
//		refineEq();
//		whatsdifference();
//		testSimpeMethod();
//		readTest();
	}
	static void readTest() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("b.txt"));
		while (sc.hasNext()){
			String txt = sc.nextLine();
			int i = txt.indexOf('?');
			txt = txt.substring(i+2);
//			System.out.println(txt);
			Scanner sc2 = new Scanner(txt);
			double d =  sc2.nextDouble();
			System.out.println(d);
		}
	}
	static void readVerbsH() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("verbs.txt"));
		HashMap<String, String> verbs = new HashMap<String, String>();
		while (sc.hasNext()){
			String nextLine = sc.nextLine();
			StringTokenizer st = new StringTokenizer(nextLine);
			verbs.put(st.nextToken(), nextLine) ;
		}
		Scanner scnew = new Scanner(new File("verbsn.txt"));
		while (scnew.hasNext()){
			String nextLine = scnew.nextLine();
//			System.out.println(nextLine);
			Scanner sc2 = new Scanner(nextLine);
			String verb = sc2.next();
			if (verbs.containsKey(verb)){
				System.out.println(verbs.get(verb));
			}
			else{
				System.out.println(verb+" not found");
			}
//			System.out.print(verb+" ");
//			sc2.next();
//			
//			
//			System.out.println(sc2.nextLine());
		}
	}
	static void testSimpeMethod() throws FileNotFoundException{
		ops = new ArrayList<HashSet<String>>();
		Scanner sc = new Scanner(new File("base.txt"));
		HashSet<String> h = new HashSet<String>();
		ops.add(h);
		while (sc.hasNext()){
			String l = sc.nextLine();
			if (l.trim().length()==0){
				h = new HashSet<String>();
				ops.add(h);
			}
			else{
				h.add(l);
			}
		}
		for (HashSet<String> hh:ops){
			System.out.println(hh);
		}
	}
	public static int BaseGuess(String q){
		StringTokenizer st = new StringTokenizer(q);
		ArrayList<Double> nums = new ArrayList<Double>();
		
		while (st.hasMoreTokens()){
			String tok = st.nextToken();
			double d;
			try {
				d = Double.parseDouble(tok);
				nums.add(d);
			} catch (Exception e) {
			}
		}
		return 0;
	}
	
	static void testEquations() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("DfN/DS2/ans.txt"));
		Scanner eqsc = new Scanner(new File("DfN/DS2/eq.txt"));
		int i=1;
		while (sc.hasNext()){
			double res = sc.nextDouble();
			String eq = eqsc.nextLine();
			double sres = World.solveEquation(eq, "X");
			if (Math.abs(res-sres)>.00001){
				System.out.println(i+" "+sres);
				System.out.println(eq);
			}
			i++;
		}
		System.out.println(i);
	}
	
	static void whatsdifference() throws FileNotFoundException{
		Scanner sc1 = new Scanner(new File("a.txt"));
		Scanner sc2 = new Scanner(new File("DfN/refined/DS1/q.txt"));
		String l2 = sc2.nextLine();
		int i=1;
		while (sc1.hasNext()){
			String l = sc1.nextLine();
			
			if (l.equals(l2)){
				l2 = sc2.next();
				continue;
			}
			else{
				System.out.println(i);
				System.out.println(l);
			}
			i++;
		}
	}
	
	
	
	static void refineAns() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("DfN/DS2/ans.txt"));
		while (sc.hasNext()){
			String s = sc.nextLine();
			System.out.println(problemAnalyser.Util.getDouble(s));
		}
	}
	
	static void refineEq() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("DfN/DS2/eq.txt"));
		while (sc.hasNext()){
			String l = sc.nextLine();
			Scanner sc2 = new Scanner(l);
			
			ArrayList<String> tokens = new ArrayList<String>();
			while (sc2.hasNext()){
				String tok = sc2.next();
//				System.out.println(tok);
				if (isNumber(tok)){
					tokens.add(problemAnalyser.Util.getDouble(tok)+"");
				}
				else{
					tokens.add(tok);
				}
			}
			
//			boolean isDouble = false;
			double lastNum = 0;
			String s = "";
			for (String tok:tokens){
//				System.out.println(tok);
				if (!isNumber(tok)){
					if (lastNum!=0){
						s += lastNum +" ";
						lastNum = 0;
					}
					s += tok+" ";
//					System.out.println("here "+tok);
				}
				else{
					lastNum += Double.parseDouble(tok);
				}
			}
			if (lastNum!=0){
				s += lastNum;
			}
			
			System.out.println(s.trim());
		}
	}
	
	static boolean isNumber(String s){
		char c = s.charAt(0);
		if (c<='9' && c>='0'){
			return true;
		}
		return false;
	}
	
//	ArrayList<String> tokensPreProcess(ArrayList<String> toks){
//		ArrayList<String> ret = new ArrayList<String>();
//		for (String s:)
//	}
	
}
