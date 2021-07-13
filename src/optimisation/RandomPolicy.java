package optimisation;

import observation.Connection;
import observation.Pointable;
import observation.TelescopeState;
import simulation.Clock;

public class RandomPolicy extends DispatchPolicy {
    @Override
    public Connection findNextPath(Pointable current) {

        Connection next = null;
        int random = (int )(Math.random() * current.getNeighbours().size());
        Connection conn = current.getNeighbours().get(random);
        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

        //System.out.println(next.getDistance()+" "+state.getSlewTime());
        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
        return next;
    }
}
