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

public class BetaPlotFunction extends AnalysisFunction {
	
	public final static String TITLE = "beta vs x plot";
	
	public BetaPlotFunction(AnalysisControl control) {
		super(TITLE, control);
	}
	
	/**
	 * Queries records identified by mgi, computes the 
	 * average beta values for each distinct x and passes
	 * the averages to 
	 * {@link edu.emory.physics.surfdep.largesystems.controllers.VisualizationManager}.
	 * 
	 * @param mgi A {@link ModelGroupIdentifier}
	 */
	protected void betaVsXPlot(ModelGroupIdentifier mgi) {
		
		// Query data
		ResultSet data = control.selectWhere(AnalysisControl.DB_TABLE_MODELS, mgi);
		
		try {
			
			HashMap<Double, Average> beta_avg = new HashMap<Double, Average>();
			
			while(data.next()) {
				
				double x = data.getDouble("x");
				double beta = data.getDouble("beta");
				
				Average avg = beta_avg.get(x);
				if (avg != null) {
					avg.val = (avg.val * avg.samples + beta) / (avg.samples + 1);
					avg.samples = avg.samples + 1;
					beta_avg.put(x, avg);
				}
				else {
					beta_avg.put(x, new Average(beta, 1));
				}
				
			}
			
			// Pass averages to visManager to plot
			control.getVisManager().plotBetaVsX(beta_avg);
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			control.showMessage("Null result set returned");
			npe.printStackTrace();
		}
		
		// Relaunch control window
		control.showControlWindow();
		
	}

	@Override
	public Consumer<HashMap<String, String>> createAnalyzer() {
		return (HashMap<String, String> input) -> 
			betaVsXPlot(new ModelGroupIdentifier(input));
	}
	
}
