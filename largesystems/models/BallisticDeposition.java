package bdm.largesystems.models;

/**
 * BallisticDeposition.java
 * Created:	7 May 2014
 * @author	Tyler Parsons
 *  
 * A re-implementation of a well known
 * surface deposition model based on
 * the bonding of atoms. 
 */
public class BallisticDeposition extends LargeSystemDeposition {

	public BallisticDeposition() {
		super();
	}
	
	/**
	 * Selects a column at random and deposits
	 * an atom there, letting it fall and bond
	 * with the the first horizontally or ver-
	 * tically adjacent neighbor.
	 */
	@Override
	protected Point deposit() {
		
		int col = (int)(Math.random()*L);
		int h = localMaxHeight(col - 1, col + 1);
		if (getBit(col, h) == 1 && isValid(col, h+1)) {
			Point p = new Point();
			p.x = col;
			p.y = h+1;
			return p;
		}
		else {
			Point p = new Point();
			p.x = col;
			p.y = h;
			return p;
		}
	}

}