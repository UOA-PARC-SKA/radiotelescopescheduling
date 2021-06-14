package astrometrics;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import observation.Target;


public class Conversions 
{
	
	public static double degreesToRadians(String deg)
	{
		String[] parts = deg.split(":");
		boolean negative = false;
		//System.out.println("Degrees "+deg);
		
		double degrees = Double.parseDouble(parts[0]);
		if(degrees < 0)
			negative = true;
		for (int i = 1; i < parts.length; i++) 
		{
			switch (i) {
			case 1:
				if(parts[1].length() > 0)
				{
					double minutes = Double.parseDouble(parts[1]);
					if(negative)
						degrees -= minutes/60;
					else
						degrees += minutes/60;
				}			
				break;

			case 2:
				if(parts[2].length() > 0)
				{		
					double seconds = Double.parseDouble(parts[2]);
					if(negative)
						degrees += seconds/3600;
					else
						degrees += seconds/3600;
				}				
				break;
			}
		}
		//System.out.println(degrees);
		double d = Math.toRadians(degrees);
		//System.out.println(d);
		return d;
	}
	
	public static double hoursToRadians(String hours)
	{
		String[] parts = hours.split(":");
		//System.out.println("Hours "+ hours);
		double decimalHours = Double.parseDouble(parts[0]);
		
		for (int i = 1; i < parts.length; i++) 
		{
			
			switch (i) {
			case 1:
				if(parts[1].length() > 0)
				{
					double minutes = Double.parseDouble(parts[1]);
					decimalHours += minutes/60;
				}				
				break;
			case 2:
				if(parts[2].length() > 0)
				{
					double seconds = Double.parseDouble(parts[2]);
					decimalHours += seconds/3600;
				}				
				break;
			case 3:
				if(parts[3].length() > 0)
				{
					double millis = Double.parseDouble(parts[3]);
					decimalHours += millis/3600000;
				}				
				break;
			}
		}
		//System.out.println(decimalHours);
		double radians = decimalHours/12 * Math.PI;
		//System.out.println(d);
		
		return radians;	
	}
	
	// UTC is GMT and GMST is sidereal time at Greenwich
	public static double convertUTCToGMST(GregorianCalendar gc)
	{
		double mjd = getMJD(gc);
		//Julian centuries from fundamental epoch J2000 to this UT
		double julianCent = (mjd-51544.5)/36525;

		double s2r=7.272205216643039903848711535369e-5;

		double gmst = (mjd % 1.0) *(2*Math.PI)+(24110.54841+
			                         (8640184.812866+
			                         (0.093104-(6.2e-6)*julianCent)*julianCent)*julianCent)*s2r;
		gmst %= (2*Math.PI);
		
		return gmst;
	}
	
	public static double getMJD(GregorianCalendar gc)
	{
		 int year = gc.get(GregorianCalendar.YEAR);
		 int month = gc.get(GregorianCalendar.MONTH) + 1;

		 int days = (1461*(year-(12-month)/10+4712))/4
		    +(306*((month+9)%12)+5)/10
		    -(3*((year-(12-month)/10+4900)/100))/4
		    +gc.get(GregorianCalendar.DAY_OF_MONTH)-2399904;

		  double hours = gc.get(GregorianCalendar.HOUR_OF_DAY) + gc.get(GregorianCalendar.MINUTE)/60.0 + gc.get(GregorianCalendar.SECOND)/3600.0;
		  return days+(hours/24);
	}
	
	public static double degreesToRadians(double value)
	{
		double denominator = 180.0/Math.PI;
		return value / denominator;
	}
	
	public static double hoursToRadians(double value)
	{
		double denominator = 12.0/Math.PI;
		return value/denominator;
	}
	/*
	 * Not used. Very accurate way of calculating LST at the time of rise or set of an object
	 */
//	public static long calculateTimeOfHorizonCrossing(Location loc, EquatorialCoordinates eqc, double parallax, double radius)
//	{
//		//according to https://www.imcce.fr/langues/en/grandpublic/systeme/promenade-en/pages3/367.html
//		//R is the refraction at horizon...we could use the value R = 34' adopted in the Nautical Ephemerides 
//		double R = 34;
//		double  radiusofEarth =  6378140; 
//		//eta 1 compensates for the elevation of the point of view
//		double eta1 = Math.acos(radiusofEarth/(radiusofEarth+loc.getElevation()));
//		//eta 2 compensates for mountains at the horizon; we choose to ignore
//		double eta2 = 0;
//		//the parallax is negligeable for all bodies except the Moon for which we will take it as 57'.
//		//radius is the radius of the object, use 16' for the sun and moon, 0 for everything else (jupiter??)
//		double h0 = parallax - R - radius - eta1 + eta2;
//				
//		double cosHourAngle = (Math.sin(h0) - Math.sin(loc.getLatitude()) * Math.sin(eqc.getDeclination()))
//				/(Math.cos(loc.getLatitude())*Math.cos(eqc.getDeclination()));
//		
//		//set time: (probably radians, multiply by 12 and divide by PI)
//		long time = (long) (eqc.getRightAscension() + Math.acos(cosHourAngle));
//		//(rise time would be eqc.getRightAscension() - Math.acos(cosHourAngle);
//		return time;
//	}

	
	
/*
 * Returns the time in milliseconds
 */
	
	public static long calculateTimeOfHorizonCrossing(Location loc, EquatorialCoordinates eqc, GregorianCalendar gc)
	{
		
		double cosHourAngle = -Math.tan(loc.getLatitude())*Math.tan(eqc.getDeclination());
		//some never set, negatives need to be checked
		if(cosHourAngle < -1 || cosHourAngle > 1)
			return 4102405200000l;

		double lmstInRadians =  eqc.getRightAscension() + Math.acos(cosHourAngle);
//		
		double gmst = convertUTCToGMST(gc);
		double lmst = gmst + loc.getLongitude();	
		double deltaLMST = lmstInRadians - lmst;
		deltaLMST += 2 * Math.PI;
		deltaLMST %= 2 * Math.PI;
		
		long time = (long)  (deltaLMST * 12/Math.PI  * 3600000);
		time += gc.getTimeInMillis();
		// a circumpolar object is one whose dec is larger than 90 deg - latitude of location
//		double circumpolarLimitinRadians = (Math.PI/2) - Math.abs(loc.getLatitude());
//		if(Math.abs(eqc.getDeclination()) > circumpolarLimitinRadians)
//		{
//			System.out.println("Is circumpolar "+eqc.getDeclination());
//			//return a very large date
//			//return time + 86400000;
//			 return 4102405200000l;
//		}
		//subtracting three minutes means we have a safety margin
		return time-240000;
		
	}
	
	//need seconds to compare with observation times
	public static double getTimeUntilObjectSetsInSeconds(Location loc, Target target, GregorianCalendar gc)
	{
		long objectSets = Conversions.calculateTimeOfHorizonCrossing(loc, target.getEquatorialCoordinates(), gc);
		GregorianCalendar gc2 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		gc2.setTimeInMillis(objectSets);
		HorizonCoordinates hc = target.getHorizonCoordinates(loc, gc2);
		if(hc.getAltitude() < 0)
			System.out.println("Altitude expected "+hc.getAltitude());
		long currentTimeMillis = gc.getTimeInMillis();
		return (objectSets - currentTimeMillis)/1000;
	}
	
}
