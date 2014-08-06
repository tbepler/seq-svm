package bepler.seq.svm;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private boolean verbose = false;
	
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
	
	public void setVerbose(boolean verbose){
		this.verbose = verbose;
	}
	
	private static double[] asArray(List<Double> vals){
		double[] array = new double[vals.size()];
		for( int i = 0 ; i < array.length ; ++i ){
			array[i] = vals.get(i);
		}
		return array;
	}
	
	private static class FeaturesValue{
		public svm_node[] features;
		public double val;
		public FeaturesValue(FeatureBuilder builder, String seq, double val){
			this.features = extractFeatures(builder, seq); this.val = val;
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

	public SeqSVMModel train(final List<String> seqs, final List<Double> vals, int k, Random random,
			double terminationEpsilon, File saveIntermediariesTo){
		if(seqs.size() != vals.size()){
			throw new RuntimeException("Sequences list and values list must be of the same size.");
		}
		//redirect stdout to stderr
		PrintStream sout = System.out;
		System.setOut(System.err);

		try{
			ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			if(verbose){
				System.err.println("Running on "+Runtime.getRuntime().availableProcessors()+" cores.");
				System.err.println("Extracting sequence features.");
			}
			final List<FeaturesValue> shuffle = new ArrayList<FeaturesValue>();
			List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
			for( int i = 0 ; i < seqs.size() ; ++i ){
				final int index = i;
				tasks.add(new Callable<Object>(){

					@Override
					public Object call() throws Exception {
						FeaturesValue feature = new FeaturesValue(builder, seqs.get(index), vals.get(index));
						synchronized(shuffle){
							shuffle.add(feature);
						}
						return null;
					}

				});
			}
			try {
				exec.invokeAll(tasks);
			} catch (InterruptedException e) {
				//bad things
				throw new Error(e);
			}
			Collections.shuffle(shuffle, random);

			//build the cross-validation sets
			if(verbose){
				System.err.println("Building cross validation sets.");
			}
			List<CrossValidationSet> crossValSets = this.buildCrossValidationSets(k,shuffle);

			//grid search the parameters in parallel
			if(verbose){
				System.err.println("Grid searching parameters.");
			}
			GridSearch search = new GridSearchParallel(eps, cs, terminationEpsilon, crossValSets, builder);
			svm_parameter param = search.search(saveIntermediariesTo);

			//build a model using the best parameters and all the given data
			if(verbose){
				System.err.println("Generating model with parameters: ");
				System.err.println("Epsilon = "+param.p);
				System.err.println("C = "+param.C);
			}
			svm_problem prob = new svm_problem();
			prob.l = seqs.size();
			prob.y = asArray(vals);
			prob.x = generateFeatures(builder, seqs);
			svm_model model = svm.svm_train(prob, param);

			return new SeqSVMModel(builder, model);
		}finally{
			//restore system.out
			System.setOut(sout);
		}
		
	}

	private List<CrossValidationSet> buildCrossValidationSets(
			final int k,
			final List<FeaturesValue> shuffle
			) {
		
		final int blockSize = shuffle.size() / k;
		final List<CrossValidationSet> crossValSets = new ArrayList<CrossValidationSet>();
		for( int i = 0 ; i < k ; ++i ){
			int start = blockSize * i;
			int end = i == k-1 ? shuffle.size() : start + blockSize;

			List<svm_node[]> testSet = new ArrayList<svm_node[]>();
			List<Double> testValues = new ArrayList<Double>();
			List<svm_node[]> trainingSet = new ArrayList<svm_node[]>();
			List<Double> trainingValues = new ArrayList<Double>();
			
			for( int j = 0 ; j < shuffle.size() ; ++j ){
				FeaturesValue sv = shuffle.get(j);
				if( j >= start && j < end ){
					testSet.add(sv.features);
					testValues.add(sv.val);
				}else{
					trainingSet.add(sv.features);
					trainingValues.add(sv.val);
				}
			}
			
			CrossValidationSet set = new CrossValidationSet(
					trainingSet.toArray(new svm_node[trainingSet.size()][]),
					asArray(trainingValues),
					testSet.toArray(new svm_node[testSet.size()][]),
					asArray(testValues)
					);
			
			crossValSets.add(set);
		}
		return crossValSets;
	}
	
	
	
}
