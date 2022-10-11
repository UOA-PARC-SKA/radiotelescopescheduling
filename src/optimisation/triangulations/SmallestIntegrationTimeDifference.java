package optimisation.triangulations;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;
import observation.*;
import observation.Observable;
import simulation.Clock;
import util.Utilities;
import util.exceptions.OutOfObservablesException;

import java.util.*;

public class SmallestIntegrationTimeDifference extends NNOptimisation{

    public SmallestIntegrationTimeDifference(){
        hm = new HashMap<Double, Target>();
        sortedDist = new ArrayList<Double>();
    }

    public void createLinks(List<Target> targets, Pointable current, Pointable current1, double ratio, Clock clock, Location loc, Telescope telescope, Telescope telescope1) throws OutOfObservablesException{

        sortedDist.clear();
        hm.clear();

        HashMap<Target, Double> hm1 = new HashMap<Target, Double>();
        HashMap<Target, Double> hm2 = new HashMap<Target, Double>();
        LinkedHashMap<Target, Double> index = new LinkedHashMap<Target, Double>();

        current.clearNeighbours();
        current1.clearNeighbours();

        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);

            if (current == target)
                continue;
            if (current1 == target)
                continue;

            if(!isReadyForObservation(target, clock, loc))
                continue;

            Observable pulsar = target.findObservableByObservationTime();
            double serviceTime = pulsar.getRemainingIntegrationTime();

            double slewTime = calcSlewTime(target, telescope);
            double totalTime = serviceTime+slewTime;
            hm1.put(target, totalTime);

            slewTime = calcSlewTime(target, telescope1);
            totalTime = serviceTime+slewTime;
            hm2.put(target, totalTime);

            index.put(target, Math.abs(hm1.get(target)-hm2.get(target)));
        }
        if(index.isEmpty())
            throw new OutOfObservablesException();

        index = sortMap(index);
        List<Target> orderedList = new ArrayList<>(index.keySet());

        int neighboursCap = (Math.min(orderedList.size(), 10));
        for (int i = 0; i < neighboursCap; i++) {
            Connection c = new Connection(current, orderedList.get(i), hm1.get(orderedList.get(i)));
            current.addNeighbour(c);

            Connection c1 = new Connection(current1, orderedList.get(i), hm2.get(orderedList.get(i)));
            current1.addNeighbour(c1);
        }

    }

    private double calcSlewTime(Target target, Telescope telescope){
        HorizonCoordinates hc = target.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
        TelescopeState possState = telescope.getStateForShortestSlew(hc);
        GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
        int slewInSeconds = (int) possState.getSlewTime();
        setTime.add(GregorianCalendar.SECOND, slewInSeconds);
        double time = Conversions.getTimeUntilObjectSetsInSeconds(telescope.getLocation(), target, setTime);

        return time;
    }

    public LinkedHashMap<Target, Double> sortMap(Map<Target, Double> map) {
        List<Map.Entry<Target, Double>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> {
            int compare = (o1.getValue()).compareTo(o2.getValue());
            //int compare = (o1.getKey()).compareTo(o2.getKey());
            //return -compare;
            return compare;
        });

        LinkedHashMap<Target, Double> returnMap = new LinkedHashMap<Target, Double>();
        for (Map.Entry<Target, Double> entry : list) {
            returnMap.put(entry.getKey(), entry.getValue());
        }
        return returnMap;
    }

}
