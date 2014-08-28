package bepler.util;

public class StatsUtil {
	
	public static double meanSquaredError(double[] x, double[] y){
		double mse = 0;
		for( int i = 0 ; i < x.length ; ++i ){
			mse += Math.pow(x[i] - y[i], 2);
		}
		return mse / (double) x.length;
	}
	
}
