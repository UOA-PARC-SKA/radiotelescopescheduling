package observation;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import astrometrics.HorizonCoordinates;
import astrometrics.Location;

// A position without any observable target
public class Position implements Pointable
{
	private HorizonCoordinates hc;
	private List<Connection> neighbours; 
	
	public Position(HorizonCoordinates hc)
	{
		this.hc = hc;
		neighbours = new ArrayList<Connection>();
	}
	
	public void setHorizonCoordinates(HorizonCoordinates coords)
	{
		hc = coords;
	}
	
	public HorizonCoordinates getHorizonCoordinates()
	{
		return hc;
	}

	@Override
	public boolean hasNeighbours() 
	{
		if(neighbours.size() > 0)
			return true;
		return false;
	}

	@Override
	public List<Connection> getNeighbours() 
	{
		return neighbours;
	}

	@Override
	public double angularDistanceTo(Pointable p, Location loc, GregorianCalendar gc) 
	{

		return this.hc.calculateAngularDistanceTo(p.getHorizonCoordinates(loc, gc));
		
	}

	@Override
	public void addNeighbour(Connection c) 
	{
		neighbours.add(c);
	}

	@Override
	public boolean hasLinkToTarget(Target candidate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HorizonCoordinates getHorizonCoordinates(Location loc,
			GregorianCalendar gc) {
		
		return this.hc;
	}

	@Override
	public void clearNeighbours() {
		this.neighbours.clear();
		
	}

	@Override
	public boolean hasObservable() {
		//always false for Position objects
		return false;
	}

	@Override
	public boolean hasCompleteObservation() {
		//always true for Position objects
		return true;
	}



	

	
}
