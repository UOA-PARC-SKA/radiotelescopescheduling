package optimisation.triangulations;

import astrometrics.Location;
import observation.Connection;
import observation.Pointable;
import observation.Target;
import observation.TelescopeState;
import simulation.Clock;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RandomOptimisation extends NNOptimisation {

    ArrayList<Double> distList = new ArrayList<Double>();

    public RandomOptimisation()
    {
        hm = new HashMap<Double, Target>();
        sortedDist = new ArrayList<Double>();
    }

    public void createRandomLinks(List<Target> targets, Pointable current, double ratio, Clock clock, Location loc) throws OutOfObservablesException {

        System.out.print(targets);

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
}
