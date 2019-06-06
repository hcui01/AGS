import java.util.ArrayList;


public class TestLeak {
	 public static void main(String[] args) {
	       ArrayList<String> list = new ArrayList<String>();
	        while (1<2){
	            list.add("OutOfMemoryError soon");
	        }

	    }
}
