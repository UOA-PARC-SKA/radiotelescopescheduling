package optimisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import observation.Connection;
import observation.Pointable;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.TelescopeState;
import simulation.Clock;
import util.exceptions.LastEntryException;
import util.exceptions.OutOfObservablesException;


public abstract class Policy 
{
	protected List<Target> remaining;
	protected List<Target> observables;
	//how long do we wait until checking for observables coming from behind the horizon
	protected int waitTime;
	protected Telescope telescope;
	protected TelescopeState currentTelescopeState = null;
	protected Schedule schedule;
	
	public abstract Connection findNextPath(Pointable pointable);
	public abstract void addDynamicNeighbours(Pointable current) throws OutOfObservablesException;
	public abstract void nextMove();
	
	public void initialise(Properties props, Telescope scope, Schedule s, List<Target> targets)
	{
		telescope = scope;
		schedule = s;
		observables = new ArrayList<Target>();
		remaining = new ArrayList<Target>();
		waitTime =Integer.parseInt(props.getProperty("wait_time"));

		for (Target target : targets) 
		{
			if(target.needsObserving())
				observables.add(target);
		}
		if(observables.size() == 0)
		{
			System.err.println("Nothing to observe. Quitting now.");
			System.exit(1);
		}
	}
	
	//Some are below the horizon, but still in the pool
		public boolean hasNoMoreObservables() {
			for (Target target : observables) 
			{
				if(!target.hasCompleteObservation())
					return false;
			}
			return true;
		}
		
		protected void waitForObservables() throws LastEntryException
		{
			int waitingPeriod = 0;
			remaining = getRemainingObservables();

			if (remaining.size() == 1)
			{
				schedule.setComplete(true);
				throw new LastEntryException();
			}
			while(true)
			{
				//advance clock until more observables emerge
				Clock.getScheduleClock().advanceBy(waitTime);
				waitingPeriod += waitTime;
				try {
					addDynamicNeighbours(schedule.getCurrentState().getCurrentTarget());
					break;
				} catch (OutOfObservablesException e1) {
					continue;
				}
			}
			schedule.getCurrentState().addWaitTime(waitingPeriod);
			schedule.getCurrentState().addComment("Waited for more observables to rise above the horizon.");
		}
		
		public List<Target> getRemainingObservables() {
			
			remaining.clear();
			for (Target target : observables) 
			{
				if(!target.hasCompleteObservation())
					remaining.add(target);
			}
			return remaining;
		}
		public TelescopeState getCurrentTelescopeState() {
			return currentTelescopeState;
		}

}
