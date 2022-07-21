package plugin.trackmate.examples;

import java.util.ArrayList;

import fiji.plugin.trackmate.TrackMatePlugIn;
import ij.ImageJ;
import ij.measure.CurveFitter;
import ij.plugin.frame.ContrastAdjuster;
public class RunTrackMate
{

	public static void main( final String[] args )
	{
		
		ArrayList<Double> a = new ArrayList<>();
		a.add(1.0);
		a.add(2.0);
		a.add(2.0);
		ArrayList<Double> b = new ArrayList<>();
		b.add(3.0);
		b.add(2.0);
		
		ArrayList<Double> c = new ArrayList<>();
		c.add(2.0);
		c.add(4.0);
		c.add(1.0);
		c.add(4.0);
		
		
		ArrayList<ArrayList<Double>> arr = new ArrayList<>();
		arr.add(a);
		arr.add(b);
		arr.add(c);
		
		ArrayList<Double> arr1 = averageArrOfArr(arr);
		for(Double d: arr1) {
			System.out.println(d);
		}
	}
	private static ArrayList<Double> averageArrOfArr(ArrayList<ArrayList<Double>> arr) {
		int largestLength = getLargestLength(arr);
		int nArrayLists = arr.size();
		ArrayList<Double> output = new ArrayList<>();
		
		for(int i = 0; i<largestLength; i++) {
			double sum = 0;
			int divisor = nArrayLists;
			for(int j = 0; j<nArrayLists; j++) {
				if(!(i>=arr.get(j).size())) {
					sum+=arr.get(j).get(i);
				}else {
					divisor--;
				}
			}
			output.add(sum/divisor);
		}
		return output;
	}
	private static int getLargestLength(ArrayList<ArrayList<Double>> arr) {
		int largestLength = -1;
		for(ArrayList<Double> a: arr) {
			if(a.size()>largestLength) {
				largestLength = a.size();
			}
		}
		return largestLength;
	}
}

