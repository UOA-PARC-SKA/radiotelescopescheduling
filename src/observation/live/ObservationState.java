package observation.live;


import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import astrometrics.HorizonCoordinates;
import astrometrics.Location;
import observation.Connection;
import observation.Observable;
import observation.Pointable;
import observation.Position;
import observation.interference.Satellite;
import simulation.Clock;



public class ObservationState 
{
	public static final int OBSERVATION_NONE = 0;
	public static final int OBSERVATION_COMPLETE_SCINT_MAX = 1;
	public static final int OBSERVATION_COMPLETE_SCINT_NORMAL = 2;
	public static final int OBSERVATION_COMPLETE_SCINT_REPEAT = 3;
	public static final int OBSERVATION_ABORTED_SCINT_WEAK = 4;
	
	public static final int OBSERVATION_INTERRUPTION_NONE = 0;
	public static final int OBSERVATION_ABORTED_OBJECT_SET = 1;
	public static final int OBSERVATION_ABORTED_SATELLITE = 2;
	public static final int OBSERVATION_ABORTED_PLANET = 3;

	private Pointable currentTarget = null;
	private Connection linkToHere = null;
	private Observable currentObservable = null;
	//if the observable has been observed before, there is a difference; hence own variable
	private double integrationTime;
	private long startTime;
	private long endTime;
	private HorizonCoordinates startHC;
	private HorizonCoordinates endHC;
	private boolean aborted;
	//waiting for new observables after the current integration is finished
	private long waitingTime; 
	private String comment = "";
	private int howManiethAttempt ;
	private List<Satellite> potentiallyInterfering = null;
	private Satellite collisionSat = null;
	private int obsResult = -1;
	private int interruptionResult = -1;
	
	//State always needs a target, first link may not be available yet
	public ObservationState (Pointable target, GregorianCalendar gc, Connection link, Observable o, Location loc)
	{
		this(target, gc.getTimeInMillis(), null, null);
		startHC = target.getHorizonCoordinates(loc, gc);
		this.linkToHere = link;
		currentObservable = o;
		currentObservable.incrementAttempts();
		howManiethAttempt = o.getAttempts();
	}

	public ObservationState (Pointable target, GregorianCalendar gc, Connection link, Observable o, Location loc, long offset)
	{
		this(target, gc.getTimeInMillis()-offset, null, null);
		startHC = target.getHorizonCoordinates(loc, gc);
		this.linkToHere = link;
		currentObservable = o;
		currentObservable.incrementAttempts();
		howManiethAttempt = o.getAttempts();
	}
	
	public ObservationState(Pointable position, long timeInMillis, Object object, Object object2) {
		currentTarget = position;
		integrationTime = 0;
		//need to account for slew time to here
		startTime = timeInMillis;
		aborted = false;
		waitingTime = 0;
		this.potentiallyInterfering = new ArrayList<Satellite>();
	}

	public Pointable getCurrentTarget() 
	{
		return currentTarget;
	}
	public void setCurrentTarget(Pointable currentTarget) 
	{
		this.currentTarget = currentTarget;
	}
	public Connection getLinkToHere() 
	{
		return linkToHere;
	}
	

	public Observable getCurrentObservable() {
		return currentObservable;
	}


	//this could still be the second (or later) attempt
//	public void setObservationCompletedInstantly( String comment, double observationTime)
//	{
//		this.comment = comment;
//
//		this.integrationTime =observationTime;
//		currentObservable.addTimeObserved(observationTime);
//		currentObservable.incrementAttempts();
//		currentObservable.setObservationComplete(true);
//	}
	
