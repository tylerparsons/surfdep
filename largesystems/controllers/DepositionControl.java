package bdm.largesystems.controllers;

import bdm.largesystems.models.BallisticDiffusionModel;
import bdm.largesystems.models.LargeSystemDeposition;
import bdm.largesystems.utils.AlertDialog;
import bdm.largesystems.utils.EmbeddedDBArray.DBOperationCallback;
import bdm.largesystems.utils.InputDialog;
import bdm.largesystems.utils.LinearRegression;
import bdm.largesystems.utils.LinearRegression.Function;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.opensourcephysics.controls.AbstractSimulation;
import org.opensourcephysics.controls.SimulationControl;

/**
 * DepositionControl.java
 * Created:	7 May 2014
 * @author	Tyler Parsons
 *  
 * An executable class that manages instantiation
 * of models, visualization, data analysis,
 * UI and I/O of parameters.
 */
public class DepositionControl extends AbstractSimulation {

	private ArrayList<LargeSystemDeposition> models;
	private LargeSystemDeposition model;
	private LinearRegression lnw_vs_lnL;
	
	private DataManager dataManager;
	private VisualizationManager visManager;
	
	// Auto incremented id associated with each model
	
	// Invoked when analysis of a model is finished.
	private Runnable analysisCallback = null;
	
	// Static trial parameters
	private static int modelId = 0;
	private static int remainingTrials;
	private static int clearMod;
	private static int plotAllMod;
	
	// Driectory in which simulation data is stored
	private final static String DIR_DATA_ROOT = "data\\";

	public long pointsConsidered = 0;
	
/**************************
 * Initialization Methods *
 **************************/
	
	/**
	 * Constructor
	 */
	public DepositionControl() {
		
		//set up visualizations
		model = new BallisticDiffusionModel();
		models = new ArrayList<LargeSystemDeposition>();
		
		visManager = new VisualizationManager(model.getClass().getName());
		dataManager = new DataManager(
			DIR_DATA_ROOT + "id_log.txt",
			DIR_DATA_ROOT + "deposition_data.txt",
			DIR_DATA_ROOT + "deposition_data.csv"
		);
		dataManager.startTrial();
	}
	
	public void initialize() {
		
		//Create a new model for each simulation
		model = new BallisticDiffusionModel();
		
		// Setup plots
		visManager.initPlots();
		
		// Obtain parameters from control
		HashMap<String, Double> params = model.parameters();
		for (String name: params.keySet()) {
			params.put(name, control.getDouble(name));
		}
		params.put("modelId", (double)++modelId);
		model.init(params);
		
		// Enable database operation alerts
		model.registerDBOperationCallbacks(onPush, onPull);
		
		if (control.getBoolean("Enable Visualizations")) {
			visManager.initVisuals(model);
		}		
	}
	
