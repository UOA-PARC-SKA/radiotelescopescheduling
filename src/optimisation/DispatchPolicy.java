package optimisation;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import observation.Connection;
import observation.Observable;
import observation.Pointable;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.TelescopeState;
import observation.interference.SkyState;
import observation.live.ObservationState;
import optimisation.triangulations.DynamicNNOptimisation;
import optimisation.triangulations.AllPulsarsAsNeighbours;
//import optimisation.triangulations.TravellingSalesmanPreoptimisation;
import optimisation.triangulations.TravellingSalesmanPreoptimisation;
import simulation.Clock;
import simulation.Simulation;
import util.Utilities;
import util.exceptions.LastEntryException;
import util.exceptions.OutOfObservablesException;


public abstract class DispatchPolicy {
	protected List<Target> remaining;
	protected List<Target> observables;
	//how long do we wait until checking for observables coming from behind the horizon
	protected int waitTime;

	protected Telescope[] telescopes;

	protected TelescopeState[] currentTelescopeStates = null;

	protected Schedule[] schedules;

	private double triangulationRatio;
	private DynamicNNOptimisation dno;
	private AllPulsarsAsNeighbours allOb;
	private TravellingSalesmanPreoptimisation tspo;

	//public abstract Connection findNextPath(Pointable pointable);



	public abstract Connection[] findNextPaths(Pointable[] pointables);

	public void initialise(Properties props, Telescope[] scopes, Schedule[] s, List<Target> targets, SkyState skyState) {
		telescopes = scopes;
		schedules = s;
		currentTelescopeStates = new TelescopeState[Simulation.NUMTELESCOPES];
		observables = new ArrayList<Target>();
		remaining = new ArrayList<Target>();
		waitTime = Integer.parseInt(props.getProperty("wait_time"));

		for (Target target : targets) {
			if (target.needsObserving())
				observables.add(target);
		}
		if (observables.size() == 0) {
			System.err.println("Nothing to observe. Quitting now.");
			System.exit(1);
		}
		triangulationRatio = Double.parseDouble(props.getProperty("nn_distance_ratio"));
		dno = new DynamicNNOptimisation();
		allOb = new AllPulsarsAsNeighbours();
		tspo = new TravellingSalesmanPreoptimisation();
	}

/*
	public void initialise(Properties props, Telescope scope, Schedule s, List<Target> targets, SkyState skyState) {
		telescope = scope;
		schedule = s;
		observables = new ArrayList<Target>();
		remaining = new ArrayList<Target>();
		waitTime = Integer.parseInt(props.getProperty("wait_time"));

		for (Target target : targets) {
			if (target.needsObserving())
				observables.add(target);
		}
		if (observables.size() == 0) {
			System.err.println("Nothing to observe. Quitting now.");
			System.exit(1);
		}
		triangulationRatio = Double.parseDouble(props.getProperty("nn_distance_ratio"));
		dno = new DynamicNNOptimisation();
		allOb = new AllPulsarsAsNeighbours();
		tspo = new TravellingSalesmanPreoptimisation();
	}
*/

	//Some are below the horizon, but still in the pool
	public boolean hasNoMoreObservables() {
		for (Target target : observables) {
			if (!target.hasCompleteObservation())
				return false;
		}
		return true;
	}

