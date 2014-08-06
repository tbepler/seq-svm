package bepler.seq.svm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import arnaudsj.java.libsvm.svm;
import arnaudsj.java.libsvm.svm_model;
import arnaudsj.java.libsvm.svm_node;
import arnaudsj.java.libsvm.svm_parameter;
import arnaudsj.java.libsvm.svm_problem;

public class GridSearchParallel implements GridSearch{
	
	private final double[] ps;
	private final double[] cs;
	private final double term;
	private final List<CrossValidationSet> crossValSets;
	private final FeatureBuilder features;
	private final ExecutorService exec;
	
	public GridSearchParallel(double[] ps, double[] cs, double term,
			List<CrossValidationSet> crossValSets, FeatureBuilder features){
		this.ps = ps.clone();
		this.cs = cs.clone();
		this.term = term;
		this.crossValSets = new ArrayList<CrossValidationSet>(crossValSets);
		this.features = features;
		this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	private double crossValidate(List<CrossValidationSet> sets, final svm_parameter param, File dir){
		final List<Double> scores = new ArrayList<Double>();
		final List<svm_model> models = new ArrayList<svm_model>();
		Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for(final CrossValidationSet set : sets){
			tasks.add(new Callable<Object>(){
				@Override public Object call() throws Exception {
					svm_problem prob = new svm_problem();
					prob.l = set.trainValues.length;
					prob.y = set.trainValues;
					prob.x = set.trainSet;
					svm_model model = svm.svm_train(prob, param);
					synchronized(scores){
						scores.add(testModel(model, set.testValues, set.testSet));
						models.add(model);
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
		
		if(scores.size() != sets.size()){
			//an error occurred in at least one of the model training threads
			System.err.println("Warning: an error occurred while cross validating parameters: Epsilon="+param.p+" C="+param.C
					+". Only "+scores.size()+ " out of "+sets.size()+" models completed.");
		}
		
		if(dir != null){
			if(!dir.exists()){
				dir.mkdirs();
			}
			//write the cross validation models to the directory
			for( int i = 0 ; i < scores.size() ; ++i ){
				svm_model model = models.get(i);
				double score = scores.get(i);
				String name = "model_eps"+model.param.p+"_C"+model.param.C+
						"_k"+i+"_score"+score+".txt";
				File target = new File(dir, name);
				try {
					PrintStream out = new PrintStream( new BufferedOutputStream (new FileOutputStream(target)));
					new SeqSVMModel(features, model).write(out);
				} catch (FileNotFoundException e) {
					System.err.println("Error: unable to write file "+target);
				}
			}
		}
		
		//sum the scores
		double total = 0;
		for(double score : scores){
			total += score;
		}
		
		return total / (double) scores.size();
	}
	
	private double testModel(svm_model model, double[] testValues, svm_node[][] testSet){
		double score = 0;
		for( int i = 0 ; i < testValues.length ; ++i ){
			//the error is the difference between the predicted value and the actual value
			double err = Math.abs(svm.svm_predict(model, testSet[i]) - testValues[i]);
			score += err;
		}
		return score / (double) testValues.length;
	}
	
	private svm_parameter initParam(double p, double c, double termEps){
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.EPSILON_SVR;
		param.kernel_type = svm_parameter.LINEAR;
		param.C = c;
		param.p = p;
		param.eps = termEps;
		return param;
	}
	
	private Map<svm_parameter, Double> computeScores(final File saveIntermediariesTo){
		//use a new cached thread pool for this, as the crossValidate method adds tasks
		//to the fixed size thread pool
		ExecutorService threadCache = Executors.newCachedThreadPool();
		final Map<svm_parameter, Double> scores = new ConcurrentHashMap<svm_parameter, Double>();
		List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for( final double e : ps ){
			for( final double c : cs	){
				tasks.add(new Callable<Object>(){

					@Override
					public Object call() throws Exception {
						svm_parameter param = initParam(e, c, term);
						double score = crossValidate(crossValSets, param, saveIntermediariesTo);
						scores.put(param, score);
						return null;
					}
					
				});
			}
		}
		try {
			threadCache.invokeAll(tasks);
		} catch (InterruptedException e1) {
			//bad things
			throw new Error(e1);
		}
		return scores;
	}

	@Override
	public svm_parameter search(File saveIntermediariesTo) {
		//build the grid search score table
		Map<svm_parameter, Double> scores = this.computeScores(saveIntermediariesTo);
		//picks the parameters that produce a model with the lowest
		//average error
		svm_parameter best = null;
		double bestScore = Double.POSITIVE_INFINITY;
		for(svm_parameter param : scores.keySet()){
			double score = scores.get(param);
			if(score < bestScore){
				bestScore = score;
				best = param;
			}
		}
		return best;
	}
	
}
