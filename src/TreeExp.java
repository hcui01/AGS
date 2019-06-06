
import java.beans.FeatureDescriptor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.swing.event.InternalFrameListener;

public class TreeExp {
	
	class Term{
		/*
		 if the node type is "THRE", then
		 coefficient = max
		 var = min
		 note min can only be integer
		 */
		double coefficient;
		int var;
		public Term(double c){
			coefficient = c;
			// var = 1 means this is a pure number 
			var = -1;
		}
		public Term(double c, int v){
			coefficient = c;
			// var = 1 means this is a pure number 
			var = v;
		}
		public Term(Term t){
			var = t.var;
			coefficient = t.coefficient;
		}
		
		
	} 
	
	//node type has "SUM"/"MIN"/"PRO"/"DIV"/"LEA"/EX
	String expType;
	//if a node is leaf, then there is a term assgined to it
	Term term = null;
	//if a node is not leaf, then it has children
	public ArrayList<TreeExp> subExp = null;
	//pointer to the father
	public ArrayList<TreeExp> father = null;

	public int counter = 0; 
	
	boolean ifInQ = false;
	
	int printCounter = 0;
	
	static final double ACTIVE_THRE = 500;
	
	public TreeExp(){};
	
	static HashMap<Double, TreeExp> nodesToNumber = new HashMap<Double, TreeExp>();
	
	static final boolean ifLift = true;
	
	public long numOfNonLeaf = 0;
	
    
    //long startDec = System.currentTimeMillis();
  	static HashMap<Object, HashMap<Object, TreeExp>> PlusHash = new HashMap<Object, HashMap<Object, TreeExp>>();
  	static HashMap<Object, HashMap<Object, TreeExp>> MinusHash = new HashMap<Object, HashMap<Object, TreeExp>>();
  	static HashMap<Object, HashMap<Object, TreeExp>> TimesHash = new HashMap<Object, HashMap<Object, TreeExp>>();
  	static HashMap<Object, HashMap<Object, TreeExp>> DividHash = new HashMap<Object, HashMap<Object, TreeExp>>();
  	static HashMap<Object, HashMap<Object, TreeExp>> ExpHash = new HashMap<Object, HashMap<Object, TreeExp>>();
  	static HashMap<Object, HashMap<Object, TreeExp>> ThresholdHash = new HashMap<Object, HashMap<Object, TreeExp>>();
	

	public static TreeExp BuildNewTreeExp(double constant, TreeExp f){
		if(Global.ifLift && nodesToNumber.containsKey(constant)){
			return nodesToNumber.get(constant);
		}
		else{
			TreeExp node = new TreeExp(constant, f);
			if(Global.ifLift){
				nodesToNumber.put(constant, node);
			}
			return node;
		}
	}

	//constructor with the type and father
	public TreeExp(String type, TreeExp f){
		if(father == null){
			father = new ArrayList<TreeExp>();
		}
		if(f != null)
		father.add(f);
		expType = new String(type);
		
		term = null;
		subExp = new ArrayList<TreeExp>();
		if(!type.equals("LEA")){
			counter ++;
		}
	}
	
	//constructor for a number and father
	public TreeExp(double constant, TreeExp f){

		if(father == null){
			father = new ArrayList<TreeExp>();
		}
		if(f != null)
		father.add(f);
		expType = "LEA";
		term = new Term(constant, -1);
		subExp = null;

	}
	
	//constructor for a variable and father
	public TreeExp(int v, TreeExp f) {

		if(father == null){
			father = new ArrayList<TreeExp>();
		}
		if(f != null)
		father.add(f);
		expType = "LEA";
		term = new Term(1, v);
		subExp = null;
		counter ++;
	}
	
	//constructor for a variable and father
	// only used for THRE node
	public TreeExp(String type, double c, int v, TreeExp f) {

		if (father == null) {
			father = new ArrayList<TreeExp>();
		}
		if (f != null)
			father.add(f);
		expType = new String(type);
		term = new Term(c, v);
		subExp = new ArrayList<TreeExp>();
		counter ++;
	}


	//take gradient of oriExp wrt var
	//fill in the content for gradient exp
	private double GetGradientForTerm(int v){
		if(v == term.var){
			return term.coefficient;
		}
		else{
			return 0;
		}
	}
	
	public long size(ArrayList<TreeExp> visited){
		long res = 0;
		if(visited.contains(this)){
			return 0;
		}
		else{
			if(expType.equals("LEA")){
				res = 1;
			}
			else{
				long sum = 0;
				for(TreeExp sub: subExp){
					sum += sub.size(visited);
				}
				res = sum + 1;
			}
		}
		visited.add(this);
		return res;
	}
	
	public void RevAccGradient(int maxVarSize, boolean ifSort, ArrayList<TreeExp> que, ArrayList<Double> delta, ArrayList<Double> varVal) throws Exception{
		HashMap<Integer, Double> result = new HashMap<Integer, Double>();
		HashMap<TreeExp, Double> partialDev = new HashMap<TreeExp, Double>();
		HashMap<TreeExp, Double> valRec = new HashMap<TreeExp, Double>();
		int queIndex = 0;
		int countVar = 0;
		while(queIndex < que.size()){
			//pop que[index]
			TreeExp currentT = que.get(queIndex);
			double thePartialDev = 0;
			if(currentT == this){
				thePartialDev = 1;
				partialDev.put(this, 1.0);
			}
			else{
				thePartialDev = currentT.GetPartialGradient(ifSort, partialDev, varVal,valRec);
				//if(thePartialDev!= 0) {
				//	System.out.println(thePartialDev);
				//}
			}
			if (currentT.expType.equals("LEA") && currentT.term.var != -1) {
				result.put(currentT.term.var, thePartialDev);
			}
			//pop
			queIndex ++;
			if(currentT.expType.equals("LEA") && currentT.term.coefficient == 1.0 && currentT.term.var != -1){
				countVar ++;
				if(countVar >= maxVarSize){
					break;
				}
			}
		}
		//System.out.println(result);
		for(int i = 0; i < varVal.size(); i ++){
			if(!result.containsKey(i)){
				delta.add(0.0);
			}
			else{
				delta.add(result.get(i));
			}
		}
		//PrintDev(que.get(0), partialDev, new HashMap<TreeExp, Boolean>());
		
	}	
	
