/**
 * LargeSystemDeposition.java
 * Created:	7 May 2014
 * @author	Tyler Parsons
 *  
 * An abstract superclass of deposition
 * models. Provides support for model im-
 * plementation, visualization and stati-
 * stical analysis.
 */
package surfdep.largesystems.models;


import java.awt.Color;
import java.awt.Graphics;
import java.lang.Math;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;

import bdm.largesystems.utils.EmbeddedDBArray;
import bdm.largesystems.utils.EmbeddedDBArray.DBOperationCallback;
import bdm.largesystems.utils.LinearRegression;


public abstract class LargeSystemDeposition implements Drawable {


/********************
 * Model Properties *
 ********************/
	
	/**
	 * Length
	 */
	protected int L;
	/**
	 * Height
	 */
	protected int H;
	/**
	 * Slot height
	 */
	protected int dH;
	/**
	 * Array storing the height of each column.
	 */
	protected int[] height;
	/**
	 * Map storing other model parameters.
	 */
	protected HashMap<String, Double> parameters;
	
	/**
	 * Stores width for systems with L*H >
	 * {@link EmbeddedDBArray.MAX_ARRAY_SIZE}
	 */
	protected EmbeddedDBArray width;
	protected long maxSteps;
	
	protected double h_avg;
	protected int minHeight;
	protected int maxHeight;
	
/****************
 * Time Scaling *	
 ****************/

	/**
	 * Measured in total points deposited.
	 */
	protected long time;
	
	/**
	 * Determines whether a measurement of the width
	 * should be conducted at the given time. It can
	 * be implemented in a variety of ways to enable
	 * different definitions of time scales.
	 */
	protected Predicate<Long> measurementScheduler;
	
	/**
	 * Determines whether this model's quantities should
	 * be averaged for the given t.
	 */
	protected Predicate<Long> averageScheduler; 
	
	/**
	 * Called during step returns true.
	 */
	protected Consumer<Long> stepCallback;
	
	/**
	 * Provides access to scaled time.
	 */
	protected Supplier<Long> timeScaler;
	
	// Stores last integer value of certain parameters
	protected long last_h_avg;
	protected int last_scaled_ln_t;
	
/******************
 * Slot Variables *	
 ******************/
	
	/**
	 * Square lattice storing representation of surface.
	 */
	protected int[][] lattice;
	
	/**
	 * Tracks whether the bottom of slot has just been
	 * cleared, to prevent duplicate clearing as the
	 * first row is populated.
	 */
	protected boolean bottomCleared;
	/**
	 * Tracks whether the top of slot has just been
	 * cleared, to prevent duplicate clearing as the
	 * first row is populated.
	 */
	protected boolean topCleared;
	

/******************
 * Initialization *
 ******************/
	
	/**S
	 * super() must be called in all constructor overloads!
	 */
	public LargeSystemDeposition() {
		initParams();
		setDefaultTimeScale(1);
	}
	
	/**
	 * Constructs a LargeSystemDeposition with h_avg time scaling.
	 * super() must be called in all constructor overloads!
	 */
	public LargeSystemDeposition(double averageFactor) {
		// Set default parameters
		initParams();
		// Store averageFactor
		parameters.put("A", averageFactor);
		// Time scaling
		setLogarithmicTimeScale(averageFactor);
	}
	
	/**
	 * Initializes model for default time scaling.
	 * @param params contains updated parameters input by user
	 */
	public void init(HashMap<String, Double> params) {
		init(params, ((int)getParameter("L"))*(int)getParameter("H"));
	}
	
	/**
	 * @param params contains updated parameters input by user
	 * @param N	max size of member
	 * 			{@link bdm.largesystems.utils.EmbeddedDBArray}.
	 */
	public void init(HashMap<String, Double> params, int N) {
		
		for (String key: params.keySet())
			parameters.put(key, params.get(key));
		L = (int)getParameter("L");
		H = (int)getParameter("H");
		dH = (int)getParameter("dH");
		height = new int[L];
		
		// Define an array to store width values
		maxSteps = (N);
		width = new EmbeddedDBArray(maxSteps);
		
		time = -1L;	//Incremented once before used
		
		// Initialize slot, which will store the uppermost
		// surface of the deposition as a 2D bit array.
		lattice = new int[dH][L%32 == 0 ? L/32 : L/32 + 1];
		
		initFunctions();
		initDrawingParams();
	}
	
	/**
	 * Constructs params and adds params with a
	 * default value.
	 */
	public void initParams() {
		parameters = new HashMap<String, Double>();
		setParameter("L", 256);
		setParameter("H", 524288);
		setParameter("dH", 2048);		
	}
	
