package observation;



public class Beam 
{
	public static final int BEAMSHAPE_CIRCLE = 0;
	public static final int	BEAMSHAPE_RECTANGLE = 1;
	
	public static final int OVERLAP_NONE = 0;
	public static final int OVERLAP_PERIPHERY = 1;
	public static final int	OVERLAP_CORE = 2;
	
	
	private int beamShape;
	private double radius;
	
	public Beam (int shape)
	{
		beamShape = shape;		
	}
	
	// The target currently pointed at has Eq coordinates
//	public int getOverlap(Target currentTarget, GregorianCalendar gc)
//	{
//		
//	}
	
	
}
