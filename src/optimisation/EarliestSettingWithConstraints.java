package optimisation;

import java.util.ArrayList;
import java.util.Collections;
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

public class EarliestSettingWithConstraints extends DispatchPolicy 
{
	private SimpleSlewDistancePolicy sdp;
	private SkyState skyState;
	//needed to store a target that will get interrupted in case we have no other ones
	private Connection onlyOption;
	TelescopeState onlyPossState;
	private long interruptedWhen;
	private double scintIntervalLimit;
	private List<WholeSheBang> orderedList;
	
	public void initialise(Properties props, Telescope scope, Schedule s, List<Target> targets, SkyState skyState)
	{
		super.initialise(props, scope, s, targets, skyState);
		this.skyState = skyState;
		scintIntervalLimit = Double.parseDouble(props.getProperty("scint_timescale_limit"));
		sdp = new SimpleSlewDistancePolicy();
		sdp.initialise(props, scope, s, targets, skyState);
		orderedList = new ArrayList<>();
	}
	
	/*This attempts to avoid satellites from the start. It also applies a factor of 1.5 to the expected integration time when scintillation timescale is short*/
	@Override
	public Connection findNextPath(Pointable pointable) 
	{
		orderedList.clear();
		WholeSheBang wsb;

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
				if(time >= (observable.getRemainingIntegrationTime() * getAdjustingFactor(observable)) )
				{
					wsb = new WholeSheBang();
					wsb.conn = conn;
					wsb.time = time;
			//		System.out.println("Min time "+minTime);
					wsb.tstate = possState;
					orderedList.add(wsb);
				}
			}
		}
		Collections.sort(orderedList);
//		System.out.println();
//		for (WholeSheBang whole : orderedList) 
//		{
//			System.out.println(whole.time);
//			Target t = (Target) whole.conn.getOtherTarget(pointable);
//			GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
//			int slewInSeconds = (int) whole.tstate.getSlewTime();
//			setTime.add(GregorianCalendar.SECOND, slewInSeconds);
//			if(!willBeInterruptedBySatellite(setTime, t, whole.conn, whole.tstate, whole.time))
//			{
//				next = whole.conn;
//				currentTelescopeState = whole.tstate;
//				break;
//			}
//		}
		//

//			if(next == null)
			{
				if(orderedList.isEmpty())
					return sdp.findNextPath(pointable);
				// if we have things in there, use them
				currentTelescopeState = orderedList.get(0).tstate;
				next = orderedList.get(0).conn;
			}
				

		telescope.applyNewState(currentTelescopeState);
		schedule.addLink(next, currentTelescopeState);
		return next;
	}
	
//	public Connection findNextPath(Pointable pointable) 
//	{
//		onlyOption = null;
//		interruptedWhen = 0;
//		long minTime = Long.MAX_VALUE;
//		Connection next = null;
//		
//		for (Connection conn : pointable.getNeighbours()) 
//		{
//			Target t = (Target) conn.getOtherTarget(pointable);
//			
//			HorizonCoordinates hc = t.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
//			//there are generally two ways to slew to a location (one way or the other)
//			//this applies the shortest
//			TelescopeState possState = telescope.getStateForShortestSlew(hc);
//			GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
//			int slewInSeconds = (int) possState.getSlewTime();
//			setTime.add(GregorianCalendar.SECOND, slewInSeconds);
//			long time = (long)Conversions.getTimeUntilObjectSetsInSeconds(telescope.getLocation(), t, setTime);
//			
//			List<Observable> obs = t.getObservables();
//			for (Observable observable : obs) 
//			{
//				
//				time = getAdjustedTime(time, observable);
//				if(willBeInterruptedBySatellite(setTime, t, conn, possState, time))
//					continue;
//				System.out.println(observable.getName()+" time "+time);
//
//				//we want the min time but only if there is sufficient time to complete a potential observation
//				if(time >= observable.getRemainingIntegrationTime() && minTime > time)
//				{
//					next = conn;
//					minTime = time;
//			//		System.out.println("Min time "+minTime);
//					currentTelescopeState = possState;
//				}
//			}
//		}
//		//
//		if (next == null)
//		{
//			next = onlyOption;
//			currentTelescopeState = onlyPossState;
//		}
//		if (next == null)
//			return sdp.findNextPath(pointable);
//		telescope.applyNewState(currentTelescopeState);
//		schedule.addLink(next, currentTelescopeState);
//		return next;
//	}
	
	//since we don't know the actual integration time, the outcome is not necessarily fact
	private boolean willBeInterruptedBySatellite(GregorianCalendar gc, Target target, Connection conn, TelescopeState state, long totalObsTime)
	{
		long newObsTime = skyState.willCollideWithSatelliteAt(gc, target, telescope, totalObsTime);
		
		if(totalObsTime > newObsTime)
		{
			if(newObsTime > interruptedWhen)
			{
				interruptedWhen = newObsTime;
				onlyOption = conn;
				onlyPossState = state;
			}
			return true;
		}
		return false;
	}
	
	private double getAdjustingFactor( Observable obs)
	{
		//1800 usually doesn't take more than nominal time
		if(obs.getScintillationTimescale() < 1800)
		{
			if(obs.getScintillationTimescale() > scintIntervalLimit * obs.getExpectedIntegrationTime())
				return 1.5;
		}
		return 1.0;
	}
	
	private class WholeSheBang implements Comparable<WholeSheBang>
	{
		long time;
		Connection conn;
		TelescopeState tstate;
		


		@Override
		public int compareTo(WholeSheBang o) 
		{
			if(o.time > this.time)
				return -1;
			else if (o.time < this.time)
				return 1;
			return 0;
		}
		
		
	}
}


