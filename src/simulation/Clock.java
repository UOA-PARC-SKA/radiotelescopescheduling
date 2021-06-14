package simulation;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class Clock 
{
	private GregorianCalendar gc;
	private int increment;
	private int magnitude;
	private static Clock simulationClock = null;
	private static Clock scheduleClock = null;
	
	private Clock () 
	{
		
		increment = 100;
		magnitude = GregorianCalendar.SECOND;
	}
	
	public void setSimulationSpeed(int increment)
	{
		this.increment = increment;
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
	
	public void advance()
	{
		gc.add(magnitude, increment);
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
	
	public static Clock getScheduleClock()
	{
		if (scheduleClock == null)		
			scheduleClock = new Clock();
		return scheduleClock;
	}
}
