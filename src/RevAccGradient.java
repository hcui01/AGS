

import java.lang.reflect.Array;
import java.nio.channels.SeekableByteChannel;



import java.security.AllPermission;
import java.util.*;

import javax.naming.InitialContext;
import javax.xml.soap.SAAJMetaFactory;





public class RevAccGradient{

	
	public RevAccGradient() {
		
	}
	
	public RevAccGradient(long t){
		_timeAllowed = t;
	}
	
	public RevAccGradient(DBN d, long t, StringBuilder r, Random rand){
		theDBN = d;
		_timeAllowed = t;
		recor = r;
		_random = rand;
	}
	/******************************
	 * DBN related
	 ******************************/
	DBN theDBN = null;
	
	public Random _random = null;
	
	/*******************************
	 * SOGBOFA related
	 *******************************/
	boolean ifConverge = false;
	final double convergeNorm = 0.01;
	//if we only do certain nubber of pdates
	//we cannot see the overall trends of ratio action seen/updates
	//so we use this to adjust our estimate
	//so action seen at each step = actionSeen / staCounter * actionEstimateAdj
	final double actionEstimateAdj = 1;
	//const for random policy
	double randomAction = 1.0 / 2;
	//double alpha = 0.00001;
	//convergence threashold
	//this is just init value. it will be adjusted by another par
	double ConvergeT = 0.0000001;
	
	//the portion of time spent on sampling final actions, given the marginal prob of each bit
	final double SampleTime = 0.2;
	//how many times do we wanna update each action bit
	int numOfIte = 200;
	//if trySeeing  > ratioOftrials * # possbile act, then set numOfIte = ratioOftrials * # possbile act
	// meaning that by estimation, we only wanna see a certain ratio of all actions
	//otherwise it is wasting time
	//how many updates to make to do the estimate
	
	Double ratioOfTrials = 0.3;

	long t00 = 0;


	//if time out
	boolean stopAlgorithm = false;
	//int counter;
	//if use multi layer vars
	final boolean ifMultiLayer = false;
	final boolean ifAllVar = true;
	//min number of varibales
	int baseVarNum = 200;       
	
	//base number of dived the alpha legal region
	int AlphaTime = 10;
	//if record the tree
	final boolean ifRecord = false;
	//oldQ * this = ConvergeT
	final double RelativeErr = 0.01;

	//max time of iteratively shrink alpha
	final int MaxShrink = 5;
	//when we go beyond legal region, do we project back by
	//decreasing the same value for all vars or by times a factor
	final boolean ifProjectwtTime = false;
	
	final boolean ifPrint = false;
	final boolean ifPrintEst = false;
	final boolean ifPrintBit = false;
	boolean ifPrintInitial = false;
	
	int SearchDepth = -1;
	
	final boolean ifPrintProb = false;
	//print out the starting and ending points of each random restart
	final boolean ifPrintGrid = false;
	
	//specially for elevator
	boolean ifOldEle = false;
	boolean ifNewEle = false;
	
	final boolean ifDefaultNoop = true;
	
	// if we already go over this depth, we use calculation rather than real estimate
	final int MaxEstimateDepth = 10;
	
	//if use forward accumulation or reverse accumulation
	final boolean ifReverseAccu = true;
	
	final double fixAlpha = -1;
	
	final boolean ifRecordRoutine = true;
	
	final boolean ifTopoSort = true;
	
	/*
	int bit1Count = 0;
	int bit2Count = 0;
	int bit3Count = 0;
	int bit4Count = 0;
	int bitNoCount = 0;
	*/
	
	int maxDepth = 0;
	
	//stats
	double roundRandom = 0;
	double roundUpdates = 0;
	double roundSeen = 0;
	
	public long _timeAllowed = 10000;
	
	
	ArrayList<Double> bestNumAct = new ArrayList<Double>();
	double highestScore = -Double.MAX_VALUE;
	HashMap<ArrayList<Double>, Double> routine = new HashMap<ArrayList<Double>, Double>();
	public StringBuilder recor = null;
	public ArrayList<Integer> alreadyTrave = new ArrayList<>();
	//ArrayList<ArrayList<Double>> triedAct = new ArrayList<ArrayList<Double>>();
	
	//a table transfers from actions to numbers
	//HashMap<Integer, HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Integer>>> trans2Num = new HashMap<Integer, HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Integer>>>();
	
