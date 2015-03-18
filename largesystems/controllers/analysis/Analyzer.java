package surfdep.largesystems.controllers.analysis;

import java.util.HashMap;

/**
 * An interface requiring support for handling
 * analysis input parameters.
 * 
 * @author Tyler
 */
interface Analyzer {
	
	public void analyze(HashMap<String, String> input);
	
}