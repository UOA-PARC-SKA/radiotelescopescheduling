package optimisation.triangulations;

import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;

import java.util.ArrayList;
import java.util.List;

public class SimpleRoundRobinOptimisation extends NNOptimisation {

    public SimpleRoundRobinOptimisation() {
        // No additional initialization needed for round-robin
    }

    /**
     * Creates connections using simple round-robin partitioning strategy.
     * Each telescope gets assigned targets in a round-robin fashion.
     */
    public void createRoundRobinLinks(List<Target> targets, Pointable[] currents, double ratio,
                                      Clock[] clocks, Location loc, Telescope[] telescopes)
            throws OutOfObservablesException {

        // Clear existing neighbours for all telescopes
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            currents[i].clearNeighbours();
        }

        // Filter available targets (not currently being observed and ready for observation)
        List<Target> availableTargets = getAvailableTargets(targets, currents, clocks, loc);

        if (availableTargets.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Partition targets using round-robin assignment
        List<List<Target>> partitions = partitionRoundRobin(availableTargets);

        // Create connections for each telescope based on its partition
        for (int telescopeIndex = 0; telescopeIndex < Simulation.NUMTELESCOPES; telescopeIndex++) {
            List<Target> assignedTargets = partitions.get(telescopeIndex);

            // Create connections from current position to all assigned targets
            for (Target target : assignedTargets) {
                // Double-check target is still valid for this specific telescope
                if (!isReadyForObservation(target, clocks[telescopeIndex], loc)) {
                    continue;
                }

                double dist = calculateDistance(currents[telescopeIndex], target, loc, clocks[telescopeIndex]);
                Connection connection = new Connection(currents[telescopeIndex], target, dist);
                currents[telescopeIndex].addNeighbour(connection);
            }
        }

        // Ensure at least one telescope has valid neighbours
        boolean hasValidNeighbours = false;
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            if (!currents[i].getNeighbours().isEmpty()) {
                hasValidNeighbours = true;
                break;
            }
        }

        if (!hasValidNeighbours) {
            throw new OutOfObservablesException();
        }
    }

    /**
     * Partition targets using simple round-robin assignment
     */
    private List<List<Target>> partitionRoundRobin(List<Target> targets) {
        List<List<Target>> partitions = new ArrayList<>();

        // Initialize empty partitions
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            partitions.add(new ArrayList<>());
        }

        // Round-robin assignment
        for (int i = 0; i < targets.size(); i++) {
            int telescopeIndex = i % Simulation.NUMTELESCOPES;
            partitions.get(telescopeIndex).add(targets.get(i));
        }

        return partitions;
    }

    /**
     * Get list of targets that are available for observation
     */
    private List<Target> getAvailableTargets(List<Target> targets, Pointable[] currents,
                                             Clock[] clocks, Location loc) {
        List<Target> available = new ArrayList<>();

        for (Target target : targets) {
            // Skip if target doesn't need observing or is complete
            if (!target.needsObserving() || target.hasCompleteObservation()) {
                continue;
            }

            // Skip if target is currently being observed by any telescope
            boolean currentlyObserved = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (currents[k] == target) {
                    currentlyObserved = true;
                    break;
                }
            }
            if (currentlyObserved) {
                continue;
            }

            // Check if target is observable by at least one telescope
            boolean observableByAny = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (isReadyForObservation(target, clocks[k], loc)) {
                    observableByAny = true;
                    break;
                }
            }

            if (observableByAny) {
                available.add(target);
            }
        }

        return available;
    }

    /**
     * Calculate angular distance between two pointables
     */
    private double calculateDistance(Pointable from, Target to, Location loc, Clock clock) {
        try {
            return from.angularDistanceTo(to, loc, clock.getTime());
        } catch (WrongTypeException e) {
            try {
                return to.angularDistanceTo(from, loc, clock.getTime());
            } catch (WrongTypeException e1) {
                e1.printStackTrace();
                return Double.POSITIVE_INFINITY; // Fallback in case of error
            }
        }
    }
}