package optimisation;


import observation.Connection;

import observation.Pointable;
import observation.TelescopeState;
import simulation.Clock;

public class SimpleSlewDistancePolicy extends DispatchPolicy
{
	

	
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




	
	

}