	//get the gradients for variables using gradient tree
	// used for reverse accumulation
	public double GetPartialGradient(boolean sortGurentee,
			HashMap<TreeExp, Double> partialDev, ArrayList<Double> varVal,
			HashMap<TreeExp, Double> valRec) throws Exception {
		double thePartDev = 0;
		ArrayList<Double> selves = new ArrayList<Double>();
		ArrayList<Double> fathers = new ArrayList<Double>();
		for (TreeExp f : father) {
			if (f == null || !f.ifInQ) {
				continue;
			}
			// the partial gradient of f to this
			double selfDev = 1;
			// the partial gradient of Q to f
			double fatherPartDev = 0;
			if (partialDev.containsKey(f)) {
				fatherPartDev = partialDev.get(f);
			} else {
				if (sortGurentee) {
					throw new Exception("father hasn't been calculated");
				} else {
					fatherPartDev = f.GetPartialGradient(sortGurentee, partialDev, varVal, valRec);
				}
			}
			if (f.expType.equals("SUM")) {
				selfDev = 1;
			} else {
				if (f.expType.equals("PRO")) {
					double logSelfDev = 0;
					boolean ifSelfDev0 = false;
					for (TreeExp sibling : f.subExp) {
						if (sibling.equals(this)) {
							continue;
						}
						double theSibVal = sibling.RealValue(varVal, valRec);
						if(theSibVal < 0) {
							@SuppressWarnings("unused")
							int amiwa = 1;
							throw new Exception("!!!!!!!");
						}
						if(Equal0(theSibVal)) {
							selfDev = 0;
							ifSelfDev0 = true;
							break;
						}
						else {
							logSelfDev += Math.log(theSibVal);
						}
					}
					if(!ifSelfDev0) {
						selfDev = Math.exp(logSelfDev);
					}
				} else {
					if (f.expType.equals("DIV")) {
						if (f.subExp.get(0) == this) {
							selfDev /= f.subExp.get(1)
									.RealValue(varVal, valRec);
						} else {
							selfDev = (0 - f.subExp.get(0).RealValue(varVal,
									valRec))
									/ Math.pow(
											f.subExp.get(1).RealValue(varVal,
													valRec), 2);
						}
					} else {
						if (f.expType.equals("MIN")) {
							if (f.subExp.get(0) == this) {
								selfDev = 1;
							} else {
								selfDev = -1;
							}
						} else {
							if (f.expType.equals("EXP")) {
								selfDev = Math.exp(this.RealValue(varVal,
										valRec));
							} else {
								if (f.expType.equals("SIGMOID")) {
									double a = this.RealValue(varVal, valRec);
									if (a > ACTIVE_THRE) {
										selfDev = Math.pow(10, -5);
									} else {
										if (a < -ACTIVE_THRE) {
											selfDev = -Math.pow(10, -5);
										} else {
											selfDev = Sigmoid(a)
													* (1 - Sigmoid(a));
										}
									}
								} else {
									if (f.expType.equals("THR")) {
										selfDev = 1;
									}
								}
							}
						}
					}
				}
			}
			int countNeg = 0;
			double selfDev_pos = 0;
			double fatherPartDev_pos = 0;
			if(selfDev < 0) {
				countNeg ++;
				selfDev_pos = 0 - selfDev;
			}
			else{
				selfDev_pos = selfDev;
			}
			
			
			if(fatherPartDev < 0) {
				countNeg ++;
				fatherPartDev_pos = 0 - fatherPartDev;
			}
			else {
				fatherPartDev_pos = fatherPartDev;
			}
			
			if(selfDev != 0 && fatherPartDev != 0) {
				double dev = Math.exp(Math.log(selfDev_pos) + Math.log(fatherPartDev_pos));
				if(countNeg % 2 != 0) {
					dev = 0 - dev;
				}
				thePartDev += dev;
			}
			
			selves.add(selfDev);
			fathers.add(fatherPartDev);
		}
		partialDev.put(this, thePartDev);
		return thePartDev;
	}
	
	//Get realvalue of a tree
	public double RealValue(ArrayList<Double> varVal, HashMap<TreeExp, Double> valRec) {

		if(valRec.containsKey(this)){
			return valRec.get(this); 
		}
		else{
			double r = 0;
			double rt = 0;
			if(expType.equals("LEA")){
				if(term.var == -1){
					r = term.coefficient;
				}
				else{
					r = varVal.get(term.var) * term.coefficient;
				}
			}
			else{
				if(expType.equals("SUM")){
					r = 0;
					for(TreeExp subT: subExp){
						double v = subT.RealValue(varVal, valRec);
						r += v;
					}

				}
				else{
					if(expType.equals("MIN")){

						r = subExp.get(0).RealValue(varVal, valRec);

						for(int i = 1; i < subExp.size(); i ++){
							r -= subExp.get(i).RealValue(varVal, valRec);
						}
					}
					else{
						if(expType.equals("PRO")){

							double logR = 0;
							boolean ifRIs0 = false;
							for(TreeExp subT: subExp){
								double v = subT.RealValue(varVal, valRec);
								if(Equal0(v)) {
									ifRIs0 = true;
									r = 0;
									break;
								}
								else {
									logR += Math.log(v);
								}
							}
							if(!ifRIs0) {
								r = Math.exp(logR);
							}
						}
						else{
							if(expType.equals("DIV")){
								double a = subExp.get(0).RealValue(varVal, valRec);
								double b = subExp.get(1).RealValue(varVal, valRec);
								r = a / b;	
							}
							else{
								if(expType.equals("EXP")){
									rt = subExp.get(0).RealValue(varVal, valRec);
									r = Math.exp(rt);
								}
								else if(expType.equals("SIGMOID")){
									double a = subExp.get(0).RealValue(varVal, valRec);
									long start = Runtime.getRuntime().totalMemory();	
									if(a > ACTIVE_THRE){
										r = 1;
									}
									else{
										if(a < -ACTIVE_THRE){
											r = 0;
										}
										else{
											r = Sigmoid(a);
										}
									}
								}
								else{
									if(expType.equals("THR")){
										double a = subExp.get(0).RealValue(varVal, valRec);
										if(a > term.coefficient){
											r = term.coefficient;
										}
										else{
											if(a < term.var){
												r = term.var;
											}
											else{
												r = a;
											}
										}
									}
									else{
										if(expType.equals("POW")){
											double a = subExp.get(0).RealValue(varVal, valRec);
											double b = subExp.get(0).RealValue(varVal, valRec);
											r = Math.pow(a, b);
										}
									}
								}
							}
						}
					}
				}
			}
			valRec.put(this, r);
			return r;
		}
	}
	
