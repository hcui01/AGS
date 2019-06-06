import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.CertPathTrustManagerParameters;


public class Optimal {
	FactorGraph fg = null;

	DBN oriDBN = null;
	Optimal(FactorGraph theFg, DBN orDBN, DBN ori){
		fg = theFg;

		oriDBN = ori;
	}
	double Evaluate(HashMap<Integer, Boolean> solution) throws Exception{
		
		HashMap<Integer, Boolean> constNodes = new HashMap<Integer, Boolean>();
		constNodes.putAll(fg.evidence);
		constNodes.putAll(solution);
		
		//nodes to trees
		ArrayList<ArrayList<TreeExp>> nodes2Tree = new ArrayList<ArrayList<TreeExp>>();
		HashMap<TreeExp, ArrayList<Integer>> tree2Node = new HashMap<TreeExp, ArrayList<Integer>>();
		for(int i = 0; i < fg.CPTs.size(); i ++){
			tree2Node.put(fg.CPTs.get(i), new ArrayList<Integer>());
		}
		for(int i = 0; i < fg.nodeNum / 2; i ++){
			nodes2Tree.add(new ArrayList<TreeExp>());
			nodes2Tree.get(i).add(fg.CPTs.get(i));
			tree2Node.get(fg.CPTs.get(i)).add(i);
			ArrayList<Integer> theOut = oriDBN.outList.get(i);
			for(int j = 0; j < theOut.size(); j ++){
				int dest = theOut.get(j);
				TreeExp destTree = fg.CPTs.get(dest);
				nodes2Tree.get(i).add(destTree);
				tree2Node.get(destTree).add(i);
			}
		}
		/*
		TreeExp prod = new TreeExp(1.0, null);
		for(int i = 0; i < fg.CPTs.size(); i ++){
			prod = prod.TIMES(fg.CPTs.get(i));
		}
		*/
		TreeExp prod = null;
		for(int i = 0; i < fg.nodeNum / 2; i ++){
			//prod = prod.SumOverI(constNodes, i)	
			/**************** variable elimination ******************/
			//first build a product of all trees that have node i involved
			prod = new TreeExp(1.0, null);
			ArrayList<Integer> involved = new ArrayList<Integer>();
			for(int j = 0; j < nodes2Tree.get(i).size(); j ++){
				TreeExp theT = nodes2Tree.get(i).get(j);
				prod = prod.TIMES(theT);
				ArrayList<Integer> nodesInvolved = tree2Node.get(theT);
				for(Integer theNode: nodesInvolved){
					if(!involved.contains(theNode)){
						involved.add(theNode);
					}
				}
				for(Integer theNode: nodesInvolved){
					ArrayList<TreeExp> theList = nodes2Tree.get(theNode);
					theList.remove(theList.indexOf(theT));
				}
				j --;
			}
			prod = prod.SumOverI(constNodes, i);
			//prod.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
			//create false branch
			//variables in false branch are recordede in varCollector
			/*
			HashMap<Integer, TreeExp> varCollector = new HashMap<Integer, TreeExp>();
			TreeExp falseBranch = prod.Copy(new HashMap<TreeExp, TreeExp>(), varCollector);
			//put in values of true into this tree
			fg.variables.get(i * 2).ToNumberNode(1.0);
			fg.variables.get(i * 2 + 1).ToNumberNode(0.0);
			//prod.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
			//put in values of false branch into this tree
			varCollector.get(i * 2).ToNumberNode(0.0);
			varCollector.get(i * 2 + 1).ToNumberNode(1.0);
			//falseBranch.Prunning(new HashMap<TreeExp, Boolean>(), new HashMap<Integer, TreeExp>());
			prod = prod.ADD(falseBranch);
			*/
			tree2Node.put(prod, new ArrayList<Integer>());
			for(Integer theNode: involved){
				nodes2Tree.get(theNode).add(prod) ;
				tree2Node.get(prod).add(theNode);
			}
			//System.out.println(i);
		}
		double eval = prod.RealValue(new ArrayList<Double>(), new HashMap<TreeExp, Double>());
		for(int i = 0; i < fg.variables.size(); i ++){
			fg.variables.get(i).term.coefficient = 1.0;
			fg.variables.get(i).term.var = i;
		}
		return eval;
	}
	
	double BPEvaluate(HashMap<Integer, Boolean> solution) throws Exception{
		ArrayList<Integer> fakeMAP = new ArrayList<Integer>();
		StringBuilder rec = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		//long ori = runtime.totalMemory();
		BP bp = new BP(fakeMAP, solution, fg, 0.001, rec, 30000);
		
		//System.out.println("BP costs " + (runtime.totalMemory() - ori) );
		fakeMAP = null;
		if(!bp.ifConverge){
			return -1;
		}
		else{
			double res = bp.GetMarginalEvi(fg.evidence);
			bp.oldMessages = null;
			bp = null;
			return res;
		}
	}	
}
