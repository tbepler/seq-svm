package bepler.seq.svm;

import java.io.File;

import arnaudsj.java.libsvm.svm_parameter;

public interface GridSearch {
	
	public svm_parameter search(File saveIntermediariesTo);
	
}
