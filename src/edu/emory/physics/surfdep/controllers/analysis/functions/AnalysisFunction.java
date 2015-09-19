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
import edu.emory.physics.surfdep.utils.InputDialog;

import java.util.HashMap;
import java.util.function.Consumer;


/**
 * Abstracts functionality for generating InputDialogs
 * and running callbacks into a single wrapper class
 * for all AnalysisControl functions.
 * 
 * @author Tyler Parsons
 */
public abstract class AnalysisFunction implements Consumer<HashMap<String, String>> {
		
	protected String title;
	protected String inputMessage;
	protected String[] inputParams;
	protected AnalysisControl control;
	
	public AnalysisFunction(
		String title,
		String inputMessage,
		String[] inputParams,
		AnalysisControl control
	) {
		this.title = title;
		this.inputMessage = inputMessage;
		this.inputParams = inputParams;
		this.control = control;
	}
	
	public AnalysisFunction(String title, AnalysisControl control) {
		this(
			title,
			AnalysisControl.DEFAULT_INPUT_MSG,
			AnalysisControl.COMPLETE_MODEL_PARAMS,
			control
		);
	}
	
	/**
	 * Create input dialog to enable user
	 * specification of trials over which
	 * to run average.
	 */
	public void analyze() {

		new InputDialog(
			inputMessage,
			inputParams,
			this
		);
		
	}
	
	public String getTitle() {
		return title;
	}
		
//	protected String inputMessage;
//	protected String[] inputParams;
//	protected Consumer<HashMap<String, String>> analyzer;
//	
//	public AnalysisFunction(
//		String inputMessage,
//		String[] inputParams,
//		Consumer<HashMap<String, String>> analyzer
//	) {
//		this.inputMessage = inputMessage;
//		this.inputParams = inputParams;
//		this.analyzer = analyzer;
//	}
//	
//	public AnalysisFunction(Consumer<HashMap<String, String>> analyzer) {
//		this(
//			AnalysisControl.DEFAULT_INPUT_MSG,
//			AnalysisControl.COMPLETE_MODEL_PARAMS,
//			analyzer
//		);
//	}
//	
//	public abstract Consumer<HashMap<String, String>> createAnalyzer();
//	
//	public void analyze() {
//		
//		// Create input dialog to enable user
//		// specification of trials over which
//		// to run average
//		
//		new InputDialog(
//			inputMessage,
//			inputParams,
//			(HashMap<String, String> input) -> analyzer.accept(input)
//		);
//		
//	}
	
}
