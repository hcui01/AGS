import java.awt.dnd.DnDConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import javax.management.RuntimeErrorException;


public class Main {
	static public String dir = DBN.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	static public String fileName = "results";
	static public boolean ifPrintStep = false;
	static public boolean ifBPEval = true;
	static public boolean ifExactEval = false;
	static public boolean ifOptimal = false;
	static public int numberOfNodes = 100;
	static public int numberOfEvi = 10;
	static public int numberOfMap = 10;
	static public long startingTime = 1000;
	static public long endingTime = 3000;
	static public int numRuns = 20;
	static public double ratioOfMap = 0.5;
	static public int maxMAP = -1;
	static public String realFileName = null;
	static public ArrayList<String> instanceNames = Global.probNamesRef;
	static public String instanceName = null;
	static public boolean ifRandom = false;
	static public String fileSep = System.getProperties().getProperty("file.separator");
	static public StringBuilder recor = new StringBuilder();
	static public String instanceDir = ".." + fileSep + ".." + fileSep + "instances" + fileSep;
	public static ArrayList<Double> RunOneGraph(int i, long time) throws Exception{
		recor = new StringBuilder();
		String ite = "******************" + " iteration " + i + " ***************************";
		recor.append(ite + "\n");
		@SuppressWarnings("unused")
		Random r = new Random();
		r.setSeed(i);
		if(ifPrintStep){
			System.out.println("DBN generation starts!");
		}
		Runtime runtime = Runtime.getRuntime();
		//long ori = runtime.totalMemory();
		DBN dbn = null;
		//DBN dbnForBP = null;
		if (ifRandom) {
			dbn = new DBN(r, numberOfNodes, numberOfEvi, numberOfMap);
		} else {
			dbn = new DBN(r, instanceDir + instanceName + ".uai",
					instanceDir + instanceName + ".uai.evid", null, 
					ratioOfMap, maxMAP, true);
			//dbnForBP = new DBN(r, "instances" + fileSep + instanceName + ".uai",
					//"instances" + fileSep + instanceName + ".uai.evid", null, 
					//ratioOfMap, maxMAP, true);
		}

		/*********************************************
		 *  copy the UAI, evid, and MAP files to bin
		 *********************************************/
		Records rec = new Records();
		String MAPString = new String();
		MAPString += dbn.MAP.size() + " ";
		for(int m: dbn.MAP) {
			MAPString += (m + " ");
		}
		
		//copyFilesReplace(instanceDir + instanceName + ".uai", 
			//	instanceName + "_" + i  + fileSep + instanceName + ".uai");
		//copyFilesReplace(instanceDir + instanceName + ".uai.evid", 
			//	instanceName + "_" + i  + fileSep + instanceName + ".uai.evid");
		//rec.fileAppend(instanceName + "_" + i  + fileSep + instanceName + ".uai.map", MAPString);
		
		/*********************************************
		 *  turn the DBN into factor graph as MPBP works on undirected
		 **********************************************/
		if(ifPrintStep){
			System.out.println("Facotr graph generation starts!");
		}
		FactorGraph fg = new FactorGraph(dbn);
		
		/*********************************************
		 *  MPBP run
		 *********************************************/
		if(ifPrintStep){
			System.out.println("BP starts!");
		}
		BP bp = new BP(fg, time, recor);
		
		/**********************************************
		 * record solution of BP to bin 
		 ***********************************************/
		
		createDir(instanceName + "_" + i  + fileSep + "Solution"  + fileSep + "MPBP"  + fileSep + (time / 1000));
		String solutionStr = new String();
		
		for(int k = 0; k < fg.MAP.size(); k ++){
			int theMAP = fg.MAP.get(k);
			if(!bp.MapRes.get(theMAP)){
				solutionStr += (false + " ");
			}
			else {
				solutionStr += (true + " ");
			}
		}
		//System.out.println("BP Results: " + solutionStr);
		rec.fileAppend(instanceName + "_" + i + fileSep + "Solution"  + fileSep + "MPBP"  + fileSep + (time / 1000) + fileSep + "Solution", solutionStr);

		/**********************************************
		 * Evaluarte BP results
		 **********************************************/
		/*
		if(numberOfMap == 1){
			if(EvalRes(bp.MapRes, fg)){
				countBPRight ++;
			}
		}
		*/
		
		/**************************
		 * reduction to another dbn for SOGBOFA preparation
		 **************************/
		if(ifPrintStep){
			System.out.println("Reduction starts!");
		}
		DBN red = dbn.Reduction3();
		

		/************************
		 * SOGBOFA run
		 ************************/
		if(ifPrintStep){
			System.out.println("SOGBOFA starts!");
		}
		//ori = runtime.totalMemory();
		RevAccGradient sogSolver = new RevAccGradient(red, time, recor, r);

		//System.out.println("SOGBOFA costs " + (runtime.totalMemory() - ori) );
		ArrayList<Double> res = sogSolver.getActions();
		HashMap<Integer, Boolean> SOGRes = new HashMap<>();
		
		//Extract marginals of all nodes in the graph
		for(int k = 0; k < res.size(); k ++){
			if(res.get(k) == 0){
				SOGRes.put(red.MAP.get(k), false);
			}
			else{
				SOGRes.put(red.MAP.get(k), true);
			}
		}
		
		/*****************************************
		 *  record solution of SOGBOFA to bin
		 ****************************************/
		createDir(instanceName + "_" + i + fileSep + "Solution"  + fileSep + "AGS"  + fileSep + (time/1000));
		solutionStr = new String();
		for(int k = 0; k < res.size(); k ++){
			if(res.get(k) == 0){
				solutionStr += (false + " ");
			}
			else {
				solutionStr += (true + " ");
			}
		}
		rec.fileAppend(instanceName + "_" + i + fileSep + "Solution"  + fileSep + "AGS"  + fileSep + (time/1000) + fileSep + "Solution", solutionStr);
		
		/*******************************************
		 * evaluate the results from MPBP && SOGBOFA
		 ********************************************/
		if(ifPrintStep){
	    	System.out.println("Evaluation starts!");
	    }
		ArrayList<Double> twoEval = null;
		if(ifExactEval || ifBPEval) {
			twoEval = FirstIsBetter(ifOptimal, bp.MapRes, SOGRes, fg, red, dbn, ifBPEval);
		}
		
		dbn = null;
		fg = null;
		bp = null;
		red = null;
		sogSolver = null;
		SOGRes = null;
		return twoEval;
	}
	public static ArrayList<Double> Exe(int runTime, int maxOut, int maxIn, int numberOfNodes, int numberOfEvidence, int numberOfMap, long time, boolean ifBPEval) throws Exception{
		int countBPRight = 0;
		int countSOGRight = 0;
		int countSOGWins = 0;
		int countBPWins = 0;
		ArrayList<Double> firstEval  = new ArrayList<Double>();
		ArrayList<Double> secondEval = new ArrayList<Double>();
		ArrayList<Double> OPTEval = new ArrayList<Double>(); 
		ArrayList<Double> evalDiff = new ArrayList<Double>(); 
		ArrayList<Double> results = new ArrayList<>();
		Records rec = new Records();
		
		long currentTime = System.currentTimeMillis();
		for(int i = 0; i < runTime; i ++){
			createDir(instanceName + "_" + i);
			TreeExp.nodesToNumber.clear();
			//System.out.println("plus hash: " + TreeExp.PlusHash.size());
			TreeExp.PlusHash.clear();		
			TreeExp.MinusHash.clear();
			TreeExp.TimesHash.clear();
			TreeExp.DividHash.clear();
			TreeExp.nodesToNumber = null;
			TreeExp.PlusHash = null;
			TreeExp.MinusHash = null;
			TreeExp.TimesHash = null;
			TreeExp.DividHash = null;
			TreeExp.nodesToNumber = new HashMap<Double, TreeExp>();
			TreeExp.PlusHash = new HashMap<Object, HashMap<Object, TreeExp>>();
			TreeExp.MinusHash = new HashMap<Object, HashMap<Object, TreeExp>>();
			TreeExp.TimesHash = new HashMap<Object, HashMap<Object, TreeExp>>();
			TreeExp.DividHash = new HashMap<Object, HashMap<Object, TreeExp>>();
			ArrayList<Double> twoEval = RunOneGraph(i, time);
			if(twoEval != null) {
				firstEval.add(twoEval.get(0));
				if (twoEval.get(0) > twoEval.get(1)) {
					countBPWins++;
				}
				secondEval.add(twoEval.get(1));
				// difference = SOG - BP
				evalDiff.add(twoEval.get(1) - twoEval.get(0));
				if(ifOptimal){
					OPTEval.add(twoEval.get(2));
				}
				recor.append("BP Score:" + twoEval.get(0) + "\n");
				recor.append("AGS score:" + twoEval.get(1) + "\n");
				if (twoEval.get(0) < twoEval.get(1)) {
					countSOGWins++;
				}
				rec.fileAppend(dir, "BP_" + time, String.valueOf(twoEval.get(0)));
				if(new Global().ifLift){
					rec.fileAppend(dir, "LIFT_" + time, String.valueOf(twoEval.get(1)));
				}
				else{
					rec.fileAppend(dir, "AGS_" + time, String.valueOf(twoEval.get(1)));
				}
				
				if(ifOptimal){
					rec.fileAppend(dir, "OPT_" + time, String.valueOf(twoEval.get(2)));
				}
			}
			
			//System.out.println("out of 1000: " + countRight + " are right!");
			if(ifOptimal){
				recor.append("BP: out of " + numRuns + ": "  + countBPRight + " are right!\n");
				recor.append("AGS: out of " + numRuns + ": "  + countSOGRight + " are right!\n");
			}
			else{
				recor.append("AGS: out of " + numRuns + ": "  + countSOGWins + " wins!\n");
				recor.append("BP: out of " + numRuns + ": "  + countBPWins + " wins!\n");
			}
			System.out.print(recor);
			rec.fileAppend(dir, realFileName, recor.toString());
			recor = null;
			System.gc();
		}
		results.add(countBPWins + 0.0);
		results.add(countSOGWins + 0.0);
		//calculate avg and std for BP
		double avg = 0, std = 0;
		for(double d: firstEval){
			avg += d;
		}
		avg /= firstEval.size();
		for(double d: firstEval){
			std += Math.pow(d - avg, 2.0);
		}
		std = Math.sqrt(std / firstEval.size());
		results.add(avg);
		results.add(std);
		//calculate avg and std for SOGBOFA
		avg = 0;
		std = 0;
		for(double d: secondEval){
			avg += d;
		}
		avg /= secondEval.size();
		for(double d: secondEval){
			std += Math.pow(d - avg, 2.0);
		}
		std = Math.sqrt(std / secondEval.size());
		results.add(avg);
		results.add(std);
		//calculate avg and std for difference
		avg = 0;
		std = 0;
		for (double d : evalDiff) {
			avg += d;
		}
		avg /= secondEval.size();
		for (double d : evalDiff) {
			std += Math.pow(d - avg, 2.0);
		}
		std = Math.sqrt(std / secondEval.size());
		results.add(avg);
		results.add(std);
		//calculate avg and std for OPT
		avg = 0;
		std = 0;
		for (double d : OPTEval) {
			avg += d;
		}
		avg /= secondEval.size();
		for (double d : OPTEval) {
			std += Math.pow(d - avg, 2.0);
		}
		std = Math.sqrt(std / secondEval.size());
		results.add(avg);
		results.add(std);
		
		firstEval = null;
		secondEval = null;
		evalDiff = null;
		rec = null;
		recor = null;
		return results;
	}
	
