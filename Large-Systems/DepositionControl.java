/*
######################################
DepositionControl.java
@author		Tyler Parsons
@created	7 May 2014
 
A runnable class that manages instant-
iation of models, visualization, data 
analysis, UI and I/O of parameters.
######################################
*/
package ch13;

import ch13.EmbeddedDBArray.DBOperationCallback;
import ch13.LinearRegression.Function;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.opensourcephysics.controls.AbstractSimulation;
import org.opensourcephysics.controls.SimulationControl;
import org.opensourcephysics.frames.PlotFrame;
import org.opensourcephysics.frames.LatticeFrame;

/*
 * To Do:
 * 
 * - Develop way to increase space efficiency
 * 		- Not necessary to save entire lattice
 * 		- Implement a smaller byte[][], ~ L*100
 * 		- When a column overflows, start reusing
 * 		  the bottom row (after clearing it)
 * 			- Track max height (change calculateAverageHeight
 * 			  to analyzeHeight, update averageHeight and maxheight)
 * 			- every time a new maxHeight is reached, clear
 * 			  byte[maxHeight%H]
 * 				- only do this when maxHeight changes
 * 					e.g. not when two sites share maxHeight
 * 		- h[i] remains, the same, position in lattice
 * 		  becomes lattice[h[i]%H][i]
 * - Multithreading
 * 		- Would allow for use of entire cpu
 * 		- Multithread up to 4 separate models?
 * 			- less space efficient, but easier to implement
 * 			- still useful
 */
public class DepositionControl extends AbstractSimulation {

	private LatticeFrame lattice;
	private PlotFrame width_vs_time, width_vs_length;
	private LinearRegression lnw_vs_lnL;
	private ArrayList<LargeSystemDeposition> models;
	private LargeSystemDeposition model;
	private DepositionDataManager dataManager;

	
/**************************
 * Initialization Methods *
 **************************/
	
	public DepositionControl() {
		
		//set up visualizations
		model = new BallisticDiffusionModel();
		models = new ArrayList<LargeSystemDeposition>();
		lattice = new LatticeFrame(model.getClass().getName());
		
		width_vs_time = new PlotFrame("ln t (t in steps)", "ln w", "ln w = b*ln t + C");
		width_vs_time.setAutoscaleX(true);
		width_vs_time.setAutoscaleY(true);
		width_vs_length = new PlotFrame("ln L", "ln w_avg", "ln w = a*ln L + C (After Saturation)");
		width_vs_length.setAutoscaleX(true);
		width_vs_length.setAutoscaleY(true);
		
		dataManager = new DepositionDataManager(
			"C:\\Users\\Tyler\\Documents\\Classes\\CurrentClasses\\PHYS436\\workspace\\csm\\data\\id_log.txt",
			"C:\\Users\\Tyler\\Documents\\Classes\\CurrentClasses\\PHYS436\\workspace\\csm\\data\\deposition_data.txt",
			"C:\\Users\\Tyler\\Documents\\Classes\\CurrentClasses\\PHYS436\\workspace\\csm\\data\\deposition_data.csv"
		);
		dataManager.startTrial();
	}
	
	public void initialize() {
		//Create a new model for each simulation
		model = new BallisticDiffusionModel();
		lattice.clearDrawables();
		lattice.setVisible(false);
		width_vs_length.setVisible(false);
		
		//Set Parameters
		HashMap<String, Double> params = model.parameters();
		for (String name: params.keySet()) {
			params.put(name, control.getDouble(name));
		}
		model.init(params);
		
		// Enable database operation alerts
		model.registerDBOperationCallbacks(onPush, onPull);
		
		if (control.getBoolean("Enable Visualizations")) {
			lattice.addDrawable(model);
			lattice.setVisible(true);
			lattice.setPreferredMinMax(
				0, model.getLength()*model.getXSpacing(),
				0, model.getdH()*model.getYSpacing()
			);
		}		
	}
	
	
/*****************
 * Control Setup *
 *****************/
	
	public void reset() {
		//Add Parameters to control
		HashMap<String, Double> params = model.parameters();
		for (String name: params.keySet()) {
			control.setValue(name, params.get(name));
		}
		//Control Values
		control.setValue("Save Data", true);
		control.setValue("Plot All", false);
		control.setValue("Enable Visualizations", true);
		enableStepsPerDisplay(true);
	}
	
	
/****************
 * Calculations *
 ****************/
	
	protected void doStep() {
		if(model.getAverageHeight() > 0.9*model.getHeight()) {
			stopSimulation();
			return;
		}
		try {
			model.step();
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			stopSimulation();
			return;
		}
		long time = model.getTime();
		long mod = pointModulus(time);
		if(time%mod == 0) {
			width_vs_time.append(model.getLength(), Math.log(time),
								 Math.log(model.getWidth(time)));
		}
	}
	
	public void stopRunning() {
		
		//Save model for later use, avoiding duplicate entries in model set
		if (!exists(model))
			models.add(model);
		
		//Request input of t_cross and implement input callback
		//to analyze model
		
		new TCrossInputDialog(new TCrossInputDialog.InputHandler() {

			@Override
			public void onInputReceived(HashMap<String, Double> input) {
				analyzeModel(
					(int) Math.exp(input.get(TCrossInputDialog.KEY_T_0).doubleValue()),
					(int) Math.exp(input.get(TCrossInputDialog.KEY_T_CROSS1).doubleValue()),
					(int) Math.exp(input.get(TCrossInputDialog.KEY_T_CROSS2).doubleValue())
				);
				
			}
			
		});
		
		
	}
	