	//Get realvalue of a tree
	public double RealValue(HashMap<TreeExp, Double> valRec) {

		
		if (valRec.containsKey(this)) {
			return valRec.get(this);
		} else {
			double r = 0;
			double rt = 0;
			if (expType.equals("LEA")) {
				
				if (term.var == -1) {
					r = term.coefficient;
				} else {
					r = valRec.get(this) * term.coefficient;
				}

			} else {
				if (expType.equals("SUM")) {
					
					r = 0;
					for (TreeExp subT : subExp) {
						double v = subT.RealValue(valRec);
						r += v;
					}
					
					

				} else {
					if (expType.equals("MIN")) {

						r = subExp.get(0).RealValue(valRec);

						for (int i = 1; i < subExp.size(); i++) {
							r -= subExp.get(i).RealValue(valRec);
						}
					} else {
						if (expType.equals("PRO")) {
							/*
							r = 1;
							for(TreeExp subT: subExp){
								double v = subT.RealValue(valRec);
								r *= v;
								if(r == 0){
									break;
								}
							}
							*/
							double logR = 0;
							boolean ifRIs0 = false;
							for(TreeExp subT: subExp){
								double v = subT.RealValue(valRec);
								if(Equal0(v)) {
									ifRIs0 = true;
									r = 0;
									break;
								}
								else {
									logR += Math.log(v);
								}
							}
							if(!ifRIs0) {
								r = Math.exp(logR);
							}
						} else {
							if (expType.equals("DIV")) {
								double a = subExp.get(0).RealValue(valRec);
								double b = subExp.get(1).RealValue(valRec);
								r = a / b;
							} else {
								if (expType.equals("EXP")) {
									rt = subExp.get(0)
											.RealValue(valRec);
									r = Math.exp(rt);
								} else if (expType.equals("SIGMOID")) {
									double a = subExp.get(0).RealValue(valRec);
									long start = Runtime.getRuntime()
											.totalMemory();
									if (a > ACTIVE_THRE) {
										r = 1;
									} else {
										if (a < -ACTIVE_THRE) {
											r = 0;
										} else {
											r = Sigmoid(a);
										}
									}
								} else {
									if (expType.equals("THR")) {
										double a = subExp.get(0).RealValue(valRec);
										if (a > term.coefficient) {
											r = term.coefficient;
										} else {
											if (a < term.var) {
												r = term.var;
											} else {
												r = a;
											}
										}
									} else {
										if (expType.equals("POW")) {
											double a = subExp.get(0).RealValue(valRec);
											double b = subExp.get(0).RealValue(valRec);
											r = Math.pow(a, b);
										}
									}
								}
							}
						}
					}
				}
			}
			valRec.put(this, r);
			return r;
		}
		
		//return new Random().nextDouble();
	}

	//Get realvalue of a tree
	public double RealValue(ArrayList<Integer> theNode, ArrayList<Double> varVal, HashMap<TreeExp, Double> valRec) {

		if (valRec.containsKey(this)) {
			return valRec.get(this);
		} else {
			double r = 0;
			double rt = 0;
			if (expType.equals("LEA")) {
				if (term.var == -1) {
					r = new Double(term.coefficient);
				} else {
					r = varVal.get(theNode.indexOf(term.var)) * term.coefficient;
					//r = 1;
				}
			} else {
				if (expType.equals("SUM")) {
					r = 0;
					for (TreeExp subT : subExp) {
						long start = Runtime.getRuntime().totalMemory();
						double v = subT.RealValue(theNode, varVal, valRec);
						long dure = Runtime.getRuntime().totalMemory() - start;
	
						r += v;
					}

				} else {
					if (expType.equals("MIN")) {

						long start = Runtime.getRuntime().totalMemory();
						r = subExp.get(0).RealValue(theNode, varVal, valRec);
						long dure = Runtime.getRuntime().totalMemory() - start;

						for (int i = 1; i < subExp.size(); i++) {
							start = Runtime.getRuntime().totalMemory();
							r -= subExp.get(i).RealValue(theNode, varVal, valRec);
							dure = Runtime.getRuntime().totalMemory() - start;

						}
					} else {
						if (expType.equals("PRO")) {
							/*
							r = 1;
							for(TreeExp subT: subExp){
								double v = subT.RealValue(theNode, varVal, valRec);
								r *= v;
								if(r == 0){
									break;
								}
							}
							*/
							double logR = 0;
							boolean ifRIs0 = false;
							for(TreeExp subT: subExp){
								double v = subT.RealValue(theNode, varVal, valRec);
								if(Equal0(v)) {
									ifRIs0 = true;
									r = 0;
									break;
								}
								else {
									logR += Math.log(v);
								}
							}
							if(!ifRIs0) {
								r = Math.exp(logR);
							}
						} else {
							if (expType.equals("DIV")) {
								long start = Runtime.getRuntime().totalMemory();
								double a = subExp.get(0).RealValue(theNode, varVal,
										valRec);
								double b = subExp.get(1).RealValue(theNode, varVal,
										valRec);
								long dure = Runtime.getRuntime().totalMemory()
										- start;

								r = a / b;
							} else {
								if (expType.equals("EXP")) {
									long start = Runtime.getRuntime()
											.totalMemory();
									rt = subExp.get(0)
											.RealValue(theNode, varVal, valRec);
									r = Math.exp(rt);
								} else if (expType.equals("SIGMOID")) {
									double a = subExp.get(0).RealValue(theNode, varVal,
											valRec);
									long start = Runtime.getRuntime()
											.totalMemory();
									long dure = Runtime.getRuntime()
											.totalMemory() - start;

									if (a > ACTIVE_THRE) {
										r = 1;
									} else {
										if (a < -ACTIVE_THRE) {
											r = 0;
										} else {
											r = Sigmoid(a);
										}
									}
								} else {
									if (expType.equals("THR")) {
										long start = Runtime.getRuntime()
												.totalMemory();
										double a = subExp.get(0).RealValue(theNode, 
												varVal, valRec);
										long dure = Runtime.getRuntime()
												.totalMemory() - start;

										if (a > term.coefficient) {
											r = term.coefficient;
										} else {
											if (a < term.var) {
												r = term.var;
											} else {
												r = a;
											}
										}
									} else {
										if (expType.equals("POW")) {
											long start = Runtime.getRuntime()
													.totalMemory();
											double a = subExp.get(0).RealValue(theNode, 
													varVal, valRec);
											double b = subExp.get(0).RealValue(theNode, 
													varVal, valRec);
											long dure = Runtime.getRuntime()
													.totalMemory() - start;

											r = Math.pow(a, b);
										}
									}
								}
							}
						}
					}
				}
			}

			valRec.put(this, r);
			return r;
		}
	}
	
	
	public double Sigmoid(double x){
		double a = 1.0/(1.0+Math.exp(0.0-x));
		return a;
	}
	
