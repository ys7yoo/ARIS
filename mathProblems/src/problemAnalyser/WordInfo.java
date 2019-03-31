package problemAnalyser;

import java.util.ArrayList;

public class WordInfo {
	String name;
	String lemma;
	String pos;
	String NE;
	
	public WordInfo(String name, String lemma, String pos, String NE){
		this.name = name;
		this.pos = pos;
		this.lemma = lemma;
		this.NE = NE;
	}
	public String toString(){
		return (name+" "+pos+" "+lemma+" "+NE);
	}
}
