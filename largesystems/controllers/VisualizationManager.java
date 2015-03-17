package surfdep.largesystems.controllers;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.opensourcephysics.frames.LatticeFrame;
import org.opensourcephysics.frames.PlotFrame;

import surfdep.largesystems.models.LargeSystemDeposition;
import surfdep.largesystems.utils.LinearRegression;

public class VisualizationManager {

	private LatticeFrame lattice;
	private PlotFrame width_vs_time, width_vs_length;
	
	/**
	 *  Color palette for plotting models
	 */
	private Color[] colors = {
			Color.CYAN,
			Color.ORANGE,
			Color.MAGENTA,
			Color.PINK,
			Color.YELLOW,
			Color.RED,
			Color.GREEN,
			Color.BLUE
	};
	
/*************
 * Constants *
 *************/
	
	final static int N_max = 10000;
	final static int mod_max = 10000;
	final static double alphaOverBeta = 1.5D;
	
/******************
 * Nested Classes *
 ******************/
	
	/**
	 * Data object for storing a plotted
	 * point collection's marker color
	 * and index.
	 */
	private class MarkerData {
		Color color;
		int index;
		MarkerData(Color c, int i) {
			color = c;	index = i;
		}
	}
	
	/**
	 * Wraps data for plotting a point
	 * @author Tyler
	 */
	public static class Point {
		/** Dataset index */
		public int i;
		/** Dep. variable */
		public double x;
		/** Ind. variable */
		public double y;
		
		public Point(int I, double X, double Y) {
			i = I; x = X; y = Y;
		}
	}
	
	/**
	 * Abstract conversion of Data into Point.
	 * @author Tyler
	 */
	protected interface Plotter<Data> {
		public Point getPoint(
			int index,
			Data data
		);		
	}
	
/******************
 * Initialization *
 ******************/
	
	public VisualizationManager(String modelName) {
		
		// Instantiate plots
		lattice = new LatticeFrame(modelName);		
		width_vs_time = new PlotFrame("ln t (t in steps)", "ln w", "ln w = b*ln t + C");
		width_vs_time.setAutoscaleX(true);
		width_vs_time.setAutoscaleY(true);
		width_vs_length = new PlotFrame("ln L", "ln w_avg", "ln w = a*ln L + C (After Saturation)");
		width_vs_length.setAutoscaleX(true);
		width_vs_length.setAutoscaleY(true);
		
	}
	
	/**
	 * Set up plots for all trials.
	 */
	public void initPlots() {
		
		lattice.clearDrawables();
		lattice.setVisible(true);
		width_vs_length.clearDrawables();
		width_vs_length.setVisible(true);
		width_vs_time.clearData();
		width_vs_time.setVisible(true);
		
	}
	
	/**
	 * Sets up visualizations if they are enabled
	 */
	public void initVisuals(LargeSystemDeposition model) {
		
		lattice.addDrawable(model);
		lattice.setVisible(true);
		lattice.setPreferredMinMax(
			0, model.getLength()*model.getXSpacing(),
			0, model.getdH()*model.getYSpacing()
		);
		
	}
	
	public void hideLattice() {
		
		lattice.setVisible(false);
		
	}
	
/************
 * Plotting *
 ************/
	
	public void logPlotWidth(int L, double lnt, double lnw) {		
		width_vs_time.append(L, lnt, lnw);
	}
	
	/**
	 * Plots all models on two different plots
	 * 	- width_vs_time
	 * 		-> used to measure beta
	 * 		-> plots entire width array up to
	 * 		   total run time for each model
	 * 		-> plots ln(w/L^(1/2)) vs ln (t/L^(1/2)) 
	 */
	public void plotAllModels(ArrayList<LargeSystemDeposition> models) {
		
		/** width_vs_time **/
		width_vs_time.clearData();
		for (int i = 0; i < models.size(); i++) {
			
			//plot entire width array, set color
			LargeSystemDeposition m = models.get(i);
			width_vs_time.setMarkerColor(i, colors[i%colors.length]);
			long time = m.getTime();
			
			for (long t = 1; t <= time; t++) {
				
				long mod = dynamicPointModulus(t, m.getLength())*models.size();
				
				if (t % mod == 0) {
					width_vs_time.append(
							i,
							Math.log(t/((double)(m.getLength()*m.getLength()))),
							Math.log(m.getWidth(t)/Math.sqrt(m.getLength()))
					);
				}
			}	
		}
		
	}
	
