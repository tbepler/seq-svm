package bepler.seq.svm;

import arnaudsj.java.libsvm.svm_node;

public class CrossValidationSet{
	
	public final svm_node[][] trainSet;
	public final double[] trainValues;
	public final svm_node[][] testSet;
	public final double[] testValues;
	
	public CrossValidationSet(svm_node[][] trainSet, double[] trainValues, svm_node[][] testSet, double[] testValues){
		this.trainSet = trainSet; this.trainValues = trainValues; this.testSet = testSet; this.testValues = testValues;
	}

	
}
