package surfdep.largesystems.controllers.supplier;

import java.util.HashMap;
import java.util.function.Consumer;

import surfdep.largesystems.controllers.trials.DepositionControl;
import surfdep.largesystems.utils.InputDialog;

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
