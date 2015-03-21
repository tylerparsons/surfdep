package surfdep.controllers.supplier;

import java.util.function.Consumer;

public interface AsyncSupplier<T> {

	public void get(Consumer<T> onSupplyCallback);
	
}
