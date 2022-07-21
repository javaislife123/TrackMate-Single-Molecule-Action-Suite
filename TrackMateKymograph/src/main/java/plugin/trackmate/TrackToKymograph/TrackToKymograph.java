package plugin.trackmate.TrackToKymograph;

import java.awt.Frame;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.Slicer;
import ij.plugin.frame.ContrastAdjuster;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;


public class TrackToKymograph implements TrackMateAction
{

	private Logger logger;
	public static String unit;
	
	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log("Launching Track To Kymograph \n");
		
		Model trackmateModel = trackmate.getModel();
		
		unit = trackmateModel.getSpaceUnits()+"^2/" + trackmateModel.getTimeUnits();
		
		int ntracks = trackmateModel.getTrackModel().nTracks( true );
		
		
		if ( ntracks == 0 )
		{
			logger.log( "No visible track found. Aborting.\n" );
			return;
		}
		
		
		//List of all track IDs 
		final Set< Integer > trackIDs = trackmateModel.getTrackModel().trackIDs( true );
		
		//Used to store original image to be accessed later
		ImagePlus imageGlobal = WindowManager.getCurrentImage();
		//Loop through each track
		int j = 0;
		for ( final Integer trackID : trackIDs )
		{
			if(j>3) {
				break;
			}
			j++;
			final Set< Spot > track = trackmateModel.getTrackModel().trackSpots( trackID );

			
			// Sort them by time
			final TreeSet< Spot > sortedTrack = new TreeSet<>( Spot.timeComparator );
			sortedTrack.addAll( track );
			
		
			// Stores the x and y values of each spot
			ArrayList<Double> x_values = new ArrayList<>();
			ArrayList<Double> y_values = new ArrayList<>();
			
			for ( final Spot spot : sortedTrack )
			{
					x_values.add(spot.getFeature( Spot.POSITION_X )); 
					y_values.add(spot.getFeature( Spot.POSITION_Y )); 
					
			}
			//converts to array
			double[] x_data = new double[x_values.size()];
			double[] y_data = new double[y_values.size()];
		
			for(int i=0; i<x_data.length; i++) {
				x_data[i] = x_values.get(i);
				y_data[i] = y_values.get(i);
			}
		
			//fits line onto data
			CurveFitter line = new CurveFitter(x_data, y_data);
			line.doFit(CurveFitter.STRAIGHT_LINE);	
		
			// y = slope*x + b 
			double b = line.getParams()[0];
			double slope = line.getParams()[1];
	
			//calculates bounds of the line before slicing
			double[] x_extremes = getSmallestAndLargest(x_data);
			double smallest = x_extremes[0]-3;
			double largest = x_extremes[1]+3;
		
		
			
			ImagePlus img = imageGlobal;
			
			//creates new line with attributes that fit the data
			Calibration cali = imageGlobal.getCalibration();
			Roi line_roi = new Line(cali.getRawX(smallest), cali.getRawY(smallest*slope+b), cali.getRawX(largest), cali.getRawY(largest*slope+b));
			
	
			img.setRoi(line_roi);
			
			
			Slicer slicer = new Slicer();
			ImagePlus imageS = slicer.reslice(img);
		
			ImageProcessor ip = imageS.getProcessor();
			ip = ip.rotateRight();
			ip.flipHorizontal();
			ImagePlus image_rot = new ImagePlus("",ip);
		
		
			Overlay ov = new Overlay();
	
			boolean first = true;
			double prevT = Double.NaN;
			double prevD = Double.NaN;
			Spot prevSpot = null;
			ArrayList<Double> diffusionCoefficientList = new ArrayList<>();
			
			for ( final Spot spot : sortedTrack )
			{
				if(first) {
				
					first = false;	
					prevT = spot.getFeature(Spot.POSITION_T);
				
					double lineDistance = calcDistanceToLine(spot,slope,b);
					double originDistance = spot.getFeature(Spot.POSITION_X)-smallest; 
					prevD = Math.sqrt(Math.abs(lineDistance*lineDistance-originDistance*originDistance));	
					
				}else {
					double lineDistance = calcDistanceToLine(spot,slope,b);
				
					double originDistance = spot.getFeature(Spot.POSITION_X)-smallest;
					double distanceOnLine = Math.sqrt(Math.abs(lineDistance*lineDistance-originDistance*originDistance));	
					double frameInt = cali.frameInterval;
					
					if(frameInt==0.)frameInt = 1.;
					
					ov.add(new Line(spot.getFeature(Spot.POSITION_T)/frameInt+1,cali.getRawY(distanceOnLine)+1,prevT/frameInt+1,cali.getRawY(prevD)+1));	
					
					
					prevT = spot.getFeature(Spot.POSITION_T);
					prevD = distanceOnLine;
					
				}
				//Skips first spot in list
				if(prevSpot==null) {
					prevSpot=spot;
				}else {
				
					//Features for current spot
					final double frame = spot.getFeature( Spot.POSITION_T );
					final double x = spot.getFeature( Spot.POSITION_X );
					final double y = spot.getFeature( Spot.POSITION_Y );
					final double r = Math.sqrt(x*x+y*y);
					
					//Features for previous spot
					final double prevFrame = prevSpot.getFeature( Spot.POSITION_T );
					final double prevx = prevSpot.getFeature( Spot.POSITION_X );
					final double prevy = prevSpot.getFeature( Spot.POSITION_Y );
					final double prevr = Math.sqrt(prevx*prevx+prevy*prevy);
					
					//Calculate diffusion coefficient and add it to list
					double diffusionCoefficient=((prevr-r)*(prevr-r))/(2*(frame-prevFrame));
					diffusionCoefficientList.add(diffusionCoefficient);
					
			
				
		}
			}
		Plot plot = new Plot("Diffusion Coefficients for Kymograph", "Time", "Diffusion Coefficients");
		ArrayList<Double> arr = new ArrayList<>();
		for(double i = 0; i<diffusionCoefficientList.size(); i++) {
			arr.add(i);
		}
	
		 
		plot.addPoints(arr, diffusionCoefficientList, Plot.LINE);
		plot.show();
		image_rot.setOverlay(ov);
		
		//Brightness and Contrast
		
		
		
		autoAdjust(image_rot,image_rot.getProcessor());
		
		image_rot.show();
		
	
	}	
	
	
}


	private void autoAdjust(ImagePlus imp, ImageProcessor ip) {
		
		ImageStatistics stats = imp.getRawStatistics();
		int limit = stats.pixelCount/10;
		int[] histogram = stats.histogram;
		double min;
		double max;
		int autoThreshold = 2500;
		int threshold = stats.pixelCount/autoThreshold;
		int i = -1;
		boolean found = false;
		int count;
		do {
			i++;
			count = histogram[i];
			if (count>limit) count = 0;
			found = count> threshold;
		} while (!found && i<255);
		int hmin = i;
		i = 256;
		do {
			i--;
			count = histogram[i];
			if (count>limit) count = 0;
			found = count > threshold;
		} while (!found && i>0);
		int hmax = i;
		
		if (hmax>=hmin) {
			
			min = stats.histMin+hmin*stats.binSize;
			max = stats.histMin+hmax*stats.binSize;
			if (min==max)
				{min=stats.min; max=stats.max;}
			imp.setDisplayRange(min, max);
			
	}
	}

	private double calcDistanceToLine(Spot spot, double slope, double b) {
		double x1 = 0;
		double y1 = b;
		double x2 = 1;
		double y2 = slope+b;
		double x0 = spot.getFeature(Spot.POSITION_X);
		double y0 = spot.getFeature(Spot.POSITION_Y);
		
		double top = Math.abs((x2-x1)*(y1-y0)-(x1-x0)*(y2-y1));
		double bottom =Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
		return top/bottom;
	}


	public double[] getSmallestAndLargest(double[] dList) {
		double smallest = dList[0];
		double largest = dList[0];
		
		for(double d: dList) {
			if(d<smallest) {
				smallest = d;
			}
			if(d>largest) {
				largest = d;
			}
		}
		double[] output = {smallest,largest};
		return output;	
	}
	//public double calcDistanceFromLine(double x1, double y1, double m, double b) {
		
	public void calcDiffusionCoef( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		
		Model trackmateModel = trackmate.getModel();
		
		
		int ntracks = trackmateModel.getTrackModel().nTracks( true );
		
		
		if ( ntracks == 0 )
		{
			
			return;
		}
		
		
		//List of all track IDs 
		final Set< Integer > trackIDs = trackmateModel.getTrackModel().trackIDs( true );
		
		//List of each tracks' diffusion coefficients where each index is its own track
		final ArrayList<ArrayList<Double>> totalDiffusionCoefficientList = new ArrayList<>();
		
		//Loop through each track
		for ( final Integer trackID : trackIDs )
		{
			
			final Set< Spot > track = trackmateModel.getTrackModel().trackSpots( trackID );

			
			// Sort them by time
			final TreeSet< Spot > sortedTrack = new TreeSet<>( Spot.timeComparator );
			sortedTrack.addAll( track );
			
			//Store previous spot 
			Spot prevSpot=null;
			
			ArrayList<Double> diffusionCoefficientList = new ArrayList<>();
			
			for ( final Spot spot : sortedTrack )
			{
				
				//Skips first spot in list
				if(prevSpot==null) {
					prevSpot=spot;
				}else {
				
					//Features for current spot
					final double frame = spot.getFeature( Spot.POSITION_T );
					final double x = spot.getFeature( Spot.POSITION_X );
					final double y = spot.getFeature( Spot.POSITION_Y );
					final double r = Math.sqrt(x*x+y*y);
					
					//Features for previous spot
					final double prevFrame = prevSpot.getFeature( Spot.POSITION_T );
					final double prevx = prevSpot.getFeature( Spot.POSITION_X );
					final double prevy = prevSpot.getFeature( Spot.POSITION_Y );
					final double prevr = Math.sqrt(prevx*prevx+prevy*prevy);
					
					//Calculate diffusion coefficient and add it to list
					double diffusionCoefficient=((prevr-r)*(prevr-r))/(2*(frame-prevFrame));
					diffusionCoefficientList.add(diffusionCoefficient);
					
				}
				
			}
			//adds current list of diffusion coefficients for current track to 
			//global list of individual diffusion coefficient lists 
			totalDiffusionCoefficientList.add(diffusionCoefficientList);
			
	}
		

}
	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}
	
}