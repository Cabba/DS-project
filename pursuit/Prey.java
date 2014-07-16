package it.unipr.aotlab.actomos.examples.pursuit;

import it.unipr.aotlab.actomos.discrete.Cell;
import it.unipr.aotlab.actomos.discrete.d2d.Cell2DState;
import it.unipr.aotlab.actomos.discrete.d2d.Cell2DView;
import it.unipr.aotlab.actomos.discrete.d2d.Move2DView;
import it.unipr.aotlab.code.actor.Case;
import it.unipr.aotlab.code.actor.Message;
import it.unipr.aotlab.code.actor.Reference;
import it.unipr.aotlab.code.actor.StateView;
import it.unipr.aotlab.code.actor.cases.Cycler;
import it.unipr.aotlab.code.error.ConfigurationException;
import it.unipr.aotlab.code.error.ConfigurationInfo;
import it.unipr.aotlab.code.error.ErrorManager;
import it.unipr.aotlab.code.filtering.MessagePattern;
import it.unipr.aotlab.code.filtering.constraints.IsInstance;
import it.unipr.aotlab.code.runtime.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.util.Pair;

import javax.vecmath.Point2i;

public class Prey extends Cell {

	private static final long serialVersionUID = 1L;

	// Configurable parameters
	private static final int RADIUS;
	private static final int FOV;
	private static final double CHANGE_DIR_PROB;
	private static final int DIM;

	private PursuitPolicy policy;
	private final Point2i direction;
	private final Point2i nextMove;
	private boolean goalReached;

	// List of all the message for resolve the conflict in a distributed way.
	private final List<Pair<Reference, Point2i>> conflictMessages;
	// Notify if the position is already occupied, during this cycle, by another
	// actor.
	private boolean conflict;

	private final List<Point2i> predators;
	private final List<Point2i> goals;

	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if ((b.getString("prey.radius") == null) || (b.getString("prey.fov") == null) || (b.getString("prey.change_direction_prob") == null) || (b.getString("field_dim") == null)) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			b = null;
			ErrorManager.notifyError(Controller.PROVIDER, new ConfigurationException(ConfigurationInfo.OP, new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.pursuit" }, e));
			System.exit(-1);
		}

		RADIUS = Integer.parseInt(b.getString("prey.radius"));
		FOV = Integer.parseInt(b.getString("prey.fov"));
		CHANGE_DIR_PROB = Double.parseDouble(b.getString("prey.change_direction_prob"));
		DIM = Integer.parseInt(b.getString("field_dim"));
	}

	public Prey() {
		this.direction = new Point2i(-1, 0);
		this.nextMove = new Point2i();
		this.conflictMessages = new ArrayList<>();
		this.conflict = false;
		this.predators = new ArrayList<>();
		this.goals = new ArrayList<>();
		this.goalReached = false;
	}

