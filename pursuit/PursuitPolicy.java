package it.unipr.aotlab.actomos.examples.pursuit;

import it.unipr.aotlab.code.actor.Reference;

import java.io.Serializable;
import java.util.List;

import javafx.util.Pair;

import javax.vecmath.Point2i;

public interface PursuitPolicy extends Serializable{

	public Point2i movePrey(List<Point2i> goals, List<Point2i> predators, Prey instance);

	public Point2i movePredator(List<Pair<Reference, Point2i>> predators, Point2i prey, Predator instance);

	public Pair<Reference, Point2i> solveConflict(List<Pair<Reference, Point2i>> conflictingMessages);
}
