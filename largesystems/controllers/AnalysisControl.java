package bdm.largesystems.controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import bdm.largesystems.controllers.VisualizationManager.Point;
import bdm.largesystems.utils.InputDialog;
import bdm.largesystems.utils.LinearRegression;
import bdm.largesystems.utils.ModelGroupIdentifier;
import bdm.largesystems.utils.MySQLClient;

/**
 * AnalysisControl.java
 * Created:	5 January 2015
 * @author Tyler
 * 
 * A GUI control used to run analysis
 * of the models and aggregate data.
 * Connects to the local MySQL database
 * to query and analyze past trial data.
 */
public class AnalysisControl {

	/**
	 * Bridge to the local MySQL database
	 * containing past model data.
	 */
	protected MySQLClient db;
	
	/**
	 * Object for handling plots and visualizations.
	 */
	protected VisualizationManager visManager;
	
	/**
	 * An ordered map of analysis function names to Runnables
	 * for executing each function.
	 */
	protected LinkedHashMap<String, AnalysisFunction> analysisFunctions;
	
	/**
	 * An array of descriptions of analysis functions
	 * that this control can execute.
	 */
	protected String[] functionNames;
	
	/**
	 * Name of models table.
	 */
	protected final static String DB_TABLE_MODELS = "models";
	
	/**
	 * Name of models table.
	 */
	private static final String DB_TABLE_AVERAGES = "logarithmic_averages";
	
	/**
	 * A set of parameters which identify a unique model.
	 */
	protected final static String[] COMPLETE_MODEL_PARAMS = {
			"trial",
			"modelId",
			"L",
			"H",
			"x",
			"p_diff",
			"l_0"
	};


/*************
 * Constants *
 *************/
	
	/**
	 * Default scaling exponent.
	 */
	public final static double Z_DEFAULT = 2;
	
	public final static String DEFAULT_INPUT_MSG = "Identify models to analyze";
	

/******************
 * Nested Classes *
 ******************/
	
	/**
	 * An interface requiring support for handling
	 * analysis input parameters.
	 * 
	 * @author Tyler
	 */
	protected interface Analyzer {
		
		public void analyze(HashMap<String, String> input);
		
	}
	
	/**
	 * Abstracts functionality for generating InputDialogs
	 * and running callbacks into a single wrapper class
	 * for all AnalysisControl functions.
	 * 
	 * @author Tyler
	 */
	protected static class AnalysisFunction {
		
		String inputMessage;
		String[] inputParams;
		Analyzer analyzer;
		
		public AnalysisFunction(String inputMessage, String[] inputParams, Analyzer analyzer) {
			this.inputMessage = inputMessage;
			this.inputParams = inputParams;
			this.analyzer = analyzer;
		}
		
		public void analyze() {
			
			// Create input dialog to enable user
			// specification of trials over which
			// to run average
			
			new InputDialog(
				inputMessage,
				inputParams,
				(HashMap<String, String> input) -> analyzer.analyze(input)
			);
			
		}
		
		public static AnalysisFunction defaultInputAf(Analyzer analyzer) {
			return new AnalysisFunction(
					AnalysisControl.DEFAULT_INPUT_MSG,
					AnalysisControl.COMPLETE_MODEL_PARAMS,
					analyzer
			);
		}
		
	}
	
	/**
	 * Used to compute running averages.
	 * @author Tyler
	 */
	public class Average {
		double val;
		int samples;
	}
	
	/**
	 * An Integer object which is mutable
	 * and thus the value of final objects
	 * can be changed.
	 * @author Tyler
	 *
	 */
	public class MutableInteger {
		public int i;
		public MutableInteger(int I) {i = I;}
	}
	
	
/******************
 * Initialization *
 ******************/
	
	public AnalysisControl() {
		
		this(new VisualizationManager("BallisticDiffusionModel"));
		
	}
	
	public AnalysisControl(VisualizationManager vm) {
		
		visManager = vm;
		db = MySQLClient.getSingleton("depositions", "bdm", "d3po$ition$");
		
		initAnalysisFunctions();
		initControlWindow();
		
	}
	
