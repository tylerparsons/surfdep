package surfdep.largesystems.controllers.analysis;

import java.util.HashMap;
import java.util.function.Consumer;

import surfdep.largesystems.utils.InputDialog;

/**
 * Abstracts functionality for generating InputDialogs
 * and running callbacks into a single wrapper class
 * for all AnalysisControl functions.
 * 
 * @author Tyler
 */
public class AnalysisFunction {
	
	protected String inputMessage;
	protected String[] inputParams;
	protected Consumer<HashMap<String, String>> analyzer;
	
	public AnalysisFunction(
		String inputMessage,
		String[] inputParams,
		Consumer<HashMap<String, String>> analyzer
	) {
		this.inputMessage = inputMessage;
		this.inputParams = inputParams;
		this.analyzer = analyzer;
	}
	
	public AnalysisFunction(Consumer<HashMap<String, String>> analyzer) {
		this(
			AnalysisControl.DEFAULT_INPUT_MSG,
			AnalysisControl.COMPLETE_MODEL_PARAMS,
			analyzer
		);
	}
	
	public void analyze() {
		
		// Create input dialog to enable user
		// specification of trials over which
		// to run average
		
		new InputDialog(
			inputMessage,
			inputParams,
			(HashMap<String, String> input) -> analyzer.accept(input)
		);
		
	}
	
}
