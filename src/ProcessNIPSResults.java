import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class ProcessNIPSResults {
	static public String resDir = "E:\\Research\\EXP\\20180807(NIPS_COMPLEMENTARY_2)\\results\\";
	public static void main(String[] args) throws Exception {
		String outputDir = "compiled\\Normalized\\numbers\\";
		boolean ifNorm = true;
		HashMap<String, HashMap<String, HashMap<Long, HashMap<String, String>>>> resultsSet = new HashMap<>();
		for(int k = 0; k < Global.ratioRef.size(); k ++) {
			for(int d = 0; d < Global.probNamesRef.size(); d ++) {
				String theRatio = Global.ratioRef.get(k);
				String theDom = Global.probNamesRef.get(d);
				String MPBP_AGS = resDir + "Eval_" + theRatio + "_" + theDom + "_stdout";
				String AOBBF = resDir + "AAOBF_" + theRatio + "_" + theDom + "_stdout";
				ArrayList<String> probNames = new ArrayList<>();
				//counting the new score of the new Ratio 
				int count = 0;
				ArrayList<ArrayList<String>> scores = new ArrayList<>(); 
				ArrayList<String> scores_AA = new ArrayList<>();
				String line = null;
				try {
					// FileReader reads text files in the default encoding.
					FileReader fileReader = new FileReader(MPBP_AGS);
					// Always wrap FileReader in BufferedReader.
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					//reading each line of the file
					while ((line = bufferedReader.readLine()) != null) {
						if(line.startsWith("Solution:")) {
							int end = line.indexOf(")") - 1;
							int start = line.indexOf("(") + 1;
							String content = line.substring(start, end + 1); 
							if(scores.size() == 0 || scores.get(scores.size() - 1).size() == 2) {
								scores.add(new ArrayList<String>());
							}
							scores.get(scores.size() - 1).add(content);
						}
					}
				}
				catch (Exception e) {
					// TODO: handle exception
				}	
				System.out.println(scores);
				int countJump = 0;
				for(int theTime: Global.timesRef) {
					if(theTime < 1000) {
						countJump ++;
					}
				}
				line = null;
				try {
					// FileReader reads text files in the default encoding.
					FileReader fileReader = new FileReader(AOBBF);
					// Always wrap FileReader in BufferedReader.
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					//reading each line of the file
					String lastLine = null;
					Records rec = new Records();
					boolean ifLastStart = false;
					while ((line = bufferedReader.readLine()) != null) {
						if(line.contains("mmap-solver/Release/mmap-solver ")) {
							if(ifLastStart) {
								scores_AA.add("NA");
							}
							int a = line.indexOf("--input-file");
							String sub = line.substring(a);
							String probName = sub.substring(13, sub.indexOf("/"));
							
							if(probNames.size() == 0 || !probName.equals(probNames.get(probNames.size() - 1))) {
								for(int t = 1; t <= countJump; t ++) {
									
									probNames.add(probName);
									
									
								}
								for(int t = 1; t <= countJump; t ++) {
									scores_AA.add("NA");
								}
							}
							
							probNames.add(probName);
					
							ifLastStart = true;
						}
						if(line.startsWith("----------------") && lastLine != null && lastLine.startsWith("[")) {
							int end = lastLine.indexOf("(") - 2;
							int start = end;
							while(lastLine.charAt(start) != ' ') {
								start --;
							}
							start ++;
							
							String content = lastLine.substring(start, end + 1); 
							//System.out.println(content);
							scores_AA.add("-" + content);
							ifLastStart = false;
							
						}
						else {
							if(line.startsWith("Solution:")){
								int end = line.indexOf(")") - 1;
								int start = line.indexOf("(") + 1;
								String content = line.substring(start, end + 1); 
								//System.out.println(content);
								scores_AA.add(content);
								ifLastStart = false;
							}
						}
						lastLine = line;
					}
				}
				catch (Exception e) {
					// TODO: handle exception
				}
				if(scores_AA.size() == scores.size() - 1) {
					scores_AA.add(scores_AA.get(scores_AA.size() - 1));
				}
				if(scores.size() != scores_AA.size() || scores_AA.size() != probNames.size()) {
					throw new Exception("Score lists should be same long!!!!");
				}
				for(int i = 0; i < probNames.size(); i ++) {
					long time = Global.timesRef.get(i % Global.timesRef.size());
					String content = "\n*********************************\n";
					content += "Problem: " + probNames.get(i) + "   Time: " + time + "\n";
					// if the probName list is longer than scores
					// meaning that evaluation is not finished
					// in this case we put "Eval Stop";
					String scoreString1 = null;
					String scoreString2 = null;
					if(i >= scores.size()) {
						scoreString1 = "EvalStop!";
						scoreString2 = "EvalStop!";
						throw new Exception("Evaluation has to finish!!");
					}
					else {
						scoreString1 = String.valueOf(scores.get(i).get(0));
						scoreString2 = String.valueOf(scores.get(i).get(1));
					}
					String scoreString3 = null;
					//possibly aaobf missing last solution
					if(i == scores_AA.size()) {
						scoreString3 = String.valueOf(scores_AA.get(i - 1));
					}
					else {
						scoreString3 = String.valueOf(scores_AA.get(i));
					}
					content += "AGS: " + scoreString1 + "   MPBP: " + scoreString2 + "   AAOBF: " + scoreString3 + "\n";
					new Records().fileAppend("AGS_MPBP_AAOBF_" + theRatio + "_" + theDom, content);
					
					//put things together into resultsset
					if(!resultsSet.containsKey(probNames.get(i))) {
						resultsSet.put(probNames.get(i), new HashMap<String, HashMap<Long, HashMap<String, String>>>());
					}
					if(!resultsSet.get(probNames.get(i)).containsKey(Global.ratioRef.get(k))) {
						resultsSet.get(probNames.get(i)).put(Global.ratioRef.get(k), new HashMap<Long, HashMap<String, String>>());
					}
					if(!resultsSet.get(probNames.get(i)).get(Global.ratioRef.get(k)).containsKey(time)) {
						resultsSet.get(probNames.get(i)).get(Global.ratioRef.get(k)).put(time, new HashMap<String, String>());
					}
					
					resultsSet.get(probNames.get(i)).get(Global.ratioRef.get(k)).get(time).put(Global.algorithmsRef.get(0), scoreString1);
					resultsSet.get(probNames.get(i)).get(Global.ratioRef.get(k)).get(time).put(Global.algorithmsRef.get(1), scoreString2);
					resultsSet.get(probNames.get(i)).get(Global.ratioRef.get(k)).get(time).put(Global.algorithmsRef.get(2), scoreString3);
				}
			}
		}
		//build the max results set
		//regardless algorithm && time, find the max score per probelm/ratio/isnatance
		HashMap<String, HashMap<String, HashMap<Integer, Double>>> maxResSet = new HashMap<>();
		for(int i = 0; i < Global.probNamesRef.size(); i ++) {
			String theProb = Global.probNamesRef.get(i);
			if(!maxResSet.containsKey(theProb)) {
				maxResSet.put(theProb, new HashMap<String, HashMap<Integer, Double>>());
			}
			for(int j = 0; j < Global.ratioRef.size(); j ++) {
				String theRatio = Global.ratioRef.get(j);
				if(!maxResSet.get(theProb).containsKey(theRatio)) {
					maxResSet.get(theProb).put(theRatio, new HashMap<Integer, Double>());
				}
				for(int m = 0; m < Global.countMAPSets; m ++) {
					double max = -Double.MAX_VALUE;
					for(int t = 0; t < Global.timesRef.size(); t ++) {
						for(int a = 0; a < Global.algorithmsRef.size(); a ++) {
							long theTime = Global.timesRef.get(t);
							String theAlg = Global.algorithmsRef.get(a);
							double theVal = 0;
							if(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-Infinity")||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("NA") ||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-inf")) {
								theVal = -Math.log(Double.MAX_VALUE);
							}
							else {
								theVal = Double.valueOf(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg));
							}
							if(theVal > max) {
								max = theVal;
							}
						}
					}
					maxResSet.get(theProb).get(theRatio).put(m, max);
				}
			}
		}
				
		//get the final results for NIPS plotting				
		for(int i = 0; i < Global.probNamesRef.size(); i ++) {
			String theProb = Global.probNamesRef.get(i);
			for(int j = 0; j < Global.ratioRef.size(); j ++) {
				String theRatio = Global.ratioRef.get(j);
				for(int t = 0; t < Global.timesRef.size(); t ++) {
					long theTime = Global.timesRef.get(t);
					ArrayList<Double> avg = new ArrayList<>();
					ArrayList<Double> std = new ArrayList<>();
					ArrayList<Double> ind = new ArrayList<>();
					for(int a = 0; a < Global.algorithmsRef.size(); a ++) {
						avg.add(0.0);
						std.add(0.0);
						String theAlg = Global.algorithmsRef.get(a);
						for(int m = 0; m < Global.countMAPSets; m ++) {
							double theVal = 0;
							boolean ifMinInf = false;
							if(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-Infinity")||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("NA") ||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-inf")) {
								theVal = -Math.log(Double.MAX_VALUE);
								ifMinInf = true;
							}
							else {
								theVal = Double.valueOf(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg));
							}
							
							double score = 0;
							if(ifNorm) {
								if(ifMinInf) {
									score = 1.0;
								}
								else {
									score = (theVal - maxResSet.get(theProb).get(theRatio).get(m)) / theVal;
								}
							}
							else {
								score = theVal - maxResSet.get(theProb).get(theRatio).get(m);
							}
							
							//record individual file
							if(m == 0) {
								ind.add(score);
							}
							avg.set(a, avg.get(a) + score);
						}
						avg.set(a, avg.get(a) / Global.countMAPSets);
						
						for(int m = 0; m < Global.countMAPSets; m ++) {
							double theVal = 0;
							boolean ifMinInf = false;
							if(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-Infinity")||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("NA") ||
									resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg).equals("-inf")) {
								theVal = -Math.log(Double.MAX_VALUE);
								ifMinInf = true;
							}
							else {
								theVal = Double.valueOf(resultsSet.get(theProb + "_" + m).get(theRatio).get(theTime).get(theAlg));
							}
							double score = 0;
							if(ifNorm) {
								if(ifMinInf) {
									score = 1.0;
								}
								else {
									score = (theVal - maxResSet.get(theProb).get(theRatio).get(m)) / theVal;
								}
							}
							else {
								score = theVal - maxResSet.get(theProb).get(theRatio).get(m);
							}
							std.set(a, std.get(a) + Math.pow(score - avg.get(a), 2.0));
						}
						std.set(a, Math.sqrt(std.get(a) / Global.countMAPSets));
					}
					//record aggregating files
					for(int a = 0; a < Global.algorithmsRef.size(); a ++) {
						new Records().fileAppend(resDir + outputDir + theProb + "_AGG_" + theRatio + "_" + Global.algorithmsRef.get(a), 
								theTime + " " + avg.get(a) + " " + std.get(a), false);
					}
					//record individual file
					for(int a = 0; a < Global.algorithmsRef.size(); a ++) {
						new Records().fileAppend(resDir + outputDir + theProb + "_0_" +  theRatio + "_" + Global.algorithmsRef.get(a), 
								theTime + " " + ind.get(a), false);
					}
				}
			}
		}
		/*
		for(int i = 0; i < Global.probNamesRef.size(); i ++) {
			for(int t = 0; t < 3; t ++) {
				String fileCont = new String();
				String theProb = Global.probNamesRef.get(i) + "_" + t;
				for(int j = 0; j < Global.timesRef.size(); j ++) {
					long theTime = Global.timesRef.get(j);
					for(int k = 0; k < Global.ratioRef.size(); k ++) {
						String theRatio = Global.ratioRef.get(k);
						for(String algName: Global.algorithmsRef) {
							if(resultsSet == null || resultsSet.get(theProb) == null || resultsSet.get(theProb).get(theRatio) == null || resultsSet.get(theProb).get(theRatio).get(theTime) == null || resultsSet.get(theProb).get(theRatio).get(theTime).get(algName) == null) {
								@SuppressWarnings("unused")
								int a = 1;
							}
							fileCont += resultsSet.get(theProb).get(theRatio).get(theTime).get(algName) + "\t";
						}
					}
					fileCont += "\n";
				}
				new Records().fileAppend("results\\" + theProb, fileCont);
			}
		}
		*/
	}
}
