/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2022 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package plugin.trackmate.examples.detector;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class ModifiedLogDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */
	private static boolean isNotified = false;
	private final static String BASE_ERROR_MESSAGE = "LogDetector: ";

	/** The image to segment. Will not modified. */
	protected RandomAccessible< T > img;

	protected double radius;

	protected double threshold;

	protected boolean doSubPixelLocalization;

	protected boolean doMedianFilter;

	protected String baseErrorMessage;

	protected String errorMessage;

	/** The list of {@link Spot} that will be populated by this detector. */
	protected List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;

	protected final double[] calibration;

	/*
	 * CONSTRUCTORS
	 */

	public ModifiedLogDetector( final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter )
	{
		
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radius = radius;
		this.threshold = threshold;
		this.doSubPixelLocalization = doSubPixelLocalization;
		this.doMedianFilter = doMedianFilter;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 )
		{
			errorMessage = baseErrorMessage + "Image must be 1D, 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		
		ImagePlus currentImage = WindowManager.getCurrentImage();
		if(currentImage.getRoi()==null&&!isNotified) {
			isNotified = true;
			JOptionPane.showMessageDialog(null, "Add ROI");
		}
	
		while(currentImage.getRoi()==null||currentImage.getRoi().getState()!=Roi.NORMAL) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Line roi = null;
		if(currentImage.getRoi() instanceof Line) {
			roi = (Line)currentImage.getRoi();
		}
		
		final long start = System.currentTimeMillis();

		/*
		 * Copy to float for convolution.
		 */

		final ImgFactory< FloatType > factory = Util.getArrayOrCellImgFactory( interval, new FloatType() );
		Img< FloatType > floatImg = DetectionUtils.copyToFloatImg( img, interval, factory );
		
		
		
		/*
		 * Do median filtering (or not).
		 */

		if ( doMedianFilter )
		{
			floatImg = DetectionUtils.applyMedianFilter( floatImg );
			if ( null == floatImg )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Failed to apply median filter.";
				return false;
			}
		}


		// Squeeze singleton dimensions
		int ndims = interval.numDimensions();
		for ( int d = 0; d < interval.numDimensions(); d++ )
			if ( interval.dimension( d ) <= 1 )
				ndims--;

		final Img< FloatType > kernel = DetectionUtils.createLoGKernel( radius, ndims, calibration );
		final FFTConvolution< FloatType > fftconv = new FFTConvolution<>( floatImg, kernel );

		/*
		 * Determine the right img factory for FFT calculation.
		 */
		Interval fftinterval = floatImg;
		for ( int d = 0; d < kernel.numDimensions(); d++ )
			fftinterval = Intervals.expand( fftinterval, kernel.dimension( d ), d );
		final ImgFactory< ComplexFloatType > imgFactory = Util.getArrayOrCellImgFactory( fftinterval, new ComplexFloatType() );
		fftconv.setFFTImgFactory( imgFactory );

		final ExecutorService service = Executors.newFixedThreadPool(numThreads);
		fftconv.setExecutorService(service);

		fftconv.convolve();
		service.shutdown();

		final long[] minopposite = new long[ interval.numDimensions() ];
		interval.min( minopposite );
		final IntervalView< FloatType > to = Views.translate( floatImg, minopposite );
		spots = DetectionUtils.findLocalMaxima( to, threshold, calibration, radius, doSubPixelLocalization, numThreads );
		
		
		List< Spot > MSpots = new ArrayList<>();
		Polygon points = roi.getPoints();
		for(Spot s: spots) {
			double x = s.getFeature(Spot.POSITION_X);
			double y = s.getFeature(Spot.POSITION_Y);	
			double x1 = points.xpoints[0];
			double y1 = points.ypoints[0];
			double x2 = points.xpoints[1];
			double y2 = points.ypoints[1];
			if(calcDisToLine(x,y,x1,y1,x2,y2)<5) {
				MSpots.add(s);
			}
		}
		spots = MSpots;
		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		
		
		
		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
	
	public double calcDisToLine(double x,double y,double x1,double y1,double x2,double y2) {
		  double A = x - x1;
		  double B = y - y1;
		  double C = x2 - x1;
		  double D = y2 - y1;

		  double dot = A * C + B * D;
		  double len_sq = C * C + D * D;
		  double param = -1;
		  if (len_sq != 0) {
			  param = dot / len_sq;
		  }
		      

		  double xx, yy;

		  if (param < 0) {
		    xx = x1;
		    yy = y1;
		  }
		  else if (param > 1) {
		    xx = x2;
		    yy = y2;
		  }
		  else {
		    xx = x1 + param * C;
		    yy = y1 + param * D;
		  }

		  double dx = x - xx;
		  double dy = y - yy;
		  return Math.sqrt(dx * dx + dy * dy);
	}
}
