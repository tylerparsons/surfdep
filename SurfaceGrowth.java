package ch13;

import org.opensourcephysics.controls.AbstractSimulation;
import org.opensourcephysics.controls.SimulationControl;

import org.opensourcephysics.frames.PlotFrame;
import org.opensourcephysics.frames.LatticeFrame;
import java.awt.Color;

import ch13.Parameter;
import ch13.LinearRegression.Function;
import java.util.ArrayList;

import java.util.Scanner;

public class SurfaceGrowth extends AbstractSimulation {

	private LatticeFrame lattice;
	private PlotFrame width_vs_time, width_vs_length;
	private LinearRegression lnw_vs_lnL;
	private ArrayList<Deposition> models;
	private Deposition model;
	private DepositionDataManager dataManager;

	
/**************************
 * Initialization Methods *
 **************************/
	
	public SurfaceGrowth() {
		
		//set up visualizations
		model = new BallisticDiffusionModel();
		models = new ArrayList<Deposition>();
		lattice = new LatticeFrame(model.getClass().getName());
		
		width_vs_time = new PlotFrame("ln t (t in steps)", "ln w", "ln w = b*ln t + C");
		width_vs_time.setAutoscaleX(true);
		width_vs_time.setAutoscaleY(true);
		width_vs_length = new PlotFrame("ln L", "ln w_avg", "ln w = a*ln L + C (After Saturation)");
		width_vs_length.setAutoscaleX(true);
		width_vs_length.setAutoscaleY(true);
		
		dataManager = new DepositionDataManager(
			"data/logFile.txt",
			"data/data.txt",
			"data/data.csv"
		);
		dataManager.startTrial();
	}
	
	public void initialize() {
		//Create a new model for each simulation
		model = new BallisticDiffusionModel();
		lattice.clearDrawables();
		lattice.addDrawable(model);
		
		//Set Parameters
		ArrayList<Parameter> params = model.parameters();
		Parameter p;
		for (int i = 0; i < params.size(); i++) {
			p = params.get(i);
			p.value = control.getDouble(p.name);
		}
		
		model.init();
		
		lattice.setPreferredMinMax( 0, model.getLength()*model.getXSpacing(),
									0, model.getHeight()*model.getYSpacing());
		width_vs_length.setVisible(false);
	}
	
	
/*****************
 * Control Setup *
 *****************/
	
	public void reset() {
		//Add Parameters to control
		ArrayList<Parameter> params = model.parameters();
		Parameter p;
		for (int i = 0; i < params.size(); i++) {
			p = params.get(i);
			control.setValue(p.name, p.defaultValue);
		}
		//Control Values
		control.setValue("Save Data", true);
		control.setValue("Plot All", false);
		enableStepsPerDisplay(true);
	}
	
	
/****************
 * Calculations *
 ****************/
	
	protected void doStep() {
		try {
			model.step();
		} catch(ArrayIndexOutOfBoundsException e) {
			stop();
			return;
		}
		int time = model.getTime();
		if(time%pointModulus(time) == 0) {
			width_vs_time.append(model.getLength(), Math.log(time),
					 Math.log(model.getWidth(time)));
		}
	}
	
	public void stopRunning() {
		
		//Save model for later use, avoiding duplicate entries in model set
		if (!exists(model))
			models.add(model);
		
		//Estimate t_cross
		Scanner in = new Scanner(System.in);
		System.out.println("Define regions over which to run the regression:");
		System.out.print("ln(t_0) = ");
		int t_0 = (int)Math.exp(in.nextDouble());
		System.out.print("ln(t_cross1) = ");
		int t_cross1 = (int)Math.exp(in.nextDouble());
		System.out.print("ln(t_cross2) = ");
		int t_cross2 = (int)Math.exp(in.nextDouble());
		
		//Run calculations
		model.calculateBeta(t_0, t_cross1);
		model.calculatelnw_avg(t_cross2);
		double beta_avg = calculateAverageBeta();
		double alpha = calculateAlpha();
		
		//Wrap data in Parameters to pass to dataManager as list
		ArrayList<Parameter> addlParams = new ArrayList<Parameter>();
		addlParams.add(new Parameter("averageHeight", model.averageHeight()));
		addlParams.add(new Parameter("width", model.getWidth(model.getTime())));
		addlParams.add(new Parameter("numsteps", model.getTime()));
		addlParams.add(new Parameter("t_cross1", t_cross1));
		addlParams.add(new Parameter("t_cross2", t_cross2));
		addlParams.add(new Parameter("lnw_avg", model.getlnw_avg()));
		addlParams.add(new Parameter("beta", model.getBeta()));
		addlParams.add(new Parameter("beta_avg", beta_avg));
		addlParams.add(new Parameter("alpha", alpha));
		addlParams.add(new Parameter("R2", lnw_vs_lnL.R2()));
		
		//Print params to control
		for(Parameter p: model.parameters())
			control.println(p.name + " = " + p.value);
		for(Parameter p: addlParams)
			control.println(p.name + " = " + p.value);
		
		//Save, display data
		if (control.getBoolean("Save Data")) {
			dataManager.saveToTxt(model, /*t_cross1, alpha, beta_avg,*/ addlParams);
			dataManager.saveToCSV(model, addlParams);
			String description = "L"+model.getLength()+"H"+model.getHeight();
			dataManager.saveImage(lattice, "lattices", description + ".jpeg");
			dataManager.saveImage(width_vs_time, "plots", description + ".jpeg");
		}
		if (control.getBoolean("Plot All")) {
			plotAll();
			dataManager.saveImage(width_vs_time, ".", "masterPlot.jpeg");
			dataManager.saveImage(width_vs_length, ".", "alphaPlot.jpeg");
		}
	}
	
