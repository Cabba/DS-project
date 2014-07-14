package it.unipr.aotlab.actomos.tools;

import it.unipr.aotlab.actomos.discrete.d2d.Cell2DView;
import it.unipr.aotlab.code.actor.Reference;
import it.unipr.aotlab.code.interaction.Cycle;
import it.unipr.aotlab.code.logging.content.Destroyed;
import it.unipr.aotlab.code.logging.content.Initialized;
import it.unipr.aotlab.code.logging.content.Processed;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Point2i;

/**
 * 
 * The {@code LogProcessorD2D} class processes a binary logging file of a 2D
 * discrete space simulation generating a 2D grid graphical representation of
 * each step of the simulation.
 * 
 * These 2D graphical representations should identify the behaviors of the
 * entities acting in the world with different colors.
 * 
 **/

public final class LogProcessorD2D extends LogProcessor {
	// Cell view colors.
	private static final Color EMPTY = Color.WHITE;
	// Cell pixel size.
	private static final Dimension DIM = new Dimension(15, 15);

	private final Map<Reference, Color> colorMap;
	private final Map<Reference, Point2i> positionMap;
	
	// World x-side.
	private int sideX;
	// World Y-side.
	private int sideY;
	// World view.
	private JLabel[][] world;

	/**
	 * Class constructor.
	 * 
	 **/
	public LogProcessorD2D() {
		this.colorMap = new HashMap<>();
		this.positionMap = new HashMap<>();
	}

	/**
	 * Gets the size of the world x-side.
	 * 
	 * @return the size.
	 * 
	 **/
	public int getSideX() {
		return this.sideX;
	}

	/**
	 * Gets the size of the world y-side.
	 * 
	 * @return the size.
	 * 
	 **/
	public int getSideY() {
		return this.sideY;
	}

	/** {@inheritDoc} **/
	@Override
	public void additional(final Object[] v) {
		int i = 1;

		this.sideX = (int) v[i++];
		this.sideY = (int) v[i];

		this.world = new JLabel[this.sideX][this.sideY];

		JPanel panel = new JPanel(new GridLayout(this.sideX, this.sideY, 1, 1));
		panel.setBackground(Color.BLACK);
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		for (int x = 0; x < this.sideX; x++) {
			for (int y = 0; y < this.sideY; y++) {
				this.world[x][y] = new JLabel();
				this.world[x][y].setOpaque(true);
				this.world[x][y].setBackground(EMPTY);
				this.world[x][y].setPreferredSize(DIM);
				this.world[x][y].setToolTipText("x=" + x + ", y=" + y);

				panel.add(this.world[x][y]);
			}
		}

		final int dim = 600;

		this.component = new JScrollPane(panel);

		this.component.setPreferredSize(new Dimension(dim, dim));
	}

	/** {@inheritDoc} **/
	@Override
	public void clear() {
		for (int x = 0; x < this.sideX; x++) {
			for (int y = 0; y < this.sideY; y++) {
				this.world[x][y].setBackground(BACKGROUND);
			}
		}
	}

	/** {@inheritDoc} **/
	@Override
	public void update(final Object o) {	
		
		if (o instanceof Processed) {
			Processed p = (Processed) o;
			
			if (p.getMessage().getContent() instanceof Cycle) {
				Cell2DView v = (Cell2DView) p.getView();
				
				if(this.colorMap.containsKey(p.getReference()) && this.positionMap.containsKey(p.getReference())){
					// Reset the old position
					Point2i old = this.positionMap.remove(p.getReference());
					this.positionMap.put(p.getReference(), new Point2i(v.getX(), v.getY()));
					
					this.world[old.x][old.y].setBackground(BACKGROUND);
					this.world[v.getX()][v.getY()].setBackground( this.colorMap.get(p.getReference()) );
				}
			}
		}
		else if (o instanceof Initialized) {
			Initialized i = (Initialized) o;
			
			if ((i.getView() != null) && (i.getView() instanceof Cell2DView)) {
				this.colorMap.put(i.getReference(), this.colors.get(i.getBehavior()));
				
				Cell2DView p = (Cell2DView) i.getView();
				// Track the current position
				this.positionMap.put(i.getReference(), new Point2i(p.getX(), p.getY()));
			
				this.world[p.getX()][p.getY()].setBackground(this.colors.get(i.getBehavior()));
			}
		} else if (o instanceof Destroyed) {
			Destroyed d = (Destroyed) o;

			if ((d.getView() != null) && (d.getView() instanceof Cell2DView)) {
				Cell2DView p = (Cell2DView) d.getView();

				this.world[p.getX()][p.getY()].setBackground(BACKGROUND);
			}
		}
	}

	/** {@inheritDoc} **/
	@Override
	public void refresh(final Color o, final Color n) {
		for (int x = 0; x < this.sideX; x++) {
			for (int y = 0; y < this.sideY; y++) {
				if (this.world[x][y].getBackground().equals(o)) {
					this.world[x][y].setBackground(n);
				}
			}
		}
	}
}
