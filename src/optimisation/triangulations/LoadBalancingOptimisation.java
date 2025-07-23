package optimisation.triangulations;

import astrometrics.Location;
import observation.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;
import util.exceptions.WrongTypeException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LoadBalancingOptimisation extends NNOptimisation {

    public LoadBalancingOptimisation() {
        // No initialization required
    }

    /**
     * Creates connections using load-balancing strategy based on expected observation time.
     */
    public void createLoadBalancedLinks(List<Target> targets, Pointable[] currents, double ratio,
                                        Clock[] clocks, Location loc, Telescope[] telescopes)
            throws OutOfObservablesException {

        // Clear existing neighbours
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            currents[i].clearNeighbours();
        }

        // Filter available targets
        List<Target> availableTargets = getAvailableTargets(targets, currents, clocks, loc);

        if (availableTargets.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Partition targets using load-balancing
        List<List<Target>> partitions = partitionLoadBalancing(availableTargets);

        // Create connections from each telescope to its assigned targets
        for (int telescopeIndex = 0; telescopeIndex < Simulation.NUMTELESCOPES; telescopeIndex++) {
            List<Target> assignedTargets = partitions.get(telescopeIndex);

            for (Target target : assignedTargets) {
                if (!isReadyForObservation(target, clocks[telescopeIndex], loc)) continue;

                double dist = calculateDistance(currents[telescopeIndex], target, loc, clocks[telescopeIndex]);
                Connection connection = new Connection(currents[telescopeIndex], target, dist);
                currents[telescopeIndex].addNeighbour(connection);
            }
        }

        // Ensure at least one telescope has targets
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
     * Load-balancing partitioning using Longest Processing Time algorithm.
     */
    private List<List<Target>> partitionLoadBalancing(List<Target> targets) {
        List<List<Target>> partitions = createEmptyPartitions();
        double[] telescopeLoads = new double[Simulation.NUMTELESCOPES];

        // Sort targets by expected observation time (descending)
        targets.sort(Comparator.comparingDouble(this::getEstimatedObservationTime).reversed());

        for (Target target : targets) {
            int lightestIndex = findLightestLoadedTelescope(telescopeLoads);
            partitions.get(lightestIndex).add(target);
            telescopeLoads[lightestIndex] += getEstimatedObservationTime(target);
        }

        return partitions;
    }

    private double getEstimatedObservationTime(Target target) {
        try {
            Observable obs = target.findObservableByObservationTime();
            if (obs != null) {
                return obs.getExpectedIntegrationTime();
            }
        } catch (Exception ignored) {}
        return 1800; // Default to 30 minutes
    }

    private int findLightestLoadedTelescope(double[] loads) {
        int minIndex = 0;
        for (int i = 1; i < loads.length; i++) {
            if (loads[i] < loads[minIndex]) {
                minIndex = i;
            }
        }
        return minIndex;
    }

    private List<List<Target>> createEmptyPartitions() {
        List<List<Target>> partitions = new ArrayList<>();
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            partitions.add(new ArrayList<>());
        }
        return partitions;
    }

    private List<Target> getAvailableTargets(List<Target> targets, Pointable[] currents,
                                             Clock[] clocks, Location loc) {
        List<Target> available = new ArrayList<>();

        for (Target target : targets) {
            if (!target.needsObserving() || target.hasCompleteObservation()) continue;

            boolean currentlyObserved = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (currents[k] == target) {
                    currentlyObserved = true;
                    break;
                }
            }
            if (currentlyObserved) continue;

            boolean observable = false;
            for (int k = 0; k < Simulation.NUMTELESCOPES; k++) {
                if (isReadyForObservation(target, clocks[k], loc)) {
                    observable = true;
                    break;
                }
            }

            if (observable) available.add(target);
        }

        return available;
    }

    private double calculateDistance(Pointable from, Target to, Location loc, Clock clock) {
        try {
            return from.angularDistanceTo(to, loc, clock.getTime());
        } catch (WrongTypeException e) {
            try {
                return to.angularDistanceTo(from, loc, clock.getTime());
            } catch (WrongTypeException e1) {
                e1.printStackTrace();
                return Double.POSITIVE_INFINITY;
            }
        }
    }
}
