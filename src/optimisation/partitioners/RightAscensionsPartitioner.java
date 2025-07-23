package optimisation.partitioners;

import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Proximity-based partitioner that assigns targets to telescopes based on angular distance from current positions.
 * Minimizes total slew time by preferentially assigning nearby targets to each telescope.
 * Uses greedy assignment strategy to optimize overall system efficiency.
 */
public class RightAscensionsPartitioner implements PulsarPartitioner {

    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        List<List<Target>> partitions = createEmptyPartitions();
        if (pulsars.isEmpty()) {
            return partitions;
        }

        // Create a working copy of targets
        List<Target> sortedTargets = new ArrayList<>(pulsars);

        // Sort targets by Right Ascension for spatial grouping
        sortedTargets.sort(Comparator.comparingDouble(
                t -> t.getEquatorialCoordinates().getRightAscension()
        ));

        int numTargets = sortedTargets.size();
        int numTelescopes = Simulation.NUMTELESCOPES;
        int baseSize = numTargets / numTelescopes;
        int remainder = numTargets % numTelescopes;

        int startIdx = 0;
        for (int i = 0; i < numTelescopes; i++) {
            // Distribute the remainder across the first few telescopes
            int blockSize = baseSize + (i < remainder ? 1 : 0);
            int endIdx = Math.min(startIdx + blockSize, numTargets);

            if (startIdx >= numTargets) break;

            List<Target> telescopeBlock = sortedTargets.subList(startIdx, endIdx);
            partitions.get(i).addAll(telescopeBlock);

            // Update telescope position to last target in its block
            if (!telescopeBlock.isEmpty()) {
                currentPositions[i] = telescopeBlock.getLast();
            }

            startIdx = endIdx;
        }

        return partitions;
    }
}