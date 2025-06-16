package optimisation.partitioners;

import observation.Pointable;
import observation.Target;
import simulation.Simulation;

import java.util.ArrayList;
import java.util.List;

public interface PulsarPartitioner {
    List<List<Target>> partition(List<Target> pulsars, Pointable[] currentPositions);

    default List<List<Target>> createEmptyPartitions() {
        List<List<Target>> partitions = new ArrayList<>();
        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            partitions.add(new ArrayList<>());
        }
        return partitions;
    }
}