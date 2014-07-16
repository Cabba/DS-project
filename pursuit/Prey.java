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

public class Prey extends Cell {

	private static final long serialVersionUID = 1L;

	// Configurable parameters
	private static final int FOV;
	private static final int DIM;

	private PursuitPolicy policy;
	private final Point2i direction;
	private final Point2i nextPos;
	private boolean goalReached;

	// List of all the message for resolve the conflict in a distributed way.
	private final List<Pair<Reference, Point2i>> conflictingMessages;
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

		FOV = Integer.parseInt(b.getString("prey.fov"));
		DIM = Integer.parseInt(b.getString("field_dim"));
	}

	public Prey() {
		this.direction = new Point2i(-1, 0);
		this.nextPos = new Point2i();
		
		this.conflictingMessages = new ArrayList<>();
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
		return initialize();
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize() {

		ArrayList<Case> l = new ArrayList<>();

		l.add(new CyclerCase());
		l.add(new ProcessPerceptionsCase());

		// Initialize next move
		this.nextPos.x = getPosition().x + this.direction.x;
		this.nextPos.y = getPosition().y + this.direction.y;
		ToroidalUtils.normailizePosition(this.nextPos, DIM);
		sendInformation();

		return l;
	}

	private final class ProcessPerceptionsCase extends Case {
		private static final long serialVersionUID = 1L;

		private ProcessPerceptionsCase() {
			super(new MessagePattern(MessagePattern.CONTENT, new IsInstance(StateView.class)));
		}

		@Override
		public void process(final Message m) {
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
				if ((Prey.this.nextPos.x == v.getNextPos().x) && (Prey.this.nextPos.y == v.getNextPos().y)) {
					Prey.this.conflictingMessages.add(new Pair<>(m.getSender(), v.getNextPos()));
				}
				// Check if my next position is conflicting with the actual
				// position of an actor
				if ((Prey.this.nextPos.x == v.getPos().x) && (Prey.this.nextPos.y == v.getPos().y)) {
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
			this.conflictingMessages.clear();
			Cell2DState s = (Cell2DState) this.getState();
			this.nextPos.x = s.getX();
			this.nextPos.y = s.getY();
			return;
		}

		// There are no conflicts (only the current agent message)
		if (this.conflictingMessages.size() <= 1) {
			this.conflictingMessages.clear();
			return;
		}

		Pair<Reference, Point2i> res = this.policy.solveConflict(this.conflictingMessages);

		// If this actor loose set nextMove to the current position
		if (res.getKey() != this.getReference()) {
			Cell2DState s = (Cell2DState) this.getState();
			this.nextPos.x = s.getX();
			this.nextPos.y = s.getY();
		}

		this.conflictingMessages.clear();
	}

	/**
	 * This function move the actor if there are no conflicts and compute the
	 * next move
	 */
	private void move() {
		// Change the actor position to the next move
		Cell2DState s = (Cell2DState) this.getState();
		s.setX(this.nextPos.x);
		s.setY(this.nextPos.y);
		this.setState(s);

		// Compute the next position
		roaming();

		// Update next move
		this.nextPos.x = s.getX() + this.direction.x;
		this.nextPos.y = s.getY() + this.direction.y;
		ToroidalUtils.normailizePosition(this.nextPos, DIM);
	}

	// Send a request for the next position
	private void sendInformation() {
		send(GBROADCAST, getMoveView());
	}

	private Move2DView getMoveView() {
		return new Move2DView(getPosition(), this.nextPos.x, this.nextPos.y, "Prey");
	}

	private void roaming() {
		Point2i dir = this.policy.movePrey(this.goals, this.predators, this);
		this.direction.set(dir);
	}

	public Point2i getPosition(){
		Cell2DState s = (Cell2DState) this.getState();
		return new Point2i(s.getX(), s.getY());
	}
	
	public Point2i getDirection(){
		return this.direction;
	}

}
