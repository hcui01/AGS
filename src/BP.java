import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.print.attribute.HashAttributeSet;


public class BP {
	double convergeThres = 0.0001;
	ArrayList<ArrayList<ArrayList<Double>>> oldMessages = new ArrayList<>();
	FactorGraph fg = null;
	HashMap<Integer, Boolean> MapRes = new HashMap<>();
	double maxChangeForBP = 2;
	public boolean ifConverge = true;
	public static boolean ifPrintIte = false;
	BP(FactorGraph theFG, long t, StringBuilder recor){
		long currentTime = System.currentTimeMillis();
		fg = theFG;
		//initialize messages
		//long ori = Runtime.getRuntime().totalMemory();
		for(int i = 0 ; i < fg.nodeNum; i ++){
			ArrayList<ArrayList<Double>> col = new ArrayList<>();
			oldMessages.add(col);
			col = null;
			for(int j = 0; j < fg.connectedList.get(i).size(); j ++){
				ArrayList<Double> tmpMsg = ConstructMessage(0.5, 0.5);
				oldMessages.get(i).add(tmpMsg);
				tmpMsg = null;
			}
		}

		//keep iterating on all messages
		double maxChange = Double.MAX_VALUE;
		//initialize the max nodes
		//ori = Runtime.getRuntime().totalMemory();
		for(int mapNode: fg.MAP){
			MapRes.put(mapNode, true);
		}

		//cancel iteration when max change is small enough
		int iteCounter = 0;
		boolean ifTimeout = false;
		while(true){
			
			
			long used = System.currentTimeMillis() - currentTime;
			if(ifTimeout){
				break;
			}
			if(maxChange < convergeThres){
				recor.append("BP converged in " + used + " milliseconds!\n");
				break;
			}
			ArrayList<ArrayList<ArrayList<Double>>> messages = new ArrayList<ArrayList<ArrayList<Double>>>();
			// traverse each node in the fg
			for(int i = 0; i < fg.nodeNum; i ++){
				if(ifTimeout){
					break;
				}
				messages.add(new ArrayList<ArrayList<Double>>());
				for(int j = 0; j < fg.connectedList.get(i).size(); j ++){
					if(ifTimeout){
						break;
					}
					int dest = fg.connectedList.get(i).get(j);
					
					//1. evidence node always sends out constant message
					//ori = Runtime.getRuntime().totalMemory();
					if(fg.evidence.containsKey(i)){
						double value = 0.0;
						if(fg.evidence.get(i)){
							value = 1.0;
						}
						ArrayList<Double> theMsg = ConstructMessage(value, 1.0 - value);
						//System.out.println(theMsg);
						messages.get(i).add(theMsg);
						theMsg = null;
						continue;
					}

					//2. max node (must be variable node)
					//ori = Runtime.getRuntime().totalMemory();
					if(fg.MAP.contains(i)){
						//calculate usual mesage from i to dest
						ArrayList<Double> tmpMsg = GetVarNodeMsg(oldMessages, i, dest);
						//calculate current belief of I
						ArrayList<Double> currentBeliefI = GetVarNodeMsg(oldMessages, i, -1);
						//System.out.println(currentBeliefI);
						//belief of true is larger, abonden second item of the message
						if(currentBeliefI.get(0) > currentBeliefI.get(1)){
							tmpMsg.set(1, 0.0);
							MapRes.put(i, true);
						}
						if(currentBeliefI.get(1) > currentBeliefI.get(0)){
							tmpMsg.set(0, 0.0);
							MapRes.put(i, false);
						}
						//System.out.println(tmpMsg);
						messages.get(i).add(tmpMsg);
						tmpMsg = null;
						continue;
					}

					//3. sum node (variable node)
					//ori = Runtime.getRuntime().totalMemory();
					if(i < fg.nodeNum / 2){
						//calculate usual mesage from i to dest
						ArrayList<Double> tmpMsg = GetVarNodeMsg(oldMessages, i, dest);
						messages.get(i).add(tmpMsg);
						tmpMsg = null;
						//System.out.println(tmpMsg);
						continue;
					}

					//4. sum mode (factor node)
					//ori = Runtime.getRuntime().totalMemory();
					ArrayList<Double> theMsg = new ArrayList<Double>();
					
					theMsg = ConstructMessage(GetMessageFromFactor(oldMessages, i, dest, true), GetMessageFromFactor(oldMessages, i, dest, false));
					//System.out.println(theMsg);
					messages.get(i).add(theMsg);
					theMsg = null;

					//if already time out simply quit
					used = System.currentTimeMillis() - currentTime;
					if(used > t){
						recor.append("BP Time Out: " + used + " used\n");		
						ifTimeout = true;
					}
				}		
			}
			//ori = Runtime.getRuntime().totalMemory();
			maxChange = -1;
			for(int i = 0; i < messages.size(); i ++){
				for(int j = 0; j < messages.get(i).size(); j ++){
					for(int k = 0; k < 2; k ++){
						double dif = Math.abs(oldMessages.get(i).get(j).get(k) - messages.get(i).get(j).get(k));
						if(dif > maxChange){
							maxChange = dif;
						}
						double m = messages.get(i).get(j).get(k);
						oldMessages.get(i).get(j).set(k, m);
					}
				}
			}
			//System.out.println("ite" + iteCounter + ": " + oldMessages.get(0));
			//System.out.println("ite" + iteCounter + " finished!");
			if(maxChange < maxChangeForBP){
				maxChangeForBP = maxChange;
			}
			messages = null;

			//System.out.println("Max Change: " + maxChange);
			//System.out.println("MAP results: " + MapRes + "\n");
			iteCounter ++;
			if(ifPrintIte){
				System.out.println(maxChange);
			}
		}
		
		oldMessages = null;
		recor.append("BP: " + iteCounter + " iterations, " + "Max change is " + maxChangeForBP + "\n");
	}
	
	
	
