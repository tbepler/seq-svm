package bepler.seq.svm;

import java.io.PrintStream;

import arnaudsj.java.libsvm.svm;
import arnaudsj.java.libsvm.svm_model;
import arnaudsj.java.libsvm.svm_node;

public class SeqSVMModel {
	
	private final FeatureBuilder builder;
	private final svm_model model;
	
	public SeqSVMModel(FeatureBuilder builder, svm_model model){
		this.builder = builder;
		this.model = model;
	}
	
	public double predict(String seq){
		svm_node[] nodes = SeqSVMTrainer.extractFeatures(builder, seq);
		return svm.svm_predict(model, nodes);
	}
	
	public Feature[] getFeatures(){
		return builder.getFeatures();
	}
	
	public double[] getWeights(){
		return model.sv_coef[0];
	}
	
	public void write(PrintStream out){
		double[] weights = this.getWeights();
		Feature[] features = this.getFeatures();
		for( int i = 0 ; i < weights.length ; ++i ){
			out.println(features[i]+" : "+weights[i]);
		}
	}

}