	/*
	public TreeExp GetTrueHalf(TreeExp theNode){
		if(theNode.father.size() != 1)
	}
	*/
	//see bellow
	public void ClearFather(){
		father.clear();
		if(subExp != null){
			for(TreeExp child: subExp){
			
			child.ClearFather();
			child.father.add(this);
			}
		}
	}
	
	
	
	
	//clear F
	//remove the nodes that are not relatd to F
	public void MarkForQ(){
		this.ifInQ = true;
		if(this.subExp!= null){
			for(TreeExp child: this.subExp){
				if(!child.ifInQ){
					child.MarkForQ();
				}
			}
		}
	}
	public void CountFather(HashMap<TreeExp, Integer> fatherCounter){
		if(father!=null){
			//ArrayList<TreeExp> deletFather = new ArrayList<TreeExp>();
			int fadeFather = 0;
			for(TreeExp f: father){
				if(!f.ifInQ){
					fadeFather ++;
					//f.subExp.remove(this);
				}
			}
			//father.removeAll(deletFather);
			fatherCounter.put(this, father.size() - fadeFather);
		}
		else{
			fatherCounter.put(this, 0);
		}
		if(subExp != null){
			for(TreeExp child: subExp){
				if(!fatherCounter.containsKey(child)){
					child.CountFather(fatherCounter);
				}
			}
		}
	}
	public HashMap<TreeExp, Integer> ClearF(){
		//width first search all nodes starting from 
		//long t00 = System.currentTimeMillis();
		MarkForQ();
		//System.out.println(System.currentTimeMillis() - t00);
		//Search Again to delete fathers that are not in the tree
		//ArrayList<TreeExp> deletFathers = new ArrayList<TreeExp>();
		//t00 = System.currentTimeMillis();
		HashMap<TreeExp, Integer> fatherCounter = new HashMap<TreeExp, Integer>();
		CountFather(fatherCounter);
		//System.out.println(System.currentTimeMillis() - t00);
		return fatherCounter;
	}
	
	public void GenUnsortQue(HashMap<TreeExp, Boolean> ifVisited, ArrayList<TreeExp> res){
		res.add(this);
		ifVisited.put(this, true);
		if(subExp != null){
			for(TreeExp child: subExp){
				if(!ifVisited.containsKey(child)){
					child.GenUnsortQue(ifVisited, res);
				}
			}
		}
	}
	
	//topological sorting
	public ArrayList<TreeExp> TopologQueue(boolean ifSort) {
		ArrayList<TreeExp> queue = new ArrayList<TreeExp>();
		if(ifSort){
			HashMap<TreeExp, Integer> fatherCounter = ClearF();
			//long t00 = System.currentTimeMillis();
			ArrayList<TreeExp> noFather = new ArrayList<TreeExp>();
			//HashMap<TreeExp, Integer> fatherCounter = new HashMap<TreeExp, Integer>();
			noFather.add(this);
			// this could be dangeous
			int noFatherCounter = 0;
			while (noFatherCounter != noFather.size()) {
				// pop out the first item of noFather
				TreeExp currentNoF = noFather.get(noFatherCounter);
				if(currentNoF.term == null || (currentNoF.term != null) && currentNoF.term.var != -1){
					 numOfNonLeaf ++;
				}
				queue.add(currentNoF);
				
				if (!currentNoF.expType.equals("LEA")) {
					for (TreeExp child : currentNoF.subExp) {
						if(currentNoF == this && !fatherCounter.containsKey(child)){
							continue;
						}
						int fSize = fatherCounter.get(child);

						if (fSize == 1) {
							fatherCounter.put(child, 0);
							noFather.add(child);

						} else {
							if (fSize != 0) {
								fatherCounter.put(child, fSize - 1);
							}
						}
					}
				}
				noFatherCounter++;
			}
			//System.out.println(System.currentTimeMillis() - t00);
			/*
			 * Iterator<?> iter = fatherCounter.entrySet().iterator(); while
			 * (iter.hasNext()) { Map.Entry entry = (Map.Entry) iter.next();
			 * TreeExp key = (TreeExp)entry.getKey();
			 * 
			 * if(key.expType.equals("LEA") && key.term.var!=-1){
			 * queue.add(key); } }
			 */
		}
		else{
			MarkForQ();
			GenUnsortQue(new HashMap<TreeExp, Boolean>(), queue);
		}
		return queue;
	}
	
	private void PrintDev(TreeExp Q, HashMap<TreeExp, Double> dev, HashMap<TreeExp, Boolean> ifPrinted){
		printCounter ++;
		System.out.println(printCounter + " " + dev.get(Q));
		ifPrinted.put(Q, true);
		if(Q.subExp!=null)
		for(TreeExp child: Q.subExp){
			if(!ifPrinted.containsKey(child))
			PrintDev(child, dev, ifPrinted);
		}
	}
	
	
	
	public double ToNumber(){
		if(expType.equals("LEA") && term.var == -1){
			return term.coefficient;
		}
		else{
			return Double.NaN;
		}
	}
	
	public TreeExp ADD(TreeExp another){


		if(new Global().ifLift){
			if(PlusHash.containsKey(this) && PlusHash.get(this).containsKey(another)){
				//System.out.println("hit");
				return PlusHash.get(this).get(another);
			}
			if(PlusHash.containsKey(another) && PlusHash.get(another).containsKey(this)){
				//System.out.println("hit");
				return PlusHash.get(another).get(this);
			}
		}

		TreeExp res = null;
		double thisD = this.ToNumber();
		double d = another.ToNumber();
		//if any addent is 0 simply return the other one
		if (!Double.isNaN(thisD) && Equal0(thisD)) {
			res = another;
		} else {
			if (!Double.isNaN(d) && Equal0(d)) {
				res = this;
			} else {
				//if the two addent are both numbers, just add them
				if(!Double.isNaN(d) && !Double.isNaN(thisD)){
					
					res = BuildNewTreeExp(thisD + d, null);
				}
				else{
					// if this is already a sum node 
					if (expType.equals("SUM") && father == null) {
						
						subExp.add(another);
						if (another.father == null) {
							another.father = new ArrayList<TreeExp>();
						}
						another.father.add(this);
						res = this;
						
					} else {
						if(this == another){
							res = this.TIMES(BuildNewTreeExp(2.0, null));
						}
						else{
							if(this.expType.equals("PRO") && this.subExp.size() == 2 ){
								if(this.subExp.get(0) == another && this.subExp.get(1).expType.equals("LEA")
										&& !Double.isNaN(this.subExp.get(1).term.coefficient)){
									this.subExp.get(1).term.coefficient ++;
									res = this;
								}
								else{
									if(this.subExp.get(1) == another && this.subExp.get(0).expType.equals("LEA") 
								
										&& !Double.isNaN(this.subExp.get(0).term.coefficient)){
										this.subExp.get(0).term.coefficient ++;
										res = this;
									}
									else{
										TreeExp newExp = new TreeExp("SUM", null);
										newExp.subExp.add(this);
										newExp.subExp.add(another);
										if (another.father == null) {
											another.father = new ArrayList<TreeExp>();
										}
										another.father.add(newExp);
										if (father == null) {
											father = new ArrayList<TreeExp>();
										}
										father.add(newExp);
										res = newExp;
									}
								}
							}
							else{
								TreeExp newExp = new TreeExp("SUM", null);
								newExp.subExp.add(this);
								newExp.subExp.add(another);
								if (another.father == null) {
									another.father = new ArrayList<TreeExp>();
								}
								another.father.add(newExp);
								if (father == null) {
									father = new ArrayList<TreeExp>();
								}
								father.add(newExp);
								res = newExp;
							}
						}
					}
				}
			}
		}
		if(new Global().ifLift){
			if(!PlusHash.containsKey(this)){
				PlusHash.put(this, new HashMap<Object, TreeExp>());
			}
			PlusHash.get(this).put(another, res);
		}

		return res;
	}
	
