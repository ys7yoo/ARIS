package dataSetGenerator;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.PDFTextStripperByArea;

public class ParseQuestions {

	public static void main(String[] args) throws FileNotFoundException {
		// File f = new File(
		// "D:/cources/research_UW/math_words/data/math-aids/word_add_1digit_2addend.pdf");
		// readPDF(f);
		// parseMathAidsFile(f);
		parseMathAids();
	}

	static String readPDF(File f) {
		PDDocument document;

		try {
			document = PDDocument.load(f);
			document.getClass();
			if (document.isEncrypted()) {
				try {
					document.decrypt("");
				} catch (InvalidPasswordException e) {
					System.err
							.println("Error: Document is encrypted with a password.");
					System.exit(1);
				}
			}

			PDFTextStripperByArea stripper = new PDFTextStripperByArea();
			stripper.setSortByPosition(true);
			PDFTextStripper stripper2 = new PDFTextStripper();
			String st = stripper2.getText(document);

			System.out.println(st);
			return st;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static void parseMathAids() throws FileNotFoundException {
		String folderName = "other/irrelevant_math-aids/";
		File folder = new File(
				folderName);
		File[] files = folder.listFiles();
		PrintStream op = new PrintStream(new File(folderName + "mathDS2.xls"));
		op.println("Question\tNum\tObject");
		for (File f : files) {
			if (!f.getName().endsWith(".pdf")){
				continue;
			}
			try {
				ArrayList<MAidQuestion> mqs = parseMathAidsFile(f);
				for (MAidQuestion mq : mqs) {
					op.println(mq.getExcelString());
				}
			} catch (Exception e) {
				System.err.println(f.getName());
				e.printStackTrace();
			}

		}
		op.close();
		System.err.println("END");

	}

	static ArrayList<MAidQuestion> parseMathAidsFile(File f) {
		String docs = readPDF(f);
		Scanner sc = new Scanner(docs);
		String line = sc.nextLine();
		while (!line.startsWith("1 )") && !line.startsWith("1)")) {
			line = sc.nextLine();
		}
		line = sc.nextLine();
		while (!line.startsWith("1 )") && !line.startsWith("1)")) {
			line = sc.nextLine();
		}
		int qnum = 1;
		ArrayList<MAidQuestion> maidQuestions = new ArrayList<MAidQuestion>();

		while (sc.hasNext()) {
			String q = "";
			qnum++;
			while (!line.startsWith(qnum + " )") &&
					!line.startsWith(qnum + ")")) {
				q = q + line + " ";
				if (!sc.hasNext()) {
					break;
				}
				line = sc.nextLine();
			}
			MAidQuestion mq = new MAidQuestion(q);
			if (mq.goodQ) {
				maidQuestions.add(mq);
			}
		}

		for (MAidQuestion mq : maidQuestions) {
			System.out.println(mq);
		}
		return maidQuestions;
	}

}