	/**
	 * Enumerates the analysis functions by adding
	 * them to the function map.
	 */
	public void initAnalysisFunctions() {
		
		// Function map
		analysisFunctions = new LinkedHashMap<String, AnalysisFunction>();
		
		analysisFunctions.put("Calculate average", new AnalysisFunction(
			"Enter valid parameter name ",
			new String[] {"Parameter name"},
			(HashMap<String, String> input) -> {
				// Run average for the given parameter
				final String paramName = input.get("Parameter name");
				AnalysisFunction.defaultInputAf(new Analyzer() {
					@Override
					public void analyze(HashMap<String, String> input) {
						calcAvg(paramName, new ModelGroupIdentifier(input));
					}
				}).analyze();
			}
		));

		analysisFunctions.put("Scaled avg width plot", new AnalysisFunction(
			AnalysisControl.DEFAULT_INPUT_MSG,
			new String[] {	// Default params plus z, S minus H, trial, modelId
				"L",
				"x",
				"z",
				"S",
				"p_diff",
				"l_0"
			},
			(HashMap<String, String> input) -> {
				String zStr; double z = Z_DEFAULT;
				if ((zStr = input.remove("z")) != null && !zStr.equals(""))
					z = Double.parseDouble(zStr);
				scaledAvgWidthPlot(new ModelGroupIdentifier(input), z);
			}	
		));

		analysisFunctions.put("beta vs x plot", AnalysisFunction.defaultInputAf(
			(HashMap<String, String> input) -> {
				betaVsXPlot(new ModelGroupIdentifier(input));
			}	
		));

		analysisFunctions.put("alpha plot", AnalysisFunction.defaultInputAf(
			(HashMap<String, String> input) -> {
				alphaPlot(new ModelGroupIdentifier(input));
			}	
		));
		
		
		// String[] containing function names
		functionNames = new String[analysisFunctions.size()];
		
		int i = 0;
		for (String name: analysisFunctions.keySet()) {
			functionNames[i++] = name;
		}
		
	}
	
	/**
	 * Shows a JOptionPane allowing selection
	 * and execution of all analysis functions.
	 */
	private void initControlWindow() {
		
		Thread t = new Thread( () -> {
				
			// Create dialog
			String functionName = (String)JOptionPane.showInputDialog(
					null, "Select an analysis function",
					"Analysis Control",JOptionPane.QUESTION_MESSAGE,
					null,functionNames,"Add a new friend"
			);
			
			// Invoke callback
			if (analysisFunctions.get(functionName) != null)
				analysisFunctions.get(functionName).analyze();
			
		});
		t.start();
		
	}
	
	
/**********************
 * Function Callbacks *
 **********************/

