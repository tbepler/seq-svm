package bepler.seq.svm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
	private final ExecutorService exec;
	private boolean searched = false;
	
	public GridSearchParallel(double[] ps, double[] cs, double term, List<CrossValidationSet> crossValSets){
		this.ps = ps.clone();
		this.cs = cs.clone();
		this.term = term;
		this.crossValSets = new ArrayList<CrossValidationSet>(crossValSets);
		this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	private double crossValidate(List<CrossValidationSet> sets, svm_parameter param){
		double score = 0;
		for(CrossValidationSet set : sets){
			svm_problem prob = new svm_problem();
			prob.l = set.trainValues.length;
			prob.y = set.trainValues;
			prob.x = set.trainSet;
			svm_model model = svm.svm_train(prob, param);
			score += this.testModel(model, set.testValues, set.testSet);
		}
		return score / (double) sets.size();
	}
	
	private double testModel(svm_model model, double[] testValues, svm_node[][] testSet){
		double score = 0;
		for( int i = 0 ; i < testValues.length ; ++i ){
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
	
	private Map<svm_parameter, Double> computeScores(){
		final Map<svm_parameter, Double> scores = new ConcurrentHashMap<svm_parameter, Double>();
		for( final double e : ps ){
			for( final double c : cs	){
				exec.execute(new Runnable(){

					@Override
					public void run() {
						svm_parameter param = initParam(e, c, term);
						double score = crossValidate(crossValSets, param);
						scores.put(param, score);
					}
					
				});
			}
		}
		exec.shutdown();
		try {
			exec.awaitTermination(0, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return scores;
	}

	@Override
	public svm_parameter search() {
		if(searched){
			throw new RuntimeException("This GridSearch has already been searched. A new one must be built for each future search.");
		}
		searched = true;
		Map<svm_parameter, Double> scores = this.computeScores();
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
