package bepler.seq.svm;

import java.util.Arrays;

public class Feature {
	
	private final String kmer;
	private final int index;
	
	public Feature(String kmer, int index){
		this.kmer = kmer;
		this.index = index;
	}
	
	public String getKmer(){
		return kmer;
	}
	
	public int getIndex(){
		return index;
	}
	
	public boolean containedBy(String s){
		return s.regionMatches(index, kmer, 0, kmer.length());
	}
	
	@Override
	public String toString(){
		return "["+index+"]"+kmer;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(new Object[]{kmer, index});
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null) return false;
		if(o == this) return true;
		if(o instanceof Feature){
			Feature that = (Feature) o;
			return equals(this.kmer, that.kmer) && this.index == that.index;
		}
		return false;
	}
	
	private static boolean equals(Object o1, Object o2){
		if(o1 == o2) return true;
		if(o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}
	
}
