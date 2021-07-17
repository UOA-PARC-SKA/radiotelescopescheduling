package optimisation;

import observation.*;
import optimisation.triangulations.RandomOptimisation;
import simulation.Clock;
import util.exceptions.OutOfObservablesException;

public class RandomPolicy extends DispatchPolicy {

    @Override
    public Connection findNextPath(Pointable current) {

        Connection next;

        int random = (int) (Math.random() * current.getNeighbours().size());
        Connection conn = current.getNeighbours().get(random);
        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

//        while (((Target) next.getOtherTarget(schedule.getCurrentState().getCurrentTarget())).findObservableByObservationTime() == null) {
//            random = (int) (Math.random() * current.getNeighbours().size());
//            conn = current.getNeighbours().get(random);
//            p = conn.getOtherTarget(current);
//            possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
//            currentTelescopeState = possState;
//            next = conn;
//        }

        //System.out.println(next.getDistance()+" "+state.getSlewTime());
        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
        return next;
    }
}
