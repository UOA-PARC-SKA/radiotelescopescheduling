package observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import observation.live.ObservationState;

public class Schedule 
{
	private List<ObservationState> scheduleStates;
	private ObservationState currentState;
	private List<Connection> finalPath;
	private long startTime;
	private long endTime;
	private boolean complete = false;
	
	
	public Schedule()
	{
		finalPath = Collections.synchronizedList(new ArrayList<Connection>());
		scheduleStates = new ArrayList<ObservationState>();
	}
	
	public void addLink(Connection link, TelescopeState state)
	{
		link.setFinalSlewTime(state.getSlewTime());
		synchronized(finalPath){
			this.finalPath.add(link);
		}
	}

	public List<Connection> getFinalPath() {
		return finalPath;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public void setEndTime(long time) {
		this.endTime = time;
	}

	public long getStartTime() 
	{
		return startTime;
	}

	public long getEndTime() 
	{
		return endTime;
	}
	
	public long getScheduleLength()
	{
		return endTime - startTime;
		
	}
	public void addState(ObservationState state)
	{
		scheduleStates.add(state);
		currentState = state;
	}

	public List<ObservationState> getScheduleStates() 
	{
		return scheduleStates;
	}

	public boolean isComplete() 
	{
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	
	public ObservationState getCurrentState()
	{
		return currentState;
	}
}
