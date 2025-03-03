package hmm_sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FlowControl {
	
	public static void main(String[] args){
		int dS = 10000;
		int[] trajectorySizes = new int[]{dS};//5,50,100,200,500,1000,2000,4000,8000,16000,32000,64000,128000,256000};
		int dataSizeForFixedPlots = dS;
		int base = 2; // Haven't tested for bases other than 2 ... no guarantees
	
		/*for (int i : trajectorySizes) {
			int[] temp = new int[]{i};
			FlowControl.testLabyrinths(temp, i, base);
		}*/
		//String f = "ErrorStorage";

		FlowControl.testLoops(trajectorySizes, dataSizeForFixedPlots, base);
		//FlowControl.testLabyrinths(trajectorySizes, dataSizeForFixedPlots,  base);

		//FlowControl.computeKeySearchStuff(trajectorySizes, dataSizeForFixedPlots, base, f, "Over-Base");
	}
	
	public static void testLabyrinths(int[] trajectorySizes, int dataSizeForFixedPlots, int base){
		//ADD REPEITIONS TO MODELSIZEEFFECTOVERBASE, not using it right now 
		
		int repetitions = 10;
		int stretchFactor = 1;
		int hSize = 600;
		int basisSize = 300;
		int fixedModelSize = 50;
		int keyLocation = 10;
		
		//int[] modelSizes = new int[]{32,33,34,35, 40, 60, 80};
		
		int[] modelSizes = new int[]{2,4,6,8,10,12,14,16,18,20,25,30,35,40}; //JUMPS

		String workingFolder = "keySearchPacMan/";
	
		System.out.println("Generating data:");
		System.out.println("");
		FlowControl.createFolder(workingFolder);
		LabyrinthGraph l = LabyrinthGraph.pacMan(workingFolder, hSize, stretchFactor, keyLocation, true);
		//LabyrinthGraph l = LabyrinthGraph.testLabyrinth(workingFolder, hSize, stretchFactor);
		l.generateData(trajectorySizes, repetitions, true);
		
		System.out.println("");
		
		System.out.println("Reading data into models");
		FlowControl.readDataIntoModels(workingFolder, basisSize);
		System.out.println("Done loading models");
		System.out.println("");
		
		testEngine a = new testEngine(workingFolder,"Models_Emperical_" + workingFolder, "Models_True_" + workingFolder, dataSizeForFixedPlots, basisSize, base, modelSizes, fixedModelSize ,repetitions, true );
		
		String identifier = "Datasize:" + dataSizeForFixedPlots + "PacMan";

		a.modelSizeEffectOverBaseImprovement(identifier, dataSizeForFixedPlots);
		//a.plotSingularValues(100);
		//a.makePlots();
	}
	
	public static void testLoops(int[] trajectorySizes, int dataSizeForFixedPlots, int base){
		int repetitions = 10;
		//IF YOU HAVE A BUG WITH READING FILES, DELETE THE FOLDER AND RERUN~~~!!!

		int loop1 = 64;
		int loop2 = 16;
		int hSize = 600;
		int basisSize = 150;
		//int[] modelSizes = new int[]{2,3,4,6,8,10,11,12,13,15,18,25};
		int[] modelSizes = new int[]{10,12,15,17,20,25,30,35,40,45,50,55,60,65};
		//int[] modelSizes = new int[]{30,32,34,35,40,45,50};
		//Bug of having all errors exactly the same seems to occur when taken model size is really large e.g 50 was tried
		
		String workingFolder = Integer.toString(loop1) + "_" + Integer.toString(loop2) + "_Toy_Labyrinth/";
		
		System.out.println("Generating data:");
		System.out.println("");
		FlowControl.createFolder(workingFolder);
		double selfTransitionProbability = 0.00;
		rawHMM r = rawHMM.makeLabyrinth(workingFolder, loop1, loop2, selfTransitionProbability, hSize, .5, .5);
		r.generateData(trajectorySizes, repetitions, false);
		
		System.out.println("Reading data into models");
		FlowControl.readDataIntoModels(workingFolder, basisSize);
		System.out.println("Done loading models");
		System.out.println("");
		
		int debugMS = modelSizes[0];
		testEngine a = new testEngine(workingFolder,"Models_Emperical_" + workingFolder, "Models_True_" + workingFolder, dataSizeForFixedPlots , basisSize, base, modelSizes, debugMS , repetitions , true);
		
		String identifier = "Datasize:" + dataSizeForFixedPlots + "," + loop1 + ":" + loop2 + ",ST:"+ selfTransitionProbability;
		a.modelSizeEffectOverBaseImprovement(identifier, dataSizeForFixedPlots);

	}
	
	public static void computeKeySearchStuff(int[] trajectorySizes, int dataSizeForFixedPlots, int base, String f, String computeType){
		int repetitions = 10;
		int stretchFactor = 10;
		int hSize = 500;
		int basisSize = 300;
		int key = 10;
		int samples = 1000;
		
		if (computeType == "Over-MaxK"){
			double[] maxKs = new double[]{ 300 };	// get all 0s when maxK is <= 20
			double[] mS = new double[]{ 30, 50, 70 };
			double maxPowers[] = { 1, 16, 32, 64, 128};

			double[][][] errorInfoTraining = new double[maxKs.length][maxPowers.length][mS.length];
			double[][][] errorInfoTesting = new double[maxKs.length][maxPowers.length][mS.length];
			double[][][] xAxes = new double[maxKs.length][maxPowers.length][mS.length];
			
			for (int i=0;i<maxPowers.length;i++) {
				KeySearching ks = new KeySearching(samples, key, basisSize, hSize, stretchFactor, trajectorySizes, dataSizeForFixedPlots, repetitions, base);
				ErrorPair e = ks.searchOverMaxK(mS, maxKs, 128);
				double[][] t1 = e.getTrainingErrors();
				double[][] t2 = e.getTestingErrors();
				for (int j = 0; j < e.getTrainingErrors().length; j++) {
					errorInfoTraining[j][i] = t1[j];
					errorInfoTesting[j][i] = t2[j];
					xAxes[j][i] = mS;
				}
			}
			String title = "Distance Predictions";
			String internalComment = "Darker Curves --> Richer Base System";
			//FlowControl.writeKeyErrorsToFile(errorInfoTraining, errorInfoTesting, xAxes, f);
			//FlowControl.printErrors(maxKs, f, title, internalComment);
		}
		if (computeType == "Over-Base"){
			double[] maxKs = new double[]{ 100 };	// get all 0s when maxK is <= 20
			double[] mS = new double[]{30,40,50,60,70,100};
			double maxPowers[] = { 1, 8, 16, 64, 128};

			double[][][] errorInfoTraining = new double[maxKs.length][maxPowers.length][mS.length];
			double[][][] errorInfoTesting = new double[maxKs.length][maxPowers.length][mS.length];
			double[][][] xAxes = new double[maxKs.length][maxPowers.length][mS.length];
			
			for (int i=0;i<maxKs.length;i++){
				System.out.print("Current MaxK: ");
				System.out.println(maxKs[i]);
				KeySearching ks = new KeySearching(samples, key, basisSize, hSize, stretchFactor, trajectorySizes, dataSizeForFixedPlots, repetitions, base);
				ErrorPair e = ks.searchOverBase(mS, maxPowers, (int) maxKs[i]);
				double[][] t1 = e.getTrainingErrors();
				double[][] t2 = e.getTestingErrors();
				for (int j = 0; j < e.getTrainingErrors().length; j++) {
					errorInfoTraining[i][j] = t1[j];
					errorInfoTesting[i][j] = t2[j];
					xAxes[i][j] = mS;
				}
			}
			String title = "Distance Predictions";
			String internalComment = "Darker Curves --> Richer Base System";
			//FlowControl.writeKeyErrorsToFile(errorInfoTraining, errorInfoTesting, xAxes, f);
			//FlowControl.printErrors(maxKs, f, title, internalComment);
		}
		System.out.println("Done generated error data");
		System.out.println();	
	}
	
	public static void printErrors(double[] maxKs, String f, String title, String internalComment){
		ModelKeySearchComparison r = FlowControl.readKeyErrorsToFile( f );
		r.printOut();
		double[][][] testing = r.getTestingData();
		double[][][] training = r.getTrainingData();
		double[][][] xAxes = r.getxAxes();
		
		String workingFolder = "keySearchPacMan/";
		String empModels = "Models_Emperical_" + workingFolder;
		String pltFolder = workingFolder + "Plotting_" + empModels + "/";
		
		for (int i = 0; i < xAxes.length; i++) {
			double[][] t1 = testing[i];
			double[][] t2 = training[i];
			double[][] x = xAxes[i];
			
			String filename = pltFolder + "KeySearchBaseComp";
			String xaxisLabel = "ModelSize";

			String s = Integer.toString( (int) maxKs[i] );
			System.out.println("Writing out " + filename + "Testing_" + s );
			OutputData.outputData(filename + "Testing_" + s, xaxisLabel, "", x, t1, title, internalComment);
			System.out.println("Writing out " + filename + "Training_" + s);
			OutputData.outputData(filename + "Training_" + s, xaxisLabel, "", x, t2, title, internalComment);
		}
	}
	
	public static void writeKeyErrorsToFile(double[][][] d1, double[][][] d2, double[][][] d3, String s){
		try{
			ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream(s) );
			oos.writeObject(d1);
			oos.writeObject(d2);
			oos.writeObject(d3);
			oos.close();
		}
		catch(Exception e){
			System.out.println("Problem writing error data to file");
			e.printStackTrace();
		}
	}
	
	public static ModelKeySearchComparison readKeyErrorsToFile( String s ){
		try{
			ObjectInputStream ois = new ObjectInputStream( new FileInputStream(s) );
			double[][][] train = (double[][][]) ois.readObject();
			double[][][] test = (double[][][]) ois.readObject();
			double[][][] xaxis = (double[][][]) ois.readObject();
	
			ModelKeySearchComparison r = new ModelKeySearchComparison(train, test, xaxis);
			ois.close();
			return r;
		}
		catch(Exception e){
			System.out.println("Problem writing error data to file");
			e.printStackTrace();
			return null;
		}
	}
	
	public FlowControl(){
		
	}
	
	public static void createFolder(String folder){
		File dir = new File(folder);
		dir.mkdir();
	}
	
	public static File[] getFiles(String folder){
		File dir = new File(folder);
		return dir.listFiles();
	}
	
	public static void readDataIntoModels(String workingFolder, int basisSize){
		String inFolder = workingFolder + "Emperical_" + workingFolder;
		String outFolder = workingFolder + "Models_Emperical_" + workingFolder; 
		FlowControl.createFolder(outFolder);
		
		String inFile = workingFolder + "True_" + workingFolder;
		String TrueOut = workingFolder + "Models_True_" + workingFolder;
		FlowControl.createModelsFromFile("", "", inFile, TrueOut, basisSize);
		
		File[] files = FlowControl.getFiles(inFolder);
		for (File file : files) {
			FlowControl.createModelsFromFile(inFolder, outFolder, file.getName(), "Models_" + file.getName(), basisSize);
		}
	}	
	
	
	public static void createModelsFromFile(String inFolder, String outFolder, String fileIn, String fileOut, int basisSize){
		double[][] data = FlowControl.readData(inFolder + fileIn);
		/*System.out.println(fileOut);
		System.out.println("Data length FlowControl");
		System.out.println(data.length);
		System.out.println();
		*/
		HankelSVDModel[] modelsForFixedTrajectorySize = new HankelSVDModel[data.length];

		for (int i = 0; i < modelsForFixedTrajectorySize.length; i++) {
			modelsForFixedTrajectorySize[i] = new HankelSVDModel(data[i], basisSize);
		}
		
		FlowControl.outputModelsToFile(modelsForFixedTrajectorySize, outFolder, fileOut);
		
	}
	
	private static void outputModelsToFile(HankelSVDModel[] h, String outfolder, String fileOut){
		try{
			ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream(outfolder + fileOut) );
			for (int i = 0; i < h.length; i++) {
				oos.writeObject(h[i]);
			}
			oos.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public static void outputData(String s, double[][] data, boolean verbose){
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream( new FileOutputStream( s ) );
			out.writeObject( data );
			out.close();
		} catch (IOException e) {
			System.out.println("Problem writing data");
			e.printStackTrace();
		}
		
		if(verbose){
			System.out.println("Done generating data for " + s);
		}
	}
	
	public static double[][] readData(String filename){
		double[][] data;
		try{
			ObjectInputStream ois = new ObjectInputStream( new FileInputStream(filename) );
			data = (double[][]) ois.readObject();
			ois.close();
			return data;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}


}