	public TreeExp MINUS(TreeExp another){

		if(new Global().ifLift){
			if(MinusHash.containsKey(this) && MinusHash.get(this).containsKey(another)){
				//System.out.println("hit");
				return MinusHash.get(this).get(another);
			}
		}
		double thisD = this.ToNumber();
		double d = another.ToNumber();
		TreeExp res = null;
		if (!Double.isNaN(d) && Equal0(d)) {
			res = this;
		} else {
			// if the two addent are both numbers, just add them
			if (!Double.isNaN(d) && !Double.isNaN(thisD)) {
				res = TreeExp.BuildNewTreeExp(thisD - d, null);
			} else {
				if (expType.equals("MIN") && father == null) {
					
					subExp.add(another);
					if (another.father == null) {
						another.father = new ArrayList<TreeExp>();
					}
					another.father.add(this);
					res = this;
					
				} else {
					if(this == another){
						res = BuildNewTreeExp(0.0, null);
					}
					else{
						TreeExp newExp = new TreeExp("MIN", null);
						newExp.subExp.add(this);
						newExp.subExp.add(another);
						if (another.father == null) {
							another.father = new ArrayList<TreeExp>();
						}
						another.father.add(newExp);
						if (father == null) {
							father = new ArrayList<TreeExp>();
						}
						father.add(newExp);
						res = newExp;
					}
				}
			}
		}
		if(new Global().ifLift){
			if(!MinusHash.containsKey(this)){
				MinusHash.put(this, new HashMap<Object, TreeExp>());
			}
			MinusHash.get(this).put(another, res);
		}

		return res;
	}
	
	public TreeExp TIMES(TreeExp another){

		if(new Global().ifLift){
			if(TimesHash.containsKey(this) && TimesHash.get(this).containsKey(another)){
				//System.out.println("hit");
				return TimesHash.get(this).get(another);
			}
			if(TimesHash.containsKey(another) && TimesHash.get(another).containsKey(this)){
				//System.out.println("hit");
				return TimesHash.get(another).get(this);
			}
		}
		TreeExp res = null;
		double thisD = this.ToNumber();
		double d = another.ToNumber();
		if((!Double.isNaN(d) && Equal0(d)) || (!Double.isNaN(thisD) && Equal0(thisD))){
			res = TreeExp.BuildNewTreeExp(0.0, null);
		}
		else{
			if((!Double.isNaN(d) && d == 1)){
				res = this;
			}
			else{
				if((!Double.isNaN(thisD) && thisD == 1)){
					res = another;
				}
				else{
					if(!Double.isNaN(thisD) && !Double.isNaN(d)){
						res = TreeExp.BuildNewTreeExp(thisD * d, null);
					}
					else{
						if(expType.equals("PRO") && father == null){
							subExp.add(another);
							if(another.father == null){
								another.father = new ArrayList<TreeExp>();
							}
							another.father.add(this);
							res = this;
						}
						else{
							if(this == another){
								res = this.POW(TreeExp.BuildNewTreeExp(2.0, null));
							}
							else{
								if(this.expType.equals("POW") && this.subExp.size() == 2 &&
									this.subExp.get(0) == another && this.subExp.get(1).expType.equals("LEA") && 
									!Double.isNaN(this.subExp.get(1).term.coefficient)){
									this.subExp.get(1).term.coefficient ++;
									res = this;
								}
								else{
									TreeExp newExp = new TreeExp("PRO", null);

								
									newExp.subExp.add(this);
									newExp.subExp.add(another);
									if(another.father == null){
										another.father = new ArrayList<TreeExp>();
									}
									another.father.add(newExp);
									if(father == null){
										father = new ArrayList<TreeExp>();
									}
									father.add(newExp);
									res = newExp;
								}
							}
						}
					}
				}
			}
		}
		if(new Global().ifLift){
			if(!TimesHash.containsKey(this)){
				TimesHash.put(this, new HashMap<Object, TreeExp>());
			}
			TimesHash.get(this).put(another, res);
		}

		return res;
	}
	
	public TreeExp DIVID(TreeExp another){

		if(new Global().ifLift){
			if(DividHash.containsKey(this) && DividHash.get(this).containsKey(another)){
				//System.out.println("hit");
				return DividHash.get(this).get(another);
			}
		}
		double thisD = this.ToNumber();
		double d = another.ToNumber();
		TreeExp res = null;
		if(!Double.isNaN(thisD) && Equal0(thisD)){
			res = TreeExp.BuildNewTreeExp(0.0, null);
		}
		else{
			if(!Double.isNaN(d) && d == 1){
				res = this;
			}
			else{
				if(!Double.isNaN(d) && d != 0 && !Double.isNaN(thisD)){
					res = TreeExp.BuildNewTreeExp(thisD / d, null);
				}
				else{
					if(this == another){
						res = BuildNewTreeExp(1.0, null);
					}
					else{
						TreeExp newExp = new TreeExp("DIV", null);
						newExp.subExp.add(this);
						newExp.subExp.add(another);
						if(another.father == null){
							another.father = new ArrayList<TreeExp>();
						}
						another.father.add(newExp);
						if(father == null){
							father = new ArrayList<TreeExp>();
						}
						father.add(newExp);
						res = newExp;
					}
				}
			}
		}
		if(new Global().ifLift){
			if(!DividHash.containsKey(this)){
				DividHash.put(this, new HashMap<Object, TreeExp>());
			}
			DividHash.get(this).put(another, res);
		}

		return res;
	}
	
	public TreeExp EXP(){
		TreeExp newExp = new TreeExp("EXP", null);
		newExp.subExp.add(this);
		if (father == null) {
			father = new ArrayList<TreeExp>();
		}
		father.add(newExp);
		return newExp;
	}
	
	public TreeExp SIG(){
		TreeExp newExp = new TreeExp("SIGMOID", null);
		newExp.subExp.add(this);
		if (father == null) {
			father = new ArrayList<TreeExp>();
		}
		father.add(newExp);
		return newExp;
	}
	

	
	
	public TreeExp POW(TreeExp another) {
		TreeExp newExp = new TreeExp("POW", null);
		newExp.subExp.add(this);
		newExp.subExp.add(another);
		if (father == null) {
			father = new ArrayList<TreeExp>();
		}
		father.add(newExp);
		if (another.father == null) {
			another.father = new ArrayList<TreeExp>();
		}
		another.father.add(newExp);
		return newExp;
	}
	
