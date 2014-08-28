package bepler.seq.svm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import bepler.util.ArrayUtil;
import bepler.util.StatsUtil;

public class Test implements Module{
	
	public static final String MODEL_FLAG = "-m";
	public static final String DATA_FLAG = "-t";
	
	private LinearModel model = null;
	private List<String> seqs = null;
	private List<Double> vals = null;

	@Override
	public String usage() {
		return "Usage: test "
				+MODEL_FLAG + " MODEL "
				+DATA_FLAG + " TEST_DATA "
				;
	}

	@Override
	public void setArgs(String[] args) throws Exception {
		model = null;
		seqs = null;
		vals = null;
		for( int i = 0 ; i < args.length ; ++i ){
			String arg = args[i];
			switch(arg){
			case MODEL_FLAG:
				InputStream in = new FileInputStream(new File(args[++i]));
				model = LinearModel.readModel(in);
				in.close();
				break;
			case DATA_FLAG:
				parseSeqs(new File(args[++i]));
				break;
			default:
				throw new Exception("Unrecognized flag: "+arg);
			}
		}
	}
	
	private void parseSeqs(File f) throws Exception{
		seqs = new ArrayList<String>();
		vals = new ArrayList<Double>();
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		while((line = reader.readLine()) != null){
			String[] tokens = line.split("\\s+");
			String seq = tokens[0];
			seqs.add(seq);
			vals.add(Double.parseDouble(tokens[1]));
		}
		reader.close();
	}

	@Override
	public void execute() {
		List<Double> predictions = new ArrayList<Double>();
		for( String seq : seqs ){
			predictions.add(model.predict(seq));
		}
		double[] testValues = ArrayUtil.toArray(vals);
		double[] predictValues = ArrayUtil.toArray(predictions);
		PearsonsCorrelation cor = new PearsonsCorrelation();
		double r2 = Math.pow(cor.correlation(testValues, predictValues), 2);
		double mse = StatsUtil.meanSquaredError(testValues, predictValues);
		System.out.println("r2 = "+r2);
		System.out.println("mse = "+mse);
		System.out.println("Sequence\tActual\tPredicted");
		for( int i = 0 ; i < seqs.size() ; ++i ){
			System.out.println(seqs.get(i) + "\t" + vals.get(i) + "\t" + predictions.get(i));
		}
	}

}
