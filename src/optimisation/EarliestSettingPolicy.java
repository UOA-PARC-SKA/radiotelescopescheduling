package optimisation;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import observation.Connection;
import observation.Observable;
import observation.Pointable;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.TelescopeState;
import observation.interference.SkyState;
import simulation.Clock;
import util.Utilities;

public class EarliestSettingPolicy extends DispatchPolicy {
	
	private SimpleSlewDistancePolicy sdp;
	
	public void initialise(Properties props, Telescope scope, Schedule s, List<Target> targets, SkyState state)
	{
		super.initialise(props, scope, s, targets, state);
		sdp = new SimpleSlewDistancePolicy();
		sdp.initialise(props, scope, s, targets, state);
	}

	@Override
	public Connection findNextPath(Pointable pointable) {
		long minTime = Long.MAX_VALUE;
		Connection next = null;
		
		for (Connection conn : pointable.getNeighbours()) 
		{
			Target t = (Target) conn.getOtherTarget(pointable);
			
			HorizonCoordinates hc = t.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
			//there are generally two ways to slew to a location (one way or the other)
			//this applies the shortest
			TelescopeState possState = telescope.getStateForShortestSlew(hc);
			GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
			int slewInSeconds = (int) possState.getSlewTime();
			setTime.add(GregorianCalendar.SECOND, slewInSeconds);
			long time = (long)Conversions.getTimeUntilObjectSetsInSeconds(telescope.getLocation(), t, setTime);
			
			
			List<Observable> obs = t.getObservables();
			for (Observable observable : obs) 
			{
			//	System.out.println(observable.getName()+" time "+time);

				//we want the min time but only if there is sufficient time to complete a potential observation
				if(time >= observable.getRemainingIntegrationTime() && minTime > time)
				{
					next = conn;
					minTime = time;
			//		System.out.println("Min time "+minTime);
					currentTelescopeState = possState;
				}
			}
		}

		//some targets never set, so we have to have a secondary criterion:
		if (next == null)
			return sdp.findNextPath(pointable);
		telescope.applyNewState(currentTelescopeState);
		schedule.addLink(next, currentTelescopeState);
		return next;
	}

}
