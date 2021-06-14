package observation;

import java.util.ArrayList;
import java.util.List;

import astrometrics.AzimuthLimits;
import astrometrics.HorizonCoordinates;


public class TelescopeState 
{

	private static final double MINIMUM_ELEVATION = 0.0;
	private static final double MAXIMUM_ELEVATION = Math.PI/4;
	

	private HorizonCoordinates currentPosition;
	//slew time to current position
	private double slewTime;
	
	

	private TelescopeState(HorizonCoordinates pos, double time)
	{
		currentPosition = pos;

		slewTime = time;	
	}
	

	//depending on the access route (right or left turn) there can be two ways to slew to a location
	public static List<TelescopeState> getNewStates(TelescopeState current, HorizonCoordinates next)
	{

		long[] slewTimes = Telescope.calculateSlewTimesBetween(current.getCurrentPosition(), next);

		List<TelescopeState> states = new ArrayList<TelescopeState>();

		for (int i = 0; i < slewTimes.length; i++) 
		{
			TelescopeState nextState = new TelescopeState(next, slewTimes[i]);
			states.add(nextState);
		}
		return states;
	}
	

	


	public static TelescopeState getInitialState(HorizonCoordinates initial)
	{
		//it took zero slew time to get to the starting position
		return new TelescopeState(initial, 0);
	}

	public double getSlewTime() {
		return slewTime;
	}
	// during observation, the telescope position changes. 
	// Update at the end
	public void updatePosition(HorizonCoordinates hc)
	{
		this.currentPosition = hc;
	}

	public HorizonCoordinates getCurrentPosition() {
		return currentPosition;
	}
	
	

}
