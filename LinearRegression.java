/*
######################################
LinearRegression.java
@author		Tyler Parsons
@created	7 May 2014
 
A class that creates a simple linear 
regression given a function definition.
Uses wrapper interface Function to ana-
lyze continuous and constant functions,
and arrays.
######################################
*/
package bdm;

import java.awt.Color;
import java.awt.Graphics;

import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;

public class LinearRegression implements Drawable{
	
	protected double m;
	protected double b;
	protected double R2;
	protected double dx;
	
	public interface Function {
		public double val(double x);
	}
	
	
/***************************************
 * Regression for Continuous Functions *
 ***************************************/	
	
	/*
	 * Creates a linear regression of the given representation of a function
	 * @param f - independent variable
	 * @param g - dependent variable
	 * @param x1 - start of interval
	 * @param x2 - end of interval
	 * @param d - step size
	 * 
	 * */	
	public LinearRegression(Function f, Function g, double x1, double x2, double d) {
		dx = d;
		calculate(f, g, x1, x2);
	}
	
	public void calculate(Function f, Function g, double x1, double x2) {
		
		double f_avg = mean(f, x1, x2);
		double g_avg = mean(g, x1, x2);
		double fg_avg = meanProduct(f, g, x1, x2);		
		double f2_avg = meanProduct(f, f, x1, x2);
		double g2_avg = meanProduct(g, g, x1, x2);
		
		m = (fg_avg - f_avg*g_avg)/(f2_avg - f_avg*f_avg);
		b = g_avg - m*f_avg;
		R2 = ((fg_avg - f_avg*g_avg)*(fg_avg - f_avg*g_avg))/
			 ((f2_avg - f_avg*f_avg)*(g2_avg - g_avg*g_avg));
	}
	
	public double mean(Function f, double x1, double x2) {
		double sum = 0;
		int ctr = 0;
		double f_x;
		for (double x = x1; x <= x2; x += dx) {
			f_x = f.val(x);
			if (!Double.isInfinite(f_x)) {
				sum += f.val(x);
				ctr++;
			}
		}
		return (ctr == 0) ? 0 : sum/((double)ctr);
	}
	
	public double meanProduct(Function f, Function g, double x1, double x2) {
		double sum = 0;
		int ctr = 0;
		double f_x, g_x;
		for (double x = x1; x <= x2; x += dx) {
			f_x = f.val(x);
			g_x = g.val(x);
			if (!Double.isInfinite(f_x) && !Double.isInfinite(g_x)) {
				sum += f_x*g_x;
				ctr++;
			}
		}
		return (ctr == 0) ? 0 : sum/((double)ctr);
	}
	
	
/******************
 * Plot Utilities *
 ******************/
	
		public void draw(DrawingPanel dp, Graphics g) {
			
			int x1 = dp.xToPix(dp.getXMin());
			int y1 = dp.yToPix(m*dp.getXMin() + b);
			int x2 = dp.xToPix(dp.getXMax());
			int y2 = dp.yToPix(m*dp.getXMax() + b);
			
			g.setColor(Color.BLACK);
			g.drawLine(x1, y1, x2, y2);
		}
		
	
/*********************
 * Getters & Setters *
 *********************/
	
	public double m()	{return m;}
	public double b()	{return b;}
	public double R2()	{return R2;}
	
	public void setStepSize(double d)	{dx = d;}
	
	
}
