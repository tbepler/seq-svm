package bepler.seq.svm.test;

import bepler.seq.svm.Feature;
import bepler.seq.svm.FeatureBuilder;

public class TestFeatureBuilder extends junit.framework.TestCase {
	
	private static final FeatureBuilder b = new FeatureBuilder(2, 1, 3, FeatureBuilder.DNA);
	private static final Feature[] expectedFeatures = new Feature[]{
		new Feature("A", 0),
		new Feature("C", 0),
		new Feature("G", 0),
		new Feature("T", 0),
		new Feature("A", 1),
		new Feature("C", 1),
		new Feature("G", 1),
		new Feature("T", 1),
		new Feature("AA", 0),
		new Feature("CA", 0),
		new Feature("GA", 0),
		new Feature("TA", 0),
		new Feature("AC", 0),
		new Feature("CC", 0),
		new Feature("GC", 0),
		new Feature("TC", 0),
		new Feature("AG", 0),
		new Feature("CG", 0),
		new Feature("GG", 0),
		new Feature("TG", 0),
		new Feature("AT", 0),
		new Feature("CT", 0),
		new Feature("GT", 0),
		new Feature("TT", 0),
	};
	
	private static final String s = "TC";
	private static final int[] stringFeatures = new int[]{
		0,
		0,
		0,
		1,
		0,
		1,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		1,
		0,
		0,
		0,
		0,
		0,
		0,
		0,
		0
	};
	
	public void testFeatureBuilder(){
		
		Feature[] test = b.getFeatures();
		assertEquals(expectedFeatures.length, test.length);
		for( int i = 0 ; i < expectedFeatures.length ; ++i ){
			assertEquals(expectedFeatures[i], test[i]);
		}
		
	}
	
	public void testFeaturize(){
		int[] test = b.featurize(s);
		assertEquals(stringFeatures.length, test.length);
		for( int i = 0 ; i < stringFeatures.length ; ++i ){
			assertEquals(stringFeatures[i], test[i]);
		}
	}
	
}
