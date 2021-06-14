package optimisation;

import java.util.List;
import java.util.Properties;

import observation.Connection;
import observation.Observable;
import observation.Pointable;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.TelescopeState;
import observation.live.ObservationState;
import optimisation.triangulations.DynamicNNOptimisation;
import simulation.Clock;
import util.exceptions.OutOfObservablesException;

public class BasicNNScheduler extends Policy
{
	

	private double triangulationRatio;
	private DynamicNNOptimisation dno;
	
	public void initialise(Properties props, Telescope scope, Schedule s, List<Target> targets)
	{
		super.initialise(props, scope, s, targets);
		triangulationRatio = Double.parseDouble(props.getProperty("nn_distance_ratio"));
		dno = new DynamicNNOptimisation();
	}
	
	//Shortest path based on slew time
	public Connection findNextPath(Pointable current)
	{
		double minDist = Double.POSITIVE_INFINITY;
		Connection next = null;
		
		//System.out.println();
		for (Connection conn : current.getNeighbours()) 
		{
			Pointable p = conn.getOtherTarget(current);
			List<TelescopeState> possStates = telescope.getNewStates(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
			//System.out.println("Angle dist "+conn.getDistance()+" slew time "+possStates.get(0).getSlewTime());
//			if(possStates.size() > 1)
//				System.out.println("States "+possStates.size());
			for (TelescopeState telescopeState : possStates) 
			{
				if(minDist > telescopeState.getSlewTime())
				{
					next = conn;
					minDist = telescopeState.getSlewTime();
					currentTelescopeState = telescopeState;
				}
			}
	
		}
		//System.out.println(next.getDistance()+" "+state.getSlewTime());
		telescope.applyNewState(currentTelescopeState);
		schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
		return next;
	}
	
	public void nextMove()
	{

		Connection link = findNextPath(schedule.getCurrentState().getCurrentTarget());
		Target newTarget = (Target) link.getOtherTarget(schedule.getCurrentState().getCurrentTarget());
		Observable o = newTarget.findObservableByObservationTime();
		schedule.addState(new ObservationState(newTarget, Clock.getScheduleClock().getTime(), link, o, telescope.getLocation()));


	}
	
	public void addDynamicNeighbours(Pointable current) throws OutOfObservablesException
	{
		dno.createDynamicLinksByTriangles(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
	}
	
	

}
