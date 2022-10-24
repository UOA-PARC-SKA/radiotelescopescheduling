package observation;


import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import simulation.Clock;
import util.Utilities;
import util.exceptions.WrongTypeException;
import astrometrics.EquatorialCoordinates;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;


public class Target implements Pointable
{
	private EquatorialCoordinates eCoordinates;
	private List<Observable> observables;
	private List<Connection> neighbours; 
	private long id;
	private HorizonCoordinates horizonCoordinates;
	private GregorianCalendar dateOfHCs = null;
	
	
	
	public Target (EquatorialCoordinates ec)
	{
		this.eCoordinates = ec;
		this.observables = new ArrayList<Observable>();
		neighbours = new ArrayList<Connection>();

	}
	
	public String getName()
	{
		if(observables.size() == 1)
			return observables.get(0).getName();
		if(observables.size() == 0)
			return "None";
		return "Several";
	}
	@Override
	public double angularDistanceTo(Pointable p, Location loc, GregorianCalendar gc) throws WrongTypeException
	{
		if(p instanceof Position)
			throw new WrongTypeException();
		Target t2 = (Target)p;
		return this.eCoordinates.calculateAngularDistanceTo(t2.eCoordinates);
	}
	
	public void addObservable(Observable o)
	{
		observables.add(o);	
	}
	
	public void addNeighbour(Connection c)
	{
		neighbours.add(c);	
	}
	
	public List<Connection> getNeighbours()
	{
		List<Connection> list = new ArrayList<Connection>();
		list.addAll(neighbours);
		return list;
	}

	public boolean hasLinkToTarget(Target target1)
	{
		for (Connection connection : neighbours)
		{
			if(connection.getOtherTarget(this) == target1)
				return true;
		}
		return false;
	}
	
	public Connection getConnectionTo(Target t)
	{
		for (Connection connection : neighbours)
		{
			if(connection.getOtherTarget(this) == t)
				return connection;
		}
		return null;
	}
	
	public boolean hasNeighbours()
	{
		return !neighbours.isEmpty();
	}
	
	public EquatorialCoordinates getEquatorialCoordinates() {
		return eCoordinates;
	}
	
	public HorizonCoordinates getHorizonCoordinates(Location loc, GregorianCalendar gc)
	{	
		if(dateOfHCs == null || !gc.equals(dateOfHCs)) // coords are stale
		{
			dateOfHCs = Utilities.cloneDate(gc);
			horizonCoordinates = HorizonCoordinates.getHorizonCoordinates(eCoordinates, loc, gc);
		}
		
		return horizonCoordinates;
	}
	public void removeConnection(Connection connection) {
		this.neighbours.remove(connection);
		
	}
	
	public List<Observable> getObservables()
	{
		return observables;
	}
	
	public Connection getConnectionForTarget(Target t)
	{
		for (Connection connection : neighbours) 
		{
			if(connection.hasTarget(t))
				return connection;
		}
		return null;
	}

	public boolean hasLocation(EquatorialCoordinates ec) {
		if(ec.getDeclination() == eCoordinates.getDeclination() && ec.getRightAscension() == eCoordinates.getRightAscension())
			return true;
		return false;
	}
	
	public String toString()
	{
		return this.observables.toString();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	//This gets set when the observations are entered at the beginning of
	// the season. It doesn't change when the observation is complete, only when 
	// it leaves the system at the end of the period.
	public boolean needsObserving()
	{
		for (Observable observable : observables) 
		{
			//at the moment, any member object that needs observing means we have
			//to go to this place
			if(observable.needsObserving())
				return true;
		}
		return false;
	}
	
	//This means the object has been sufficiently observed even though
	//its status is still needs observing.
	public boolean hasCompleteObservation()
	{
		for (Observable observable : observables) 
		{
			//at the moment, any member object that needs observing means we have
			//to go to this place
			if(!observable.isObservationComplete())
				return false;
		}
		return true;
	}



	@Override
	public void setHorizonCoordinates(HorizonCoordinates coords) {
		// Does nothing by design, HC aren't settable
		// because they change with time
		
	}

	@Override
	public void clearNeighbours() {
		this.neighbours.clear();
		
	}



	//within scintillation timescale of previous observation
	public boolean tooCloseToPreviousObservation(Clock clock)
	{
		for (Observable observable : observables) 
		{
			//means we have just been there and scintillation wasn't good
			if(observable.doNotLookYet(clock.getTime()))
				return true;
		}
		return false;
	}

	@Override
	public boolean hasObservable() {
		//always true for targets
		return true;
	}
	
	//problem of not picking up other observables if they have to be observed
	public Observable findObservableByObservationTime()
	{
		double maxObs = 0;
		Observable toObserve = null;
		for (Observable  o : observables) 
		{
			if(maxObs < o.getRemainingIntegrationTime())
			{
				maxObs = o.getRemainingIntegrationTime();
				toObserve = o;
			}
		}
		//if(maxObs==0) return null;
		return toObserve;
	}
}
