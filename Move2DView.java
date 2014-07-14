package it.unipr.aotlab.actomos.discrete.d2d;

import it.unipr.aotlab.code.actor.StateView;

import javax.vecmath.Point2i;

/**
 *
 * The {@code Cell2DView} class defines a view of the state representation
 * of a 2D cell that shows the information about its position.
 *
**/

public final class Move2DView implements StateView
{
  private static final long serialVersionUID = 1L;

  private Point2i pos;
  private Point2i nextPos;
  private final String info;

  public Move2DView(int x, int y, int nextX, int nextY, String info)
  {
	this.pos = new Point2i();
	this.nextPos = new Point2i();
	
    this.pos.x = x;
    this.pos.y = y;
    
    this.nextPos.x = nextX;
    this.nextPos.y = nextY;
    
    this.info = info;
  }
  
  public Point2i getPos(){
	  return this.pos;
  }
  
  public Point2i getNextPos(){
	  return this.nextPos;
  }
  
  public String getInfo(){
	  return this.info;
  }
  
  
}