	private double calculateAverageBeta() {
		double sum = 0;
		for(Deposition m: models) {
			sum += m.getBeta();
		}
		return sum/(double)models.size();
	}
	
	// Runs regression of lnw_avg vs lnL
	private double calculateAlpha() {
		
		//wrap lnL, lnw_avg in Functions
		Function lnw_avg = new Function() {
			public double val(double x) {
				return models.get((int)x).getlnw_avg();
			}
		};
		Function lnL = new Function() {
			public double val(double x) {
				return Math.log(models.get((int)x).getLength());
			}
		};
		//Pass functions to regression
		lnw_vs_lnL = new LinearRegression(lnL, lnw_avg, 0, (double)models.size()-1, 1);
		//width_vs_length.addDrawable(lnw_vs_lnL);
		return lnw_vs_lnL.m();
	}
	
	public boolean exists(Deposition m) {
		for (Deposition mod: models)
			if (m.equals(mod))
				return true;
		return false;
	}
	
	
/***************************
 * Visualization Functions *
 ***************************/
	
	/*
	 * Point Density Calculator
	 * 	- Determines density of plotted points
	 *    based on time elapsed and N_max, the
	 *    plot's point capacity.
	 *  - Density decays such that lim t->inf-
	 *    inity N(t) = N_max
	 *  - Returns a max of 1000 to continue p-
	 *    lotting for very large t
	 * */
	final static int N_max = 10000;
	final static int mod_max = 10000;
	
	private int pointModulus(int t) {
		if (t > N_max*((int)(Math.log(mod_max))))
			return mod_max;
		return (int)(Math.exp(((double)t)/((double)N_max))) + 1;
	}
	
	/*
	 * Plots all models on two different plots
	 * 	- width_vs_time
	 * 		-> used to measure beta
	 * 		-> plots entire width array up to
	 * 		   total run time for each model
	 * 	- lnw_vs_lnA
	 * 		-> plots ln of avgerage width ag-
	 * 		   ainst ln L
	 * 		-> runs linear regression for alpha
	 * 
	 * */
	private Color[] colors = {Color.CYAN,
							  Color.ORANGE,
							  Color.MAGENTA,
							  Color.PINK,
							  Color.YELLOW,
							  Color.RED,
							  Color.GREEN,
							  Color.BLUE};
	
	private void plotAll() {
		
		// width_vs_time
		width_vs_time.clearDrawables();
		for (int i = 0; i < models.size(); i++) {
			Deposition m = models.get(i);
			//plot entire width array, set color
			width_vs_time.setMarkerColor(i, colors[i%colors.length]);
			int time = m.getTime();
			for (int t = 1; t <= time; t++) {
				int mod = pointModulus(t)*models.size();
				if (t % mod == 0) {
					width_vs_time.append(i, Math.log(t),
							 Math.log(m.getWidth(t)));
				}
			}	
		}
		
		// width_vs_length
		width_vs_length.clearDrawables();
		for (int i = 0; i < models.size(); i++) {
			Deposition m = models.get(i);
			width_vs_length.append(i, Math.log(m.getLength()), m.getlnw_avg());
		}
		// Draw linear regression
		width_vs_length.addDrawable(lnw_vs_lnL);
	}
	
	
/********
 * Main *
 ********/	
	
	public static void main(String[] args) {
		SimulationControl.createApp(new SurfaceGrowth());
	}

}