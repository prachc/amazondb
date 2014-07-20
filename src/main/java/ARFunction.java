
public class ARFunction {
	public static String summation(String[] input){
    	double toutput = 0;
    	
    	for (int i = 0; i < input.length; i++)
			toutput += Double.parseDouble(input[i]);
		
    	return Double.toString(toutput);
	}
}
