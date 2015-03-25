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
import java.util.function.Consumer;

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.controllers.analysis.Average;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;
import edu.emory.physics.surfdep.utils.MySQLClient;

public class CalcAvgFunction extends AnalysisFunction {
	
	protected static AnalysisControl control;
	
	protected static Consumer<HashMap<String, String>> ANALYZER = 
		(HashMap<String, String> input) -> {
		// Run average for the given parameter
		final String[] paramNames = input.get("Parameter names").split(",");
		new AnalysisFunction(
			(control = AnalysisControl.getSingleton()).createSavingAnalyzer("Calculate averages",
				(ModelGroupIdentifier mgi) -> {
					calcAvgs(mgi, paramNames);
				}
			)
		).analyze();
	};
	
	public CalcAvgFunction() {
		super(
			"Enter valid parameter name ",
			new String[] {"Parameter names"},
			ANALYZER
		);
		control = AnalysisControl.getSingleton();
	}
	
	protected CalcAvgFunction(Consumer<HashMap<String, String>> analyzer) {
		super(analyzer);
	}
	
	/**
	 * Calculates and displays a variable number of averages.
	 */
	protected static void calcAvgs(String title, ModelGroupIdentifier mgi, String ... paramNames) {
		
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
		control.initControlWindow();
		
	}
	
	/**
	 * Calculates and displays a variable number of averages.
	 */
	protected static void calcAvgs(ModelGroupIdentifier mgi, String ... paramNames) {
		calcAvgs("Calculate averages", mgi, paramNames);
	}
	
	protected static Average[] avg(ModelGroupIdentifier mgi, String ... paramNames) {
		
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
