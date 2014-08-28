package bepler.seq.svm;

public interface Module {

	public String usage();

	public void setArgs(String[] args) throws Exception;

	public void execute();

}