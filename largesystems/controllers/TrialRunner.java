package bdm.largesystems.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import org.opensourcephysics.controls.SimulationControl;

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
		final HashMap<String, Double> params = new HashMap<String, Double>();
		
		while (in.hasNext()) {
			String line = in.nextLine();
			System.out.println(line);
			String[] kvPair = line.split(":\t");
			params.put(kvPair[0], Double.parseDouble(kvPair[1]));
		}
		in.close();
		
		// Determine number of trials to run
		DepositionControl.remainingTrials = params.remove("numTrials").intValue();
		// Grab other params
		DepositionControl.clearMod = params.remove("clearMod").intValue();
		DepositionControl.plotAllMod = params.remove("plotAllMod").intValue();
		if (params.containsKey("averageFactor"))
			DepositionControl.averageFactor = params.remove("averageFactor").doubleValue();
		
		// Create Simulation
		final DepositionControl control = new DepositionControl();
		SimulationControl.createApp(control);
		
		// Run trials recursively
		control.initialize(params);
		control.setAnalysisCallback( () -> {
			
			if (--DepositionControl.remainingTrials > 0) {
				
				if ((DepositionControl.modelId+1) % DepositionControl.plotAllMod == 0) {
					control.plotAll();
				}
				if ((DepositionControl.modelId+1) % DepositionControl.clearMod == 0) {
					control.clearMemory();
				}
				
				control.initialize(params);
				control.startSimulation();
			}
			
		});
		control.startSimulation();
		
	}
	
}
