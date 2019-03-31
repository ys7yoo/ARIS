package jsonGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import problemAnalyser.MathCoreNLP;
import problemAnalyser.QuantitativeEntity;
import verbAnalyze.SentenceAnalyzer;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import equationExtraction.World;

public class MathJsonGenerator {
	public static void main(String[] args) throws IOException {
		// if (1 == 1) {
		// readJsons();
		// return;
		// }
		if (args == null || args.length == 0) {
			System.out.println("Enter input file name");
			return;
		}
		String fileName = args[0];
		boolean hasQuestion = true;
		if (args.length > 1) {
			hasQuestion = Boolean.parseBoolean(args[1]);
		}
		String outName = SentenceAnalyzer.refined?"output_refined.txt":"output.txt";
		if (fileName != null) {
			
			generateJsonForInputFromFile(fileName, outName, hasQuestion);
		} else {
			generateJsonForInput(null, "output.txt", hasQuestion);
		}
	}

	public static void generateJsonForInput(String[] input, String outFileName,
			boolean hasQuestion) throws IOException {
		MathCoreNLP.debug = false;
		World[] worlds = MathCoreNLP.analyzeQuestionsWeb(input, null,
				hasQuestion);
		writeWorlds(worlds, outFileName);
	}

	public static void generateJsonForInputFromFile(String fileName,
			String outFileName, boolean hasQuestion) throws IOException {
		MathCoreNLP.debug = true;
		World[] worlds = MathCoreNLP.analyzeQuestionsWeb(null, fileName,
				hasQuestion);
		writeWorlds(worlds, outFileName);
	}

	static void writeWorlds(World[] worlds, String outFileName)
			throws IOException {
		if (outFileName == null) {
			outFileName = "output.txt";
		}
		PrintStream op = new PrintStream(new File(outFileName));
		int i = 0;
		int numQ = 0;
		for (World w : worlds) {
			ArrayList<CompactQuantitativeEntity> compactQEntities = new ArrayList<CompactQuantitativeEntity>();
			numQ++;
			// if (w==null){
			// continue;
			// }
			for (QuantitativeEntity qent : w.quantitativeEntities) {
				if (qent.getVerbid()!=null){
					compactQEntities.add(new CompactQuantitativeEntity(qent));
					i++;
				}
				System.out.println("cent " + i + ": " + qent);
			}
			if (w.qCEntity.getVerbid()!=null){
				compactQEntities.add(new CompactQuantitativeEntity(w.qCEntity));
				i++;
			}
			System.out.println("cent " + i + ": " + w.qCEntity);
			String json = JsonWriter.objectToJson(compactQEntities);
			System.out.println("NQ: "+numQ);
			if (!SentenceAnalyzer.refined){
				if (numQ==250 || numQ==390 || numQ==525){
					System.out.println("numberofcents: "+ i);
				}
			}
			else{
				if (numQ== 243 || numQ==383 || numQ==504){
					System.out.println("numberofcents: "+ i);
				}
			}
			
			// System.out.println(json);
			op.println(json);
		}
	}

	public static ArrayList<CompactQuantitativeEntity> readJsons()
			throws IOException {
		
		String fName = SentenceAnalyzer.refined?"output_refined.txt":"output.txt";
		
		Scanner sc = new Scanner(new File(fName));
		JsonReader jr = new JsonReader(new FileInputStream(new File(
				fName)));
		int i = 0;
		ArrayList<CompactQuantitativeEntity> ret = new ArrayList<CompactQuantitativeEntity>();
		while (true) {
			try {
				ArrayList<CompactQuantitativeEntity> cents = (ArrayList<CompactQuantitativeEntity>) jr
						.readObject();
				for (CompactQuantitativeEntity cent : cents) {
					if (cent.getVerb() != null) {
						ret.add(cent);
					}
				}
				// System.out.println(cents);
				i++;
				// System.out.println(i);
			} catch (Exception e) {
				break;
			}

		}

		return ret;
	}
}
