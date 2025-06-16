package optimisation.partitioners;

import observation.Observable;
import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Load-balancing partitioner that implements the Longest Processing Time algorithm to ensure even work distribution.
 * Tracks cumulative observation time per telescope and prevents any telescope from being overloaded.
 * Maximises overall system utilisation when all targets have similar scientific value.
 */
public class LoadBalancingPartitioner implements PulsarPartitioner {
    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        List<List<Target>> partitions = createEmptyPartitions();

        if (pulsars.isEmpty()) {
            return partitions;
        }

        // Sort targets by estimated observation time (longest first for better balancing)
        List<Target> sortedPulsars = pulsars.stream()
                .sorted((a, b) -> Double.compare(getEstimatedObservationTime(b), getEstimatedObservationTime(a)))
                .collect(Collectors.toList());

        // Track workload for each telescope
        double[] telescopeLoads = new double[Simulation.NUMTELESCOPES];

        // Assign each target to the telescope with the lightest current load
        for (Target pulsar : sortedPulsars) {
            int lightestTelescope = findLightestLoadedTelescope(telescopeLoads);
            partitions.get(lightestTelescope).add(pulsar);
            telescopeLoads[lightestTelescope] += getEstimatedObservationTime(pulsar);
        }

        return partitions;
    }

    private double getEstimatedObservationTime(Target target) {
        try {
            Observable obs = target.findObservableByObservationTime();
            if (obs != null) {
                return obs.getExpectedIntegrationTime();
            }
        } catch (Exception e) {
            // Fallback estimation
        }
        return 1800; // Default 30 minutes
    }

    private int findLightestLoadedTelescope(double[] loads) {
        int lightestIndex = 0;
        for (int i = 1; i < loads.length; i++) {
            if (loads[i] < loads[lightestIndex]) {
                lightestIndex = i;
            }
        }
        return lightestIndex;
    }
}
