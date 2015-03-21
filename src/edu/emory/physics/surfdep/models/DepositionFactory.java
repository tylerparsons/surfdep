package edu.emory.physics.surfdep.models;

public class DepositionFactory {

	public Deposition createDeposition(String type) {
		if (type.equals("BallisticDeposition")) {
			return new BallisticDeposition();
		}
		// Declare other types here
		// ...
		else {
			return null;
		}
	}

	public Deposition createDeposition(String type, double averageFactor) {
		if (type.equals("BallisticDeposition")) {
			return new BallisticDeposition(averageFactor);
		}
		// Declare other types here
		// ...
		else {
			return null;
		}
	}
	
}
