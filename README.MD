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
HOW TO USE AGS

* Prerequisites: Java SE 1.8 or higher
* Compile
  In the SOGBOFA/ directory, type command ./compile
* Run AGS and MPBP on UAI files
  ./run_AGS UAI-dir instance-name ratio-MAP num-rounds timeout 
  For example: 
  ./run_AGS instances/ 50-12-1 0.1 10 10
  Note that the time out is in seconds. UAI probelms are sum-inference problems, so for each run, the system randomly generates MAP  nodes for the problem. The ratio-MAP argument is the ratio of MAP nodes to the size of the GM.
* Run AGS and MPBP on random problems
  ./run_AGS num-nodes num-MAP num-evidence num-rounds timeout 
  For example: 
  ./run_AGS 100 10 10 10 10
  For this mode, the system generates a GM randomly with pre-fxied total number of nodes, number of MAP nodes and number of evidence nodes, for each run.
* Evaluation
  After running with AGS and MPBP on each problem, the system evaluates the solutions by excuting a belief propagation and performing sum-inference on the evidence nodes. 
* Results
  After finishing the requested number of runs, a result files will be recorded in the bin/ folder, with the following names
  Evaluation_instance-name_timeout, if running with UAI files
  Evaluation_num-nodes_num-evidence_num-MAP_timeout, if running with random problems
  The ouputs to the terminal are recorded into these files. Such outputs include running statistics and results for AGS.
  For each problem, the system also records the solutions of AGS and BP to this problem. The solutions files are in bin/instance-name/Solution/AGS/timeout

-------------------------------------------------------------------------
UAI FILES READY TO USE

Please find all the instance files that we used in the NeurIPS 2018 paper in the instances/ folder.

-------------------------------------------------------------------------
Run the AAOBF Algorithm

We only provide the binary files for AAOBF in the AAOBF/ foler. To reproduce the results of AAOBF in our paper, enter the folder and use the following command:
./timeout -t ${timeOut} -m ${memOut} ./mmap-solver --input-file ${dir}${prob}.uai --evidence-file ${dir}${prob}.uai.evid --map-file ${dir}${prob}.uai.map --algorithm any-aaobf --heuristic wmb-mm --ibound 10 --seed 12345678 --verbose --positive
where the ${timeOut} is the inference time (seconds), $memOut$ is the maximum memory (KB), ${dir} is the directory of the problem files, and ${pro} is the domain name. For example, 
./timeout -t 10 -m ${memOut} ./mmap-solver --input-file instances/50-20-1.uai --evidence-file instances/50-20-1.uai.evid --map-file instances/50-20-1.uai.map --algorithm any-aaobf --heuristic wmb-mm --ibound 10 --seed 12345678 --verbose --positive

The aaobf algorithm is anytime, so it prints the anytime lower bound of the evaluation, as follows:

--- Starting search ---
[0.01] w        1          378          378          378            0      99.2043 (8.24364e-44)      -3.4273 (30.7933)
[0.06] w        1         2888         2944         2905           11       59.955 (9.15957e-27)     -3.39922 (29.9409)
[0.2] w        1         7724         7884         7766           38      34.8011 (7.69263e-16)     -3.33087 (27.9627)
[4.03] w        1        40134        41382        40362          396      22.1063 (2.50819e-10)     -2.73589 (15.4235)
 
Let's look at the last line starting with time [4.03]. The numbers 40134, 41382, 40362 and 396 stand for total number of OR nodes expanded, total number of AND nodes expanded, number of nodes (both OR and AND) expanded by the depth-first component, number of nodes (both AND and OR) expanded by the best-first component. The following 4 numbers correspond to the bounds and here's a bit tricky: 22.1063 is the negative log of the lower bound, so ln(LB) is -22.1063; (2.50819e-10) is the lower bound (i.e., ln(2.50819e-10) = -22.1063; -2.73589 is the negative log of the upper bound, so ln(UB) is 2.73589; (15.4235) is the upper bound (i.e., ln(15.4235) = 2.73589.

Once running out of time the results can be extracted from the output of the software.

-------------------------------------------------------------------------
Use AAOBF for Exact Evaluation

In our paper, for some problems that are not very large, we use AAOBF to evaluate solutions exactly. To do so, enter the AAOBF/ folder and excute the following command:
./mmap-solver --input-file ${dir}${prob}.uai --evidence-file ${dir}${prob}.uai.evid --map-file ${dir}${prob}.uai.map --solution-file ${solutionDir}/${Solution} --algorithm evaluator --seed 12345678 --verbose --positive --eval ao

And the ouput should look like:
----------------- Evaluation done ------------------
Problem name: 50-20-10.uai
Status: success
OR nodes: 824
AND nodes: 1649
Time elapsed: 0.01 seconds
Solution: 7.62682e-250 (-573.615)

In this example, the line that starts with Solution: has two numbers 7.62682e-250 (-573.615). The first number is the value of the summation problem and includes the constant generated when asserting the evidence and MAP assignment. (-573.615) is the log of the summation value (i.e., ln(7.62682e-250) = -573.615).

Note that all the "log" is the natural log ln.

-------------------------------------------------------------------------
OTHER WORK

Checkout the other project that uses the similar idea of "Aggregate Simulation", but work on MDP planning. 
  Link (https://github.com/hcui01/SOGBOFA)
