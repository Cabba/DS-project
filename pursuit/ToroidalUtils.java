package it.unipr.aotlab.actomos.examples.pursuit;

import javax.vecmath.Point2i;

public class ToroidalUtils {
	
	public static void normailizePosition(Point2i pos, int max) {
		if (pos.x < 0)
			pos.x += max;
		if (pos.y < 0)
			pos.y += max;
		if (pos.x >= max)
			pos.x -= max;
		if (pos.y >= max)
			pos.y -= max;
	}

	public static Point2i toroidalDistance(Point2i s, Point2i d, int max) {
		Point2i res = new Point2i();

		int x = d.x - s.x;
		int y = d.y - s.y;

		int xshift = (d.x + max) - s.x;
		int yshift = (d.y + max) - s.y;

		// Assign the best distance for each component
		if (Math.abs(x) <= Math.abs(xshift))
			res.x = x;
		else
			res.x = xshift;
		if (Math.abs(y) <= Math.abs(yshift))
			res.y = y;
		else
			res.y = yshift;

		return res;
	}
}
