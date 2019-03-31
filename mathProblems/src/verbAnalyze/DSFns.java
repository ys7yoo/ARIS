package verbAnalyze;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;

import problemAnalyser.MathCoreNLP;

public class DSFns {
	public static void main(String[] args) throws FileNotFoundException {
//		decouple();
		makeNewVerbs();
	}
	static void decouple() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("ixl_all.txt"));
		while (sc.hasNext()){
			String l = sc.nextLine();
			int qmarkPlace = l.lastIndexOf("?");
			String q = l.substring(0,qmarkPlace+1);
			String res = l.substring(qmarkPlace+1).trim();
//			System.out.println(q);
			System.out.println(res);
		}
	}
	static void makeNewVerbs() throws FileNotFoundException{
		Scanner sc = new Scanner(new File("verbs2.txt"));
		while (sc.hasNext()){
			String line = sc.nextLine();
			StringTokenizer st = new StringTokenizer(line);
			String v = st.nextToken();
			if (!MathCoreNLP.verbMean.containsKey(v)){
				System.out.println(v+"\t?");
			}
		}
	}
}
