package it.unipr.aotlab.actomos.examples.pursuit;

import it.unipr.aotlab.actomos.discrete.d2d.Cell2DState;
import it.unipr.aotlab.code.actor.Reference;
import it.unipr.aotlab.code.error.ConfigurationException;
import it.unipr.aotlab.code.error.ConfigurationInfo;
import it.unipr.aotlab.code.error.ErrorManager;
import it.unipr.aotlab.code.runtime.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.util.Pair;

import javax.vecmath.Point2i;

public class PursuitPolicyImpl implements PursuitPolicy {

	private static final int DIM;
	private static final double PREDATOR_CHANGE_DIR_PROB;
	private static final double PREY_CHANGE_DIR_PROB;

	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if ((b.getString("predator.change_direction_prob") == null) || (b.getString("prey.change_direction_prob") == null) || (b.getString("field_dim") == null)) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			b = null;
			ErrorManager.notifyError(Controller.PROVIDER, new ConfigurationException(ConfigurationInfo.OP, new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.pursuit" }, e));
			System.exit(-1);
		}

		PREY_CHANGE_DIR_PROB = Double.parseDouble(b.getString("prey.change_direction_prob"));
		PREDATOR_CHANGE_DIR_PROB = Double.parseDouble(b.getString("predator.change_direction_prob"));
		DIM = Integer.parseInt(b.getString("field_dim"));
	}

	PursuitPolicyImpl() {
	}

	@Override
	public Point2i movePrey(List<Point2i> goals, List<Point2i> predators, Prey instance) {
		return null;
	}

	@Override
	public Point2i movePredator(List<Pair<Reference, Point2i>> predators, Point2i prey, Predator instance) {
		Point2i result = new Point2i();

		// If there is no prey
		if (prey == null) {
			Random r = new Random();

			double trial = r.nextDouble();
			if (trial < PREDATOR_CHANGE_DIR_PROB) {
				int c = r.nextInt(3);
				// Invert direction
				if (c == 0) {
					result.x = -instance.getDirection().x;
					result.y = -instance.getDirection().y;
				}
				// Turn around of 90 degrees
				else if (c == 1) {
					int tmp = instance.getDirection().x;
					result.x = instance.getDirection().y;
					result.y = tmp;
				}
				// Invert and turn around
				else if (c == 2) {
					int tmp = instance.getDirection().x;
					result.x = -instance.getDirection().y;
					result.y = -tmp;
				}
			}
			return result;
		}

		// If the prey is tracked
		Point2i shift = new Point2i();
		// Assign the tracked edge
		int trackedEdge = 0;
		List<Integer> ref = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			ref.add(predators.get(i).getKey().hashCode());
		}
		Collections.sort(ref);

		for (int i = 0; i < 4; ++i) {
			if (instance.getReference().hashCode() == ref.get(i)) {
				trackedEdge = i;
				break;
			}
		}

		// Assign the target
		if (trackedEdge == 0) {
			shift.x = 1;
			shift.y = 0;
		} else if (trackedEdge == 1) {
			shift.x = 0;
			shift.y = 1;
		} else if (trackedEdge == 2) {
			shift.x = -1;
			shift.y = 0;
		} else if (trackedEdge == 3) {
			shift.x = 0;
			shift.y = -1;
		}

		Cell2DState s = (Cell2DState) instance.getState();
		Point2i pos = new Point2i(s.getX(), s.getY());

		int deltaX = ToroidalUtils.toroidalDistance(pos, prey, DIM).x + shift.x;
		int deltaY = ToroidalUtils.toroidalDistance(pos, prey, DIM).y + shift.y;

		if (Math.abs(deltaX) >= Math.abs(deltaY)) {
			result.x = (int) Math.signum((double) deltaX);
			result.y = 0;
		} else {
			result.x = 0;
			result.y = (int) Math.signum((double) deltaY);
		}

		return result;

	}

	@Override
	public Pair<Reference, Point2i> solveConflict(List<Pair<Reference, Point2i>> conflictingMessages) {
		// Initialization
		Pair<Reference, Point2i> pair;
		int max = conflictingMessages.get(0).getKey().hashCode();
		Pair<Reference, Point2i> res = conflictingMessages.get(0);

		// Loop for al the conflicts
		for (int i = 1; i < conflictingMessages.size(); ++i) {
			pair = conflictingMessages.get(i);
			if (pair.getKey().hashCode() > max) {
				max = pair.getKey().hashCode();
				res = pair;
			}
		}

		return res;
	}

}
