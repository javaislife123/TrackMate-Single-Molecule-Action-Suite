package plugin.trackmate.DiffusionCoefficientCalculator;

import java.awt.Frame;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;


import javax.swing.JOptionPane;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;


public class DiffusionCoefficientTracker implements TrackMateAction
{

	private Logger logger;
	public static String unit;
	
	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		
		logger.log("Launching Diffusion Coefficient Tracker \n");
		
		
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
		
	//Format GUI message
	String message = "";
	for(int i = 0; i<totalDiffusionCoefficientList.size(); i++) {
		double avg_DC = getAverage(totalDiffusionCoefficientList.get(i));
		message += String.format("Track: %d, [DC]: %.3f%s%n", i+1, avg_DC, unit);
	}
		
	JOptionPane.showMessageDialog(null, message,"Results", JOptionPane.INFORMATION_MESSAGE );
	
	CustomCSVWriter writer = new CustomCSVWriter();
	writer.write(totalDiffusionCoefficientList);
}


	private double getAverage(ArrayList<Double> arrayList) {
		double sum = 0;
		for(double d:arrayList) {
			sum+=d;
		}
		return sum/arrayList.size();
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}
}