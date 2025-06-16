package optimisation.partitioners;

import observation.Observable;
import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Priority-weighted partitioner that performs multi-criteria optimisation considering scientific priority, observation completeness, and integration times.
 * Uses sophisticated scoring to balance observation importance with telescope proximity and current workload.
 * Ensures high-priority targets are observed even if it means slightly longer slew times.
 */
public class PriorityWeightedPartitioner implements PulsarPartitioner {
    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        List<List<Target>> partitions = createEmptyPartitions();

        if (pulsars.isEmpty()) {
            return partitions;
        }

        // Sort pulsars by priority score (highest first)
        List<Target> sortedPulsars = pulsars.stream()
                .sorted((a, b) -> Double.compare(calculatePriorityScore(b), calculatePriorityScore(a)))
                .collect(Collectors.toList());

        // Assign high-priority targets first, distributing among telescopes
        for (int i = 0; i < sortedPulsars.size(); i++) {
            Target pulsar = sortedPulsars.get(i);
            int telescopeIndex = findBestTelescopeForTarget(pulsar, currentPositions, partitions);
            partitions.get(telescopeIndex).add(pulsar);
        }

        return partitions;
    }

    private double calculatePriorityScore(Target target) {
        double score = 0;

        try {
            Observable obs = target.findObservableByObservationTime();
            if (obs != null) {
                // Factor in remaining integration time (more remaining = higher priority)
                double remainingTime = obs.getRemainingIntegrationTime();
                score += remainingTime * 0.1; // Scale factor to balance with other components

                // Factor in expected integration time (shorter total observations get slight priority)
                double totalTime = obs.getExpectedIntegrationTime();
                score += 100.0 / Math.max(1, totalTime);
            }

            // Prioritize targets that need observing and aren't complete
            if (target.needsObserving() && !target.hasCompleteObservation()) {
                score += 200;
            }

            // Add small random component to break ties
            score += Math.random() * 10;

        } catch (Exception e) {
            score = Math.random() * 10; // Fallback score
        }

        return score;
    }

    private int findBestTelescopeForTarget(Target target, Pointable[] currentPositions,
                                           List<List<Target>> currentPartitions) {
        int bestTelescope = 0;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            double score = calculateTelescopeScore(target, currentPositions[i], currentPartitions.get(i));
            if (score > bestScore) {
                bestScore = score;
                bestTelescope = i;
            }
        }

        return bestTelescope;
    }

    private double calculateTelescopeScore(Target target, Pointable currentPosition, List<Target> currentTargets) {
        double score = 0;

        // Prefer telescopes with fewer assigned targets (load balancing)
        score -= currentTargets.size() * 100;

        // Prefer telescopes closer to the target
        try {
            if (currentPosition instanceof Target) {
                Target currentTarget = (Target) currentPosition;
                double ra1 = target.getEquatorialCoordinates().getRightAscension();
                double dec1 = target.getEquatorialCoordinates().getDeclination();
                double ra2 = currentTarget.getEquatorialCoordinates().getRightAscension();
                double dec2 = currentTarget.getEquatorialCoordinates().getDeclination();

                double distance = angularDistance(ra1, dec1, ra2, dec2);
                score -= distance * 50; // Closer is better
            }
        } catch (Exception e) {
            // Continue with other factors
        }

        return score;
    }

    private double angularDistance(double ra1, double dec1, double ra2, double dec2) {
        ra1 = Math.toRadians(ra1);
        dec1 = Math.toRadians(dec1);
        ra2 = Math.toRadians(ra2);
        dec2 = Math.toRadians(dec2);

        double deltaRA = ra2 - ra1;
        double a = Math.sin(dec1) * Math.sin(dec2) +
                Math.cos(dec1) * Math.cos(dec2) * Math.cos(deltaRA);
        return Math.acos(Math.max(-1, Math.min(1, a)));
    }
}
