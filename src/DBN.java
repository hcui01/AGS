import java.awt.font.NumericShaper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.print.attribute.standard.MediaSize.Other;


public class DBN {
	public Random rand = null;
	//adjacency matrix
	public int[][] adjacencyMatrix = null;
	public HashMap<Integer, Boolean> evidence = new HashMap<>();
	public ArrayList<Integer> MAP = new ArrayList<>();
	public int[] numberIn = null;
	public int MAX_COMB = 3;
	public ArrayList<ArrayList<Integer>> outList = new ArrayList<>();
	public ArrayList<ArrayList<Integer>> inList = new ArrayList<>();
	public ArrayList<TreeExp> CPTs = new ArrayList<>();
	public int nodeNum = 0;

	public HashMap<Integer, TreeExp> variables = new HashMap<>();
	//for reduction results
	public int sIndex = 0;
	public int tIndex = 0;
	public int dIndex = 0;
	public int vIndex = 0;

	public DBN(Random r, int numberNodes, int numEvi, int numMAP) {
		rand = r;
		//initialize number of nodes 
		nodeNum = numberNodes;
		variables = new HashMap<>();
		//initialize outlist and inlist
		for(int i = 0; i < numberNodes; i ++){
			outList.add(new ArrayList<Integer>());
			inList.add(new ArrayList<Integer>());
		}
		//adjacency matrix
		adjacencyMatrix = new int[numberNodes][numberNodes];
		evidence = new HashMap<Integer, Boolean>();
		MAP = new ArrayList<Integer>();
		int[] countIn = new int[numberNodes];
		
		double p = 2 * Math.log(numberNodes) / numberNodes;
		for (int i = 0; i < numberNodes; i++) {
			for (int j = i + 1; j < numberNodes; j++) {
				double dice = r.nextDouble();
				// connect i and j
				if (dice <= p) {
					adjacencyMatrix[i][j] = 1;
					outList.get(i).add(j);
					inList.get(j).add(i);
					countIn[j]++;
				}
			}
		}
		
		
		/*******manual module*******
		adjacencyMatrix[0][1] = 1;
		adjacencyMatrix[0][2] = 1;
		adjacencyMatrix[1][2] = 1;
		adjacencyMatrix[2][3] = 1;
		outList.get(0).add(1);
		outList.get(0).add(2);
		outList.get(1).add(2);
		outList.get(2).add(3);
		inList.get(1).add(0);
		inList.get(2).add(0);
		inList.get(2).add(1);
		inList.get(3).add(2);
		countIn[0] = 0;
		countIn[1] = 1;
		countIn[2] = 2;
		countIn[3] = 1;
		****************************/
		
		
		
		//*************************
		numberIn = countIn;
		
		
		//generate map and evi
		ArrayList<Integer> index = new ArrayList<>();
		for(int i = 0; i < numberNodes; i ++){
			index.add(i);
		}
		Collections.shuffle(index, rand);
		//generate evidence nodes
		evidence = new HashMap<>();
		for(int i = 0; i < numEvi; i ++){
			double dice = rand.nextDouble();
			Boolean value = false;
			if(dice > 0.5){
				value = true;
			}
			evidence.put(index.get(i), value);
		}
		//generate MAP
		for(int i = numEvi; i < numMAP + numEvi && i < index.size(); i ++){
			MAP.add(index.get(i));
		}
		
		/*******manual module*******
		evidence.put(1, false);
		MAP.add(2);
		****************************/
		
		// Generate CPTs
		for (int i = 0; i < numberNodes; i++) {
			// generate CPT
			int numberComb = rand.nextInt(MAX_COMB) + 1;
			int maxNumberComb = 1;
			for(int j = 0; j < countIn[i]; j ++){
				maxNumberComb *= 2;
			}
			if(countIn[i] == 0){
				numberComb = 0;
			}
			else{
				if(numberComb > maxNumberComb){
					numberComb = maxNumberComb;
				}
			}
			TreeExp theCPT = GenerateCPT(i, numberComb);
			CPTs.add(theCPT);
			theCPT = null;
		}
	}
	
