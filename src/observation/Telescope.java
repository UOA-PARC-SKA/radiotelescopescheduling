package observation;


import java.util.List;

import astrometrics.AzimuthLimits;
import astrometrics.Conversions;

import astrometrics.HorizonCoordinates;
import astrometrics.Location;

public class Telescope 
{
	public static final int PARKES = 0;
	public static final int WARKWORTH = 1;
	public static final int MOUNTPLEASANT = 2;
	
	
	
	public static final Location[] LOCATION = new Location[3];
	public static final AzimuthLimits[] MAX_MIN_AZIMUTHS = new AzimuthLimits[3];
	public static final HorizonCoordinates PARKING_COORDINATES = HorizonCoordinates.getPredefined(Math.PI/2, 0);
	
	// 1 deg per second
	private static final double  AZIMUTH_VELOCITY = 1.0 * Math.PI/180.0;
	private static final double  ELEVATION_VELOCITY = 1.0 * Math.PI/180.0;
	private static AzimuthLimits azimuthLimits;

	

	static
	{
		LOCATION[PARKES] = new Location (Conversions.degreesToRadians("-32:59:59.8"), Conversions.degreesToRadians("148:15:44.3"), 0.324);
		LOCATION[WARKWORTH] = new Location (Conversions.degreesToRadians("-36:25:59"), Conversions.degreesToRadians("174:39:46"), 0.090);
		LOCATION[MOUNTPLEASANT] = new Location (Conversions.degreesToRadians("-42:48:12.66"), Conversions.degreesToRadians("147:26:24.27"), 0);
		
		MAX_MIN_AZIMUTHS[PARKES] = new AzimuthLimits(Conversions.degreesToRadians(315), Conversions.degreesToRadians(-135));
	}
	private String name;
	private Location location;
	private TelescopeState currentState;
	private Observable currentObservable;

	
	public static Telescope getTelescope(String type)
	{
		switch (type) {
		case "Warkworth":		
			setAzimuthLimits(Telescope.MAX_MIN_AZIMUTHS[WARKWORTH]);
			return new Telescope(Telescope.LOCATION[Telescope.WARKWORTH], "Warkworth");
		case "MountPleasant":		
			setAzimuthLimits(Telescope.MAX_MIN_AZIMUTHS[MOUNTPLEASANT]);
			return new Telescope(Telescope.LOCATION[Telescope.MOUNTPLEASANT], "Mount Pleasant");
		default:
			setAzimuthLimits(Telescope.MAX_MIN_AZIMUTHS[PARKES]);
			return new Telescope(Telescope.LOCATION[Telescope.PARKES], "Parkes");
			
		}
	}
	
	private Telescope(Location l, String n)
	{
		this.name = n;
		this.location = l;
		//set to default
		this.currentState = TelescopeState.getInitialState(Telescope.PARKING_COORDINATES);
	}

	public Location getLocation() 
	{
		return location;
	}

	public Observable getCurrentObservable() 
	{
		return currentObservable;
	}

	public void setCurrentObservable(Observable currentObservable) 
	{
		this.currentObservable = currentObservable;
	}
	
	
	public List<TelescopeState> getNewStates(HorizonCoordinates nextPos)
	{
		return TelescopeState.getNewStates(this.currentState, nextPos);
		
	}
	
	public TelescopeState getStateForShortestSlew(HorizonCoordinates nextPos)
	{
		List<TelescopeState> states = TelescopeState.getNewStates(this.currentState, nextPos);
		TelescopeState state = null;
		double minDist = Double.POSITIVE_INFINITY;
		for (TelescopeState telescopeState : states) 
		{
			if(minDist > telescopeState.getSlewTime())
			{
				minDist = telescopeState.getSlewTime();
				state = telescopeState;
			}
		}
		return state;
	}

	public void applyNewState(TelescopeState ts)
	{
		this.currentState = ts;
	}
	public void applyWaitState ()
	{
		this.currentState = TelescopeState.getInitialState(Telescope.PARKING_COORDINATES);
	}
	
	public static long calculateShortestSlewTimeBetween(HorizonCoordinates current, HorizonCoordinates next)
	{
		long[] slewTimes = calculateSlewTimesBetween(current, next);
		
		long shortestTime = Long.MAX_VALUE;

		for (int i = 0; i < slewTimes.length; i++) 
		{
			if(slewTimes[i] < shortestTime)
				shortestTime = slewTimes[i];
		}

		return shortestTime;
	}
	
	public static long[] calculateSlewTimesBetween(HorizonCoordinates current, HorizonCoordinates next)
	{
		double diff_elevation = Math.abs( current.getAltitude() - next.getAltitude());
		double time_elevation = diff_elevation / ELEVATION_VELOCITY;
		double next_azimuth = next.getAzimuth();
		// consider every valid slew in azimuth
		while (next_azimuth > azimuthLimits.getMinAzimuth())
			next_azimuth -= 2*Math.PI;

		// after the above loop, the azimuth will be less than the minimum (or equal!)
		if(next_azimuth < azimuthLimits.getMinAzimuth())
			next_azimuth += 2*Math.PI;
		
		long[] shortestTime = new long[2];
		shortestTime[0] = Long.MAX_VALUE;
		shortestTime[1] = Long.MAX_VALUE;

		int i = 0;
		while (next_azimuth < azimuthLimits.getMaxAzimuth())
		{
			double diff_azimuth = Math.abs( current.getAzimuth() - next_azimuth );
			double time_azimuth = diff_azimuth / AZIMUTH_VELOCITY;

			double time = time_azimuth > time_elevation ? time_azimuth : time_elevation;

			
			shortestTime[i++] = (long) time;

			next_azimuth += 2*Math.PI;
		}
		return shortestTime;
	}

	public static void setAzimuthLimits(AzimuthLimits al)
	{
		azimuthLimits = al;
	}
}