	public int Size(ArrayList<TreeExp> visited){
		
		int r = 0;
		if(visited.contains(this)){
			r = 0;
			return r;
		}
		else{
			if(expType.equals("LEA")){
				r = 1;
			}
			else{
				r = 0;
				for(TreeExp subT: subExp){
					r += subT.Size(visited);
					
				}
				r ++;
			}
		}
		visited.add(this);
		return r;
	}
	
	//Given a list and a node
	//starting from the node, traverse all the noodes that it reaches
	//terminating the search if encountering a node in the list
	//return the list - all reaching nodes
	public void RemoveUsefulSingleLayer(ArrayList<TreeExp> nodes, TreeExp reward){
		//width first search starting from reward
		ArrayList<TreeExp> queue = new ArrayList<TreeExp>();
		ArrayList<TreeExp> visited = new ArrayList<TreeExp>();
		queue.add(reward);
		int index = 0;
		while(index < queue.size()){
			TreeExp head = queue.get(index);
			visited.add(head);
			for(TreeExp child: head.subExp){
				//if the child in already in nodes meaning the search terminates here
				//if a node is already searched no need to search it again
				if(!nodes.contains(child) && !visited.contains(child)){
					queue.add(child);
				}
			}
			index ++;
		}
		//all visited anodes are reachable from reward
		//this means those nodes are useful, remove them from gabage list
		nodes.removeAll(visited);
	}
	
	
	//exp is yet to come
	public String toString(){
		if (expType.equals("LEA")) {
			if (term.var == -1) {
				return String.valueOf(term.coefficient);
			} else {
				return "v" + String.valueOf(term.var);
			}
		} else {
			if (expType.equals("PRO")) {
				String s = new String();
				s += "(";
				for (TreeExp subT : subExp) {
					s += (subT.toString());
					if (subExp.indexOf(subT) != subExp.size() - 1) {
						s += " ";
					}
				}
				s += ")";
				return "(* " + s + ")";
			} else {
				if (expType.equals("SUM")) {
					String s = new String();
					s += "(";
					for (TreeExp subT : subExp) {
						s += (subT.toString());
						if (subExp.indexOf(subT) != subExp.size() - 1) {
							s += " ";
						}
					}
					s += ")";
					return "(+ " + s + ")";
				} else {
					if (expType.equals("MIN")) {
						String s = new String();
						s += "(";
						for (TreeExp subT : subExp) {
							s += (subT.toString());
							if (subExp.indexOf(subT) != subExp.size() - 1) {
								s += " ";
							}
						}
						s += ")";
						return "(- " + s + ")";
					} else {
						if (expType.equals("DIV")) {
							String s = new String();
							s += "(";
							for (TreeExp subT : subExp) {
								s += (subT.toString());
								if (subExp.indexOf(subT) != subExp.size() - 1) {
									s += " ";
								}
							}
							s += ")";
							return "(/ " + s + ")";
						} else {
							if (expType.equals("EXP")) {
								String s = new String();
								s += "e^{";
								for (TreeExp subT : subExp) {
									s += (subT.toString());
									if (subExp.indexOf(subT) != subExp.size() - 1) {
										s += " ";
									}
								}
								s += "}";
								return s ;
							}
							else{
								if (expType.equals("THR")) {
									String s = new String();
									s += "THR{";
									for (TreeExp subT : subExp) {
										s += (subT.toString());
										if (subExp.indexOf(subT) != subExp.size() - 1) {
											s += " ";
										}
									}
									s += "}";
									return s ;
								}
								else{
									if (expType.equals("MIN")) {
										String s = new String();
										s += "MIN{";
										for (TreeExp subT : subExp) {
											s += (subT.toString());
											if (subExp.indexOf(subT) != subExp.size() - 1) {
												s += " ";
											}
										}
										s += "}";
										return s ;
									}
									else{
										if (expType.equals("POW")) {
											String s = new String();
											s += subExp.get(0).toString();
											s += " ^ ";
											s += subExp.get(1).toString();
											return s ;
										}
										else{
											if (expType.equals("SIGMOID")) {
												String s = new String();
												s += "SIG{";
												s += subExp.get(0).toString();
												s += "}";
												return s ;
											}
										}
									}
								}
							}
						}
					}
				}
			}	
		}
		return new String();
	}
	
	//return a new tree which is the sum of true/false brance of the tree
	TreeExp SumOverI(HashMap<Integer, Boolean> evidence, int i){
		
		if(evidence.containsKey(i)){
			if(evidence.get(i)){
				HashMap<TreeExp, TreeExp> copyMapForTrue = new HashMap<>();
				TreeExp trueBranch = Copy(copyMapForTrue);
				ArrayList<TreeExp> searched = new ArrayList<>();
				trueBranch.Plugin(searched, i, true);
				return trueBranch;
			}
			else{
				HashMap<TreeExp, TreeExp> copyMapForFalse = new HashMap<>();
				TreeExp falseBranch = Copy(copyMapForFalse);
				ArrayList<TreeExp> searched = new ArrayList<>();
				falseBranch.Plugin(searched, i, false);
				return falseBranch;
			}
		}
		else{
			HashMap<TreeExp, TreeExp> copyMapForTrue = new HashMap<>();
			HashMap<Integer, TreeExp> varMapForTrue = new HashMap<Integer, TreeExp>();
			TreeExp trueBranch = Copy(copyMapForTrue);
			
			HashMap<TreeExp, TreeExp> copyMapForFalse = new HashMap<>();
			HashMap<Integer, TreeExp> varMapForFalse = new HashMap<Integer, TreeExp>();
			TreeExp falseBranch = Copy(copyMapForFalse);
			ArrayList<TreeExp> searched = new ArrayList<>();
			trueBranch.Plugin(searched, i, true);
			searched = new ArrayList<>();
			falseBranch.Plugin(searched, i, false);
			return trueBranch.ADD(falseBranch);
		}
	}
	
