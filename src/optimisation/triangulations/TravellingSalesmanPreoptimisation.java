package optimisation.triangulations;

import java.util.*;

import astrometrics.Conversions;
import observation.*;
import observation.Observable;
import simulation.Clock;
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

            /*
            if(target.hasCompleteObservation())
                continue;

            if(target.findObservableByObservationTime()==null)
                continue;

 */

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
}
