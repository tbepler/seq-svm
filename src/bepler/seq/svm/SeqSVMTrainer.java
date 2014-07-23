package bepler.seq.svm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import arnaudsj.java.libsvm.svm;
import arnaudsj.java.libsvm.svm_model;
import arnaudsj.java.libsvm.svm_node;
import arnaudsj.java.libsvm.svm_parameter;
import arnaudsj.java.libsvm.svm_problem;

public class SeqSVMTrainer {
	
	public static final int DNA = FeatureBuilder.DNA;
	
	private final FeatureBuilder builder;
	private final double[] eps;
	private final double[] cs;
	
	public SeqSVMTrainer(int seqLen, int[] kmerLens, char[] alphabet, double[] eps, double[] c){
		builder = new FeatureBuilder(seqLen, kmerLens, alphabet);
		this.eps = eps.clone();
		this.cs = c.clone();
	}
	
	public SeqSVMTrainer(int seqLen, int minKmer, int maxKmer, char[] alphabet, double[] eps, double[] c){
		builder = new FeatureBuilder(seqLen, minKmer, maxKmer, alphabet);
		this.eps = eps.clone();
		this.cs = c.clone();
	}
	
	private static double[] asArray(List<Double> vals){
		double[] array = new double[vals.size()];
		for( int i = 0 ; i < array.length ; ++i ){
			array[i] = vals.get(i);
		}
		return array;
	}
	
	private static class StringValue{
		public String seq;
		public double val;
		public StringValue(String seq, double val){
			this.seq = seq; this.val = val;
		}
	}
	
	public static svm_node[][] generateFeatures(FeatureBuilder builder, List<String> seqs){
		svm_node[][] nodes = new svm_node[seqs.size()][];
		for( int i = 0 ; i < seqs.size() ; ++i ){
			nodes[i] = extractFeatures(builder, seqs.get(i));
		}
		return nodes;
	}
	
	public static svm_node[] extractFeatures(FeatureBuilder builder, String seq){
		double[] features = builder.featurizeAsDouble(seq);
		svm_node[] nodes = new svm_node[features.length];
		for( int j = 0 ; j < features.length ; ++j ){
			svm_node node = new svm_node();
			node.index = j;
			node.value = features[j];
			nodes[j] = node;
		}
		return nodes;
	}
	
	public SeqSVMModel train(List<String> seqs, List<Double> vals, int k, Random random, double terminationEpsilon){
		if(seqs.size() != vals.size()){
			throw new RuntimeException("Sequences list and values list must be of the same size.");
		}
		
		List<StringValue> shuffle = new ArrayList<StringValue>();
		for( int i = 0 ; i < seqs.size() ; ++i ){
			shuffle.add(new StringValue(seqs.get(i), vals.get(i)));
		}
		Collections.shuffle(shuffle, random);
		
		//build the cross-validation sets
		List<CrossValidationSet> crossValSets = this.buildCrossValidationSets(k,shuffle);
		
		//grid search the parameters in parallel
		GridSearch search = new GridSearchParallel(eps, cs, terminationEpsilon, crossValSets);
		svm_parameter param = search.search();
		
		//build a model using the best parameters and all the given data
		svm_problem prob = new svm_problem();
		prob.l = seqs.size();
		prob.y = asArray(vals);
		prob.x = generateFeatures(builder, seqs);
		svm_model model = svm.svm_train(prob, param);
		
		return new SeqSVMModel(builder, model);
		
	}

	private List<CrossValidationSet> buildCrossValidationSets(
			int k,
			List<StringValue> shuffle
			) {
		
		int blockSize = shuffle.size() / k;
		List<CrossValidationSet> crossValSets = new ArrayList<CrossValidationSet>();
		for( int i = 0 ; i < k ; ++i ){
			int start = blockSize * i;
			int end = i == k-1 ? shuffle.size() : start + blockSize;

			List<String> testSet = new ArrayList<String>();
			List<Double> testValues = new ArrayList<Double>();
			List<String> trainingSet = new ArrayList<String>();
			List<Double> trainingValues = new ArrayList<Double>();
			
			for( int j = 0 ; j < shuffle.size() ; ++j ){
				StringValue sv = shuffle.get(j);
				if( j >= start && j < end ){
					trainingSet.add(sv.seq);
					trainingValues.add(sv.val);
				}else{
					testSet.add(sv.seq);
					testValues.add(sv.val);
				}
			}
			
			CrossValidationSet set = new CrossValidationSet(
					generateFeatures(builder, trainingSet),
					asArray(trainingValues),
					generateFeatures(builder, testSet),
					asArray(testValues)
					);
			
			crossValSets.add(set);
		}
		return crossValSets;
	}
	
	
	
}