	TreeExp SumOverIWtChangeList(HashMap<Integer, Boolean> evidence, int i, ArrayList<Integer> changeList){
		
		if(evidence.containsKey(i)){
			if(evidence.get(i)){
				HashMap<TreeExp, TreeExp> copyMapForTrue = new HashMap<>();
				TreeExp trueBranch = Copy(changeList, copyMapForTrue);
				ArrayList<TreeExp> searched = new ArrayList<>();
				trueBranch.Plugin(searched, i, true);
				return trueBranch;
			}
			else{
				HashMap<TreeExp, TreeExp> copyMapForFalse = new HashMap<>();
				TreeExp falseBranch = Copy(changeList, copyMapForFalse);
				ArrayList<TreeExp> searched = new ArrayList<>();
				falseBranch.Plugin(searched, i, false);
				return falseBranch;
			}
		}
		else{
			HashMap<TreeExp, TreeExp> copyMapForTrue = new HashMap<>();
			HashMap<Integer, TreeExp> varMapForTrue = new HashMap<Integer, TreeExp>();
			TreeExp trueBranch = Copy(changeList, copyMapForTrue);
			
			HashMap<TreeExp, TreeExp> copyMapForFalse = new HashMap<>();
			HashMap<Integer, TreeExp> varMapForFalse = new HashMap<Integer, TreeExp>();
			TreeExp falseBranch = Copy(changeList, copyMapForFalse);
			ArrayList<TreeExp> searched = new ArrayList<>();
			trueBranch.Plugin(searched, i, true);
			searched = new ArrayList<>();
			falseBranch.Plugin(searched, i, false);
			return trueBranch.ADD(falseBranch);
		}
	}
	
	TreeExp ReplaceNodes(int i, int j, ArrayList<Integer> changeList, HashMap<Integer, TreeExp> variabels){

		HashMap<TreeExp, TreeExp> copyMapForTrue = new HashMap<>();
		TreeExp trueBranch = Copy(changeList, copyMapForTrue);
		ArrayList<TreeExp> searched = new ArrayList<>();
		trueBranch.Plugin(searched, i, j, variabels);
		return trueBranch;
	}
	
	//constructor for a variable and father
	// only used for THRE node
	public TreeExp(String type, double c, int v, ArrayList<TreeExp> sub) {

		expType = new String(type);
		term = new Term(c, v);
		subExp = sub;
		father = new ArrayList<TreeExp>();
	}
	
	public TreeExp(String type, Term t, ArrayList<TreeExp> sub) {

		expType = new String(type);
		term = t;
		subExp = sub;
		father = new ArrayList<TreeExp>();
	}
	
	public TreeExp Copy(HashMap<TreeExp, TreeExp> copyMap, HashMap<Integer, TreeExp> varCollector){
		if(copyMap.containsKey(this)){
			return copyMap.get(this);
		}
		ArrayList<TreeExp> sub = new ArrayList<>();
		if(subExp == null){
			sub = null;
		}
		else{
			for(int i = 0; i < subExp.size(); i ++){
				TreeExp newChild = subExp.get(i).Copy(copyMap, varCollector);
				sub.add(newChild);
			}
		}
		Term newTerm = null;
		if(term != null){
			newTerm = new Term(term.coefficient, term.var);
		}
		TreeExp theNewNode = new TreeExp(expType, newTerm, sub);
		if(theNewNode.term != null && theNewNode.term.var != -1){
			//if(!varCollector.containsKey(theNewNode.term.var)){
			varCollector.put(theNewNode.term.var, theNewNode);
		}
		if(sub != null){
			for(int i = 0; i < sub.size(); i ++){
				sub.get(i).father.add(theNewNode);
			}
		}
		copyMap.put(this, theNewNode);
		return theNewNode;
	}
	
	public TreeExp Copy(HashMap<TreeExp, TreeExp> copyMap){
		if(copyMap.containsKey(this)){
			return copyMap.get(this);
		}
		ArrayList<TreeExp> sub = new ArrayList<>();
		if(subExp == null){
			sub = null;
		}
		else{
			for(int i = 0; i < subExp.size(); i ++){
				TreeExp newChild = subExp.get(i).Copy(copyMap);
				sub.add(newChild);
			}
		}
		Term newTerm = null;
		if(term != null){
			newTerm = new Term(term.coefficient, term.var);
		}
		TreeExp theNewNode = new TreeExp(expType, newTerm, sub);

		if(sub != null){
			for(int i = 0; i < sub.size(); i ++){
				sub.get(i).father.add(theNewNode);
			}
		}
		copyMap.put(this, theNewNode);
		return theNewNode;
	}
	
	public TreeExp Copy(ArrayList<Integer> chaneList, HashMap<TreeExp, TreeExp> copyMap){
		if(copyMap.containsKey(this)){
			return copyMap.get(this);
		}
		
		if(term != null && term.var != -1 && !chaneList.contains(term.var)){
			return this;
		}
		ArrayList<TreeExp> sub = new ArrayList<>();
		if(subExp == null){
			sub = null;
		}
		else{
			for(int i = 0; i < subExp.size(); i ++){
				TreeExp newChild = subExp.get(i).Copy(chaneList, copyMap);
				sub.add(newChild);
			}
		}
		Term newTerm = null;
		if(term != null){
			newTerm = new Term(term.coefficient, term.var);
		}
		TreeExp theNewNode = new TreeExp(expType, newTerm, sub);

		if(sub != null){
			for(int i = 0; i < sub.size(); i ++){
				sub.get(i).father.add(theNewNode);
			}
		}
		
		copyMap.put(this, theNewNode);
		return theNewNode;
	}
	
	public void ToNumberNode(double c){

		this.term = new Term(c);
		this.expType = "LEA";
		if(subExp != null)
		for(int i = 0; i < subExp.size(); i ++){
			
			if(subExp.get(i).father.contains(this)){
				int index = subExp.get(i).father.indexOf(this);
				subExp.get(i).father.remove(index);
			}
			
		}
		this.subExp = null;
	}
	
	public void ReplaceTree(TreeExp newTree){
		if(this != newTree){
			for(int i = 0; i < father.size(); i ++){
				TreeExp theF = father.get(i);
				if(theF.subExp != null && theF.subExp.contains(this)) {
					theF.subExp.set(theF.subExp.indexOf(this), newTree);
				}
				
				if(!newTree.father.contains(theF))
				newTree.father.add(theF);
			}
			
			father = new ArrayList<TreeExp>();
		}
		
		
		
		/*
		expType = new String(newTree.expType);
		if(newTree.term != null){
			term = new Term(newTree.term);
		}
		else{
			term = null;
		}
		subExp = new ArrayList<>();
		if(newTree.subExp != null){
			for(TreeExp theSub: newTree.subExp){
				subExp.add(theSub);
				theSub.father.add(this);
			}
		}
		else{
			subExp = null;
		}
		*/
	}
	
	public long FindVarID(ArrayList<TreeExp> searched, int var){
		if(searched.contains(this)){
			return -1;
		}
		if(term != null && term.var == var){
			//specificVarID = ;
			//System.out.println(specificVarID);
			return System.identityHashCode(this);
		}
		long returnVal = -1;
		if(subExp != null)
		for(int i = 0; i < subExp.size(); i ++){
			TreeExp theSub = subExp.get(i);
			long res = theSub.FindVarID(searched, var);
			if(res != -1){
				returnVal = res;
				return res;
			}
		}
		searched.add(this);
		return -1;
	}
	
	public boolean Equal0(double a ) {
		return ((a - 0.0) < Math.exp(-600.0)) && ((0.0 - a) < Math.exp(-600.0));
	}
	
