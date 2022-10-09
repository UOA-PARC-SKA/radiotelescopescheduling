package optimisation;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import db.MongoDBReader;
import io.TargetLocationReader;
import observation.Connection;
import observation.Position;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.interference.Satellite;
import observation.interference.SkyState;
import observation.live.Observation;
import observation.live.ObservationState;
import simulation.Clock;
import util.Utilities;
import util.exceptions.LastEntryException;
import util.exceptions.OutOfObservablesException;


public class Scheduler
{


	private Schedule schedule = null;
	private Schedule schedule1 = null;

	private List<Target> targets;

	private Observation observation = null;
	private Observation observation1 = null;
	private SkyState skyState = null;
	private DispatchPolicy policy;


	public Scheduler(Properties props, Telescope telescope, Telescope telescope1) throws Exception
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
		schedule1 = new Schedule();
		startSchedulingClock(props.getProperty("observation_start"));
		skyState = new SkyState(props.getProperty("norad_file_path"));
		skyState.createAllBadThingsThatMove(telescope);
		observation = new Observation(props, telescope, skyState);
		observation1 = new Observation(props, telescope1, skyState);
		// policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
		policy = (DispatchPolicy) Class.forName(props.getProperty("policy_class")).newInstance();
		if(policy instanceof MultiTelescopesMTSPPolicy)
			policy.initialise(props, telescope, telescope1, schedule, schedule1, targets, skyState);
		else
			policy.initialise(props, telescope, schedule, targets, skyState);
	}




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



	public void buildSchedule(String preoptimisation)
	{
		makeInitialState();

		while (!schedule.isComplete() || !schedule1.isComplete())
		{
			System.out.println("running");
			try {
				policy.addNeighbours(preoptimisation, schedule.getCurrentState().getCurrentTarget(), schedule1.getCurrentState().getCurrentTarget());
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
					} catch (LastEntryException e1) {
						break;
					}
				}
			}
			if(policy.next2Move()==-1) break;
			observation.observe(schedule.getCurrentState());
			observation1.observe(schedule1.getCurrentState());
			// added this here in hope to fix the JVM error
			System.gc();
		}
		schedule.setEndTime(Clock.getScheduleClock().getTime().getTimeInMillis());
		schedule1.setEndTime(Clock.getScheduleClock().getTime().getTimeInMillis());
	}



	private void makeInitialState()
	{
		ObservationState firstState = new ObservationState(new Position(Telescope.PARKING_COORDINATES),
				Clock.getScheduleClock().getTime().getTimeInMillis(), null, null);
		firstState.setStartingCoordinates(Telescope.PARKING_COORDINATES);
        ObservationState firstState1 = new ObservationState(new Position(Telescope.PARKING_COORDINATES),
                Clock.getScheduleClock().getTime().getTimeInMillis(), null, null);
        firstState1.setStartingCoordinates(Telescope.PARKING_COORDINATES);

		schedule.addState(firstState);
		schedule1.addState(firstState1);
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

	public Schedule getSchedule()
	{
		return schedule;
	}

	public Schedule getSchedule1()
	{
		return schedule1;
	}

	public List<Target> getAllTargets()
	{
		return targets;
	}


	public SkyState getSkyState() {
		return skyState;
	}




}