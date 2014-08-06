package bepler.seq.svm;

public class Main {
	
	public static void main(String[] args){
		Parse p = null;
		try {
			p = new Parse(args);
		} catch (Exception e) {
			System.err.println(Parse.usage());
			System.exit(1);
		}
		if(p != null){
			p.execute();
		}
	}
	
	
}
