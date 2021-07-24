package optimisation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import observation.Connection;
import observation.Observable;
import observation.Pointable;
import observation.Position;
import observation.Schedule;
import observation.Target;
import observation.Telescope;
import observation.TelescopeState;
import observation.interference.SkyState;
import observation.live.ObservationState;
import optimisation.triangulations.DynamicNNOptimisation;
import optimisation.triangulations.RandomOptimisation;
import simulation.Clock;
import util.exceptions.LastEntryException;
import util.exceptions.OutOfObservablesException;


public abstract class DispatchPolicy {
    protected List<Target> remaining;
    protected List<Target> observables;
    //how long do we wait until checking for observables coming from behind the horizon
    protected int waitTime;
    protected Telescope telescope;
    protected TelescopeState currentTelescopeState = null;
    protected Schedule schedule;
    private double triangulationRatio;
    private DynamicNNOptimisation dno;
    private RandomOptimisation ro;


    public abstract Connection findNextPath(Pointable pointable);


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
        ro = new RandomOptimisation();
    }

    //Some are below the horizon, but still in the pool
    public boolean hasNoMoreObservables() {
        for (Target target : observables) {
            if (!target.hasCompleteObservation())
                return false;
        }
        return true;
    }

    protected void waitForObservables(String preoptimisation) throws LastEntryException {
        int waitingPeriod = 0;
        remaining = getRemainingObservables();
//			for (Target target : remaining) {
//				System.out.println(
//				target.getEquatorialCoordinates().getDeclination());
//			}

        if (remaining.size() == 1) {
            schedule.setComplete(true);
            throw new LastEntryException();
        }
        while (true) {
            //advance clock until more observables emerge
            Clock.getScheduleClock().advanceBy(waitTime);
            waitingPeriod += waitTime;
            try {
            	if(preoptimisation.equals("random")) {
					addRandomNeighbours(schedule.getCurrentState().getCurrentTarget());
				} else {
					addDynamicNeighbours(schedule.getCurrentState().getCurrentTarget());
				}
                break;
            } catch (OutOfObservablesException e1) {
                continue;
            }
        }
        telescope.applyWaitState();
        schedule.getCurrentState().addWaitTime(waitingPeriod);
        schedule.getCurrentState().addComment("Waited for more observables to rise above the horizon.");
    }

    protected void waitForObservablesRandom() throws LastEntryException {
        int waitingPeriod = 0;
        remaining = getRemainingObservables();
//			for (Target target : remaining) {
//				System.out.println(
//				target.getEquatorialCoordinates().getDeclination());
//			}

        if (remaining.size() == 1) {
            schedule.setComplete(true);
            throw new LastEntryException();
        }
        while (true) {
            //advance clock until more observables emerge
            Clock.getScheduleClock().advanceBy(waitTime);
            waitingPeriod += waitTime;
            try {
                addRandomNeighbours(schedule.getCurrentState().getCurrentTarget());
                break;
            } catch (OutOfObservablesException e1) {
                continue;
            }
        }
        telescope.applyWaitState();
        schedule.getCurrentState().addWaitTime(waitingPeriod);
        schedule.getCurrentState().addComment("Waited for more observables to rise above the horizon.");
    }

    public List<Target> getRemainingObservables() {

        remaining.clear();
        for (Target target : observables) {
            if (!target.hasCompleteObservation())
                remaining.add(target);
        }
        return remaining;
    }


    public void nextMove() {

        Connection link = findNextPath(schedule.getCurrentState().getCurrentTarget());
        Target newTarget = (Target) link.getOtherTarget(schedule.getCurrentState().getCurrentTarget());
        Observable o = newTarget.findObservableByObservationTime();
        schedule.addState(new ObservationState(newTarget, Clock.getScheduleClock().getTime(), link, o, telescope.getLocation()));

    }


    public void addDynamicNeighbours(Pointable current) throws OutOfObservablesException {
        dno.createDynamicLinksByTriangles(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
    }

    public void addRandomNeighbours(Pointable current) throws OutOfObservablesException {
        ro.createRandomLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
    }

    public void addNeighbours(String preoptimisation, Pointable current) throws OutOfObservablesException {
        if (preoptimisation.equals("random")) {
            ro.createRandomLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
        } else {
            dno.createDynamicLinksByTriangles(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation());
        }
    }
}