	BP(ArrayList<Integer> fakeMAP, HashMap<Integer, Boolean> fakeEvidence, FactorGraph theFG, double thre, StringBuilder recor, long maxTime){
		long currentTime = System.currentTimeMillis();
		fg = theFG;
		Random r = new Random();
		//initialize messages
		//long ori = Runtime.getRuntime().totalMemory();
		for(int i = 0 ; i < fg.nodeNum; i ++){
			ArrayList<ArrayList<Double>> col = new ArrayList<>();
			oldMessages.add(col);
			col = null;
			for(int j = 0; j < fg.connectedList.get(i).size(); j ++){
				
				
				double pos = 0.5/*r.nextDouble()*/;
				
				ArrayList<Double> tmpMsg = ConstructMessage(pos, 1 - pos);
				oldMessages.get(i).add(tmpMsg);
				tmpMsg = null;
			}
		}
		
		//keep iterating on all messages
		double maxChange = Double.MAX_VALUE;
		//cancel iteration when max change is small enough
		int iteCounter = 0;
		long lastTime = System.currentTimeMillis();
		while(maxChange > thre){
			///every 20% time consumption try to decide if we need random restarts
			if(System.currentTimeMillis() - lastTime >= maxTime * 0.2){
				if(maxChangeForBP > 0.2){
					System.out.println("Evaluation Reset!");
					//Random r = new Random();
					/*
					for(int i = 0 ; i < fg.nodeNum; i ++){
						for(int j = 0; j < fg.connectedList.get(i).size(); j ++){
							double pos = r.nextDouble();
							oldMessages.get(i).set(j, ConstructMessage(pos, 1.0 - pos));
						}
					}
					*/
				}
				lastTime = System.currentTimeMillis();
			}
			iteCounter ++;
			if(System.currentTimeMillis() - currentTime > maxTime){
				ifConverge = false;
				break;
			}
			ArrayList<ArrayList<ArrayList<Double>>> messages = new ArrayList<>();
			// traverse each node in the fg
			for(int i = 0; i < fg.nodeNum; i ++){
				
				messages.add(new ArrayList<ArrayList<Double>>());
				for(int j = 0; j < fg.connectedList.get(i).size(); j ++){

					int dest = fg.connectedList.get(i).get(j);
					//1. evidence node always sends out constant message
					if(fakeEvidence.containsKey(i)){
						double value = 0.0;
						if(fakeEvidence.get(i)){
							value = 1.0;
						}
						ArrayList<Double> tmpMsg = ConstructMessage(value, 1.0 - value);
						messages.get(i).add(tmpMsg);
						tmpMsg = null;
						continue;
					}
					//2. max node (must be variable node)
					if(fakeMAP.contains(i)){
						//calculate usual mesage from i to dest
						ArrayList<Double> tmpMsg = GetVarNodeMsg(oldMessages, i, dest);
						//calculate current belief of I
						ArrayList<Double> currentBeliefI = GetVarNodeMsg(oldMessages, i, -1);
						//belief of true is larger, abonden second item of the message
						if(currentBeliefI.get(0) > currentBeliefI.get(1)){
							tmpMsg.set(1, 0.0);
							MapRes.put(i, true);
						}
						if(currentBeliefI.get(1) > currentBeliefI.get(0)){
							tmpMsg.set(0, 0.0);
							MapRes.put(i, false);
						}
						messages.get(i).add(tmpMsg);
						tmpMsg = null;
						continue;
					}
					//3. sum node (variable node)
					if(i < fg.nodeNum / 2){
						//calculate usual mesage from i to dest
						ArrayList<Double> tmpMsg = GetVarNodeMsg(oldMessages, i, dest);
						messages.get(i).add(tmpMsg);
						tmpMsg = null;
						continue;
					}
					//4. sum mode (factor node)
					ArrayList<Double> theMsg = ConstructMessage(GetMessageFromFactor(oldMessages, i, dest, true), 
							GetMessageFromFactor(oldMessages, i, dest, false));
					//System.out.println(theMsg);
					messages.get(i).add(theMsg);
					theMsg = null;
				}		
			}
			maxChange = -1;
			for(int i = 0; i < messages.size(); i ++){
				for(int j = 0; j < messages.get(i).size(); j ++){
					for(int k = 0; k < 2; k ++){
						double dif = Math.abs(oldMessages.get(i).get(j).get(k) - messages.get(i).get(j).get(k));
						if(dif > maxChange){
							maxChange = dif;
						}

						oldMessages.get(i).get(j).set(k, messages.get(i).get(j).get(k));
					}
				}
			}
			messages = null;
			if(maxChange < maxChangeForBP){
				maxChangeForBP = maxChange;
			}
			if(ifPrintIte){
				System.out.println(maxChange);
			}
		}
		System.out.println("BP Evaluation: Max change is " + maxChangeForBP);
		//System.out.println(System.currentTimeMillis() - currentTime);
	}
	
