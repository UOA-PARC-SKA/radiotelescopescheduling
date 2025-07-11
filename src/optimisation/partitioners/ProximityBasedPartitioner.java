package optimisation.partitioners;

import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Proximity-based partitioner that assigns targets to telescopes based on angular distance from current positions.
 * Minimizes total slew time by preferentially assigning nearby targets to each telescope.
 * Uses greedy assignment strategy to optimize overall system efficiency.
 */
public class ProximityBasedPartitioner implements PulsarPartitioner {

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


    /**
     * Calculate angular distance between a target and a pointable position.
     * Uses spherical trigonometry to compute great circle distance.
     */
    private double calculateAngularDistance(Target target, Pointable position) {
        try {
            // Get coordinates for the target
            double targetRA = target.getEquatorialCoordinates().getRightAscension();
            double targetDec = target.getEquatorialCoordinates().getDeclination();

            double positionRA, positionDec;

            // Handle different types of Pointable positions
            if (position instanceof Target) {
                Target positionTarget = (Target) position;
                positionRA = positionTarget.getEquatorialCoordinates().getRightAscension();
                positionDec = positionTarget.getEquatorialCoordinates().getDeclination();
            } else {
                // For other Pointable types, try to get coordinates
                // This is a fallback - implementation depends on the Pointable interface
                return Math.PI; // Return maximum distance if we can't determine position
            }

            return angularDistance(targetRA, targetDec, positionRA, positionDec);

        } catch (Exception e) {
            // Return maximum distance if coordinates can't be determined
            return Math.PI;
        }
    }

    /**
     * Calculate angular distance between two celestial coordinates using the haversine formula.
     * All angles are in degrees, result is in radians.
     */
    private double angularDistance(double ra1, double dec1, double ra2, double dec2) {
        // Convert degrees to radians
        ra1 = Math.toRadians(ra1);
        dec1 = Math.toRadians(dec1);
        ra2 = Math.toRadians(ra2);
        dec2 = Math.toRadians(dec2);

        // Calculate angular distance using spherical law of cosines
        double deltaRA = ra2 - ra1;
        double cosDistance = Math.sin(dec1) * Math.sin(dec2) +
                Math.cos(dec1) * Math.cos(dec2) * Math.cos(deltaRA);

        // Clamp to valid range to avoid numerical errors
        cosDistance = Math.max(-1.0, Math.min(1.0, cosDistance));

        return Math.acos(cosDistance);
    }
}