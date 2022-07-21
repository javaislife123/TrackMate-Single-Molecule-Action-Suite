package plugin.trackmate.DiffusionCoefficientCalculator;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;


@Plugin( type = TrackMateActionFactory.class, enabled = true, visible = true )
public class DiffusionCoefficientTrackerFactory implements TrackMateActionFactory
{

	private static final String INFO_TEXT = "<html>This action will calculate the diffusion coefficient for each track.</html>";

	private static final String KEY = "DIFFUSION_COEFFICIENT_CALCULATOR";

	private static final String NAME = "Calculate Diffusion Coefficient";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateAction create()
	{
		return new DiffusionCoefficientTracker();
	}
}