	public final void step() {
		
		// Callback invoked before next step
		onStep(time++);

		// Select deposition location
		Point p = deposit();
		
		// Clear one half of slot after preceding half fills
		if (!bottomCleared && (p.y % dH) == 0) {
			clearBottom();
			bottomCleared = true;
			topCleared = false;
		}
		else if (!topCleared && (p.y % (dH/2)) == 0 && (p.y % dH) != 0) {
			clearTop();
			topCleared = true;
			bottomCleared = false;
		}
		
		// Populate site determined by subclass		
		setBit(p.x, p.y);
		height[p.x] = p.y;
		
		// Calculate and store snapshot of system
		analyzeHeight();
		if (measure(time)) {
			recordWidth(width());
		}
		
	}
	
	/**
	 * Override this method to deposit point.
	 */
	protected abstract Point deposit();
	
	
/*******************
 * Bit Array Utils *
 *******************/

	protected int getBit(int x, int y) {
		return (lattice[y%dH][x/32] & (1 << (x%32))) == 0 ? 0 : 1;
	} 
	
	protected void setBit(int x, int y) {
		lattice[y%dH][x/32] |= (1 << (x%32));
	}

	/**
	 *  Clears the bottom half of the slot
	 */
	protected void clearBottom() {
		for(int y = 0; y < dH/2; y++)
			for(int x = 0; x < L/32; x++)
				lattice[y][x] = 0;
	}

	/**
	 *  Clears the top half of the slot
	 */
	protected void clearTop() {
		for(int y = dH/2; y < dH; y++)
			for(int x = 0; x < L/32; x++)
				lattice[y][x] = 0;
	}
	

/*************************
 * Accessing Width Array *
 *************************/
 
	public double getWidth(long t) {
		return width.get(t);
	}
 
	public void recordWidth(double w) {
		width.record(w);
	}
	
	public void registerDBOperationCallbacks(
			DBOperationCallback onPush,
			DBOperationCallback onPull
		) {
		width.registerPushCallback(onPush);
		width.registerPullCallback(onPull);
	}
	
	
/******************
 * Helper Methods *
 ******************/
	
	public boolean measure(long t) {
		return measurementScheduler.test(t);
	}
	
	public void onStep(long t) {
		stepCallback.accept(t);
	}
	
	public boolean takeAverage(long t) {
		return averageScheduler.test(t);
	}
	
	public long getScaledTime() {
		return timeScaler.get();
	}
	
	public void setHavgTimeScale() {
		// Measure when (int)h_avg increments
		measurementScheduler = (Long t) -> {return (int)h_avg > last_h_avg;};
		// Store last h_avg value
		stepCallback = (Long t) -> last_h_avg = (int)h_avg;
		// Return current h_avg
		timeScaler = () -> {return last_h_avg;};
		// Same as measurement
		averageScheduler = (Long t) -> {return (int)h_avg > last_h_avg;};
	}
	
	public void setLogarithmicTimeScale(double averageFactor) {
		// Measure when (int)h_avg increments
		measurementScheduler = (Long t) -> {return true;};
		// Store last h_avg value
		stepCallback = (Long t) -> {
			last_scaled_ln_t =  (int)(Math.log(t)/averageFactor);
		};
		// Return current h_avg
		timeScaler = () -> {return time;};
		// Same as measurement
		averageScheduler = (Long t) -> {
			return (int)(Math.log(t)/averageFactor) > last_scaled_ln_t;
		};
	}
	
	public void setDefaultTimeScale(int mod) {
		// Measure every averageFactor depositions
		measurementScheduler = (Long t) -> {return true;};
		// Do nothingreturn (int)h_avg > last_h_avg;
		stepCallback = (Long t) -> {};
		// Return current (long)h_avg
		timeScaler = () -> {return time;};
		// Every mod measurements
		averageScheduler = (Long t) -> {return time % (long)mod == 0;};
	}
	
	protected boolean isValid(int x, int y) {
		return !((x < 0 || x >= L) || (y < 0 || y >= H));
	}
	
	protected boolean hasNeighbors(int x, int y) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (isValid(x+dx, y+dy) && getBit(x+dx, y+dy) == 1) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Given a range of columms, return the max
	 * value of height within the range.
	 */
	protected int localMaxHeight(int lo, int hi) {
		if (hi<lo)
			return -1;
		lo = (lo<0) ? 0 : lo;
		hi = (hi>=L)? L-1 : hi;
		int max = height[(hi-lo)/2 + lo];
		for (int i = lo; i <= hi; i++)
			if (height[i] > max)
				max = height[i];
		return max;
	}

	public double getParameter(String name) {
		return parameters.get(name).doubleValue();
	}

	protected void setParameter(String name, double value) {
		parameters.put(name, value);
	}		
	

/************************
 * Statistical Analysis *
 ************************/
	
	protected double beta;
	protected double saturatedLnw_avg;
	protected LinearRegression.Function lnw;
	protected LinearRegression.Function lnt;
	protected LinearRegression lnw_vs_lnt;
	
	public void initFunctions() {
		
		lnw = (double x) -> {
			return Math.log(getWidth((long)x));
		};
		lnt = (double x) -> {
			return (double)Math.log((long)x);
		};
	}

