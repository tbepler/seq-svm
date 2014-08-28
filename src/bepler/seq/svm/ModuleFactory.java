package bepler.seq.svm;

public class ModuleFactory {
	
	public static final String TRAIN = "train";
	public static final String TEST = "test";
	
	public Module build(String name) throws Exception{
		switch(name){
		case TRAIN: return new Train();
		case TEST: return new Test();
		default: throw new RuntimeException("Unknown module: "+name);
		}
	}
	
	public String[] modules(){
		return new String[]{TRAIN, TEST};
	}
	
	public String usage(){
		String s = "Usage: seqsvm module\nModules: ";
		for(String m : modules()){
			s += "\n" + m;
		}
		return s;
	}
	
}
