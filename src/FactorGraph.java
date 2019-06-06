import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.print.attribute.HashAttributeSet;


public class FactorGraph {
	public int[][] adjacencyMatrix = null;
	int nodeNum = 0;
	public Random rand = new Random(1);
	public HashMap<Integer, Boolean> evidence = null;
	public ArrayList<Integer> MAP = null;
	ArrayList<ArrayList<Integer>> connectedList = new ArrayList<>();
	ArrayList<TreeExp> CPTs = new ArrayList<>();
	HashMap<Integer, TreeExp> variables = null;

	public FactorGraph(DBN theDBN) {
		CPTs = theDBN.CPTs;
		nodeNum = theDBN.nodeNum * 2;
		evidence = theDBN.evidence;
		MAP = theDBN.MAP;
		variables = theDBN.variables;
		adjacencyMatrix = new int[nodeNum][nodeNum];
		// for each node in dbn
		for(int i = 0; i < nodeNum; i ++){
			connectedList.add(new ArrayList<Integer>());
		}
		for(int i = 0; i < theDBN.nodeNum; i ++){
			// corresponding factor node is connected to the node
			Connect(i, correspondFactor(i));
			// each incoming node to the node is connected to the factor node
			for(int j = 0; j < theDBN.inList.get(i).size(); j ++){
				int neibough = theDBN.inList.get(i).get(j);
				Connect(correspondFactor(i), neibough);
			}
		}
	}
	
	private void Connect(int a, int b){
		adjacencyMatrix[a][b] = 1;
		adjacencyMatrix[b][a] = 1;
		connectedList.get(a).add(b);
		connectedList.get(b).add(a);
	}
	
	public int correspondFactor(int num){
		return num + nodeNum / 2;
	}
	
	public int correspondVar(int factor){
		return factor - nodeNum / 2;
	}
}
