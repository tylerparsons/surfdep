package ch13;

import java.util.ArrayList;

public class RandomDeposition extends Deposition {
	
	private ArrayList<Point> growthSites;
	private Point site, neighbor;

	public void init() {
		super.init();
		
		growthSites = new ArrayList<Point>();
		site = new Point();
		neighbor = new Point();

		// Add all surface sites to growthSites
		for (int i = 0; i < L; i++) {
			neighbor = new Point();
			neighbor.x = i;
			neighbor.y = 0;
			growthSites.add(neighbor);
		}

	}

	public void step(){
		super.step();

		// Return to terminate process for an empty list
		if (growthSites.size() == 0)
			return;

		// Select a site at random from possible growthSites, populate it
		int p = (int) (Math.random() * growthSites.size());
		site = growthSites.remove(p);
		lattice[site.x][site.y] = 1;
		// Update height
		if (site.y > height[site.x])
			height[site.x] = site.y;

		// Add unoccupied neighbors to growthSites
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				int x = site.x + dx;
				int y = site.y + dy;
				if (isValid(x, y) && lattice[x][y] == 0) {
					// Add neighbor as Point
					neighbor = new Point();
					neighbor.x = x;
					neighbor.y = y;
					growthSites.add(neighbor);
				}
			}
		}
		
	}

}