	private static double FindOptScore(FactorGraph fg, DBN theDBN, DBN oriDBN, Boolean ifBPEval) throws Exception{
		 int numberOfAct = (int)Math.pow(2.0, fg.MAP.size());
		 double maxVal = -1;
		 HashMap<Integer, Boolean> bestsolution = new HashMap<>();
		 for(int i = 0; i < numberOfAct; i ++){
			HashMap<Integer, Boolean> theSolution = new HashMap<>();
			int tmp = i;
			for(int j = 0; j < fg.MAP.size(); j ++){
				int residule = tmp % 2;
				tmp /= 2;
				theSolution.put(fg.MAP.get(j), residule == 1 ? true : false);
			}
			Optimal eval = new Optimal(fg, theDBN, oriDBN);
			double theVal = -1;
			if(ifBPEval){
				theVal = eval.BPEvaluate(theSolution);
			}
			else{
				theVal = eval.Evaluate(theSolution);
			}
			if(theVal > maxVal){
				bestsolution = theSolution;
				maxVal = theVal;
			}
		 }
		 return maxVal;
	}

	
	
	private static ArrayList<Double> FirstIsBetter(Boolean ifOptimal, HashMap<Integer, Boolean> firstRes, HashMap<Integer, Boolean> secondRes, FactorGraph fg, DBN theDBN, DBN oriDBN, boolean ifBPEval) throws Exception{
		//red to corresponding results in original graph
		HashMap<Integer, Boolean> oriRes = new HashMap<>();
		ArrayList<Double> res = new ArrayList<>();
		// ori = runtime.totalMemory();
		for (int i = 0; i < fg.MAP.size(); i++) {
			oriRes.put(fg.MAP.get(i), secondRes.get(theDBN.MAP.get(i)));
		}
		boolean ifSameResul = true;
		for(int i = 0; i < fg.MAP.size(); i ++){
			int theMap = fg.MAP.get(i);
			if(oriRes.get(theMap) != firstRes.get(theMap)){
				ifSameResul = false;
				break;
			}
		}
		if(ifSameResul){
			res.add(-1.0);
			res.add(-1.0);
			recor.append("Same results: tie!\n");
			return res;
		}
		
		//long ori = runtime.totalMemory();
		Optimal eval = null;
		
			eval = new Optimal(fg, theDBN, oriDBN);
			//System.out.println("Optimal costs " + (runtime.totalMemory() - ori) );
			recor.append("Evaluation Result: \n");
			
			double oneVal = 0;
			if(ifBPEval){
				//ori = runtime.totalMemory();
				oneVal = eval.BPEvaluate(firstRes);
				//System.out.println("BPEvaluate costs " + (runtime.totalMemory() - ori) );
			}
			else{
				oneVal = eval.Evaluate(firstRes);
			}
			eval = null;
			//ori = runtime.totalMemory();
			eval = new Optimal(fg, theDBN, oriDBN);
			//System.out.println("Optimal costs " + (runtime.totalMemory() - ori) );
			
			//System.out.println("oriput costs " + (runtime.totalMemory() - ori) );
			double secondVal = 0;
			if(ifBPEval){
				//ori = runtime.totalMemory();
				secondVal = eval.BPEvaluate(oriRes);
				//System.out.println("BPEvaluate costs " + (runtime.totalMemory() - ori) );
			}
			else{
				secondVal = eval.Evaluate(oriRes);
			}
			
			if(oneVal == -1){
				secondVal = -1;
			}
			if(secondVal == -1){
				oneVal = -1;
			}
			recor.append("BP: " + oneVal + "\n");
			recor.append("AGS: " + secondVal + "\n");
			
			if(oneVal > secondVal){
				recor.append("BP ");
			}
			else{
				if(secondVal > oneVal){
					recor.append("AGS ");
				}
				else{
					recor.append("No one ");
				}
			}
			
			recor.append("wins!!\n\n");
			
			res.add(oneVal);
			res.add(secondVal);
			if(ifOptimal){
				res.add(FindOptScore(fg, theDBN, oriDBN, ifBPEval));
			}
			//System.out.println("Evaluation " + i + ": " + oneVal + " VS. " + secondVal);
			
			
		
		oriRes = null;
		firstRes = null;
		secondRes = null;
		eval = null;
		return res;
	}
	/*
	public static void BPConverge(int runTime, int maxOut, int maxIn, int numberOfNodes, int numberOfEvidence, int numberOfMap, double thre, long maxTime){
		int countBPRight = 0;
		int countSOGRight = 0;
		int countSOGWins = 0;
		int countBPWins = 0;
		Records rec = new Records();
		
		System.out.println(maxOut + "_" + maxIn + "_" + numberOfNodes + "_" + numberOfEvidence + "_" + numberOfMap + "_" + thre + "_" + maxTime);
		for(int i = 0; i < runTime; i ++){
			recor = null;
			recor = new StringBuilder();
			String ite = "******************" + " iteration " + i + " ***************************";
			recor.append(ite + "\n");
			@SuppressWarnings("unused")
			Random r = new Random();
			r.setSeed(i);
			DBN dbn = new DBN(r, maxOut, maxIn, numberOfNodes, numberOfEvidence, numberOfMap);
			
			//System.out.println("Directed graph generated!");
			@SuppressWarnings("unused")
			FactorGraph fg = new FactorGraph(dbn);
			
			//System.out.println("Factor graph generated!");
			@SuppressWarnings("unused")
			long currentTime = System.currentTimeMillis();
			BP bp = new BP(fg, thre, recor, maxTime);
			System.out.println(runTime + ": " + (System.currentTimeMillis() - currentTime) + "\n");
		}
	}
	*/
	public static void main(String[] args) throws Exception {
		if(args.length == 1) {
			ratioOfMap = Double.valueOf(args[0]);
		}
		if(args.length == 2) {
			ratioOfMap = Double.valueOf(args[0]);
			instanceNames = new ArrayList<String>(new ArrayList<String>(Arrays.asList(args[1])));
		}
		if(args.length == 4) {
			ratioOfMap = Double.valueOf(args[0]);
			
			numRuns = Integer.valueOf(args[2]);
			Global.timesRef = new ArrayList<>();
			Global.timesRef.add(Integer.valueOf(args[3]));
			ifRandom = true;
		}

		if(args.length == 5) {
			if(args[0].matches("-?(0|[1-9]\\d*)")) {
				numberOfNodes = Integer.valueOf(args[0]);
				numberOfMap = Integer.valueOf(args[1]);
				numberOfEvi = Integer.valueOf(args[2]);
				numRuns = Integer.valueOf(args[3]);
				Global.timesRef = new ArrayList<>();
				Global.timesRef.add(Integer.valueOf(args[4]));
				ifRandom = true;
			}else {
				instanceDir = args[0];
				instanceNames = new ArrayList<String>(new ArrayList<String>(Arrays.asList(args[1])));
				ratioOfMap = Double.valueOf(args[2]);
				numRuns = Integer.valueOf(args[3]);
				Global.timesRef = new ArrayList<>();
				Global.timesRef.add(Integer.valueOf(args[4]));
				ifRandom = false;
			}
			
		}
		Records r = new Records();
		//long time = Integer.valueOf(args[6]);
		/*
		long time = Long.valueOf(args[0]);
		numberOfNodes = Integer.valueOf(args[1]);
		numberOfEvi = Integer.valueOf(args[2]);
		*/
		
		for(int i = 0; i < instanceNames.size(); i ++) {
			instanceName = instanceNames.get(i);
			dir = dir.substring(0, dir.lastIndexOf('/') + 1);
			//System.out.println("************** Start running on: " + instanceName + "*************");
			for(long time: Global.timesRef){
				time *= 1000;
				if(ifRandom)
					realFileName = "Evaluation_" + fileName + "_" + numberOfNodes + "_" + numberOfEvi + "_" + numberOfMap + "_" + (time / 1000);
				else {
					realFileName = "Evaluation_" + fileName + "_" + args[1] + "_" + (time / 1000);
				}
				ArrayList<Double> sta = Exe(numRuns, -1, -1, numberOfNodes, numberOfEvi, numberOfEvi, time, ifBPEval);
				String folder = instanceName + "\\";
				/*
				r.fileAppend(dir, folder + "BP_num_wins", time + " " + sta.get(0));
				if(new Global().ifLift){
					r.fileAppend(dir, folder + "LIFT_num_wins", time + " " + sta.get(1));
				}
				else{
					r.fileAppend(dir, folder + "SOG_num_wins", time + " " + sta.get(1));
				}
				
				r.fileAppend(dir, folder + "BP_scores", time + " " + sta.get(2) + " " + sta.get(3));
				if(new Global().ifLift){
					r.fileAppend(dir, folder + "LIFT_scores", time + " " + sta.get(4) + " " + sta.get(5));
				}
				else{
					r.fileAppend(dir, folder + "SOG_scores", time + " " + sta.get(4) + " " + sta.get(5));
				}
				if(new Global().ifLift){
					r.fileAppend(dir, folder + "L_B_Diffrence", time + " " + sta.get(6) + " " + sta.get(7));
				}
				else{
					r.fileAppend(dir, folder + "S_B_Diffrence", time + " " + sta.get(6) + " " + sta.get(7));
				}
				
				if(ifOptimal){
					r.fileAppend(dir, folder + "OPT_scores", time + " " + sta.get(8) + " " + sta.get(9));
				}
				*/
				
				
				
			}
			
		}
		r = null;
	}
	
	public static boolean createDir(String destDirName) {  
		MyPath myPath = new MyPath();
		String absPath = myPath.getProjectPath();
		destDirName = absPath + System.getProperties().getProperty("file.separator") + destDirName;
        File dir = new File(destDirName);  
        if (dir.exists()) {   
            return false;  
        }  
        if (!destDirName.endsWith(File.separator)) {  
            destDirName = destDirName + File.separator;  
        }  
        if (dir.mkdirs()) {  
            return true;  
        } else {  
            return false;  
        }  
    }
	
	private static void copyFilesReplace(String source, String dest) throws IOException {
		Records rec = new Records();
		FileReader fileReader = new FileReader(source);
		// Always wrap FileReader in BufferedReader.
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		DeleteFile(dest);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			//copy the file to new directory \
			//used to orgnize the problem files and solutions
			rec.fileAppend(dest, line);
		}
		// Always close files.
		bufferedReader.close();
	}
	
	private static void DeleteFile(String dest) throws IOException {
		File file = new File(dest);
		boolean result = Files.deleteIfExists(file.toPath());
	}
}
