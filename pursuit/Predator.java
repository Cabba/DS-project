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
import java.util.ResourceBundle;

import javafx.util.Pair;

import javax.vecmath.Point2i;

public class Predator extends Cell {

	private static final long serialVersionUID = 1L;

	// Configurable parameters
	private static final int RADIUS;
	private static final int FOV;
	private static final double CHANGE_DIR_PROB;
	private static final int DIM;

	private PursuitPolicy policy;
	private final Point2i direction;
	private final Point2i nextMove;

	// Conflict resolution
	private final List<Pair<Reference, Point2i>> conflictMessages;
	private boolean conflictDetected;

	private final List<Pair<Reference, Point2i>> predators;
	private Point2i preyPos;
	private boolean canSeePrey;

	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if ((b.getString("prey.radius") == null) || (b.getString("predator.fov") == null) || (b.getString("predator.change_direction_prob") == null) || (b.getString("field_dim") == null)) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			b = null;
			ErrorManager.notifyError(Controller.PROVIDER, new ConfigurationException(ConfigurationInfo.OP, new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.pursuit" }, e));
			System.exit(-1);
		}

		RADIUS = Integer.parseInt(b.getString("prey.radius"));
		FOV = Integer.parseInt(b.getString("predator.fov"));
		CHANGE_DIR_PROB = Double.parseDouble(b.getString("predator.change_direction_prob"));
		DIM = Integer.parseInt(b.getString("field_dim"));
	}

	public Predator() {
		this.direction = new Point2i(-1, 0);
		this.nextMove = new Point2i();

		this.conflictMessages = new ArrayList<>();
		this.conflictDetected = false;

		// Movement logic
		this.preyPos = new Point2i();
		this.predators = new ArrayList<>();
		this.canSeePrey = false;
	}

	@Override
	public int getRadius() {
		return RADIUS;
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize(final Object[] v) {
		if (v[0] instanceof PursuitPolicy)
			this.policy = (PursuitPolicy) v[0];
		if (this.policy != null)
			System.out.println("Policy setted in predator");
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

	/*
	 * Wait until a LEAVE_OK message arrive, and when this arrive set the agent
	 * behavior as changeable.
	 */
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

				// CHECK PREY POSITION
				if (v.getInfo() == "Prey") {
					Point2i dist = ToroidalUtils.toroidalDistance(getPosition(), v.getPos(), DIM);
					int absX = Math.abs(dist.x);
					int absY = Math.abs(dist.y);
					// I can see the prey
					if ((absX <= FOV) && (absY <= FOV)) {
						// System.out.println(Predator.this.getReference().toString()
						// + "Can see the prey!!");
						Predator.this.preyPos = v.getPos();
						Predator.this.canSeePrey = true;
					}
				}

				if (v.getInfo() == "Predator") {
					Point2i dist = ToroidalUtils.toroidalDistance(getPosition(), v.getPos(), DIM);
					int absX = Math.abs(dist.x);
					int absY = Math.abs(dist.y);
					// I can see the prey
					if ((absX <= FOV) && (absY <= FOV)) {
						Predator.this.predators.add(new Pair<>(m.getSender(), v.getPos()));
					}
				}

				// EVALUATE CONFLICTS
				// Check if there are conflicting next move
				if ((Predator.this.nextMove.x == v.getNextPos().x) && (Predator.this.nextMove.y == v.getNextPos().y)) {
					Predator.this.conflictMessages.add(new Pair<>(m.getSender(), v.getNextPos()));
				}
				// Check if my next position is conflicting with the actual
				// position of an actor
				if ((Predator.this.nextMove.x == v.getPos().x) && (Predator.this.nextMove.y == v.getPos().y)) {
					Predator.this.conflictDetected = true;
				}
			}
			// Chek the current position (for fixed actors)
			else if (m.getContent() instanceof Cell2DView) {
				Cell2DView v = (Cell2DView) m.getContent();
				Cell2DState s = (Cell2DState) Predator.this.getState();

				/*
				 * if ((Predator.this.nextMove.x == v.getX()) &&
				 * (Predator.this.nextMove.y == v.getY())) {
				 * Predator.this.conflictDetected = true; }
				 */
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
			solveConflicts();
			move();
			sendInformation();
		}
	}

	// ////////////////
	// UTILS FUNCTIONS
	// ////////////////

	private void solveConflicts() {

		if (this.conflictDetected == true) {
			this.conflictDetected = false;
			this.conflictMessages.clear();

			Cell2DState s = (Cell2DState) this.getState();
			this.nextMove.x = s.getX();
			this.nextMove.y = s.getY();
			return;
		}

		if (this.conflictMessages.size() < 2) {
			this.conflictMessages.clear();
			return;
		}

		Pair<Reference, Point2i> res = this.policy.solveConflict(this.conflictMessages);

		// If this actor loose set nextMove to the current position
		if (res.getKey() != this.getReference()) {
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
		Cell2DState s = (Cell2DState) this.getState();
		s.setX(this.nextMove.x);
		s.setY(this.nextMove.y);
		this.setState(s);

		// Compute the next position
		Point2i source = new Point2i(s.getX(), s.getY());
		Point2i dir;
		if (this.canSeePrey == false) {
			dir = this.policy.movePredator(this.predators, null, this);
		} else {
			dir = this.policy.movePredator(this.predators, this.preyPos, this);
		}
		this.direction.set(dir);

		// Update next move
		this.nextMove.x = s.getX() + this.direction.x;
		this.nextMove.y = s.getY() + this.direction.y;
		ToroidalUtils.normailizePosition(this.nextMove, DIM);

		// Reset
		this.canSeePrey = false;
	}

	// Send a request for the next position
	private void sendInformation() {
		send(GBROADCAST, getMoveView());
	}

	private Move2DView getMoveView() {
		Cell2DState state = (Cell2DState) this.getState();
		return new Move2DView(state.getX(), state.getY(), this.nextMove.x, this.nextMove.y, "Predator");
	}

	public Point2i getDirection() {
		return this.direction;
	}

	public Point2i getPosition() {
		Cell2DState s = (Cell2DState) this.getState();
		return new Point2i(s.getX(), s.getY());
	}

}
