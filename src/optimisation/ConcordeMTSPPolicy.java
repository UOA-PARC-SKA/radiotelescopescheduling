package optimisation;

import astrometrics.EquatorialCoordinates;
import astrometrics.Location;
import observation.*;
import observation.Observable;
import optimisation.partitioners.*;
import simulation.Clock;
import simulation.Simulation;
import util.exceptions.OutOfObservablesException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class ConcordeMTSPPolicy extends DispatchPolicy {

    private static final String CONCORDE_IMAGE = "alehkot/concorde-tsp:1.0";
    private static final String CONTAINER_TSPLIB_DIR = "/usr/local/opt/concorde/";
    private static final String TSPLIB_DIR = "tsplib";

    // Tracking in-progress observations
    private Map<Integer, Target> currentlyObserving;
    private Map<Integer, Long> observationStartTimes;
    private PulsarPartitioner partitioner;
    private final Map<Integer, Target> waitingTargets;


    public ConcordeMTSPPolicy() {
        currentlyObserving = new HashMap<>();
        observationStartTimes = new HashMap<>();
        partitioner = new PriorityWeightedPartitioner(); // TODO: use config
        waitingTargets = new ConcurrentHashMap<>();
    }

    @Override
    public Connection[] findNextPaths(Pointable[] currents) {
        try {
            return solveMTSP(currents);
        } catch (Exception e) {
            throw new RuntimeException("Failed to solve MTSP", e);
        }
    }

    private Connection[] solveMTSP(Pointable[] currents) throws Exception {
        // Step 1: Get available pulsars (excluding currently observing ones)
        List<Target> availablePulsars = getAvailablePulsars();

        if (availablePulsars.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Step 2: Partition pulsars among telescopes
        List<List<Target>> partitions = partitioner.partition(availablePulsars, currents);

        // Step 3: Solve TSP for each partition (sequential for simplicity)
        Connection[] connections = new Connection[Simulation.NUMTELESCOPES];

        for (int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            if (partitions.get(i).isEmpty()) {
                // No targets for this telescope - handle gracefully
                connections[i] = handleNoTargetsForTelescope(i, currents[i]);
            } else {
                connections[i] = solveSingleTSP(i, currents[i], partitions.get(i));
            }

            // Update tracking
            Target nextTarget = (Target) connections[i].getOtherTarget(currents[i]);
            currentlyObserving.put(i, nextTarget);
            observationStartTimes.put(i, Clock.getScheduleClock()[i].getTime().getTimeInMillis());
        }

        return connections;
    }

    private List<Target> getAvailablePulsars() {
        List<Target> available = new ArrayList<>();

        for (Target target : observables) {
            if (!target.hasCompleteObservation() && !isCurrentlyBeingObserved(target)) {
                available.add(target);
            }
        }

        return available;
    }

    private boolean isCurrentlyBeingObserved(Target target) {
        return currentlyObserving.containsValue(target);
    }

    private Connection solveSingleTSP(int telescopeIndex, Pointable current, List<Target> targets) throws Exception {
        if (targets.isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Create neighbors for this telescope's targets
        createNeighborsForTelescope(telescopeIndex, current, targets);

        if (current.getNeighbours().isEmpty()) {
            throw new OutOfObservablesException();
        }

        // Build distance matrix
        long[][] distanceMatrix = buildDistanceMatrix(telescopeIndex, current);

        // Write TSP file
        Path tspFile = writeTspFile(distanceMatrix, telescopeIndex);

        // Solve with Concorde
        Path solFile = runConcorde(tspFile);

        // Parse solution
        int[] tour = parseSolution(solFile);

        // Get next connection (first move in tour)
        int nextIndex = tour[1]; // tour[0] is starting point
        Connection nextConnection = current.getNeighbours().get(nextIndex - 1);

        // Update telescope state
        Pointable nextTarget = nextConnection.getOtherTarget(current);
        TelescopeState newState = telescopes[telescopeIndex].getStateForShortestSlew(
                nextTarget.getHorizonCoordinates(telescopes[telescopeIndex].getLocation(),
                        Clock.getScheduleClock()[telescopeIndex].getTime())
        );

        currentTelescopeStates[telescopeIndex] = newState;
        telescopes[telescopeIndex].applyNewState(newState);
        schedules[telescopeIndex].addLink(nextConnection, newState);

        return nextConnection;
    }

    private void createNeighborsForTelescope(int telescopeIndex, Pointable current, List<Target> targets) {
        current.clearNeighbours();

        for (Target target : targets) {
            try {
                // Check if target is observable for this telescope
                if (!isReadyForObservation(target, Clock.getScheduleClock()[telescopeIndex],
                        telescopes[telescopeIndex].getLocation())) {
                    continue;
                }

                double dist = current.angularDistanceTo(target,
                        telescopes[telescopeIndex].getLocation(),
                        Clock.getScheduleClock()[telescopeIndex].getTime());

                Connection connection = new Connection(current, target, dist);
                current.addNeighbour(connection);

            } catch (Exception e) {
                // Skip this target if there's an error calculating distance
                continue;
            }
        }
    }

    private boolean isReadyForObservation(Target target, Clock clock, Location location) {
        // This method should be implemented based on your existing logic
        // For now, assuming it exists in the parent class or can be accessed
        try {
            // Basic horizon check - you may need to adjust this based on your actual implementation
            return target.getHorizonCoordinates(location, clock.getTime()).getAltitude() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Connection handleNoTargetsForTelescope(int telescopeIndex, Pointable current) {
        // Create or reuse a waiting target for this telescope
        Target waitingTarget = waitingTargets.computeIfAbsent(telescopeIndex, idx -> {
            return new Target(new EquatorialCoordinates(0, 0));
        });

        return new Connection(current, waitingTarget, 0);
    }

    private long[][] buildDistanceMatrix(int telescopeIndex, Pointable current) {
        List<Connection> neighbors = current.getNeighbours();
        int size = neighbors.size() + 1; // +1 for starting position
        long[][] matrix = new long[size][size];

        // Initialize diagonal to 0
        for (int i = 0; i < size; i++) {
            matrix[i][i] = 0;
        }

        // Fill first row and column (distances from/to starting position)
        for (int i = 1; i < size; i++) {
            Connection conn = neighbors.get(i - 1);
            Pointable target = conn.getOtherTarget(current);

            TelescopeState state = telescopes[telescopeIndex].getStateForShortestSlew(
                    target.getHorizonCoordinates(telescopes[telescopeIndex].getLocation(),
                            Clock.getScheduleClock()[telescopeIndex].getTime())
            );

            Observable obs = ((Target) target).findObservableByObservationTime();
            long cost = (long) state.getSlewTime() + (obs != null ? obs.getExpectedIntegrationTime() : 0);

            matrix[0][i] = cost;
            matrix[i][0] = cost;
        }

        // Fill remaining matrix (distances between targets)
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                Connection conn1 = neighbors.get(i - 1);
                Connection conn2 = neighbors.get(j - 1);

                Pointable target1 = conn1.getOtherTarget(current);
                Pointable target2 = conn2.getOtherTarget(current);

                long slewTime = Telescope.calculateShortestSlewTimeBetween(
                        target1.getHorizonCoordinates(telescopes[telescopeIndex].getLocation(),
                                Clock.getScheduleClock()[telescopeIndex].getTime()),
                        target2.getHorizonCoordinates(telescopes[telescopeIndex].getLocation(),
                                Clock.getScheduleClock()[telescopeIndex].getTime())
                );

                Observable obs2 = ((Target) target2).findObservableByObservationTime();
                long cost = slewTime + (obs2 != null ? obs2.getExpectedIntegrationTime() : 0);

                matrix[i][j] = cost;
                matrix[j][i] = cost;
            }
        }

        return matrix;
    }

    private Path writeTspFile(long[][] matrix, int telescopeIndex) throws IOException {
        String fileName = "tsp_telescope_" + telescopeIndex + ".tsp";
        Path file = Paths.get(TSPLIB_DIR, fileName);

        // Ensure directory exists
        Files.createDirectories(file.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("NAME: " + fileName.replace(".tsp", "") + "\n");
            writer.write("TYPE: TSP\n");
            writer.write("DIMENSION: " + matrix.length + "\n");
            writer.write("EDGE_WEIGHT_TYPE: EXPLICIT\n");
            writer.write("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n");
            writer.write("EDGE_WEIGHT_SECTION\n");

            for (long[] row : matrix) {
                for (long val : row) {
                    writer.write(val + " ");
                }
                writer.write("\n");
            }
            writer.write("EOF\n");
        }
        return file;
    }

    private Path runConcorde(Path tspFile) {
        try {
            String baseFileName = tspFile.getFileName().toString();
            String base = baseFileName.replaceAll("\\.tsp$", "");
            Path solPath = tspFile.getParent().resolve(base + ".sol");

            // 1. Check if container is running
            Process checkContainer = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", "concorde")
                    .redirectErrorStream(true)
                    .start();
            checkContainer.waitFor();

            String output = new String(checkContainer.getInputStream().readAllBytes()).trim();
            if (!output.equals("true")) {
                System.out.println("Starting Concorde container...");

                // Build volume path
                String hostVolumePath = Paths.get(System.getProperty("user.dir"), "tsplib").toAbsolutePath().toString();

                // Start container with while true loop to keep it alive
                new ProcessBuilder(
                        "docker", "run", "-d",
                        "--name", "concorde",
                        "-v", hostVolumePath + ":" + CONTAINER_TSPLIB_DIR,
                        "--entrypoint", "sh",
                        CONCORDE_IMAGE,
                        "-c", "while true; do sleep 1000; done")
                        .inheritIO()
                        .start().waitFor();
            }

            // 2. Run Concorde inside running container
            new ProcessBuilder("docker", "exec", "concorde",
                    "concorde", "-x", "-B", "-V", CONTAINER_TSPLIB_DIR + baseFileName)
                    .inheritIO()
                    .start().waitFor();

            return solPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Concorde", e);
        }
    }

    private int[] parseSolution(Path solFile) throws IOException {
        List<String> lines = Files.readAllLines(solFile);
        List<Integer> tour = new ArrayList<>();

        // Skip first line (dimension) and parse remaining lines
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                String[] nodes = line.split("\\s+");
                for (String node : nodes) {
                    try {
                        tour.add(Integer.parseInt(node));
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse node: " + node);
                    }
                }
            }
        }

        if (tour.isEmpty()) {
            throw new IOException("Empty tour solution");
        }

        return tour.stream().mapToInt(i -> i).toArray();
    }
}