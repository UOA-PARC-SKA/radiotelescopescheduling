package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class Utilities 
{
	//public static final Random random = new Random(2222);
	public static final Random random = new Random(System.currentTimeMillis());
	// array of second intervals to choose from uniformly randomly for the scintillation timescale
	private static final int[] timescales = {20, 60, 180, 600, 1800, 5400};

	public static double getRayleighRandom()
	{
		double x = random.nextGaussian();
		double y = random.nextGaussian();

		return Math.sqrt((Math.pow(x, 2)+ Math.pow(y, 2)));
	}

	public static int getScintillationTimescale()
	{
		return timescales[random.nextInt(timescales.length)];
	}
	public static String getDateAsString(long l)
	{
		return getDateAsString(new Date(l));
	}
	
	public static String getDateAsString(Date d)
	{
		String dateString = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(d);
		return dateString;
	}

	public static String getDateAsShortString(Date d)
	{
		String dateString = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(d);
		return dateString;
	}
	
	public static String millisecondsToDateFormat(long milliseconds)
	{

	    long diffInSeconds = milliseconds / 1000;

	    long diff[] = new long[] { 0, 0, 0, 0 };
	    /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    /* days */diff[0] = (diffInSeconds = (diffInSeconds / 24));

	   String s = String.format(
	        "%d day%s, %d hour%s, %d minute%s, %d second%s",
	        diff[0],
	        diff[0] > 1 ? "s" : "",
	        diff[1],
	        diff[1] > 1 ? "s" : "",
	        diff[2],
	        diff[2] > 1 ? "s" : "",
	        diff[3],
	        diff[3] > 1 ? "s" : "");
	   //System.out.println(s);
	   return s;
	}
	
	public static double millisecondsToHours(long milliseconds)
	{
		double hours = 0;
	    long diffInSeconds = milliseconds / 1000;

	    long diff[] = new long[] { 0, 0, 0, 0 };
	    /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    /* days */diff[0] = (diffInSeconds = (diffInSeconds / 24));

	    hours += diff[0] * 24.0;
	    hours += diff[1] ;
	    hours += diff[2] / 60.0;
	    hours += diff[3] / 3600.0;
	    return hours;
	}
	
	public static double getMean(double[] d)
	{
		double avg = 0;
		for (int i = 0; i < d.length; i++) {
			avg += d[i];
		}
		return avg/d.length;
	}
	
	public static double getStdDev(double[] d, double mean)
	{
		double stDev = 0;
		for (int i = 0; i < d.length; i++) {
			stDev += Math.abs(mean - d[i]);
		}
		return stDev/d.length;
	}
	

	public static GregorianCalendar cloneDate(GregorianCalendar gc1)
	{
		GregorianCalendar gc2 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		gc2.set(gc1.get(GregorianCalendar.YEAR), 
				gc1.get(GregorianCalendar.MONTH), 
				gc1.get(GregorianCalendar.DAY_OF_MONTH), 
				gc1.get(GregorianCalendar.HOUR_OF_DAY), 
				gc1.get(GregorianCalendar.MINUTE), 
				gc1.get(GregorianCalendar.SECOND));
				
		return gc2;			
	}
	
	public static Date stringToDate(String dateString)
	{
				
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = sdf.parse(dateString);
			return date;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
	
	public static GregorianCalendar getDateInFuture()
	{
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		gc.set(2099, 12, 31);
		System.out.println();
		return gc;
	}

}
