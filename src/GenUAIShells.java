import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


public class GenUAIShells {
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		Records rec = new Records();
		String dir = args[0];
		//generate AGS shells
		for(int i = 0; i < Global.ratioRef.size(); i ++) {
			String theRatio = Global.ratioRef.get(i);
			for(String dom: Global.probNamesRef) {
				String content = new String();
				content += "#!/bin/bash\n";
				content += "#SBATCH --mem=120000\n";
				content += "#SBATCH --output=/cluster/shared/hcui01/AGS_" + theRatio + "_" + dom + "_stdout\n";
				content += "#SBATCH --error=/cluster/shared/hcui01/AGS_" + theRatio + "_" + dom + "_errout\n";
				content += "\n";
				content += "module load  java/1.8.0_60\n";
				content += "java -jar " + "MMAP.jar " + theRatio + " " + dom + "\n";
				content += "sleep 10000h\n";
				WriteToFile(dir + "\\AGS_" + theRatio + "_" + dom, content);
			}
		}
		
		//generate AAOBF shells
		for(int i = 0; i < Global.ratioRef.size(); i ++) {
			String theRatio = Global.ratioRef.get(i);
			for(String dom: Global.probNamesRef) {
				String content = new String();
				content += "#!/bin/bash\n";
				content += "#SBATCH --mem=120000\n";
				content += "#SBATCH --output=/cluster/shared/hcui01/AAOBF_" + theRatio + "_" + dom + "_stdout\n";
				content += "#SBATCH --error=/cluster/shared/hcui01/AAOBF_" + theRatio + "_" + dom + "_errout\n";
				content += "\n";
				content += "module load  java/1.8.0_60\n";
				content += "for dir in */\n";
				content += "do\n";
				content += "  prob=${dir%*/}\n";
				content += "  prob=${prob##*/}\n";
				content += "  prob=${prob%_*}\n";
				content += "  for i in ";
				for(int t: Global.timesRef) {
					if(t < 1000) {
						continue;
					}
					content += (t / 1000) + " ";
				}
				content += "\n";
				content += "  do\n";
				content += "    (./../../mmap-solver/Release/timeout -t ${i} -m 80000000 ./../../mmap-solver/Release/mmap-solver --input-file ${dir}${prob}.uai --evidence-file ${dir}${prob}.uai.evid --map-file ${dir}${prob}.uai.map --algorithm any-aaobf --heuristic wmb-mm --ibound 10 --seed 12345678 --verbose --positive ) &\n";
				content += "    sleep 10s\n";
				content += "    sleep ${i}s\n";
				content += "  done\n";
				content += "done\n";
				content += "sleep 10000h\n";
				WriteToFile(dir + "\\AAOBF_" + theRatio + "_" + dom, content);
			}
		}
		
		//generate evaluation shells
		for(int i = 0; i < Global.ratioRef.size(); i ++) {
			String theRatio = Global.ratioRef.get(i);
			for(String dom: Global.probNamesRef) {
				String content = new String();
				content += "#!/bin/bash\n";
				content += "#SBATCH --mem=120000\n";
				content += "#SBATCH --output=/cluster/shared/hcui01/Eval_" + theRatio + "_" + dom + "_stdout\n";
				content += "#SBATCH --error=/cluster/shared/hcui01/Eval_" + theRatio + "_" + dom + "_errout\n";
				content += "\n";
				content += "module load  java/1.8.0_60\n";
				content += "for dir in */\n";
				content += "do\n";
				content += "  prob=${dir%*/}\n";
				content += "  prob=${prob##*/}\n";
				content += "  prob=${prob%_*}\n";
				content += "  for i in ";
				for(int t: Global.timesRef) {
					content += t + " ";
				}
				content += "\n";
				content += "  do\n";
				content += "    echo -e \"\\n\\n ************************************************ \\n\"\n";
				content += "    solutionDir=${dir}Solution/AGS/${i}\n";
				content += "    echo Evaluate: AGS  Problem: ${prob}  time: ${i} milliseconds\n";
				content += "    ./../../mmap-solver/Release/mmap-solver --input-file ${dir}${prob}.uai --evidence-file ${dir}${prob}.uai.evid --map-file ${dir}${prob}.uai.map --solution-file ${solutionDir}/Solution --algorithm evaluator --seed 12345678 --verbose --positive --eval ao\n";
				content += "    echo -e \"\\n\\n ************************************************ \\n\"\n";
				content += "    echo Evaluate: MPBP  Problem: ${prob}  time: ${i}\n";
				content += "    ./../../mmap-solver/Release/mmap-solver --input-file ${dir}${prob}.uai --evidence-file ${dir}${prob}.uai.evid --map-file ${dir}${prob}.uai.map --solution-file ${dir}/Solution/MPBP/${i}/Solution --algorithm evaluator --seed 12345678 --verbose --positive --eval ao\n";
				content += "  done\n";
				content += "done\n";
				content += "sleep 10000h\n";
				WriteToFile(dir + "\\EVAL_" + theRatio + "_" + dom, content);
			}
		}
	}
	
	static void WriteToFile(String fileName, String content) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		writer.print(content);
		writer.close();
	}
}