	protected void waitForObservables(String preoptimisation) throws LastEntryException {
		System.out.println("Now waiting!");
		int[] waitingPeriod = new int[Simulation.NUMTELESCOPES];
        for(int i=0; i< Simulation.NUMTELESCOPES; i++)
            waitingPeriod[i] = 0;

        remaining = getRemainingObservables();
//			for (Target target : remaining) {
//				System.out.println(
//				target.getEquatorialCoordinates().getDeclination());
//			}

		while (true) {
			if (remaining.size()<Simulation.NUMTELESCOPES) {
				for(int i=0; i< Simulation.NUMTELESCOPES; i++)
					schedules[i].setComplete(true);
				throw new LastEntryException();
			}

			//advance clock until more observables emerge, only delay the earliest clock
            int earliest = 0;
			for(int i=0; i< Simulation.NUMTELESCOPES; i++){
                if(Clock.getScheduleClock()[i].getTime().getTime().getTime() <
                        Clock.getScheduleClock()[earliest].getTime().getTime().getTime())
                    earliest = i;
            }
            Clock.getScheduleClock()[earliest].advanceBy(waitTime);
			waitingPeriod[earliest] += waitTime;

			try {
				/*
				if(preoptimisation.equals("all")) {
					addAllNeighbours(schedule.getCurrentState().getCurrentTarget());
				}
				else if (preoptimisation.equals("tsp")) {
					addTSPNeighbours(schedule.getCurrentState().getCurrentTarget());
				}
				else {
					addDynamicNeighbours(schedule.getCurrentState().getCurrentTarget());
				}

				 */
				Pointable[] pointables = new Pointable[Simulation.NUMTELESCOPES];
				for(int i=0; i< Simulation.NUMTELESCOPES; i++)
					pointables[i] = schedules[i].getCurrentState().getCurrentTarget();
				addNeighbours(preoptimisation, pointables);
				break;
			} catch (OutOfObservablesException e1) {
				continue;
			}
		}

		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
            if(waitingPeriod[i] != 0){
                telescopes[i].applyWaitState();
                schedules[i].getCurrentState().addWaitTime(waitingPeriod[i]);
                schedules[i].getCurrentState().addComment("Waited for more observables to rise above the horizon.");
            }
		}

	}

	public void addTelescopeIdleTime(int telescope_num){
		Clock.getScheduleClock()[telescope_num].advanceBy(waitTime);
		telescopes[telescope_num].applyWaitState();
		schedules[telescope_num].getCurrentState().addWaitTime(waitTime);
		schedules[telescope_num].getCurrentState().addComment("Waited for more observables to rise above the horizon.");
	}


	public List<Target> getRemainingObservables() {

		remaining.clear();
		for (Target target : observables) {
			if (!target.hasCompleteObservation())
				remaining.add(target);
		}
		return remaining;
	}

/*
	public void nextMove() {

		Connection link = findNextPath(schedule.getCurrentState().getCurrentTarget());
		Target newTarget = (Target) link.getOtherTarget(schedule.getCurrentState().getCurrentTarget());
		Observable o = newTarget.findObservableByObservationTime();
		schedule.addState(new ObservationState(newTarget, Clock.getScheduleClock().getTime(), link, o, telescope.getLocation()));

	}

 */

	public void nextMoves(){
		Pointable[] pointables = new Pointable[Simulation.NUMTELESCOPES];
		for(int i=0; i< Simulation.NUMTELESCOPES; i++)
			pointables[i] = schedules[i].getCurrentState().getCurrentTarget();

		Connection[] link = findNextPaths(pointables);
		for(int i=0; i< Simulation.NUMTELESCOPES; i++){
			Target newTarget = (Target) link[i].getOtherTarget(schedules[i].getCurrentState().getCurrentTarget());
			Observable o = newTarget.findObservableByObservationTime();
			schedules[i].addState(new ObservationState(newTarget, Clock.getScheduleClock()[i].getTime(), link[i], o, telescopes[i].getLocation()));
		}
	}

	public void addNeighbourtoScheduleState(int telescope_num, Connection link, Clock clock){
		Target newTarget = (Target) link.getOtherTarget(schedules[telescope_num].getCurrentState().getCurrentTarget());
		Observable o = newTarget.findObservableByObservationTime();
		schedules[telescope_num].addState(new ObservationState(newTarget, clock.getTime(), link, o, telescopes[telescope_num].getLocation()));
	}


/*
	public void addDynamicNeighbours(Pointable current) throws OutOfObservablesException {
		dno.createDynamicLinksByTriangles(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
	}

	public void addAllNeighbours(Pointable current) throws OutOfObservablesException {
		allOb.createAllLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
	}

	public void addTSPNeighbours(Pointable current) throws OutOfObservablesException {
		tspo.createTSPLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation(), telescope);
	}

	public void addNeighbours(String preoptimisation, Pointable current) throws OutOfObservablesException {
		if (preoptimisation.equals("all")) {
			allOb.createAllLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
		}
		else if (preoptimisation.equals("tsp")) {
			tspo.createTSPLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation(), telescope);
		}
		else {
			dno.createDynamicLinksByTriangles(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
		}
	}

 */



	public void addNeighbours(String preoptimisation, Pointable[] currents) throws OutOfObservablesException {
		if (preoptimisation.equals("tsp")) {
			tspo.createTSPLinks(observables, currents, triangulationRatio, Clock.getScheduleClock(), telescopes[0].getLocation(), telescopes);

		}
		else if (preoptimisation.equals("all")){
			allOb.createAllLinks(observables, currents, triangulationRatio, Clock.getScheduleClock(), telescopes[0].getLocation(), telescopes);
		}
		else {
			tspo.createTSPLinks(observables, currents, triangulationRatio, Clock.getScheduleClock(), telescopes[0].getLocation(), telescopes);
			}

		for(Pointable current : currents)
			if(current.getNeighbours().size()<1)
				throw new OutOfObservablesException();
	}
}
