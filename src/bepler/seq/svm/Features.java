package bepler.seq.svm;

import java.util.ArrayList;
import java.util.List;

public class Features {
	
	public static boolean[] featurize(String s, Feature[] features){
		boolean[] array = new boolean[features.length];
		for( int i = 0 ; i < features.length ; ++i ){
			array[i] = features[i].containedBy(s);
		}
		return array;
	}
	
	public static List<Feature> featurize(String s, int[] sizes){
		List<Feature> list = new ArrayList<Feature>();
		for( int size : sizes ){
			for( int i = 0 ; i < s.length() - size + 1 ; ++i ){
				String substr = s.substring(i, i + size);
				list.add(new Feature(substr, i));
			}
		}
		return list;
	}
	
}
