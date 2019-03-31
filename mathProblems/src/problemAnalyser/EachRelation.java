package problemAnalyser;

public class EachRelation {
	Entity ent1;
	Entity ent2;
	String n1;
	String n2;
	//n1 ent1 = n2 ent2
	public EachRelation(Entity ent1, Entity ent2, String n1, String n2) {
		this.ent1 = ent1;
		this.ent2 = ent2;
		this.n1 = n1;
		this.n2 = n2;
	}
	public String toString(){
		String ret = n1 +" "+ ent1.name + " contains "+n2+" "+ent2.name;
		System.out.println("str erel: "+ret);
		return ret;
	}
}
