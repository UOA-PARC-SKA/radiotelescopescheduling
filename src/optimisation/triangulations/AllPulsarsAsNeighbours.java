package optimisation.triangulations;

import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AllPulsarsAsNeighbours extends NNOptimisation {

    ArrayList<Double> distList = new ArrayList<Double>();

    public AllPulsarsAsNeighbours()
    {
        hm = new HashMap<Double, Target>();
        sortedDist = new ArrayList<Double>();
    }

    public void createAllLinks(List<Target> targets, Pointable current, double ratio, Clock clock, Location loc) throws OutOfObservablesException {

        current.clearNeighbours();
        Target target;
        ArrayList<Connection> neighbours = new ArrayList<Connection>();

        for (int i = 0; i < targets.size(); i++) {

            target = targets.get(i);

            if (current == target)
                continue;

            if(!isReadyForObservation(target, clock, loc))
                continue;

            double dist = 0;

            try {
                dist = current.angularDistanceTo(target, loc, clock.getTime());
            } catch (WrongTypeException e) {
                try {
                    dist = target.angularDistanceTo(current, loc, clock.getTime());
                } catch (WrongTypeException e1) {
                    e1.printStackTrace();
                }
            }

            Connection c = new Connection(current, target, dist);
            neighbours.add(c);
//            current.addNeighbour(c);
        }

        if(neighbours.isEmpty())
            throw new OutOfObservablesException();

        for (Connection connection : neighbours) {
            current.addNeighbour(connection);
        }
    }


    public void createAllLinks(List<Target> targets, Pointable[] currents, double ratio, Clock[] clock, Location loc, Telescope[] telescopes) throws OutOfObservablesException {
        for(int i = 0; i< Simulation.NUMTELESCOPES; i++)
            currents[i].clearNeighbours();

        Target target;
        ArrayList<Connection> neighbours = new ArrayList<Connection>();

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

            double dist = 0;

            for(int k = 0; k< Simulation.NUMTELESCOPES; k++){

                try {
                    dist = currents[k].angularDistanceTo(target, loc, clock[k].getTime());
                } catch (WrongTypeException e) {
                    try {
                        dist = target.angularDistanceTo(currents[k], loc, clock[k].getTime());
                    } catch (WrongTypeException e1) {
                        e1.printStackTrace();
                    }
                }

                Connection c = new Connection(currents[k], target, dist);
                currents[k].addNeighbour(c);
            }

        }

        if(currents[0].getNeighbours().isEmpty())
            throw new OutOfObservablesException();

    }
}
