package simulation;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.TimeZone;


public class Clock extends Observable
{
	private GregorianCalendar gc;
	private int increment;
	private int magnitude;
	private double speedMultiplier;
	private static Clock simulationClock = null;
	private static Clock[] scheduleClocks = null;
	
	private Clock () 
	{
		
		increment = 100;
		magnitude = GregorianCalendar.SECOND;
		speedMultiplier = 1.0;
	}
	
	public void setSimulationSpeed(int increment)
	{
		this.increment = increment;
	}

	public void setSpeedMultiplier(double multiplier) {
		this.speedMultiplier = Math.max(0, Math.min(5.0, multiplier));
		setChanged();
		notifyObservers();
	}

	public double getSpeedMultiplier() {
		return speedMultiplier;
	}

	public void start()
	{
		gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	}
	
	public void startAt(Date date)
	{
		gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		gc.setTime(date);
	}
	
	public void advanceSimulationBy(int incr)
	{
		int adjustedIncr = (int)(incr * speedMultiplier);
		gc.add(magnitude, adjustedIncr);
		setChanged();
		notifyObservers();
	//	gc.add(magnitude, 0);
	}

	public void advanceBy(int incr)
	{
	//	gc.add(magnitude, 0);
		gc.add(magnitude, incr);
	}
	
	public GregorianCalendar getTime()
	{
		return gc;
	}
	
	public static Clock getSimulationClock()
	{
		if (simulationClock == null)		
			simulationClock = new Clock();
		return simulationClock;
	}
	
	public static Clock[] getScheduleClock()
	{
		if (scheduleClocks == null)	{
			scheduleClocks = new Clock[Simulation.NUMTELESCOPES];
			for(int i=0; i< Simulation.NUMTELESCOPES; i++)
				scheduleClocks[i] = new Clock();
		}
		return scheduleClocks;
	}
}
