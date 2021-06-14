package observation.interference;

import java.util.Date;

import astrometrics.HorizonCoordinates;
import ext.NovasLink;
import observation.Telescope;

/*
 * Represents planets and the sun that may come in the way of observations.
 */
public class CelestialBody 
{
	
	public static final int SUN = 10;
	public static final int MOON = 11;
	public static final int JUPITER = 5;
	private String name;
	private HorizonCoordinates horizonCoordinates;
	private Date coordDate; //to check whether the coords are stale
	private NovasLink novasLink;
	private Telescope telescope;
	private int novasID; //needed to find the coordinates in the novas module
	private double radius; // unlike far away pulsars, these have a radius
	
	public CelestialBody(String name, Date date, NovasLink link, Telescope t)
	{
		coordDate = new Date(date.getTime());
		novasLink = link;
		telescope = t;
		switch (name) {
		case "Sun":
			//angular diameter 31'31'' – 32'33'' 
			radius = 0.004734206;
			novasID = SUN;
			break;
		case "Moon":
			//angular diameter 29'20'' – 34'6''
			radius = 0.004959644;
			novasID = MOON;
			break;
		case "Jupiter":
			//angular diameter 29.8'' – 50.59''
			radius = 0.000122634;
			novasID = JUPITER;
			break;
		}
		horizonCoordinates = novasLink.getCelestialBodiesHC( date, novasID, t);
		
		//System.out.println("HC in java "+horizonCoordinates.getAltitude()+ " "+horizonCoordinates.getAzimuth());
	}
	
	
	public HorizonCoordinates getHorizonCoordinates(Date d) 
	{
		if(d.compareTo(coordDate) != 0) // coords are stale
		{
			coordDate = new Date(d.getTime());
			horizonCoordinates = novasLink.getCelestialBodiesHC(d, novasID, telescope);
			
		}
		
		return horizonCoordinates;
	}


	public String getName() {
		return name;
	}


	public int getNovasID() {
		return novasID;
	}


	public double getRadius() {
		return radius;
	}
}
