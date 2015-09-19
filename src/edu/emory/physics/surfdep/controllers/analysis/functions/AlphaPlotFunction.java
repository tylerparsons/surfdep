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

import edu.emory.physics.surfdep.controllers.VisualizationManager;
import edu.emory.physics.surfdep.controllers.VisualizationManager.Point;
import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.utils.LinearRegression;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;

public class AlphaPlotFunction extends AnalysisFunction {
	
	public final static String TITLE = "alpha plot";
	
	public AlphaPlotFunction(AnalysisControl control) {
		super(TITLE, control);
	}
	
	@Override
	public void accept(HashMap<String, String> input) {
		alphaPlot(new ModelGroupIdentifier(input));
	}
	
	/**
	 * Queries L and lnw_avg values for the given
	 * {@link edu.emory.physics.surfdep.largesystems.utils.ModelGroupIdentifier},
	 * create a linear regression and passes it to
	 * the member {@link VisualizationManager} along
	 * with the points for plotting.
	 * 
	 * @param mgi A {@link ModelGroupIdentifier}
	 */
	protected void alphaPlot(ModelGroupIdentifier mgi) {
		
		ResultSet data = control.getDb().query(
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
			control.getVisManager().logPlotWidthVsLength(lnw_avgByL, lnw_vs_lnL);
			
			// Show alpha value
			control.showMessage("alpha = " + lnw_vs_lnL.m() +
						"\nR^2 = " + lnw_vs_lnL.R2());
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		// Relaunch control window
		control.showControlWindow();
		
	}
	
}
