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
import edu.emory.physics.surfdep.controllers.analysis.functions.CalcSlopeFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.ScaledPlotFunction;
import edu.emory.physics.surfdep.controllers.analysis.functions.UnscaledPlotFunction;

import edu.emory.physics.surfdep.utils.CredentialLoader;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;
import edu.emory.physics.surfdep.utils.MySQLClient;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

/**
 * AnalysisControl.java
 * Created:	5 January 2015
 * 
 * A GUI control used to run analysis
 * of the models and aggregate data.
 * Connects to the local MySQL database
 * to query and analyze past trial data.
 * 
 * @author Tyler Parsons
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
	protected String[] functionTitles;
	
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
		public int value;
		public MutableInteger(int v) {value = v;}
	}
	
	
/******************
 * Initialization *
 ******************/
	
	public AnalysisControl() {
		this(new VisualizationManager("BallisticDiffusionModel"));
	}
	
	public AnalysisControl(VisualizationManager vm) {
		
		visManager = vm;
		
		try {
			
			StringBuilder username = new StringBuilder();
			StringBuilder password = new StringBuilder();
			
			CredentialLoader.load("db_credentials.txt", username, password);
			
			db = MySQLClient.getSingleton("depositions", username.toString(),
														 password.toString());
		} catch (Exception e) {
			e.printStackTrace();
			// Cannot continue program execution without a database connection
			assert(false);
		}
		
		dataManager = new DataManager(TXT_FILE_PATH);
		initAnalysisFunctions();
	}
	
	/**
	 * Enumerates the analysis functions by adding
	 * them to the function map.
	 */
	private void initAnalysisFunctions() {
		
		AnalysisFunction[] functionList = {
			new CalcAvgFunction(this),
			new ScaledPlotFunction(this),
			new UnscaledPlotFunction(this),
			new AvgTxFunction(this),
			new CalcSlopeFunction(this),
			new BetaPlotFunction(this),
			new AlphaPlotFunction(this)
		};
		
		// Function map for lookup of name by title
		analysisFunctions = new LinkedHashMap<String, AnalysisFunction>();
		// String[] containing function names
		functionTitles = new String[functionList.length];
		
		int i = 0;
		for (AnalysisFunction function: functionList) {
			analysisFunctions.put(function.getTitle(), function);
			functionTitles[i++] = function.getTitle();
		}
		
		storeFilenames(functionTitles);
		
	}
	
	protected void storeFilenames(String[] functionTitles) {
		if (filenames == null) filenames = new HashMap<>();
		for (String title: functionTitles) {
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
	public void showControlWindow() {
		
		Thread t = new Thread( () -> {
				
			// Create dialog
			String functionName = (String)JOptionPane.showInputDialog(
					null, "Select an analysis function",
					"Analysis Control",JOptionPane.QUESTION_MESSAGE,
					null,functionTitles,"Calculate averages"
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
		HashMap<String, String> params = new HashMap<>(mgi.getInputParams());
		// csv
//		dataManager.printToCSV(
//			filenames.get(title),
//			dataManager.getCSVOutput(params)
//		);
		// txt
		params.put("sqlWhereClause", mgi.sqlWhereClause());
		dataManager.saveToTxt(params);
	}
	
	public void saveData(String title, HashMap<String, Double> params) {
		// csv
//		dataManager.printToCSV(
//			filenames.get(title),
//			dataManager.getCSVOutput(params)
//		);
		// txt
		dataManager.saveToTxt(params);
	}

	public void printToTxt(String output) {
		dataManager.printToTxt(output);
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
	
/********
 * Main *
 ********/
	
	public static void main(String[] args) {
		new AnalysisControl().showControlWindow();
	}
	
}
