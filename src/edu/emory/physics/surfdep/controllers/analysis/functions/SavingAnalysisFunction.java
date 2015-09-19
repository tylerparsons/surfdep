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

import java.util.HashMap;

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.utils.InputDialog;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;

/**
 * An AnalysisFunction which saves input data using
 * {@link AnalysisControl} helper functions.
 * 
 * @author Tyler
 */
public abstract class SavingAnalysisFunction extends AnalysisFunction {

	public SavingAnalysisFunction(
		String title,
		String inputMessage,
		String[] inputParams,
		AnalysisControl control
	) {
		super(title, inputMessage, inputParams, control);
	}
	
	public SavingAnalysisFunction(String title, AnalysisControl control) {
		super(title, control);
	}

	/**
	 * Saves and prints data before invoking analysis callback.
	 */
	@Override
	public void analyze() {		
		new InputDialog(
			inputMessage,
			inputParams,
			(HashMap<String, String> input) -> {
				ModelGroupIdentifier mgi = new ModelGroupIdentifier(input);
				control.printToTxt("\n"+inputMessage);
				control.saveData(inputMessage, mgi);
				analyzer.accept(input);
			}
		);
	}

}