	/**
	 * Scales the average width data using the scaling exponent
	 * {@code z} and plots the result. Different t regions of the
	 * plot should collapse, or coincide for different z.
	 * 
	 * @param lengths distinct system lengths in {@code data}.
	 * @param data	SQL query result data
	 * @param nPoints total points to be plotted
	 * @param z scaling exponent equal to alpha/beta
	 */
	public void scaledAvgWidthPlot(
			ArrayList<Integer> lengths,
			ResultSet data,
			int nPoints, double z
	) {
		
		// Generate color map of lengths to java.awt.Colors
		HashMap<Integer, MarkerData> colorMap = new HashMap<Integer, MarkerData>();
		for (int i = 0; i < lengths.size(); i++)
			colorMap.put(lengths.get(i), new MarkerData(colors[i%colors.length], i));
		
		// Clear Plot Frame
		width_vs_time.clearData();
		
		// Determine point mod
		int mod = (int) staticPointModulus(nPoints);
		
		try {
		
			int p = 0, i = 0;	// points plotted, iterations taken
			while (data.next() && p < 10000) {
				
				// Plot every numLengths points
				if (i++ % mod == 0) {
				
					// Grab data points, using h_avg time scaling
					double t = data.getDouble("h_avg");
					int L = data.getInt("L");
					double w = data.getDouble("w_avg");
					
					// Set marker color
					MarkerData md = colorMap.get(L);
					width_vs_time.setMarkerColor(
							md.index,
							md.color
					);
				
					// Add to PlotFrame
					width_vs_time.append(
							md.index,
							Math.log(t/(Math.pow(L, z))),
							Math.log(w/Math.sqrt(L))
					);
					
					p++;
				}	
			}
		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * lnw_vs_lnL
	 * 	-> plots ln of avgerage width against ln L
	 * 	-> runs linear regression for alpha
	 * @param data		An ArrayList
	 * @param lnw_vs_lnL
	 */
	public <Data> void logPlotWidthVsLength(
			ArrayList<Data> data,
			LinearRegression lnw_vs_lnL
	) throws IllegalArgumentException {
		
		// Clear
		width_vs_length.clearDrawables();
		
		// Parse generic Data parameter to plot points
		if (data.get(0) instanceof LargeSystemDeposition) {
		
			plotPointList(
				data,
				width_vs_length,
				(int i, Data point) -> {
					LargeSystemDeposition lsd = (LargeSystemDeposition)point;
					return new Point(i, Math.log(lsd.getLength()), lsd.getSaturatedLnw_avg());
				}
			);
			
		}
		else if (data.get(0) instanceof Point) {
		
			plotPointList(
				data,
				width_vs_length,
				(int i, Data d) -> {
					return (Point)d;
				}
			);
						
		}
		else {
			throw new IllegalArgumentException("Unsupported Data type parameter passed");
		}
		
		// Draw linear regression
		width_vs_length.addDrawable(lnw_vs_lnL);
		width_vs_length.setVisible(true);

	}
	
	/**
	 * Abstracts iteration and plotting for
	 * generic Data type.
	 * @param points
	 * @param graph
	 * @param plotter
	 */
	protected <Data> void plotPointList(
			ArrayList<Data> points,
			PlotFrame graph,
			Plotter<Data> plotter
	) {
		
		for (int i = 0; i < points.size(); i++) {
			Point p = plotter.getPoint(i, points.get(i));
			graph.append(p.i, p.x, p.y);
		}
		
	}

	/**
	 * Plots average beta values across all x.
	 * @param beta_avg
	 */
	public void plotBetaVsX(HashMap<Double, AnalysisControl.Average> beta_avg) {
		
		// Create new plot
		PlotFrame beta_vs_x = new PlotFrame(
			"x", "beta", "Average beta values across all x"
		);
		beta_vs_x.setVisible(true);
		
		// Plot averages
		for (Double x: beta_avg.keySet()) {
			beta_vs_x.append(0, x, beta_avg.get(x).val);
		}
		
	}
	
	
/*******************************************
 * Point Density Calculator				   *
 * 	- Determines density of plotted points *
 *    based on time elapsed and N_max, the *
 *    plot's point capacity.			   *
 *  - Density decays such that lim t->inf- *
 *    inity N(t) = N_max				   *
 *  - Returns a max of 1000 to continue	   *
 *    plotting for very large t			   *
 *******************************************/
	
	public long expectedT_cross(int L) {
		return (long) Math.pow(L, alphaOverBeta);
	}
	
	/**
	 * Appropriate point modulus for the
	 * growth phase of the deposition.
	 */
	private long growthModulus(long t) {
		if (t > N_max*((int)(Math.log(mod_max))))
			return mod_max;
		return (int)(Math.exp(((double)t)/((double)N_max))) + 1;
	}
	
	/**
	 * Appropriate point modulus for the
	 * saturation phase of the deposition.
	 */
	private long saturationModulus(long t) {
		if (t > (long)(Math.log(Double.MAX_VALUE) + Math.log(N_max)))
			return mod_max;
		return (long)Math.exp(((double)t) - Math.log((double)N_max)) + 1;
	}

	/**
	 * Determines the phase of the deposition at time t
	 * and selects the appropriate point modulus based
	 * on system size.
	 * @param t time
	 * @return point modulus
	 */
	public long dynamicPointModulus(long t, int L) {
		if (t < expectedT_cross(L)) {
			return growthModulus(t);
		}
		return saturationModulus(t);
	}
	
	/**
	 * Computes a simple, constant point modulus.
	 * @param N number of total points
	 * @return (N/10000) + 1
	 */
	public long staticPointModulus(long N) {
		return (N/10000) + 1;
	}
	
/***********
 * Getters *
 ***********/
	
	public LatticeFrame getLattice() {
		return lattice;
	}
	
	public PlotFrame getWidthVsTime() {
		return width_vs_time;
	}
	
	public PlotFrame getWidthVsLength() {
		return width_vs_length;
	}
	
}
