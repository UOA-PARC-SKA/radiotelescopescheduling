package ext;


import java.util.GregorianCalendar;

import astrometrics.HorizonCoordinates;
import observation.Telescope;
import observation.interference.Satellite;

public class NoradLink 
{
	static {
	      System.loadLibrary("norad"); // Load native library at runtime
	                                   // norad.dll (Windows) or libnorad.so (Unixes)
	   }
	
	public HorizonCoordinates getSatelliteHC(Satellite satellite, Telescope telescope, GregorianCalendar gc)
	{
			long seconds =gc.getTimeInMillis() /1000;
			HorizonCoordinates hc = this.getSatHorizonCoordinates(satellite, seconds, telescope);
			return hc;
	}
	
	// the native Method adds Orbit objects to the satellites
	private native HorizonCoordinates getSatHorizonCoordinates(Satellite sat, long time, Telescope telescope);

	public double[] getDistancesForInterval(Satellite sat, Telescope scope, int intervalInSeconds, int howMany) 
	{
		double[] speeds = new double[howMany];
		//where we start makes no difference, just for calculating speed
		GregorianCalendar gc = new GregorianCalendar();
		HorizonCoordinates before = this.getSatelliteHC(sat, scope, gc);
		HorizonCoordinates after;
		for (int i = 0; i < howMany; i++) {
			gc.add(GregorianCalendar.SECOND, intervalInSeconds);
			after = this.getSatelliteHC(sat, scope, gc);
			speeds[i] = before.calculateAngularDistanceTo(after);
			before = after;
			//System.out.println(sat.getName()+" speed "+speeds[i]);
		}
		return speeds;
	}
	
	
	

}
