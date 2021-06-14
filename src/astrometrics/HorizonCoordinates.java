package astrometrics;

import java.util.GregorianCalendar;

public class HorizonCoordinates 
{
	private double altitude;
	private double azimuth ;
	
	private HorizonCoordinates(double alt, double az)
	{
		this.altitude = alt;
		this.azimuth = az;
	}
	
	public double getAltitude() {
		return altitude;
	}
	public double getAzimuth() {
		return azimuth;
	}
	//potential slow down. Look at a pool for HorizonCoordinates objects
	public static HorizonCoordinates getHorizonCoordinates (EquatorialCoordinates ec, Location loc, GregorianCalendar gc)
	{
		double hourAngle = getHourAngle(gc, ec, loc);
		double sinAltitude = Math.sin(ec.getDeclination()) * Math.sin(loc.getLatitude())
				+ Math.cos(ec.getDeclination()) * Math.cos(loc.getLatitude()) * Math.cos(hourAngle);
		double altitude = Math.asin(sinAltitude);
		//
		double cosA = (Math.sin(ec.getDeclination()) - sinAltitude * Math.sin(loc.getLatitude()))
				/(Math.cos(altitude) * Math.cos(loc.getLatitude()));
		double a = Math.acos(cosA);
		
		double azimuth = a;
		//If sin(HA) is negative, then AZ = A, otherwise AZ = 360 - A 
		if (Math.sin(hourAngle) >= 0)
			azimuth = 2*Math.PI - a;
		HorizonCoordinates hc = new HorizonCoordinates(altitude, azimuth);
		
		return hc;
	}
	
	// LMST local mean sidereal time
	// MST same as GST or GMST means sidereal time (sidereal time at Greenwich)
	public static double getHourAngle(GregorianCalendar gc, EquatorialCoordinates ec, Location loc)
	{
		
		double lmst =  Conversions.convertUTCToGMST(gc) + loc.getLongitude();
		double hourAngle = lmst - ec.getRightAscension();
		return hourAngle;
	}
	
	public static HorizonCoordinates getPredefined(double alt, double az)
	{
		return new HorizonCoordinates(alt, az);
	}
	
	public double calculateAngularDistanceTo(HorizonCoordinates other)
	{
		double theta = Math.acos(Math.sin(this.altitude) 
				* Math.sin(other.altitude) 
				+ Math.cos(this.altitude) 
				* Math.cos(other.altitude) 
				* Math.cos (this.azimuth
				- other.azimuth));
		return theta;
	}
	

}