	public double GetMarginalEvi(HashMap<Integer, Boolean> theEvi){
		double sum = 0;
		HashMap<Integer, Double> trueMar = new HashMap<>();
		HashMap<Integer, Double> falseMar = new HashMap<>();
		for(Integer e: fg.evidence.keySet()){
			trueMar.put(e, 0.0);
			falseMar.put(e, 0.0);
		}
		for(int i = 0; i < oldMessages.size(); i ++){
			for(int j = 0; j < oldMessages.get(i).size(); j ++){
				int dest = fg.connectedList.get(i).get(j);
				if(theEvi.containsKey(dest)){
					double tmp = Math.log(oldMessages.get(i).get(j).get(0));

					trueMar.put(dest, trueMar.get(dest) + tmp);
					
					tmp = Math.log(oldMessages.get(i).get(j).get(1));
					falseMar.put(dest, falseMar.get(dest) +  tmp);
				}
			}
		}
		for(Integer e: theEvi.keySet()){
			double t = 1.0 / (1.0 + Math.exp(falseMar.get(e) - trueMar.get(e)));
			
			double f = 1 - t;

			if(theEvi.get(e)){
				sum += Math.log(t);
			}
			else{
				sum += Math.log(f);
			}
		}
		trueMar = null;
		falseMar = null;
		oldMessages = null;
		return sum;
	}
	