	@Override
	public int getRadius() {
		return 0;
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize(final Object[] v) {
		if(v[0] instanceof PursuitPolicy ) this.policy = (PursuitPolicy)v[0];
		if(this.policy != null) System.out.println("Policy setted in prey");
		return initialize();
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize() {

		ArrayList<Case> l = new ArrayList<>();

		l.add(new CyclerCase());
		l.add(new ProcessPositionCase());

		// Initialize next move
		Cell2DState s = (Cell2DState) this.getState();

		this.nextMove.x = s.getX() + this.direction.x;
		this.nextMove.y = s.getY() + this.direction.y;
		ToroidalUtils.normailizePosition(this.nextMove, DIM);
		sendInformation();

		return l;
	}

	private final class ProcessPositionCase extends Case {
		private static final long serialVersionUID = 1L;

		private ProcessPositionCase() {
			super(new MessagePattern(MessagePattern.CONTENT, new IsInstance(StateView.class)));
		}

		@Override
		public void process(final Message m) {
			// Check if the next move of another agent equal to this agent
			// and resolve the conflict

			// Check the next move (for movable actors)
			if (m.getContent() instanceof Move2DView) {
				Move2DView v = (Move2DView) m.getContent();

				// CHECK PREDATOR POSITION
				// Check if the predator position is inside the field of view of
				// the prey
				if (v.getInfo() == "Predator") {
					Cell2DState s = (Cell2DState) Prey.this.getState();

					Point2i dist = ToroidalUtils.toroidalDistance(new Point2i(s.getX(), s.getY()), v.getPos(), DIM);
					int absX = Math.abs(dist.x);
					int absY = Math.abs(dist.y);
					// I can see the prey
					if ((absX <= FOV) && (absY <= FOV)) {
						Prey.this.predators.add(v.getPos());
					}
				}

				// CHECK CONFLICTING MESSAGES
				// Check if there are conflicting next move
				if ((Prey.this.nextMove.x == v.getNextPos().x) && (Prey.this.nextMove.y == v.getNextPos().y)) {
					Prey.this.conflictMessages.add(new Pair<>(m.getSender(), v.getNextPos()));
				}
				// Check if my next position is conflicting with the actual
				// position of an actor
				if ((Prey.this.nextMove.x == v.getPos().x) && (Prey.this.nextMove.y == v.getPos().y)) {
					Prey.this.conflict = true;
				}
			}

			// Chek the current position (for goal actors)
			else if (m.getContent() instanceof Cell2DView) {
				if (Prey.this.goalReached == false) {
					Cell2DView v = (Cell2DView) m.getContent();
					Cell2DState s = (Cell2DState) Prey.this.getState();

					if ((v.getX() == s.getX()) && (v.getY() == s.getY())) {
						Prey.this.goalReached = true;
						return;
					}

					Prey.this.goals.add(new Point2i(v.getX(), v.getY()));
				}
			}

		}
	}

	/*
	 * Case managing the scheduler cycle messages.
	 */
	private final class CyclerCase extends Cycler {

		private static final long serialVersionUID = 1L;

		private CyclerCase() {
			super();
		}

		/** {@inheritDoc} **/
		@Override
		public void process(final Message m) {
			if (Prey.this.goalReached == false) {
				solveConflicts();
				move();
				sendInformation();
			}
		}
	}

	// /////////////////
	// / UTILS FUNCTIONS
	// /////////////////

	private void solveConflicts() {

		if (this.conflict) {
			this.conflict = false;
			this.conflictMessages.clear();
			Cell2DState s = (Cell2DState) this.getState();
			this.nextMove.x = s.getX();
			this.nextMove.y = s.getY();
			return;
		}

		// There are no conflicts (only the current agent message)
		if (this.conflictMessages.size() <= 1) {
			this.conflictMessages.clear();
			return;
		}

		//System.out.println("Conflict size: " + this.conflictMessages.size());
		// Initialization
		Pair<Reference, Point2i> pair;
		int max = this.conflictMessages.get(0).getKey().hashCode();
		Pair<Reference, Point2i> res = this.conflictMessages.get(0);

		// Loop for all the conflicts
		for (int i = 1; i < this.conflictMessages.size(); ++i) {
			pair = this.conflictMessages.get(i);
			if (pair.getKey().hashCode() > max) {
				max = pair.getKey().hashCode();
				res = pair;
			}
		}

		// If this actor loose set nextMove to the current position
		if (res.getKey() != this.getReference()) {
			//System.out.println(this.getReference().toString() + " loose the conflict.");
			Cell2DState s = (Cell2DState) this.getState();
			this.nextMove.x = s.getX();
			this.nextMove.y = s.getY();
		}

		this.conflictMessages.clear();
	}

	/**
	 * This function move the actor if there are no conflicts and compute the
	 * next move
	 */
	private void move() {
		// Change the actor position to the next move
		//System.out.println(this.getClass().toString() + " moving in " + this.nextMove.toString());
		Cell2DState s = (Cell2DState) this.getState();
		s.setX(this.nextMove.x);
		s.setY(this.nextMove.y);
		this.setState(s);

		// Compute the next position
		roaming(this.goals, this.predators);

		// Update next move
		this.nextMove.x = s.getX() + this.direction.x;
		this.nextMove.y = s.getY() + this.direction.y;
		ToroidalUtils.normailizePosition(this.nextMove, DIM);
	}

	// Send a request for the next position
	private void sendInformation() {
		send(GBROADCAST, getMoveView());
	}

	private Move2DView getMoveView() {
		Cell2DState state = (Cell2DState) this.getState();
		return new Move2DView(state.getX(), state.getY(), this.nextMove.x, this.nextMove.y, "Prey");
	}

	private void roaming(List<Point2i> goals, List<Point2i> predators) {
		Random r = new Random();

		// If the prey see at least one predator change probability is increased
		double change_prob = predators.size() > 0 ? CHANGE_DIR_PROB : CHANGE_DIR_PROB - (CHANGE_DIR_PROB * 0.5);

		double trial = r.nextDouble();
		if (trial < change_prob) {
			int c = r.nextInt(3);
			// Invert direction
			if (c == 0) {
				this.direction.x = -this.direction.x;
				this.direction.y = -this.direction.y;
			}
			// Turn around of 90 degrees
			else if (c == 1) {
				int tmp = this.direction.x;
				this.direction.x = this.direction.y;
				this.direction.y = tmp;
			}
			// Invert and turn around
			else if (c == 2) {
				int tmp = this.direction.x;
				this.direction.x = -this.direction.y;
				this.direction.y = -tmp;
			}
		}
	}

	public Point2i getPosition(){
		Cell2DState s = (Cell2DState) this.getState();
		return new Point2i(s.getX(), s.getY());
	}
	
	public Point2i getDirection(){
		return this.direction;
	}
	// TODO: remove this behavior
	/*
	 * public void escape(){ System.out.println("size = " +
	 * this.predatorsPos.size()); Cell2DState s = (Cell2DState)this.getState();
	 * 
	 * int deltaX = 0; int deltaY = 0; Point2i dist = new Point2i(); for(int i =
	 * 0; i < this.predatorsPos.size(); ++i){ //deltaX +=
	 * this.predatorsPos.get(i).x - s.getX(); //deltaY +=
	 * this.predatorsPos.get(i).y - s.getY(); dist = toroidalDistance(new
	 * Point2i(s.getX(), s.getY()), this.predatorsPos.get(i));
	 * System.out.println("Toroidal distance = " + dist.toString());
	 * 
	 * deltaX += dist.x; deltaY += dist.y; } // Invert direction deltaX *= -1;
	 * deltaY *= -1;
	 * 
	 * if(Math.abs(deltaX) > Math.abs(deltaY)){ this.direction.x =
	 * (int)Math.signum((double)deltaX); this.direction.y = 0; } else{
	 * this.direction.x = 0; this.direction.y =
	 * (int)Math.signum((double)deltaY); }
	 * 
	 * this.predatorsPos.clear(); }
	 */

}
