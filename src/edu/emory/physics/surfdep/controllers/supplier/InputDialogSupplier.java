package edu.emory.physics.surfdep.controllers.supplier;

import edu.emory.physics.surfdep.controllers.trials.DepositionControl;
import edu.emory.physics.surfdep.utils.InputDialog;

import java.util.HashMap;
import java.util.function.Consumer;

public class InputDialogSupplier implements AsyncSupplier<HashMap<String, String>> {

	@Override
	public void get(Consumer<HashMap<String, String>> onSupplyCallback) {
		new InputDialog(
			"Input t_x values",
			DepositionControl.T_X_INPUT_KEYS,
			onSupplyCallback
		);
	}

}
