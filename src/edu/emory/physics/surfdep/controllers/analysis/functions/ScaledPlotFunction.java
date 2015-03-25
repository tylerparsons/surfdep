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
package edu.emory.physics.surfdep.controllers.analysis.functions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import edu.emory.physics.surfdep.controllers.VisualizationManager;
import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.utils.InputDialog;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;
import edu.emory.physics.surfdep.utils.MySQLClient;

public class ScaledPlotFunction extends AnalysisFunction {

	protected static AnalysisControl control;
	
	protected static Consumer<HashMap<String, String>> ANALYZER = 
		(HashMap<String, String> input) -> {
			String zStr; final double z;
			if ((zStr = input.remove("z")) != null && !zStr.equals(""))
				z = Double.parseDouble(zStr);
			else
				z = AnalysisControl.Z_DEFAULT;
			(control = AnalysisControl.getSingleton()).createSavingAnalyzer(
				"Scaled avg width plot",
				(ModelGroupIdentifier mgi) -> {
					scaledAvgWidthPlot(mgi, z);
				}
			).accept(input);
		};
	
	public ScaledPlotFunction() {
		super(
			AnalysisControl.DEFAULT_INPUT_MSG,
			new String[] {	// Default params plus z, S minus H, trial, modelId
				"L",
				"x",
				"z",
				"S",
				"p_diff",
				"l_0"
			},
			ANALYZER
		);
		control = AnalysisControl.getSingleton();
	}
	
	/**
	 * Initiates an {@link InputDialog} to request
	 * parameters and then displays a scaled average
	 * width plot.
	 *
	 * @param z scaling exponent alpha/beta
	 */
	public static void scaledAvgWidthPlot(ModelGroupIdentifier mgi, double z) {
		
		// Setup plots
		VisualizationManager visManager = control.getVisManager();
		visManager.getWidthVsTime().setVisible(true);
		
		// Query distinct lengths
		MySQLClient db = control.getDb();
		ResultSet lengths = db.query(
			"SELECT DISTINCT L FROM "+AnalysisControl.DB_TABLE_AVERAGES+" WHERE " + mgi.sqlWhereClause()
		);

		// Query total number of points to plot
		ResultSet count = db.query(
			"SELECT count(*) FROM "+AnalysisControl.DB_TABLE_AVERAGES+" WHERE " + mgi.sqlWhereClause()
		);
		
		// Query all data, ordering by length to ensure all models are plotted
		ResultSet data = db.query(
			"SELECT * FROM " + AnalysisControl.DB_TABLE_AVERAGES +
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
			control.showMessage("Null result set returned");
			npe.printStackTrace();
		}
		
		// Relaunch control window
		control.initControlWindow();		
		
	}
	
}
