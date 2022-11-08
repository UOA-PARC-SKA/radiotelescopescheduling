package optimisation;

import java.util.ArrayList;
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
import simulation.Simulation;
import util.Utilities;

public class EarliestSettingPolicy extends DispatchPolicy {
	
	private SimpleSlewDistancePolicy sdp;
	
	public void initialise(Properties props, Telescope[] scopes, Schedule[] s, List<Target> targets, SkyState state)
	{
		super.initialise(props, scopes, s, targets, state);
		sdp = new SimpleSlewDistancePolicy();
		sdp.initialise(props, scopes, s, targets, state);
	}

	/*
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
	 */


	@Override
	public Connection[] findNextPaths(Pointable[] pointables) {
		long minTime = Long.MAX_VALUE;
		Connection[] next = new Connection[Simulation.NUMTELESCOPES];
		List<Target> targets = new ArrayList<Target>();

		for(int i = 0; i< Simulation.NUMTELESCOPES; i++){
			Target target = null;
			minTime = Long.MAX_VALUE;

			for (Connection conn : pointables[i].getNeighbours())
			{
				Target t = (Target) conn.getOtherTarget(pointables[i]);
				if(targets.contains(t))
					continue;

				HorizonCoordinates hc = t.getHorizonCoordinates(telescopes[i].getLocation(), Clock.getScheduleClock()[i].getTime());
				//there are generally two ways to slew to a location (one way or the other)
				//this applies the shortest
				TelescopeState possState = telescopes[i].getStateForShortestSlew(hc);
				GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock()[i].getTime());
				int slewInSeconds = (int) possState.getSlewTime();
				setTime.add(GregorianCalendar.SECOND, slewInSeconds);
				long time = (long)Conversions.getTimeUntilObjectSetsInSeconds(telescopes[i].getLocation(), t, setTime);


				List<Observable> obs = t.getObservables();
				for (Observable observable : obs)
				{
					//	System.out.println(observable.getName()+" time "+time);

					//we want the min time but only if there is sufficient time to complete a potential observation
					if(time >= observable.getRemainingIntegrationTime() && minTime > time)
					{
						target = t;
						next[i] = conn;
						minTime = time;
						//		System.out.println("Min time "+minTime);
						currentTelescopeStates[i] = possState;
					}
				}
			}

			targets.add(target);
		}

		//some targets never set, so we have to have a secondary criterion:
		for(int i = 0; i< Simulation.NUMTELESCOPES; i++)
			if (next[i] == null)
				return sdp.findNextPaths(pointables);

		for(int i = 0; i< Simulation.NUMTELESCOPES; i++){
			telescopes[i].applyNewState(currentTelescopeStates[i]);
			schedules[i].addLink(next[i], currentTelescopeStates[i]);
		}
		return next;
	}

}
