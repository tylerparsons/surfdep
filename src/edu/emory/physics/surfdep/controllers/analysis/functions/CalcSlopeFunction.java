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

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.utils.LinearRegression;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;

public class CalcSlopeFunction extends SavingAnalysisFunction {
	
	public static final String TITLE = "calculate slope";
	
	public CalcSlopeFunction(AnalysisControl control) {
		super(
			TITLE,
			"Enter scaled ln t values between which to calculate slope",
			new String[] {"ln t1", "ln t2"},
			control
		);
	}
	
	@Override
	public Consumer<HashMap<String, String>> createAnalyzer() {
		return (HashMap<String, String> input) -> {
			double lnt1 = Double.parseDouble(input.remove("ln t1"));
			double lnt2 = Double.parseDouble(input.remove("ln t2"));
			new SavingAnalysisFunction(title, control) {
				@Override
				public Consumer<HashMap<String, String>> createAnalyzer() {
					return (HashMap<String, String> in) -> {
						calcSlope(new ModelGroupIdentifier(input), lnt1, lnt2);
					};
				}
			}.analyze();
		};
	}

	/**
	 * Runs a least squares regression from t1 to t2 over the lnw vs. lnt
	 * plot for the single model identified by {@code mgi}.
	 * @param mgi identifies a single model
	 * @param lnt1 start boundary of regression
	 * @param lnt2 end boundary of regression
	 */
	protected void calcSlope(ModelGroupIdentifier mgi, double lnt1, double lnt2) {
		
		// Scale t
		double z = 1.5;
		int L = 256;
		lnt1 += Math.log(Math.pow(L, z));
		lnt2 += Math.log(Math.pow(L, z));
		
		int t1 = (int)Math.exp(lnt1);
		int t2 = (int)Math.exp(lnt2);
		ResultSet data = control.getDb().query(
			"SELECT * FROM " + AnalysisControl.DB_TABLE_AVERAGES + " WHERE " +
			mgi.sqlWhereClause() + " AND t BETWEEN " + t1 + " AND " + t2
		);
		
		// Store data in lists, with initial capacity O(log t)
		final ArrayList<Double> lnw = new ArrayList<>();
		final ArrayList<Double> lnt = new ArrayList<>();
		
		// TODO debug this
		try {
			int i = 0;
			while (data.next()) {
				lnw.add(i, Math.log(data.getDouble("w_avg")));
				lnt.add(i++, data.getDouble("t"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		
		// Construct linear regression
		LinearRegression lnw_vs_lnt = new LinearRegression(
			// Independent variable
			(double x) -> {
				return lnt.get((int)x);
			},
			// Dependent variable
			(double x) -> {
				return lnw.get((int)x);						
			},
			// x_i
			0,
			// x_f
			lnw.size()-1,
			// dx
			1
		);
		
		// Save data
		HashMap<String, Double> params = new HashMap<>();
		params.put("m", lnw_vs_lnt.m());
		params.put("R2", lnw_vs_lnt.R2());
		control.saveData("Calculate slope", params);
		
		// Display result
		control.showMessage("m = " + params.get("m") +
							"\nR^2 = " + params.get("R2"));
		
		// Relaunch control window
		control.showControlWindow();
		
	}
	
}
