package optimisation.triangulations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import observation.Connection;
import observation.Pointable;
import observation.Target;
import simulation.Clock;
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
    public void createTSPLinks(List<Target> targets, Pointable current, double ratio, Clock clock, Location loc) throws OutOfObservablesException
    {
        // exclude everything that's not within 90 degrees

        current.clearNeighbours();
        Target target2;

        sortedDist.clear();
        hm.clear();

        for (int j = 0; j < targets.size(); j++)
        {
            target2 = targets.get(j);
            if (current == target2)
                continue;
            //this checks
            //- whether the target needs observing
            //- whether the target's observation is complete already
            //- whether the target is within scintillation timescale of previous observation
            //- whether the target is up
            if(!isReadyForObservation(target2, clock, loc))
                continue;



            double dist = 0;
            try {
                dist = current.angularDistanceTo(target2, loc, clock.getTime());
            } catch (WrongTypeException e)
            {
                try {
                    dist = target2.angularDistanceTo(current, loc, clock.getTime());
                } catch (WrongTypeException e1) {
                    // Should never happen, this means either one is a target
                    e1.printStackTrace();
                }
            }
            hm.put(dist, target2);
            sortedDist.add(dist);
        }
        if(sortedDist.isEmpty())
            throw new OutOfObservablesException();
        Collections.sort(sortedDist);

        int neighboursCap = (Math.min(sortedDist.size(), 50));

        for (int i = 0; i < neighboursCap; i++) {
            Connection c = new Connection(current, hm.get(sortedDist.get(i)), sortedDist.get(i));
            current.addNeighbour(c);
        }

//        current.clearNeighbours();
//        Target target;
//        ArrayList<Connection> neighbours = new ArrayList<Connection>();
//
//        for (int i = 0; i < targets.size(); i++) {
//
//            target = targets.get(i);
//
//            if (current == target)
//                continue;
//
//            if(!isReadyForObservation(target, clock, loc))
//                continue;
//
//            double dist = 0;
//
//            try {
//                dist = current.angularDistanceTo(target, loc, clock.getTime());
//            } catch (WrongTypeException e) {
//                try {
//                    dist = target.angularDistanceTo(current, loc, clock.getTime());
//                } catch (WrongTypeException e1) {
//                    e1.printStackTrace();
//                }
//            }
//
//            Connection c = new Connection(current, target, dist);
//            neighbours.add(c);
////            current.addNeighbour(c);
//        }
//
//        if(neighbours.isEmpty())
//            throw new OutOfObservablesException();
//
//        int neighboursCap = (Math.min(neighbours.size(), 30));
//
//        for (int i = 0; i < neighboursCap; i++) {
//            current.addNeighbour(neighbours.get(i));
//        }
    }
}
