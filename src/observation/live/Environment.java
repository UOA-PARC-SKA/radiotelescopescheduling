package observation.live;

import java.util.GregorianCalendar;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;
import observation.Pointable;
import observation.Target;
import observation.Telescope;
import observation.interference.SkyState;
import simulation.Clock;
import util.Utilities;

public class Environment 
{
	public static final int SCINTILLATION_RANGE_LOW = 0;
	public static final int SCINTILLATION_RANGE_MEDIUM = 1;
	public static final int SCINTILLATION_RANGE_HIGH = 2;
	
	//max and min for the numbers form the Rayleigh distribution
	private double maxScintillations;
	private double minScintillations;
	//skyState has all the bad things that move
	private SkyState skyState;

	
	
	public Environment(double max, double min, SkyState state)
	{
		maxScintillations = max;
		minScintillations = min;
		skyState = state;
				
	}
	
	public int getScintillationRange(double rayleigh)
	{
		if(rayleigh > maxScintillations)
			return SCINTILLATION_RANGE_HIGH;
		if (rayleigh > minScintillations)
			return SCINTILLATION_RANGE_MEDIUM;
				
		return SCINTILLATION_RANGE_LOW;
	}
	
	public double getScintillation()
	{
		double rayleigh = Utilities.getRayleighRandom();
	//	System.out.println("Rayleigh "+rayleigh);
		return rayleigh;
	}
	

	
	public boolean isAboveHorizon(Pointable pointable, Telescope telescope, GregorianCalendar gc)
	{
		HorizonCoordinates targetHc = pointable.getHorizonCoordinates(telescope.getLocation(), gc);
		if(targetHc.getAltitude() > 0)
			return true;
		return false;
	}
	
//	public List<Satellite> getSatellitesThatMightInterfere(ObservationState state, Telescope telescope, GregorianCalendar gc)
//	{
//		potentiallyInterfering = skyState.getSatellitesThatMightInterfere(state, telescope, gc);
//		return potentiallyInterfering;
//	}


	
	/*
	 * We assume that if the scintillation time interval is short, we have to re-check 
	 * scintillation every timescale interval.
	 * In the meantime, a satellite can break the observation. 
	 * The object can set.
	 * The actually needed observation time might be longer depending on the scintillation.

	 * There is an overlay between scintillation interval (the time to adjust the scintillation) and checking 
	 * for collisions with satellites and the object setting. 
	 * We assume that the scintillation will change after every interval, and depending on the current scintillation,
	 * the observation for that interval is shortened or lengthened.
	 * So we subdivide the nominal time into intervals. 
	 * Depending on the scintillation, the actual time needed for this interval is shorter or longer. 
	 * If there are no interruptions, the nominal time is the remaining time and the actual time is the
	 * sum of the intervals.
	 * If a satellite comes in during that period, the actual time has to be truncated and the nominal time has to
	 * be reduced in the same proportion. 
	 */
	protected void checkScintillationRepeatedly(ObservationState state, Telescope scope)
	{
		int currentScintTimescale = state.getCurrentObservable().getScintillationTimescale();
		double nominalTime = state.getCurrentObservable().getRemainingIntegrationTime();
		if(nominalTime==0) System.out.println(nominalTime);
		int outcome = ObservationState.OBSERVATION_INTERRUPTION_NONE;
	//	System.out.println("Nominal "+nominalTime + " remaining "+state.getCurrentObservable().getRemainingIntegrationTime());
		int noTimescales = (int) Math.ceil( nominalTime/currentScintTimescale);
		//the last interval will likely be shorter than currentScintTimescale
		double remainder = nominalTime - (currentScintTimescale * (noTimescales-1));
		double[] scintIntervals = new double[noTimescales];
		double totalActualTime = 0;
		for (int i = 0; i < noTimescales-1; i++) 
		{
			scintIntervals[i] = currentScintTimescale / this.getScintillation();
			totalActualTime += scintIntervals[i];
		}
		scintIntervals[noTimescales-1] = remainder / this.getScintillation();
		totalActualTime += scintIntervals[noTimescales-1];
		totalActualTime = Math.floor(totalActualTime);//remove decimals
		long obsTime = (long) totalActualTime;

		//This returns the time until which the observation lasted; if the full time, it was successful
		obsTime = skyState.willCollideWithSatelliteAt(Clock.getScheduleClock().getTime(), state, scope, obsTime);
		
		if(obsTime < totalActualTime)
		{
			totalActualTime = obsTime;
			outcome = ObservationState.OBSERVATION_ABORTED_SATELLITE;
		}
		
		obsTime = (long)getSecondsBeforeObjectSets(scope.getLocation(), state, Clock.getScheduleClock().getTime());
		
		if(obsTime < totalActualTime)
		{
			totalActualTime = obsTime;
			outcome = ObservationState.OBSERVATION_ABORTED_OBJECT_SET;
		}
		state.setIntegrationTime(totalActualTime);
		
		
		//find the equivalent nominal time
		if(outcome !=  ObservationState.OBSERVATION_INTERRUPTION_NONE)
		{
			nominalTime = 0;
			int interruptedIntervalNo = -1;
			double runningActualTotal = 0;
			double lastProportion = 0;
			for (int i = 0; i < scintIntervals.length; i++) 
			{
				runningActualTotal += scintIntervals[i];
				nominalTime += currentScintTimescale;
				if(totalActualTime < runningActualTotal)
				{
					interruptedIntervalNo = i;
					//the last interval is likely incomplete
					runningActualTotal -= scintIntervals[i];
					nominalTime -= currentScintTimescale;
					//get the proportion that exceeds the previous interval
					lastProportion = totalActualTime - runningActualTotal;
					lastProportion /= scintIntervals[i];
					break;
				}
			}
			
			if(interruptedIntervalNo < noTimescales - 1)
				nominalTime += lastProportion * currentScintTimescale;
			else
				nominalTime += lastProportion * remainder;
			
		}
		
		switch (outcome) {
		case ObservationState.OBSERVATION_INTERRUPTION_NONE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_REPEAT, ObservationState.OBSERVATION_INTERRUPTION_NONE);
			state.setObservationCompleted("Completed incrementally checking scint every "+currentScintTimescale+" seconds.", nominalTime);
			break;

		case ObservationState.OBSERVATION_ABORTED_OBJECT_SET:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_REPEAT, ObservationState.OBSERVATION_ABORTED_OBJECT_SET);
			state.setAborted("Aborted: Checked scintillation repeatedly; observed until object set. ", nominalTime);