	//this function reads UAI files and generates DBN
	//if ifBlur is true, all deterministic nodes would becomes stochastic
	public DBN(Random r, String fileDir, String eviFileDir, String queFileDir, double ratioMAP, int maxMAP, boolean ifBlur){
		//open the file
		String line = null;
		rand = r;
		
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(fileDir);
			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			//reading each line of the file
			int lineCounter = -1;
			int numVar = -1;
			int numberFact = -1;
			int factSize = 0;
			int combCounter = 0;
			int CPTIndex = -1;
			int factSizeBegin = 0;
			int realFactSize = 0;
			ArrayList<ArrayList<ArrayList<Integer>>> parents = new ArrayList<>();
			ArrayList<Double> theProb = new ArrayList<>();
			while ((line = bufferedReader.readLine()) != null) {
				
				if(line.length() == 0){
					continue;
				}
				lineCounter ++;
				//1. graph type
				if(lineCounter == 0){
					continue;
				}
				//2. number of variables
				if(lineCounter == 1){
					numVar = Integer.valueOf(line);
					nodeNum = numVar;
					numberIn = new int[numVar];
					adjacencyMatrix = new int[numVar][numVar]; 
					for(int i = 0; i < numVar; i ++){
						inList.add(new ArrayList<Integer>());
						outList.add(new ArrayList<Integer>());
					}
					continue;
				}
				//3. cardinality, directly ignore in binary domain
				if(lineCounter == 2){
					continue;
				}
				//4. number of factor (should be the same as number of variables?){
				if(lineCounter == 3){
					numberFact = Integer.valueOf(line);
					if(numberFact != numVar){
						System.out.println("number of factors and number of variables nt match!\n");
						new Exception();
						break;
					}
					continue;
				}
				
				//5. since then each line specifies the factors
				
				if(lineCounter > 3 && lineCounter <= 3 + numVar){
					if(CPTIndex == 194) {
						@SuppressWarnings("unused")
						int a = 1;
					}
					String[] strNums  = null;
					if(line.contains("\t")){
						strNums = line.split("\t");
					}
					else{
						strNums = line.split(" ");
					}
					
					int sizeOfFact = Integer.valueOf(strNums[0]);
					int destNode = lineCounter - 4;
					numberIn[destNode] = sizeOfFact - 1;
					for(int i = 1; i < sizeOfFact; i ++){
						int pNode = Integer.valueOf(strNums[i]);
						adjacencyMatrix[pNode][destNode] = 1;
						inList.get(destNode).add(pNode);
						outList.get(pNode).add(destNode);
					}
					continue;
				}
				
				//6. then comes the probs
				if(factSize == 0){
					//deal with the CPTs
					factSize = Integer.valueOf(line) / 2;
					parents = new ArrayList<>();
					theProb = new ArrayList<>();
					combCounter = 0;
					CPTIndex ++;
					realFactSize = factSize;
				}
				else{
					
					String[] strNum = line.substring(1).split(" ");
					double thePosProb = Double.valueOf(strNum[1]);
					
					if(ifBlur) {
						if(thePosProb == 1.0){
							thePosProb = 0.9999;
						}
						if(thePosProb == 0.0){
							thePosProb = 0.0001;
						}
					}
					
					//calculate true and false values by CPTIndex
					ArrayList<Boolean> reversed = new ArrayList<>();
					int index = realFactSize - factSize;
					for(int j = 0; j < inList.get(CPTIndex).size(); j ++){
						if(index % 2 == 1){
							reversed.add(true);
						}
						else{
							reversed.add(false);
						}
						index /= 2;
					}

					//recover true and false variables and add them to parents
					ArrayList<Integer> newParent = new ArrayList<>();
					int theP = 0;
					for(int j = reversed.size() - 1; j >= 0; j --){
						if(reversed.get(j)){
							newParent.add(TrueID(inList.get(CPTIndex).get(theP)));
							theP ++;
						}
						else{
							newParent.add(FalseID(inList.get(CPTIndex).get(theP)));
							theP ++;
						}
					}
					//traverse the prob array, see if there is already same value there
					boolean ifFound = false;
					for(int i = 0; i < theProb.size(); i ++){
						if(thePosProb == theProb.get(i)){		
							parents.get(i).add(newParent);
							ifFound = true;
							break;
						}
					}
					if(!ifFound){
						parents.add(new ArrayList<ArrayList<Integer>>());
						parents.get(parents.size() - 1).add(newParent);
						theProb.add(thePosProb);
					}
					factSize --;
					if(factSize == 0){
						if(CPTIndex == 194) {
							@SuppressWarnings("unused")
							int a = 1;
						}
						GenerateCPTfromUAIFile(CPTIndex, theProb, parents);
					}
				}
			}
			// Always close files.
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileDir + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + fileDir + "'");

		}
		
		/*****************************
		 * get evidence
		 *****************************/
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(eviFileDir);
			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			int lineCounter = -1;
			int numberOfEvi = 0;
			while ((line = bufferedReader.readLine()) != null) {
				
				if(line.length() == 0){
					continue;
				}
				lineCounter ++;
				//1. graph type
				if(lineCounter == 0){
					numberOfEvi = Integer.valueOf(line);
				}
				else{
					String[] strNum = null;
					if(line.contains("\t")){
						strNum = line.split("\t");
					}
					else{
						strNum = line.substring(1).split(" ");
					}
					
					boolean theVal = strNum[1].equals("1") ? true : false;
					evidence.put(Integer.valueOf(strNum[0]), theVal);
				}
			}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileDir + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + fileDir + "'");
		}
		
		/******************************
		 * generate MAP
		 ******************************/
		if(queFileDir == null){
			int numOfMAP = ((Double)((nodeNum - evidence.size()) * ratioMAP)).intValue();
			if(maxMAP != -1 && numOfMAP > maxMAP) {
				numOfMAP = maxMAP;
			}
			ArrayList<Integer> index = new ArrayList<>();
			for(int i = 0; i < nodeNum; i ++){
				index.add(i);
			}
			Collections.shuffle(index, rand);
			//generate MAP
			int countPossibleMAP = 0;
			for(int i = 0; i < nodeNum; i ++){
				int theNode = index.get(i);
				if(!evidence.containsKey(theNode)) {
					MAP.add(theNode);
					countPossibleMAP ++;
				}
				if(countPossibleMAP == numOfMAP) {
					break;
				}
			}
		}
	}
	
	public void GenerateCPTfromUAIFile(int theNode, ArrayList<Double> prob, 
			ArrayList<ArrayList<ArrayList<Integer>>> parents){
		TreeExp trueProb = TreeExp.BuildNewTreeExp(0.0, null);
		for(int i = 0; i < prob.size(); i ++){
			TreeExp sumCond = TreeExp.BuildNewTreeExp(0.0, null);
			for(int j = 0; j < parents.get(i).size(); j ++){
				TreeExp theCondition = TreeExp.BuildNewTreeExp(1.0, null);
				for(int k = 0; k < parents.get(i).get(j).size(); k ++){
					int parentID = parents.get(i).get(j).get(k);
					TreeExp theParent = null;
					if(!variables.containsKey(parentID)){
						theParent = new TreeExp(parentID, null);
						variables.put(parentID, theParent);
					}
					else{
						theParent = variables.get(parentID);
					}
					theCondition = theCondition.TIMES(theParent);
				}
				sumCond = sumCond.ADD(theCondition);
			}
			trueProb = trueProb.ADD(sumCond.TIMES(TreeExp.BuildNewTreeExp(prob.get(i), null)));
		}
		TreeExp falseProb = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueProb);
		TreeExp trueTree = null;
		int trueID = TrueID(theNode);
		if(!variables.containsKey(trueID)){
			trueTree = new TreeExp(trueID, null);
			variables.put(trueID, trueTree);
		}
		else{
			trueTree = variables.get(trueID);
		}
		TreeExp falseTree = null;
		int falseID = FalseID(theNode);
		if(!variables.containsKey(falseID)){
			falseTree = new TreeExp(falseID, null);
			variables.put(falseID, falseTree);
		}
		else{
			falseTree = variables.get(falseID);
		}
		CPTs.add(trueTree.TIMES(trueProb).ADD(falseTree.TIMES(falseProb)));
	}
	
	
	public DBN(int no, HashMap<Integer, TreeExp> var, int[][] adj, HashMap<Integer, Boolean> evi, 
			ArrayList<Integer> M, int[] ni, ArrayList<ArrayList<Integer>> ol, ArrayList<ArrayList<Integer>> il, ArrayList<TreeExp> C) {
		nodeNum = no;
		variables = var;
		adjacencyMatrix = adj;
		evidence = evi;
		MAP = M;
		numberIn = ni;
		outList = ol;
		inList = il;
		CPTs = C;
	}
	
	public DBN Reduction() throws Exception{
		
		/***************************************
		 * nodeNum
		 ***************************************/
		int newNodeNum = nodeNum + evidence.size() + MAP.size() * 2 + 3;
		
		/***********************************
		 * variables
		 ***********************************/
		HashMap<Integer, TreeExp> newVariables = new HashMap<Integer, TreeExp>();
		for(int i = 0; i < newNodeNum; i ++){
			int trueID = TrueID(i);
			int falseID = FalseID(i);
			newVariables.put(trueID, new TreeExp(trueID, null));
			newVariables.put(falseID, new TreeExp(falseID, null));
		}
		
		/****************************************
		 * adjacency matrix
		 ****************************************/
		//first copy the adjacency matrix
		int[][] newAdjacencyMatrix = new int[newNodeNum][newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < nodeNum; j ++){
				newAdjacencyMatrix[i][j] = adjacencyMatrix[i][j];
			}
		}
		//remove edgeds out from the evidence
		for (int i : evidence.keySet()) {
			for(int j = 0; j < outList.get(i).size(); j ++){
				int dest = outList.get(i).get(j);
				newAdjacencyMatrix[i][dest] = 0;
			}
		}
		int sIndex = nodeNum;
		int sNodeNum = MAP.size() + 1;
		//each map node points to a s node
		for(int i = 0; i < MAP.size(); i ++){
			int theMap = MAP.get(i);
			newAdjacencyMatrix[theMap][sIndex + i + 1] = 1;
		}
		//each s node points to next s node, starting from s0
		for(int i = 0; i < sNodeNum - 1; i ++){
			newAdjacencyMatrix[sIndex + i][sIndex + i + 1] = 1;
		}
		//last s nodredue points to v
		int vIndex = newNodeNum - 1;
		int lastSIndex = sIndex + sNodeNum - 1;
		newAdjacencyMatrix[lastSIndex][vIndex] = 1;
		//each decition node points to each s node
		int dIndex = sIndex + sNodeNum;
		int dNodeNum = MAP.size();
		int sOneIndex = sIndex + 1;
		for(int i = 0; i < dNodeNum; i ++){
			newAdjacencyMatrix[dIndex + i][sOneIndex + i] = 1;
		}
		//each evidence points to an t node
		int tIndex = dIndex + dNodeNum;
		int tOneIndex = tIndex + 1;
		int tNodeNum = evidence.size() + 1;
		int countEvi = 0;
		for(int theEvi: evidence.keySet()){
			newAdjacencyMatrix[theEvi][tOneIndex + countEvi] = 1;
			countEvi ++;
		}
		// each t node points to next t node
		for(int i = 0; i < tNodeNum; i ++){
			newAdjacencyMatrix[tIndex + i][tIndex + i + 1] = 1;
		}
		// last t points to v
		int lastTIndex = tIndex + tNodeNum - 1;
		newAdjacencyMatrix[lastTIndex][vIndex] = 1;
		
		
		/*********************************
		 * evidence
		 *********************************/
		HashMap<Integer, Boolean> newEvidence = new HashMap<Integer, Boolean>();
		
		/*********************************
		 * MAP
		 *********************************/
		//map nodes are all d nodes
		ArrayList<Integer> newMap = new ArrayList<>();
		for(int i = dIndex; i < dIndex + dNodeNum; i ++){
			newMap.add(i);
		}
		
		/********************************
		 * numberIn
		 ********************************/
		int[] newNumberIn = new int[newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			newNumberIn[i] = numberIn[i];
		}
		//reduce incoming edges from evidence
		for(int theEvi: evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newNumberIn[dest] --;
			}
		}
		
		for(int i = sOneIndex; i < sIndex + sNodeNum; i ++){
			newNumberIn[i] = 3;
		}
		for(int i = tOneIndex; i < tIndex + tNodeNum; i ++){
			newNumberIn[i] = 2;
		}
		newNumberIn[vIndex] = 2;
		
		/************************************
		 * outList
		 ************************************/
		//copy outlist first
		ArrayList<ArrayList<Integer>> newOutList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newOutList.add(new ArrayList<Integer>());
		}
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < outList.get(i).size(); j ++){
				newOutList.get(i).add(outList.get(i).get(j));
			}
		}
		//remove outlist from evidence
		//each evidence points to a t node
		countEvi = 0;
		for(int theEvi: evidence.keySet()){
			// first clear out all outlists of the evidence nodes
			newOutList.set(theEvi, new ArrayList<Integer>());
			// each evidence points to a t node
			newOutList.get(theEvi).add(tOneIndex + countEvi);
			countEvi ++;
		}
		//each t node pints to the next one
		for(int i = tIndex; i < lastTIndex; i ++){
			newOutList.get(i).add(i + 1);
		}
		//last t node points to v
		newOutList.get(lastTIndex).add(vIndex);
		
		//each MAP points to a s node
		for (int i = 0; i < MAP.size(); i ++) {
			int theMAP = MAP.get(i);
			newOutList.get(theMAP).add(sOneIndex + i);
		}
		// each s node pints to the next one
		for (int i = sIndex; i < lastSIndex; i++) {
			newOutList.get(i).add(i + 1);
		}
		// last s node points to v
		newOutList.get(lastSIndex).add(vIndex);
		
		//each d node points to a s node
		for(int i = 0; i < dNodeNum; i ++){
			newOutList.get(dIndex + i).add(sOneIndex + i);
		}
		
		/***********************************
		 * inList
		 ***********************************/
		//copy inlist first
		ArrayList<ArrayList<Integer>> newInList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newInList.add(new ArrayList<Integer>());
		}
		for (int i = 0; i < nodeNum; i++) {
			for (int j = 0; j < inList.get(i).size(); j++) {
				newInList.get(i).add(inList.get(i).get(j));
			}
		}
		// remove inlist from evidence's children
		// each evidence points to a t node
		countEvi = 0;
		for (int theEvi : evidence.keySet()) {
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newInList.get(dest).remove(new Integer(theEvi));
			}
			// each evidence points to a t node
			newInList.get(tOneIndex + countEvi).add(theEvi);
			countEvi++;
		}
		// each t node pints to the next one
		for (int i = tOneIndex; i <= lastTIndex; i++) {
			newInList.get(i).add(i - 1);
		}
		// last t node points to v
		newInList.get(vIndex).add(lastTIndex);

		// each MAP points to a s node
		for (int i = 0; i < MAP.size(); i++) {
			int theMAP = MAP.get(i);
			newInList.get(sOneIndex + i).add(theMAP);
		}
		// each s node pints to the next one
		for (int i = sOneIndex; i <= lastSIndex; i++) {
			newInList.get(i).add(i - 1);
		}
		// last s node points to v
		newInList.get(vIndex).add(lastSIndex);

		// each d node points to a s node
		for (int i = 0; i < dNodeNum; i++) {
			newInList.get(sOneIndex + i).add(dIndex + i);
		}
		
		/***********************************
		 * CPTs
		 ***********************************/
		//first call acopy all the CPTs
		ArrayList<TreeExp> newCPTs = new ArrayList<>();
		HashMap<TreeExp, TreeExp> copyMap = new HashMap<TreeExp, TreeExp>();
		//HashMap<Integer, TreeExp> varCollector = new HashMap<Integer, TreeExp>();
		for(int i = 0 ; i < nodeNum; i ++){
			TreeExp theCPT = CPTs.get(i).Copy(copyMap, newVariables);
			newCPTs.add(theCPT);
		}
		//plug in value for each evidence
		
		//long oldVarID = newCPTs.get(690).FindVarID(new ArrayList<TreeExp>(), 630);
		for(int theEvi : evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				TreeExp oriCPT = newCPTs.get(dest);
				ArrayList<Integer> changeList = new ArrayList<Integer>();
				changeList.add(TrueID(theEvi));
				changeList.add(FalseID(theEvi));
				//TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				newOne.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
				newCPTs.set(dest, newOne);
			}
		}
		//newCPTs.get(690).FindVarID(new ArrayList<TreeExp>(), 630);
		
		//generate cpt for s0
		newCPTs.add(TreeExp.BuildNewTreeExp(0.5, null).TIMES(TrueTree(newVariables, sIndex)).ADD(FalseTree(newVariables, sIndex).TIMES(TreeExp.BuildNewTreeExp(0.5, null))));
		//generate CPTs for s1 to sn
		int countMAP  = 0;
		for(int i = sOneIndex; i <= lastSIndex; i ++){
			TreeExp TrueM = TrueTree(newVariables, MAP.get(countMAP));
			TreeExp TrueD = TrueTree(newVariables, i + dNodeNum);
			TreeExp jointTrueCond = TrueM.TIMES(TrueD);
			TreeExp FalseM = FalseTree(newVariables, MAP.get(countMAP));
			TreeExp FalseD = FalseTree(newVariables, i + dNodeNum);
			TreeExp jointFalseCond = FalseM.TIMES(FalseD);
			TreeExp jointCond = jointTrueCond.ADD(jointFalseCond);
			TreeExp TrueLast = TrueTree(newVariables, i - 1);
			TreeExp trueCond = jointCond.TIMES(TrueLast);
			TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
			TreeExp TrueRes = TrueTree(newVariables, i);
			TreeExp FalseRes = FalseTree(newVariables, i);
			TreeExp jointRes = trueCond.TIMES(TrueRes).ADD(falseCond.TIMES(FalseRes));
			newCPTs.add(jointRes);
			countMAP ++;
		}
		//d nodes have no CPTs
		for(int i = dIndex; i < tIndex; i ++){
			newCPTs.add(null);
		}
		//generate CPTs for t0
		newCPTs.add(TreeExp.BuildNewTreeExp(0.5, null).TIMES(TrueTree(newVariables, tIndex)).
				ADD(FalseTree(newVariables, tIndex).TIMES(TreeExp.BuildNewTreeExp(0.5, null))));
		
		//generate CPTs for t1 to tn
		int countT = 0;
		for(int theEvi: evidence.keySet()){
			boolean eMust = false;
			if(evidence.get(theEvi)){
				eMust = true;
			}
			TreeExp trueETree = null;
			TreeExp falseETree = null;
			if(eMust){
				trueETree = TrueTree(newVariables, theEvi);
				falseETree = FalseTree(newVariables, theEvi);
			}
			else{
				falseETree = TrueTree(newVariables, theEvi);
				trueETree = FalseTree(newVariables, theEvi);
			}
			TreeExp lastTrue = TrueTree(newVariables, tIndex + countT);
			TreeExp trueCond = lastTrue.TIMES(trueETree);
			TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
			TreeExp theCPT = trueCond.TIMES(TrueTree(newVariables, tOneIndex + countT)).ADD
					(falseCond.TIMES(FalseTree(newVariables, tOneIndex + countT)));
			countT ++;
			newCPTs.add(theCPT);
		}
		
		//generate CPTs for v
		TreeExp trueCond = TrueTree(newVariables, lastTIndex).TIMES(TrueTree(newVariables, lastSIndex));
		TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
		TreeExp trueBranch = trueCond.TIMES(TrueTree(newVariables, vIndex));
		TreeExp falseBranch = falseCond.TIMES(FalseTree(newVariables, vIndex));
		newCPTs.add(trueBranch.ADD(falseBranch));
		
		/************************************
		 * record index
		 ************************************/
		DBN result = new DBN(newNodeNum, newVariables, newAdjacencyMatrix, newEvidence, 
				newMap, newNumberIn, newOutList, newInList, newCPTs);
		result.sIndex = sIndex;
		result.tIndex = tIndex;
		result.dIndex = dIndex;
		result.vIndex = vIndex;
		
		return result;
	}
	
	public DBN Reduction2() throws Exception{
	
		/***************************************
		 * nodeNum
		 ***************************************/
		int newNodeNum = nodeNum +  1;
		
		/***********************************
		 * variables
		 ***********************************/
		HashMap<Integer, TreeExp> newVariables = new HashMap<Integer, TreeExp>();
		for(int i = 0; i < newNodeNum; i ++){
			int trueID = TrueID(i);
			int falseID = FalseID(i);
			newVariables.put(trueID, new TreeExp(trueID, null));
			newVariables.put(falseID, new TreeExp(falseID, null));
		}
		
		/****************************************
		 * adjacency matrix
		 ****************************************/
		//first copy the adjacency matrix
		int[][] newAdjacencyMatrix = new int[newNodeNum][newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < nodeNum; j ++){
				newAdjacencyMatrix[i][j] = adjacencyMatrix[i][j];
			}
		}
		//remove edgeds out from the evidence
		for (int i : evidence.keySet()) {
			for(int j = 0; j < outList.get(i).size(); j ++){
				int dest = outList.get(i).get(j);
				newAdjacencyMatrix[i][dest] = 0;
			}
		}
		
		int vIndex = newNodeNum - 1;
		//each evidence points to an t node
		for (int i : evidence.keySet()) {
			newAdjacencyMatrix[i][vIndex] = 1;
		}
		
		
		
		/*********************************
		 * evidence
		 *********************************/
		HashMap<Integer, Boolean> newEvidence = new HashMap<Integer, Boolean>();
		
		/*********************************
		 * MAP
		 *********************************/
		//map nodes are all d nodes
		ArrayList<Integer> newMap = new ArrayList<>();
		for(int i = dIndex; i < MAP.size(); i ++){
			newMap.add(i);
		}
		
		/********************************
		 * numberIn
		 ********************************/
		int[] newNumberIn = new int[newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			newNumberIn[i] = numberIn[i];
		}
		//reduce incoming edges from evidence
		for(int theEvi: evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newNumberIn[dest] --;
			}
		}
		
		newNumberIn[vIndex] = evidence.size() + 1;
		
		/************************************
		 * outList
		 ************************************/
		//copy outlist first
		ArrayList<ArrayList<Integer>> newOutList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newOutList.add(new ArrayList<Integer>());
		}
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < outList.get(i).size(); j ++){
				newOutList.get(i).add(outList.get(i).get(j));
			}
		}
		//remove outlist from evidence
		//each evidence points to a t node
		for(int theEvi: evidence.keySet()){
			// first clear out all outlists of the evidence nodes
			newOutList.set(theEvi, new ArrayList<Integer>());
			// each evidence points to a t node
			newOutList.get(theEvi).add(vIndex);
		}
		

		
		/***********************************
		 * inList
		 ***********************************/
		//copy inlist first
		ArrayList<ArrayList<Integer>> newInList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newInList.add(new ArrayList<Integer>());
		}
		for (int i = 0; i < nodeNum; i++) {
			for (int j = 0; j < inList.get(i).size(); j++) {
				newInList.get(i).add(inList.get(i).get(j));
			}
		}
		// remove inlist from evidence's children
		// each evidence points to a t node
		for (int theEvi : evidence.keySet()) {
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newInList.get(dest).remove(new Integer(theEvi));
			}
			// each evidence points to a t node
			newInList.get(vIndex).add(theEvi);
		}

		
		
		/***********************************
		 * CPTs
		 ***********************************/
		//first call acopy all the CPTs
		ArrayList<TreeExp> newCPTs = new ArrayList<>();
		HashMap<TreeExp, TreeExp> copyMap = new HashMap<TreeExp, TreeExp>();
		//HashMap<Integer, TreeExp> varCollector = new HashMap<Integer, TreeExp>();
		for(int i = 0 ; i < nodeNum; i ++){
			TreeExp theCPT = CPTs.get(i).Copy(copyMap, newVariables);
			newCPTs.add(theCPT);
		}
		//plug in value for each evidence
		
		//long oldVarID = newCPTs.get(690).FindVarID(new ArrayList<TreeExp>(), 630);
		for(int theEvi : evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				
				int dest = outList.get(theEvi).get(j);
				TreeExp oriCPT = newCPTs.get(dest);
				ArrayList<Integer> changeList = new ArrayList<Integer>();
				changeList.add(TrueID(theEvi));
				changeList.add(FalseID(theEvi));
				//TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				newOne.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
				newCPTs.set(dest, newOne);
				
				//long id = newCPTs.get(690).FindVarID(new ArrayList<TreeExp>(), 630);
				//System.out.println(newCPTs.get(690).specificVarID);
				//if(id != oldVarID){
					
					//@SuppressWarnings("unused")
					//int a = 1;
				//}
			}
		}
		//newCPTs.get(690).FindVarID(new ArrayList<TreeExp>(), 630);
		
		
		
		
		//generate CPTs for v
		TreeExp trueCond = TreeExp.BuildNewTreeExp(1.0, null);
		for(int theEvi : evidence.keySet()){
			if(evidence.get(theEvi)){
				trueCond = trueCond.TIMES(TrueTree(newVariables, theEvi));
			}
			else{
				trueCond = trueCond.TIMES(FalseTree(newVariables, theEvi));
			}
		}
	
		TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
		TreeExp trueBranch = trueCond.TIMES(TrueTree(newVariables, vIndex));
		TreeExp falseBranch = falseCond.TIMES(FalseTree(newVariables, vIndex));
		newCPTs.add(trueBranch.ADD(falseBranch));
		
		/************************************
		 * record index
		 ************************************/
		DBN result = new DBN(newNodeNum, newVariables, newAdjacencyMatrix, newEvidence, 
				newMap, newNumberIn, newOutList, newInList, newCPTs);
		result.sIndex = sIndex;
		result.dIndex = dIndex;
		result.vIndex = vIndex;
		return result;
	}
	
	//pass in the index of MAP node in old graph
	//return node index in new graph
	public int GetNewDNode(int i){
		return i + nodeNum;
	}
	
	public int GetSNode(int i){
		return i + nodeNum + MAP.size();
	}
	
	public int GetVNode(){
		return nodeNum - 1;
	}
	
	public DBN Reduction3() throws Exception{
		
		/***************************************
		 * nodeNum
		 ***************************************/
		int newNodeNum = nodeNum + MAP.size() * 2 + 1;
		vIndex = newNodeNum - 1;
		/***********************************
		 * variables
		 ***********************************/
		HashMap<Integer, TreeExp> newVariables = new HashMap<Integer, TreeExp>();
		for(int i = 0; i < newNodeNum; i ++){
			int trueID = TrueID(i);
			int falseID = FalseID(i);
			newVariables.put(trueID, new TreeExp(trueID, null));
			newVariables.put(falseID, new TreeExp(falseID, null));
		}
		
		/****************************************
		 * adjacency matrix
		 ****************************************/
		//first copy the adjacency matrix
		int[][] newAdjacencyMatrix = new int[newNodeNum][newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < nodeNum; j ++){
				newAdjacencyMatrix[i][j] = adjacencyMatrix[i][j];
			}
		}
		//remove edgeds out from the evidence
		for (int i : evidence.keySet()) {
			for(int j = 0; j < outList.get(i).size(); j ++){
				int dest = outList.get(i).get(j);
				newAdjacencyMatrix[i][dest] = 0;
			}
			//add evidence adjacent to v node
			newAdjacencyMatrix[i][vIndex] = 1;
		}
		//MAP nodes have no outcoming edges
		for(int i = 0; i < MAP.size(); i ++){
			//remove edges from the MAP nodes
			int m = MAP.get(i);
			int newD = GetNewDNode(i);
			int theS = GetSNode(i);
			for(int j = 0; j < outList.get(m).size(); j ++){
				int dest = outList.get(m).get(j);
				newAdjacencyMatrix[m][dest] = 0;
				newAdjacencyMatrix[newD][dest] = 1;
			}
			//M nodes and decition nodes all point to s node
			newAdjacencyMatrix[newD][theS] = 1;
			newAdjacencyMatrix[m][theS] = 1;
			newAdjacencyMatrix[theS][vIndex] = 1;
		}
		
		/*********************************
		 * evidence
		 *********************************/
		HashMap<Integer, Boolean> newEvidence = new HashMap<Integer, Boolean>();
		
		/*********************************
		 * MAP
		 *********************************/
		//map nodes are all d nodes
		ArrayList<Integer> newMap = new ArrayList<>();
		for(int i = 0; i < MAP.size(); i ++){
			newMap.add(GetNewDNode(i));
		}
		
		/********************************
		 * numberIn
		 ********************************/
		int[] newNumberIn = new int[newNodeNum];
		for(int i = 0; i < nodeNum; i ++){
			newNumberIn[i] = numberIn[i];
		}
		//reduce incoming edges from evidence
		for(int theEvi: evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newNumberIn[dest] --;
			}
		}
		
		//set incoming number for new nodes
		//new decision nodes have no incoming edges
		for(int d: newMap){
			newNumberIn[d] = 0;
		}
		//S nodes all have 2 incoming edges
		int sSize = MAP.size();
		for(int i = 0; i < sSize; i ++){
			newNumberIn[GetSNode(i)] = 2;
		}
		//v nodes have incoming edges from all s nodes and evidence nodes
		newNumberIn[vIndex] = evidence.size() + sSize;
		
		/************************************
		 * outList
		 ************************************/
		//copy outlist first
		ArrayList<ArrayList<Integer>> newOutList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newOutList.add(new ArrayList<Integer>());
		}
		for(int i = 0; i < nodeNum; i ++){
			for(int j = 0; j < outList.get(i).size(); j ++){
				newOutList.get(i).add(outList.get(i).get(j));
			}
		}
		//remove outlist from evidence
		//each evidence points to v node
		for(int theEvi: evidence.keySet()){
			// first clear out all outlists of the evidence nodes
			newOutList.set(theEvi, new ArrayList<Integer>());
			// each evidence points to v node
			newOutList.get(theEvi).add(vIndex);
		}
		
		//decision nodes only points to S
		for(int d: MAP){
			newOutList.set(d, new ArrayList<Integer>());
			int theS = GetSNode(MAP.indexOf(d));
			newOutList.get(d).add(theS);
			//newD has original decision nodes outs and s
			int theNewD = GetNewDNode(MAP.indexOf(d));
			for(int j = 0; j < outList.get(d).size(); j ++){
				int dest = outList.get(d).get(j);
				newOutList.get(theNewD).add(dest);
			}
			newOutList.get(theNewD).add(theS);
		}
		
		//all S point to v
		for(int i = 0; i < MAP.size(); i ++){
			int theS = GetSNode(i);
			newOutList.get(theS).add(vIndex);
		}
		
		/***********************************
		 * inList
		 ***********************************/
		//copy inlist first
		ArrayList<ArrayList<Integer>> newInList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < newNodeNum; i ++){
			newInList.add(new ArrayList<Integer>());
		}
		for (int i = 0; i < nodeNum; i++) {
			for (int j = 0; j < inList.get(i).size(); j++) {
				newInList.get(i).add(inList.get(i).get(j));
			}
		}
		// remove inlist from evidence's children
		// each evidence points to a t node
		for (int theEvi : evidence.keySet()) {
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				newInList.get(dest).remove(new Integer(theEvi));
			}
			// each evidence points to a t node
			newInList.get(vIndex).add(theEvi);
		}
		
		//children of each decision nodes now has new decision node as parents
		for (int d: MAP) {
			int theDIndex = MAP.indexOf(d);
			for(int j = 0; j < outList.get(d).size(); j ++){
				int dest = outList.get(d).get(j);
				newInList.get(dest).remove(new Integer(d));
				newInList.get(dest).add(GetNewDNode(theDIndex));
			}
			// each s points to v node
			newInList.get(vIndex).add(GetSNode(theDIndex));
		}
		
		//each s node has two parents, decision and new decision
		for(int i = 0; i < MAP.size(); i ++){
			int theD = MAP.get(i);
			int theNewD = GetNewDNode(i);
			int theS = GetSNode(i);
			newInList.get(theS).add(theD);
			newInList.get(theS).add(theNewD);
		}

		
		
		/***********************************
		 * CPTs
		 ***********************************/
		//first call acopy all the CPTs
		ArrayList<TreeExp> newCPTs = new ArrayList<>();
		HashMap<TreeExp, TreeExp> copyMap = new HashMap<TreeExp, TreeExp>();
		//HashMap<Integer, TreeExp> varCollector = new HashMap<Integer, TreeExp>();
		for(int i = 0 ; i < nodeNum; i ++){
			TreeExp theCPT = CPTs.get(i).Copy(copyMap, newVariables);
			newCPTs.add(theCPT);
		}
		//plug in value for each evidence
		for(int theEvi : evidence.keySet()){
			for(int j = 0; j < outList.get(theEvi).size(); j ++){
				int dest = outList.get(theEvi).get(j);
				TreeExp oriCPT = newCPTs.get(dest);
				ArrayList<Integer> changeList = new ArrayList<Integer>();
				changeList.add(TrueID(theEvi));
				changeList.add(FalseID(theEvi));
				//TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence, theEvi, changeList);
				newOne.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
				newCPTs.set(dest, newOne);
				oriCPT = null;
			}
		}
		
		
		//replace CPT of children of decision nodes with new decision nodes
		for (int i = 0; i < MAP.size(); i ++) {
			int d = MAP.get(i);
			for (int j = 0; j < outList.get(d).size(); j++) {
				int dest = outList.get(d).get(j);
				TreeExp oriCPT = newCPTs.get(dest);
				ArrayList<Integer> changeList = new ArrayList<Integer>();
				changeList.add(TrueID(d));
				changeList.add(FalseID(d));
				// TreeExp newOne = oriCPT.SumOverIWtChangeList(evidence,
				// theEvi, changeList);
				int theNewD = GetNewDNode(i);
				TreeExp newOne = oriCPT.ReplaceNodes(d, theNewD, changeList, newVariables);
				newOne.Prunning(new HashMap<TreeExp, Boolean>(),
						new HashMap<Integer, TreeExp>());
				newCPTs.set(dest, newOne);
			}
		}
		
		//add CPTs for new decision nodes
		for(int i = 0; i < MAP.size(); i ++){
			int newD = GetNewDNode(i);
			TreeExp newCPT = TreeExp.BuildNewTreeExp(0.5, null).TIMES(TrueTree(newVariables, newD));
			newCPT = newCPT.ADD(TreeExp.BuildNewTreeExp(0.5, null).TIMES(FalseTree(newVariables, newD)));
			newCPTs.add(newCPT);
		}
		
		
		//generate CPTs for s
		for(int i = 0; i < MAP.size(); i ++){
			int theS = GetSNode(i);
			int theD = MAP.get(i);
			int theNewD = GetNewDNode(i);
			TreeExp trueCond = TrueTree(newVariables, theD).TIMES(TrueTree(newVariables, theNewD));
			trueCond = trueCond.ADD(FalseTree(newVariables, theD).TIMES(FalseTree(newVariables, theNewD)));
			TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
			TreeExp newCPT = trueCond.TIMES(TrueTree(newVariables, theS))
					.ADD(falseCond.TIMES(FalseTree(newVariables, theS)));
			newCPTs.add(newCPT);
		}
		
		//generate CPTs for v
		TreeExp trueCond = TreeExp.BuildNewTreeExp(1.0, null);
		for(int theEvi : evidence.keySet()){
			if(evidence.get(theEvi)){
				trueCond = trueCond.TIMES(TrueTree(newVariables, theEvi));
			}
			else{
				trueCond = trueCond.TIMES(FalseTree(newVariables, theEvi));
			}
		}
		
		for(int i = 0; i < MAP.size(); i ++){
			trueCond = trueCond.TIMES(TrueTree(newVariables, GetSNode(i)));
		}
	
		TreeExp falseCond = TreeExp.BuildNewTreeExp(1.0, null).MINUS(trueCond);
		TreeExp trueBranch = trueCond.TIMES(TrueTree(newVariables, vIndex));
		TreeExp falseBranch = falseCond.TIMES(FalseTree(newVariables, vIndex));
		newCPTs.add(trueBranch.ADD(falseBranch));
		
		/************************************
		 * record index
		 ************************************/
		DBN result = new DBN(newNodeNum, newVariables, newAdjacencyMatrix, newEvidence, 
				newMap, newNumberIn, newOutList, newInList, newCPTs);
		sIndex = nodeNum + MAP.size();
		dIndex = nodeNum;
		result.sIndex = sIndex;
		result.dIndex = dIndex;
		result.vIndex = vIndex;
		return result;
	}

	
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
	
	private TreeExp GenerateCPT(int theNode, int numberComb){

		ArrayList<ArrayList<Boolean>> conds = new ArrayList<>();
		while(true){
			ArrayList<Boolean> theCond = new ArrayList<>();
			for(int j = 0; j < numberIn[theNode]; j ++){
				double dice = rand.nextDouble();
				if(dice > 0.5){
					theCond.add(true);
				}
				else{
					theCond.add(false);
				}
			}
			if(conds.contains(theCond)){
				continue;
			}
			else{
				conds.add(theCond);
			}
			if(conds.size() >= numberComb){
				break;
			}
		}
		
		//prepare the cond trees for the node
		int theNodeTrueID = TrueID(theNode);
		int theNodeFalseID = FalseID(theNode);
		TreeExp theNodeTrueTree = null;
		TreeExp theNodeFalseTree = null;
		if(variables.containsKey(theNodeTrueID)){
			theNodeTrueTree = variables.get(theNodeTrueID);
		}
		else{
			theNodeTrueTree = new TreeExp(theNodeTrueID, null);
			variables.put(theNodeTrueID, theNodeTrueTree);
		}
		if(variables.containsKey(theNodeFalseID)){
			theNodeFalseTree = variables.get(theNodeFalseID);
		}
		else{
			theNodeFalseTree = new TreeExp(theNodeFalseID, null);
			variables.put(theNodeFalseID, theNodeFalseTree);
		}
		
		//prepare two sets of conditions
		// one for true value one for false value
		ArrayList<TreeExp> theCondTreeExps = new ArrayList<TreeExp>();
		for(int i = 0; i < conds.size(); i ++){
			theCondTreeExps.add(TreeExp.BuildNewTreeExp(1.0, null));
		}
		ArrayList<TreeExp> theCondTreeExpsForTrue = new ArrayList<TreeExp>();
		ArrayList<TreeExp> theCondTreeExpsForFalse = new ArrayList<TreeExp>();
		for(int i = 0; i < conds.size(); i ++){
			ArrayList<Boolean> theCond = conds.get(i);
			for(int j = 0; j < theCond.size(); j ++){
				int theID = j;
				if(theCond.get(j)){
					theID = TrueID(inList.get(theNode).get(j));
				}
				else{
					theID = FalseID(inList.get(theNode).get(j));
				}
				TreeExp theVarTree = null;
				if(variables.containsKey(theID)){
					theVarTree = variables.get(theID);
				}
				else{
					theVarTree = new TreeExp(theID, null);
					variables.put(theID, theVarTree);
				}
				theCondTreeExps.set(i, theCondTreeExps.get(i).TIMES(theVarTree));
			}
			theCondTreeExpsForTrue.add(theCondTreeExps.get(i).TIMES(theNodeTrueTree));
			theCondTreeExpsForFalse.add(theCondTreeExps.get(i).TIMES(theNodeFalseTree));
		}
		
		TreeExp CPT = TreeExp.BuildNewTreeExp(0.0, null);
		for(int i = 0; i < conds.size(); i ++){
			double trueVal = rand.nextDouble();
			CPT = CPT.ADD(theCondTreeExpsForTrue.get(i).TIMES(TreeExp.BuildNewTreeExp(trueVal, null)));

			CPT = CPT.ADD(theCondTreeExpsForFalse.get(i).TIMES(TreeExp.BuildNewTreeExp(1.0 - trueVal, null)));
		}
		
		TreeExp sumConds = TreeExp.BuildNewTreeExp(0.0, null);
		for(int i = 0; i < conds.size(); i ++){
			sumConds = sumConds.ADD(theCondTreeExps.get(i));
		}
		double trueVal = rand.nextDouble();
		CPT = CPT.ADD(TreeExp.BuildNewTreeExp(1.0, null).MINUS(sumConds).TIMES(theNodeTrueTree).TIMES(TreeExp.BuildNewTreeExp(trueVal, null)));
		CPT = CPT.ADD(TreeExp.BuildNewTreeExp(1.0, null).MINUS(sumConds).TIMES(theNodeFalseTree).TIMES(TreeExp.BuildNewTreeExp(1.0 - trueVal, null)));	
		return CPT;
	}
	/*
	private static Boolean EvalRes(HashMap<Integer, Boolean> res, FactorGraph fg) throws Exception{
		Optimal eval = new Optimal(fg);
		
		double oneVal = eval.Evaluate(res);
		recor.append("choose " + res + ": " + oneVal + "\n");
		
		
		res.put(fg.MAP.get(0), !res.get(fg.MAP.get(0)));
		double theOther = new Optimal(fg).Evaluate(res);
		recor.append("the other " + res + ": " + theOther + "\n");
		
		String conclusion = oneVal > theOther ? "correct!" : "wrong!";
		recor.append("BP: " + conclusion + "\n\n");
		if(oneVal > theOther){
			return true;
		}
		else{
			return false;
		}
	}
	*/
}
