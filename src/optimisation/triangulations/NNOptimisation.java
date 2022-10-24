package optimisation.triangulations;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import astrometrics.HorizonCoordinates;
import astrometrics.Location;
import observation.Connection;
import observation.Pointable;
import observation.Target;
import simulation.Clock;
import util.exceptions.WrongTypeException;

public abstract class NNOptimisation 
{
	protected HashMap<Double, Target> hm;
	protected ArrayList<Double> sortedDist;
	
	
	// Pulsar1 has its neighbours sorted out. Pulsar2 is a candidate. Have to find the nearest existing 
	//neighbour of p1 for p2 to do the triangulation.

	protected void workOutTriangle(Pointable target1, Target candidate, double distance, double ratio, Location loc, GregorianCalendar gc) 
	{
		//0 = mainToClosest, 1 = closestToCandidate, 2 = candidateToMain;
		double[] dists = new double[3];
		double x = 0, y = 0, z = 0;
		double angle;
		double maxDist = 0;
		int largest = -1;
	//	System.out.println("Investigating "+candidate.getPulsarName());
		Pointable t, closest = null;
		double minAngle = Double.POSITIVE_INFINITY;
		
		
		//existing neighbours
		List<Connection> conns1 = target1.getNeighbours();
		try {
			x = target1.angularDistanceTo(candidate, loc, gc);
		} catch (WrongTypeException e) {
			//means the other one is of type 'Position'
			try {
			x =	candidate.angularDistanceTo(target1, loc, gc);
			} catch (WrongTypeException e1) {
				//this shouldn't happen, but anyway
				e1.printStackTrace();
			}
		}
		//find existing neighbour closest to candidate
		for (Connection connection : conns1) 
		{
			t =  connection.getOtherTarget(target1);	
			try {
				y = candidate.angularDistanceTo(t, loc, gc);
			} catch (WrongTypeException e) {
				try {
				y =	t.angularDistanceTo(candidate, loc, gc);
				} catch (WrongTypeException e1) {
					//this shouldn't happen, but anyway
					e1.printStackTrace();
				}
			}
			try {
				z = target1.angularDistanceTo(t, loc, gc);
			} catch (WrongTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			angle = Math.acos((Math.pow(x, 2) + Math.pow(z, 2) - Math.pow(y, 2))/(2*x*z));
			if (minAngle > angle)
			{
				minAngle = angle;
				closest = t;
			}	
			
		}
	//	System.out.println("Closest "+closest.getPulsarName() + " angle "+ minAngle);
		
	//	if(minAngle > )
		try {
			maxDist = dists[0] = target1.angularDistanceTo(closest, loc, gc);
		} catch (WrongTypeException e) {
			try {
				maxDist = dists[0] = closest.angularDistanceTo(target1, loc, gc);
			} catch (WrongTypeException e1) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		largest = 0;
		try {
			dists[1] = candidate.angularDistanceTo(closest, loc, gc);
		} catch (WrongTypeException e) {
			try {
				dists[1] = closest.angularDistanceTo(candidate, loc, gc);
			} catch (WrongTypeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			dists[2] = target1.angularDistanceTo(candidate, loc, gc);
		} catch (WrongTypeException e) {
			try {
				dists[2] = candidate.angularDistanceTo(target1, loc, gc);
			} catch (WrongTypeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		for (int j = 1; j < dists.length; j++) 
			if(dists[j] > maxDist)
			{
				maxDist = dists[j];
				largest = j;
			}
		switch (largest) 
		{// mainToClosest (very unlikely?)
		case 0:
			System.out.println("Closest "+closest+" cand "+candidate);
			System.out.println("Main to closest "+dists[0]);
			System.out.println("closest to cand "+dists[1]);
			System.out.println("Main to cand "+dists[2]);
			break;
		// closestToCandidate; candidate far away from closest included, so need a link between main and cand
		case 1:
			if(!target1.hasLinkToTarget(candidate))
			{
				Connection co = new Connection(target1, candidate, dists[2]);
				//to have a connection to this, the other one has to be a target
				if(target1 instanceof Target)
				{
					Target t1 = (Target) target1;
					//if the connection object already exists, don't make a new one
					if(candidate.hasLinkToTarget(t1))
						co = candidate.getConnectionForTarget(t1);
				}
	
				target1.addNeighbour(co);
			}
			break;
			// candidateToMain
		case 2:

			if (dists[2] < (dists[1] + dists[0]) * ratio)
			{
				if(!target1.hasLinkToTarget(candidate))
				{
					Connection co = new Connection(target1, candidate, dists[2]);
					//to have a connection to this, the other one has to be a target
					if(target1 instanceof Target)
					{
						Target t1 = (Target) target1;
						//if the connection object already exists, don't make a new one
						if(candidate.hasLinkToTarget(t1))
							co = candidate.getConnectionForTarget(t1);
					}		
					target1.addNeighbour(co);	
				}
			}
			break;
		}

	}
	
	protected boolean isReadyForObservation(Target t, Clock clock, Location loc)
	{
		if(!t.needsObserving())
			return false;
		if(t.hasCompleteObservation())
			return false;
		//within scintillation timescale of previous observation
		if(t.tooCloseToPreviousObservation(clock))
			return false;
		HorizonCoordinates hc = t.getHorizonCoordinates(loc, clock.getTime());
		if(hc.getAltitude() < 0)
			return false;

		return true;
	}

}
