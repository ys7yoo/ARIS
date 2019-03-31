package dataSetGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class GenerateTrainTest {
	public static void main(String[] args) throws FileNotFoundException {
//		divideTrainTest();
		generatePure();
	}
	static void divideTrainTest() throws FileNotFoundException{
		String root = "other/irrelevant_math-aids/";
		Scanner sc = new Scanner(new File(root + "mathDS2.xls"));
		PrintStream train = new PrintStream(new File(root+"train.xls"));
		PrintStream test = new PrintStream(new File(root+"test.xls"));
		HashSet<Integer> testNums = getTestPos(75, 75);
		System.out.println(testNums.size());
//		PrintStream train = new PrintStream(new File(""))
		int i=0;
		while (sc.hasNextLine()){
			String line = sc.nextLine();
			if (testNums.contains(i)){
				test.println(line);
			}
			else{
				train.println(line);
			}
			i++;
		}
		train.close();
		test.close();
		sc.close();
	}
	static HashSet<Integer> getTestPos(int N, int testNum){
		ArrayList<Integer> all = new ArrayList<Integer>();
		for (int i=0; i<N; i++){
			all.add(i);
		}
		HashSet<Integer> test = new HashSet<Integer>();
		for (int i=0; i<testNum; i++){
			int rand = (int) (Math.random()*all.size());
			int tnum = all.remove(rand);
			test.add(tnum);
		}
		return test;
	}
	
	public static void generatePure() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("DfN/a.txt"));
		Scanner sc2 = new Scanner(new File("DfN/aa.txt"));
		Scanner scnot = new Scanner(new File("DfN/not +-.txt"));
		HashSet<String> notS = new HashSet<String>();
		while (scnot.hasNext()){
			notS.add(scnot.nextLine());
		}
		PrintStream op = new PrintStream(new File("pure_a.txt"));
		PrintStream op2 = new PrintStream(new File("pure_aa.txt"));
		while (sc.hasNext()){
			String l = sc.nextLine();
			String a = sc2.nextLine();
			if (!notS.contains(l)){
				op.println(l);
				op2.println(a);
			}
		}
		
	}
}
