package bepler.util;

import java.util.Collection;

public class ArrayUtil {
	
	public static double[] toArray(Collection<Double> col){
		double[] array = new double[col.size()];
		int i = 0;
		for( double d : col ){
			array[i++] = d;
		}
		return array;
	}
	
}
