package optimisation;

import astrometrics.HorizonCoordinates;
import observation.*;
import observation.interference.SkyState;
import simulation.Clock;

import java.util.List;
import java.util.Properties;

public class LargestSlewPolicy extends DispatchPolicy {

    public Connection findNextPath(Pointable current)
    {
        double maxDist = 0;
        Connection next = null;

        for (Connection conn : current.getNeighbours())
        {
            Pointable p = conn.getOtherTarget(current);
            TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));

            if(possState.getSlewTime() >= maxDist)
            {
                next = conn;
                maxDist = possState.getSlewTime();
                currentTelescopeState = possState;
            }

        }
        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);
//		updateObservable((Target)next.getOtherTarget(current));
        return next;
    }


}
