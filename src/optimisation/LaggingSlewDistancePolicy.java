package optimisation;

import observation.Connection;
import observation.Pointable;
import observation.Telescope;
import observation.TelescopeState;
import simulation.Clock;

public class LaggingSlewDistancePolicy extends DispatchPolicy {
	
	private Pointable previous = null;

	@Override
	public Connection findNextPath(Pointable current) {
		long minDist = Long.MAX_VALUE;
		Connection next = null;
		long currentDist = 0;
		
		//System.out.println();
		for (Connection conn : current.getNeighbours()) 
		{
			Pointable p = conn.getOtherTarget(current);
			currentDist = Telescope.calculateShortestSlewTimeBetween(current.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()),p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
			//include the distance to the previous location (the hc are recalculated because time has elapsed since the last call to findNextPath)
			if(previous != null)
				currentDist += Telescope.calculateShortestSlewTimeBetween(current.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()),p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
			
			//System.out.println("Angle dist "+conn.getDistance()+" slew time "+possStates.get(0).getSlewTime());
//			if(possStates.size() > 1)
//				System.out.println("States "+possStates.size());
			if(minDist > currentDist)
			{
				next = conn;
				minDist = currentDist;

			}
		}
		Pointable p = next.getOtherTarget(current);
		TelescopeState currentTelescopeState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));

		//System.out.println(next.getDistance()+" "+state.getSlewTime());
		telescope.applyNewState(currentTelescopeState);
		schedule.addLink(next, currentTelescopeState);
		previous = current;
//		updateObservable((Target)next.getOtherTarget(current));
		return next;
	
	}

}
