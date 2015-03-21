package surfdep.controllers.trials;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import org.opensourcephysics.controls.SimulationControl;

import surfdep.controllers.supplier.AsyncSupplier;
import surfdep.controllers.supplier.CachedInputSupplier;
import surfdep.controllers.supplier.InputDialogSupplier;

/**
 * TrialRunner.java
 * Created: 15 March 2015
 * @author Tyler
 *
 * Runs multiple trials. Reads in trial parameters
 * from a txt file.
 */
public class TrialRunner {

	/**
	 * Main method.
	 * @param args Not used
	 */
	public static void main(String[] args) {
		
		// Read parameters
		String filePath = DepositionControl.DIR_DATA_ROOT + "trial_params.txt";
		Scanner in = null;
		try {
			in = new Scanner(new File(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		final HashMap<String, Double> numericParams = new HashMap<>();
		HashMap<String, String> textParams = new HashMap<>();
		while (in.hasNext()) {
			String line = in.nextLine();
			System.out.println(line);
			String[] kvPair = line.split("\t");
			try {
				numericParams.put(kvPair[0], Double.parseDouble(kvPair[1]));
			} catch (NumberFormatException nfe) {
				textParams.put(kvPair[0], kvPair[1]);
			}
		}
		in.close();
		
		// Create control
		String modelType = textParams.get("modelType");
		final DepositionControl control = new DepositionControl(modelType);
		
		// Determine number of trials to run
		DepositionControl.remainingTrials = numericParams.remove("numTrials").intValue();
		// Grab other params
		DepositionControl.clearMod = numericParams.remove("clearMod").intValue();
		DepositionControl.plotAllMod = numericParams.remove("plotAllMod").intValue();
		if (numericParams.containsKey("averageFactor"))
			DepositionControl.averageFactor = numericParams.remove("averageFactor").doubleValue();
		
		// Instantiate AsyncSupplier to provide input for model analysis
		AsyncSupplier<HashMap<String, String>> supplier;
		try {
			// Use explicity defined input parameters if possible
			supplier = new CachedInputSupplier(control, numericParams);
		} catch (IllegalArgumentException e) {
			supplier = new InputDialogSupplier();
		}
		control.setAsyncInputSupplier(supplier);
		
		// Create Simulation
		SimulationControl.createApp(control);
		
		// Run trials recursively
		control.initialize(numericParams);
		control.setAnalysisCallback( () -> {
			
			if (--DepositionControl.remainingTrials > 0) {
				
				if ((DepositionControl.modelId+1) % DepositionControl.plotAllMod == 0) {
					control.plotAll();
				}
				if ((DepositionControl.modelId+1) % DepositionControl.clearMod == 0) {
					control.clearMemory();
				}
				
				control.initialize(numericParams);
				control.startSimulation();
			}
			
		});
		control.startSimulation();
		
	}
	
}
