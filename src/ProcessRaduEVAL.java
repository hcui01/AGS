import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.plaf.ActionMapUIResource;

public class ProcessRaduEVAL {
	
	public static void main(String[] args) {
		HashMap<String, HashMap<String, HashMap<Long, HashMap<String, String>>>> resultsSet = new HashMap<>();
		for(int k = 0; k < Global.ratioRef.size(); k ++) {
			
			String MPBP_AGS = "E:\\Research\\EXP\\20180807(NIPS_COMPLEMENTARY_2)\\results\\Radu_Eval_" + Global.ratioRef.get(k) + "_stdout";
			String AOBBF = "E:\\Research\\EXP\\20180807(NIPS_COMPLEMENTARY_2)\\results\\Radu_" + Global.ratioRef.get(k) + "_stdout";
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
					if(line.startsWith("./mmap-solver")) {
						if(ifLastStart) {
							scores_AA.add("NA");
						}
						int a = line.indexOf("/");
						String sub1 = line.substring(line.indexOf("/") + 1, line.length());
						String sub2 = sub1.substring(sub1.indexOf("/") + 1, sub1.length());
						String sub3 = sub2.substring(sub2.indexOf("/") + 1, sub2.length());
						String sub4 = sub3.substring(sub3.indexOf("/") + 1, sub3.length());
						String probName = sub4.substring(0, sub4.indexOf("/"));
						
						if(probNames.size() == 0 || !probName.equals(probNames.get(probNames.size() - 1))) {
							for(int t = 1; t <= 9; t ++) {
								
								probNames.add(probName);
								
								
							}
							for(int t = 1; t <= 9; t ++) {
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
							scores_AA.add("-" + content);
							ifLastStart = false;
						}
					}
					lastLine = line;
				}
			}
			catch (Exception e) {
				// TODO: handle exception
			}

			long time = 100;
			for(int i = 0; i < probNames.size(); i ++) {
				if(i == 0 || !probNames.get(i).equals(probNames.get(i-1))) {
					time = 100;
				}
				
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
				new Records().fileAppend("AGS_MPBP_AAOBF_" + Global.ratioRef.get(k), content);
				
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
				
				if(time < 1000) {
					time += 100;
				}
				else {
					if(time < 10000) {
						time += 1000;
					}
					else {
						time += 10000;
					}
				}
			}
		}
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
	}
}
	
