package observation;

import java.util.GregorianCalendar;

import simulation.Clock;
import util.Utilities;


public class Pulsar implements Observable
{
	private String pulsarName;
	private int expectedIntegrationTime;
	//this is the actual observation time, lengthened or shortened by the strength of the signal
	private double actualObservationTime;
	//private boolean observationComplete;
	private int scintTimescale;
	private GregorianCalendar doNotLookUntil;
	private int attempts;
	//this is the time to compare when we need the remaining observation time
	private double nominalObservationTime;
	
	
	public Pulsar(String name)
	{
		this.pulsarName = name;
	}

	public String getName() {
		return pulsarName;
	}

	public String toString()
	{
		return this.pulsarName ;
	}

	public String getPulsarName() {
		return pulsarName;
	}

	public void setPulsarName(String pulsarName) {
		this.pulsarName = pulsarName;
	}

	public void setExpectedIntegrationTime(double integrationTime) {
		this.expectedIntegrationTime = (int) integrationTime;
	}
	
	//this means it is in the group that was scheduled or is yet to be scheduled
	//(distinguishing it from objects that don't need observing in this term
	public boolean needsObserving()
	{
		return (expectedIntegrationTime > 0);		
	}

	@Override
	public int getExpectedIntegrationTime() {
		return expectedIntegrationTime;
	}

	@Override
	public double getActualTimeObserved() {
		
		return actualObservationTime;
	}

	
	public void setActualTimeObserved(double time) {
		this.actualObservationTime = time;
	}


	public boolean isObservationComplete() 
	{
		return (expectedIntegrationTime <= nominalObservationTime);
	}

//	public void setObservationComplete(boolean observationComplete) {
//		this.observationComplete = observationComplete;
//	}

	@Override
	public int getScintillationTimescale() 
	{
		return this.scintTimescale;
	}
	


	@Override
	public double getRemainingIntegrationTime() {
		
		return this.expectedIntegrationTime - this.nominalObservationTime;
	}

	@Override
	public void addActualTimeObserved(double elapsed) 
	{
		this.actualObservationTime += elapsed;
		
	}
	
	@Override
	public void addNominalTimeObserved(double elapsed) 
	{
		this.nominalObservationTime += elapsed;
		
	}


	public boolean doNotLookYet(GregorianCalendar other) {
		
		if(other.before(doNotLookUntil))
		{
//			if (doNotLookUntil != null)
//			{
//				System.out.println("Too close "+Utilities.getDateAsString(doNotLookUntil.getTime()));
//				System.out.println("Current time: "+Utilities.getDateAsString(Clock.getScheduleClock().getTime().getTime()));
//			}
			return true;
		}
		return false;
	}

	//This is the current time plus the polling interval
	@Override
	public void setDontLookTime() {
		this.doNotLookUntil = new GregorianCalendar();
		this.doNotLookUntil.setTimeInMillis(Clock.getScheduleClock().getTime().getTimeInMillis() + (scintTimescale * 1000));
		
	}
	@Override
	public void setDontLookTime(long seconds) 
	{
		this.doNotLookUntil = new GregorianCalendar();
		this.doNotLookUntil.setTimeInMillis(Clock.getScheduleClock().getTime().getTimeInMillis() 
				+(seconds * 1000));
	}

	public int getAttempts() {
		return attempts;
	}
	
	public void incrementAttempts()
	{
		attempts++;
	}

	@Override
	public double getNominalTimeObserved() {
		
		return nominalObservationTime;
	}

	@Override
	public void setScintillationTimescale(int scintillationTimescale) {
		this.scintTimescale = scintillationTimescale;
		
	}

	
}