	//the obs was done in intervals; any shortened periods have already been added
	public void setObservationCompleted(String comment, double nominalTime) 
	{
		this.comment = comment;
		currentObservable.addNominalTimeObserved(nominalTime);
		currentObservable.addActualTimeObserved(integrationTime);	
	}
	
//	public void setInterruptedBySatellite(double nominalTimeObserved, GregorianCalendar exactTime, Satellite satellite, double timeInSeconds)
//	{
//		this.setAborted("Interfering satellite "+satellite.getName()+" at "
//				+ Utilities.getDateAsString(exactTime.getTime().getTime()), nominalTimeObserved);
//		this.collisionSat = satellite;
//		this.addIntegrationTime(timeInSeconds);
//		Clock.getScheduleClock().advanceBy((int)timeInSeconds);
//		System.out.println("Compare "+Utilities.getDateAsString(exactTime.getTimeInMillis())+" with "
//		+Utilities.getDateAsString(Clock.getScheduleClock().getTime().getTimeInMillis()));
//	}
	//Add to the integration time
//	public void addIntegrationTime(double elapsed)
//	{
//		integrationTime += elapsed;
//	}
	
	public void setIntegrationTime(double elapsed)
	{
		integrationTime += elapsed;
	}
	
	public void addAdjustedIntegrationTime(double elapsed, double rayleigh)
	{
		integrationTime += elapsed / rayleigh;
	}
	
	//any integration time must be added to the observationTime
	public void setAborted(String comment, double nominalTime)
	{
		this.comment += comment;
		this.currentObservable.addNominalTimeObserved(nominalTime);
		this.currentObservable.addActualTimeObserved(this.integrationTime);
		aborted = true;
	}
	
	public boolean wasAborted()
	{

		return aborted;
	}

	public String getComment() {
		return comment;
	}


	public long getStartTime() {
		return startTime;
	}

	public void addWaitTime(int waitingPeriod) {
		waitingTime += waitingPeriod;
	}

	public long getWaitingTime()
	{
		return waitingTime;
	}

	public void addComment(String string) {
		comment += string +" ";
		
	}

	//this is really a debug string, and a sloppy one
	public String toString()
	{
		String str = "";
		if(linkToHere != null)
		{
		//it has come form other
			Pointable other = this.linkToHere.getOtherTarget(this.currentTarget);
			if (other instanceof Position)
				str = "No target ";
			else
			{
				str = other.toString();
			}
		}
		str+= " to "+currentTarget.toString();
		return str;
	}

	public double getIntegrationTime() {
		return integrationTime;
	}

	public int getHowManiethAttempt() {
		return howManiethAttempt;
	}
	
	public void setHowManiethAttempt(){
		
	}

	public List<Satellite> getPotentiallyInterfering() {
		return potentiallyInterfering;
	}

	//need to make deep copy.
	public void setPotentiallyInterfering(List<Satellite> potentiallyInterfering) {
		this.potentiallyInterfering.addAll(potentiallyInterfering);
	}

	public Satellite getCollisionSat() {
		return collisionSat;
	}

	public void setCollisionSat(Satellite collisionSat) {
		this.collisionSat = collisionSat;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(Location loc) {
		this.endTime = startTime + 
				(long)(linkToHere.getFinalSlewTime()*1000) + 
				(long) (this.waitingTime*1000)
				+(long) (this.integrationTime * 1000);
		this.setEndHC(loc, Clock.getScheduleClock().getTime());
	}

	public HorizonCoordinates getStartHC() {
		return startHC;
	}



	public HorizonCoordinates getEndHC() {
		return endHC;
	}

	public void setEndHC(Location loc, GregorianCalendar gc ) {
		this.endHC = this.currentTarget.getHorizonCoordinates(loc, gc);
	}

	public void setStartingCoordinates(HorizonCoordinates parkingCoordinates) {
		this.startHC = parkingCoordinates;
		
	}
	
	public void setObservationResults(int observation, int interruption)
	{
		this.obsResult = observation;
		this.interruptionResult = interruption;
	}

	public int getNoSatellites() {
		
		return potentiallyInterfering.size();
	}

	public int getObsResult() {
		return obsResult;
	}

	public int getInterruptionResult() {
		return interruptionResult;
	}
	
}