	/**
	 * Initiates an {@link InputDialog} to request
	 * parameters and then displays the result of
	 * calculation.
	 */
	public void calcAvg(String paramName, ModelGroupIdentifier mgi) {
		
		double sigma_param = 0;	// Sum over all param values
		int N = 0;	// Number of samples
		
		ResultSet results = selectWhere(DB_TABLE_MODELS, mgi);

		try {
			
			while(results.next()) {
				sigma_param += results.getDouble(paramName);
				N++;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		showMessage(paramName + "_avg = " + (sigma_param/N));
		initControlWindow();
		
	}
	
	/**
	 * Initiates an {@link InputDialog} to request
	 * parameters and then displays a scaled average
	 * width plot.
	 *
	 * @param z scaling exponent alpha/beta
	 */
	public void scaledAvgWidthPlot(ModelGroupIdentifier mgi, double z) {
		
		// Setup plots
		visManager.getWidthVsTime().setVisible(true);
		
		System.out.println("SELECT DISTINCT L FROM "+DB_TABLE_AVERAGES+" WHERE " + mgi.sqlWhereClause());
		
		// Query distinct lengths
		ResultSet lengths = db.query(
			"SELECT DISTINCT L FROM "+DB_TABLE_AVERAGES+" WHERE " + mgi.sqlWhereClause()
		);

		// Query total number of points to plot
		ResultSet count = db.query(
			"SELECT count(*) FROM "+DB_TABLE_AVERAGES+" WHERE " + mgi.sqlWhereClause()
		);
		
		// Query all data, ordering by length to ensure all models are plotted
		ResultSet data = db.query(
			"SELECT * FROM " + DB_TABLE_AVERAGES +
			" WHERE " + mgi.sqlWhereClause() +
			" ORDER BY L DESC"
		);
		
		try {
			
			// Determine total number of records
			count.next();
			int nPoints = count.getInt(1);
			
			// Store distinct lengths in ArrayList
			ArrayList<Integer> lengthList = new ArrayList<Integer>();
			while (lengths.next()) {
				lengthList.add(lengths.getInt(1));
			}
			
			// Delegate plotting to visManager
			visManager.scaledAvgWidthPlot(lengthList, data, nPoints, z);

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			showMessage("Null result set returned");
			npe.printStackTrace();
		}
		
		// Relaunch control window
		initControlWindow();		
		
	}
	
	/**
	 * Queries records identified by mgi, computes the 
	 * average beta values for each distinct x and passes
	 * the averages to 
	 * {@link bdm.largesystems.controllers.VisualizationManager}.
	 * 
	 * @param mgi A {@link ModelGroupIdentifier}
	 */
	public void betaVsXPlot(ModelGroupIdentifier mgi) {
		
		// Query data
		ResultSet data = selectWhere(DB_TABLE_MODELS, mgi);
		
		try {
			
			HashMap<Double, Average> beta_avg = new HashMap<Double, Average>();
			
			while(data.next()) {
				
				double x = data.getDouble("x");
				double beta = data.getDouble("beta");
				
				if (beta_avg.containsKey(x)) {
					Average avg = beta_avg.get(x);
					avg.val = (avg.val * avg.samples + beta) / (avg.samples + 1);
					avg.samples = avg.samples + 1;
					beta_avg.put(x, avg);
				}
				else {
					Average avg = new Average();
					avg.val = beta;
					avg.samples = 1;
					beta_avg.put(x, avg);
				}
				
			}
			
			// Pass averages to visManager to plot
			visManager.plotBetaVsX(beta_avg);
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			showMessage("Null result set returned");
			npe.printStackTrace();
		}
		
		// Relaunch control window
		initControlWindow();
		
	}
	
	/**
	 * Queries L and lnw_avg values for the given
	 * {@link bdm.largesystems.utils.ModelGroupIdentifier},
	 * create a linear regression and passes it to
	 * the member {@link VisualizationManager} along
	 * with the points for plotting.
	 * 
	 * @param mgi A {@link ModelGroupIdentifier}
	 */
	public void alphaPlot(ModelGroupIdentifier mgi) {
		
		ResultSet data = db.query(
			"SELECT L, lnw_avg FROM " +
			" models WHERE " + mgi.sqlWhereClause() +
			" ORDER BY L ASC"
		);
		
		final ArrayList<Point> lnw_avgByL = new ArrayList<>();
		
		try {
			
			// Column indices
			int iL = 1;
			int iLnw_avg = 2;
			
			while (data.next()) {
				
				// Grab Data
				int L = data.getInt(iL);
				double lnw_avg = data.getDouble(iLnw_avg);
				
				// Add to list
				lnw_avgByL.add(new Point(L, Math.log(L), lnw_avg));
				
			}
			
			// Create LinearRegression
			LinearRegression lnw_vs_lnL = new LinearRegression(
					// Independent variable
					(double x) -> {
						return lnw_avgByL.get((int)x).x;
					},
					// Dependent variable
					(double x) -> {
						return lnw_avgByL.get((int)x).y;						
					},
					// x_i
					0,
					// x_f
					lnw_avgByL.size()-1,
					// dx
					1
			);
			
			// Plot
			visManager.logPlotWidthVsLength(lnw_avgByL, lnw_vs_lnL);
			
			// Show alpha value
			showMessage("alpha = " + lnw_vs_lnL.m() +
						"\nR^2 = " + lnw_vs_lnL.R2());
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		// Relaunch control window
		initControlWindow();
		
	}
	

/*****************
 * SQL Utilities *
 *****************/
	
	public ResultSet selectWhere(String table, ModelGroupIdentifier mgi) {

//		System.out.println(
//			"SELECT * FROM " + table +
//			" WHERE " + mgi.sqlWhereClause()
//		);
		return db.query(
			"SELECT * FROM " + table +
			" WHERE " + mgi.sqlWhereClause()
		);
		
	}
	

/********************
 * Helper Functions *
 ********************/
	
	public void showMessage(String message) {
		JOptionPane.showMessageDialog(null, message);
	}
	
	
/********
 * Main *
 ********/
	
	public static void main(String[] args) {
		new AnalysisControl();
	}
	
}
