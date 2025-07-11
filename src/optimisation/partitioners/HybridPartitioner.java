package optimisation.partitioners;

import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid partitioner that combines multiple strategies using weighted voting with configurable strategy weights.
 * Uses ensemble decision-making to leverage strengths of different algorithms (40% spatial, 30% proximity, 30% priority).
 * Most robust approach for complex multi-objective optimisation scenarios but computationally intensive.
 */
public class HybridPartitioner implements PulsarPartitioner {
    private final List<PulsarPartitioner> strategies;
    private final List<Double> weights;

    public HybridPartitioner() {
        this.strategies = Arrays.asList(
                new KMeansClusteringPartitioner(),
                new ProximityBasedPartitioner(),
                new PriorityWeightedPartitioner()
        );
        this.weights = Arrays.asList(0.4, 0.3, 0.3); // Weights for each strategy
    }

    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        if (pulsars.isEmpty()) {
            return createEmptyPartitions();
        }

        // Get partitions from each strategy
        List<List<List<Target>>> allPartitions = strategies.stream()
                .map(strategy -> strategy.partition(pulsars, currentPositions))
                .collect(Collectors.toList());

        // Score each target-telescope assignment across all strategies
        Map<Target, int[]> targetScores = new HashMap<>();

        for (Target target : pulsars) {
            int[] scores = new int[Simulation.NUMTELESCOPES];

            for (int strategyIndex = 0; strategyIndex < strategies.size(); strategyIndex++) {
                List<List<Target>> partition = allPartitions.get(strategyIndex);
                double weight = weights.get(strategyIndex);

                for (int telescopeIndex = 0; telescopeIndex < partition.size(); telescopeIndex++) {
                    if (partition.get(telescopeIndex).contains(target)) {
                        scores[telescopeIndex] += (int) (weight * 100);
                    }
                }
            }

            targetScores.put(target, scores);
        }

        // Assign targets to telescopes based on combined scores
        List<List<Target>> finalPartitions = createEmptyPartitions();
        List<Target> unassigned = new ArrayList<>(pulsars);

        // Use a greedy approach to assign targets
        while (!unassigned.isEmpty()) {
            Target bestTarget = null;
            int bestTelescope = 0;
            int bestScore = -1;

            for (Target target : unassigned) {
                int[] scores = targetScores.get(target);
                for (int i = 0; i < scores.length; i++) {
                    // Adjust score based on current telescope load
                    int adjustedScore = scores[i] - finalPartitions.get(i).size() * 10;
                    if (adjustedScore > bestScore) {
                        bestScore = adjustedScore;
                        bestTarget = target;
                        bestTelescope = i;
                    }
                }
            }

            if (bestTarget != null) {
                finalPartitions.get(bestTelescope).add(bestTarget);
                unassigned.remove(bestTarget);
            } else {
                break; // Safety break
            }
        }

        return finalPartitions;
    }
}
