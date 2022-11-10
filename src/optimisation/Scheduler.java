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
		observations = new Observation[Simulation.NUMTELESCOPES];

		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
			schedules[i] = new Schedule();
		}

		startSchedulingClock(props.getProperty("observation_start"));
		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
			skyState = new SkyState(props.getProperty("norad_file_path"));
			skyState.createAllBadThingsThatMove(telescopes[i], Clock.getScheduleClock()[i]);
			observations[i] = new Observation(props, telescopes[i], skyState, Clock.getScheduleClock()[i]);
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

				if(schedules[0].getCurrentState().getCurrentTarget().getNeighbours().size()<Simulation.NUMTELESCOPES){
					System.out.println("targets are not enough for all telescopes");
					int num = schedules[0].getCurrentState().getCurrentTarget().getNeighbours().size();
					for(int i = 0; i<num; i++){
						Connection link = schedules[i].getCurrentState().getCurrentTarget().getNeighbours().get(i);
						policy.addNeighbourtoScheduleState(i, link, Clock.getScheduleClock()[i]);
						observations[i].observe(schedules[i].getCurrentState());
					}
					continue;
				}

			} catch (OutOfObservablesException e) {

				if(policy.hasNoMoreObservables())
				{
					for(int i=0; i< Simulation.NUMTELESCOPES; i++)
						schedules[i].setComplete(true);
					complete = true;
					System.out.println("GOOD!!!!!!");
					break;
				}
				else {
					boolean[] teleMarks = new boolean[Simulation.NUMTELESCOPES];
					if(policy.nextMovesEach(teleMarks)){
						for(int i=0; i< Simulation.NUMTELESCOPES; i++){
							if(!teleMarks[i])
								continue;
							observations[i].observe(schedules[i].getCurrentState());
						}
						continue;
					}
					try {
						if(policy.waitForObservables(preoptimisation, teleMarks)){
							for(int i=0; i< Simulation.NUMTELESCOPES; i++){
								if(!teleMarks[i])
									continue;
								observations[i].observe(schedules[i].getCurrentState());
							}
							continue;
						}

						if(schedules[0].getCurrentState().getCurrentTarget().getNeighbours().size()<Simulation.NUMTELESCOPES){
							System.out.println("targets are not enough for all telescopes");
							int num = schedules[0].getCurrentState().getCurrentTarget().getNeighbours().size();
							for(int i = 0; i<num; i++){
								Connection link = schedules[i].getCurrentState().getCurrentTarget().getNeighbours().get(i);
								policy.addNeighbourtoScheduleState(i, link, Clock.getScheduleClock()[i]);
								observations[i].observe(schedules[i].getCurrentState());
							}
							continue;
						}

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
			schedules[i].setEndTime(Clock.getScheduleClock()[i].getTime().getTimeInMillis());
	}



	private void makeInitialState()
	{
		for(int i=0; i< Simulation.NUMTELESCOPES; i++) {
			ObservationState firstState = new ObservationState(new Position(Telescope.PARKING_COORDINATES),
					Clock.getScheduleClock()[i].getTime().getTimeInMillis(), null, null);
			firstState.setStartingCoordinates(Telescope.PARKING_COORDINATES);
			schedules[i].addState(firstState);
		}
	}

/*
	public List<Connection> getFinalPath()
	{
		return schedule.getFinalPath();
	}
 */


	public void startSchedulingClock(String property)
	{
		Date startDate = Utilities.stringToDate(property);
		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
			Clock.getScheduleClock()[i].startAt(startDate);
			schedules[i].setStartTime(startDate.getTime());
		}
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