	private void analyzeModel(int t_0, int t_cross1, int t_cross2) {
		
		//Run calculations
		model.calculateBeta(t_0, t_cross1);
		model.calculateSaturatedLnw_avg(t_cross2);
		double beta_avg = calculateAverageBeta();
		double alpha = calculateAlpha();
		
		//Wrap data in Parameters to pass to dataManager as list
		HashMap<String, Double> addlParams = new HashMap<String, Double>();
		addlParams.put("averageHeight", new Double(model.getAverageHeight()));
		addlParams.put("width", new Double(model.getWidth(model.getTime())));
		addlParams.put("numsteps", new Double(model.getTime()));
		addlParams.put("t_cross1", new Double(t_cross1));
		addlParams.put("t_cross2", new Double(t_cross2));
		addlParams.put("lnw_avg", new Double(model.getSaturatedLnw_avg()));
		addlParams.put("beta", new Double(model.getBeta()));
		addlParams.put("beta_avg", new Double(beta_avg));
		addlParams.put("alpha", new Double(alpha));
		addlParams.put("R2", new Double(lnw_vs_lnL.R2()));
		
		//Print params to control
		for(String name: model.parameters().keySet())
			control.println(name + " = " + model.getParameter(name));
		for(String name: addlParams.keySet())
			control.println(name + " = " + addlParams.get(name).doubleValue());
		
		//Save, display data
		if (control.getBoolean("Save Data")) {
			dataManager.saveToTxt(model, addlParams);
			dataManager.saveToCSV(model, addlParams);
			String fileName = "L"+model.getLength()+"H"+model.getHeight();
			dataManager.saveImage(lattice, "lattices", fileName + ".jpeg");
			dataManager.saveImage(width_vs_time, "plots", fileName + ".jpeg");
		}
		if (control.getBoolean("Plot All")) {
			plotAll();
			dataManager.saveImage(width_vs_time, ".", "masterPlot.jpeg");
			dataManager.saveImage(width_vs_length, ".", "alphaPlot.jpeg");
		}
		
	}
	
	private double calculateAverageBeta() {
		double sum = 0;
		for(LargeSystemDeposition m: models) {
			sum += m.getBeta();
		}
		return sum/(double)models.size();
	}
	
	// Runs regression of lnw_avg vs lnL
	private double calculateAlpha() {
		
		//wrap lnL, lnw_avg in Functions
		Function lnw_avg = new Function() {
			public double val(double x) {
				return models.get((int)x).getSaturatedLnw_avg();
			}
		};
		Function lnL = new Function() {
			public double val(double x) {
				return Math.log(models.get((int)x).getLength());
			}
		};
		//Pass functions to regression
		lnw_vs_lnL = new LinearRegression(lnL, lnw_avg, 0, (double)models.size()-1, 1);
		return lnw_vs_lnL.m();
	}
	
	public boolean exists(LargeSystemDeposition m) {
		return models.contains(m);
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
	final static double alphaOverBeta = 1.5D;
	
	private long expectedT_cross(int L) {
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

	private long pointModulus(long t) {
		if (t < expectedT_cross(model.getLength())) {
			return growthModulus(t);
		}
		return saturationModulus(t);
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
	private void plotAll() {
		
		// width_vs_time
		width_vs_time.clearDrawables();
		for (int i = 0; i < models.size(); i++) {
			LargeSystemDeposition m = models.get(i);
			//plot entire width array, set color
			width_vs_time.setMarkerColor(i, colors[i%colors.length]);
			long time = m.getTime();
			for (long t = 1; t <= time; t++) {
				long mod = pointModulus(t)*models.size();
				if (t % mod == 0) {
					width_vs_time.append(i, Math.log(t),
							 Math.log(m.getWidth(t)));
				}
			}	
		}
		
		// width_vs_length
		width_vs_length.clearDrawables();
		for (int i = 0; i < models.size(); i++) {
			LargeSystemDeposition m = models.get(i);
			width_vs_length.append(i, Math.log(m.getLength()), m.getSaturatedLnw_avg());
		}
		// Draw linear regression
		width_vs_length.addDrawable(lnw_vs_lnL);
	}
	
	// Color palette for plotting models
	private Color[] colors = {Color.CYAN,
							  Color.ORANGE,
							  Color.MAGENTA,
							  Color.PINK,
							  Color.YELLOW,
							  Color.RED,
							  Color.GREEN,
							  Color.BLUE};
	
/**************************
 * DB Operation Callbacks *
 **************************/

	private AlertDialog dbPushAlert;
	private AlertDialog dbPullAlert;
	
	private DBOperationCallback onPush = new DBOperationCallback() {

		@Override
		public void onOperationStarted() {
			dbPushAlert = new AlertDialog(
				"Push Alert",
				"Pushing records from memory to local database.\nThis may take several minutes."
			);
		}

		@Override
		public void onOperationCompleted(final long opTime) {
			
			dbPushAlert.showMessage(
				"Push completed in "+(opTime/1000L)+" s."
			);
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(5000L);
					} catch (InterruptedException ie) {}
					
					dbPushAlert.dispose();
				}
				
			}).run();
		}
		
	};
	
	private DBOperationCallback onPull = new DBOperationCallback() {

		@Override
		public void onOperationStarted() {
			dbPullAlert = new AlertDialog(
				"Pull Alert",
				"Pulling records from memory to local database.\nThis may take several minutes."
			);
		}

		@Override
		public void onOperationCompleted(final long opTime) {
			
			dbPullAlert.showMessage(
				"Pull completed in "+(opTime/1000L)+" s."
			);
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(5000L);
					} catch (InterruptedException ie) {}
					
					dbPullAlert.dispose();
				}
				
			}).run();
		}
		
	};
	
	
/********
 * Main *
 ********/	
	
	public static void main(String[] args) {
		SimulationControl.createApp(new DepositionControl());
	}

}
