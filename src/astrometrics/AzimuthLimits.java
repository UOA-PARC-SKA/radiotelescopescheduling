package astrometrics;

public 	class AzimuthLimits
{
	private double maxAzimuth;
	private double minAzimuth;
	
	public AzimuthLimits(double max, double min)
	{
		maxAzimuth = max;
		minAzimuth = min;
	}

	public double getMaxAzimuth() {
		return maxAzimuth;
	}

	public double getMinAzimuth() {
		return minAzimuth;
	}
}
