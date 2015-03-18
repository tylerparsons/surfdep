package surfdep.largesystems.controllers.analysis;

/**
 * Used to compute running averages.
 * @author Tyler
 */
public class Average {
	
	public double val;
	public int samples;
	
	public Average(double v, int s) {
		val = v; samples = s;
	}
	
}