package it.unipr.aotlab.actomos.examples.pursuit;

import it.unipr.aotlab.actomos.discrete.d2d.SimulatorD2D;
import it.unipr.aotlab.code.configuration.Configuration;
import it.unipr.aotlab.code.error.ConfigurationException;
import it.unipr.aotlab.code.error.ConfigurationInfo;
import it.unipr.aotlab.code.error.ErrorManager;
import it.unipr.aotlab.code.logging.BinaryWriter;
import it.unipr.aotlab.code.logging.ConsoleWriter;
import it.unipr.aotlab.code.logging.Logger;
import it.unipr.aotlab.code.logging.TextualFormatter;
import it.unipr.aotlab.code.runtime.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javax.vecmath.Point2i;

public final class Initiator2D {

	private static final int DIM;
	static {
		ResourceBundle b;

		try {
			b = ResourceBundle.getBundle("it.unipr.aotlab.actomos.examples.pursuit.pursuit");

			if (b.getString("field_dim") == null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			b = null;
			ErrorManager.notifyError(Controller.PROVIDER, new ConfigurationException(ConfigurationInfo.OP, new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.pursuit" }, e));
			System.exit(-1);
		}

		DIM = Integer.parseInt(b.getString("field_dim"));
	}
	
	public static void main(final String[] v) {
		Configuration c = Controller.INSTANCE.getConfiguration();

		
		c.setFilter(
		        Logger.LOGCONFIGURATION
		        | Logger.LOGCLOCK
		        | Logger.LOGBEHAVIORINIT
		        | Logger.LOGMESSAGEPROCESSING
		        | Logger.LOGOUTPUTMESSAGE
		        | Logger.LOGACTORSHUTDOWN);
		
		//c.setFilter(Logger.ALLLOGS);
		
		c.addHandler(BinaryWriter.class.getName(), null, null, "pursuit", "d2d");
		c.addHandler(ConsoleWriter.class.getName(), TextualFormatter.class.getName(), null);
		
		final long length = 100;
		final int sideX = DIM;
		final int sideY = DIM;
		
		List<Point2i> p= generateDifferentRandomPoints(5, DIM);
		
		final Integer preyPos[] = {p.get(0).x,p.get(0).y};
		final Integer goalPos[] = {9, 9};
		final Integer predatorsPos[] = {p.get(1).x,p.get(1).y,  p.get(2).x,p.get(2).y,  p.get(3).x,p.get(3).y,  p.get(4).x,p.get(4).y};

		final Object[] prey = new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.Prey", preyPos };
		final Object[] predator = new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.Predator", predatorsPos };
		final Object[] fixed_goal = new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.Goal", goalPos };
		final Object[] goal = new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.Goal", 0.01};
		final Object[] empty = new Object[] { "it.unipr.aotlab.actomos.examples.pursuit.Empty", 1.0 };

		final int radius = 1;

		c.setScheduler(SimulatorD2D.class.getName());

		c.setArguments(length, sideX, sideY, new Object[] { prey, predator /*fixed_goal*/}, /*new Object[]{ goal, empty }*/ null, radius);

		Controller.INSTANCE.run();
	}
	
	public static List<Point2i> generateDifferentRandomPoints(int pointsNumber, int limit){
		List<Point2i> pl = new ArrayList<>();
		Random r = new Random();
		
		
		pl.add(new Point2i(r.nextInt(limit), r.nextInt(limit)) );
		
		for(int count = 0; count < (pointsNumber-1);){
			Point2i p = new Point2i();
			p.x = r.nextInt(limit);
			p.y = r.nextInt(limit);
			
			if( pl.contains(p) == true)	continue;
			else{
				count++;
				pl.add(p);
			}
		}
		
		return pl;
	}

}
