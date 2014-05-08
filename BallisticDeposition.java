package ch13;

public class BallisticDeposition extends Deposition {

	/*
	 * Selects a column at random and deposits
	 * an atom there, letting it fall and bond
	 * with the the first horizontally or ver-
	 * tically adjacent neighbor.
	 * 
	 * */
	public void step() {
		super.step();
		
		int col = (int)(Math.random()*L);
		int h = maxHeight(col - 1, col + 1);
		if (lattice[col][h] == 1 && isValid(col, h+1)) {
			lattice[col][h+1] = 1;
			height[col] = h + 1;
		}
		else {
			lattice[col][h] = 1;
			height[col] = h;
		}
	}

}