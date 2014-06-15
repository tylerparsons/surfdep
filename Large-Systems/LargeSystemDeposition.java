/*
######################################
Deposition.java
@author		Tyler Parsons
@created	7 May 2014
 
An abstract superclass of deposition
models. Provides support for model im-
plementation, visualization and stati-
stical analysis.
######################################
*/
package ch13;

import ch13.LinearRegression.Function;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.Math;
import java.util.HashMap;

import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;


public abstract class LargeSystemDeposition implements Drawable {

	
	protected int L;	// Length
	protected int H;	// Max height
	protected int dH;	// Slot height
	protected int[][] lattice;
	protected double[] width;
	protected int[] height;
	
	protected double averageHeight;
	protected double minHeight;
	protected double maxHeight;
	
	protected int time;	//measured in steps
	protected int Max_Steps;
	
	// Tracks whether slot has just been cleared,
	// to prevent duplicate clearing as the first
	// row is populated.
	protected boolean bottomCleared;
	protected boolean topCleared;
	
	protected HashMap<String, Double> parameters;
	
	//super() must be called in all constructor overloads!
	public LargeSystemDeposition() {
		parameters = new HashMap<String, Double>();
		setParameter("L", 512);
		setParameter("H", 16384);
	}
	
	/**
	 * @param params - contains updated parameters input by user
	 */
	public void init(HashMap<String, Double> params) {
		
		parameters = params;
		L = (int)getParameter("L");
		H = (int)getParameter("H");
		height = new int[L];
		width = new double[L*H];
		time = 0;
		
		// Initialize slot, which will store the uppermost
		// surface of the deposition as a 2D bit array.
		dH = 256;
		lattice = new int[dH][L/32];
		
		initFunctions();
		initDrawingParams();
	}
	
	public final void step() {
		time++;
		// Calculate and store snapshot of system after each step
		analyzeHeight();
		width[time] = width();
		// Clear one half of slot after preceding half fills
		if (!bottomCleared && (maxHeight % dH) == 0) {
			clearBottom();
			bottomCleared = true;
			topCleared = false;
		}
		else if (!topCleared && (maxHeight % (dH/2)) == 0 && (maxHeight%dH) != 0) {
			clearTop();
			topCleared = true;
			bottomCleared = false;
		}
		// Populate a site determined by subclass
		Point p = depositePoint();
		setBit(p.x, p.y);
		height[p.x] = p.y;
	}
	
	/*
	 * Override this method to deposite point.
	 * */
	protected abstract Point depositePoint();
	
	
/*******************
 * Bit Array Utils *
 *******************/

	protected int getBit(int x, int y) {
		return (lattice[y%dH][x/32] & (1 << (x%32))) >> (x%32);
	} 
	
	protected void setBit(int x, int y) {
		lattice[y%dH][x/32] |= (1 << (x%32));
	}
	
	protected void clearBit(int x, int y) {
		lattice[y%dH][x/32] ^= (1 << (x%32));
	}

	// Clears the bottom half of the slot
	protected void clearBottom() {
		for(int y = 0; y < dH/2; y++)
			for(int x = 0; x < L/32; x++)
				lattice[y][x] = 0;
	}
	
	protected void clearTop() {
		for(int y = dH/2; y < dH; y++)
			for(int x = 0; x < L/32; x++)
				lattice[y][x] = 0;
	}
	

/******************
 * Helper Methods *
 ******************/	
	
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
	
	/*
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

	protected double getParameter(String name) {
		return parameters.get(name).doubleValue();
	}

	protected void setParameter(String name, double value) {
		parameters.put(name, new Double(value));
	}		
	

/************************
 * Statistical Analysis *
 ************************/
	
	protected double beta;
	protected double saturatedLnw_avg;
	protected Function lnw;
	protected Function lnt;
	protected LinearRegression lnw_vs_lnt;
	
	public void initFunctions() {
		
		lnw = new Function() {
			public double val(double x) {
				return Math.log(width[(int)x]);
			}
		};
		lnt = new Function() {
			public double val(double x) {
				return Math.log((int)x);
			}
		};
	}

	/*
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
	 * */
	public void calculateBeta(int t_0, int t_cross) {
		if (t_cross <= 0)
			return;
		lnw_vs_lnt = new LinearRegression(lnt, lnw, t_0, t_cross, 1);
		beta = lnw_vs_lnt.m();
	}
	
	//Calculate saturatedLnw_avg during saturation
	public void calculateSaturatedLnw_avg (int t_cross) {
		double sum = 0;
		for (int t = t_cross; t < time; t++)
			sum += width[t];
		saturatedLnw_avg = Math.log(((double)sum)/((double)(time-t_cross)));
	}
	
	// Calculate max, min and avg height
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
		averageHeight = ((double)sum)/((double)L);
	}
	
	// Instantaneous "width" of the surface
	public double width() {
		double sum = 0;
		for (int i = 0; i < L; i++)
			sum += (height[i]-averageHeight)*(height[i]-averageHeight);
		return Math.sqrt(sum/(double)L);
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
	
	// Scales an initialValue by a factor of 1/2*(latticeSize/scale)
	protected int scale(int initialValue, int latticeSize, int scale) {
		int scalar = (2*initialValue)/(2*(1 + latticeSize/scale));
		return scalar!=0 ? scalar : 1;
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
	public int getTime()						{return time;}
	public double getBeta()						{return beta;}
	public double getWidth(int t)				{return width[t];}
	public double getAtomicLength()				{return atomicLength;}
	public double getAtomicHeight()				{return atomicHeight;}
	public double getAverageHeight()			{return averageHeight;}
	public double getSaturatedLnw_avg()			{return saturatedLnw_avg;}
	public double getXSpacing()					{return xSpacing;}
	public double getYSpacing()					{return ySpacing;}
	public HashMap<String, Double> parameters()	{return parameters;}

}
