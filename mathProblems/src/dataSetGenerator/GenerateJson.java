package dataSetGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonWriter;

public class GenerateJson {
	public static void main(String[] args) throws IOException {

		createJson();

	}

	static void createJson() throws IOException {
		Scanner sc = new Scanner(new File("DS1/q.txt"));
		Scanner sc2 = new Scanner(new File("DS1/ans.txt"));
		Scanner sc3 = new Scanner(new File("DS1/eq.txt"));
		
		JSONArray arr = new JSONArray();

		int i = 1;
		while (sc.hasNext()) {
			// if (i == 2) {
			// break;
			// }
			String q = sc.nextLine();
			String eq = sc3.nextLine();
			String ans = sc2.nextLine();
			Json j = new Json(i, q, eq, ans);
			arr.add(j.getJsonObj());
			System.out.println(j.getJsonObj());
			i++;
		}

		PrintStream op = new PrintStream(new File("DS1/DS1.json"));
		op.println(arr);
		op.close();
	}

}

class Json {
	int iIndex;
	String sQuestion;
	ArrayList<String> lEquations;
	ArrayList<String> lSolutions;

	public Json(int iIndex, String q, String eq, String ans) {
		this.iIndex = iIndex;
		this.sQuestion = q;
		this.lEquations = new ArrayList<String>();
		this.lEquations.add(eq);
		this.lSolutions = new ArrayList<String>();
		this.lSolutions.add(ans);
	}

	JSONObject getJsonObj() {
		JSONObject ret = new JSONObject();

		JSONArray l2 = new JSONArray();
		l2.add(lSolutions.get(0));
		ret.put("lSolutions", l2);

		JSONArray l = new JSONArray();
		l.add(lEquations.get(0));
		ret.put("lEquations", l);

		ret.put("sQuestion", sQuestion);

		ret.put("iIndex", iIndex);

		return ret;
	}
}