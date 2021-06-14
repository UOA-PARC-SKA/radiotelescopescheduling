package observation.interference;

import java.util.GregorianCalendar;

import astrometrics.HorizonCoordinates;
import ext.NoradLink;
import observation.Telescope;
import util.Utilities;


//To figure out whether something interferes with an observation, use the angular distance in HorizonCoordinates

public class Satellite implements BadThingThatMoves 
{
	//exclusion zone around satellites, the same for all of them
	private static double minAngDistToSatellite;
	//lines we get from a file and pass to norad for calculation of position 
	private String tleLine1;
	private String tleLine2;
	private String name;
	private String type; //grouping by file name
	private HorizonCoordinates horizonCoordinates;
	private GregorianCalendar coordDate; //to check whether the coords are stale
	private NoradLink noradLink;
	private Telescope telescope;
	//angularDistancePerSecond
	private double averageSpeed;
	private double stDevSpeed;
	

	public Satellite(String temp)
	{
		name = temp;
	}
	
	public void initialise(GregorianCalendar gc, NoradLink link, Telescope t)
	{
		coordDate = gc;
		noradLink = link;
		telescope = t;
		horizonCoordinates = noradLink.getSatelliteHC(this, telescope, coordDate);
		
	}
	
	@Override
	public HorizonCoordinates getHorizonCoordinates(GregorianCalendar gc) 
	{
		if(!gc.equals(coordDate)) // coords are stale
		{
			coordDate = Utilities.cloneDate(gc);
			horizonCoordinates = noradLink.getSatelliteHC(this, telescope, coordDate);
		}
		
		return horizonCoordinates;
	}


	public void setFirstLine(String temp) 
	{
		
		tleLine1 = temp;
	}

	public void setSecondLine(String temp) 
	{
		tleLine2 = temp;
		
	}

	public String getName() {
		return name;
	}
	
	public String getFirstLine() {
		return tleLine1;
	}
	
	public String getSecondLine() {
		return tleLine2;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(name +"\n");
		sb.append(tleLine1+"\n");
		sb.append(tleLine2+"\n");
		return sb.toString();
	}

	public void setHorizonCoordinates(HorizonCoordinates horizonCoordinates) {
		this.horizonCoordinates = horizonCoordinates;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
	public void calculateSpeed(double[] angDist)
	{
		this.averageSpeed = Utilities.getMean(angDist);
		this.stDevSpeed = Utilities.getStdDev(angDist, averageSpeed);
		//make it per second:
		this.averageSpeed /= 60;
		this.stDevSpeed /= 60;
		//System.out.println(this.getName()+", "+averageSpeed);
	}
	//speed is radians per second
	public double getAverageSpeed() {
		return averageSpeed;
	}

	public double getStDevSpeed() {
		return stDevSpeed;
	}

	public static void setMinAngDist(double angDist)
	{
		minAngDistToSatellite = angDist;
	}
	
	public static double getMinAngDist()
	{
		return minAngDistToSatellite;
	}

}
