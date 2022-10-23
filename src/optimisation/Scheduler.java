package optimisation;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import db.MongoDBReader;
import io.TargetLocationReader;
import observation.*;
import observation.interference.Satellite;
import observation.interference.SkyState;
import observation.live.Observation;
import observation.live.ObservationState;
import simulation.Clock;
import simulation.Simulation;
import util.Utilities;
import util.exceptions.LastEntryException;
import util.exceptions.OutOfObservablesException;


public class Scheduler
{


	private Schedule[] schedules = null;

	private List<Target> targets;

	private Observation[] observations = null;

	private SkyState skyState = null;
	private DispatchPolicy policy;


	public Scheduler(Properties props, Telescope[] telescopes) throws Exception
	{
		Satellite.setMinAngDist(Double.parseDouble(props.getProperty("satellite_closeness_limit")));
		boolean useDB = Boolean.parseBoolean(props.getProperty("useDB"));
		if (useDB)
		{
			MongoDBReader mdr = new MongoDBReader();
			targets = mdr.retrieveAllTargets();
		}else
		{
			TargetLocationReader fr = new TargetLocationReader();
			targets = fr.getPulsarData(props.getProperty("dataset"));
			fr.addObservationData(targets, props.getProperty("observations_dataset"));
		}

		schedules = new Schedule[Simulation.NUMTELESCOPES];
		startSchedulingClock(props.getProperty("observation_start"));
		skyState = new SkyState(props.getProperty("norad_file_path"));
		skyState.createAllBadThingsThatMove(telescopes[0]);
		observations = new Observation[Simulation.NUMTELESCOPES];

		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
			schedules[i] = new Schedule();
			observations[i] = new Observation(props, telescopes[i], skyState);

		}
		// policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
		policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
//		if(policy instanceof MultiTelescopesMTSPPolicy)
		policy.initialise(props, telescopes, schedules, targets, skyState);
//		else
//			policy.initialise(props, telescope, schedule, targets, skyState);
	}



/*
	public Scheduler(Properties props, Telescope telescope) throws Exception
	{
		Satellite.setMinAngDist(Double.parseDouble(props.getProperty("satellite_closeness_limit")));
		boolean useDB = Boolean.parseBoolean(props.getProperty("useDB"));
		if (useDB)
		{
			MongoDBReader mdr = new MongoDBReader();
			targets = mdr.retrieveAllTargets();
		}else
		{
			TargetLocationReader fr = new TargetLocationReader();
			targets = fr.getPulsarData(props.getProperty("dataset"));
			fr.addObservationData(targets, props.getProperty("observations_dataset"));
		}


		schedule = new Schedule();
		startSchedulingClock(props.getProperty("observation_start"));
		skyState = new SkyState(props.getProperty("norad_file_path"));
		skyState.createAllBadThingsThatMove(telescope);
		observation = new Observation(props, telescope, skyState);

		// policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
		policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
		policy.initialise(props, telescope, schedule, targets, skyState);
	}
 */



	public void buildSchedule(String preoptimisation)
	{
		makeInitialState();

		boolean complete = false;
		while (!complete)
		{
			System.out.println("running");
			try {
				Pointable[] pointables = new Pointable[Simulation.NUMTELESCOPES];
				for(int i=0; i< Simulation.NUMTELESCOPES; i++)
					pointables[i] = schedules[i].getCurrentState().getCurrentTarget();
				policy.addNeighbours(preoptimisation, pointables);

				/*
				if(schedule.getCurrentState().getCurrentTarget().getNeighbours().size()==1){
					System.out.println("This is an only one neighbour case");

					Connection link = schedule.getCurrentState().getCurrentTarget().getNeighbours().get(0);
					if(link.getOtherTarget(schedule.getCurrentState().getCurrentTarget())==schedule1.getCurrentState().getCurrentTarget()) {
						link = schedule1.getCurrentState().getCurrentTarget().getNeighbours().get(0);
						policy.addNeighbourtoScheduleState(schedule1, link);
						observation1.observe(schedule1.getCurrentState());
					}
					else{
						policy.addNeighbourtoScheduleState(schedule, link);
						observation.observe(schedule.getCurrentState());
					}
					continue;
				}
				*/

			} catch (OutOfObservablesException e)
			{
				if(policy.hasNoMoreObservables())
				{
					schedule.setComplete(true);
					schedule1.setComplete(true);
					System.out.println("GOOD!!!!!!");
					break;
				}
				else
				{
					try {
						policy.waitForObservables(preoptimisation);
						/*
						if(schedule.getCurrentState().getCurrentTarget().getNeighbours().size()==1){
							System.out.println("This is an only one neighbour case");

							Connection link = schedule.getCurrentState().getCurrentTarget().getNeighbours().get(0);
							if(link.getOtherTarget(schedule.getCurrentState().getCurrentTarget())==schedule1.getCurrentState().getCurrentTarget()) {
								link = schedule1.getCurrentState().getCurrentTarget().getNeighbours().get(0);
								policy.addNeighbourtoScheduleState(schedule1, link);
								observation1.observe(schedule1.getCurrentState());
							}
							else{
								policy.addNeighbourtoScheduleState(schedule, link);
								observation.observe(schedule.getCurrentState());
							}
							continue;
						}
						 */

					} catch (LastEntryException e1) {
						break;
					}
				}
			}
			policy.nextMoves();
			for(int i=0; i< Simulation.NUMTELESCOPES; i++)
				observations[i].observe(schedules[i].getCurrentState());

			// added this here in hope to fix the JVM error
			System.gc();
		}
		for(int i=0; i< Simulation.NUMTELESCOPES; i++)
			schedules[i].setEndTime(Clock.getScheduleClock().getTime().getTimeInMillis());
	}



	private void makeInitialState()
	{
		for(int i=0; i< Simulation.NUMTELESCOPES; i++) {
			ObservationState firstState = new ObservationState(new Position(Telescope.PARKING_COORDINATES),
					Clock.getScheduleClock().getTime().getTimeInMillis(), null, null);
			firstState.setStartingCoordinates(Telescope.PARKING_COORDINATES);
			schedules[i].addState(firstState);
		}
	}


	public List<Connection> getFinalPath()
	{
		return schedule.getFinalPath();
	}


	public void startSchedulingClock(String property)
	{
		Date startDate = Utilities.stringToDate(property);
		Clock.getScheduleClock().startAt(startDate);
		schedule.setStartTime(startDate.getTime());
		schedule1.setStartTime(startDate.getTime());
	}

	public Schedule getSchedule(int i)
	{
		return schedules[i];
	}


	public List<Target> getAllTargets()
	{
		return targets;
	}


	public SkyState getSkyState() {
		return skyState;
	}




}