			break;
			
		case ObservationState.OBSERVATION_ABORTED_SATELLITE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_REPEAT, ObservationState.OBSERVATION_ABORTED_SATELLITE);
			state.setAborted("Aborted: Checked scintillation repeatedly; interrupted by satellite "+state.getCollisionSat().getName(), nominalTime);
			
			break;
		}

		Clock.getScheduleClock().advanceBy((int)totalActualTime);
//		Target t = (Target)state.getCurrentTarget();
//		System.out.println("Repeated Scintillation altitude after obs "+t.getHorizonCoordinates(scope.getLocation(), Clock.getScheduleClock().getTime()).getAltitude());

	}

	

	
	/*
	 * Scintillation is normal, observation can proceed at nominal speed, if object sets, maxTime is the time remaining until object sets. 
	 */
	
	protected void applyConditions(ObservationState state, Telescope scope)
	{
		double time = state.getCurrentObservable().getRemainingIntegrationTime();
		
		int outcome = ObservationState.OBSERVATION_INTERRUPTION_NONE;
		
		double maxTime = getSecondsBeforeObjectSets(scope.getLocation(), state, Clock.getScheduleClock().getTime());
		
		if(time > maxTime)
		{
			outcome = ObservationState.OBSERVATION_ABORTED_OBJECT_SET;
			time =  maxTime;
		}
		long obsTime = (long) time;
	
		obsTime = skyState.willCollideWithSatelliteAt(Clock.getScheduleClock().getTime(), state, scope, obsTime);
		if(Math.floor(time) > obsTime)
		{
			outcome = ObservationState.OBSERVATION_ABORTED_SATELLITE;
			time =  obsTime;
		}		
		
		state.setIntegrationTime(time);
		
		switch (outcome) {
		case ObservationState.OBSERVATION_INTERRUPTION_NONE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_NORMAL, ObservationState.OBSERVATION_INTERRUPTION_NONE);
			state.setObservationCompleted("Completed: Nominal time; scintillation good.", time);
			break;

		case ObservationState.OBSERVATION_ABORTED_OBJECT_SET:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_NORMAL, ObservationState.OBSERVATION_ABORTED_OBJECT_SET);
			state.setAborted("Aborted: Normal scintillation; observed until object set.", time);

			break;
			
		case ObservationState.OBSERVATION_ABORTED_SATELLITE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_NORMAL, ObservationState.OBSERVATION_ABORTED_SATELLITE);
			state.setAborted("Aborted: Normal scintillation; interrupted by satellite "+state.getCollisionSat().getName(), time);
			break;
		}
		

		Clock.getScheduleClock().advanceBy((int)time);
