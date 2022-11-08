package optimisation;

import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;

import java.util.ArrayList;
import java.util.List;

public class RandomPolicy extends DispatchPolicy {

    /*
    @Override
    public Connection findNextPath(Pointable current) {

        Connection next;

        int random = (int) (Math.random() * current.getNeighbours().size());
        Connection conn = current.getNeighbours().get(random);
        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
        return next;
    }

     */


    @Override
    public Connection[] findNextPaths(Pointable[] currents) {

        Connection[] next = new Connection[Simulation.NUMTELESCOPES];
        List<Pointable> points = new ArrayList<Pointable>();

        for(int i = 0; i< Simulation.NUMTELESCOPES; i++){
            Connection conn = null;
            Pointable p = null;
            do {
                int random = (int) (Math.random() * currents[i].getNeighbours().size());
                conn = currents[i].getNeighbours().get(random);
                p = conn.getOtherTarget(currents[i]);
            }while (points.contains(p));

            points.add(p);
            TelescopeState possState = telescopes[i].getStateForShortestSlew(p.getHorizonCoordinates(telescopes[i].getLocation(), Clock.getScheduleClock()[i].getTime()));
            currentTelescopeStates[i] = possState;
            next[i] = conn;

            telescopes[i].applyNewState(currentTelescopeStates[i]);
            schedules[i].addLink(next[i], currentTelescopeStates[i]);
//		updateObservable((Target)next.getOtherTarget(current));

        }
        return next;
    }
}
