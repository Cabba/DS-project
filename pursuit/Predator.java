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

public class Predator extends Cell {

	private static final long serialVersionUID = 1L;

	// Configurable parameters
	private static final int RADIUS;
	private static final int FOV;
	private static final double CHANGE_DIR_PROB;
	private static final int DIM;
	
	private static int EDGE_TRACKER_CONST = 0;
	
	private final Point2i direction;
	private final Point2i nextMove;
	private final List<Pair<Reference,Point2i>> conflictMessages;
	private boolean conflictDetected;
	
	private Point2i preyPos;
	private boolean canSeePrey;
	private final int trackedEdge;
	
	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if (
					(b.getString("prey.radius") == null) || 
					(b.getString("predator.fov") == null) ||
					(b.getString("predator.change_direction_prob") == null) ||
					(b.getString("field_dim") == null)
					) {
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
		this.direction = new Point2i(-1,0);
		this.nextMove = new Point2i();
		
		// Comunication logic
		this.conflictMessages = new ArrayList<>();
		this.conflictDetected = false;
		
		// Movement logic
		this.preyPos = new Point2i();
		this.canSeePrey = false;
		
		this.trackedEdge = this.EDGE_TRACKER_CONST++;
	}

	@Override
	public int getRadius() {
		return RADIUS;
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize(final Object[] v) {
		return initialize();
	}

	/** {@inheritDoc} **/
	@Override
	public List<Case> initialize() {

		ArrayList<Case> l = new ArrayList<>();

		l.add(new CyclerCase());
		l.add(new ProcessPositionCase());

		// Initialize next move
		
		Cell2DState s = (Cell2DState)this.getState();
		
		this.nextMove.x = s.getX() + this.direction.x;
		this.nextMove.y = s.getY() + this.direction.y;
		normailizePosition(this.nextMove);
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
			if(m.getContent() instanceof Move2DView){
				Move2DView v = (Move2DView)m.getContent();
				
				// CHECK PREY POSITION
				if( v.getInfo() == "Prey" ){
					Cell2DState s = (Cell2DState)Predator.this.getState();
					int absX = Math.abs( v.getPos().x - s.getX() );
					int absY = Math.abs( v.getPos().y - s.getY() );
					// I can see the prey
					if( (absX <= FOV) && (absY <= FOV) ){
						System.out.println(Predator.this.getReference().toString() + "Can see the prey!!");
						Predator.this.preyPos = v.getPos();
						Predator.this.canSeePrey = true;
					}
					else{
						System.out.println(Predator.this.getReference().toString() + "Cant see the prey!!");
						Predator.this.canSeePrey = false;
					}
				}
				
				// EVALUATE CONFLICTS
				// Check if there are conflicting next move
				if( (Predator.this.nextMove.x == v.getNextPos().x) && (Predator.this.nextMove.y == v.getNextPos().y) ){
					Predator.this.conflictMessages.add(new Pair<>(m.getSender(), v.getNextPos()));
				}
				// Check if my next position is conflicting with the actual position of an actor
				if( (Predator.this.nextMove.x == v.getPos().x) && (Predator.this.nextMove.y == v.getPos().y) ){
					Predator.this.conflictDetected = true;
				}
			}
			// Chek the current position (for fixed actors)
			else if(m.getContent() instanceof Cell2DView){
				
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

	//////////////////
	// UTILS FUNCTIONS
	//////////////////
	
	private void solveConflicts(){
		
		if(this.conflictDetected == true){
			this.conflictDetected = false;
			this.conflictMessages.clear();
			
			Cell2DState s = (Cell2DState)this.getState();
			this.nextMove.x = s.getX();
			this.nextMove.y = s.getY();
			return;
		}
		
		if(this.conflictMessages.size() < 2){
			this.conflictMessages.clear();
			return;
		}
		
		System.out.println("Conflict size: " + this.conflictMessages.size());
		// Initialization
		Pair<Reference, Point2i> pair;
		int max = this.conflictMessages.get(0).getKey().hashCode();
		Pair<Reference, Point2i> res = this.conflictMessages.get(0);
		
		// Loop for al the conflicts
		for(int i = 1; i < this.conflictMessages.size(); ++i){
			pair = this.conflictMessages.get(i);
			if(pair.getKey().hashCode() > max){
				max = pair.getKey().hashCode();
				res = pair;
			}
		}
		
		// If this actor loose set nextMove to the current position
		if(res.getKey() != this.getReference()){
			System.out.println(this.getReference().toString() + " loose the conflict.");
			Cell2DState s = (Cell2DState)this.getState();
			this.nextMove.x = s.getX();
			this.nextMove.y = s.getY();
		}
		
		this.conflictMessages.clear();
	}
	
	/**
	 * This function move the actor if there are no conflicts and compute the next move
	 */
	private void move() {
		// Change the actor position to the next move
		System.out.println(this.getClass().toString() + " moving in " + this.nextMove.toString());
		Cell2DState s = (Cell2DState) this.getState();
		s.setX(this.nextMove.x);
		s.setY(this.nextMove.y);
		this.setState(s);
		
		// Compute the next position
		if(this.canSeePrey == false){
			roaming();
		}
		else{
			followPrey();
		}
		
		// Update next move
		this.nextMove.x = s.getX() + this.direction.x;
		this.nextMove.y = s.getY() + this.direction.y;
		normailizePosition(this.nextMove);
	}

	// Send a request for the next position
	private void sendInformation() {
		send(GBROADCAST, getMoveView());
	}

	private Move2DView getMoveView(){
		Cell2DState state = (Cell2DState) this.getState();
		return new Move2DView(state.getX(), state.getY(), this.nextMove.x, this.nextMove.y, "Predator");
	}
	
	private void roaming() {
		Random r = new Random();

		double trial = r.nextDouble();
		if (trial < CHANGE_DIR_PROB) {
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
			else if( c== 2){
				int tmp = this.direction.x;
				this.direction.x = -this.direction.y;
				this.direction.y = -tmp;
			}
		}
		System.out.println("direction = " + this.direction.toString());
	}
	
	public void followPrey(){
		Cell2DState s = (Cell2DState)this.getState();
		Point2i shift = new Point2i();
		
		// Choose traking target
		if(this.trackedEdge == 0){
			shift.x = 1; shift.y = 0;
		}else if(this.trackedEdge == 1){
			shift.x = 0; shift.y = 1;
		}else if(this.trackedEdge == 2){
			shift.x = -1; shift.y = 0;
		}else if(this.trackedEdge == 3){
			shift.x = 0; shift.y = -1;
		}
		
		
		int deltaX = (this.preyPos.x - s.getX()) + shift.x;
		int deltaY = (this.preyPos.y - s.getY()) + shift.y;
		
		if( Math.abs(deltaX) >= Math.abs(deltaY) ){
			this.direction.x = (int)Math.signum((double)deltaX);
			this.direction.y = 0;
		}
		else{
			this.direction.x = 0;
			this.direction.y = (int)Math.signum((double)deltaY);
		}
		
		System.out.println("Direction = " + this.direction.toString());
	}
	
	public void normailizePosition(Point2i pos){
		if(pos.x < 0) pos.x += DIM;
		if(pos.y < 0) pos.y += DIM;
		if(pos.x >= DIM) pos.x -= DIM;
		if(pos.y >= DIM) pos.y -= DIM;
	}
}
