package bepler.seq.svm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Parse {

	public static final double DEFAULT_TERMINATION_EPSILON = 0.01;
	public static final int DEFAULT_CROSS_VALIDATION = 5;
	
	public static char[] asCharArray(Collection<Character> col){
		char[] array = new char[col.size()];
		int index = 0;
		for(char c : col){
			array[index++] = c;
		}
		return array;
	}
	
	public static double[] asDoubleArray(Collection<Double> col){
		double[] array = new double[col.size()];
		int index = 0;
		for(double d : col){
			array[index++] = d;
		}
		return array;
	}
	
	public static int[] asIntArray(Collection<Integer> col){
		int[] array = new int[col.size()];
		int index = 0;
		for(int i : col){
			array[index++] = i;
		}
		return array;
	}
	
	public static String usage(){
		return "Usage: seqsvm Seqs_File Features_File Epsilon_File C_File [Cross_Validation_K] [Terminator_Epsilon] [Intermediaries_Dir]";
	}
	
	private final List<String> seqs = new ArrayList<String>();
	private final List<Double> vals = new ArrayList<Double>();
	private final char[] alphabet;
	private final int[] kmers;
	private final double[] ps;
	private final double[] cs;
	private int seqLen;
	private int k = DEFAULT_CROSS_VALIDATION;
	private double term = DEFAULT_TERMINATION_EPSILON;
	private File intermediariesDir = null;
	
	public Parse(String[] args){
		if(args.length != 4 && args.length != 5 && args.length != 6){
			System.err.println(usage());
			throw new Error();
		}
		try{
			alphabet = parseSequences(args[0]);
			kmers = parseInts(args[1]);
			ps = parseDoubles(args[2]);
			cs = parseDoubles(args[3]);
			if(args.length == 7){
				k = Integer.parseInt(args[4]);
				term = Double.parseDouble(args[5]);
				intermediariesDir = new File(args[6]);
			}else if(args.length == 6){
				try{
					k = Integer.parseInt(args[4]);
					term = Double.parseDouble(args[5]);
				}catch(Exception e){
					try{
						term = Double.parseDouble(args[4]);
						intermediariesDir = new File(args[5]);
					} catch (Exception e2){
						k = Integer.parseInt(args[4]);
						intermediariesDir = new File(args[5]);
					}
				}
			}else if(args.length == 5){
				try{
					k = Integer.parseInt(args[4]);
				}catch(Exception e){
					try{
						term = Double.parseDouble(args[4]);
					} catch (Exception e2){
						intermediariesDir = new File(args[4]);
					}
				}
			}
		} catch(Exception e){
			System.err.println(usage());
			throw new Error(e);
		}
	}
	
	private String arrayToString(double[] array){
		String s = "";
		for(double d : array){
			s += d + " ";
		}
		return s;
	}
	
	private String arrayToString(int[] array){
		String s = "";
		for(int i : array){
			s += i + " ";
		}
		return s;
	}
	
	public void execute(){
		System.err.println("Building model for kmers: "+arrayToString(kmers));
		System.err.println("Using epsilons: "+arrayToString(ps));
		System.err.println("Using Cs: "+arrayToString(cs));
		System.err.println("Using K: "+k);
		System.err.println("Using terminal epsilon: "+term);
		System.err.println("Sequence length: "+seqLen);
		System.err.println("Total sequences: "+seqs.size());
		SeqSVMTrainer trainer = new SeqSVMTrainer(seqLen, kmers, alphabet, ps, cs);
		trainer.setVerbose(true);
		SeqSVMModel model = trainer.train(seqs, vals, k, new Random(), term, intermediariesDir);
		model.write(System.out);
	}
	
	private int[] parseInts(String file) throws FileNotFoundException{
		File f = new File(file);
		Scanner s = new Scanner(f);
		List<Integer> ints = new ArrayList<Integer>();
		while(s.hasNextInt()){
			ints.add(s.nextInt());
		}
		s.close();
		return asIntArray(ints);
	}
	
	private double[] parseDoubles(String file) throws FileNotFoundException{
		File f = new File(file);
		Scanner s = new Scanner(f);
		List<Double> vals = new ArrayList<Double>();
		while(s.hasNextDouble()){
			vals.add(s.nextDouble());
		}
		s.close();
		return asDoubleArray(vals);
	}
	
	private char[] parseSequences(String file) throws IOException{
		File f = new File(file);
		Set<Character> alphabet = new HashSet<Character>();
		BufferedReader reader = new BufferedReader(new FileReader(f));
		seqLen = -1;
		String line;
		try {
			while((line = reader.readLine()) != null){
				String[] tokens = line.split("\\s+");
				String seq = tokens[0];
				for(char c : seq.toCharArray()){
					alphabet.add(c);
				}
				if(seqLen == -1){
					seqLen = seq.length();
				}else if(seqLen != seq.length()){
					throw new Error("Sequences must all be of same length.");
				}
				seqs.add(seq);
				vals.add(Double.parseDouble(tokens[1]));
			}
		} catch (NumberFormatException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}finally{
			try {
				reader.close();
			} catch (IOException e) {
				throw e;
			}
		}
		return asCharArray(alphabet);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
