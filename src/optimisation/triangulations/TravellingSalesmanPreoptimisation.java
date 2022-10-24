package optimisation.triangulations;

import java.util.*;

import astrometrics.Conversions;
import observation.*;
import observation.Observable;
import simulation.Clock;
import simulation.Simulation;
import util.Utilities;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;

public class TravellingSalesmanPreoptimisation extends NNOptimisation
{


    public TravellingSalesmanPreoptimisation()
    {
        hm = new HashMap<Double, Target>();
        sortedDist = new ArrayList<Double>();
    }

    /*
    //These targets are already the subset that needs observing
    public void createTSPLinks(List<Target> targets, Pointable current, double ratio, Clock clock, Location loc, Telescope telescope) throws OutOfObservablesException
    {
        HashMap<Long, Target> hm_tsp = new HashMap<Long, Target>();
        ArrayList<Long> sortedDist_tsp = new ArrayList<Long>();

        current.clearNeighbours();
        Target target;

        sortedDist.clear();
        hm.clear();

        double maxSettingTime = 0;

        for (int i = 0; i < targets.size(); i++) {
            target = targets.get(i);

            if (current == target)
                continue;

            if(!isReadyForObservation(target, clock, loc))
                continue;

            HorizonCoordinates hc = target.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
            TelescopeState possState = telescope.getStateForShortestSlew(hc);
            GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
            int slewInSeconds = (int) possState.getSlewTime();
            setTime.add(GregorianCalendar.SECOND, slewInSeconds);
            long time = (long) Conversions.getTimeUntilObjectSetsInSeconds(telescope.getLocation(), target, setTime);
            hm_tsp.put(time, target);
            sortedDist_tsp.add(time);
        }

        if(sortedDist_tsp.isEmpty())
            throw new OutOfObservablesException();

        Collections.sort(sortedDist_tsp);

        int neighboursCap = (Math.min(sortedDist_tsp.size(), 10));

        for (int i = 0; i < neighboursCap; i++) {
            Connection c = new Connection(current, hm_tsp.get(sortedDist_tsp.get(i)), sortedDist_tsp.get(i));
            current.addNeighbour(c);
        }
    }
     */


    public void createTSPLinks(List<Target> targets, Pointable[] currents, double ratio, Clock[] clock, Location loc, Telescope[] telescopes) throws OutOfObservablesException
    {
        HashMap<Long, Target> hm_tsp = new HashMap<Long, Target>();
        ArrayList<Long> sortedDist_tsp = new ArrayList<Long>();

        for(int i = 0; i< Simulation.NUMTELESCOPES; i++)
            currents[i].clearNeighbours();

        Target target;

        sortedDist.clear();
        hm.clear();

        double maxSettingTime = 0;

        for (int i = 0; i < targets.size(); i++) {
            target = targets.get(i);

            boolean mark = true;
            for(int k=0; k< Simulation.NUMTELESCOPES; k++)
                if (currents[k] == target){
                    mark = false;
                    break;
                }
            if(!mark)
                continue;

           mark = true;
            for(int k=0; k< Simulation.NUMTELESCOPES; k++)
                if(!isReadyForObservation(target, clock[k], loc)){
                    mark = false;
                    break;
                }
            if(!mark)
                continue;

            HorizonCoordinates hc = target.getHorizonCoordinates(telescopes[0].getLocation(), Clock.getScheduleClock()[0].getTime());
            TelescopeState possState = telescopes[0].getStateForShortestSlew(hc);
            GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock()[0].getTime());
            int slewInSeconds = (int) possState.getSlewTime();
            setTime.add(GregorianCalendar.SECOND, slewInSeconds);
            long time = (long) Conversions.getTimeUntilObjectSetsInSeconds(telescopes[0].getLocation(), target, setTime);
            hm_tsp.put(time, target);
            sortedDist_tsp.add(time);
        }

        if(sortedDist_tsp.isEmpty())
            throw new OutOfObservablesException();

        Collections.sort(sortedDist_tsp);

        int neighboursCap = (Math.min(sortedDist_tsp.size(), 10));

        for (int i = 0; i < neighboursCap; i++) {
            for(int k=0; k< Simulation.NUMTELESCOPES; k++){
                target = hm_tsp.get(sortedDist_tsp.get(i));
                HorizonCoordinates hc = target.getHorizonCoordinates(telescopes[k].getLocation(), Clock.getScheduleClock()[k].getTime());
                TelescopeState possState = telescopes[k].getStateForShortestSlew(hc);
                GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock()[k].getTime());
                int slewInSeconds = (int) possState.getSlewTime();
                setTime.add(GregorianCalendar.SECOND, slewInSeconds);
                long time = (long) Conversions.getTimeUntilObjectSetsInSeconds(telescopes[k].getLocation(), target, setTime);
                Connection c = new Connection(currents[k], target, time);
                currents[k].addNeighbour(c);
            }
        }
    }
}
