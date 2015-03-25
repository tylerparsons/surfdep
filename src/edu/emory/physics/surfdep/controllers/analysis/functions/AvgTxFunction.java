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

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.controllers.analysis.functions.CalcAvgFunction;
import edu.emory.physics.surfdep.controllers.trials.DepositionControl;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;

public class AvgTxFunction extends CalcAvgFunction {

	protected static AnalysisControl control;
	
	public AvgTxFunction() {
		super(
			(control = AnalysisControl.getSingleton()).createSavingAnalyzer(
				"avg t_x values", (ModelGroupIdentifier mgi) -> {
					CalcAvgFunction.calcAvgs(mgi, DepositionControl.T_X_INPUT_KEYS);
				}
			)
		);
	}
	
}
