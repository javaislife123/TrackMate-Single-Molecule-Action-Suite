package plugin.trackmate.TrackToKymograph;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;


@Plugin( type = TrackMateActionFactory.class, enabled = true, visible = true )
public class TrackToKymographFactory implements TrackMateActionFactory
{

	private static final String INFO_TEXT = "<html>This action will open a kymograph of the current track.</html>";

	private static final String KEY = "TRACK_TO_KYMOGRAPH";

	private static final String NAME = "Track to Kymograph";

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
		return new TrackToKymograph();
	}
}