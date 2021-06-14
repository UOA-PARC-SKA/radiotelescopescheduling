package evaluation;


import java.util.List;

import observation.Connection;
import observation.Schedule;
import observation.live.ObservationState;


public class Results 
{
	private Schedule schedule;


	public Schedule getSchedule() {
		return schedule;
	}


	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}
	
	public double getTotalSlewTime()
	{
		double slewTime = 0;
		List<Connection> connections = schedule.getFinalPath();
		for (Connection connection : connections) 
		{
			slewTime += connection.getFinalSlewTime();
		}	
		return slewTime;
	}
	
	public double getRelativeSlewTime(double slewTime)
	{
		return slewTime/schedule.getScheduleLength();
	}

	public double getTotalObservationTime()
	{
		double observationTime = 0;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			observationTime += state.getIntegrationTime();
			
		}
		return observationTime;
	}
	
	public long getTotalWaitingTime() {
		long waitingTime = 0;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			waitingTime += state.getWaitingTime();
			
		}
		return waitingTime;	
	}
	
	public double getRelativeObservationTime(double observationTime)
	{
		return observationTime/schedule.getScheduleLength();
	}


	public double getRelativeWaitingTime(double waitingTime) {
		
		return waitingTime/schedule.getScheduleLength();
	}

	//this returns the largest number of attempts needed to complete an observation
	public int getMaxNoAttempts()
	{
		int maxAttempts = 0;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			if(maxAttempts < state.getHowManiethAttempt())
				maxAttempts = state.getHowManiethAttempt();
			
		}
		return maxAttempts;
	}
	//how many times was the maximum attempts needed
	public int getTimesMaxAttempt(int maxAttempt)
	{
		int counter = 0;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			if(maxAttempt == state.getHowManiethAttempt())
				counter++;
			
		}
		return counter;
	}


	public int[] getOutcomesHistogram() 
	{
		int[] outcomes = new int[5];
		int obsResult;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			obsResult = state.getObsResult();
			if(obsResult >= 0)
				outcomes[obsResult]++;
			
		}
		return outcomes;
	}
	
	public int[] getInterruptionsHistogram() 
	{
		int[] outcomes = new int[3];
		int result ;
		for (ObservationState state : schedule.getScheduleStates()) 
		{
			result = state.getInterruptionResult();
			if(result >= 0)
				outcomes[result]++;
			
		}
		return outcomes;
	}
}
