package bepler.seq.svm;

public class Main {
	
	public static void main(String[] args){
		ModuleFactory fac = new ModuleFactory();
		//if(args.length > 0){
			try{
				Module m = fac.build(args[0]);
				String[] modArgs = new String[args.length - 1];
				System.arraycopy(args, 1, modArgs, 0, modArgs.length);
				try {
					m.setArgs(modArgs);
					m.execute();
				} catch (Exception e) {
					System.err.println(m.usage());
					System.exit(1);
				}
			} catch (Exception e){
				System.err.println(fac.usage());
				System.exit(2);
			}
		//}
	}
	
	
}