	public boolean Is0(){
		return term != null && Equal0(term.coefficient) && term.var == -1;
	}
	
	public boolean Is1(){
		return term != null && term.coefficient == 1.0 && term.var == -1;
	}
	
	public boolean IsVar(){
		return term != null && term.var != -1;
	}
	
	public boolean IsNum(){
		return term != null && term.var == -1;
	}
	
	
	public void Prunning(HashMap<TreeExp, Boolean> Searched, HashMap<Integer, TreeExp> varCollector) throws Exception{
		if(Searched.containsKey(this)){
			return;
		}
		
		//System.out.println(this);
		// if this node is a leaf
		if(subExp != null){
			for(int i = 0; i < subExp.size(); i ++){
				TreeExp theSub = subExp.get(i);
				theSub.Prunning(Searched, varCollector);
			}
			boolean ifAllNumber = true;
			boolean ifContain0 = false;
			boolean ifContain1 = false;
			int indexOf1 = 0;

			for(int i = 0; i < subExp.size(); i ++){
				// if the su is operator, prunning on it
				// or if the sub is a variable, prunning on it
				TreeExp theSub = subExp.get(i);
				if(!theSub.IsNum()){
					ifAllNumber = false;
				}
				
				if(theSub.Is0()){
					ifContain0 = true;
				}
				if(theSub.Is1()){
					ifContain1 = true;
					indexOf1 = i;
				}
			}
			// if all subs are numbers
			if(ifAllNumber){
				double v = this.RealValue(new ArrayList<Double>(), new HashMap<TreeExp, Double>());
				//create new node to replace this node
				/*
				TreeExp newTree = new TreeExp(v, null);
				for(int j = 0; j < father.size(); j ++){
					TreeExp theF = father.get(j);
					int index = theF.subExp.indexOf(this);
					theF.subExp.remove(index);
					theF.subExp.add(index, newTree);
				}
				father = new ArrayList<>();
				*/
				this.ToNumberNode(v);
			}
			
			
			if(expType.equals("PRO") && ifContain0){

				this.ToNumberNode(0.0);
			}
			
			if(expType.equals("PRO") && ifContain1 && subExp.size() == 2){
				int repalceIndex = 1 - indexOf1;
				TreeExp theSub = subExp.get(repalceIndex);
				this.ReplaceTree(theSub);
			}
		}
		Searched.put(this, true);
	}
	
	
	public void Plugin(ArrayList<TreeExp> searched, int i, Boolean val){
		if(searched.contains(this)){
			return;
		}
		searched.add(this);
		int trueNode = i * 2;
		int falseNode = i * 2 + 1;
		if(this.term != null && term.var == trueNode){
			term.var = -1;
			if(val){
				term.coefficient = 1.0;
			}
			else{
				term.coefficient = 0.0;
			}
		}
		if(term != null && term.var == falseNode){
			term.var = -1;
			if(val){
				term.coefficient = 0.0;
			}
			else{
				term.coefficient = 1.0;
			}
		}
		if(subExp != null){
			for(int k = 0; k < subExp.size(); k ++){
				subExp.get(k).Plugin(searched, i, val);
			}
		}
		searched.add(this);
	}
	
	public void Plugin(ArrayList<TreeExp> searched, int i, int j, HashMap<Integer, TreeExp> variables){
		if(searched.contains(this)){
			return;
		}
		searched.add(this);
		int trueNode = i * 2;
		int falseNode = i * 2 + 1;
		int newTrueNode = j * 2;
		int newFalseNode = j * 2 + 1;
		if(this.term != null && term.var == trueNode){
			this.ReplaceTree(variables.get(newTrueNode));
		}
		if(term != null && term.var == falseNode){
			this.ReplaceTree(variables.get(newFalseNode));
		}
		if(subExp != null){
			for(int k = 0; k < subExp.size(); k ++){
				subExp.get(k).Plugin(searched, i, j, variables);
			}
		}
		searched.add(this);
	}
	/*
	public void PluginWtKnownNodes(ArrayList<TreeExp> var, int i, Boolean val){

		searched.add(this);
		int trueNode = i * 2;
		int falseNode = i * 2 + 1;
		if(this.term != null && term.var == trueNode){
			term.var = -1;
			if(val){
				term.coefficient = 1.0;
			}
			else{
				term.coefficient = 0.0;
			}
		}
		if(term != null && term.var == falseNode){
			term.var = -1;
			if(val){
				term.coefficient = 0.0;
			}
			else{
				term.coefficient = 1.0;
			}
		}
		if(subExp != null){
			for(int k = 0; k < subExp.size(); k ++){
				subExp.get(k).Plugin(searched, i, val);
			}
		}
	}
	*/
	// for plugin values of variables
	public void Plugin(ArrayList<TreeExp> searched, int i, double val){
		if(searched.contains(this)){
			return;
		}
		searched.add(this);
		int trueNode = i * 2;
		int falseNode = i * 2 + 1;
		if(term != null && term.var == trueNode){
			term.var = -1;
			term.coefficient = val;
		}
		if(term != null && term.var == falseNode){
			term.var = -1;
			term.coefficient = 1.0 - val;
		}
		if(subExp != null){
			for(int k = 0; k < subExp.size(); k ++){
				subExp.get(k).Plugin(searched, i, val);
			}
		}
	}
	
	public void Plugin(ArrayList<TreeExp> searched, int i, TreeExp val){
		if(searched.contains(this)){
			return;
		}
		searched.add(this);
		int trueNode = i * 2;
		int falseNode = i * 2 + 1;
		if(this.term != null && term.var == trueNode){
			//each father replace this child
			int fatherSize = father.size();
			for(int j = 0; j < fatherSize; j ++){
				TreeExp theF = father.get(j);
				ArrayList<TreeExp> theSub = theF.subExp;
				int currentIndex = theSub.indexOf(this);
				theF.subExp.set(currentIndex, val);
				if(val.father == null){
					val.father = new ArrayList<>();
				}
				val.father.add(theF);
			}
			father = new ArrayList<>();
		}
		if(term != null && term.var == falseNode){
			//each father replace this child
			int fatherSize = father.size();
			for(int j = 0; j < father.size(); j ++){
				TreeExp theF = father.get(j);
				ArrayList<TreeExp> theSub = theF.subExp;
				int currentIndex = theSub.indexOf(this);
				TreeExp falseVal = BuildNewTreeExp(1.0, null).MINUS(val);
				theF.subExp.set(currentIndex, falseVal);
				if(falseVal.father == null){
					falseVal.father = new ArrayList<>();
				}
				falseVal.father.add(theF);
			}
			father = new ArrayList<>();
		}
		if(subExp != null){
			for(int k = 0; k < subExp.size(); k ++){
				subExp.get(k).Plugin(searched, i, val);
			}
		}
	}
}


