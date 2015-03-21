package surfdep.controllers.supplier;

import java.util.HashMap;
import java.util.function.Consumer;

import surfdep.controllers.trials.DepositionControl;

/**
 * AnalysisCallback.java
 * Created:	17 March 2015
 * @author	Tyler Parsons
 *  
 * A callback which conducts analysis of a model after simulation.
 * If proper parameters for t cross are provided, it invokes the
 * {@link DepositionControl#
 */
public class CachedInputSupplier implements AsyncSupplier<HashMap<String, String>> {

	private HashMap<String, String> cachedInput;
	
	public CachedInputSupplier(
		DepositionControl control,
		HashMap<String, Double> params
	) throws IllegalArgumentException {
		
		cachedInput = new HashMap<>();
		for (String key: DepositionControl.T_X_INPUT_KEYS) {
			String value;
			if ((value = params.remove(key).toString()) == null) {
				throw new IllegalArgumentException("Missing t_x input parameter \""+key+"\"");
			}
			cachedInput.put(key, value);
		}
			
	}

	@Override
	public void get(Consumer<HashMap<String, String>> onSupplyCallback) {
		onSupplyCallback.accept(cachedInput);
	}
	
}