	/**
	 * Width follows Scaling Relation:
	 * 		w(L, t) ~ L^a * f(t/L^(a/b)
	 * where
	 * 		f(u) ~ u^b		,	u << 1
	 * 		f(u) = const	,	u >> 1.
	 * From this, letting u = t/L^(a/b),
	 * 		lnw = b*lnt + C	,	t << L^(a/b)
	 * 		lnw = a*lnL + C	,	t >> L^(a/b).
	 * Linear regression gives
	 * 		a = alpha = (roughness exponent),
	 * 		b = beta = (growth exponent).
	 * 
	 */
	public void calculateBeta(int t_0, int t_x) {
		if (t_x <= 0)
			return;
		lnw_vs_lnt = new LinearRegression(lnt, lnw, t_0, t_x, 1);
		beta = lnw_vs_lnt.m();
	}
	
	/** 
	 * Calculate saturatedLnw_avg during saturation
	 * @param t_x
	 */
	public void calculateSaturatedLnw_avg (long t_x) {
		double sum = 0;
		for (long t = t_x; t < time; t++)
			sum += getWidth(t);
		saturatedLnw_avg = (double)Math.log((sum)/((double)(time-t_x)));
	}
	
	/**
	 * Calculate max, min and avg height
	 */
	public void analyzeHeight() {
		int sum = 0;
		minHeight = height[0];
		maxHeight = height[0];
		for (int i = 0; i < L; i++) {
			if (height[i] < minHeight)
				minHeight = height[i];
			if (height[i] > maxHeight)
				maxHeight = height[i];
			sum += height[i];
		}
		h_avg = ((double)sum)/((double)L);
	}
	
	/**
	 *  Instantaneous "width" of the surface
	 * @return
	 */
	public double width() {
		double sum = 0;
		for (int i = 0; i < L; i++)
			sum += (height[i]-h_avg)*(height[i]-h_avg);
		return (double)Math.sqrt(sum/(double)L);
	}
	

/******************
 * Plot Utilities *
 ******************/
	
	//Drawing parameters
	protected int atomicLength;
	protected int atomicHeight;
	protected int xSpacing;
	protected int ySpacing;
	
	protected void initDrawingParams() {
		atomicLength = scale(8, L, 128);
		atomicHeight = scale(8, H, 128);
		xSpacing = atomicLength;
		ySpacing = atomicLength;
	}
	
	public void draw(DrawingPanel dp, Graphics g) {
		
		for (int i = 0; i < L; i++) {
			for (int j = 0; j < dH; j++) {
				if (getBit(i, j) == 1) {
					g.setColor(Color.RED);
					g.fillRect(	dp.xToPix(i*(xSpacing)),
								dp.yToPix(j*(ySpacing)),
								atomicLength,
								atomicHeight);
				}
				else {
					g.setColor(Color.BLACK);
					g.fillRect(	dp.xToPix(i*(xSpacing)),
								dp.yToPix(j*(ySpacing)),
								atomicLength,
								atomicHeight);
				}
			}
		}
	}
	
	/**
	 *  Scales an initialValue by a factor of 1/2*(latticeSize/scale)
	 * @param initialValue
	 * @param latticeSize
	 * @param scale
	 * @return
	 */
	protected int scale(int initialValue, int latticeSize, int scale) {
		int scalar = (2*initialValue)/(2*(1 + latticeSize/scale));
		return scalar!=0 ? scalar : 1;
	}
	
/*******************
 * Debug Utilities *
 *******************/
	
	
	protected void printLocalTopography(int x, int y, int dx, int dy) {
		
		int x_min = (x-dx)<0 ? 0:(x-dx);
		int x_max = (x+dx)>(L-1) ? (L-1):(x+dx);
		int y_min = (y-dy)<0 ? 0:(y-dy);
		int y_max = (y+dy)>(dH-1) ? (dH-1):(y+dy);
		
		for (int j = y_max; j >= y_min; j--) {
			for (int i = x_min ; i < x_max + 1; i++) {
				System.out.print(getBit(i, j) + (i==x && j==y ? "<":" "));// + "("+i+", "+j+") ");
			}
			System.out.println();
		}
	}

/******************
 * Nested Classes *
 ******************/

	protected class Point {
		int x;
		int y;
	}

	
/***********
 * Getters *
 ***********/
	
	public int getdH()							{return dH;}
	public int getLength()						{return L;}
	public int getHeight()						{return H;}
	public long getTime()						{return time;}
	public int getMaxHeight()					{return maxHeight;}
	
	public double getBeta()						{return beta;}
	public double getAtomicLength()				{return atomicLength;}
	public double getAtomicHeight()				{return atomicHeight;}
	public double getAverageHeight()			{return h_avg;}
	public double getSaturatedLnw_avg()			{return saturatedLnw_avg;}
	public double getXSpacing()					{return xSpacing;}
	public double getYSpacing()					{return ySpacing;}
	
	public HashMap<String, Double> parameters()	{return new HashMap<String, Double>(parameters);}

}
