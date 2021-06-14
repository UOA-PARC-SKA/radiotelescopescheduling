package astrometrics;

public class EquatorialCoordinates
{
	
	private double rightAscension;
	private double declination;


	public EquatorialCoordinates(double ra, double dec)
	{
		this.rightAscension = ra;
		this.declination = dec;
	}


	public double getRightAscension() 
	{
		return rightAscension;
	}


	public double getDeclination() 
	{
		return declination;
	}
	
	public String toString()
	{
		return "RA "+rightAscension+" dec "+declination;
	}

	public double calculateAngularDistanceTo(EquatorialCoordinates other)
	{
		double theta = Math.acos(Math.sin(this.declination) 
				* Math.sin(other.declination) 
				+ Math.cos(this.declination) 
				* Math.cos(other.declination) 
				* Math.cos (this.rightAscension
				- other.rightAscension));
		return theta;
	}
	
}
