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
import java.util.HashMap;

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.controllers.analysis.Average;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;
import edu.emory.physics.surfdep.utils.MySQLClient;

public class CalcAvgFunction extends SavingAnalysisFunction {
	
	public static final String TITLE = "calc avg";
		
	public CalcAvgFunction(AnalysisControl control) {
		super(
			TITLE,
			"Enter valid parameter name ",
			new String[] {"Parameter names"},
			control
		);
	}
	
	public CalcAvgFunction(String title, AnalysisControl control) {
		super(title, control);
	}
	
	@Override
	public void accept(HashMap<String, String> input) {
		// Determine parameters to average
		final String[] paramNames = input.remove("Parameter names").split(",");
		// Launch analysis function to identify models for which to
		// compute average values for the given parameters
		new AnalysisFunction(title, control) {
			@Override
			public void accept(HashMap<String, String> in) {
				calcAvgs(new ModelGroupIdentifier(in), paramNames);
			}
		}.analyze();
	}
	
	/**
	 * Calculates and displays a variable number of averages.
	 */
	protected void calcAvgs(String title, ModelGroupIdentifier mgi, String ... paramNames) {
		
		HashMap<String, Double> data = new HashMap<>();
		Average[] avgs = avg(mgi, paramNames);
		String result = "";
		
		for (int i = 0; i < avgs.length; i++) {
			result += "avg " + paramNames[i]
					+  " = "  + avgs[i].val + "\n";
			data.put(paramNames[i], avgs[i].val);
		}
		
		control.saveData(title, data);
		control.showMessage(result);
		control.showControlWindow();
		
	}
	
	/**
	 * Calculates and displays a variable number of averages.
	 */
	protected void calcAvgs(ModelGroupIdentifier mgi, String ... paramNames) {
		calcAvgs("Calculate averages", mgi, paramNames);
	}
	
	protected Average[] avg(ModelGroupIdentifier mgi, String ... paramNames) {
		
		// Query averages for mgi
		String[] avgKeys = new String[paramNames.length];
		String columns = "";
		for (int i = 0; i < avgKeys.length; i++) {
			avgKeys[i] = "avg(" + paramNames[i] + ")";
			columns += avgKeys[i] +",";
		}
		// Add count function
		columns += "count(*)";
		
		MySQLClient db = control.getDb();
		ResultSet results = db.query(
			"SELECT " + columns + " FROM " + AnalysisControl.DB_TABLE_MODELS + 
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
	
}
