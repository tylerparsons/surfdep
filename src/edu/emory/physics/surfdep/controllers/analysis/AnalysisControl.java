/**
 * Copyright 2015, Tyler Parsons
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.physics.surfdep.controllers.analysis;

import edu.emory.physics.surfdep.controllers.DataManager;
import edu.emory.physics.surfdep.controllers.VisualizationManager;
import edu.emory.physics.surfdep.controllers.analysis.functions.AlphaPlotFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.AnalysisFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.AvgTxFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.BetaPlotFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.CalcAvgFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.ScaledPlotFunction;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;
import edu.emory.physics.surfdep.utils.MySQLClient;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

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
	 * Singleton
	 */
	private static AnalysisControl singleton;
	
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
	public final static String DB_TABLE_MODELS = "models";
	
	/**
	 * Name of models table.
	 */
	public static final String DB_TABLE_AVERAGES = "logarithmic_averages";
	
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
	public final static String[] COMPLETE_MODEL_PARAMS = {
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
		
	}
	
	public void init() {
		
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
		
		analysisFunctions.put("Calculate averages", new CalcAvgFunction());
		analysisFunctions.put("Scaled avg width plot", new ScaledPlotFunction());
		analysisFunctions.put("avg t_x values", new AvgTxFunction());
		analysisFunctions.put("beta vs x plot", new BetaPlotFunction());
		analysisFunctions.put("alpha plot", new AlphaPlotFunction());
		
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
	public void initControlWindow() {
		
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
	
/***********
 * Getters *
 ***********/
	
	public MySQLClient getDb() {
		return db;
	}

	public VisualizationManager getVisManager() {
		return visManager;
	}
	
	public static AnalysisControl getSingleton() {
		return singleton;
	}
	
/********
 * Main *
 ********/
	
	public static void main(String[] args) {
		singleton = new AnalysisControl();
		singleton.init();
	}
	
}