	//get the meesage from factor i to variable dest
	//also pass in the value of J
	//true then calculate the true item of j
	//false otherwise
	private double GetMessageFromFactor(ArrayList<ArrayList<ArrayList<Double>>> messages, int i, int dest, Boolean valOfJ){
		TreeExp theCPT = fg.CPTs.get(fg.correspondVar(i));
		HashMap<TreeExp, Double> val = new HashMap<>();
		//traverse all nodes connected to the factor
		for(int k = 0; k < fg.connectedList.get(i).size(); k ++){
			int theNode = fg.connectedList.get(i).get(k);
			if(theNode == dest){
				double theVal = 1.0;
				if(!valOfJ){
					theVal = 0.0;
				}
				val.put(fg.variables.get(theNode * 2), theVal);
				val.put(fg.variables.get(theNode * 2 + 1), 1.0 - theVal);
			}
			else{
				ArrayList<Double> theMsg = GetMessage(messages, i, theNode);
				val.put(fg.variables.get(theNode * 2), theMsg.get(0));
				val.put(fg.variables.get(theNode * 2 + 1), theMsg.get(1));
			}
		}
		//long currentMem = Runtime.getRuntime().totalMemory();
		//System.out.println(currentMem);
		double theMsg = theCPT.RealValue(val);
		val = null;

		return theMsg;
	}
	
	//get message from node j to node i
	private ArrayList<Double> GetMessage(ArrayList<ArrayList<ArrayList<Double>>> messages, int i, int j){
		return messages.get(j).get(fg.connectedList.get(j).indexOf(i));
	}
	
	//get variable node messages from i to j
	//if j = -1, returns the current elief of i
	//res = [p(i=true), p(i=false)]
	private ArrayList<Double> GetVarNodeMsg(ArrayList<ArrayList<ArrayList<Double>>> messages, int i, int dest) {
		ArrayList<Double> res = new ArrayList<>();
		double trueProb = 0.0;
		double falseProb = 0.0;
		//get the index of node dest
		int j = fg.connectedList.get(i).indexOf(dest);
		boolean if00 = false;
		boolean if10 = false;
		int index00 = -1;
		int index10 = -1;
		for (int k = 0; k < fg.connectedList.get(i).size(); k++) {
			if(k == j){
				continue;
			}
			int theNode = fg.connectedList.get(i).get(k);
			ArrayList<Double> theMsg = GetMessage(messages, i, theNode);
			//deal with special case for UAI files
			//if the destination node is the factor node of i, and i has no other parents
			//this means i actually is an evidence
			if( theNode == fg.correspondFactor(i) && fg.connectedList.get(theNode).size() == 1){
				res = null;
				return ConstructMessage(theMsg.get(0), theMsg.get(1));
			}
			if(theMsg.get(0) == 0){
				if00 = true;
				index00 = fg.connectedList.get(i).get(k);
			}
			else{
				trueProb += Math.log(theMsg.get(0));
			}
			
			if(theMsg.get(1) == 0){
				if10 = true;
				index10 = fg.connectedList.get(i).get(k);
			}
			else{
				falseProb += Math.log(theMsg.get(1));
			}
			
		}
		double c = 1.0 / (1.0 + Math.exp(falseProb - trueProb));
		if(if00){
			res.add(0.0);
		}
		else{
			res.add(c);
		}
		if(if10){
			res.add(0.0);
		}
		else{
			res.add(1-c);
		}
		if(res.get(0) == 0 && res.get(1) == 0){
			
			try {
				throw new Exception();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}
	
	private ArrayList<Double> ConstructMessage(double a, double b){
		ArrayList<Double> res = new ArrayList<>();
		res.add(a);
		res.add(b);
		return res;
	}
	
}
