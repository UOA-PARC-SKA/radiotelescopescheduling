package optimisation.triangulations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import observation.Connection;
import observation.Pointable;
import observation.Target;
import simulation.Clock;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;

public class DynamicNNOptimisation extends NNOptimisation
{
	

	public DynamicNNOptimisation() 
	{
		hm = new HashMap<Double, Target>();
		sortedDist = new ArrayList<Double>();
	}
	
	//These targets are already the subset that needs observing
	public void createDynamicLinksByTriangles(List<Target> targets, Pointable current, double ratio, Clock clock, Location loc) throws OutOfObservablesException
	{
		// exclude everything that's not within 90 degrees

		current.clearNeighbours();
		Target target2;

		sortedDist.clear();
		hm.clear();

		for (int j = 0; j < targets.size(); j++) 
		{
			target2 = targets.get(j);
			if (current == target2)
				continue;
			//this checks
			//- whether the target needs observing
			//- whether the target's observation is complete already
			//- whether the target is within scintillation timescale of previous observation
			//- whether the target is up
			if(!isReadyForObservation(target2, clock, loc))
				continue;
			
			

			double dist = 0;
			try {
				dist = current.angularDistanceTo(target2, loc, clock.getTime());
			} catch (WrongTypeException e) 
			{
				try {
					dist = target2.angularDistanceTo(current, loc, clock.getTime());
				} catch (WrongTypeException e1) {
					// Should never happen, this means either one is a target
					e1.printStackTrace();
				}
			}
	//		if(dist > cutoff)
	//			continue;
			hm.put(dist, target2);
			sortedDist.add(dist);
		}
		if(sortedDist.isEmpty())
			throw new OutOfObservablesException();
		Collections.sort(sortedDist);

		//make connection to closest
		Connection c = new Connection(current, hm.get(sortedDist.get(0)), sortedDist.get(0));
		current.addNeighbour(c);

		for (int j = 1; j < sortedDist.size(); j++) 
		{
			target2 = hm.get(sortedDist.get(j));
			workOutTriangle(current, target2, sortedDist.get(j), ratio, loc, clock.getTime());

		}
		//	System.out.println("Pulsar "+ target1.getName()+" neighbour list "+target1.getNeighbours().size());
	}
	
	


}
