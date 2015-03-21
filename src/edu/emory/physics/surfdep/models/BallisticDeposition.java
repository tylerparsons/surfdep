/**
 * Copyright 2015, Tyler Parsons
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.physics.surfdep.models;

/**
 * BallisticDeposition.java
 * Created:	7 May 2014
 * @author	Tyler Parsons
 *  
 * A re-implementation of a well known
 * surface deposition model based on
 * the bonding of atoms. 
 */
public class BallisticDeposition extends Deposition {

	public BallisticDeposition() {
		super();
	}
	
	public BallisticDeposition(double averagefactor) {
		super(averagefactor);
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