	public void initialize(HashMap<String, Double> params) {
		
		//Create a new model for each simulation
		model = new BallisticDiffusionModel();	
		
		// Setup plots
		visManager.initPlots();
		
		//Set Parameters
		params.put("modelId", (double)modelId++);
		model.init(params);
		
		// Enable database operation alerts
		model.registerDBOperationCallbacks(onPush, onPull);
		
		// Enable/disable visualizations
		if (params.get("Enable Visualizations") != null
		&&	params.remove("Enable Visualizations") == 1) {
			visManager.initVisuals(model);
		}
		else {
			visManager.hideLattice();
		}
		
		// Set steps per display if applicable
		if(params.get("stepsPerDisplay") != null)
			setStepsPerDisplay(params.remove("stepsPerDisplay").intValue());
		
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
	
	protected void doStep() {
		
		// Stop before model reaches maximum height
		if (model.getAverageHeight() > 0.9*model.getHeight()) {
			stopSimulation();
			return;
		}
		
//		if (model.getTime() == EmbeddedDBArray.MAX_ARRAY_SIZE - 1) {
//			stopSimulation();
//			return;
//		}
		
		// Catch and report any exceptions without 
		// losing simulation to runtime errors
		try {
			model.step();
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			stopSimulation();
			return;
		}
		
		// Plot points according to the distribution
		// given by point modulus, which is intended
		// to prevent overflow of points on the plot
		// frame
		long t = model.getTime();
		if (model.measure(t)) {
			long mod = visManager.staticPointModulus(model.getHeight());
			if(pointsConsidered++ % mod == 0
			|| t < visManager.expectedT_cross(model.getLength())) {
				// Plot using h_avg time scaling				
				visManager.logPlotWidth(
						model.getLength(),
						Math.log(model.getScaledTime()),
						Math.log(model.getWidth(model.getScaledTime()))
				);
				// Save average width for this model type
				// for future scaled plots
				dataManager.updateScaledW_avg(model);
			}
		}
	}
	
	public void stopRunning() {
		
		//Save model for later use, avoiding duplicate entries in model set
		if (!exists(model))
			models.add(model);
		
		//Request input of t_cross and implement input callback
		//to analyze model
		
		// Dialog for inputting t_x values
		new InputDialog(
			"Input t_x values",
			new String[] {"t_0", "t_x1", "t_x2"},
			(HashMap<String, String> input) -> {
				// Grab parameters from input map, pass to analyzeModel
				String t_0, t_x1, t_x2;
				analyzeModel(
					(int) Math.exp(Double.parseDouble(
							(t_0 = input.get("t_0").trim()).equals("") ? "0" : t_0)),
					(int) Math.exp(Double.parseDouble(
							(t_x1 = input.get("t_x1").trim()).equals("") ? "0" : t_x1)),
					(int) Math.exp(Double.parseDouble(
							(t_x2 = input.get("t_x2").trim()).equals("") ? "0" : t_x2))
				);
			}
		);
		
	}
	
	/**
	 * Dereferences all models and suggests garbage collection.
	 */
	public void clearMemory() {
		models.clear();
		model = null;
		System.gc();
	}
	
	public void setAnalysisCallback(Runnable callback) {
		analysisCallback = callback;
	}
	
	
/****************
 * Calculations *
 ****************/	
	
	private void analyzeModel(int t_0, int t_x1, int t_x2) {
		
		//Run calculations
		model.calculateBeta(t_0, t_x1);
		model.calculateSaturatedLnw_avg(t_x2);
		double beta_avg = calculateAverageBeta();
		double alpha = calculateAlpha();
		
		//Wrap data in Parameters to pass to dataManager as list
		HashMap<String, Double> addlParams = new HashMap<String, Double>();
		addlParams.put("h_avg", new Double(model.getAverageHeight()));
		addlParams.put("w", new Double(model.getWidth(model.getScaledTime())));
		addlParams.put("t", new Double(model.getTime()));
		addlParams.put("t_0", new Double(t_0));
		addlParams.put("t_x1", new Double(t_x1));
		addlParams.put("t_x2", new Double(t_x2));
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
			dataManager.saveAll(model, addlParams);
			String fileName = "L"+model.getLength()+"H"+model.getHeight()+"_"+modelId;
			dataManager.saveImage(visManager.getLattice(), "lattices", fileName + ".jpeg");
			dataManager.saveImage(visManager.getWidthVsTime(), "plots", fileName + ".jpeg");
		}
		if (control.getBoolean("Plot All")) {
			plotAll();
			dataManager.saveImage(visManager.getWidthVsTime(), ".", "masterPlot_"+modelId+".jpeg");
			dataManager.saveImage(visManager.getWidthVsLength(), ".", "alphaPlot_"+modelId+".jpeg");
		}
		
		// Invoke callback if one has been specified
		if (analysisCallback != null)
			analysisCallback.run();
		
	}
	
	private double calculateAverageBeta() {
		double sum = 0;
		for(LargeSystemDeposition m: models) {
			sum += m.getBeta();
		}
		return sum/(double)models.size();
	}
	
	/**
	 * Runs regression of lnw_avg vs lnL
	 * @return alpha
	 */
	private double calculateAlpha() {
		
		//wrap lnL, lnw_avg in Functions
		Function lnw_avg = (double x) -> {
			return models.get((int)x).getSaturatedLnw_avg();
		};
		Function lnL = (double x) -> {
			return Math.log(models.get((int)x).getLength());
		};
		//Pass functions to regression
		lnw_vs_lnL = new LinearRegression(lnL, lnw_avg, 0, (double)models.size()-1, 1);
		return lnw_vs_lnL.m();
	}

	protected void plotAll() {
		visManager.plotAllModels(models);
		visManager.logPlotWidthVsLength(models, lnw_vs_lnL);
	}
	
	public boolean exists(LargeSystemDeposition m) {
		return models.contains(m);
	}
	
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
			
			new Thread( () -> {
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException ie) {}
				
				dbPushAlert.dispose();				
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
			
			new Thread( () -> {
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException ie) {}
				
				dbPullAlert.dispose();				
			}).run();
		}
		
	};
	
	
/********
 * Main *
 ********/	
	
	/**
	 * Runs multiple trials. Reads in trial parameters
	 * from a txt file.
	 * 
	 * @param args Not used
	 */
	public static void main(String[] args) {
		
		// Create Simulation
		final DepositionControl control = new DepositionControl();
		SimulationControl.createApp(control);
		
		// Read parameters
		String filePath = DIR_DATA_ROOT + "trial_params.txt";
		Scanner in = null;
		try {
			in = new Scanner(new File(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		final HashMap<String, Double> params = new HashMap<String, Double>();
		
		while (in.hasNext()) {
			String line = in.nextLine();
			System.out.println(line);
			String[] kvPair = line.split(":\t");
			params.put(kvPair[0], Double.parseDouble(kvPair[1]));
		}
		in.close();
		
		// Determine number of trials to run
		remainingTrials = params.remove("numTrials").intValue();
		clearMod = params.remove("clearMod").intValue();
		plotAllMod = params.remove("plotAllMod").intValue();
		
		// Run trials recursively
		control.initialize(params);
		control.setAnalysisCallback( () -> {
			
			if (--remainingTrials > 0) {
				
				if ((modelId+1) % plotAllMod == 0) {
					control.plotAll();
				}
				if ((modelId+1) % clearMod == 0) {
					control.clearMemory();
				}
				
				control.initialize(params);
				control.startSimulation();
			}
			
		});
		control.startSimulation();
		
	}

}
