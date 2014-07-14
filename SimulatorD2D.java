package it.unipr.aotlab.actomos.discrete.d2d;

import it.unipr.aotlab.actomos.discrete.DiscreteSimulator;
import it.unipr.aotlab.code.actor.Behavior;
import it.unipr.aotlab.code.actor.Case;
import it.unipr.aotlab.code.actor.Reference;
import it.unipr.aotlab.code.error.ErrorManager;
import it.unipr.aotlab.code.error.ExecutionException;
import it.unipr.aotlab.code.error.ExecutionInfo;
import it.unipr.aotlab.code.runtime.Controller;
import it.unipr.aotlab.code.runtime.registry.Registry;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.util.Pair;

import javax.vecmath.Point2i;

/**
 * 
 * The {@code SimulatorD2D} class defines a scheduler for 2D ABMS discrete space
 * simulation.
 * 
 **/

public final class SimulatorD2D extends DiscreteSimulator {
	private static final long serialVersionUID = 1L;

	// Grid x-side.
	private int sideX;
	// Grid Y-side.
	private int sideY;
	// Grid cells references.
	private Reference[][] references;
	
	private List<Pair<Point2i, Reference>> ref;
	// Behavior- probability map.
	private List<SimpleEntry<String, Double>> behaviors;
	// Map that contains grid points and behaviors for that points
	private Map<Point2i, String> fixedBehaviors;

	/**
	 * {@inheritDoc}
	 * 
	 * @param v
	 *            the arguments:
	 * 
	 *            the simulation length, the size of the x-side of the grid, the
	 *            size of the y-side of the grid and the behaviors description.
	 * 
	 *            Each behavior is described through:
	 * 
	 *            the qualified class name, and the probabilities that it can be
	 *            the first behavior of a cell.
	 * 
	 **/
	@Override
	@SuppressWarnings("unchecked")
	public List<Case> initialize(final Object[] v) {
		int i = 0;

		setLength((long) v[i++]);

		this.sideX = (int) v[i++];
		this.sideY = (int) v[i++];

		String bh = null;

		// Check for actors with a specified position
		makeFixedBehaviors((Object[]) v[i++]);

		// Check for actors with random position
		makeRandomBehaviors((Object[]) v[i++]);

		this.references = new Reference[this.sideX][this.sideY];
		
		this.ref = new ArrayList<Pair<Point2i, Reference>>();
				
		int mr = (int) v[i];
		try {
			for (int x = 0; x < this.sideX; x++) {
				for (int y = 0; y < this.sideY; y++) {

					//System.out.println("Trying to get: " + x + ", " + y);
					bh = behavior(x, y);
					
					if(bh != null){
						this.ref.add( 
								new Pair<Point2i, Reference>(new Point2i(x, y), actor((Class<? extends Behavior>) Class.forName(bh))) 
								);
					}
				}
			}
		} catch (Exception e) {
			ErrorManager.notifyError(getReference(), new ExecutionException(ExecutionInfo.AC, new Object[] { bh }, e));
		}

		Registry registry = Controller.INSTANCE.getRegistry();
		
		for(int count = 0; count < this.ref.size(); ++count){
			Point2i p = this.ref.get(count).getKey();
			registry.get(this.ref.get(count).getValue()).setState(new Cell2DState(p.x, p.y));
		}
		

		return null;
	}

	// Builds the behavior- probability map.
	private void makeRandomBehaviors(final Object[] bs) {
		this.behaviors = new ArrayList<>();
		if( bs == null ) return;
		
		Double v = 0.0;

		for (Object o : bs) {
			Object[] b = (Object[]) o;

			v += (double) b[1];

			this.behaviors.add(new SimpleEntry<>((String) b[0], v));
		}
	}

	private void makeFixedBehaviors(final Object[] bs) {
		this.fixedBehaviors = new HashMap<>();

		if (bs == null)
			return;

		for (Object o : bs) {
			Object[] b = (Object[]) o;

			String className = (String) b[0];
			Integer[] positions = (Integer[]) b[1];

			Integer x, y;
			// Iterate for all the given positions
			for (int i = 0; i < positions.length; i += 2) {
				x = positions[i];
				y = positions[i + 1];
				System.out.println(className + " in position " + x + ", " + y);
				this.fixedBehaviors.put(new Point2i(x, y), className);
			}
		}
	}

	// Chooses the behavior of a cell.
	private String behavior(int x, int y) {
		Point2i p = new Point2i(x, y);
		if (this.fixedBehaviors.containsKey(p)) {
			return this.fixedBehaviors.get(p);
		} else {
			if(this.behaviors.size() == 0) return null;
			final Random random = new Random();

			double v = random.nextDouble();

			for (SimpleEntry<String, Double> s : this.behaviors) {
				if (v < s.getValue()) {
					return s.getKey();
				}
			}
		}
		return null;
	}

	// Finds the references of the cells to which send/receive state
	// information.
	private Map<Point2i, Reference> findMap(final int x, final int y, final int m) {
		Map<Point2i, Reference> ns = new HashMap<>();

		//System.out.println("Adding neighbors for actor in position (" + x + ", " + y);
		/*
		for (int i = 1; i <= m; i++) {
			ns.put(i, findSet(x, y, i));
		}
		*/
		int shiftX, shiftY;
		for(int dx = -m; dx <= m; ++dx){
			for(int dy = -m; dy <= m; ++dy){
				
				if( (dx == 0) && (dy == 0)) continue;
				
				// Check bounds for top and the bottom
				if( (x + dx) < 0)  shiftX = x + dx + this.sideX;
				else if( (x + dx) >= 10 ) shiftX = (x + dx) - this.sideX;
				else shiftX = x + dx;
				
				// Check the bounds  for right and left
				if( (y + dy) < 0 ) shiftY = y + dy + this.sideY;
				else if( (y + dy) >= this.sideY ) shiftY = (y + dy) - this.sideY;
				else shiftY = y + dy;
				
				//System.out.println("shiftX = " + shiftX + " shiftY = " + shiftY);
				
				// Setting the relative position
				ns.put(new Point2i(dx, dy), this.references[shiftX][shiftY]);
			}
		}
		
		return ns;
	}
	/*
	 * // Finds the references of the cells at a specific distance. private
	 * Set<Reference> findSet(final int x, final int y, final int r) {
	 * Set<Reference> ss = new HashSet<>();
	 * 
	 * int posX = x; int posY = y;
	 * 
	 * for (int a = 0; a < this.sideX; a++) { int dX, dY;
	 * 
	 * for (int o = 0; o < this.sideY; o++) { dX = Math.abs(posX - a); dY =
	 * Math.abs(posY - o);
	 * 
	 * if (isInSet(dX, dY, r)) { ss.add(this.references[a][o]); } } }
	 * 
	 * return ss; }
	 * 
	 * // Checks if a cell is at a specific distance. private boolean
	 * isInSet(final int dX, final int dY, final int r) { if (r == 0) { return
	 * ((dX == 0) && (dY == 0)); } else if (((dX <= r) || (dX >= (this.sideX -
	 * r))) && ((dY <= r) || (dY >= (this.sideY - r)))) { int l = r - 1;
	 * 
	 * return !(((dX <= l) || (dX >= (this.sideX - l))) && ((dY <= l) || (dY >=
	 * (this.sideY - l)))); }
	 * 
	 * return false; }
	 */
}
