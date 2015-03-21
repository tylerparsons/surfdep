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
package edu.emory.physics.surfdep.controllers.supplier;

import edu.emory.physics.surfdep.controllers.trials.DepositionControl;

import java.util.HashMap;
import java.util.function.Consumer;

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
			Double value;
			if ((value = params.remove(key)) == null) {
				throw new IllegalArgumentException("Missing t_x input parameter \""+key+"\"");
			}
			cachedInput.put(key, "" + Math.log(value.doubleValue()));
		}
			
	}

	@Override
	public void get(Consumer<HashMap<String, String>> onSupplyCallback) {
		onSupplyCallback.accept(cachedInput);
	}
	
}