//		Target t = (Target)state.getCurrentTarget();
//		System.out.println("Normal Scintillation altitude after obs "+t.getHorizonCoordinates(scope.getLocation(), Clock.getScheduleClock().getTime()).getAltitude());
	}
	/*
	 * Scintillation is high, observation can end before time, object does not set
	 * during observation. 
	 */
	public void applyConditionsHighScint(ObservationState state, Telescope scope, double rayleigh) 
	{

		double nominalTime = state.getCurrentObservable().getRemainingIntegrationTime();
		double actualTime = nominalTime / rayleigh;
		int outcome = ObservationState.OBSERVATION_INTERRUPTION_NONE;
		
		double maxTime = getSecondsBeforeObjectSets(scope.getLocation(), state, Clock.getScheduleClock().getTime());
		
		if(actualTime > maxTime)
		{
			outcome = ObservationState.OBSERVATION_ABORTED_OBJECT_SET;
			actualTime =  maxTime;
		}
		long obsTime = (long) actualTime;
		
		obsTime = skyState.willCollideWithSatelliteAt(Clock.getScheduleClock().getTime(), state, scope, obsTime);
		if(Math.floor(actualTime) > obsTime)
		{
			outcome = ObservationState.OBSERVATION_ABORTED_SATELLITE;
			actualTime =  obsTime;
		}		
		
		
		nominalTime = actualTime * rayleigh;
		nominalTime = Math.round(nominalTime);
		actualTime = Math.round(actualTime);
		state.setIntegrationTime(actualTime);
		
		switch (outcome) {
		case ObservationState.OBSERVATION_INTERRUPTION_NONE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_MAX, ObservationState.OBSERVATION_INTERRUPTION_NONE);
			state.setObservationCompleted("Completed: Shortened interval; scintillation of "+rayleigh, nominalTime);
			break;

		case ObservationState.OBSERVATION_ABORTED_OBJECT_SET:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_MAX, ObservationState.OBSERVATION_ABORTED_OBJECT_SET);
			state.setAborted("Aborted: High scintillation; observed until object set.", nominalTime);

			break;
			
		case ObservationState.OBSERVATION_ABORTED_SATELLITE:
			state.setObservationResults(ObservationState.OBSERVATION_COMPLETE_SCINT_MAX, ObservationState.OBSERVATION_ABORTED_SATELLITE);
			state.setAborted("Aborted: High scintillation; interrupted by satellite "+state.getCollisionSat().getName(), nominalTime);
			
			break;
		}


		Clock.getScheduleClock().advanceBy((int)actualTime);
//		Target t = (Target)state.getCurrentTarget();
//		System.out.println("High Scintillation altitude after obs "+t.getHorizonCoordinates(scope.getLocation(), Clock.getScheduleClock().getTime()).getAltitude());
	}



	

//	private long willCollideWithSatelliteAt(GregorianCalendar currentTime, ObservationState state, Telescope scope, long totalObsTime)
//	{
//		boolean wasInterrupted = false;
//		double dist;
//		long timeToCoverDist = 0;
//		
//		GregorianCalendar endDate = Utilities.cloneDate(currentTime); 
//		endDate.add(GregorianCalendar.SECOND, (int) totalObsTime);
//		long earliestInterruption = Long.MAX_VALUE;
//		GregorianCalendar tempDate = Utilities.cloneDate(currentTime);  
//		HorizonCoordinates observableHc = state.getCurrentTarget().getHorizonCoordinates(scope.getLocation(), tempDate);
//		for (Satellite satellite : potentiallyInterfering) 
//		{
//			timeToCoverDist = 0;
//			tempDate = Utilities.cloneDate(currentTime); 
//			do{
//				tempDate.add(GregorianCalendar.SECOND, (int)timeToCoverDist);
//				HorizonCoordinates satHc = satellite.getHorizonCoordinates(tempDate);
//				//we've already found an interruption, and the current time examined is later
//				if(tempDate.getTime().getTime() > currentTime.getTimeInMillis()+earliestInterruption)
//					break;
//				dist = satHc.calculateAngularDistanceTo(observableHc);
//				if(dist <= minAngDistToSatellite+(satellite.getStDevSpeed()*2))
//				{	
//					wasInterrupted = true;
//					earliestInterruption = tempDate.getTimeInMillis() - currentTime.getTimeInMillis();
//					earliestInterruption /=1000;
//					//remember which satellite it was
//					state.setCollisionSat(satellite);
//					break;
//				}	
//				timeToCoverDist =  (long) (dist/satellite.getAverageSpeed());
//				
//			}while(tempDate.before(endDate));
//
//		}
//		if(!wasInterrupted)
//			earliestInterruption = totalObsTime;
//		return earliestInterruption;
//	}


	public double getSecondsBeforeObjectSets(Location loc, ObservationState state, GregorianCalendar gc)
	{
		double setTime = Conversions.getTimeUntilObjectSetsInSeconds(loc, ((Target)state.getCurrentTarget()), gc);
		if(setTime < 60)// ridiculously short time, give up.
		{
			setTime = 0;
			state.getCurrentObservable().setDontLookTime(60);
		}
		return setTime;
	}



	

	
}
