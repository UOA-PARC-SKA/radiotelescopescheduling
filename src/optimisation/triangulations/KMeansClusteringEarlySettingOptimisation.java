package optimisation.triangulations;

import astrometrics.Conversions;
import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

public class KMeansClusteringEarlySettingOptimisation extends KMeansClusteringOptimisation {

    // Helper class to associate targets with their setting times
    private static class TargetSettingInfo {
        Target target;
        double timeUntilSet; // in seconds

        TargetSettingInfo(Target target, double timeUntilSet) {
            this.target = target;
            this.timeUntilSet = timeUntilSet;
        }
    }

    @Override
    public void createKMeansLinks(List<Target> targets, Pointable[] currents, double ratio,
                                  Clock[] clocks, Location loc, Telescope[] telescopes)
            throws OutOfObservablesException {

        // Clear existing neighbours for all telescopes
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            currents[i].clearNeighbours();
        }

        // Get available targets (filtered and observable)
        List<Target> availableTargets = getAvailableTargets(targets, currents, clocks, loc);
        if (availableTargets.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Determine reference time (minimum current time across all telescopes)
        GregorianCalendar refTime = findEarliestTime(clocks);

        // Select targets: top 30 earliest setting or all if <= 30
        List<Target> selectedTargets = selectEarliestSettingTargets(availableTargets, loc, refTime);

        // Cluster selected targets
        List<List<Target>> partitions = partitionKMeans(selectedTargets, currents);

        // Create connections for each telescope based on clustered targets
        for (int telescopeIndex = 0; telescopeIndex < Simulation.NUMTELESCOPES; telescopeIndex++) {
            for (Target target : partitions.get(telescopeIndex)) {
                if (isReadyForObservation(target, clocks[telescopeIndex], loc)) {
                    double dist = calculateDistance(currents[telescopeIndex], target, loc, clocks[telescopeIndex]);
                    Connection conn = new Connection(currents[telescopeIndex], target, dist);
                    currents[telescopeIndex].addNeighbour(conn);
                }
            }
        }

        // Fallback: Assign observable targets to telescopes with no connections
        for (int telescopeIndex = 0; telescopeIndex < Simulation.NUMTELESCOPES; telescopeIndex++) {
            if (currents[telescopeIndex].getNeighbours().isEmpty()) {
                for (Target target : availableTargets) {
                    if (isReadyForObservation(target, clocks[telescopeIndex], loc)) {
                        double dist = calculateDistance(currents[telescopeIndex], target, loc, clocks[telescopeIndex]);
                        Connection conn = new Connection(currents[telescopeIndex], target, dist);
                        currents[telescopeIndex].addNeighbour(conn);
                    }
                }
            }
        }

        // Ensure at least one telescope has valid connections
        boolean hasValidConnections = false;
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            if (!currents[i].getNeighbours().isEmpty()) {
                hasValidConnections = true;
                break;
            }
        }
        if (!hasValidConnections) {
            throw new OutOfObservablesException();
        }
    }

    /**
     * Finds the earliest current time among all telescope clocks.
     */
    private GregorianCalendar findEarliestTime(Clock[] clocks) {
        GregorianCalendar earliest = (GregorianCalendar) clocks[0].getTime().clone();
        for (int i = 1; i < Simulation.NUMTELESCOPES; i++) {
            GregorianCalendar currentTime = clocks[i].getTime();
            if (currentTime.before(earliest)) {
                earliest = currentTime;
            }
        }
        return earliest;
    }

    /**
     * Selects up to 30 earliest setting targets from the available list.
     */
    private List<Target> selectEarliestSettingTargets(List<Target> availableTargets, Location loc, GregorianCalendar refTime) {
        List<Target> selected = new ArrayList<>();

        // Compute setting times for all available targets
        List<TargetSettingInfo> settingInfos = new ArrayList<>();
        for (Target target : availableTargets) {
            double timeUntilSet = Conversions.getTimeUntilObjectSetsInSeconds(loc, target, refTime);
            settingInfos.add(new TargetSettingInfo(target, timeUntilSet));
        }

        // Sort by time until setting (ascending)
        settingInfos.sort(Comparator.comparingDouble(info -> info.timeUntilSet));

        // Select top 30 or all if fewer than 30
        int count = Math.min(30, settingInfos.size());
        for (int i = 0; i < count; i++) {
            selected.add(settingInfos.get(i).target);
        }
        return selected;
    }
}