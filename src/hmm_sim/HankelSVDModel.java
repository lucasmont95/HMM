package hmm_sim;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class HankelSVDModel extends HankelSVDModelParent { //implements Serializable
	
	private double[] probabilities;
	private int basisSize;
	
	private SingularValueDecomposition svd;

	
	public double[] getProbabilities() {
		return probabilities;
	}

	public SingularValueDecomposition getSvd() {
		return svd;
	}
	
	public int getBasisSize() {
		return basisSize;
	}
	
	public HankelSVDModel(){
		
	}


	public HankelSVDModel(double[] probabilities, int basisSize){
		this.probabilities = probabilities;
		this.basisSize = basisSize;
		takeSVD();
	}
	
	public int[] getCounts(){
		double min = -1;
		for (double d : probabilities) {
			if (d!=0 && (min == -1 || d < min)){
				min = d;
			}
		}
		
		int[] counts = new int[probabilities.length];
		for (int i = 0; i < counts.length; i++) {
			counts[i] =  (int) Math.round(probabilities[i]/min);
			/*if (probabilities[i] != 0){
				System.out.println(probabilities[i]/min);
				System.out.println(counts[i]);
			}*/
		}
		return counts;
	}
	
	
	public HankelSVDModel(double[] probabilities, int basisSize, SingularValueDecomposition s){
		this.probabilities = probabilities;
		this.basisSize = basisSize;
		this.svd = s;
	}

	public void takeSVD(){
		try{
			Matrix H = buildH(0, basisSize);
			SingularValueDecomposition svd = H.svd();
			this.svd = svd;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	

	public QueryEngine buildHankelBasedModel(int base, int modelSize){
		//Matrix H = SVD.getU().times(SVD.getU()).times(SVD.getV().transpose());
		Matrix H = this.getHankel();
		HashMap<String, Matrix> truncatedSVD = HankelSVDModelParent.truncateSVD(H ,modelSize);
		SingularValueDecomposition svd = new SingularValueDecomposition(H);
		
		//System.out.println("U difference");
		//truncatedSVD.getU().print(5, 5);
		//svd.getU().print(5, 5);
		
		//System.out.println("S difference HankelSVDModel");
		//System.out.println(Arrays.toString(getDiagonalArray(truncatedSVD.getS())));
		//System.out.println(Arrays.toString(getDiagonalArray(svd.getS())));
		
		Matrix di = pseudoInvDiagonalKL(truncatedSVD.get("S"));
		
		Matrix pinv = di.times(truncatedSVD.get("U").transpose());
		Matrix sinv = (truncatedSVD.get("VT")).transpose();
		
		/*System.out.println("Testing inverses");
		Matrix test1 = pinv.times(truncatedSVD.get("U").times(truncatedSVD.get("S")));
		Matrix test2 = truncatedSVD.get("VT").times(sinv);
		System.out.println(Arrays.toString( getDiagonalArray(test1) ));
		System.out.println(Arrays.toString( getDiagonalArray(test2) ));
		*/
		
		int maxExponent = (int) Math.floor((Math.log( (this.probabilities.length/2) - basisSize)/Math.log(base))) ; 

		Matrix[] H_Matrices = new Matrix[maxExponent+1];
				
		int freq;
		Matrix h;
		//System.out.println("Building queryEngine");
		for (int l = 0; l <= maxExponent; l++) {
			freq = (int) Math.pow(base,l);
			try {
				h = this.buildH(freq, freq+basisSize);
				
				//System.out.println("truncating all Hsigmas");
				//HashMap<String, Matrix> t = HankelSVDModelParent.truncateSVD(h, modelSize);	
				//H_Matrices[l] = t.get("U").times(t.get("S")).times(t.get("VT"));
				//No approx
				H_Matrices[l] = h;
			} catch (Exception e) {
				System.out.println("Problem Building Model when creating Hankel");
				e.printStackTrace();
				return null;
			}
		}
		
		Matrix Asigmas[] = new Matrix[maxExponent+1];
		Matrix t;

		for (int i = 0; i <= maxExponent; i++) {
			t = pinv.times(H_Matrices[i]).times( sinv );
			Asigmas[i] = pinv.times(H_Matrices[i]).times( sinv );
		}
		
		double[][] h_L = new double[basisSize][1];
		for (int i = 0; i < basisSize; i++) {
			h_L[i][0] = this.probabilities[i];
		}
		
		Matrix h_LS = new Matrix( h_L ).transpose();
		Matrix h_PL = h_LS.transpose();
				
		Matrix alpha_0 = h_LS.times(sinv);
		Matrix alpha_inf = pinv.times(h_PL);
	
		QueryEngine q = new QueryEngine(alpha_0, alpha_inf, Asigmas, maxExponent, base);
		
		//If Debugging Wanted
		//QueryEngine q = new QueryEngine(alpha_0, alpha_inf, Asigmas, maxExponent, base , pinv, sinv, truncatedSVD, this.svd);

		return q;
	}
	
	public int[] getOperators(int maxBaseSize, int numSubstrings){
		int[] counts = this.getCounts();
		HashMap<SequenceOfSymbols, Integer> m = new HashMap<SequenceOfSymbols, Integer>();
		
		for (int i = 1; i < counts.length; i++) {
			if (counts[i] != 0){
				SequenceOfSymbols s = new SequenceOfSymbols("1:" + i);
				m.put(s, counts[i]);
			}
		}
		
		/*System.out.println("Counts printing out!");
		System.out.println(Arrays.toString(counts));
		System.out.println();
		*/
		
		HashSet<SequenceOfSymbols> a = HeuristicsForPickingBase.chooseBaseFromData(m, maxBaseSize, numSubstrings, 1);
		int[] operators = new int[maxBaseSize+1];
		operators[0] = 1;
		
		int c=1;
		for (SequenceOfSymbols s: a) {
			int streak = s.getStreakFromString();
			operators[c] = streak;
			c++;
		}
		Arrays.sort(operators);
		
		operators = reverseOrder(operators);
		
		System.out.println("Operators");
		System.out.println( Arrays.toString(operators) );
		System.out.println();
		return operators;
	}
	
	public QueryEngine buildHankelBasedModelCustom(int base, int modelSize, int[] operators){
		//Matrix H = SVD.getU().times(SVD.getU()).times(SVD.getV().transpose());
		Matrix H = this.getHankel();
		HashMap<String, Matrix> truncatedSVD = HankelSVDModelParent.truncateSVD(H ,modelSize);
		SingularValueDecomposition svd = new SingularValueDecomposition(H);
		
		//System.out.println("U difference");
		//truncatedSVD.getU().print(5, 5);
		//svd.getU().print(5, 5);
		
		//System.out.println("S difference HankelSVDModel");
		//System.out.println(Arrays.toString(getDiagonalArray(truncatedSVD.getS())));
		//System.out.println(Arrays.toString(getDiagonalArray(svd.getS())));
		
		Matrix di = pseudoInvDiagonalKL(truncatedSVD.get("S"));
		
		Matrix pinv = di.times(truncatedSVD.get("U").transpose());
		Matrix sinv = (truncatedSVD.get("VT")).transpose();
		
		/*System.out.println("Testing inverses");
		Matrix test1 = pinv.times(truncatedSVD.get("U").times(truncatedSVD.get("S")));
		Matrix test2 = truncatedSVD.get("VT").times(sinv);
		System.out.println(Arrays.toString( getDiagonalArray(test1) ));
		System.out.println(Arrays.toString( getDiagonalArray(test2) ));
		*/
		
		//int maxExponent = (int) Math.floor((Math.log( (this.probabilities.length/2) - basisSize)/Math.log(base))) ; 

		
		
		
		Matrix[] H_Matrices = new Matrix[operators.length];
				
		int freq;
		Matrix h;
		//System.out.println("Building queryEngine");
		for (int l = 0; l < operators.length; l++) {
			freq = operators[l];
			try {
				h = this.buildH(freq, freq+basisSize);
				
				//System.out.println("truncating all Hsigmas");
				//HashMap<String, Matrix> t = HankelSVDModelParent.truncateSVD(h, modelSize);	
				//H_Matrices[l] = t.get("U").times(t.get("S")).times(t.get("VT"));
				//No approx
				H_Matrices[l] = h;
			} catch (Exception e) {
				System.out.println("Problem Building Model when creating Hankel");
				e.printStackTrace();
				return null;
			}
		}
		
		Matrix Asigmas[] = new Matrix[operators.length];
		Matrix t;

		for (int i = 0; i < operators.length; i++) {
			t = pinv.times(H_Matrices[i]).times( sinv );
			Asigmas[i] = pinv.times(H_Matrices[i]).times( sinv );
		}
		
		double[][] h_L = new double[basisSize][1];
		for (int i = 0; i < basisSize; i++) {
			h_L[i][0] = this.probabilities[i];
		}
		
		Matrix h_LS = new Matrix( h_L ).transpose();
		Matrix h_PL = h_LS.transpose();
				
		Matrix alpha_0 = h_LS.times(sinv);
		Matrix alpha_inf = pinv.times(h_PL);
	
		QueryEngine q = new QueryEngine(alpha_0, alpha_inf, Asigmas, operators, base);
		
		//If Debugging Wanted
		//QueryEngine q = new QueryEngine(alpha_0, alpha_inf, Asigmas, maxExponent, base , pinv, sinv, truncatedSVD, this.svd);

		return q;
	}
	
	
	public static int[] reverseOrder(int[] input){
		int[] r = new int[input.length];
		for (int i = 0; i < input.length; i++) {
			r[i] = input[input.length - 1 - i];
		}
		return r;
		
	}
	
	public static double[] getDiagonalArray(Matrix m){
		double[] r = new double[m.getArrayCopy()[0].length];
		for (int i = 0; i < r.length; i++) {
			r[i] = m.get(i, i);
		}
		return r;
	}
	
	public Matrix buildH(int startingIndex, int endingIndex) throws Exception{		
		int hSize = (endingIndex - startingIndex);
		if ( (hSize + startingIndex)*2 > this.probabilities.length){
			throw new Exception("You asked for too large a Hankel Matrix. Increase the max durations recorded in your Environment.");
		} 
		else{
			double[][] hankel = new double[hSize][hSize];
			for (int i = 0; i < hSize; i++) {
				for (int j = 0; j < hSize; j++) {
					hankel[i][j] = this.probabilities[i+j+startingIndex];
				}
			}
			Matrix H = new Matrix(hankel);
			return H;
		}
		
	}

	
	public int getRank(){
		return this.svd.getS().rank();
	}
	
	public Matrix getHankel(){
		return this.svd.getU().times(this.svd.getS()).times(this.svd.getV().transpose());
	}
	
	private synchronized void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException{
		stream.writeInt(this.probabilities.length);
		for (int i=0; i<this.probabilities.length; i++){
			stream.writeObject(this.probabilities[i]);
		}
		stream.writeInt(this.basisSize);
		stream.writeObject(this.svd);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
		int l = in.readInt();
		double[] probabilities = new double[l];
		for (int i = 0; i < l; i++) {
			probabilities[i] = (double) in.readObject();
		}
		this.probabilities = probabilities;
		this.basisSize = in.readInt();
		this.svd = (SingularValueDecomposition) in.readObject();
	}
	
	public void test(){
		Matrix Hbar = new Matrix( new double[][]{ {0,0.2,0.14}, {0.2,0.22,0.15}, {0.14,0.45,0.31} }).transpose();
		
		Matrix Ha = new Matrix(new double[][]{ {0.2,0.22,0.15},{0.22,0.19,0.13},{0.45,0.45,0.32} }).transpose();
		Matrix Hb = new Matrix(new double[][]{ {0.14,0.45,0.31}, {0.15,0.29,0.13}, {0.31,0.85,0.58} } ).transpose();
		Matrix hls = new Matrix(new double[][]{ {0, 0.2, 0.14} } );
		Matrix hpl = new Matrix(new double[][]{ {0, 0.2, 0.14} } ).transpose();
			
		Hbar.print(5,5);
		
		SingularValueDecomposition svd = Hbar.svd();
		Matrix p = svd.getU().times( svd.getS() );
		Matrix s = svd.getV().transpose();
		
		Matrix pinv = p.inverse();
		
		System.out.println("INVERSE TEST");
		p.times(pinv).print(5, 5);
		
		Matrix sinv = s.inverse();
		s.times(sinv).print(5, 5);
		
		Matrix Aa = pinv.times(Ha).times(sinv); 
		Matrix Ab = pinv.times(Hb).times(sinv); 
		
		Matrix alpha0 = hls.times(sinv);	//alpha0 row
		Matrix alphainf = pinv.times(hpl);	//alphainf column
		
		Matrix test1 = alpha0.times(Aa).times(alphainf);
		Matrix test2 = alpha0.times(Ab).times(alphainf);
		Matrix test3 = alpha0.times(Aa).times(Ab).times(alphainf);
		Matrix test4 = alpha0.times(Ab).times(Aa).times(alphainf);
		Matrix test5 = alpha0.times(Aa).times(Aa).times(alphainf);
		Matrix test6 = alpha0.times(Ab).times(Ab).times(alphainf);
		
		test1.print(5,5);
		test2.print(5,5);
		test3.print(5,5);
		test4.print(5,5);
		test5.print(5,5);
		test6.print(5,5);
	}

		

}
