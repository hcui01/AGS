import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Global {
	static public boolean ifLift = true;
	static public ArrayList<String> ratioRef = new ArrayList<String>(Arrays.asList(
			//NIPS Setup
			/*
			"0.5", 
			"0.3", 
			"0.2"
			*/
			//Complementary Setup
			"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"
			));
	
	static public ArrayList<String> probNamesRef = new ArrayList<String>(Arrays.asList(//"90-50-10", "90-34-5", "blockmap_05_03-0010",
			//"50-20-10", "fs-13", "mastermind_03_08_03-0001", "students_03_02-0015"));
			//NIPS SetUp
			/*
			"50-20-10",
			"90-20-1",
			"blockmap_05_01-0006",
			"fs-04",
			"mastermind_03_08_03-0001",
			"students_03_02-0015"
			*/
			//Complementary Setup
			"fs-04",
			"mastermind_03_08_03-0001",
			"50-20-10",
			"90-20-1"
			));
	static public ArrayList<Integer> timesRef = new ArrayList<Integer>(
			Arrays.asList(
					//NIPS setup
					/*
					100, 
					200, 
					300, 
					400, 
					500, 
					600, 
					700, 
					800, 
					900, 
					1000, 
					2000, 
					3000, 
					4000, 
					5000, 
					6000, 
					7000, 
					8000, 
					9000, 
					10000, 
					20000
					*/
					//Complementary SetUp
					1000,
					5000,
					10000
					));
	static public ArrayList<String> algorithmsRef = new ArrayList<String>(
			Arrays.asList("AGS", "MPBP", "AAOBF"));
	
	static public int countMAPSets = 20;
}
 