package astrometrics;

public class Location 
{
	//long and lat in radians, elevation in km (for the Norad module)
	private double latitude;
	private double longitude;
	private double elevation; 
	

	
	public Location (double lat, double lon, double el)
	{
		this.latitude = lat;
		this.longitude = lon;
		this.elevation = el;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	
	public double getLatitudeInDegrees() {
		return Math.toDegrees(latitude);
	}

	public double getLongitudeInDegrees() {
		return Math.toDegrees(longitude);
	}

	public double getElevation() {
		return elevation;
	}
	
	
}
