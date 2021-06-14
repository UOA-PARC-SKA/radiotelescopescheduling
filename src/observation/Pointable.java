package observation;

import java.util.GregorianCalendar;
import java.util.List;

import simulation.Clock;
import util.exceptions.WrongTypeException;
import astrometrics.EquatorialCoordinates;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;

// This is something you can point to in the sky
public interface Pointable 
{
	public void setHorizonCoordinates(HorizonCoordinates coords);
	public HorizonCoordinates getHorizonCoordinates(Location loc, GregorianCalendar gc);
	public boolean hasNeighbours();
	public List<Connection> getNeighbours();
	public double angularDistanceTo(Pointable p, Location loc, GregorianCalendar gc) throws WrongTypeException;
	public void addNeighbour(Connection c);
	public boolean hasLinkToTarget(Target candidate);
	public void clearNeighbours();
	public boolean hasObservable();
	public boolean hasCompleteObservation();
	
}
