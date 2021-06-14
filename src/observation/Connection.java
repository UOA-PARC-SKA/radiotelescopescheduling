package observation;

public class Connection 
{
	private Pointable target1;
	private Target target2;
	private double distance;
	// this depends on time of day and can only be added when the connection is used
	private double finalSlewTime;
	
	public Connection(Pointable t1, Target t2, double d)
	{
		target1 = t1;
		target2 = t2;
		distance = d;
	}
	
	public Connection(double d)
	{
		target1 = null;
		target2 = null;
		distance = d;
	}
	
	public Pointable getOtherTarget(Pointable t)
	{
		if(t == target1)
			return target2;
		return  target1;
	}
	
	public boolean hasTarget(Target t)
	{
		if (t == target1)
			return true;
		if (t == target2)
			return true;
		return false;
	}

	public double getDistance() {
		return distance;
	}
	
	public Pointable getFirst()
	{
		return target1;
	}
	
	public void setTarget1 (Target t)
	{
		this.target1 = t;
	}
	
	public void setTarget2 (Target t)
	{
		this.target2 = t;
	}
	
	public String toString()
	{
		return target1.toString() + " " + target2.toString();
	}

	public double getFinalSlewTime() 
	{
		return finalSlewTime;
	}

	public void setFinalSlewTime(double finalSlewTime) 
	{
		this.finalSlewTime = finalSlewTime;
	}
}
