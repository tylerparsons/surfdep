/*
######################################
BallisticDeposition.java
@author		Tyler Parsons
@created	7 May 2014
 
A re-implementation of a well known
surface deposition model based on
the bonding of atoms. 
######################################
*/
package bdm.largesystems;

public class BallisticDeposition extends LargeSystemDeposition {

	/*
	 * Selects a column at random and deposits
	 * an atom there, letting it fall and bond
	 * with the the first horizontally or ver-
	 * tically adjacent neighbor.
	 * */
	public BallisticDeposition() {
		super();
	}
	
	protected Point deposite() {
		
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