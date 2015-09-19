/**
 * 
 */
package edu.emory.physics.surfdep.controllers.analysis.functions;

import java.util.HashMap;
import java.util.function.Consumer;

import edu.emory.physics.surfdep.controllers.analysis.AnalysisControl;
import edu.emory.physics.surfdep.utils.ModelGroupIdentifier;

/**
 * 
 * @author Tyler
 */
public abstract class SavingAnalyzer implements Consumer<HashMap<String, String>> {

	protected String title;
	protected Consumer<ModelGroupIdentifier> analyzer;
	protected AnalysisControl control;
	
	public SavingAnalyzer(
		String title,
		Consumer<ModelGroupIdentifier> analyzer
	) {
		this.title = title;
		this.analyzer = analyzer;
//		this.control = AnalysisControl.getSingleton();
	}

	@Override
	public void accept(HashMap<String, String> input) {
		ModelGroupIdentifier mgi = new ModelGroupIdentifier(input);
		control.printToTxt("\n"+title);
		control.saveData(title, mgi);
		analyzer.accept(mgi);
	}
	
}
