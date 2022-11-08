package optimisation;


import observation.Connection;

import observation.Pointable;
import observation.Telescope;
import observation.TelescopeState;
import simulation.Clock;
import simulation.Simulation;

import java.util.ArrayList;
import java.util.List;

public class SimpleSlewDistancePolicy extends DispatchPolicy
{
	

	/*
	//Shortest path based on slew time
	public Connection findNextPath(Pointable current)
	{
		double minDist = Double.POSITIVE_INFINITY;
		Connection next = null;
		
		//System.out.println();
		for (Connection conn : current.getNeighbours()) 
		{
			Pointable p = conn.getOtherTarget(current);
			TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
			//System.out.println("Angle dist "+conn.getDistance()+" slew time "+possStates.get(0).getSlewTime());
//			if(possStates.size() > 1)
//				System.out.println("States "+possStates.size());
			if(minDist > possState.getSlewTime())
			{
				next = conn;
				minDist = possState.getSlewTime();
				currentTelescopeState = possState;
			}
	
		}
		//System.out.println(next.getDistance()+" "+state.getSlewTime());
		telescope.applyNewState(currentTelescopeState);
		schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
		return next;
	}
	 */

	@Override
	public Connection[] findNextPaths(Pointable[] currents)
	{
		double minDist = Double.POSITIVE_INFINITY;

		Connection[] next = new Connection[Simulation.NUMTELESCOPES];
		List<Pointable> points = new ArrayList<Pointable>();

		//System.out.println();
		for(int i = 0; i< Simulation.NUMTELESCOPES; i++){
			Telescope telescope = telescopes[i];
			minDist = Double.POSITIVE_INFINITY;
			Pointable point = null;
			for (Connection conn : currents[i].getNeighbours())
			{
				Pointable p = conn.getOtherTarget(currents[i]);
				TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock()[i].getTime()));
				//System.out.println("Angle dist "+conn.getDistance()+" slew time "+possStates.get(0).getSlewTime());
//			if(possStates.size() > 1)
//				System.out.println("States "+possStates.size());
				if(minDist > possState.getSlewTime() && !points.contains(p))
				{
					point = p;
					next[i] = conn;
					minDist = possState.getSlewTime();
					currentTelescopeStates[i] = possState;
				}
			}
			points.add(point);
			//System.out.println(next.getDistance()+" "+state.getSlewTime());
			telescope.applyNewState(currentTelescopeStates[i]);
			schedules[i].addLink(next[i], currentTelescopeStates[i]);
//		updateObservable((Target)next.getOtherTarget(current));

		}

		return next;
	}

}
