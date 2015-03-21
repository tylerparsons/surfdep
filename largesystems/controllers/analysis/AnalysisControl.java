package surfdep.largesystems.controllers.analysis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import surfdep.largesystems.controllers.DataManager;
import surfdep.largesystems.controllers.trials.DepositionControl;
import surfdep.largesystems.controllers.VisualizationManager;
import surfdep.largesystems.controllers.VisualizationManager.Point;
import surfdep.largesystems.utils.InputDialog;
import surfdep.largesystems.utils.LinearRegression;
import surfdep.largesystems.utils.ModelGroupIdentifier;
import surfdep.largesystems.utils.MySQLClient;

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
	 * Used for saving data to txt file.
	 */
	protected DataManager dataManager;
	
	/**
	 * Object for handling plots and visualizations.
	 */
	protected VisualizationManager visManager;
	
	/**
	 * An ordered map of analysis function names to AnalysisFunctions
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
	 * Path to data txt file.
	 */
	private static final String TXT_FILE_PATH = "data\\analysis\\analysis_data.txt";
	
	/**
	 * Path to data txt file.
	 */
	private static final String CSV_FILE_DIR = "data\\analysis\\";;
	
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
	
	/**
	 * Map of function titles to associated data file names.
	 */
	protected HashMap<String, String> filenames;


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
		dataManager = new DataManager(TXT_FILE_PATH);
		
		initAnalysisFunctions();
		initControlWindow();
		
	}
	
	public Consumer<HashMap<String, String>> createSavingAnalyzer(
		String title,
		Consumer<ModelGroupIdentifier> analyzer
	) {
		return (HashMap<String, String> input) -> {
			ModelGroupIdentifier mgi = new ModelGroupIdentifier(input);
			dataManager.printToTxt("\n"+title);
			saveData(title, mgi);
			analyzer.accept(mgi);
		};
	}
	
	/**
	 * Enumerates the analysis functions by adding
	 * them to the function map.
	 */
	public void initAnalysisFunctions() {
		
		// Function map
		analysisFunctions = new LinkedHashMap<String, AnalysisFunction>();
		
		analysisFunctions.put("Calculate averages", new AnalysisFunction(
			"Enter valid parameter name ",
			new String[] {"Parameter names"},
			(HashMap<String, String> input) -> {
				// Run average for the given parameter
				final String[] paramNames = input.get("Parameter names").split(",");
				new AnalysisFunction(createSavingAnalyzer("Calculate averages",
					new Consumer<ModelGroupIdentifier>() {
						@Override
						public void accept(ModelGroupIdentifier mgi) {
							calcAvgs(mgi, paramNames);
						}
					}
				)).analyze();
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
		
		analysisFunctions.put("avg t_x values", new AnalysisFunction(
			createSavingAnalyzer("avg t_x values", (ModelGroupIdentifier mgi) -> {
				calcAvgs("avg t_x values", mgi, DepositionControl.T_X_INPUT_KEYS);
			}
		)));
		
		analysisFunctions.put("beta vs x plot", new AnalysisFunction(
			createSavingAnalyzer("beta vs x plot", (ModelGroupIdentifier mgi) -> {
				betaVsXPlot(mgi);
			}
		)));

		analysisFunctions.put("alpha plot", new AnalysisFunction(
			createSavingAnalyzer("alpha plot", (ModelGroupIdentifier mgi) -> {
				alphaPlot(mgi);
			}
		)));
		
		// String[] containing function names
		functionNames = new String[analysisFunctions.size()];
		
		int i = 0;
		for (String name: analysisFunctions.keySet()) {
			functionNames[i++] = name;
		}
		
		storeFilenames(functionNames);
		
	}
	
	protected void storeFilenames(String[] titles) {
		if (filenames == null) filenames = new HashMap<>();
		for (String title: titles) {
			filenames.put(
				title, 
				CSV_FILE_DIR+title.replaceAll("\\s", "_").toLowerCase()+".csv"
			);
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
					null,functionNames,"Calculate averages"
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
	 * Calculates and displays a variable number of averages.
	 */
	public void calcAvgs(String title, ModelGroupIdentifier mgi, String ... paramNames) {
		
		HashMap<String, Double> data = new HashMap<>();
		Average[] avgs = avg(mgi, paramNames);
		String result = "";
		
		for (int i = 0; i < avgs.length; i++) {
			result += "avg " + paramNames[i]
					+  " = "  + avgs[i].val + "\n";
			data.put(paramNames[i], avgs[i].val);
		}
		
		saveData(title, data);
		showMessage(result);
		initControlWindow();
		
	}
	
	/**
	 * Calculates and displays a variable number of averages.
	 */
	public void calcAvgs(ModelGroupIdentifier mgi, String ... paramNames) {
		calcAvgs("Calculate avgs", mgi, paramNames);
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
	 * {@link surfdep.largesystems.controllers.VisualizationManager}.
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
					beta_avg.put(x, new Average(beta, 1));
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
	 * {@link surfdep.largesystems.utils.ModelGroupIdentifier},
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
	
	public Average[] avg(ModelGroupIdentifier mgi, String ... paramNames) {
		
		// Query averages for mgi
		String[] avgKeys = new String[paramNames.length];
		String columns = "";
		for (int i = 0; i < avgKeys.length; i++) {
			avgKeys[i] = "avg(" + paramNames[i] + ")";
			columns += avgKeys[i] +",";
		}
		// Add count function
		columns += "count(*)";
		
		ResultSet results = db.query(
			"SELECT " + columns + " FROM " + DB_TABLE_MODELS + 
			" WHERE " + mgi.sqlWhereClause()
		);
		
		// Parse results into Averages
		Average[] avgs = new Average[paramNames.length];
		try {
			
			results.first();
			int count = results.getInt("count(*)");
			for (int i = 0; i < avgs.length; i++)
					avgs[i] = new Average(results.getDouble(avgKeys[i]), count);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return avgs;
		
	}

	public void saveData(String title, ModelGroupIdentifier mgi) {
		HashMap<String, String> params = mgi.getInputParams();
		// csv
		dataManager.printToCSV(
			filenames.get(title),
			dataManager.getCSVOutput(params)
		);
		// txt
		params.put("sqlWhereClause", mgi.sqlWhereClause());
		dataManager.saveToTxt(params);
	}
	
	public void saveData(String title, HashMap<String, Double> params) {
		// csv
		dataManager.printToCSV(
			filenames.get(title),
			dataManager.getCSVOutput(params)
		);
		// txt
		dataManager.saveToTxt(params);
	}
	
/********
 * Main *
 ********/
	
	public static void main(String[] args) {
		new AnalysisControl();
	}
	
}
