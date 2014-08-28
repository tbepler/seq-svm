package bepler.seq.svm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class LinearModel {
	
	public abstract double intercept();

	public abstract double predict(String seq);

	public abstract Feature[] getFeatures();

	public abstract double[] getWeights();

	public void write(PrintStream out){
		double[] featureWeights = this.getWeights();
		Feature[] features = this.getFeatures();
		out.println("intercept : "+intercept());
		for( int i = 0 ; i < featureWeights.length ; ++i ){
			out.println(features[i]+" : "+featureWeights[i]);
		}
	}
	
	public static final String FLOAT_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
	public static final String LINE_REGEX = Feature.FEATURE_REGEX + "\\s*:\\s*" + FLOAT_REGEX +"\\s*";
	public static final String INTERCEPT_REGEX = "\\s*intercept\\s*:\\s*" + FLOAT_REGEX + "\\s*"; 
	
	public static LinearModel readModel(InputStream in){
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		double intercept = 0;
		List<Feature> features = new ArrayList<Feature>();
		List<Double> weights = new ArrayList<Double>();
		String line;
		try {
			while( (line = reader.readLine()) != null ){
				if(line.matches(LINE_REGEX)){
					String[] split = line.trim().split(":");
					features.add(Feature.parseFeature(split[0].trim()));
					weights.add(Double.parseDouble(split[1].trim()));
				}else if (line.matches(INTERCEPT_REGEX)){
					intercept = Double.parseDouble(line.trim().split(":")[1].trim());
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				//bad things
			}
		}
		double[] ws = new double[weights.size()];
		for( int i = 0 ; i < ws.length ; ++i ){
			ws[i] = weights.get(i);
		}
		return new LinearModelImpl(intercept, features.toArray(new Feature[features.size()]), ws);
	}
	
	private static class LinearModelImpl extends LinearModel{
		
		private final double intercept;
		private final Feature[] features;
		private final int[] sizes;
		private final double[] weights;
		private final Map<Feature, Double> map;
		
		public LinearModelImpl(double intercept, Feature[] features, double[] weights){
			this.intercept = intercept;
			this.features = features;
			Set<Integer> sizeSet = new HashSet<Integer>();
			for(Feature f : features){
				sizeSet.add(f.getKmer().length());
			}
			sizes = new int[sizeSet.size()];
			int i = 0;
			for( int size : sizeSet ){
				sizes[i++] = size;
			}
			this.weights = weights;
			map = new HashMap<Feature, Double>();
			for( i = 0 ; i < features.length ; ++i ){
				map.put(features[i], weights[i]);
			}
		}

		@Override
		public double predict(String seq) {
			double val = intercept;
			for(Feature f : Features.featurize(seq, sizes)){
				val += map.get(f);
			}
			return val;
		}

		@Override
		public Feature[] getFeatures() {
			return features.clone();
		}

		@Override
		public double[] getWeights() {
			return weights.clone();
		}

		@Override
		public double intercept() {
			return intercept;
		}
		
	}

}