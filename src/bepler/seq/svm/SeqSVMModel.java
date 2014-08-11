package bepler.seq.svm;

import java.io.PrintStream;
import java.util.Arrays;

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
		double[] svWeights = this.getWeights();
		Feature[] features = this.getFeatures();
		out.println("#SV = "+svWeights.length);
		double[] featureWeights = new double[features.length];
		Arrays.fill(featureWeights, 0);
		for( int i = 0 ; i < model.SV.length ; ++i ){
			svm_node[] sv = model.SV[i];
			double svW = svWeights[i];
			for( int j = 0 ; j < sv.length ; ++j ){
				double weight = sv[j].value;
				featureWeights[j] += svW * weight;
			}
		}
		for( int i = 0 ; i < featureWeights.length ; ++i ){
			out.println(features[i]+" : "+featureWeights[i]);
		}
	}

}