	//build the reward expectation function
	// with only the root level actions as variable
	// the other levels actions are treated as constant

	
	private TreeExp TrueTree(HashMap<Integer, TreeExp> var, int theNode){
		return var.get(TrueID(theNode));
	}
	
	private TreeExp FalseTree(HashMap<Integer, TreeExp> var, int theNode){
		return var.get(FalseID(theNode));
	}
	
	private int TrueID(int theNode){
		return theNode * 2;
	}
	
	private int FalseID(int theNode){
		return theNode * 2 + 1;
	}
	
	private void RecordMar(int index, TreeExp trueVal) throws Exception{
		int thID = TrueID(index);
		theDBN.variables.get(TrueID(index)).ReplaceTree(trueVal);
		TreeExp falseTree = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueVal);
		falseTree.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
		theDBN.variables.get(FalseID(index)).ReplaceTree(falseTree);
		alreadyTrave.add(index);
	}
	
	private TreeExp CopyLeafNode(TreeExp old) {
		if(!old.expType.equals("LEA") || old.IsVar()) {
			try {
				throw new Exception("CopyLeafNode has to copy leaf/number node!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
		}
		TreeExp newNode = new TreeExp(old.term.coefficient, null);
		return newNode;
	}
	
	private void RecordMar(int index) throws Exception{
		TreeExp t0True = theDBN.CPTs.get(index);
		int trueID = TrueID(index);
		int falseID = FalseID(index);
		theDBN.variables.get(trueID).ToNumberNode(1.0);
		theDBN.variables.get(falseID).ToNumberNode(0.0);
		t0True.Prunning(new HashMap<TreeExp, Boolean>(), theDBN.variables);
		TreeExp falseTree = TreeExp.BuildNewTreeExp(1.0, null).MINUS(t0True);
		falseTree.Prunning(new HashMap<TreeExp, Boolean>(), theDBN.variables);
		TreeExp oldTree = theDBN.variables.get(trueID);
		
		//note that the t0True could be directly just variables.get(falseID)
		//when replacing oldTree with t0True, father could be added to the false node
		// in this case, to distinguish false tree and true tree
		// use a copy of the false node instead of itself
		TreeExp t0TrueCopy = null;
		if(t0True == theDBN.variables.get(falseID)){
			t0TrueCopy = CopyLeafNode(t0True);
		}
		else {
			t0TrueCopy = t0True;
		}
		if(t0TrueCopy != oldTree){
			theDBN.variables.get(trueID).ReplaceTree(t0TrueCopy);
		}
		
		//same copy reason
		TreeExp falseTreeCopy = null;
		if(falseTree == theDBN.variables.get(trueID)){
			falseTreeCopy = CopyLeafNode(falseTree);
		}
		else {
			falseTreeCopy = falseTree;
		}
		oldTree = theDBN.variables.get(falseID);
		if(falseTreeCopy != oldTree){
			oldTree.ReplaceTree(falseTreeCopy);
		}
		alreadyTrave.add(index);
	}
	
	TreeExp BuildF() throws Exception{
		
		// marginal for each variable
		HashMap<TreeExp, TreeExp> values = new HashMap<>();
		HashMap<Integer, TreeExp> varCollector = new HashMap<Integer, TreeExp>();
		//cal marginals for parentless nodes
		//cal marginal for s0
		/*
		TreeExp s0True = theDBN.CPTs.get(s0).Copy(new HashMap<TreeExp, TreeExp>());
		s0True.Plugin(new ArrayList<TreeExp>(), s0, true);
		
		s0True.Prunning(new ArrayList<TreeExp>(), varCollector);
		*/
		
		//TreeExp s0True = 
		//RecordMar(values, s0, s0True);
		
		//cal marginal for t0
		
		
		
		
		/*
		TreeExp t0True = theDBN.CPTs.get(t0).Copy(new HashMap<TreeExp, TreeExp>());
		t0True.Plugin(new ArrayList<TreeExp>(), t0, true);
		t0True.Prunning(new ArrayList<TreeExp>(), varCollector);
		RecordMar(values, t0, t0True);
		*/
		
		
		
		//record marginal of dnodes as themselves
		/*
		for(int j = 0; j < theDBN.MAP.size(); j ++){
			int dNode = theDBN.MAP.get(j);
			RecordMar(values, dNode, new TreeExp(j, null));
		}
		*/
		for(int j = 0; j < theDBN.MAP.size(); j ++){
			int dNode = theDBN.MAP.get(j);
			RecordMar(dNode, new TreeExp(j, null));
		}
		
		
		//traverse rest of all the nodes
		for(int i = 0; i < theDBN.nodeNum - 1; i ++){

			//skip those already has a value
			if(alreadyTrave.contains(i)){
				continue;
			}
			//System.out.println(i);

			RecordMar(i);
			//System.out.println(i);

			/*
			//get CPT
			TreeExp theCPT = theDBN.CPTs.get(i));
			
			//cal true value of node i
			//firt plugin true to node I
			theCPT.Plugin(new ArrayList<TreeExp>(), i, true);
			theCPT.Prunning(new ArrayList<TreeExp>(), varCollector);
			//then sum over all its parents
			for(int j = 0; j < theDBN.inList.get(i).size(); j ++){
				int theParent = theDBN.inList.get(i).get(j);
				//get true value tree and false value tree of the node
				TreeExp trueTree = TrueTree(theDBN.variables, theParent);
				theCPT.Plugin(new ArrayList<TreeExp>(), theParent, values.get(trueTree));
				theCPT.Prunning(new ArrayList<TreeExp>(), varCollector);
			}

			values.put(TrueTree(theDBN.variables, i), theCPT);
			*/
			//System.out.println(i);
		}  
		
		theDBN.variables.get(TrueID(theDBN.vIndex)).ToNumberNode(1.0);
		theDBN.variables.get(FalseID(theDBN.vIndex)).ToNumberNode(0.0);
		theDBN.CPTs.get(theDBN.vIndex).Prunning(new HashMap<TreeExp, Boolean>(), theDBN.variables);
		//return values.get(TrueTree(theDBN.variables, theDBN.vIndex));
		return theDBN.CPTs.get(theDBN.vIndex).TIMES(TreeExp.BuildNewTreeExp(Math.exp(324.0), null));
	}
	

	
	TreeExp BuildF(long N, long W, long T) throws Exception{
		
		TreeExp F = new TreeExp();
		
		return theDBN.CPTs.get(theDBN.vIndex).TIMES(TreeExp.BuildNewTreeExp(Math.exp(324.0), null));
	}
	
	//arbitarily search over legal region of alpha
	//use the best step length
	public double FndAlpha(TreeExp F, ArrayList<Double> actionProb, ArrayList<Double> delta) {
		double maxAlpha = Double.MAX_VALUE;
		//we allow actionprob to go beyond 1
		//so first find the max prob and then acrrordingly find the space
		double maxProb = -1;
		for(double a: actionProb){
			if(a > maxProb){
				maxProb = a;
			}
		}
		maxProb += 1;
		//traverse each bit to shrink maxalpha
		for(int i = 0; i < actionProb.size(); i ++){
			double possibleAlpha = -1;
			if(delta.get(i) > 0){
				possibleAlpha = (maxProb-actionProb.get(i)) / Math.abs(delta.get(i));
			}
			if(delta.get(i) < 0){
				possibleAlpha = (actionProb.get(i) - (-1)) / Math.abs(delta.get(i));
			}
			if(possibleAlpha != -1 && possibleAlpha < maxAlpha){
				maxAlpha = possibleAlpha;
			}
		}
		//if we do concurrency projection then we need to again shrink the alpha by constraint the sum of prob be no bigger than
		//concurrency
		//System.out.println("max alpha is: " + maxAlpha);
		//now try alpha from 0 to maxAlpha
		double bestAlpha = 0;
		double bestQ = Double.NEGATIVE_INFINITY;
		ArrayList<Double> tempActProb = new ArrayList<Double>();
		for(int i = 0; i < actionProb.size(); i ++){
			tempActProb.add(0.0);
		}
		ArrayList<Double> bestActProb = new ArrayList<Double>();
		for(int i = 0; i < actionProb.size(); i ++){
			bestActProb.add(0.0);
		}
		
		// try to find the alpha with highest Q
		double realBest = -1;
		//double realNeeded = -1;
		
		// this is a loop to find smallest alpha because too large alpha could
		// be a problem
		// if we find in one iteration alpha is chosen to be the smallest among
		// possible then we extend another "alphatime"
		// between 0 and the smallest
		// maxAlpha = 0.2;
		int shrinkCounter = 0;
		double realNorm = 0;
		while (true) {
			if (fixAlpha == -1)
				bestAlpha = 0;
			else {
				bestAlpha = fixAlpha;
				AlphaTime = 1;
			}

			for (int i = 1; i <= AlphaTime; i++) {
				
				if (fixAlpha == -1){
					bestAlpha += maxAlpha / AlphaTime;
				}
				// System.out.println(bestAlpha);
				// update temp actprob
				
				for (int j = 0; j < actionProb.size(); j++) {
					double d = delta.get(j);
					double update = bestAlpha * d;
					double now = actionProb.get(j);
					if(now + update < 0){
						update = -now;
					}
					if(now + update > 1){
						update = 1 - now;
					}
					//norm += update * update;
					double newVal = now + update;

					tempActProb.set(j, newVal);
				}
				

				// update bestQ
				HashMap<TreeExp, Double> valRec = new HashMap<TreeExp, Double>();
				try {

					double theQ = F.RealValue(tempActProb, valRec);
					if (theQ > bestQ) {
						bestQ = theQ;
						// update actionProb
						for (int j = 0; j < actionProb.size(); j++) {
							bestActProb.set(j, tempActProb.get(j));
						}
						realBest = bestAlpha;
						double norm = 0;
						for(int j = 0; j < actionProb.size(); j ++){
							double diff = tempActProb.get(j) - actionProb.get(j);
							norm += diff * diff;
						}
						norm = Math.sqrt(norm / actionProb.size());
						realNorm = norm;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// System.out.println("BestAlpha is :" + realBest);

			if (fixAlpha == -1 && realBest == maxAlpha / AlphaTime) {
				maxAlpha /= AlphaTime;
				shrinkCounter++;
				
				if (shrinkCounter > MaxShrink) {
					break;
				}
				// System.out.println("Alpha is too large, will try alpha between 0 and "
				// + maxAlpha );
			} else {
				break;
			}
		}
		
		if(convergeNorm != -1 && realNorm <= convergeNorm){
			ifConverge = true;
		}

		for (int j = 0; j < actionProb.size(); j++) {
			actionProb.set(j, bestActProb.get(j));
		}
		return bestQ;
	}
	
	
	public ArrayList<Double> UpdateAllwtProj(TreeExp F) throws Exception{

		ArrayList<Double> actionProb = new ArrayList<Double>();
		int MAPSize = theDBN.MAP.size();
		for(int i = 0; i < MAPSize; i ++){
			actionProb.add(0.0);
		}
		
		//ArrayList<TExp> visited = new ArrayList<TExp>();
		//int b = F.Size(visited );
		//iteration counter
		int randomRestart = 0;
		roundRandom = 0;
		roundUpdates = 0;
		roundSeen = 0;
		
		//Record the best actionProb that gets the highest Q value in F tree
		ArrayList<Double> bestActionProb = new ArrayList<Double>();
		for(int i = 0; i < MAPSize; i ++){
			bestActionProb.add(0.0);
		}
		double bestQ = Double.NEGATIVE_INFINITY;
		ArrayList<Double> completeBest = new ArrayList<Double>();
		for(int i = 0; i < MAPSize; i ++){
			completeBest.add(0.0);
		}

		//only used for revAcc
		ArrayList<TreeExp> que = new ArrayList<TreeExp>();
		
		//long t000 = System.currentTimeMillis();
		long startTopo = System.currentTimeMillis();
		if(ifReverseAccu){
			que = F.TopologQueue(ifTopoSort);
			recor.append("Q graph size: " + F.numOfNonLeaf + "\n");
		}
		//recor.append("Topological sort uses: " + (System.currentTimeMillis() - startTopo) + "\n");
		
		
		boolean ifFirstTime = true;
		while(!stopAlgorithm){
			if(System.currentTimeMillis() - t00 > _timeAllowed){
				for(int i = 0; i < actionProb.size(); i ++){
					actionProb.set(i, 1.0);
				}	
				if(ifRecordRoutine){
					UpdateRoutine(F, actionProb, true);
				}
				stopAlgorithm = true;
				//recor.append("SOG Time used: " + (System.currentTimeMillis() - t00) + "\n");
				break;
			}
			double updatedasCounter = 0;
			//a record of how many times we keep decreasing Q
			//sometims because alpha is too large the Q keeps going down
			//if that happens for enough many times we simply force it to cenverge
			//get concrete actions
			//ifPrintInitial = true;
			if(ifFirstTime){
				for(int i = 0; i < actionProb.size(); i ++){
					actionProb.set(i, 1.0);
				}
				ifFirstTime = false;
			}
			else{
				for(int i = 0; i < actionProb.size(); i ++){
					actionProb.set(i, _random.nextDouble());
				}
			}
									
			
			if(ifPrintGrid){
				ArrayList<Integer> alreadyIn = new ArrayList<Integer>();
				for(int i = 1; i <= MAPSize; i ++){
					double maxVal = -1;
					int maxJ = -1;
					for(int j = 0; j < actionProb.size(); j ++){
						double val = actionProb.get(j);
						if(alreadyIn.contains(j)){
							continue;
						}
						if(val > maxVal){
							maxVal = val;
							maxJ = j;
						}
					}
					if(maxVal < randomAction){
						break;
					}
					alreadyIn.add(maxJ);
				}
				System.out.print("\n\nSta: ");
				for(int j: alreadyIn){
					System.out.print(j + " ");
				}
			}
			//iteratively update action probs until converge
			ifConverge = false;
			//a value table to record the realvalue of trees
			//only used in one iteration
			//need to be clear after use
			HashMap<TreeExp, Double> valRec = new HashMap<TreeExp, Double>();
			HashMap<TreeExp, Double> gradRec = new HashMap<TreeExp, Double>();
			//initialize oldQ to be realvalue calculated with initial action prob
			long startOldQ = System.currentTimeMillis();
			double oldQ = F.RealValue(actionProb, valRec);
			//recor.append("Start old Q uses: " + (System.currentTimeMillis() - startOldQ) + "\n");
			if(ifRecordRoutine){
				UpdateRoutine(F, actionProb, true);
			}
			//this is used to judge whether Q has been changed
			double initialQ = oldQ;
			if(ifPrintInitial){
				System.out.println(actionProb + " " + initialQ);
			}
			if(ifPrint){
				System.out.println("Q is initlaized to: " + oldQ);
			}
			
			//ba sed on this Q value, setup threshold
			if(oldQ != 0){
				ConvergeT = Math.abs(oldQ * RelativeErr);
			}
			else{
				ConvergeT = RelativeErr;
			}
			//update bestQ and action
			if(oldQ > bestQ){
				bestQ = oldQ;
				for(int a = 0; a < MAPSize; a ++){
					bestActionProb.set(a, actionProb.get(a));
				}
				for(int a = 0; a < actionProb.size(); a ++){
					completeBest.set(a, actionProb.get(a));
				}
			}
			
			//dead bit record
			//if during this random restart, certain bits never change, it means that Q is not related to it
			//set it to be 0
			//only for top level 
			ArrayList<Boolean> ifthisBitChange = new ArrayList<Boolean>();
			for(int a = 0; a < MAPSize; a ++){
				ifthisBitChange.add(false);
			}
			if(ifPrintBit){
				System.out.println();
				for(int a = 0; a < actionProb.size(); a++){
					System.out.println("a for " + "v" + a + " " + actionProb.get(a));
				}
				System.out.println();
			}
			//initialize newQ
			double newQ = 0; // this will be recalculated later
			
			
			
			
			while(!ifConverge && !stopAlgorithm){
				if(System.currentTimeMillis() - t00 > _timeAllowed){
					for(int i = 0; i < actionProb.size(); i ++){
						actionProb.set(i, 1.0);
					}	
					if(ifRecordRoutine){
						UpdateRoutine(F, actionProb, true);
					}
					stopAlgorithm = true;
					//recor.append("SOG Time used: " + (System.currentTimeMillis() - t00) + "\n");
					break;
				}

				//calculate delta
				ArrayList<Double> delta = new ArrayList<Double>();
				//System.out.println("");
				//this step gets delta for each bit
				
				// ArrayList<TreeExp> visited = new ArrayList<TreeExp>();
				// int queSize = F.Size(visited);
				long startRev = System.currentTimeMillis();

				F.RevAccGradient(theDBN.MAP.size(), ifTopoSort, que, delta, actionProb);
				//System.out.println(delta);
				if (System.currentTimeMillis() - t00 > _timeAllowed) {
					for (int i = 0; i < actionProb.size(); i++) {
						actionProb.set(i, 1.0);
					}
					if (ifRecordRoutine) {
						UpdateRoutine(F, actionProb, true);
					}
					stopAlgorithm = true;
					recor.append("SOG Time used: " + (System.currentTimeMillis() - t00) + "\n");
					break;
				}
				// recor.append("Get Gradient uses: " + (System.currentTimeMillis() - startRev)
				// + "\n");
				// System.out.println(que.get(0));
				// System.out.println(gTree.toString());
				if (ifPrintBit) {
					for (int i = 0; i < actionProb.size(); i++) {
						System.out.println("d for " + "v" + i + " " + delta.get(i));
					}
				}
				for (int i = 0; i < delta.size(); i++) {
					double d = delta.get(i);
					if (d != 0) {
						ifthisBitChange.set(i, true);
					}
				}
				// System.out.println(System.currentTimeMillis() - t000);
					
				
				
				
				updatedasCounter ++;
				//this step updates prob and return the Q
				long startFF = System.currentTimeMillis();
				newQ = FndAlpha(F, actionProb, delta);
				if(System.currentTimeMillis() - t00 > _timeAllowed){
					for(int i = 0; i < actionProb.size(); i ++){
						actionProb.set(i, 1.0);
					}	
					if(ifRecordRoutine){
						UpdateRoutine(F, actionProb, true);
					}
					stopAlgorithm = true;
					recor.append("SOG Time used: " + (System.currentTimeMillis() - t00) + "\n");
					break;
				}
				//recor.append("Find alpha uses: " + (System.currentTimeMillis() - startFF) + "\n");
				//System.out.println(routine.size());
				
				long startRoutine = System.currentTimeMillis();
				if(ifRecordRoutine){
					UpdateRoutine(F, actionProb, true);
				}
				//recor.append("Record routine uses: " + (System.currentTimeMillis() - startRoutine) + "\n");
				//System.out.println(routine.size());
				/*
				if(currentRound == 1){
				for(int i = 0; i < actionProb.size(); i ++){
					actionProb.set(i, 0.0);
				}
				actionProb.set(3, 1.0);
				}
				*/
				if(ifPrintBit){
					for(int a = 0; a < actionProb.size(); a++){
						System.out.println("a for " + "v" + a + " " + actionProb.get(a));
					}
					//System.out.println();
				}
				
				
				
				//now alphas are changed so we need to clear the value record in the tree
				valRec.clear();
				
				
				if(ifPrint){
					System.out.println("oldQ: " + oldQ + "\n");
					System.out.println("newQ: " + newQ + "\n");
				}
				
				
				if(convergeNorm == -1 && Math.abs(newQ - oldQ) < ConvergeT){
					ifConverge = true;
				}
				
				oldQ = newQ;
				//we don't need to clear valrec again
				//because the value when calculating newQ can be reused in next iteration
				if(System.currentTimeMillis() - t00 > _timeAllowed){
					stopAlgorithm = true;
					recor.append("SOG Time used: " + (System.currentTimeMillis() - t00) + "\n");
					break;
				}
			}
			if(ifPrintGrid){
				ArrayList<Integer> alreadyIn = new ArrayList<Integer>();
				for(int i = 1; i <= MAPSize; i ++){
					double maxVal = -1;
					int maxJ = -1;
					for(int j = 0; j < actionProb.size(); j ++){
						double val = actionProb.get(j);
						if(alreadyIn.contains(j)){
							continue;
						}
						if(val > maxVal){
							maxVal = val;
							maxJ = j;
						}
					}
					if(maxVal < randomAction){
						break;
					}
					alreadyIn.add(maxJ);
				}
				System.out.print("\nEnd: ");
				for(int j: alreadyIn){
					System.out.print(j + " ");
				}
				
			}
			//converged; continue to next random restart
			
			if(newQ != initialQ){
				roundRandom ++;
				roundUpdates += updatedasCounter;

				
				
				//randomRestart ++;
			}
			if(ifPrint){
				if(ifConverge){
					System.out.println("RR: " + randomRestart + "converged!");
				}
				else{
					System.out.println("RR: " + randomRestart + "forced to stop because running out of time.");
				}
			}
			//Get the Q value for this convergence
			if(newQ > bestQ){
				if(ifPrint){
					System.out.println("Previous best is: " + bestQ + " and now the Q is: " + newQ);
				}
				
				bestQ = newQ;
				for(int a = 0; a < MAPSize; a ++){
					bestActionProb.set(a, actionProb.get(a));
				}
				for(int a = 0; a < actionProb.size(); a ++){
					completeBest.set(a, actionProb.get(a));
				}
				//if dead bit set to 0
				 
				if(ifDefaultNoop){
					for (int a = 0; a < MAPSize; a++) {
						if (!ifthisBitChange.get(a)) {
							bestActionProb.set(a, 0.0);
						}
					}
				}				
			}
			else{
				if(ifPrint){
					System.out.println("Failed to update Q. Previous best is: " + bestQ + " and now the Q is: " + newQ); 
				}
			}
		}
		if(ifPrintGrid){
			System.out.println("In total: " + randomRestart);
		}
		//record how many random restart have been done
		String countingStr = new String();
		countingStr += "Number of Random Restart: " + roundRandom + "\n";
		countingStr += "Number of Updates: " + roundUpdates + "\n";
		countingStr += "Number of Actions Seen: " + roundSeen + "\n";
		
		recor.append(countingStr);
		
		//printout the action probs
		if(ifPrintBit){
			recor.append("final action prob: ");
			for(double a: bestActionProb){
				recor.append(a + "\n");
			}
		}
		//recor.append("best: " + bestQ + "\n");
		que = null;
		//System.out.println(completeBest);
		
		return bestActionProb; 
	}
	
	
	
	public ArrayList<Double> SampleNumAct(ArrayList<Double> varVal){
		ArrayList<Double> numAct = new ArrayList<Double>();
		for(double theVal: varVal) {
			if(theVal > 0.5) {
				numAct.add(1.0);
			}
			else {
				numAct.add(0.0);
			}
		}

		return numAct;
	}
	
	public void UpdateRoutine(TreeExp F, ArrayList<Double> varVal, boolean ifStatistics) throws Exception{
		//must test the moment
		ArrayList<Double> closestAct = SampleNumAct(varVal);
		if(!routine.containsKey(closestAct)){
			
			HashMap<TreeExp, Double> valRec = new HashMap<TreeExp, Double>();
			double val = F.RealValue(closestAct, valRec);
			//System.out.println("Update:" + val);
			//System.out.println("Actions: " + closestAct);
			//System.out.println("Ori Score:" + F.RealValue(varVal, new HashMap<TreeExp, Double>()));
			//System.out.println("Actions:" + varVal);
			
			routine.put(closestAct, val);
			if(ifStatistics){
				roundSeen ++;
				if(val > highestScore){
					//System.out.println("Update:" + val);
					//System.out.println("Actions: " + closestAct);
					highestScore = val;
					bestNumAct = closestAct;
				}
			}
		}
		
		//maybe test sampling
		//based on varVal, sample concrete action
		/*
		Random r = new Random();
		for(int i = 0; i < 5; i ++) {
			//ArrayList<Double> closestAct = SampleNumAct(varVal);
			closestAct = new ArrayList<>();
			for(int k = 0; k < varVal.size(); k ++) {
				if(r.nextDouble() < varVal.get(k)) {
					closestAct.add(1.0);
				}
				else {
					closestAct.add(0.0);
				}
			}
			if(!routine.containsKey(closestAct)){
				
				HashMap<TreeExp, Double> valRec = new HashMap<TreeExp, Double>();
				double val = F.RealValue(closestAct, valRec);
				//System.out.println("Update:" + val);
				//System.out.println("Actions: " + closestAct);
				//System.out.println("Ori Score:" + F.RealValue(varVal, new HashMap<TreeExp, Double>()));
				//System.out.println("Actions:" + varVal);
				
				routine.put(closestAct, val);
				if(ifStatistics){
					roundSeen ++;
					if(val > highestScore){
						//System.out.println("Update:" + val);
						//System.out.println("Actions: " + closestAct);
						highestScore = val;
						bestNumAct = closestAct;
					}
				}
			}
		}
		*/
		
	}
	
	//sample action from largest to smallest; build actions incrementally
	public ArrayList<Double> SampleAction() throws Exception{

		return bestNumAct;

	}
	
	

	//sample action for each bit; until get a legal action or time out (returns noop at that case)
	//last parameter: if always add the bit with largest prob to action list so long as it's legal

	
	//final get action algorithm
	public ArrayList<Double> getActions() throws Exception {
		//System.out.println(s);
		
		//initialization
		ifConverge = false;

		//const for random policy
		randomAction = 1.0 / 2;
		//double alpha = 0.00001;
		//convergence threashold
		//this is just init value. it will be adjusted by another par
		ConvergeT = 0.0000001;
		
		
		//how many times do we wanna update each action bit
		///numOfIte = 200;
		//if trySeeing  > ratioOftrials * # possbile act, then set numOfIte = ratioOftrials * # possbile act
		// meaning that by estimation, we only wanna see a certain ratio of all actions
		//otherwise it is wasting time
		//how many updates to make to do the estimate
		
		//ratioOfTrials = 0.3;

		//try seeing certain number of actions
		//trySeeing = 5;
		//the depth of variables that we reach
		//this is dynamic
		//t0 = 0;
		//final int iterationTime = 10;
		//countActBits = 0;
		//if time out
		stopAlgorithm = false;

		//min number of varibales
		//baseVarNum = 200;       
		
		//base number of dived the alpha legal region
		//AlphaTime = 10;

		//ifPrintInitial = false;
		
		//SearchDepth = -1;

		
		//specially for elevator
		ifOldEle = false;
		ifNewEle = false;

		
		/*
		int bit1Count = 0;
		int bit2Count = 0;
		int bit3Count = 0;
		int bit4Count = 0;
		int bitNoCount = 0;
		*/
		
		//maxDepth = 0;
		
		//stats
		roundRandom = 0;
		roundUpdates = 0;
		roundSeen = 0;
		
		//update action probs
		highestScore = -Double.MAX_VALUE;
		routine = new HashMap<ArrayList<Double>, Double>();

		
		//initialize action prob
		ArrayList<Double> actionProb = null;
		
		
		t00 = System.currentTimeMillis();
		
		TreeExp F = BuildF();
		//System.out.println(F);
		
		actionProb = UpdateAllwtProj(F);
		/*
		Scanner sc = new Scanner(System.in);
		for(int i = 0; i < 14; i ++){
			double arbitary = sc.nextFloat();
			bestNumAct.set(i, arbitary);
		}
		*/
		//get final action
		
		/*
		System.out.println("\nFinally we are using the action probs: ");
		for(PVAR_NAME p: s._alActionNames){
			for(ArrayList<LCONST> t: s.generateAtoms(p)){
				System.out.println(p sd robWithName.get(p).get(t));
			}
		}
		*/
		if(ifRecordRoutine){
			//recor.append("Routine records: ");
			//recor.append(bestNumAct + "\n");
			//recor.append("Final Score: " + highestScore + "\n");
		}
		ArrayList<Double> actions = new ArrayList<>();
		actions = SampleAction();
		//System.out.println("Q graph size: " + F.size(new ArrayList<TreeExp>()));
		return actions;
	}
	
	private void RecordMarNew(int index) throws Exception{
		TreeExp t0True = theDBN.CPTs.get(index);
		int trueID = TrueID(index);
		int falseID = FalseID(index);
		HashMap<Integer, Double> valueTable = new HashMap<>();
		valueTable.put(trueID, 1.0);
		valueTable.put(falseID, 0.0);
		TreeExp simplifiedTrueMar = Simplify(t0True, valueTable);
		TreeExp simplifiedFalseMar = TreeExp.BuildNewTreeExp(1.0, null).MINUS(simplifiedTrueMar);
		theDBN.variables.get(trueID).ReplaceTree(simplifiedTrueMar);
		theDBN.variables.put(trueID, simplifiedTrueMar);
		theDBN.variables.get(falseID).ReplaceTree(simplifiedFalseMar);
		theDBN.variables.put(falseID, simplifiedFalseMar);
		alreadyTrave.add(index);
	}
	
	private static TreeExp Simplify(TreeExp original, HashMap<Integer, Double> valueTable) {
		
		if(original.IsVar()) {
			int theVar = original.term.var;
			if(valueTable.containsKey(theVar)) {
				return TreeExp.BuildNewTreeExp(valueTable.get(theVar), null);
			}
			else {
				return original;
			}
		}
		if(original.IsNum()) {
			return original;
		}
		
		//if all sub trees are numbers
		//directly get the real value of the original
		// and return a pure number tree
		boolean ifSubAllNum = true;
		for(int i = 0; i < original.subExp.size(); i ++) {
			TreeExp theSub = original.subExp.get(i);
			original.subExp.set(i, Simplify(theSub, valueTable));
			theSub = null;
			if(!original.subExp.get(i).IsNum()) {
				ifSubAllNum = false;
			}
		}
		if(ifSubAllNum) {
			return TreeExp.BuildNewTreeExp(original.RealValue(new HashMap<TreeExp, Double>()), null);
		}
		
		//if sub trees contan 0 and this is product
		//simply return 0
		//cut all 1 sub trees
		if(original.expType.equals("PRO")) {
			for(int i = 0; i < original.subExp.size(); i ++) {
				TreeExp theSub = original.subExp.get(i);
				if(theSub.Is0()) {
					return TreeExp.BuildNewTreeExp(0.0, null);
				}
			}
		}
		
		return original;
	}

}

