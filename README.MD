-------------------------------------------------------------------------
OVERVIEW

AGS is an approximate Marginal MAP Solver. The idea is motivated by SOGBOFA (https://github.com/hcui01/SOGBOFA), a symbolica gradint based online planner for MDPs. We observe that SOGBFOA can be directly used for Marginal MAP Inference on a special form of graphical models. We then invent a reduction which can turn any graphical model into such form, and use sogbofa to solve it. On UAI challenge problems, AGS is competitive to the state-of-the-art. On GM that have a large number of sum nodes, AGS outperforms the state-of-the-art with significant margin.
   
Please cite our paper if you are using the planner as a baseline, and/or star the project if you like it ^_^

-------------------------------------------------------------------------

RELATED PAPERS

* Hao Cui, Radu Marinescu and Roni Khardon, From Stochastic Planning to Marginal MAP, Proceedings of the Conference on Neural Information Processing Systems (NIPS), 2018

You can easily find any paper of your interest at Hao Cui's personal page
   https://sites.google.com/view/hao-cui/home
   
-------------------------------------------------------------------------
HOW TO USE

* Prerequisites: Java SE 1.8 or higher
* Compile
  In the SOGBOFA/ directory, type command ./compile
* Run AGS on UAI files
  ./run_AGS UAI-dir instance-name ratio-MAP num-rounds timeout 
  For example: 
  ./run_AGS instances/ 50-12-1 0.1 10 10
  Note that the time out is in seconds. UAI probelms are sum-inference problems, so for each run, the system randomly generates MAP  nodes for the problem. The ratio-MAP argument is the ratio of MAP nodes to the size of the GM.
* Run AGS on random problems
  ./run_AGS num-nodes num-MAP num-evidence num-rounds timeout 
  For example: 
  ./run_AGS 100 10 10 10 10
  For this mode, the system generates a GM randomly with pre-fxied total number of nodes, number of MAP nodes and number of evidence nodes, for each run.
* Results
  After finishing the requested number of runs, a result files will be recorded in the bin/ folder, with the following names
  Evaluation_instance-name_timeout, if running with UAI files
  Evaluation_num-nodes_num-evidence_num-MAP_timeout, if running with random problems

-------------------------------------------------------------------------
UAI FILES READY TO USE

Please find all the instance files that we used in the NeurIPS 2018 paper in the instances/ folder.

-------------------------------------------------------------------------
OTHER WORK

Checkout the other project that uses the similar idea of "Aggregate Simulation", but work on MDP planning. 
  Link (https://github.com/hcui01/SOGBOFA)
