package plugin.trackmate.DiffusionCoefficientCalculator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class CustomCSVWriter {
	
	public CustomCSVWriter() {
		
	}
	public void write(ArrayList<ArrayList<Double>> listOfDiffusionCoef) {
		

		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new java.io.File("."));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		
		int returnVal = fc.showOpenDialog(null);
		File directory = null;
		if(returnVal == JFileChooser.APPROVE_OPTION) {
		    directory = fc.getSelectedFile();
		}
		
		try {
			
			String filename = JOptionPane.showInputDialog("What would you like to name this file?");
			File csvFile = new File(directory,filename+".csv");
			FileWriter writer = new FileWriter(csvFile);
			
			String output = "";
			int trackNumber = 1;
			
			for(ArrayList<Double> arr:listOfDiffusionCoef) {
				output+="Track " + trackNumber+",";
				trackNumber++;
				
				for(Double d: arr) {
					output+=d+DiffusionCoefficientTracker.unit+",";
				}
				
				output+="\n";
			}
			
			writer.write(output);
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
