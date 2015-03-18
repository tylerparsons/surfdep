package surfdep.largesystems.controllers.analysis;

import java.util.HashMap;

import surfdep.largesystems.utils.InputDialog;

/**
 * Abstracts functionality for generating InputDialogs
 * and running callbacks into a single wrapper class
 * for all AnalysisControl functions.
 * 
 * @author Tyler
 */
class AnalysisFunction {
	
	String inputMessage;
	String[] inputParams;
	Analyzer analyzer;
	
	public AnalysisFunction(String inputMessage, String[] inputParams, Analyzer analyzer) {
		this.inputMessage = inputMessage;
		this.inputParams = inputParams;
		this.analyzer = analyzer;
	}
	
	public void analyze() {
		
		// Create input dialog to enable user
		// specification of trials over which
		// to run average
		
		new InputDialog(
			inputMessage,
			inputParams,
			(HashMap<String, String> input) -> analyzer.analyze(input)
		);
		
	}
	
	public static AnalysisFunction defaultInputAf(Analyzer analyzer) {
		return new AnalysisFunction(
				AnalysisControl.DEFAULT_INPUT_MSG,
				AnalysisControl.COMPLETE_MODEL_PARAMS,
				analyzer
		);
	}
	
}