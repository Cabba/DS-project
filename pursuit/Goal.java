package it.unipr.aotlab.actomos.examples.pursuit;

import it.unipr.aotlab.actomos.discrete.Cell;
import it.unipr.aotlab.code.actor.Case;
import it.unipr.aotlab.code.actor.Message;
import it.unipr.aotlab.code.actor.cases.Cycler;

import java.util.ArrayList;
import java.util.List;

public final class Goal extends Cell {

	private static final long serialVersionUID = 1L;
	
	/*
	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if (b.getString("goal.radius") == null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			b = null;
			ErrorManager.notifyError(Controller.PROVIDER, new ConfigurationException(ConfigurationInfo.OP, new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.pursuit" }, e));
			System.exit(-1);
		}
		
		RADIUS = Integer.parseInt(b.getString("goal.radius"));
	}
	*/

	public Goal() {
	}

	@Override
	public int getRadius() {
		return 0;
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
		return l;
	}

	// Case managing the scheduler cycle messages.
	private final class CyclerCase extends Cycler {
		private static final long serialVersionUID = 1L;

		private CyclerCase() {
			super();
		}

		/** {@inheritDoc} **/
		@Override
		public void process(final Message m) {
			sendInformation();
		}
	}

	// Send a request for the next position
	private void sendInformation() {
		send(GBROADCAST, this.getState().getView() );
	}
}
