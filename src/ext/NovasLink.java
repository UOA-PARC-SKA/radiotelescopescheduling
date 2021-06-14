package ext;

import java.util.Date;

import astrometrics.HorizonCoordinates;
import observation.Telescope;


public class NovasLink 
{
	static {
	      System.loadLibrary("novas"); // Load native library at runtime
	                                   // norad.dll (Windows) or libnorad.so (Unixes)
	   }
	
	private native HorizonCoordinates getCBHorizonCoordinates(long time, int cbNovasID, Telescope telescope);
	
//	private native double getCBRadius(long time, int cbNovasID, Telescope telescope);

	
	public HorizonCoordinates getCelestialBodiesHC(Date date, int cbID, Telescope telescope)
	{
			long seconds =date.getTime()/1000;
			HorizonCoordinates hc = this.getCBHorizonCoordinates(seconds, cbID, telescope);
			return hc;
	}
	
//	public double getCBRadius(Date date, int cbID, Telescope telescope)
//	{
//			long seconds =date.getTime()/1000;
//			double d = this.getCBRadius(seconds, cbID, telescope);
//			System.out.println("Celest bodies id "+cbID);
//			return d;
//	}
	
	
}
