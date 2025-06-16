package optimisation.partitioners;

import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.ArrayList;
import java.util.List;

public class SimpleRoundRobinPartitioner implements PulsarPartitioner {
    @Override
    public List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions) {
        List<List<Target>> partitions = new ArrayList<>();

        // Initialize empty partitions
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            partitions.add(new ArrayList<>());
        }

        // Simple round-robin assignment
        for (int i = 0; i < pulsars.size(); i++) {
            int telescopeIndex = i % Simulation.NUMTELESCOPES;
            partitions.get(telescopeIndex).add(pulsars.get(i));
        }

        return partitions;
    }
}