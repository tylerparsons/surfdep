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
package bdm;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.Math;
import java.util.ArrayList;

import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;

import bdm.LinearRegression.Function;


public abstract class Deposition implements Drawable {

/************************************
 * Implementation of Physical Model *
 ************************************/
	
	protected int L;	//length
	protected int H;	//max height
	protected byte[][] lattice;
	protected double[] width;
	protected int[] height;
	protected double averageHeight;
	protected int time;	//measured in steps
	protected int Max_Steps;
	
	protected ArrayList<Parameter> parameters;
	//Stores parameters so they can easily be
	//referenced by user class
	
	
	//super() must be called in all constructor overloads!
	public Deposition() {
		parameters = new ArrayList<Parameter>();
		Parameter p1 = new Parameter("L", 128);
		parameters.add(p1);
		Parameter p2 = new Parameter("H", 128);
		parameters.add(p2);
	}
	
	public void init() {
		
		//Instantiate fields, build arrays
		L = (int)getParameter("L");
		H = (int)getParameter("H");
		time = 0;
		lattice = new byte[L][H];
		height = new int[L];
		width = new double[L*H + 1];
		initFunctions();
		
		//Scale drawing parameters based on lattice size
		atomicLength = scale(8, L, 128);
		atomicHeight = scale(8, H, 128);
		xSpacing = atomicLength;
		ySpacing = atomicLength;
		
	}
	
	public void step() {
		time++;
		// Calculate and store snapshot of system after each step
		calculateAverageHeight();
		width[time] = width();
	}
	
	protected boolean isValid(int x, int y) {
		return !((x < 0 || x >= L) || (y < 0 || y >= H));
	}
	
	protected boolean hasNeighbors(int x, int y) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (isValid(x+dx, y+dy) && lattice[x+dx][y+dy] == 1) {
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
	protected int maxHeight(int lo, int hi) {
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
	

/******************
 * Helper Methods *
 ******************/
	
	protected double getParameter(String name) {
		for (Parameter p: parameters)
			if (p.name == name)
				return p.value;
		return -1;
	}
	
	protected void addParameter(Parameter p) {
		parameters.add(p);
	}
	
/************************
 * Statistical Analysis *
 ************************/
	
	protected double beta;
	protected double lnw_avg;
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
	 * 		lnw = blnt + C	,	t << L^(a/b)
	 * 		lnw = alnL + C	,	t >> L^(a/b).
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
	
	//Calculate lnw_avg during saturation
	public void calculatelnw_avg (int t_cross) {
		double sum = 0;
		for (int t = t_cross; t < time; t++)
			sum += width[t];
		lnw_avg = Math.log(((double)sum)/((double)(time-t_cross)));
	}
	
	// Average column height
	public void calculateAverageHeight() {
		int sum = 0;
		for (int i = 0; i < L; i++)
			sum += height[i];
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
	
	public void draw(DrawingPanel dp, Graphics g) {
		
		for (int i = 0; i < L; i++) {
			for (int j = 0; j < H; j++) {
				if (lattice[i][j] == 1) {
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
	public int scale(int initialValue, int latticeSize, int scale) {
		int scalar = (2*initialValue)/(2*(1 + latticeSize/scale));
		return scalar!=0 ? scalar : 1;
	}
	
	
/******************
 * Nested Classes *
 ******************/

	public class Point {
		public int x;
		public int y;
	}

	
/***********
 * Getters *
 ***********/
	
	public int getLength()						{return L;}
	public int getHeight()						{return H;}
	public int getTime()						{return time;}
	public double getBeta()						{return beta;}
	public double getlnw_avg()					{return lnw_avg;}
	public double getWidth(int t)				{return width[t];}
	public double getAtomicLength()				{return atomicLength;}
	public double getAtomicHeight()				{return atomicHeight;}
	public double getAverageHeight()			{return averageHeight;}
	public double getXSpacing()					{return xSpacing;}
	public double getYSpacing()					{return ySpacing;}
	public ArrayList<Parameter> parameters()	{return parameters;}

}
