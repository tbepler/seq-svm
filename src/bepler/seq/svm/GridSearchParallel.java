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

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import bepler.util.StatsUtil;
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
			List<CrossValidationSet> crossValSets, FeatureBuilder features,
			int threads){
		this.ps = ps.clone();
		this.cs = cs.clone();
		this.term = term;
		this.crossValSets = new ArrayList<CrossValidationSet>(crossValSets);
		this.features = features;
		int n = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
		this.exec = Executors.newFixedThreadPool(n);
	}
	
	private Result crossValidate(List<CrossValidationSet> sets, final svm_parameter param, final File dir){
		final List<Result> scores = new ArrayList<Result>();
		Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		int i = 0;
		for(final CrossValidationSet set : sets){
			final int k = i++;
			tasks.add(new Callable<Object>(){
				@Override public Object call() throws Exception {
					svm_problem prob = new svm_problem();
					prob.l = set.trainValues.length;
					prob.y = set.trainValues;
					prob.x = set.trainSet;
					svm_model model = svm.svm_train(prob, param);
					Result r = testModel(model, set.testValues, set.testSet);
					synchronized(scores){
						scores.add(r);
					}
					writeModel(dir, r, model, k);
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
		
		
		//sum the scores
		Result mean = new Result();
		mean.mse = 0;
		mean.r2 = 0;
		for(Result score : scores){
			mean.mse += score.mse;
			mean.r2 += score.r2;
		}
		
		mean.mse = mean.mse / (double) scores.size();
		mean.r2 = mean.r2 / (double) scores.size();
		
		return mean;
	}

	private void writeModel(File dir, Result r,
			svm_model model, int k) {
		if(dir != null){
			if(!dir.exists()){
				dir.mkdirs();
			}
			//write the cross validation model to the directory
			String name = "model_eps"+model.param.p+"_C"+model.param.C+
					"_k"+k+"_mse-"+r.mse+"_r2-"+r.r2+".txt";
			File target = new File(dir, name);
			PrintStream out = null;
			try {
				out = new PrintStream( new BufferedOutputStream (new FileOutputStream(target)));
				new SeqSVMModel(features, model).write(out);
			} catch (FileNotFoundException e) {
				synchronized(System.err){
					System.err.println("Error: unable to write file "+target);
				}
			} finally {
				if(out != null){
					out.close();
				}
			}
		}
	}
	
	private Result testModel(svm_model model, double[] testValues, svm_node[][] testSet){
		double[] predictValues = new double[testValues.length];
		for( int i = 0 ; i < testValues.length ; ++i ){
			predictValues[i] = svm.svm_predict(model, testSet[i]) ;
		}
		Result r = new Result();
		PearsonsCorrelation cor = new PearsonsCorrelation();
		r.r2 = Math.pow(cor.correlation(testValues, predictValues), 2);
		r.mse = StatsUtil.meanSquaredError(testValues, predictValues);
		return r;
	}
	

	
	private static class Result implements Comparable<Result>{
		private double r2;
		private double mse;
		@Override
		public int compareTo(Result arg0) {
			double dif = mse - arg0.mse;
			if(dif < 0) return -1;
			if(dif > 0) return 1;
			return 0;
		}
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
	
	private Map<svm_parameter, Result> computeScores(final File saveIntermediariesTo){
		//use a new cached thread pool for this, as the crossValidate method adds tasks
		//to the fixed size thread pool
		ExecutorService threadCache = Executors.newCachedThreadPool();
		final Map<svm_parameter, Result> scores = new ConcurrentHashMap<svm_parameter, Result>();
		List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for( final double e : ps ){
			for( final double c : cs	){
				tasks.add(new Callable<Object>(){

					@Override
					public Object call() throws Exception {
						svm_parameter param = initParam(e, c, term);
						Result score = crossValidate(crossValSets, param, saveIntermediariesTo);
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
		Map<svm_parameter, Result> scores = this.computeScores(saveIntermediariesTo);
		//picks the parameters that produce a model with the lowest
		//average error
		svm_parameter best = null;
		Result bestScore = new Result();
		bestScore.mse = Double.POSITIVE_INFINITY;
		bestScore.r2 = 0;
		for(svm_parameter param : scores.keySet()){
			Result score = scores.get(param);
			if(score.compareTo(bestScore) < 0){
				bestScore = score;
				best = param;
			}
		}
		return best;
	}
	
}
