package bepler.seq.svm;

import java.util.ArrayList;
import java.util.List;

public class FeatureBuilder {
	
	public static final int DNA = 0;
	private static final char[] DNA_ALPH = new char[]{'A','C','G','T'};
	
	private static char[] lookup(int alphabetCode){
		switch(alphabetCode){
		case 0: return DNA_ALPH;
		default: throw new RuntimeException("Unrecoginzed alphabet code: "+alphabetCode);
		}
	}
	
	private static Feature[] computeFeatures(int seqLen, int[] kmerSizes, char[] alphabet){
		List<Feature> features = new ArrayList<Feature>();
		for(int size : kmerSizes){
			String[] kmers = computeKmers(size, alphabet);
			for( int i = 0 ; i < seqLen - size + 1 ; ++i ){
				for(String kmer : kmers){
					features.add(new Feature(kmer, i));
				}
			}
		}
		return features.toArray(new Feature[features.size()]);
	}
	
	private static String[] computeKmers(int size, char[] alphabet){
		char[][] kmers = new char[pow(alphabet.length, size)][size];
		for( int i = 0 ; i < size ; ++i ){
			int chunkSize = pow(alphabet.length, i);
			int chunks = kmers.length / chunkSize;
			for( int chunk = 0 ; chunk < chunks ; ++chunk ){
				char c = alphabet[chunk % alphabet.length];
				int start = chunk * chunkSize;
				int end = start + chunkSize;
				for( int j = start ; j < end ; ++j ){
					kmers[j][i] = c;
				}
			}
		}
		String[] s = new String[kmers.length];
		for( int i = 0 ; i < s.length ; ++i ){
			s[i] = new String(kmers[i]);
		}
		return s;
	}
	
	private static int pow(int base, int exp){
		int result = 1;
		while(exp != 0){
			if((exp & 1) != 0){
				result *= base;
			}
			exp >>= 1;
			base *= base;
		}
		return result;
	}
	
	private final int[] kmerSizes;
	private final char[] alphabet;
	private final int seqLen;
	private final Feature[] features;
	
	/**
	 * Creates a feature builder for generating features from sequences of the specified length using kmers of the
	 * sizes included in the given array and characters from the given alphabet
	 * @param seqLen
	 * @param kmers
	 * @param alphabet
	 */
	public FeatureBuilder(int seqLen, int[] kmers, char[] alphabet){
		this.seqLen = seqLen;
		this.kmerSizes = kmers.clone();
		this.alphabet = alphabet.clone();
		this.features = computeFeatures(this.seqLen, this.kmerSizes, this.alphabet);
	}
	
	/**
	 * Creates a feature builder for generating features from sequences of the specified length using kmers of the
	 * sizes included in the given array and characters from the built in alphabet specified by the alphabet code
	 * @param seqLen
	 * @param kmers
	 * @param alphabetCode
	 */
	public FeatureBuilder(int seqLen, int[] kmers, int alphabetCode){
		this(seqLen, kmers, lookup(alphabetCode));
	}
	
	/**
	 * Creates a feature builder for generating features from sequences of the specified length using kmers of sizes
	 * [min-max) and characters from the given alphabet
	 * @param seqLen
	 * @param min
	 * @param max
	 * @param alphabet
	 */
	public FeatureBuilder(int seqLen, int min, int max, char[] alphabet){
		this.seqLen = seqLen;
		int len = max-min;
		kmerSizes = new int[len];
		for( int i = 0 ; i < len ; ++i ){
			kmerSizes[i] = min + i;
		}
		this.alphabet = alphabet;
		this.features = computeFeatures(this.seqLen, kmerSizes, this.alphabet);
	}
	
	/**
	 * Creates a feature builder for generating features from sequences of the specified length using kmers of sizes
	 * [min-max) and characters from the given alphabet
	 * @param seqLen
	 * @param min
	 * @param max
	 * @param alphabet
	 */
	public FeatureBuilder(int seqLen, int min, int max, int alphabetCode){
		this(seqLen, min, max, lookup(alphabetCode));
	}
	
	public Feature[] getFeatures(){
		return features.clone();
	}
	
	public int[] featurize(String s){
		if(s == null){
			throw new NullPointerException();
		}
		if(s.length() != seqLen){
			throw new RuntimeException("FeatureBuilder: "+this+" requires strings of length "+seqLen+" but was "+s.length());
		}
		int[] binary = new int[features.length];
		for( int i = 0 ; i < features.length ; ++i ){
			if(features[i].containedBy(s)){
				binary[i] = 1;
			}else{
				binary[1] = 0;
			}
		}
		return binary;
	}
	
	public double[] featurizeAsDouble(String s){
		if(s == null){
			throw new NullPointerException();
		}
		if(s.length() != seqLen){
			throw new RuntimeException("FeatureBuilder: "+this+" requires strings of length "+seqLen+" but was "+s.length());
		}
		double[] binary = new double[features.length];
		for( int i = 0 ; i < features.length ; ++i ){
			if(features[i].containedBy(s)){
				binary[i] = 1;
			}else{
				binary[1] = 0;
			}
		}
		return binary;
	}
	
	

}
