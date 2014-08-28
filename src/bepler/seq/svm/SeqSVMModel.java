package bepler.seq.svm;

import java.util.Arrays;

import arnaudsj.java.libsvm.svm;
import arnaudsj.java.libsvm.svm_model;
import arnaudsj.java.libsvm.svm_node;

public class SeqSVMModel extends LinearModel {
	
	private final FeatureBuilder builder;
	private final svm_model model;
	
	public SeqSVMModel(FeatureBuilder builder, svm_model model){
		this.builder = builder;
		this.model = model;
	}
	
	@Override
	public double predict(String seq){
		svm_node[] nodes = SeqSVMTrainer.extractFeatures(builder, seq);
		return svm.svm_predict(model, nodes);
	}
	
	@Override
	public Feature[] getFeatures(){
		return builder.getFeatures();
	}
	
	@Override
	public double[] getWeights(){
		double[] svWeights = model.sv_coef[0];
		double[] featureWeights = new double[getFeatures().length];
		Arrays.fill(featureWeights, 0);
		for( int i = 0 ; i < model.SV.length ; ++i ){
			svm_node[] sv = model.SV[i];
			double svW = svWeights[i];
			for( int j = 0 ; j < sv.length ; ++j ){
				double weight = sv[j].value;
				featureWeights[j] += svW * weight;
			}
		}
		return featureWeights;
	}

	@Override
	public double intercept() {
		return -model.rho[0];
	}

}
